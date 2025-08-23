package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterReasoning(
    // One of the following may be set: either effort or maxTokens
    val effort: String? = null,          // "low" | "medium" | "high"
    @Json(name = "max_tokens") val maxTokens: Int? = null,
    // Optional: include/exclude reasoning tokens in response
    val exclude: Boolean? = null,
)
