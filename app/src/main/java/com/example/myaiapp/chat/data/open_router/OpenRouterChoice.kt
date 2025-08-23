package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChoice(
    val index: Int,
    val message: OpenRouterMessage? = null,      // non-stream responses
    val delta: OpenRouterMessage? = null,        // stream chunks (SSE)
    @Json(name = "finish_reason") val finishReason: String? = null,
)
