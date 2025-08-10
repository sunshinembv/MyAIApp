package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.presentation.mapper.MessageMapper
import com.example.myaiapp.core.Actor
import javax.inject.Inject

class ChatActor @Inject constructor(
    private val repository: OllamaRepository,
    private val mapper: MessageMapper,
) : Actor<ChatCommand, ChatEvents.Internal> {

    override suspend fun execute(command: ChatCommand, onEvent: (ChatEvents.Internal) -> Unit) {
        try {
            when (command) {

                is ChatCommand.CallLlm -> {
                    val answer = repository.chatOnce(
                        model = command.model,
                        content = command.content,
                        history = mapper.toChatMessages(command.history.list),
                    )
                    val messagesUIModel = mapper.toMessageUIModel(answer)
                    onEvent(ChatEvents.Internal.MessageLoaded(messagesUIModel))
                }
            }
        } catch (t: Throwable) {
            onEvent(ChatEvents.Internal.ErrorLoading(t))
        }
    }
}