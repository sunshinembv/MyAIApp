package com.example.myaiapp.chat.presentation.ui_model

import com.example.myaiapp.chat.domain.model.Role

data class MessageUiModel(
    val role: Role,
    val content: String,
    val isOwnMessage: Boolean,
    val pending: Boolean = false,
)