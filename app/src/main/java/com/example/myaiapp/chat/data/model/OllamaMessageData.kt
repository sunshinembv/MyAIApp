package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaMessageData(
    val role: RoleData? = null,
    val content: String? = null,
)