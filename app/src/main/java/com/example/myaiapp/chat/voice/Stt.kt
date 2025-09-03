package com.example.myaiapp.chat.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

interface Stt {
    fun start(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onError: (Int) -> Unit)
    fun stop()

    fun setCoroutineScope(scope: CoroutineScope)
}

class SttImpl @Inject constructor(
    private val appContext: Context,
): Stt {
    private var recognizer: SpeechRecognizer? = null
    private var stopRequested = false
    private var isStopping = false
    private var restartJob: Job? = null
    private var stopTimeoutJob: Job? = null
    private var lastPartial: String = ""

    private var onPartialCb: ((String) -> Unit)? = null
    private var onFinalCb:   ((String) -> Unit)? = null
    private var onErrorCb:   ((Int) -> Unit)? = null

    private var coroutineScope: CoroutineScope? = null

    private fun newIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        // Делаем запись «бесконечной»: авто-энд по тишине отключаем/растягиваем.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 60 * 60 * 1000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60 * 60 * 1000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60 * 60 * 1000)
        // putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        // putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
    }

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (Int) -> Unit
    ) {
        stopRequested = false
        isStopping = false
        lastPartial = ""
        restartJob?.cancel()
        stopTimeoutJob?.cancel()
        onPartialCb = onPartial
        onFinalCb   = onFinal
        onErrorCb   = onError

        release()
        createRecognizer()
        recognizer?.startListening(newIntent())
    }

    override fun stop() {
        // Пользователь нажал STOP → аккуратно завершаем сессию
        stopRequested = true
        isStopping = true

        // Перестанем автоперезапускаться
        restartJob?.cancel()
        restartJob = null

        // Просим движок выдать onResults()
        try {
            recognizer?.stopListening()
        } catch (_: Throwable) {}

        // Фоллбэк: если onResults/onError не придут за 1.2 сек — завершаем сами
        stopTimeoutJob?.cancel()
        stopTimeoutJob = coroutineScope?.launch {
            delay(1200L)
            if (isStopping) {
                val text = lastPartial
                if (text.isNotBlank()) onFinalCb?.invoke(text) else onErrorCb?.invoke(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                cleanupAfterStop()
            }
        }
    }

    private fun createRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
            setRecognitionListener(listener)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {} // игнорируем, управляем сами
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(bundle: Bundle) {
            val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()
            if (text.isNotBlank() && text != lastPartial) {
                lastPartial = text
                onPartialCb?.invoke(text)
            }
        }

        override fun onResults(bundle: Bundle) {
            val text = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty()

            if (isStopping) {
                // Это завершение по кнопке STOP → выдаём финал и чистим всё
                stopTimeoutJob?.cancel()
                if (text.isNotBlank()) onFinalCb?.invoke(text) else if (lastPartial.isNotBlank()) onFinalCb?.invoke(lastPartial)
                cleanupAfterStop()
            } else {
                // Мы просили «вечную» запись, но движок сам решил завершить —
                // отдаём финал и перезапускаем для непрерывного режима
                if (text.isNotBlank()) onFinalCb?.invoke(text)
                scheduleRestart()
            }
        }

        override fun onError(error: Int) {
            if (isStopping) {
                // Завершение по STOP, но пришла ошибка вместо результатов.
                stopTimeoutJob?.cancel()
                if (lastPartial.isNotBlank()) onFinalCb?.invoke(lastPartial) else onErrorCb?.invoke(error)
                cleanupAfterStop()
                return
            }
            if (stopRequested) return
            onErrorCb?.invoke(error)
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        if (stopRequested || isStopping) return
        restartJob?.cancel()
        restartJob = coroutineScope?.launch {
            delay(250L)
            try {
                recognizer?.stopListening()
                recognizer?.cancel()
            } catch (_: Throwable) {}
            release()
            createRecognizer()
            try {
                recognizer?.startListening(newIntent())
            } catch (_: Throwable) {
                delay(1000L)
                if (!stopRequested && !isStopping) recognizer?.startListening(newIntent())
            }
        }
    }

    private fun cleanupAfterStop() {
        isStopping = false
        stopRequested = false
        try {
            recognizer?.cancel()
        } catch (_: Throwable) {}
        release()
    }

    private fun release() {
        recognizer?.destroy()
        recognizer = null
    }

    override fun setCoroutineScope(scope: CoroutineScope) {
        coroutineScope = scope
    }
}