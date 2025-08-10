package com.example.myaiapp.chat.data.repository

import com.example.myaiapp.chat.data.mapper.ChatMessagesMapper
import com.example.myaiapp.chat.domain.model.ChatMessage
import com.example.myaiapp.chat.domain.model.ChatRequest
import com.example.myaiapp.chat.domain.model.Role
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.network.AIApi
import javax.inject.Inject

class OllamaRepositoryImpl @Inject constructor(
    private val api: AIApi,
    private val mapper: ChatMessagesMapper,
) : OllamaRepository {

    override suspend fun chatOnce(model: String, content: String, history: List<ChatMessage>): ChatMessage {
        val messages = buildList {
            addAll(history)
            add(ChatMessage(role = Role.USER, content = content))
        }
        val request = mapper.toChatRequestData(ChatRequest(model = model, messages = messages))
        val response = api.chatOnce(request)
        val message = mapper.toChatMessage(response.message)
        return message
    }
}