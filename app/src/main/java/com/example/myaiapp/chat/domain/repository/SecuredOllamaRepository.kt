package com.example.myaiapp.chat.domain.repository

import com.example.myaiapp.chat.domain.model.LlmModels

interface SecuredOllamaRepository {
    suspend fun chat(content: String, model: LlmModels): Result<String>
}