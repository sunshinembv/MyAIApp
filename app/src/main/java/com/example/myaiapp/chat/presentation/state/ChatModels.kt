package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor.RunResult
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.OutputFormat
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
    val rawHistory: List<OllamaChatMessage> = emptyList(),
) : State

sealed class ChatEvents : Event {
    sealed class Ui : ChatEvents() {
        data class CallLlm(
            val history: ImmutableList<MessageUiModel>,
            val content: String,
            val model: LlmModels,
            val rawHistory: List<OllamaChatMessage>
        ) : Ui()

        data class CallLlmToMCP(
            val history: ImmutableList<MessageUiModel>,
            val content: String,
            val model: LlmModels,
            val rawHistory: List<OllamaChatMessage>
        ) : Ui()

        data class CallLlmToMCPGitHubPr(
            val history: ImmutableList<MessageUiModel>,
            val content: String,
            val model: LlmModels,
            val rawHistory: List<OllamaChatMessage>
        ) : Ui()

        data class CallLlmToDocker(
            val history: ImmutableList<MessageUiModel>,
            val content: String,
            val login: String,
            val key: String,
            val model: LlmModels,
            val rawHistory: List<OllamaChatMessage>
        ) : Ui()

        data class Typing(val text: String) : Ui()
    }

    sealed class Internal : ChatEvents() {
        data class MessageLoaded(val message: String) :
            Internal()

        data class SummeryAndReviewLoaded(val summary: Summary, val verify: Verify): Internal()

        data class MCPResponse(val response: String): Internal()

        data class MCPResponseGitHubPr(val prBrief: List<PrBrief>): Internal()

        data class DockerResponse(val runResult: RunResult): Internal()

        data class ErrorLoading(val error: Throwable) : Internal()
    }
}

sealed class ChatCommand : Command {
    data class CallLlm(
        val history: ImmutableList<MessageUiModel>,
        val content: String,
        val model: LlmModels,
        val rawHistory: List<OllamaChatMessage>
    ) : ChatCommand()

    data class CallLlmToMCP(
        val history: ImmutableList<MessageUiModel>,
        val content: String,
        val model: LlmModels,
        val rawHistory: List<OllamaChatMessage>
    ) : ChatCommand()

    data class CallLlmToMCPGitHubPr(
        val history: ImmutableList<MessageUiModel>,
        val content: String,
        val model: LlmModels,
        val rawHistory: List<OllamaChatMessage>
    ) : ChatCommand()

    data class CallLlmToDocker(
        val history: ImmutableList<MessageUiModel>,
        val content: String,
        val login: String,
        val key: String,
        val model: LlmModels,
        val rawHistory: List<OllamaChatMessage>
    ) : ChatCommand()
}
