package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor.RunResult
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.chat.presentation.ui_model.item.UiItem
import com.example.myaiapp.chat.voice.VoiceState
import com.example.myaiapp.core.Command
import com.example.myaiapp.core.Event
import com.example.myaiapp.core.State
import com.example.myaiapp.utils.ImmutableList

data class ChatState(
    val history: ImmutableList<UiItem> = ImmutableList(emptyList()),
    val voiceState: VoiceState = VoiceState.Idle,
    val model: LlmModels = LlmModels.MISTRAL,
    val error: String? = null,
    val isEmptyState: Boolean = true,
    val typedText: String? = null,
    val isPending: Boolean = false,
    val responseType: ResponseType = ResponseType.JSON,
) : State

sealed class ChatEvents : Event {
    sealed class Ui : ChatEvents() {
        data class CallLlm(
            val content: String,
            val model: LlmModels,
            val responseType: ResponseType,
        ) : Ui()

        data object GetHistoryFromCache : Ui()

        data class Typing(val text: String) : Ui()
    }

    sealed class Internal : ChatEvents() {
        data class MessageLoaded(val message: String) :
            Internal()

        data class AskLoaded(val ask: OrchestratorResult.Ask) :
            Internal()

        data class SummeryAndReviewLoaded(val summary: Summary, val verify: Verify): Internal()

        data class MCPResponse(val response: String): Internal()

        data class MCPResponseGitHubPr(val prBrief: List<PrBrief>): Internal()

        data class DockerResponse(val runResult: RunResult): Internal()
        data class ChatHistoryLoaded(val history: List<OllamaChatMessage>): Internal()

        data class ErrorLoading(val error: Throwable) : Internal()
    }

    sealed class VoiceEvent: ChatEvents() {
        data object ListeningStarted : VoiceEvent()
        data class PartialUpdated(val text: String) : VoiceEvent()
        data class FinalRecognized(val query: String) : VoiceEvent()
        data class Speaking(val chunk: String) : VoiceEvent()
        data class Failed(val msg: String) : VoiceEvent()
        data object Idle : VoiceEvent()
    }
}

sealed class ChatCommand : Command {
    data class CallLlm(
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()

    data class CallLlmToOrchestrator(
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

    data class CallLlmToReleaseApk(
        val content: String,
        val model: LlmModels,
    ) : ChatCommand()

    data object GetHistoryFromCache: ChatCommand()
}
