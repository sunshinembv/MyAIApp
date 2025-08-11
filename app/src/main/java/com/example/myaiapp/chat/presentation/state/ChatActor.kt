package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.repository.OllamaRepositoryImpl
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.StructuredParsers
import com.example.myaiapp.chat.domain.model.OutputFormat
import com.example.myaiapp.chat.presentation.mapper.MessageMapper
import com.example.myaiapp.core.Actor
import javax.inject.Inject

class ChatActor @Inject constructor(
    private val repository: OllamaRepositoryImpl,
    private val mapper: MessageMapper,
    private val promptBuilder: PromptBuilder,
    private val parsers: StructuredParsers,
) : Actor<ChatCommand, ChatEvents.Internal> {

    override suspend fun execute(command: ChatCommand, onEvent: (ChatEvents.Internal) -> Unit) {
        try {
            when (command) {
                is ChatCommand.CallLlm -> {
                    val systemPrompt = promptBuilder.systemPrompt(OutputFormat.JSON)
                    val userPrompt = promptBuilder.userInstructionForTopic(command.content, OutputFormat.JSON)

                    val result = repository.chatOnce(
                        model = command.model.modelName,
                        systemPrompt = systemPrompt,
                        content = userPrompt,
                        history = mapper.toChatMessages(command.history.list),
                    )

                    val structured = when (OutputFormat.JSON) {
                        OutputFormat.JSON -> parsers.fromJson(result.message.content)
                    }
                    onEvent(ChatEvents.Internal.Parsed(structured))
                }
            }
        } catch (t: Throwable) {
            onEvent(ChatEvents.Internal.ErrorLoading(t))
        }
    }
}