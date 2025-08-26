package com.example.myaiapp.chat.domain.repository

import com.example.myaiapp.chat.data.model.LlmReply
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.domain.model.LlmModels

interface OllamaRepository {
    suspend fun chatOnce(model: String, systemPrompt: String, content: String, history: List<OllamaChatMessage>): LlmReply
    suspend fun chat(content: String, model: LlmModels): String
}