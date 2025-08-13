package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.model.LlmReply
import com.example.myaiapp.chat.data.model.StructuredResponse
import com.example.myaiapp.chat.data.repository.OllamaRepositoryImpl
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.model.OutputFormat
import com.example.myaiapp.core.Actor
import javax.inject.Inject

class ChatActor @Inject constructor(
    private val repository: OllamaRepositoryImpl,
    private val promptBuilder: PromptBuilder,
) : Actor<ChatCommand, ChatEvents.Internal> {

    override suspend fun execute(command: ChatCommand, onEvent: (ChatEvents.Internal) -> Unit) {
        when (command) {
            is ChatCommand.CallLlm -> {
                val systemPrompt = promptBuilder.systemPrompt(OutputFormat.JSON)

                val result = repository.chatOnce(
                    model = command.model.modelName,
                    systemPrompt = systemPrompt,
                    content = command.content,
                    history = command.rawHistory,
                )

                when (result) {
                    is LlmReply.Json -> {
                        onEvent(ChatEvents.Internal.Parsed(result.value as StructuredResponse, result.rawAssistant))
                    }
                    is LlmReply.Text -> {
                        onEvent(ChatEvents.Internal.MessageLoaded(result.text, result.rawAssistant))
                    }
                }
            }
        }
    }
}