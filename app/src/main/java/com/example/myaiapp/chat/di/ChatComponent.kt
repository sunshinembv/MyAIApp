package com.example.myaiapp.chat.di

import android.content.Context
import com.example.myaiapp.chat.presentation.ChatViewModel
import com.example.myaiapp.data_provider.di.DataProviderModule
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import dagger.Component

@Component(
    modules = [ChatModule::class, MessageRepositoryModule::class, DataProviderModule::class], dependencies = [ChatDeps::class]
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
    fun mistralApi(): MistralApi
    fun openRouterApi(): OpenRouterApi
    fun context(): Context
}
