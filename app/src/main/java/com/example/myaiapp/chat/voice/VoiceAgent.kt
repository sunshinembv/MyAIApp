package com.example.myaiapp.chat.voice

import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.presentation.state.ChatEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VoiceAgent(
    private val stt: Stt,
    private val tts: Tts,
    private val scope: CoroutineScope,
    private val ollamaRepository: OllamaRepository,
    private val obtainVoiceEvent: (ChatEvents.VoiceEvent) -> Unit,
) {
    private val mutex = Mutex()
    private var speakJob: Job? = null

    init {
        stt.setCoroutineScope(scope)
    }

    fun tapSpeak() = scope.launch {
        mutex.withLock {
            // barge-in
            speakJob?.cancel()
            tts.stop()
            obtainVoiceEvent(ChatEvents.VoiceEvent.ListeningStarted)
            stt.start(
                onPartial = {
                    obtainVoiceEvent(ChatEvents.VoiceEvent.PartialUpdated(it))
                },
                onFinal = { query ->
                    handleQuery(query)
                },
                onError = { code ->
                    obtainVoiceEvent(ChatEvents.VoiceEvent.Failed("STT error $code"))
                }
            )
        }
    }

    fun tapStop() = scope.launch {
        mutex.withLock {
            stt.stop()
            speakJob?.cancel()
            tts.stop()
            obtainVoiceEvent(ChatEvents.VoiceEvent.Idle)
        }
    }

    private fun handleQuery(query: String) {
        obtainVoiceEvent(ChatEvents.VoiceEvent.FinalRecognized(query))
        speakJob = scope.launch {
            // 1) полный ответ от LLM
            val answer = runCatching { ollamaRepository.chat(query, model = LlmModels.MISTRAL) }
                .getOrElse { e ->
                    obtainVoiceEvent(ChatEvents.VoiceEvent.Failed("LLM error: ${e.message}"))
                    return@launch
                }
            // 2) читаем по частям

            val parts = UtteranceChunker.split(answer)
            for (p in parts) {
                ensureActive()
                obtainVoiceEvent(ChatEvents.VoiceEvent.Speaking(p))
                tts.speakAndAwait(p) // бардж-ин сработает через cancel()
            }
            obtainVoiceEvent(ChatEvents.VoiceEvent.Idle)
        }
    }
}

sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data class Recognizing(val partial: String) : VoiceState
    data class Thinking(val userText: String) : VoiceState
    data class Speaking(val text: String) : VoiceState
    data class Error(val msg: String) : VoiceState
}