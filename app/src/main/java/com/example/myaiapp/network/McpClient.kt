package com.example.myaiapp.network

import com.example.myaiapp.chat.data.McpGithubParsers
import com.example.myaiapp.chat.data.model.GithubPrsInput
import com.example.myaiapp.chat.data.model.Plan
import com.example.myaiapp.chat.data.model.PrBrief
import com.example.myaiapp.chat.data.model.PrRaw
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

private const val GITHUB_PAT = ""

private const val MCP_URL = "https://api.githubcopilot.com/mcp/"
private val JSON = Json { ignoreUnknownKeys = true; prettyPrint = true }
private val JSON_MEDIA = "application/json".toMediaType()

private const val PROTO = "2025-06-18"
private const val MCP_NOTION_AUTH = ""
private const val MCP_NOTION_URL = "http://10.0.2.2:3000/mcp"

class McpClient @Inject constructor() {
    private val http = OkHttpClient()
    private var sessionId: String? = null
    private var notionSessionId: String? = null

    private fun rpc(method: String, params: JsonObject? = null, id: Int = 1): JsonObject {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0"); put("id", id); put("method", method)
            if (params != null) put("params", params)
        }

        val reqB = Request.Builder()
            .url(MCP_URL)
            .addHeader("Authorization", "Bearer $GITHUB_PAT")
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")
            .addHeader("MCP-Protocol-Version", PROTO)

        // после initialize сервер может выдать Mcp-Session-Id — добавляем на все следующие вызовы:
        sessionId?.let { reqB.addHeader("Mcp-Session-Id", it) }

        val req = reqB.post(JSON.encodeToString(payload).toRequestBody(JSON_MEDIA)).build()

