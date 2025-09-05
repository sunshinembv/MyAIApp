package com.example.myaiapp.memory.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.Profile
import com.example.myaiapp.memory.serializer.AgentPrefsSerializer
import com.example.myaiapp.memory.serializer.ProfileSerializer
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
object DataStoreModule {

    @Provides @Singleton
    fun provideDataStoreScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides @Singleton @ProfileStore
    fun provideProfileDataStore(
        appContext: Context,
        scope: CoroutineScope
    ): DataStore<Profile> = DataStoreFactory.create(
        serializer = ProfileSerializer,
        scope = scope,
        produceFile = { appContext.dataStoreFile("profile.pb") }
    )

    @Provides @Singleton @AgentPrefsStore
    fun provideAgentPrefsDataStore(
        appContext: Context,
        scope: CoroutineScope
    ): DataStore<AgentPrefs> = DataStoreFactory.create(
        serializer = AgentPrefsSerializer,
        scope = scope,
        produceFile = { appContext.dataStoreFile("agent_prefs.pb") }
    )
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProfileStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AgentPrefsStore