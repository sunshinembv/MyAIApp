package com.example.myaiapp.db.di

import android.content.Context
import androidx.room.Room
import com.example.myaiapp.chat.data.db.dao.MessageDao
import com.example.myaiapp.db.MyAIAppDatabase
import com.example.myaiapp.memory.db.dao.MemoryDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(context: Context): MyAIAppDatabase {
        return Room.databaseBuilder(context, MyAIAppDatabase::class.java, MyAIAppDatabase.DB_NAME)
            .build()
    }

    @Provides
    @Singleton
    fun provideMessageDao(myAIAppDatabase: MyAIAppDatabase): MessageDao {
        return myAIAppDatabase.messageDao()
    }

    @Provides
    @Singleton
    fun provideMemoryDao(myAIAppDatabase: MyAIAppDatabase): MemoryDao {
        return myAIAppDatabase.memoryDao()
    }
}