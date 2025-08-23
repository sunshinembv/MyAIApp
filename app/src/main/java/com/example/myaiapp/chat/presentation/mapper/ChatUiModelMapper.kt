package com.example.myaiapp.chat.presentation.mapper

import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.chat.data.model.Verify
import com.example.myaiapp.chat.data.ssh.SshDockerExecutor
import com.example.myaiapp.chat.domain.agent_orchestrator.model.OrchestratorResult
import com.example.myaiapp.chat.presentation.ui_model.item.MessageItem
import com.example.myaiapp.chat.presentation.ui_model.item.PrBriefItem
import com.example.myaiapp.chat.presentation.ui_model.item.RunResultItem
import com.example.myaiapp.chat.presentation.ui_model.item.SummaryItem
import com.example.myaiapp.chat.presentation.ui_model.item.VerifyItem
import javax.inject.Inject

class ChatUiModelMapper @Inject constructor() {

    fun toRunResultItem(runResult: SshDockerExecutor.RunResult): RunResultItem {
        return RunResultItem(
            jobId = runResult.jobId,
            exitStatus = runResult.exitStatus,
            output = runResult.output
        )
    }

    fun toPrBriefItem(prBrief: PrBrief): PrBriefItem {
        return PrBriefItem(
            title = prBrief.title,
            number = prBrief.number,
            state = prBrief.state,
            author = prBrief.author,
            createdAt = prBrief.createdAt,
            comments = prBrief.comments,
            additions = prBrief.additions,
            deletions = prBrief.deletions,
        )
    }

    fun toSummaryItem(summary: Summary): SummaryItem {
        return SummaryItem(
            title = summary.title,
            subtitle = summary.subtitle,
            summary = summary.summary,
        )
    }

    fun toVerifyItem(verify: Verify): VerifyItem {
        return VerifyItem(
            mode = verify.mode,
            ok = verify.ok,
            score = verify.score,
            notes = verify.notes,
            missingRequired = verify.missingRequired,
            missingOptional = verify.missingOptional,
        )
    }

    fun fromAskToMessageItem(ask: OrchestratorResult.Ask): MessageItem {
        return MessageItem(
            text = ask.question
        )
    }
}
