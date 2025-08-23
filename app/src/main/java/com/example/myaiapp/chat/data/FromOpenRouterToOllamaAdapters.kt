package com.example.myaiapp.chat.data

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaChatResponse
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.open_router.OpenRouterChatRequest
import com.example.myaiapp.chat.data.open_router.OpenRouterChatResponse
import com.example.myaiapp.chat.data.open_router.OpenRouterMessage
import com.example.myaiapp.chat.data.open_router.OpenRouterReasoning

fun OllamaChatRequest.toOpenRouter(): OpenRouterChatRequest = OpenRouterChatRequest(
    model = model,
    messages = messages.map { OpenRouterMessage(role = it.role.name.lowercase(), content = it.content) },
    stream = if (stream) true else null,
    // Optional: ask the model to include visible reasoning tokens
    reasoning = OpenRouterReasoning(effort = "medium", exclude = true)
)

fun OpenRouterChatResponse.toOllama(): OllamaChatResponse {
    val content = choices.firstOrNull()?.message?.content.orEmpty()
    return OllamaChatResponse(
        message = OllamaChatMessage(
            role = Role.ASSISTANT,
            content = content
        )
    )
}
