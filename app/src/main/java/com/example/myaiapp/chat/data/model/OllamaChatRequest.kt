package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessage>,
    val format: Any? = null,
    val options: OllamaOptions? = null,
    val stream: Boolean = false,
    @Json(name = "keep_alive") val keepAlive: String? = null
)