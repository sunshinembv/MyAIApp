package com.example.myaiapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.Profile
import com.example.myaiapp.auth.di.AuthDeps
import com.example.myaiapp.chat.data.db.dao.MessageDao
import com.example.myaiapp.chat.di.ChatDeps
import com.example.myaiapp.db.di.RoomModule
import com.example.myaiapp.memory.db.dao.MemoryDao
import com.example.myaiapp.memory.di.AgentPrefsStore
import com.example.myaiapp.memory.di.DataStoreModule
import com.example.myaiapp.memory.di.ProfileStore
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
        DataStoreModule::class,
    ]
)
@Singleton
interface AppComponent : AuthDeps, ChatDeps {


    override fun mistralApi(): MistralApi
    override fun openRouterApi(): OpenRouterApi
    override fun gitHubActionsApi(): GitHubActionsApi
    override fun messageDao(): MessageDao
    override fun memoryDao(): MemoryDao
    @ProfileStore
    override fun dataStoreProfile(): DataStore<Profile>
    @AgentPrefsStore
    override fun dataStoreAgentPrefs(): DataStore<AgentPrefs>

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context
        ): AppComponent
    }
}