package com.example.myaiapp.chat.presentation.ui_model

import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.domain.model.format_response.StructuredResponse

data class MessageUiModel(
    val role: Role,
    val content: String? = null,
    val response: StructuredResponse? = null,
    val isOwnMessage: Boolean = true,
    val pending: Boolean = false,
)