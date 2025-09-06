package com.example.myaiapp.chat.presentation.state

import com.example.myaiapp.chat.data.agents.docker.DockerRepository
import com.example.myaiapp.chat.data.agents.mcp.MCPRepository
import com.example.myaiapp.chat.data.git_hub.ReleaseOpsRepository
import com.example.myaiapp.chat.data.llm_policy.UserRoleHolder
import com.example.myaiapp.chat.domain.agent_orchestrator.TwoAgentOrchestrator
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.domain.model.UserRole
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.chat.domain.repository.PersonalOllamaRepository
import com.example.myaiapp.chat.domain.repository.SecuredOllamaRepository
import com.example.myaiapp.chat.domain.use_cases.ReasoningUseCase
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.AskLoaded
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.ChatHistoryLoaded
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.DockerResponse
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.MCPResponse
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.MCPResponseGitHubPr
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.MessageLoaded
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.ReasoningTurnLoaded
import com.example.myaiapp.chat.presentation.state.ChatEvents.Internal.SummeryAndReviewLoaded
import com.example.myaiapp.core.Actor
import com.example.myaiapp.memory.ImportExportConfig
import com.example.myaiapp.memory.data.repository.PersonalizationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ChatActor @Inject constructor(
    private val orchestrator: TwoAgentOrchestrator,
    private val mcpRepository: MCPRepository,
    private val dockerRepository: DockerRepository,
    private val releaseOpsRepository: ReleaseOpsRepository,
    private val ollamaRepository: OllamaRepository,
    private val reasoningUseCase: ReasoningUseCase,
    private val securedOllamaRepository: SecuredOllamaRepository,
    private val personalOllamaRepository: PersonalOllamaRepository,
    private val personalizationRepository: PersonalizationRepository,
    private val importExportConfig: ImportExportConfig,
) : Actor<ChatCommand, ChatEvents.Internal> {

    override suspend fun execute(command: ChatCommand, onEvent: (ChatEvents.Internal) -> Unit) {
        when (command) {
            is ChatCommand.CallLlmToOrchestrator -> {

                val result = orchestrator.userSays(
                    text = command.content,
                    model = command.model,
                )

                when (result) {
                    is OrchestratorResult.Ask -> {
                        onEvent(AskLoaded(result))
                    }

                    is OrchestratorResult.SummaryAndReview -> {
                        onEvent(SummeryAndReviewLoaded(result.summary, result.verify))
                    }
                }
            }

            is ChatCommand.CallLlmToMCP -> {
                val result = mcpRepository.callLlmToMCP(
                    content = command.content,
                    model = command.model,
                )

                onEvent(MCPResponse(result))
            }

            is ChatCommand.CallLlmToMCPGitHubPr -> {
                val result = mcpRepository.callLlmToMCPGitHubPr(
                    content = command.content,
                    model = command.model,
                )

                //mcpRepository.callLlmToMCPPrReport(command.content)

                onEvent(MCPResponseGitHubPr(result))
            }

            is ChatCommand.CallLlmToDocker -> {
                /*val result = dockerRepository.callLlmToDocker(
                    command.content,
                    command.login,
                    command.key,
                )*/
                val result = dockerRepository.callLlmToDockerTest(
                    content = command.content,
                    model = command.model,
                )

                onEvent(DockerResponse(result))
            }

            is ChatCommand.CallLlmToReleaseApk -> {
                val result = releaseOpsRepository.runCommand(
                    content = command.content,
                    model = command.model,
                )

                onEvent(MessageLoaded(result))
            }

            is ChatCommand.CallLlm -> {
                if (UserRoleHolder.role == UserRole.ADMIN) {
                    val result = ollamaRepository.chat(
                        content = command.content,
                        model = command.model,
                    )
                    onEvent(MessageLoaded(result))
                } else {
                    /*securedOllamaRepository.chat(
                        content = command.content,
                        model = command.model,
                    ).onSuccess {
                        onEvent(MessageLoaded(it))
                    }.onFailure {
                        onEvent(LimitExceeded(it.message.orEmpty()))
                    }*/
                    val result = personalOllamaRepository.chat(
                        content = command.content,
                        model = command.model,
                    )
                    onEvent(MessageLoaded(result))
                }
            }

            ChatCommand.GetHistoryFromCache -> {
                val result = orchestrator.getChatHistory()
                onEvent(ChatHistoryLoaded(result))
            }

            is ChatCommand.CallLlmWithReasoningMode -> {
                val result = reasoningUseCase.run(
                    content = command.content,
                    model = command.model,
                )

                onEvent(ReasoningTurnLoaded(result))
            }

            is ChatCommand.PersonalCommand.AddFact -> {
                personalizationRepository.addMemory(command.text, command.importance)
            }

            ChatCommand.PersonalCommand.Export -> {
                importExportConfig.exportConfig(personalizationRepository)
            }

            is ChatCommand.PersonalCommand.Import -> {
                importExportConfig.importConfig(personalizationRepository, command.json)
            }

            ChatCommand.PersonalCommand.LoadPersonalState -> {
                val profile = personalizationRepository.profileFlow.first()
                val prefs = personalizationRepository.prefsFlow.first()
                val memories = personalizationRepository.selectContextMemories()
                onEvent(
                    ChatEvents.Internal.PersonalInternalEvent.ProfileStateLoaded(
                        profile,
                        prefs,
                        memories
                    )
                )
            }

            is ChatCommand.PersonalCommand.SaveProfile -> {
                personalizationRepository.updateProfile {
                    name = command.name
                    city = command.city
                    locale = command.locale
                    timezone = command.tz
                }
            }

            is ChatCommand.PersonalCommand.UpdatePrefs -> {
                personalizationRepository.updatePrefs(command.block)
            }
        }
    }
}
