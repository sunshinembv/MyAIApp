package com.example.myaiapp.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myaiapp.auth.presentation.state.AuthActor
import com.example.myaiapp.auth.presentation.state.AuthEvents
import com.example.myaiapp.auth.presentation.state.AuthReducer
import com.example.myaiapp.auth.presentation.state.AuthState
import com.example.myaiapp.core.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val reducer: AuthReducer,
    private val actor: AuthActor,
) : BaseViewModel<AuthEvents, AuthState>() {

    override val state: StateFlow<AuthState>
        get() = reducer.state

    init {
        obtainEvent(AuthEvents.Ui.GetAuthStateFromDataStore)
    }

    fun obtainEvent(event: AuthEvents.Ui) {
        when (event) {
            AuthEvents.Ui.GetAuthStateFromDataStore,
            AuthEvents.Ui.LogoutAndClear,
            is AuthEvents.Ui.Register,
            is AuthEvents.Ui.ResetPinWithPassword,
            is AuthEvents.Ui.SetPin,
            is AuthEvents.Ui.VerifyPin -> {
                handleEvent(event)
            }

            is AuthEvents.Ui.OpenChatScreen -> {
                openChatScreen(event)
            }
        }
    }

    private fun handleEvent(event: AuthEvents) {
        viewModelScope.launch {
            val result = reducer.sendEvent(event)
            result.command?.let { actor.execute(it, ::handleEvent) }
        }
    }

    private fun openChatScreen(event: AuthEvents.Ui.OpenChatScreen) {
        viewModelScope.launch {
            reducer.sendEvent(event)
            event.callback.invoke()
        }
    }

    class Factory(
        private val reducer: AuthReducer,
        private val actor: AuthActor,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == AuthViewModel::class.java)
            return AuthViewModel(
                reducer,
                actor,
            ) as T
        }
    }

    class AssistedFactory(
        private val reducer: AuthReducer,
        private val actor: AuthActor,
    ) {
        fun create(): Factory {
            return Factory(reducer, actor)
        }
    }
}