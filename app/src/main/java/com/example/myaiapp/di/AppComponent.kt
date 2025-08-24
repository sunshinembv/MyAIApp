package com.example.myaiapp.di

import android.content.Context
import com.example.myaiapp.chat.di.ChatDeps
import com.example.myaiapp.network.GitHubActionsApi
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
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


    override fun mistralApi(): MistralApi
    override fun openRouterApi(): OpenRouterApi
    override fun gitHubActionsApi(): GitHubActionsApi

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context
        ): AppComponent
    }
}