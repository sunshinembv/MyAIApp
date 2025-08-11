package com.example.myaiapp.chat.data.repository

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaChatResponse
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.network.AIApi
import javax.inject.Inject

class OllamaRepositoryImpl @Inject constructor(
    private val api: AIApi,
): OllamaRepository {

    override suspend fun chatOnce(model: String, systemPrompt: String, content: String, history: List<OllamaChatMessage>): OllamaChatResponse {
        val messages = buildList {
            addAll(history)
            add(OllamaChatMessage(role = Role.USER, content = content))
        }
        val request = OllamaChatRequest(
            model = model,
            messages = messages,
            // Критично для «только JSON» в Ollama:
            format = "json",
            options = OllamaOptions(
                temperature = 0.1,
                topP = 0.95,
                numCtx = 4096,
                stop = listOf("```") // страховка от кодовых блоков
            ),
            stream = false,
            keepAlive = "5m"
        )
        return api.chatOnce(request)
    }
}