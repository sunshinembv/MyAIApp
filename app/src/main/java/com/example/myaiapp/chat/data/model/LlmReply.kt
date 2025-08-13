package com.example.myaiapp.chat.data.model

sealed interface LlmReply {
    data class Text(val text: String, val rawAssistant: List<OllamaChatMessage>) : LlmReply
    data class Json(val value: Any?, val rawAssistant: List<OllamaChatMessage>) : LlmReply
}