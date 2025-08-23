package com.example.myaiapp.chat.data.open_router

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterChatResponse(
    val id: String,
    val choices: List<OpenRouterChoice>,
    val usage: OpenRouterUsage? = null,
)
