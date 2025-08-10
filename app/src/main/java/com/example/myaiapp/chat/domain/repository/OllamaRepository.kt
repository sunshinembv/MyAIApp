package com.example.myaiapp.chat.domain.repository

import com.example.myaiapp.chat.domain.model.ChatMessage

interface OllamaRepository {
    suspend fun chatOnce(model: String, content: String, history: List<ChatMessage>): ChatMessage
}