package com.example.code_agent

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object LlmCodeFixer {
    private val codeFenceRegex =
        Regex("```(?:kotlin)?\\s*([\\s\\S]*?)\\s*```", setOf(RegexOption.MULTILINE))

    suspend fun fixFileWithLlm(
        api: OpenRouterApi,
        model: String,
        filePath: String,
        taskNote: String
    ): String {
        val src = java.io.File(filePath).readText()

        val systemPrompt = """
            You are a senior Android/Kotlin engineer.
            Goal: fix a real bug OR perform a safe micro-refactor with clear benefit.
            Constraints:
            - Return the **ENTIRE corrected file only** inside a ```kotlin``` block.
            - No explanations, no diffs, no extra text.
            - Keep package/imports and structure. No new deps or CI changes.
            - Keep style as-is (no mass reformatting/renaming).
        """.trimIndent()

        val userPrompt = buildString {
            appendLine("Task note: $taskNote")
            appendLine()
            appendLine("Analyze and fix this FULL file. Return only the entire corrected file wrapped in ```kotlin```:")
            appendLine()
            appendLine("```kotlin")
            appendLine(src)
            appendLine("```")
        }

        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", systemPrompt),
                ChatMessage("user", userPrompt)
            ),
            temperature = 0.2,
            stream = false
        )

        val resp = api.chatCompletions(request)
        val content = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()

        val code =
            codeFenceRegex.find(content)?.groups?.get(1)?.value?.trim()
                ?: Regex("```\\s*([\\s\\S]*?)\\s*```", RegexOption.MULTILINE)
                    .find(content)?.groups?.get(1)?.value?.trim()
                ?: content

        require(code.isNotBlank()) { "LLM returned empty code; aborting to avoid overwriting with empty content." }

        // перезаписываем файл
        Files.write(Paths.get(filePath), code.toByteArray(StandardCharsets.UTF_8))
        return code
    }
}