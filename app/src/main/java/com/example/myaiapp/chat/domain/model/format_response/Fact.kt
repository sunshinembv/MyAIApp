package com.example.myaiapp.chat.domain.model.format_response

import kotlinx.serialization.Serializable

@Serializable
data class Fact(
    val label: String,
    val value: String
)