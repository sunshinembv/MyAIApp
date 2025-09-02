package com.example.myaiapp.di

import android.content.Context
import com.example.myaiapp.chat.data.db.dao.MessageDao
import com.example.myaiapp.chat.di.ChatDeps
import com.example.myaiapp.db.di.RoomModule
import com.example.myaiapp.network.GitHubActionsApi
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Component(
    modules = [
        NetworkModule::class,
        RoomModule::class,
    ]
)
@Singleton
interface AppComponent : ChatDeps {


    override fun mistralApi(): MistralApi
    override fun openRouterApi(): OpenRouterApi
    override fun gitHubActionsApi(): GitHubActionsApi
    override fun messageDao(): MessageDao

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context
        ): AppComponent
    }
}