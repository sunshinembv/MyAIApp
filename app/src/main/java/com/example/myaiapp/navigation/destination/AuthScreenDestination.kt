package com.example.myaiapp.navigation.destination

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.myaiapp.auth.di.DaggerAuthComponent
import com.example.myaiapp.auth.presentation.AuthFlowRoute
import com.example.myaiapp.auth.presentation.AuthViewModel
import com.example.myaiapp.utils.appComponent
import com.example.myaiapp.utils.daggerViewModel

const val AUTH_SCREEN_ROUTE = "auth"

fun NavGraphBuilder.authScreen(
    toChatScreen: () -> Unit,
    popBackStack: () -> Unit
) {

    composable(route = AUTH_SCREEN_ROUTE) { backStackEntry ->

        val component = DaggerAuthComponent.factory().create(LocalContext.current.appComponent)
        val viewModel: AuthViewModel = daggerViewModel {
            component.getAuthViewModelAssistedFactory().create()
                .create(AuthViewModel::class.java)
        }

        AuthFlowRoute(
            viewModel = viewModel,
            toChatScreen = toChatScreen,
            popBackStack = popBackStack
        )
    }
}