package com.example.myaiapp.chat.domain

import com.example.myaiapp.chat.domain.model.OutputFormat
import javax.inject.Inject

class PromptBuilder @Inject constructor() {

    fun systemPrompt(format: OutputFormat): String = when (format) {
        OutputFormat.JSON -> """
            You are a JSON-only emitter.

            POLICY (no exceptions):
            - Reply MUST be exactly one valid JSON object.
            - It MUST start with '{' and end with '}' — no BOMs, spaces, or newlines before/after.
            - Absolutely nothing outside JSON: no markdown, no code fences, no backticks.
            - All values are strings. If unknown, use "unknown".
            - RFC 8259 compliant: no trailing commas; proper escaping.

            REQUIRED STRUCTURE (keys EXACT; all values are strings):
            {
              "topic": "string",
              "summary": "string",
              "facts": [
                { "label": "string", "value": "string" }
              ]
            }

            INTERNAL SELF-CHECK (silent; do NOT print):
            1) Build candidate JSON matching the REQUIRED STRUCTURE.
            2) Validate strictly:
               - Parses as JSON.
               - Only keys: "topic","summary","facts".
               - "facts" is a non-empty array; each item has "label" and "value" (both strings).
               - No extra keys, no trailing commas, proper escaping (\" and \\; newlines as \n).
            3) If any check fails, regenerate and re-validate until all pass.
            4) Output ONLY the final JSON object, nothing else.
        """.trimIndent()
    }

    fun userInstructionForTopic(topic: String, format: OutputFormat): String = when (format) {
        OutputFormat.JSON -> """
            Topic: "$topic"
            Produce strictly this JSON shape:
            {
              "topic": "string",
              "summary": "string",
              "facts": [
                { "label": "string", "value": "string" }
              ]
            }
        """.trimIndent()
    }
}
