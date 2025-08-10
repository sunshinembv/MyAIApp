package com.example.myaiapp.chat.di

import com.example.myaiapp.chat.presentation.ChatViewModel
import com.example.myaiapp.chat.presentation.state.ChatActor
import com.example.myaiapp.chat.presentation.state.ChatReducer
import com.example.myaiapp.chat.presentation.state.ChatState
import dagger.Module
import dagger.Provides

@Module
class ChatModule {

    @Provides
    @ChatScope
    fun provideChatViewModelAssistedFactory(
        reducer: ChatReducer,
        actor: ChatActor
    ): ChatViewModel.AssistedFactory {
        return ChatViewModel.AssistedFactory(reducer, actor)
    }

    @Provides
    @ChatScope
    fun provideChatReducer(): ChatReducer {
        return ChatReducer(ChatState())
    }
}