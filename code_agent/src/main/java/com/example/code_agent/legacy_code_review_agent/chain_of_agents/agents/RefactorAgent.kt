package com.example.code_agent.legacy_code_review_agent.chain_of_agents.agents

import com.example.code_agent.legacy_code_review_agent.LlmClient
import com.example.code_agent.legacy_code_review_agent.LlmDiff
import com.example.code_agent.legacy_code_review_agent.LlmResponse
import com.example.code_agent.legacy_code_review_agent.LlmRewrite
import com.example.code_agent.legacy_code_review_agent.OpenRouterLlmClient
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Agent
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.DiffQuality
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PatchSet
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.PipelineCtx
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.StrategyResult
import java.io.File

/**
 * Agent #3 — Refactorer (substantive, Qwen‑friendly)
 *  - Требуем содержательные изменения (минимум N код‑строк),
 *  - Фильтруем тривиальные диффы (только комментарии/пустые строки/импорты),
 *  - Для qwen‑моделей сначала просим FULL REWRITE, затем DIFF.
 */
class RefactorAgent(
    private val clientFactory: (PipelineCtx) -> LlmClient = { ctx ->
        OpenRouterLlmClient(
            endpoint = ctx.llmEndpoint,
            appTitle = "legacy-chain"
        )
    }
) : Agent<StrategyResult, PatchSet> {

    private val minCodeTouched = 4 // минимум строк кода, которые должны измениться

    override fun run(input: StrategyResult, ctx: PipelineCtx): PatchSet {
        val targets = input.targetFiles.take(ctx.maxFilesForContext)
        if (targets.isEmpty()) return PatchSet()
        val client = clientFactory(ctx)

        val diffs = mutableListOf<LlmDiff>()
        val rewrites = mutableListOf<LlmRewrite>()
        val isQwen = ctx.llmModel.contains("qwen", ignoreCase = true)

        for (path in targets) {
            val f = File(ctx.root, path)
            if (!f.exists()) continue
            val code = f.readText()
            val (lines, notNulls, depth) = quickHeuristics(code)
            val system = systemPrompt()

            fun acceptNonTrivial(dlist: List<LlmDiff>): List<LlmDiff> = dlist.filter { d ->
                val st = DiffQuality.inspectUnified(d.unified_diff)
                val v = DiffQuality.isTrivial(st, minCodeTouched)
                if (v.trivial) {
                    println("[REFACTOR] skip trivial diff for $path => ${v.reason}")
                }
                !v.trivial
            }

            if (isQwen) {
                // Сначала просим FULL REWRITE → если нет, тогда DIFF → форс‑DIFF
                val r2 = safeCall(client, system, buildUserRewrite(path, code, lines, notNulls, depth), ctx)
                if (r2.rewrites.any { it.file == path }) { rewrites += r2.rewrites.filter { it.file == path }; continue }

                val r1 = safeCall(client, system, buildUserDiff(path, code, lines, notNulls, depth), ctx)
                val good1 = acceptNonTrivial(r1.diffs.filter { it.file == path && hasHunks(it.unified_diff) })
                if (good1.isNotEmpty()) { diffs += good1; continue }

                val r3 = safeCall(client, system, buildUserForceSubstantiveDiff(path, code, lines, notNulls, depth), ctx)
                val good3 = acceptNonTrivial(r3.diffs.filter { it.file == path && hasHunks(it.unified_diff) })
                if (good3.isNotEmpty()) { diffs += good3; continue }
            } else {
                val r1 = safeCall(client, system, buildUserDiff(path, code, lines, notNulls, depth), ctx)
                val good1 = acceptNonTrivial(r1.diffs.filter { it.file == path && hasHunks(it.unified_diff) })
                if (good1.isNotEmpty()) { diffs += good1; continue }
                if (r1.rewrites.any { it.file == path }) { rewrites += r1.rewrites.filter { it.file == path }; continue }

                val r2 = safeCall(client, system, buildUserRewrite(path, code, lines, notNulls, depth), ctx)
                if (r2.rewrites.any { it.file == path }) { rewrites += r2.rewrites.filter { it.file == path }; continue }

                val r3 = safeCall(client, system, buildUserForceSubstantiveDiff(path, code, lines, notNulls, depth), ctx)
                val good3 = acceptNonTrivial(r3.diffs.filter { it.file == path && hasHunks(it.unified_diff) })
                if (good3.isNotEmpty()) { diffs += good3; continue }
            }
        }
        return PatchSet(diffs = diffs, rewrites = rewrites)
    }

    private fun safeCall(client: LlmClient, system: String, user: String, ctx: PipelineCtx): LlmResponse = try {
        client.generatePlan(system, user, ctx.llmModel, ctx.llmTemperature, ctx.llmMaxTokens)
    } catch (_: Throwable) { LlmResponse() }

    // --- quick local heuristics to nudge the model ---
    private fun quickHeuristics(code: String): Triple<Int, Int, Int> {
        val lines = code.lines().size
        val notNulls = Regex("!!").findAll(code).count()
        var depth = 0; var maxD = 0
        code.lines().forEach { ln ->
            depth += ln.count { it == '{' }
            if (depth > maxD) maxD = depth
            depth -= ln.count { it == '}' }
        }
        return Triple(lines, notNulls, maxD)
    }

    // ---------- prompts (STRICT JSON, NO code fences) ----------

    private fun systemPrompt(): String = """
        You are a Kotlin refactoring assistant. Produce a minimal but SUBSTANTIVE improvement per file.
        IMPORTANT OUTPUT RULES:
        - Return a STRICT JSON object ONLY. No Markdown, no code fences, no explanations.
        - Prefer returning a unified diff; if impossible, return a full-file rewrite.
    """.trimIndent()

    private fun buildUserDiff(path: String, code: String, lines: Int, notNulls: Int, depth: Int): String = buildString {
        appendLine("FILE: $path (exact content below)")
        appendLine(code)
        appendLine()
        appendLine("Make a SUBSTANTIVE mechanical improvement (keep behavior safe). DO NOT return comment-only/whitespace-only changes.")
        appendLine("File fingerprint: lines=$lines, notNullBangs=$notNulls, maxBlockDepth=$depth")
        appendLine("Choose ONE applicable action (examples):")
        appendLine("- replace '!!' with safe call or requireNotNull with clear message;")
        appendLine("- extract long inner block into a private function; add early-return guard;")
        appendLine("- reduce nesting (invert condition and return early);")
        appendLine("- tighten visibility (public→internal/private) for non-API members;")
        appendLine("- remove dead code or unreachable branches;")
        appendLine("- convert var→val where possible; simplify when/if;")
        appendLine("- unify error handling; add null-safety;")
        appendLine()
        appendLine("Return JSON with at least one diff. NO empty arrays.")
        appendLine("Requirements for the diff:")
        appendLine("- 'file' must be exactly '$path'")
        appendLine("- 'unified_diff' must include headers '--- a/$path' and '+++ b/$path' and at least one '@@' hunk")
        appendLine("- Change at least $minCodeTouched NON-COMMENT code lines in total (added+removed)")
        appendLine("- Context lines MUST be exact substrings from ORIGINAL (no numbering)")
        appendLine("Schema: {\"plan\":[],\"diffs\":[{\"file\":\"$path\",\"unified_diff\":\"...\"}],\"rewrites\":[],\"notes\":\"...\"}")
    }

    private fun buildUserRewrite(path: String, code: String, lines: Int, notNulls: Int, depth: Int): String = buildString {
        appendLine("FILE: $path (exact content below)")
        appendLine(code)
        appendLine()
        appendLine("Provide a FULL REWRITE with a SUBSTANTIVE, safe improvement. Keep behavior.")
        appendLine("Target at least $minCodeTouched code lines changed; do not limit to comments/whitespace/imports.")
        appendLine("Prefer: replace '!!', extract private helper, early-return guards, reduce nesting, tighten visibility.")
        appendLine("Return STRICT JSON only: {\"plan\":[],\"diffs\":[],\"rewrites\":[{\"file\":\"$path\",\"new_content\":\"...\"}],\"notes\":\"...\"}")
    }

    private fun buildUserForceSubstantiveDiff(path: String, code: String, lines: Int, notNulls: Int, depth: Int): String = buildString {
        appendLine("FILE: $path (exact content below)")
        appendLine(code)
        appendLine()
        appendLine("MANDATORY: return a unified diff for '$path' with at least ONE hunk and >= $minCodeTouched code lines changed.")
        appendLine("Disallow trivial edits (comments/whitespace/import-only). Choose a concrete refactor from the list above.")
        appendLine("Schema: {\"plan\":[],\"diffs\":[{\"file\":\"$path\",\"unified_diff\":\"...\"}],\"rewrites\":[],\"notes\":\"...\"}")
    }

    // ---------- utils ----------
    private fun hasHunks(unified: String?): Boolean = unified?.lineSequence()?.any { it.trimStart().startsWith("@@") } == true
}
