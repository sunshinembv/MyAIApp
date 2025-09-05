package com.example.myaiapp.chat.di

import com.example.myaiapp.chat.data.repository.OllamaRepositoryImpl
import com.example.myaiapp.chat.data.repository.PersonalOllamaRepositoryImpl
import com.example.myaiapp.chat.data.repository.SecuredOllamaRepositoryImpl
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.domain.repository.PersonalOllamaRepository
import com.example.myaiapp.chat.domain.repository.SecuredOllamaRepository
import dagger.Binds
import dagger.Module

@Module
interface MessageRepositoryModule {

    @Binds
    @ChatScope
    fun provideOllamaRepository(impl: OllamaRepositoryImpl): OllamaRepository

    @Binds
    @ChatScope
    fun provideSecuredOllamaRepository(impl: SecuredOllamaRepositoryImpl): SecuredOllamaRepository

    @Binds
    @ChatScope
    fun providePersonalOllamaRepository(impl: PersonalOllamaRepositoryImpl): PersonalOllamaRepository
}