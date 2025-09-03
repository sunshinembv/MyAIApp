package com.example.myaiapp.chat.voice.di

import com.example.myaiapp.chat.voice.Stt
import com.example.myaiapp.chat.voice.SttImpl
import com.example.myaiapp.chat.voice.Tts
import com.example.myaiapp.chat.voice.TtsImpl
import dagger.Binds
import dagger.Module

@Module
interface VoiceModule {

    @Binds
    fun bindAndroidStt(impl: SttImpl): Stt

    @Binds
    fun bindAndroidTts(impl: TtsImpl): Tts

}