package com.example.myaiapp.di

import android.content.Context
import com.example.myaiapp.chat.di.ChatDeps
import com.example.myaiapp.network.AIApi
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Component(
    modules = [
        NetworkModule::class,
    ]
)
@Singleton
interface AppComponent : ChatDeps {


    override fun aiApi(): AIApi

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context
        ): AppComponent
    }
}