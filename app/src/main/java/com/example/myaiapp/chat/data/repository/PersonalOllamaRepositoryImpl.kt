package com.example.myaiapp.chat.data.repository

import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.LlmContextComposer
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.repository.PersonalOllamaRepository
import com.example.myaiapp.memory.data.models.FactDTO
import com.example.myaiapp.memory.data.repository.PersonalizationRepository
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PersonalOllamaRepositoryImpl @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val llmContextComposer: LlmContextComposer,
    private val personalizationRepository: PersonalizationRepository,
) : PersonalOllamaRepository {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val factListAdapter = moshi.adapter<List<FactDTO>>(Types.newParameterizedType(List::class.java, FactDTO::class.java))

    private val history: MutableList<OllamaChatMessage> = mutableListOf()
    private val factHistory: MutableList<OllamaChatMessage> = mutableListOf()

    override suspend fun chat(content: String, model: LlmModels): String {
        return withContext(Dispatchers.IO) {

            if (history.isEmpty()) {
                history += OllamaChatMessage(Role.SYSTEM, llmContextComposer.buildSystemPrompt())
            }

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

            extractAndPersistFacts()

            response.message.content
        }
    }

    override suspend fun extractFact(
        content: String,
        model: LlmModels
    ): String {
        return withContext(Dispatchers.IO) {
            factHistory.clear()
            factHistory += history
            factHistory += listOf(OllamaChatMessage(Role.USER, content))

            val request = OllamaChatRequest(
                model = model.modelName,
                messages = factHistory,
                options = OllamaOptions(
                    temperature = 0.3,
                    numPredict = 256,
                    topP = 1.0,
                    seed = 42,
                ),
                stream = false,
                keepAlive = "5m"
            )

            val response = mistralApi.chatOnce(request)
            response.message.content
        }
    }

    suspend fun extractAndPersistFacts(
        model: LlmModels = LlmModels.MISTRAL
    ) {
        val prompt = llmContextComposer.buildExtractPrompt(history)

        // 1. дергаем LLM
        val raw = extractFact(prompt, model)

        // 2. парсим JSON
        val facts = factListAdapter.fromJson(raw).orEmpty()

        // 3. фильтруем и сохраняем
        facts.filter { it.importance >= 3 && it.text.isNotBlank() }
            .distinctBy { it.text.lowercase() }
            .take(10)
            .forEach { personalizationRepository.addMemory(it.text.trim(), it.importance, "fact", "extract") }
    }
}