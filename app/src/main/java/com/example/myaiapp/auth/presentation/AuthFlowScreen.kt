package com.example.myaiapp.auth.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myaiapp.auth.presentation.state.AuthEvents
import com.example.myaiapp.auth.presentation.state.AuthGateState
import com.example.myaiapp.auth.presentation.state.AuthState
import com.example.myaiapp.ui.theme.MyAIAppTheme


@Composable
fun AuthFlowRoute(
    viewModel: AuthViewModel,
    toChatScreen: () -> Unit,
    popBackStack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when(state.isLoading) {
        true -> CircularProgressIndicator()
        false -> AuthFlowScreen(
            authState = state,
            popBackStack = popBackStack,
            obtainEvent = viewModel::obtainEvent,
            toChatScreen = toChatScreen,
            modifier = modifier,
        )
    }
}

@Composable
fun AuthFlowScreen(
    authState: AuthState,
    popBackStack: () -> Unit,
    obtainEvent: (AuthEvents.Ui) -> Unit,
    toChatScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text("Secure Agent Login") },
                actions = {
                    if (authState.gateState !is AuthGateState.NeedRegister) {
                        TextButton(onClick = { obtainEvent(AuthEvents.Ui.LogoutAndClear) }) {
                            Text("Reset", color = MyAIAppTheme.colors.primaryTextColor)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            when (authState.gateState) {
                AuthGateState.NeedRegister -> RegisterStep(obtainEvent, toChatScreen)
                AuthGateState.NeedPinSetup -> PinSetupStep(obtainEvent)
                is AuthGateState.Locked -> PinUnlockStep(
                    retryInfo = authState.gateState.info,
                    onEvent = obtainEvent
                )
                AuthGateState.Unlocked -> {
                    obtainEvent(AuthEvents.Ui.OpenChatScreen(toChatScreen))
                }

                AuthGateState.NoAccount -> {
                    obtainEvent(AuthEvents.Ui.OpenChatScreen(toChatScreen))
                }
            }
        }
    }
}