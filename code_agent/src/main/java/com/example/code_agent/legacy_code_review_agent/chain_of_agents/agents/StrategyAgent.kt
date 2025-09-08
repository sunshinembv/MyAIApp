package com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents

import com.example.code_agent.legacy_code_review_agent.LlmClient
import com.example.code_agent.legacy_code_review_agent.LlmPlanItem
import com.example.code_agent.legacy_code_review_agent.LlmResponse
import com.example.code_agent.legacy_code_review_agent.OpenRouterLlmClient
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Agent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.FileSummary
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PipelineCtx
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ReviewReport
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.ScanResult
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.StrategyResult
import java.util.Properties

/**
 * Agent #2 — Strategist
 * Принимает ScanResult и просит LLM составить "первую связную порцию" работ:
 * приоритезированные пункты плана + список целевых файлов (до 3–5 штук),
 * без генерации диффов на этом этапе.
 */
class StrategyAgent(
    private val clientFactory: (PipelineCtx) -> LlmClient = { ctx ->
        OpenRouterLlmClient(
            endpoint = ctx.llmEndpoint,
            appTitle = "legacy-chain"
        )
    }
) : Agent<ScanResult, StrategyResult> {

    override fun run(input: ScanResult, ctx: PipelineCtx): StrategyResult {
        val report = input.report
        val system = systemPrompt()
        val user = buildUser(report, ctx)
        val client = clientFactory(ctx)

        val resp: LlmResponse = try {
            client.generatePlan(system, user, ctx.llmModel, ctx.llmTemperature, ctx.llmMaxTokens)
        } catch (t: Throwable) {
            // LLM недоступна — локальный фолбэк
            return fallbackStrategy(report, ctx, note = "LLM error: ${t.message}")
        }

        // Нормальный путь: берём план и целевые файлы из ответа модели
        if (resp.plan.isNotEmpty()) {
            val targets = resp.plan.flatMap { it.files }.distinct()
            return StrategyResult(plan = resp.plan, targetFiles = targets, notes = resp.notes)
        }

        // Фолбэк: LLM вернула пустой план → выберем топ-файлы локально
        return fallbackStrategy(report, ctx, note = resp.notes ?: "Empty LLM plan")
    }

    private fun systemPrompt(): String = """
        You are a Principal Kotlin/Android engineer.
        Task: From the given project scan, choose a SMALL, SAFE first batch (3–5 files)
        that will yield the highest readability/maintainability gain with minimal risk.
        Focus on:
        - very long functions; deep nesting; many `!!` usages; duplicated code clusters;
        - low-risk dependency modernizations (version catalog, BOM, `implementation` vs `api`).

        DO NOT output code or diffs at this stage.
        OUTPUT STRICTLY the JSON schema the user provides.
    """.trimIndent()

    private fun buildUser(r: ReviewReport, ctx: PipelineCtx): String = buildString {
        appendLine("PROJECT ROOT: ${r.root}")
        appendLine("FILES: ${r.files.size}, DUPLICATES: ${r.duplicates.size}, DEPS: ${r.dependencies.size}")
        appendLine()
        appendLine("HOT FILES (top 10 by max function length):")
        r.files.sortedByDescending { it.maxFunctionLen }.take(10).forEach { f ->
            appendLine("- ${f.path} | lines=${f.lines}, maxFn=${f.maxFunctionLen}, depth=${f.nestedBlocksMaxDepth}, bangs=${f.notNullBangCount}")
        }
        appendLine()
        appendLine("DUPLICATE CLUSTERS (top 5):")
        r.duplicates.take(5).forEach { d ->
            appendLine("- ${d.representative}")
            d.occurrences.take(2).forEach { o -> appendLine("  * ${o.file}:${o.lineFrom}…${o.lineTo}") }
        }
        appendLine()
        appendLine("DEPENDENCIES (sample up to 20):")
        r.dependencies.take(20).forEach { d -> appendLine("- ${d.notation} [${d.where}]") }
        appendLine()
        appendLine("Constraints:")
        appendLine("- Propose at most ${ctx.maxFilesForContext.coerceIn(1,5)} target files.")
        appendLine("- Prefer cohesive changes within the same module when possible.")
        appendLine("- No diffs or code changes now — only planning.")
        appendLine()
        appendLine(
            "Return JSON only (no prose):\n" +
                    "```json\n{" +
                    "\n  \"plan\": [ { \"severity\": \"high|med|low\", \"title\": \"...\", \"rationale\": \"...\", \"files\": [\"path\"] } ]," +
                    "\n  \"diffs\": []," +
                    "\n  \"rewrites\": []," +
                    "\n  \"notes\": \"...\"\n}" +
                    "\n```"
        )
    }

    private fun fallbackStrategy(r: ReviewReport, ctx: PipelineCtx, note: String): StrategyResult {
        val targets = r.files
            .sortedWith(compareByDescending<FileSummary> { it.maxFunctionLen }
                .thenByDescending { it.nestedBlocksMaxDepth }
                .thenByDescending { it.notNullBangCount })
            .map { it.path }
            .distinct()
            .take(ctx.maxFilesForContext.coerceAtLeast(1))
        val plan = listOf(
            LlmPlanItem(
                severity = if (r.duplicates.isNotEmpty() || (r.files.maxOfOrNull { it.maxFunctionLen }
                        ?: 0) >= 200) "high" else "med",
                title = "Auto-initial pass: simplify long/complex functions and remove obvious duplication",
                rationale = "Fallback when LLM plan is empty or error. Selected top files by max function length, nesting, and '!!' count.",
                files = targets
            )
        )
        return StrategyResult(plan = plan, targetFiles = targets, notes = note)
    }
}
