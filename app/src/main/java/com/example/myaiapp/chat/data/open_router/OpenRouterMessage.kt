package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterMessage(
    val role: String,                    // "system" | "user" | "assistant" | "tool"
    val content: String,
    // For reasoning models (e.g., DeepSeek R1), OpenRouter may return this field
    val reasoning: String? = null,
)
