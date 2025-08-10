package com.example.myaiapp.chat.presentation.preview_data

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.example.myaiapp.chat.domain.model.Role
import com.example.myaiapp.chat.presentation.state.ChatState
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import com.example.myaiapp.utils.ImmutableList

private val messages = listOf(
    MessageUiModel(
        role = Role.USER,
        content = "Message1",
        isOwnMessage = true,
    ),
    MessageUiModel(
        role = Role.USER,
        content = "Message2",
        isOwnMessage = false,
    ),
    MessageUiModel(
        role = Role.USER,
        content = "Message3",
        isOwnMessage = true,
    )
)

private val chatState = ChatState(
    history = ImmutableList(messages),
)

class ChatStatePreviewParameterProvider : PreviewParameterProvider<ChatState> {
    override val values: Sequence<ChatState>
        get() = sequenceOf(chatState)
}