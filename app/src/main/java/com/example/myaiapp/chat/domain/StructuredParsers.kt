package com.example.myaiapp.chat.domain

import com.example.myaiapp.chat.domain.model.format_response.StructuredResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StructuredParsers @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(text: String): StructuredResponse =
        json.decodeFromString(text)
}