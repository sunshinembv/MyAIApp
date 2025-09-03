package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyItem(
    val claim: String,
    val passed: Boolean,
    val note: String? = null
)