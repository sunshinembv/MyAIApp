package com.example.myaiapp.chat.data.mapper

import com.example.myaiapp.chat.data.model.ChatMessageData
import com.example.myaiapp.chat.data.model.ChatRequestData
import com.example.myaiapp.chat.data.model.OllamaMessageData
import com.example.myaiapp.chat.data.model.RoleData
import com.example.myaiapp.chat.domain.model.ChatMessage
import com.example.myaiapp.chat.domain.model.ChatRequest
import com.example.myaiapp.chat.domain.model.Role
import javax.inject.Inject

class ChatMessagesMapper @Inject constructor() {

    fun toChatRequestData(chatRequest: ChatRequest): ChatRequestData {
        return ChatRequestData(
            model = chatRequest.model,
            messages = toMessagesData(chatRequest.messages),
        )
    }

    fun toChatMessage(ollamaMessageData: OllamaMessageData): ChatMessage {
        val role = when (ollamaMessageData.role) {
            RoleData.USER -> Role.USER
            RoleData.ASSISTANT -> Role.ASSISTANT
            RoleData.SYSTEM -> Role.SYSTEM
            null -> error("Wrong role")
        }

        return ChatMessage(
            role = role,
            content = ollamaMessageData.content.orEmpty(),
        )
    }

    private fun toMessagesData(messages: List<ChatMessage>): List<ChatMessageData> {
        return messages.map { messageData ->
            toChatMessage(messageData)
        }
    }

    private fun toChatMessage(chatMessage: ChatMessage): ChatMessageData {
        val role = when (chatMessage.role) {
            Role.USER -> RoleData.USER
            Role.ASSISTANT -> RoleData.ASSISTANT
            Role.SYSTEM -> RoleData.SYSTEM
        }
        return ChatMessageData(
            role = role,
            content = chatMessage.content,
        )
    }
}
