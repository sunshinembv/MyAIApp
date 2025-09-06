package com.example.myaiapp.auth.di

import android.content.Context
import androidx.datastore.core.DataStore
import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.Profile
import com.example.myaiapp.auth.presentation.AuthViewModel
import com.example.myaiapp.memory.db.dao.MemoryDao
import com.example.myaiapp.memory.di.AgentPrefsStore
import com.example.myaiapp.memory.di.ProfileStore
import dagger.Component

@Component(
    modules = [
        AuthModule::class,
    ],
    dependencies = [AuthDeps::class]
)
@AuthScope
interface AuthComponent {

    @Component.Factory
    interface Factory {
        fun create(
            authDeps: AuthDeps
        ): AuthComponent
    }

    fun getAuthViewModelAssistedFactory(): AuthViewModel.AssistedFactory
}

interface AuthDeps {
    fun context(): Context
    fun memoryDao(): MemoryDao
    @ProfileStore
    fun dataStoreProfile(): DataStore<Profile>
    @AgentPrefsStore
    fun dataStoreAgentPrefs(): DataStore<AgentPrefs>
}
