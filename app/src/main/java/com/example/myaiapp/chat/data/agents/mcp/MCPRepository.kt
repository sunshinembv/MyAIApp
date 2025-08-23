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
import com.example.myaiapp.chat.domain.model.ResponseType
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.McpClient
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

class MCPRepository @Inject constructor(
    private val api: MistralApi,
) {

    private val history: MutableList<OllamaChatMessage> = mutableListOf(
        OllamaChatMessage(Role.SYSTEM, PromptBuilder.systemPrompt(ResponseType.MCP_GIT_PR))
    )

    suspend fun callLlmToMCP(content: String, model: LlmModels): String {
        history += OllamaChatMessage(Role.USER, content)
        val request = OllamaChatRequest(
            model = model.modelName,
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

    suspend fun callLlmToMCPGitHubPr(content: String, model: LlmModels): List<PrBrief> {
        history += OllamaChatMessage(Role.USER, content)
        val request = OllamaChatRequest(
            model = model.modelName,
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

    suspend fun callLlmToMCPPrReport(prompt: String, model: LlmModels): JsonObject {
        // добавляем сообщение пользователя в историю (как в других методах)
        history += OllamaChatMessage(Role.USER, prompt)

        val request = OllamaChatRequest(
            model = model.modelName,
            messages = history,
            format = "json",
            options = OllamaOptions(
                temperature = 0.1,
                topP = 0.95,
                numCtx = 4096,
                stop = listOf("```")
            ),
            stream = false,
            keepAlive = "5m"
        )

        // получаем ответ от LLM
        val response = api.chatOnce(request)
        history += response.message
        return runPrReportFlow(response.message.content)
    }

    private suspend fun runPrReportFlow(localLlmAnswerJson: String): JsonObject =
        withContext(Dispatchers.IO) {
            val mcp = McpClient()
            mcp.initialize()        // GitHub MCP
            mcp.initializeNotion()  // Notion MCP

            val prsInput = GithubPrsInput.fromJson(localLlmAnswerJson)
            val pageName = prsInput.pageName

            val searchArgs = buildJsonObject {
                put("query", pageName)  // здесь вместо "AI" подставьте название родительской страницы
                put("filter", buildJsonObject {
                    put("value", "page")      // ищем только страницы (не базы)
                    put("property", "object")
                })
            }

            val searchResp = mcp.notionToolsCall("API-post-search", searchArgs)

            val contentArray = searchResp["result"]?.jsonObject?.get("content")?.jsonArray ?: error("No content")
            val jsonString = contentArray.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content
                ?: error("No text in search response")

            val searchJson = Json.parseToJsonElement(jsonString).jsonObject
            val resultsArray = searchJson["results"]?.jsonArray ?: emptyList()

            var parentId: String? = null
            for (res in resultsArray) {
                val obj = res.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content
                val titleProp = obj["properties"]?.jsonObject?.get("title")?.jsonObject
                val titleText = titleProp
                    ?.get("title")?.jsonArray
                    ?.firstOrNull()?.jsonObject
                    ?.get("plain_text")?.jsonPrimitive?.content

                // Сравниваем название; здесь ищем точное совпадение
                if (obj["object"]?.jsonPrimitive?.content == "page" && titleText == pageName) {
                    parentId = id
                    break
                }
            }

            if (parentId == null) {
                error("Страница с названием $pageName не найдена или интеграция не имеет к ней доступа")
            }

            val briefs = mcp.fetchPrBriefs(prsInput)
            mcp.createNotionReportPage(parentId, parentId, briefs)
        }
}
