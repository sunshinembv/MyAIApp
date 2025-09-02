package com.example.myaiapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.myaiapp.chat.data.db.dao.MessageDao
import com.example.myaiapp.chat.data.db.entities.MessageEntity
import com.example.myaiapp.chat.data.db.entities.RoleConverter
import com.example.myaiapp.db.MyAIAppDatabase.Companion.DB_VERSION


@Database(
    entities = [
        MessageEntity::class,
    ],
    version = DB_VERSION
)
@TypeConverters(RoleConverter::class)
abstract class MyAIAppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        const val DB_VERSION = 1
        const val DB_NAME = "aiApp-db"
    }
}