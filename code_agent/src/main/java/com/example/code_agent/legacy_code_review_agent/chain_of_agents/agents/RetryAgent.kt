package com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents

import com.example.code_agent.legacy_code_review_agent.LlmClient
import com.example.code_agent.legacy_code_review_agent.LlmResponse
import com.example.code_agent.legacy_code_review_agent.LlmRewrite
import com.example.code_agent.legacy_code_review_agent.OpenRouterLlmClient
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Agent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PatchSet
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PipelineCtx
import java.io.File
import java.util.Properties

/**
 * RetryAgent v3 — заточен под модели, которые плохо возвращают unified diff.
 * Делает ОДИН запрос на КАЖДЫЙ файл и требует ТОЛЬКО FULL REWRITE.
 * Выход — STRICT JSON без Markdown/кодовых блоков.
 */
class RetryAgent(
    private val clientFactory: (PipelineCtx) -> LlmClient = { ctx ->
        OpenRouterLlmClient(
            endpoint = ctx.llmEndpoint,
            appTitle = "legacy-chain"
        )
    }
) : Agent<RetryAgent.Input, PatchSet> {

    data class Input(val failedPaths: List<String>)

    override fun run(input: Input, ctx: PipelineCtx): PatchSet {
        val client = clientFactory(ctx)
        val rewrites = mutableListOf<LlmRewrite>()

        for (path in input.failedPaths) {
            val f = File(ctx.root, path)
            if (!f.exists()) continue
            val code = f.readText()

            val system = """
                You are a precise Kotlin file rewriter.
                OUTPUT RULES:
                - Return a SINGLE JSON object ONLY. No Markdown, no code fences, no explanations.
                - Put the COMPLETE new file content into 'rewrites' for the provided path.
                - Keep changes minimal and safe; compile-ready Kotlin.
            """.trimIndent()

            val user = buildUser(path, code)
            val r: LlmResponse = try { client.generatePlan(system, user, ctx.llmModel, ctx.llmTemperature, ctx.llmMaxTokens) } catch (_: Throwable) { LlmResponse() }
            val rw = r.rewrites.firstOrNull { it.file == path }
            if (rw != null) rewrites += rw
        }

        return PatchSet(diffs = emptyList(), rewrites = rewrites)
    }

    private fun buildUser(path: String, code: String): String = buildString {
        appendLine("FILE: $path (exact original below)")
        appendLine(code)
        appendLine()
        appendLine("Return EXACT JSON ONLY (no code fences): {\"plan\":[],\"diffs\":[],\"rewrites\":[{\"file\":\"$path\",\"new_content\":\"...\"}],\"notes\":\"...\"}")
    }
}
