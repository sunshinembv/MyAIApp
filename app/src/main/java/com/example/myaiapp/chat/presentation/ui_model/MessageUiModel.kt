package com.example.myaiapp.chat.presentation.ui_model

import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify

data class MessageUiModel(
    val role: Role? = null,
    val agentName: String? = null,
    val content: String? = null,
    val response: Summary? = null,
    val verify: Verify? = null,
    val isOwnMessage: Boolean = true,
    val pending: Boolean = false,
)