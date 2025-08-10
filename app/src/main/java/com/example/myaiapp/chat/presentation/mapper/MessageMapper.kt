package com.example.myaiapp.chat.presentation.mapper

import com.example.myaiapp.chat.domain.model.ChatMessage
import com.example.myaiapp.chat.domain.model.Role
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import javax.inject.Inject

class MessageMapper @Inject constructor() {

    fun toMessageUIModel(
        chatMessage: ChatMessage,
    ): MessageUiModel {
        return MessageUiModel(
            role = chatMessage.role,
            content = chatMessage.content,
            isOwnMessage = chatMessage.role == Role.USER,
        )
    }

    fun toChatMessages(
        messagesUIModel: List<MessageUiModel>,
    ): List<ChatMessage> {
        val chatMessage = mutableListOf<ChatMessage>()
        messagesUIModel.map { message ->
            chatMessage.add(
                toChatMessage(
                    message,
                )
            )
        }
        return chatMessage
    }

    private fun toChatMessage(
        messageUiModel: MessageUiModel,
    ): ChatMessage {
        return ChatMessage(
            role = messageUiModel.role,
            content = messageUiModel.content,
        )
    }
}