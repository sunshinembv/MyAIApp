package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyJson(
    val checked: List<VerifyItem>,
    val score: Int,                  // 0–100
    @Json(name = "safe_final")
    val safeFinal: String? = null   // при необходимости — безопасная правка ответа
)