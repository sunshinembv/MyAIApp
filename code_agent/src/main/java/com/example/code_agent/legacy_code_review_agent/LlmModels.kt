package com.example.code_agent.legacy_code_review_agent

/**
 * Data models for the LLM output + helper to robustly extract JSON
 * from a possibly noisy LLM response.
 */

data class LlmPlanItem(
    val severity: String,
    val title: String,
    val rationale: String,
    val files: List<String> = emptyList()
)

/** Unified-diff entry returned by the model. */
data class LlmDiff(
    val file: String,
    val unified_diff: String
)

/**
 * Fallback full-file rewrite. If the model cannot produce a precise diff,
 * it may return the complete new content — we will convert it to a diff locally.
 */
data class LlmRewrite(
    val file: String,
    val new_content: String
)

/** Top-level structured response from the model. */
data class LlmResponse(
    val plan: List<LlmPlanItem> = emptyList(),
    val diffs: List<LlmDiff> = emptyList(),
    val rewrites: List<LlmRewrite> = emptyList(),
    val notes: String? = null
)

/**
 * Extract a single top-level JSON object from a possibly messy LLM output.
 * Supports fenced blocks (```json ... ```) and falls back to scanning
 * for the first balanced { ... } object.
 */
fun extractJsonObject(text: String): String {
    val cleaned = text
        .replace("```json", "```")
        .replace("```JSON", "```")
        .replace("```Json", "```")
        .trim()

    if (cleaned.startsWith("```") && cleaned.endsWith("```")) {
        return cleaned.removePrefix("```").removeSuffix("```").trim()
    }

    val start = cleaned.indexOf('{')
    require(start >= 0) { "No JSON object found in LLM output" }
    var depth = 0
    var end = -1
    for (i in start until cleaned.length) {
        when (cleaned[i]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) { end = i; break }
            }
        }
    }
    require(end > start) { "Unbalanced JSON braces in LLM output" }
    return cleaned.substring(start, end + 1)
}