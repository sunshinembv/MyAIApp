package com.example.myaiapp.chat.data.db.repository

import com.example.myaiapp.chat.data.db.dao.MessageDao
import com.example.myaiapp.chat.data.db.entities.MessageEntity
import com.example.myaiapp.chat.data.model.Role
import javax.inject.Inject

class MessageDaoRepository @Inject constructor(private val dao: MessageDao) {

    suspend fun addUser(text: String): Long =
        dao.insertMessage(MessageEntity(role = Role.USER, content = text))

    suspend fun addAssistant(text: String): Long =
        dao.insertMessage(MessageEntity(role = Role.ASSISTANT, content = text))

    suspend fun getMessages(): List<MessageEntity> = dao.getMessages()
}