package com.example.myaiapp.memory.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myaiapp.memory.db.entities.MemoryEntity

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory ORDER BY importance DESC, lastUsedAt DESC LIMIT :limit")
    suspend fun topMemories(limit: Int): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MemoryEntity)

    @Query("DELETE FROM memory WHERE id=:id")
    suspend fun delete(id: String)
}