package com.example.myaiapp.chat.domain.use_cases

import com.example.myaiapp.chat.data.json_extractor.JsonExtractor
import com.example.myaiapp.chat.data.model.ThinkJson
import com.example.myaiapp.chat.data.model.VerifyJson
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ReasoningTurn
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

class ReasoningUseCase @Inject constructor(
    private val ollamaRepository: OllamaRepository,
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val extractor = JsonExtractor(moshi)

    suspend fun run(content: String, model: LlmModels): ReasoningTurn {
        val thinkRaw = ollamaRepository.complete(SYSTEM_THINK, buildThinkUser(content), model)
        val think = extractor.parseFirstJson(thinkRaw, ThinkJson::class.java)

        val verifyRaw = ollamaRepository.complete(SYSTEM_VERIFY, buildVerifyUser(content, think), model)
        val verify = extractor.parseFirstJson(verifyRaw, VerifyJson::class.java)

        val final = verify.safeFinal?.takeIf { it.isNotBlank() } ?: think.answerDraft

        return ReasoningTurn(
            question = content,
            think = think,
            verify = verify,
            finalAnswer = final
        )
    }

    private fun buildThinkUser(q: String) =
        "Вопрос: \"\"\"$q\"\"\"\nСформируй JSON по правилам из сообщения системы."

    private fun buildVerifyUser(q: String, t: ThinkJson): String {
        val adapter = moshi.adapter(ThinkJson::class.java)
        val tj = adapter.toJson(t)
        return "Вопрос: \"\"\"$q\"\"\"\nКандидат и тезисы (JSON): \"\"\"$tj\"\"\"\nВерни JSON по правилам из сообщения системы."
    }
}

private val SYSTEM_THINK = """
            Ты аккуратный помощник. Думай, но не раскрывай длинные пошаговые рассуждения.
            Верни СТРОГИЙ JSON строго по схеме:

            {
              "think_bullets": ["<краткий пункт 1>", "..."],
              "answer_draft": "<краткий ответ-план>",
              "claims": ["<атомарный факт 1>", "..."],
              "confidence": 0-100
            }

            Правила:
            - Только JSON. Без маркдауна, без прозаических вступлений.
            - "think_bullets" 2–5 пунктов, очень короткие (план/набросок).
            - "claims" 1–5, атомарные и проверяемые.
            - Отвечай на русском.
        """.trimIndent()
private val SYSTEM_VERIFY = """
            Ты строгий проверяющий. Проверяй только внутреннюю согласованность ответа и утверждений
            (без внешних источников). Верни СТРОГИЙ JSON:

            {
              "checked": [{"claim":"...", "passed":true/false, "note":"краткая причина"}],
              "score": 0-100,
              "safe_final": "<опциональная более безопасная формулировка или null>"
            }

            Правила:
            - Только JSON.
            - Понижай score, если важные claims неубедительны.
            - Отвечай на русском.
        """.trimIndent()