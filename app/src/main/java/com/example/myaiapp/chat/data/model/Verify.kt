package com.example.myaiapp.chat.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Verify(
    val mode: String,
    val ok: Boolean,
    val score: Double,
    val missingRequired: List<String>? = null,
    val missingOptional: List<String>? = null,
    val notes: String,
) {
    companion object {

        val agentName = "VerifyAgent"
        val EMPTY = Verify(
            mode = "",
            ok = true,
            score = 0.0,
            missingRequired = emptyList(),
            missingOptional = emptyList(),
            notes = "",
        )
    }
}