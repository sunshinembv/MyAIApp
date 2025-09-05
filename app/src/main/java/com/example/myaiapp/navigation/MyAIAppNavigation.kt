package com.example.myaiapp.navigation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.myaiapp.navigation.destination.AUTH_SCREEN_ROUTE
import com.example.myaiapp.navigation.destination.authScreen
import com.example.myaiapp.navigation.destination.chatScreen
import com.example.myaiapp.navigation.destination.navigateToChatScreen

@Composable
fun MyAIAppNavigation(
    navController: NavHostController,
    onThemeChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AUTH_SCREEN_ROUTE,
        modifier = modifier.systemBarsPadding()
    ) {
        authScreen(
            toChatScreen = navController::navigateToChatScreen,
            popBackStack = navController::popBackStack,
        )

        chatScreen(
            popBackStack = navController::popBackStack
        )
    }
}