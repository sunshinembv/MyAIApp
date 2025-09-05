package com.example.myaiapp.memory.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AgentConfigV1(
    val version: Int = 1,
    val profile: ProfileDTO,
    val prefs: AgentPrefsDTO,
    val memories: List<MemoryDTO> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProfileDTO(
    val name: String, val locale: String, val timezone: String, val city: String,
    val roles: List<String> = emptyList(), val interests: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AgentPrefsDTO(
    val tone: String, val style: String, val detail: String,
    @Json(name = "useEmoji") val useEmoji: Boolean,
    val defaultLanguage: String,
    val allowedModels: List<String> = emptyList(),
    val shareProfileWithRemoteLLM: Boolean = false,
    val shareMemoriesWithRemoteLLM: Boolean = false,
    val quietHours: List<QuietHourDTO> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuietHourDTO(val start: Int, val end: Int)

@JsonClass(generateAdapter = true)
data class MemoryDTO(val text: String, val importance: Int, val kind: String = "fact")