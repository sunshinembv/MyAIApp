package com.example.myaiapp.chat.di

import com.example.myaiapp.chat.data.repository.OllamaRepositoryImpl
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import dagger.Binds
import dagger.Module

@Module
interface MessageRepositoryModule {

    @Binds
    @ChatScope
    fun provideMessageRepository(impl: OllamaRepositoryImpl): OllamaRepository
}