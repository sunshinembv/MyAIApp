package com.example.myaiapp.navigation

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.myaiapp.navigation.destination.CHAT_SCREEN_ROUTE
import com.example.myaiapp.navigation.destination.chatScreen

@Composable
fun MyAIAppNavigation(
    navController: NavHostController,
    onThemeChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = CHAT_SCREEN_ROUTE,
        modifier = modifier.systemBarsPadding()
    ) {
        chatScreen(
            popBackStack = navController::popBackStack
        )
    }
}