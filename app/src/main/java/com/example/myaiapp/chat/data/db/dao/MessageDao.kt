package com.example.myaiapp.chat.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myaiapp.chat.data.db.contracts.MessageContract
import com.example.myaiapp.chat.data.db.entities.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(entity: MessageEntity): Long

    @Query("SELECT * FROM ${MessageContract.MESSAGE_TABLE_NAME} ORDER BY ${MessageContract.Columns.CREATED_AT} ASC, id ASC")
    suspend fun getMessages(): List<MessageEntity>
}