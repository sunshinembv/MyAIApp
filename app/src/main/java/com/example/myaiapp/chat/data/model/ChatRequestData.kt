package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChatRequestData(
    val model: String,
    val messages: List<ChatMessageData>,
    val stream: Boolean = false,
)