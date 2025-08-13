package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Summary(
    val title: String,
    val subtitle: String,
    val summary: String,
) {
    companion object Companion {

        val agentName = "InterviewAgent"
        val EMPTY = Summary(
            title = "",
            subtitle = "",
            summary = "",
        )
    }
}