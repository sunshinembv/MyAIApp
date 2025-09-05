package com.example.myaiapp.auth.presentation.state

import com.example.myaiapp.auth.data.AuthRepository
import com.example.myaiapp.chat.data.llm_policy.UserRoleHolder
import com.example.myaiapp.chat.data.llm_policy.UserRoleRepository
import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.core.Actor
import javax.inject.Inject

class AuthActor @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository,
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

        // 3) Всё есть → сразу показываем экран ввода PIN (Locked)
        val info = authRepository.lockInfoText()
        onEvent(AuthEvents.Internal.AuthStateLoaded(AuthGateState.Locked(info)))
    }
}