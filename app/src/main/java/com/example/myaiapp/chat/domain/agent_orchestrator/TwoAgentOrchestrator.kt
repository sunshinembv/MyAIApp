package com.example.myaiapp.chat.domain.agent_orchestrator

import com.example.myaiapp.chat.data.agents.interview_agent.InterviewAgent
import com.example.myaiapp.chat.data.agents.interview_agent.model.InterviewAgentTurn
import com.example.myaiapp.chat.data.agents.summary_verifier_agent.SummaryVerifierAgent
import com.example.myaiapp.chat.data.db.repository.MessageDaoRepository
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.domain.model.LlmModels
import javax.inject.Inject

class TwoAgentOrchestrator @Inject constructor(
    private val interviewAgent: InterviewAgent,
    private val summaryVerifierAgent: SummaryVerifierAgent,
    private val messageDaoRepository: MessageDaoRepository,
) {
    // История диалога с InterviewAgent
    private val history: MutableList<OllamaChatMessage> = interviewAgent.seedHistory()

    suspend fun getChatHistory(): List<OllamaChatMessage> {
        return messageDaoRepository.getMessages().map { message ->
            val message = OllamaChatMessage(
                role = message.role,
                content = message.content,
            )
            history += message
            message
        }
    }

    suspend fun userSays(text: String, model: LlmModels): OrchestratorResult {
        history += OllamaChatMessage(Role.USER, text)
        messageDaoRepository.addUser(text)

        return when (val turn = interviewAgent.next(history, model)) {
            is InterviewAgentTurn.Ask -> {
                history += turn.rawAssistant
                messageDaoRepository.addAssistant(turn.question)
                OrchestratorResult.Ask(turn.question)
            }
            is InterviewAgentTurn.Summary -> {
                history += turn.rawAssistant
                // отдаём summary во второй агент для проверки
                val verify = summaryVerifierAgent.verify(turn.summaryJson, model)
                OrchestratorResult.SummaryAndReview(
                    summaryJson = turn.summaryJson,
                    summary = turn.summary,
                    verify = verify.dto
                )
            }
        }
    }
}
