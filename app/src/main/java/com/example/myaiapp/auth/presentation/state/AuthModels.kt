package com.example.myaiapp.auth.presentation.state

import com.example.myaiapp.auth.data.PinResult
import com.example.myaiapp.core.Command
import com.example.myaiapp.core.Event
import com.example.myaiapp.core.State

data class AuthState(
    val gateState: AuthGateState = AuthGateState.NeedRegister,
    val isLoading: Boolean = false,
): State

sealed interface AuthGateState {
    data object NoAccount : AuthGateState
    data object NeedRegister : AuthGateState
    data object NeedPinSetup : AuthGateState
    data class Locked(val info: String?) : AuthGateState
    data object Unlocked : AuthGateState
}

sealed class AuthEvents : Event {

    sealed class Ui : AuthEvents() {
        data class Register(val email: String, val password: String) : Ui()
        data class SetPin(val pin4: String) : Ui()
        data class VerifyPin(val pin4: String) : Ui()
        data class ResetPinWithPassword(val password: String) : Ui()
        data object LogoutAndClear : Ui()
        data class OpenChatScreen(val callback: () -> Unit) : Ui()
        data object GetAuthStateFromDataStore : Ui()
    }

    sealed class Internal : AuthEvents() {
        data object Registered : Internal()
        data object PinSet : Internal()
        data class PinVerifyStatus(val pinResult: PinResult) : Internal()
        data class AuthStateLoaded(val authGateState: AuthGateState) : Internal()
        data class PinResetStatus(val status: Boolean) : Internal()
        data object DataCleared : Internal()
    }
}

sealed class AuthCommand : Command {
    data class Register(val email: String, val password: String) : AuthCommand()
    data class SetPin(val pin4: String) : AuthCommand()
    data class VerifyPin(val pin4: String) : AuthCommand()
    data class ResetPinWithPassword(val password: String) : AuthCommand()
    data object LogoutAndClear : AuthCommand()
    data object GetAuthStateFromDataStore : AuthCommand()
}
