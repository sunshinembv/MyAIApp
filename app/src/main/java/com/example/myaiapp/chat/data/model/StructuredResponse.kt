package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StructuredResponse(
    val title: String,
    val subtitle: String,
    val summary: String,
) {
    companion object {
        val EMPTY = StructuredResponse(
            title = "",
            subtitle = "",
            summary = "",
        )
    }
}