package com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents

import com.example.code_agent.legacy_code_review_agent.ApplyResult
import com.example.code_agent.legacy_code_review_agent.LlmDiff
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Agent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ApplySummary
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PatchSet
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PipelineCtx
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.SmartPatchApply
import com.example.code_agent.legacy_code_review_agent.diffFromNewContent

class ApplyAgent : Agent<PatchSet, ApplySummary> {
    override fun run(input: PatchSet, ctx: PipelineCtx): ApplySummary {
        val results = mutableListOf<ApplyResult>()

        // 1) Сначала применяем DIFF-ы
        for (d in input.diffs) {
            val r = tryApply(ctx, d)
            results += r
        }

        // 2) Затем REWRITE → конвертируем в diff и применяем
        for (rw in input.rewrites) {
            val d: LlmDiff = diffFromNewContent(ctx.root, rw.file, rw.new_content)
            val r = tryApply(ctx, d)
            results += r
        }

        val ok = results.count { it.ok }
        val fail = results.size - ok
        return ApplySummary(results = results, ok = ok, fail = fail)
    }

    private fun tryApply(ctx: PipelineCtx, d: LlmDiff): ApplyResult {
        // Попробуем умное применение (с нормализацией и whitespace‑fallback)
        val smart = SmartPatchApply.applyUnifiedDiffSmart(ctx.root, d.file, d.unified_diff)
        if (smart.ok) return smart
        // Сообщим подробности и вернём как FAIL
        return smart
    }
}