        http.newCall(req).execute().use { resp ->
            val bodyStr = resp.body?.string() ?: ""
            // поймаем сессию с первого ответа (обычно на initialize)
            resp.header("Mcp-Session-Id")?.let { sessionId = it }

            if (!resp.isSuccessful) error("HTTP ${resp.code}: $bodyStr")
            return JSON.parseToJsonElement(bodyStr).jsonObject
        }
    }

    private fun rpcNotion(method: String, params: JsonObject? = null, id: Int = 1001): JsonObject {
        val payload = buildJsonObject {
            put("jsonrpc", "2.0"); put("id", id); put("method", method)
            if (params != null) put("params", params)
        }

        val reqB = Request.Builder()
            .url(MCP_NOTION_URL)
            .addHeader("Authorization", "Bearer $MCP_NOTION_AUTH")
            .addHeader("Accept", "application/json, text/event-stream")
            .addHeader("Content-Type", "application/json")
            .addHeader("MCP-Protocol-Version", PROTO)

        notionSessionId?.let { reqB.addHeader("Mcp-Session-Id", it) }

        val req = reqB.post(JSON.encodeToString(payload).toRequestBody(JSON_MEDIA)).build()

        http.newCall(req).execute().use { resp ->
            val raw = resp.body?.string() ?: ""
            resp.header("Mcp-Session-Id")?.let { notionSessionId = it }

            if (!resp.isSuccessful) error("HTTP ${resp.code}: $raw")

            // Если ответ в виде SSE, берём содержимое после data:
            val jsonStr = if (raw.startsWith("event:")) {
                raw.lines().firstOrNull { it.startsWith("data:") }
                    ?.removePrefix("data:")?.trim()
                    ?: error("Unexpected SSE format: $raw")
            } else {
                raw
            }

            return JSON.parseToJsonElement(jsonStr).jsonObject
        }
    }

    fun initialize(): JsonObject = rpc(
        "initialize",
        buildJsonObject {
            put("protocolVersion", PROTO)
            put("clientInfo", buildJsonObject { put("name", "myaiapp-android"); put("version", "0.1.0") })
            put("capabilities", buildJsonObject { /* ок оставить пустым */ })
        },
        id = 1
    )

    fun initializeNotion(): JsonObject = rpcNotion(
        "initialize",
        buildJsonObject {
            put("protocolVersion", PROTO)
            put("clientInfo", buildJsonObject { put("name", "myaiapp-android"); put("version", "0.1.0") })
            put("capabilities", buildJsonObject { /* можно оставить пустым */ })
        },
        id = 1000
    )

    fun showToolsList(): JsonObject = rpcNotion("tools/list", id = 999)

    fun notionToolsCall(name: String, arguments: JsonObject, id: Int = 1002): JsonObject =
        rpcNotion(
            "tools/call",
            buildJsonObject {
                put("name", name)
                put("arguments", arguments)
            },
            id = id
        )

    private fun buildPrsMarkdownReport(title: String, briefs: List<PrBrief>): String {
        val sb = StringBuilder()
        sb.append("# ").append(title).append('\n').append('\n')
        if (briefs.isEmpty()) {
            sb.append("_No pull requests found._\n")
        } else {
            briefs.forEach { pr ->
                sb.append("- PR #").append(pr.number).append(": ").append(pr.title).append('\n')
            }
        }
        return sb.toString()
    }

    fun createNotionReportPage(
        parentId: String,
        title: String,
        briefs: List<PrBrief>,
    ): JsonObject {
        val markdown = buildPrsMarkdownReport(title, briefs)
        val args = buildJsonObject {
            put("parent", buildJsonObject { put("page_id", parentId) })
            put("properties", buildJsonObject {
                put("title", buildJsonObject {
                    put("title", buildJsonArray {
                        add(buildJsonObject {
                            put("text", buildJsonObject { put("content", title) })
                        })
                    })
                })
            })
            // один параграф с markdown-текстом
            put("children", buildJsonArray {
                add(buildJsonObject {
                    put("object", "block")
                    put("type", "paragraph")
                    put("paragraph", buildJsonObject {
                        put("rich_text", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", buildJsonObject { put("content", markdown) })
                            })
                        })
                    })
                })
            })
        }
        return notionToolsCall("API-post-page", args)
    }

    fun createOrUpdateFile(p: Plan): JsonObject {
        // Основной вызов MCP инструмента (см. tool create_or_update_file).
        val args = buildJsonObject {
            put("owner", p.owner)
            put("repo", p.repo)
            put("path", p.path)
            put("content", p.content)   // обычная строка, без base64 для MCP инструмента
            put("message", p.message)
            put("branch", p.branch)
        }
        val callParams = buildJsonObject {
            put("name", "create_or_update_file")
            put("arguments", args)
        }
        return rpc("tools/call", callParams, id = 2)
    }

    fun fetchPrBriefs(githubPrsInput: GithubPrsInput): List<PrBrief> {
        val list = rpc("tools/call", buildJsonObject {
            put("name", "list_pull_requests")
            put("arguments", buildJsonObject {
                put("owner", githubPrsInput.owner); put("repo", githubPrsInput.repo)
                put("state", githubPrsInput.state.asArg()); put("sort", "updated"); put("direction", "desc"); put("per_page", 50)
            })
        }, id = 10)

        val briefsFromList: List<PrBrief> = McpGithubParsers.parsePrListFromMcpJson(list.toString())
        val briefs: List<PrBrief> = briefsFromList.take(githubPrsInput.limit).mapIndexed { idx, item ->
            val prResp: JsonObject = rpc(
                "tools/call",
                buildGetPrCall(githubPrsInput.owner, githubPrsInput.repo, item.number),
                id = 11 + idx
            )
            val detail: PrRaw = McpGithubParsers.parsePrDetail(prResp.toString()) // ← строка
            detail.toBrief().copy(title = item.title.ifBlank { detail.title })
        }
        return briefs
    }

    fun buildGetPrCall(owner: String, repo: String, number: Int, toolName: String = "get_pull_request"): JsonObject =
        buildJsonObject {
            put("name", toolName)
            put("arguments", buildJsonObject {
                put("owner", owner); put("repo", repo); put("pullNumber", number)
            })
        }
}
