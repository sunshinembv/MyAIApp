package com.example.myaiapp.chat.data.agents.interview_agent.model

import com.example.myaiapp.chat.data.model.OllamaChatMessage

sealed class InterviewAgentTurn {
    data class Ask(val question: String, val rawAssistant: OllamaChatMessage) : InterviewAgentTurn()
    data class Summary(val summaryJson: String, val summary: com.example.myaiapp.chat.data.model.Summary, val rawAssistant: OllamaChatMessage) : InterviewAgentTurn()
}