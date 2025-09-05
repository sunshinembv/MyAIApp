package com.example.myaiapp.chat.domain.repository

import com.example.myaiapp.chat.domain.model.LlmModels

interface PersonalOllamaRepository  {
    suspend fun chat(content: String, model: LlmModels): String
    suspend fun extractFact(content: String, model: LlmModels): String
}