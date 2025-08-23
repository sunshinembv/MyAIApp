package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val temperature: Double? = null,
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    val stream: Boolean? = null,
    val reasoning: OpenRouterReasoning? = null,
)
