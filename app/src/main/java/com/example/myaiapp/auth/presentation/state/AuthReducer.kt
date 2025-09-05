package com.example.myaiapp.auth.presentation.state

import com.example.myaiapp.auth.data.PinResult
import com.example.myaiapp.auth.presentation.state.AuthCommand.GetAuthStateFromDataStore
import com.example.myaiapp.auth.presentation.state.AuthCommand.Register
import com.example.myaiapp.auth.presentation.state.AuthCommand.SetPin
import com.example.myaiapp.auth.presentation.state.AuthCommand.VerifyPin
import com.example.myaiapp.auth.presentation.state.AuthGateState.Locked
import com.example.myaiapp.core.Reducer
import com.example.myaiapp.core.Result
import java.util.Date
import javax.inject.Inject

class AuthReducer @Inject constructor(
    state: AuthState,
) : Reducer<AuthEvents, AuthState, AuthCommand>(state) {

    override fun reduce(
        event: AuthEvents,
        state: AuthState
    ): Result<AuthCommand> {
        return when (event) {
            AuthEvents.Ui.GetAuthStateFromDataStore -> {
                setState(
                    state.copy(
                        isLoading = true
                    )
                )

                Result(GetAuthStateFromDataStore)
            }

            AuthEvents.Ui.LogoutAndClear -> {
                Result(AuthCommand.LogoutAndClear)
            }

            is AuthEvents.Ui.Register -> {
                val command = Register(
                    email = event.email,
                    password = event.password,
                )
                Result(command)
            }

            is AuthEvents.Ui.ResetPinWithPassword -> {
                Result(AuthCommand.ResetPinWithPassword(event.password))
            }

            is AuthEvents.Ui.SetPin -> {
                val command = SetPin(
                    pin4 = event.pin4
                )
                Result(command)
            }

            is AuthEvents.Ui.VerifyPin -> {
                val command = VerifyPin(
                    pin4 = event.pin4
                )
                Result(command)
            }

            AuthEvents.Internal.Registered -> {
                setState(
                    state.copy(
                        gateState = AuthGateState.NeedPinSetup
                    )
                )
                Result(null)
            }

            AuthEvents.Internal.PinSet -> {
                setState(
                    state.copy(
                        gateState = AuthGateState.Unlocked
                    )
                )
                Result(null)
            }

            is AuthEvents.Ui.OpenChatScreen -> {
                Result(null)
            }

            is AuthEvents.Internal.AuthStateLoaded -> {
                setState(
                    state.copy(
                        gateState = event.authGateState,
                        isLoading = false,
                    )
                )

                Result(null)
            }

            is AuthEvents.Internal.PinVerifyStatus -> {
                setState(
                    state.copy(
                        gateState = when (event.pinResult) {
                            is PinResult.Failure -> Locked("Failure")
                            is PinResult.Locked -> Locked("Locked until ${Date(event.pinResult.unlockAtMs)}")
                            is PinResult.RetryLeft -> Locked("Attempts left: ${event.pinResult.count}")
                            PinResult.Success -> AuthGateState.Unlocked
                        },
                    )
                )

                Result(null)
            }

            AuthEvents.Internal.DataCleared -> {
                setState(
                    state.copy(
                        gateState = AuthGateState.NeedRegister,
                    )
                )

                Result(null)
            }

            is AuthEvents.Internal.PinResetStatus -> {
                val status = event.status
                setState(
                    state.copy(
                        gateState = if (status) AuthGateState.NeedPinSetup else state.gateState,
                    )
                )
                Result(null)
            }
        }
    }
}