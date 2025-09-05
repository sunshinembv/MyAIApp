package com.example.myaiapp.navigation.destination

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.myaiapp.chat.di.DaggerChatComponent
import com.example.myaiapp.chat.presentation.ChatRoute
import com.example.myaiapp.chat.presentation.ChatViewModel
import com.example.myaiapp.utils.appComponent
import com.example.myaiapp.utils.daggerViewModel

private const val CHAT_SCREEN_ROUTE = "chat"

fun NavGraphBuilder.chatScreen(popBackStack: () -> Unit) {

    composable(route = CHAT_SCREEN_ROUTE) { backStackEntry ->

        val component = DaggerChatComponent.factory().create(LocalContext.current.appComponent)
        val viewModel: ChatViewModel = daggerViewModel {
            component.getChatViewModelAssistedFactory().create()
                .create(ChatViewModel::class.java)
        }

        ChatRoute(
            viewModel = viewModel,
            popBackStack = popBackStack
        )
    }
}

fun NavController.navigateToChatScreen() {
    navigate(CHAT_SCREEN_ROUTE)
}
