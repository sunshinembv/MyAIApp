package com.example.myaiapp.chat.data.agents.mcp

import com.example.myaiapp.chat.data.model.GithubPrsInput
import com.example.myaiapp.chat.data.model.OllamaChatMessage
import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaOptions
import com.example.myaiapp.chat.data.model.Plan
import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.Role
import com.example.myaiapp.chat.domain.PromptBuilder
import com.example.myaiapp.chat.domain.model.LlmModels
import com.example.myaiapp.chat.domain.model.OutputFormat
import com.example.myaiapp.network.AIApi
import com.example.myaiapp.network.McpClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject

class MCPRepository @Inject constructor(
    private val api: AIApi,
) {

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(OutputFormat.MCP_GIT_PR))
    )

    suspend fun callLlmToMCP(content: String): String {
        history += OllamaChatMessage(Role.USER, content)
        val request = OllamaChatRequest(
            model = LlmModels.MISTRAL.modelName,
            messages = history,
            format = "json",
            options = OllamaOptions(
                temperature = 0.1,
                topP = 0.95,
                numCtx = 4096,
                stop = listOf("```") // страховка от кодовых блоков
            ),
            stream = false,
            keepAlive = "5m"
        )

        val response = api.chatOnce(request)
        history += response.message
        val mcpResult = runHelloFileFlow(response.message.content)
        return mcpResult
    }

    suspend fun callLlmToMCPGitHubPr(content: String): List<PrBrief> {
        history += OllamaChatMessage(Role.USER, content)
        val request = OllamaChatRequest(
            model = LlmModels.MISTRAL.modelName,
            messages = history,
            format = "json",
            options = OllamaOptions(
                temperature = 0.1,
                topP = 0.95,
                numCtx = 4096,
                stop = listOf("```") // страховка от кодовых блоков
            ),
            stream = false,
            keepAlive = "5m"
        )

        val response = api.chatOnce(request)
        history += response.message
        val mcpResult = runPrBriefFlow(response.message.content)
        return mcpResult
    }

    private suspend fun runHelloFileFlow(
        localLlmAnswerJson: String, // ответ LLM из локального сервера
    ): String = withContext(Dispatchers.IO) {
        val plan = Moshi.Builder().build()
            .adapter(Plan::class.java)
            .fromJson(localLlmAnswerJson) ?: error("Bad plan JSON")

        require(plan.action == "create_file")

        val mcp = McpClient()
        mcp.initialize()

        val resp = mcp.createOrUpdateFile(plan)
        val result = resp["result"]?.jsonObject
            ?: error("No result: $resp")

        // Удобный URL к файлу в GitHub (ветка/путь)
        val fileUrl = "https://github.com/${plan.owner}/${plan.repo}/blob/${plan.branch}/${plan.path}"
        return@withContext fileUrl
    }

    private suspend fun runPrBriefFlow(
        localLlmAnswerJson: String,
    ): List<PrBrief> = withContext(Dispatchers.IO) {

        val prsInput = GithubPrsInput.fromJson(localLlmAnswerJson)

        val mcp = McpClient()
        mcp.initialize()

        val response = mcp.fetchPrBriefs(prsInput)
        response
    }
}