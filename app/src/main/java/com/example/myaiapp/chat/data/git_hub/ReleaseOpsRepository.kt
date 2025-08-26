package com.example.myaiapp.chat.data.git_hub

import com.example.myaiapp.chat.data.git_hub.models.OpsEnvelope
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReleaseOpsRepository @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val router: ReleaseOpsRouter,
) {
    private val moshiInst: Moshi = (Moshi.Builder().addLast(KotlinJsonAdapterFactory())
        .build())

    private val envelopeAdapter = moshiInst.adapter(OpsEnvelope::class.java).lenient()

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(ResponseType.RELEASE_OPS_SYSTEM_PROMPT))
    )

    suspend fun runCommand(content: String, model: LlmModels): String {
        return withContext(Dispatchers.IO) {
            history += OllamaChatMessage(Role.USER, content)
            val request = OllamaChatRequest(
                model = model.modelName,
                messages = history,
                options = OllamaOptions(
                    temperature = 0.1,
                    topP = 0.95,
                    numCtx = 4096,
                ),
                stream = false,
                keepAlive = "5m"
            )

            val response = when (model) {
                LlmModels.MISTRAL -> {
                    mistralApi.chatOnce(request)
                }
                LlmModels.DEEPSEEK_FREE -> {
                    openRouterApi.chat(request.toOpenRouter()).toOllama()
                }
            }

            val json = extractFirstJsonObject(response.message.content)
                ?: error("LLM did not return a JSON object")

            val env = envelopeAdapter.fromJson(json)
                ?: error("Failed to parse LLM JSON")

            router.handle(env)
        }
    }

    /**
     * Достаём первый JSON-объект из текста (на случай, если модель завернула в ```json … ``` или добавила префикс).
     */
    private fun extractFirstJsonObject(s: String): String? {
        val trimmed = s.trim()
            .removePrefix("```json").removeSuffix("```")
            .removePrefix("```").removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end   = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else null
    }
}
