package com.example.myaiapp.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.presentation.state.ChatActor
import com.example.myaiapp.chat.presentation.state.ChatEvents
import com.example.myaiapp.chat.presentation.state.ChatReducer
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.chat.voice.Stt
import com.example.myaiapp.chat.voice.Tts
import com.example.myaiapp.chat.voice.VoiceAgent
import com.example.myaiapp.core.BaseViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val reducer: ChatReducer,
    private val actor: ChatActor,
    private val stt: Stt,
    private val tts: Tts,
    private val ollamaRepository: OllamaRepository,
) : BaseViewModel<ChatEvents, ChatState>() {

    val voiceAgent = VoiceAgent(
        stt = stt,
        tts = tts,
        scope = viewModelScope,
        ollamaRepository = ollamaRepository,
        obtainVoiceEvent = ::obtainVoiceEvent
    )

    fun speak() = voiceAgent.tapSpeak()
    fun stop()  = voiceAgent.tapStop()

    override val state: StateFlow<ChatState>
        get() = reducer.state

    init {
        //obtainEvent(ChatEvents.Ui.GetHistoryFromCache)
        obtainEvent(ChatEvents.Ui.LoadPersonalState)
    }

    fun obtainEvent(event: ChatEvents.Ui) {
        when (event) {

            ChatEvents.Ui.LoadPersonalState,
            ChatEvents.Ui.GetHistoryFromCache,
            is ChatEvents.Ui.CallLlm -> {
                handleEvent(event)
            }

            is ChatEvents.Ui.Typing -> {
                handleOnlyEvent(event)
            }
        }
    }

    fun obtainVoiceEvent(event: ChatEvents.VoiceEvent) {
        when (event) {
            is ChatEvents.VoiceEvent.Failed,
            is ChatEvents.VoiceEvent.FinalRecognized,
            ChatEvents.VoiceEvent.Idle,
            ChatEvents.VoiceEvent.ListeningStarted,
            is ChatEvents.VoiceEvent.PartialUpdated,
            is ChatEvents.VoiceEvent.Speaking -> {
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: ChatEvents) {
        viewModelScope.launch {
            val result = reducer.sendEvent(event)
            result.command?.let { actor.execute(it, ::handleEvent) }
        }
    }

    private fun handleOnlyEvent(event: ChatEvents.Ui) {
        reducer.sendEvent(event)
    }

    class Factory(
        private val reducer: ChatReducer,
        private val actor: ChatActor,
        private val stt: Stt,
        private val tts: Tts,
        private val ollamaRepository: OllamaRepository,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass == ChatViewModel::class.java)
            return ChatViewModel(
                reducer,
                actor,
                stt,
                tts,
                ollamaRepository,
            ) as T
        }
    }

    class AssistedFactory(
        private val reducer: ChatReducer,
        private val actor: ChatActor,
        private val stt: Stt,
        private val tts: Tts,
        private val ollamaRepository: OllamaRepository,
    ) {
        fun create(): Factory {
            return Factory(reducer, actor, stt, tts, ollamaRepository)
        }
    }
}
