package com.example.myaiapp.chat.data.repository

import com.example.myaiapp.chat.data.llm_policy.LlmLimits
import com.example.myaiapp.chat.data.llm_policy.UserRoleHolder
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.data.toOllama
import com.example.myaiapp.chat.data.toOpenRouter
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.llm_policy.LlmPolicies
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.chat.domain.repository.SecuredOllamaRepository
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SecuredOllamaRepositoryImpl @Inject constructor(
    private val mistralApi: MistralApi,
    private val openRouterApi: OpenRouterApi,
    private val llmLimits: LlmLimits,
) : SecuredOllamaRepository {

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(ResponseType.MESSENGER))
    )

    override suspend fun chat(content: String, model: LlmModels): Result<String> {
        return withContext(Dispatchers.IO) {
            val role = UserRoleHolder.role
            val policy = LlmPolicies.map[role]!!
            if (!policy.canUseRemoteLLM && model != LlmModels.MISTRAL) return@withContext Result.failure(IllegalAccessException("Access denied for role $role"))

            val bucket = llmLimits.bucketByRole.getValue(role)
            val daily = llmLimits.dailyByRole.getValue(role)
            if (!bucket.tryConsume()) return@withContext Result.failure(IllegalStateException("Rate limit reached"))
            if (!daily.tryConsume(policy.dailyLimit)) return@withContext Result.failure(IllegalStateException("Daily cap reached"))

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

            return@withContext Result.success(response.message.content)
        }
    }
}