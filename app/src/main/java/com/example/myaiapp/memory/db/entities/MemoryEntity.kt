package com.example.myaiapp.memory.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: String,            // "fact" | "preference" | "habit"
    val text: String,            // сам факт: "Пользователь не любит звонки после 21:00"
    val importance: Int,         // 1..5
    val createdAt: Long,
    val lastUsedAt: Long?,
    val source: String? = null   // "chat", "import", "github"
)