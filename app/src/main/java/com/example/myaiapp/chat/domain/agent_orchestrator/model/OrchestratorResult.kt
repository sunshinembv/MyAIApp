package com.example.myaiapp.chat.domain.agent_orchestrator.model

import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify

sealed class OrchestratorResult {
    data class Ask(val question: String) : OrchestratorResult()
    data class SummaryAndReview(val summaryJson: String, val summary: Summary, val verify: Verify) : OrchestratorResult()
}