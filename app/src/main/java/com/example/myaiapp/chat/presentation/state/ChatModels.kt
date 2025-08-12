package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.OutputFormat
import com.example.myaiapp.chat.domain.model.format_response.StructuredResponse
import com.example.myaiapp.chat.presentation.ui_model.MessageUiModel
import com.example.myaiapp.core.Command
import com.example.myaiapp.core.Event
import com.example.myaiapp.core.State
import com.example.myaiapp.utils.ImmutableList

data class ChatState(
    val history: ImmutableList<MessageUiModel> = ImmutableList(emptyList()),
    val model: LlmModels = LlmModels.MISTRAL,
    val loading: Boolean = false,
    val error: String? = null,
    val isEmptyState: Boolean = true,
    val typedText: String? = null,
    val outputFormat: OutputFormat? = null,
) : State

sealed class ChatEvents : Event {
    sealed class Ui : ChatEvents() {
        data class CallLlm(
            val history: ImmutableList<MessageUiModel>,
            val content: String,
            val model: LlmModels,
        ) : Ui()

        data class Typing(val text: String) : Ui()
    }

    sealed class Internal : ChatEvents() {

        data class Parsed(val response: StructuredResponse) :
            Internal()

        data class ErrorLoading(val error: Throwable) : Internal()
    }
}

sealed class ChatCommand : Command {
    data class CallLlm(
        val history: ImmutableList<MessageUiModel>,
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()
}
