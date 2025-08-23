package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor.RunResult
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.chat.presentation.ui_model.item.UiItem
import com.example.myaiapp.core.Command
import com.example.myaiapp.core.Event
import com.example.myaiapp.core.State
import com.example.myaiapp.utils.ImmutableList

data class ChatState(
    val history: ImmutableList<UiItem> = ImmutableList(emptyList()),
    val model: LlmModels = LlmModels.MISTRAL,
    val error: String? = null,
    val isEmptyState: Boolean = true,
    val typedText: String? = null,
    val isPending: Boolean = false,
    val responseType: ResponseType = ResponseType.DOCKER_KOTLIN_TEST,
) : State

sealed class ChatEvents : Event {
    sealed class Ui : ChatEvents() {
        data class CallLlm(
            val content: String,
            val model: LlmModels,
            val responseType: ResponseType,
        ) : Ui()

        data class Typing(val text: String) : Ui()
    }

    sealed class Internal : ChatEvents() {
        /*data class MessageLoaded(val message: String) :
            Internal()*/

        data class AskLoaded(val ask: OrchestratorResult.Ask) :
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
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()

    data class CallLlmToMCP(
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()

    data class CallLlmToMCPGitHubPr(
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()

    data class CallLlmToDocker(
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()
}
