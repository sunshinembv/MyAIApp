package com.example.myaiapp.chat.di

import com.example.myaiapp.chat.presentation.ChatViewModel
import com.example.myaiapp.network.AIApi
import dagger.Component

@Component(
    modules = [ChatModule::class, MessageRepositoryModule::class], dependencies = [ChatDeps::class]
)
@ChatScope
interface ChatComponent {

    @Component.Factory
    interface Factory {
        fun create(
            chatDeps: ChatDeps
        ): ChatComponent
    }

    fun getChatViewModelAssistedFactory(): ChatViewModel.AssistedFactory
}

interface ChatDeps {
    fun aiApi(): AIApi
}