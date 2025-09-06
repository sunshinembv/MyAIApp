package com.example.myaiapp.auth.presentation.state

import com.example.myaiapp.auth.data.AuthRepository
import com.example.myaiapp.chat.data.llm_policy.UserRoleHolder
import com.example.myaiapp.chat.data.llm_policy.UserRoleRepository
import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.core.Actor
import com.example.myaiapp.memory.ImportExportConfig
import com.example.myaiapp.memory.data.repository.PersonalizationRepository
import javax.inject.Inject

class AuthActor @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val importExportConfig: ImportExportConfig,
) : Actor<AuthCommand, AuthEvents.Internal> {

    override suspend fun execute(
        command: AuthCommand,
        onEvent: (AuthEvents.Internal) -> Unit
    ) {
        when (command) {
            AuthCommand.LogoutAndClear -> {
                authRepository.clearAll()
                onEvent(AuthEvents.Internal.DataCleared)
            }
            is AuthCommand.Register -> {
                authRepository.register(
                    email = command.email,
                    password = command.password.toCharArray()
                )
                onEvent(AuthEvents.Internal.Registered)
            }
            is AuthCommand.ResetPinWithPassword -> {
                val status = authRepository.resetPinWithPassword(command.password.toCharArray())
                onEvent(AuthEvents.Internal.PinResetStatus(status))
            }
            is AuthCommand.SetPin -> {
                authRepository.setPin(
                    pin = command.pin4.toCharArray()
                )
                // Импортим персонализированный конфиг пока прям из кода
                importExportConfig.importConfig(personalizationRepository, SEED_JSON)
                onEvent(AuthEvents.Internal.PinSet)
            }
            is AuthCommand.VerifyPin -> {
                val result = authRepository.verifyPin(
                    pin = command.pin4.toCharArray()
                )
                onEvent(AuthEvents.Internal.PinVerifyStatus(result))
            }
            AuthCommand.GetAuthStateFromDataStore -> {
                bootstrap(onEvent)
            }
        }
    }

    private suspend fun bootstrap(onEvent: (AuthEvents.Internal) -> Unit) {
        // 1) Есть ли учётка?
        val registered = authRepository.isRegistered()
        if (!registered) {
            userRoleRepository.setRole(UserRole.GUEST)
            onEvent(AuthEvents.Internal.AuthStateLoaded(AuthGateState.NeedRegister))
            return
        }

        UserRoleHolder.role = userRoleRepository.getRole()

        // 2) Поставлен ли PIN?
        val pinSet = authRepository.isPinSet()
        if (!pinSet) {
            onEvent(AuthEvents.Internal.AuthStateLoaded(AuthGateState.NeedPinSetup))
            return
        }

        //3) Импортим персонализированный конфиг пока прям из кода
        importExportConfig.importConfig(personalizationRepository, SEED_JSON)

        // 3) Всё есть → сразу показываем экран ввода PIN (Locked)
        val info = authRepository.lockInfoText()
        onEvent(AuthEvents.Internal.AuthStateLoaded(AuthGateState.Locked(info)))
    }


    companion object {
        // Минимальный пример JSON для импорта (версия 1)
        private const val SEED_JSON = """
{
  "version": 1,
  "profile": {
    "name": "sunshine007",
    "locale": "ru-RU",
    "timezone": "Europe/Moscow",
    "city": "Moscow",
    "roles": ["android-dev","founder"],
    "interests": ["LLM","Kotlin","Cycling"]
  },
  "prefs": {
    "tone": "FRIENDLY",
    "style": "MIXED",
    "detail": "MEDIUM",
    "useEmoji": false,
    "defaultLanguage": "ru",
    "allowedModels": ["mistral:instruct","deepseek/deepseek-r1:free"],
    "shareProfileWithRemoteLLM": false,
    "shareMemoriesWithRemoteLLM": false,
    "quietHours": [{"start":22,"end":8}]
  },
  "memories": [
    {"text":"Любит музыку", "importance":4, "kind":"preference"}
  ]
}
"""
    }
}