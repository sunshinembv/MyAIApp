package com.example.myaiapp.chat.data.agents.summary_verifier_agent

import com.example.myaiapp.chat.data.LlmContentParser
import com.example.myaiapp.chat.data.agents.summary_verifier_agent.model.Verification
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.model.Verify
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

class SummaryVerifierAgent @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val llmContentParser: LlmContentParser,
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val verifyAdapter = moshi.adapter(Verify::class.java)

    private val systemPrompt = """
        Вы — строгий ревьюер продуктовых спецификаций. На вход подаётся JSON SUMMARY из другого агента.
        Ваша задача — ПРОВЕРИТЬ, что соблюдены обязательные поля A–E, оценить покрытие 12 пунктов и качество.
        Выводите ТОЛЬКО один JSON-объект и сразу <<<END>>> — строго в формате:
        {"mode":"verify","ok":true|false,"score":0.0..1.0,"missing_required":[...],"missing_optional":[...],"notes":"..."}<<<END>>>

        ЯЗЫК ВЫВОДА
        - Используйте язык диалога: определите его по содержимому поля "summary" входного SUMMARY; если язык неочевиден — по последнему пользовательскому сообщению; если и это невозможно — по умолчанию русский.
        - Локализуйте на выбранный язык все ТЕКСТОВЫЕ ЗНАЧЕНИЯ (строки) в полях:
          • elements of "missing_required"
          • elements of "missing_optional"
          • поле "notes"
        - Названия КЛЮЧЕЙ JSON ("mode","ok","score","missing_required","missing_optional","notes") оставляйте на английском.

        Обязательные поля A–E (все должны быть явно в тексте SUMMARY):
        A) PROJECT_NAME (не "TBD"); B) Платформы; C) ЦА и проблема; D) MVP-фичи (минимум 3); E) Сроки/этапы.

        12 пунктов для покрытия: название, платформы, ЦА+проблема, ценность/бизнес-цель, MVP-фичи, дизайн/бренд, интеграции, приватность/комплаенс, нефункциональные требования, сроки/этапы, ограничения/команда, метрики/монетизация.

        Правила проверки:
        - ok=true только если ВСЕ A–E найдены И реальное покрытие ≥ 80% (≥10 из 12 пунктов).
        - score — субъективная оценка качества (структурность/ясность/конкретика) от 0.0 до 1.0.
        - missing_required — перечислите отсутствующие/неявные пункты из A–E (локализованные короткие ярлыки).
        - missing_optional — перечислите отсутствующие пункты из остального списка 12 (локализованные короткие ярлыки).
        - notes — 1–3 коротких совета по улучшению (локализованные).

        Формат и дисциплина:
        - Никакого текста до/после JSON. Ровно один объект и сразу <<<END>>>.
        - Если допустили ошибку формата — немедленно повторите корректный ответ в нужном формате и завершите <<<END>>>.

        Примеры (не выводить как ответ):
        RU → {"mode":"verify","ok":false,"score":0.62,"missing_required":["Сроки/этапы"],"missing_optional":["Приватность/комплаенс","Метрики/монетизация"],"notes":"Уточните ориентировочные даты MVP. Добавьте метрики успеха."}<<<END>>>
        EN → {"mode":"verify","ok":true,"score":0.85,"missing_required":[],"missing_optional":["Brand/design"],"notes":"Consider clarifying non-functional requirements (offline, performance)."}<<<END>>>
    """.trimIndent()

    private val options = OllamaOptions(
        temperature = 0.2,
        topP = 0.9,
        topK = 40,
        repeatPenalty = 1.2,
        numCtx = 4096,
        numPredict = 256,
        stop = listOf("<<<END>>>")
    )

    suspend fun verify(summaryJson: String, model: LlmModels): Verification {
        val history = listOf(
            OllamaChatMessage(Role.SYSTEM, systemPrompt),
            // Передаём SUMMARY как USER-контент (ровно как получил от InterviewAgent)
            OllamaChatMessage(Role.USER, summaryJson)
        )

        val request = OllamaChatRequest(
            model = model.modelName,
            messages = history,
            options = options
        )

        val response = when (model) {
            LlmModels.MISTRAL -> {
                mistralApi.chatOnce(request)
            }
            LlmModels.DEEPSEEK_FREE -> {
                openRouterApi.chat(request.toOpenRouter()).toOllama()
            }
        }

        val normalized = llmContentParser.normalizeAssistantContent(response.message.content)
            ?: error("Summary Verifier Agent: no JSON found")
        val json = normalized.removeSuffix("<<<END>>>")

        val dto = verifyAdapter.fromJson(json)
            ?: error("Agent2: invalid verify JSON")

        return Verification(dto = dto, rawAssistant = response.message.copy(content = normalized))
    }
}
