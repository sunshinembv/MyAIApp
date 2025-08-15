package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Plan(
    val action: String,
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
    val content: String,
    val message: String
)
