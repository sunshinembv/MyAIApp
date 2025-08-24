package com.example.myaiapp.chat.data.git_hub.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DispatchBody(
    val ref: String,
    val inputs: Map<String, String> = emptyMap(),
)
