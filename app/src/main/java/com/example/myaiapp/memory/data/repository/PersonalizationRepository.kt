package com.example.myaiapp.memory.data.repository

import androidx.datastore.core.DataStore
import com.example.myaiapp.AgentPrefs
import com.example.myaiapp.AgentPrefsKt
import com.example.myaiapp.Profile
import com.example.myaiapp.ProfileKt
import com.example.myaiapp.copy
import com.example.myaiapp.memory.db.dao.MemoryDao
import com.example.myaiapp.memory.db.entities.MemoryEntity
import com.example.myaiapp.memory.di.AgentPrefsStore
import com.example.myaiapp.memory.di.ProfileStore
import javax.inject.Inject

class PersonalizationRepository @Inject constructor(
    @ProfileStore
    private val profileDS: DataStore<Profile>,
    @AgentPrefsStore
    private val agentPrefsDS: DataStore<AgentPrefs>,
    private val memoryDao: MemoryDao,
) {
    val profileFlow = profileDS.data
    val prefsFlow = agentPrefsDS.data

    suspend fun updateProfile(block: ProfileKt.Dsl.() -> Unit) {
        profileDS.updateData { current -> current.copy(block) }
    }

    suspend fun updatePrefs(block: AgentPrefsKt.Dsl.() -> Unit) {
        agentPrefsDS.updateData { current -> current.copy(block) }
    }

    suspend fun addMemory(
        text: String,
        importance: Int,
        kind: String = "fact",
        source: String? = "ui"
    ) {
        memoryDao.insert(
            MemoryEntity(
                kind = kind,
                text = text,
                importance = importance,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = null,
                source = source
            )
        )
    }

    suspend fun selectContextMemories(limit: Int = 8): List<MemoryEntity> =
        memoryDao.topMemories(limit).also { list ->
            val now = System.currentTimeMillis()
            list.forEach { memoryDao.insert(it.copy(lastUsedAt = now)) }
        }
}
