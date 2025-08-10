package com.example.myaiapp.chat.domain.model

data class ChatMessage(
    val role: Role,
    val content: String,
)