package com.example.myaiapp.shared_preferences.di

import com.example.myaiapp.shared_preferences.SharedPreferencesProvider
import com.example.myaiapp.shared_preferences.SharedPreferencesProviderImpl
import dagger.Binds
import dagger.Module

@Module
interface SharedPreferencesProviderModule {

    @Binds
    fun bindSharedPreferencesProvider(impl: SharedPreferencesProviderImpl): SharedPreferencesProvider
}