package com.example.myaiapp.chat.domain.model.format_response

import kotlinx.serialization.Serializable

@Serializable
data class StructuredResponse(
    val topic: String,
    val summary: String,
    val facts: List<Fact>
) {
    companion object {
        val EMPTY = StructuredResponse(
            topic = "",
            summary = "",
            facts = emptyList(),
        )
    }
}