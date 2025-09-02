package com.example.myaiapp.chat.di

import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.presentation.ChatViewModel
import com.example.myaiapp.chat.presentation.mapper.ChatUiModelMapper
import com.example.myaiapp.chat.presentation.state.ChatActor
import com.example.myaiapp.chat.presentation.state.ChatReducer
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.chat.voice.Stt
import com.example.myaiapp.chat.voice.Tts
import dagger.Module
import dagger.Provides

@Module
class ChatModule {

    @Provides
    @ChatScope
    fun provideChatViewModelAssistedFactory(
        reducer: ChatReducer,
        actor: ChatActor,
        stt: Stt,
        tts: Tts,
        ollamaRepository: OllamaRepository,
    ): ChatViewModel.AssistedFactory {
        return ChatViewModel.AssistedFactory(reducer, actor, stt, tts, ollamaRepository)
    }

    @Provides
    @ChatScope
    fun provideChatReducer(): ChatReducer {
        return ChatReducer(state = ChatState(), mapper = ChatUiModelMapper())
    }
}
