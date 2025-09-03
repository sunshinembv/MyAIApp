package com.example.myaiapp.chat.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import javax.inject.Inject

interface Tts {
    suspend fun speakAndAwait(text: String)
    fun shutdown()
    fun stop(): Int
}
class TtsImpl @Inject constructor(
    private val appContext: Context
): Tts {
    private val tts = TextToSpeech(appContext) { /* init status */ }.apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    override fun stop() = tts.stop()
    override fun shutdown() = tts.shutdown()

    override suspend fun speakAndAwait(text: String) = kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
        val id = System.currentTimeMillis().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(uttId: String?) {}
            override fun onDone(uttId: String?) { if (!cont.isCompleted) cont.resume(Unit, null) }
            override fun onError(uttId: String?) { if (!cont.isCompleted) cont.resume(Unit, null) }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
        cont.invokeOnCancellation { tts.stop() }
    }
}