package com.example.myaiapp.chat.data.repository

import com.example.myaiapp.chat.data.LlmContentParser
import com.example.myaiapp.chat.data.model.LlmReply
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaChatResponse
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.chat.domain.repository.OllamaRepository
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OllamaRepositoryImpl @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val llmContentParser: LlmContentParser,
) : OllamaRepository {

    val rawHistory = mutableListOf<OllamaChatMessage>()

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(ResponseType.VOICE))
    )

    override suspend fun chatOnce(
        model: String,
        systemPrompt: String,
        content: String,
        history: List<OllamaChatMessage>
    ): LlmReply {

        val userMessage = OllamaChatMessage(role = Role.USER, content = content)
        val messages = buildList {
            addAll(history)
            add(userMessage)
        }
        val newHistory = withSystem(messages, systemPrompt)

        val options = OllamaOptions(
            temperature = 0.2,
            topP = 0.9,
            topK = 40,
            repeatPenalty = 1.25,
            numCtx = 4096,
            numPredict = 256,
            stop = listOf("<<<END>>>")
        )

        val response = ensureJsonOrRetry(newHistory) { messages ->
            mistralApi.chatOnce(
                OllamaChatRequest(
                    model = model,
                    messages = messages,
                    options = options,
                    stream = false,
                    keepAlive = "5m"
                )
            )
        }

        val normalized =
            (llmContentParser.normalizeAssistantContent(response.message.content))
                ?: error("No JSON found after retry")
        val normalizedOllamaChatMessage = response.message.copy(content = normalized)
        rawHistory.add(userMessage)
        rawHistory.add(normalizedOllamaChatMessage)

        val json = normalized.removeSuffix("<<<END>>>")
        val llmReply = llmContentParser.parseAs(json, rawHistory)
        return llmReply
    }

    override suspend fun chat(content: String, model: LlmModels): String {
        return withContext(Dispatchers.IO) {
            history += OllamaChatMessage(Role.USER, content)
            val request = OllamaChatRequest(
                model = model.modelName,
                messages = history,
                options = OllamaOptions(
                    temperature = 0.3,
                    numPredict = 256,
                    topP = 1.0,
                    seed = 42,
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

            response.message.content
        }
    }

    private suspend fun ensureJsonOrRetry(
        messages: List<OllamaChatMessage>,
        call: suspend (List<OllamaChatMessage>) -> OllamaChatResponse
    ): OllamaChatResponse {
        val r1 = call(messages)
        if (llmContentParser.extractFirstJson(r1.message.content) != null) return r1

        val guard = OllamaChatMessage(
            role = Role.SYSTEM,
            content = """
                OUTPUT FORMAT ERROR.
                Допустим только один JSON-объект и маркер <<<END>>>.
                Либо {"mode":"ask","q":"..."}<<<END>>>
                Либо {"title":"Summary","subtitle":"Project: ...","summary":"..."}<<<END>>>.
            """.trimIndent()
        )
        val r2 = call(insertGuard(messages, guard))
        return if (llmContentParser.extractFirstJson(r2.message.content) != null) r2 else r1
    }

    private fun insertGuard(
        msgs: List<OllamaChatMessage>,
        guard: OllamaChatMessage
    ): List<OllamaChatMessage> {
        // Вставляем guard сразу после первого SYSTEM, чтобы не ломать историю
        val i = msgs.indexOfFirst { it.role == Role.SYSTEM }
        return if (i >= 0) msgs.toMutableList()
            .apply { add(i + 1, guard) } else listOf(guard) + msgs
    }

    fun withSystem(
        messages: List<OllamaChatMessage>,
        systemPrompt: String
    ): List<OllamaChatMessage> {
        // если нет system — добавим его первым
        val hasSystem = messages.any { it.role == Role.SYSTEM }
        if (!hasSystem) return listOf(OllamaChatMessage(Role.SYSTEM, systemPrompt)) + messages

        // если system есть, убедимся что он первый и один
        val firstIsSystem = messages.first().role == Role.SYSTEM
        val firstSystem = messages.firstOrNull { it.role == Role.SYSTEM }!!
        val rest =
            messages.filterIndexed { i, m -> !(i != messages.indexOf(firstSystem) && m.role == Role.SYSTEM) }
        return if (firstIsSystem) rest else listOf(firstSystem) + rest.filter { it !== firstSystem }
    }
}