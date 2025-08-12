package com.example.myaiapp.chat.domain.repository

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatResponse

interface OllamaRepository {
    suspend fun chatOnce(model: String, systemPrompt: String, content: String, history: List<OllamaChatMessage>): OllamaChatResponse
}