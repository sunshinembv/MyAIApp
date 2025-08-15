package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.agents.mcp.MCPRepository
import com.example.myaiapp.chat.domain.agent_orchestrator.TwoAgentOrchestrator
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.MessageLoaded
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.SummeryAndReviewLoaded
import com.example.myaiapp.core.Actor
import javax.inject.Inject

class ChatActor @Inject constructor(
    private val orchestrator: TwoAgentOrchestrator,
    private val mcpRepository: MCPRepository,
) : Actor<ChatCommand, ChatEvents.Internal> {

    override suspend fun execute(command: ChatCommand, onEvent: (ChatEvents.Internal) -> Unit) {
        when (command) {
            is ChatCommand.CallLlm -> {

                val result = orchestrator.userSays(
                    text = command.content
                )

                when (result) {
                    is OrchestratorResult.Ask -> {
                        onEvent(MessageLoaded(result.question))
                    }
                    is OrchestratorResult.SummaryAndReview -> {
                        onEvent(SummeryAndReviewLoaded(result.summary, result.verify))
                    }
                }
            }

            is ChatCommand.CallLlmToMCP -> {
                val result = mcpRepository.callLlmToMCP(
                    command.content
                )

                onEvent(ChatEvents.Internal.MCPResponse(result))
            }
        }
    }
}