package com.example.myaiapp.memory.serializer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.Profile

val Context.profileDS: DataStore<Profile> by dataStore(
    fileName = "profile.pb",
    serializer = ProfileSerializer
)
val Context.agentPrefsDS: DataStore<AgentPrefs> by dataStore(
    fileName = "agent_prefs.pb",
    serializer = AgentPrefsSerializer
)