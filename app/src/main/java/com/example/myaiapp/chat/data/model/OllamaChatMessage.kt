package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaChatMessage(
    val role: Role,
    val content: String,
)