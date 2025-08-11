package com.example.myaiapp.chat.presentation.mapper

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import javax.inject.Inject

class MessageMapper @Inject constructor() {

    fun toChatMessages(
        messagesUIModel: List<MessageUiModel>,
    ): List<OllamaChatMessage> {
        val chatMessage = mutableListOf<OllamaChatMessage>()
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
    ): OllamaChatMessage {
        return OllamaChatMessage(
            role = messageUiModel.role,
            content = messageUiModel.content.orEmpty(),
        )
    }
}