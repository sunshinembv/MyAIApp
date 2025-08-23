package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterUsage(
    @Json(name = "prompt_tokens") val promptTokens: Int? = null,
    @Json(name = "completion_tokens") val completionTokens: Int? = null,
    @Json(name = "total_tokens") val totalTokens: Int? = null,
)
