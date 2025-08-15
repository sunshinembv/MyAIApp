package com.example.myaiapp.chat.data

import com.example.myaiapp.chat.data.model.Ask
import com.example.myaiapp.chat.data.model.LlmReply
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.Summary
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

class LlmContentParser @Inject constructor() {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    /** Пробуем распарсить в конкретный DTO; при провале — Text */
    fun parseAs(content: String, rawAssistant: List<OllamaChatMessage>): LlmReply {
        val askAdapter = moshi.adapter(Ask::class.java)
        val summaryAdapter = moshi.adapter(Summary::class.java)

        val json = extractFirstJson(content) ?: error("No JSON found")

        val turn = runCatching { askAdapter.fromJson(json) }.getOrNull()
            ?: runCatching { summaryAdapter.fromJson(json) }.getOrNull()
            ?: error("Unknown JSON shape")

        return when (turn) {
            is Ask -> LlmReply.Text(turn.q, rawAssistant)
            is Summary -> LlmReply.Json(turn, rawAssistant)
            else -> error("Unknown JSON shape")
        }
    }

    // --- helpers ---

    fun extractFirstJson(text: String): String? {
        val endCut = text.indexOf("<<<END>>>").let { if (it >= 0) text.substring(0, it) else text }
        val s = endCut.indexOf('{')
        if (s < 0) return null
        var depth = 0
        for (i in s until endCut.length) {
            when (endCut[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return endCut.substring(s, i + 1)
                }
            }
        }
        return null
    }

    fun choosePreferred(objects: List<String>): String? {
        if (objects.isEmpty()) return null
        // Если среди объектов есть SUMMARY — берём его (модель решила резюмировать)
        objects.firstOrNull { it.contains("\"title\"") && it.contains("\"subtitle\"") && it.contains("\"summary\"") }?.let { return it }
        // Иначе — первый ASK
        return objects.first()
    }

    fun normalizeAssistantContent(raw: String): String? {
        val chosen = choosePreferred(extractJsonObjects(raw)) ?: return null
        return "$chosen<<<END>>>"
    }

    fun extractJsonObjects(raw: String): List<String> {
        val cut = raw.substringBefore("<<<END>>>", raw)
        val out = mutableListOf<String>()
        var i = cut.indexOf('{')
        while (i >= 0 && i < cut.length) {
            var depth = 0
            var j = i
            while (j < cut.length) {
                when (cut[j]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            out += cut.substring(i, j + 1)
                            break
                        }
                    }
                }
                j++
            }
            i = cut.indexOf('{', j + 1)
        }
        return out
    }
}