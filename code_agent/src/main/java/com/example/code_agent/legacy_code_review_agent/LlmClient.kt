package com.example.code_agent.legacy_code_review_agent

import com.example.code_agent.legacy_code_review_agent.chain_of_agents.JsonExtract
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.LlmResponseFlexible
import com.example.code_agent.legacy_code_review_agent.chain_of_agents.Trace
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration
import java.util.Properties

// --- Minimal Chat Completions payload/response (OpenRouter-compatible) ---
private data class ChatMessage(val role: String, val content: String)
private data class ResponseFormat(val type: String = "json_object") // enforce JSON output when supported
private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.2,
    val max_tokens: Int = 2000,
    val response_format: ResponseFormat? = ResponseFormat() // NEW: request strict JSON
)
private data class Choice(val index: Int, val message: ChatMessage)
private data class ChatResponse(val id: String?, val choices: List<Choice> = emptyList())

// Public client API used by Main.kt
interface LlmClient {
    fun generatePlan(
        system: String,
        user: String,
        model: String,
        temperature: Double = 0.2,
        maxTokens: Int = 2000
    ): LlmResponse
}

/**
 * LLM client for OpenRouter-compatible chat endpoints.
 * - Asks for JSON via response_format (when supported).
 * - Traces requests/responses to out/chain/_trace.
 * - Extracts the LAST JSON object from content (JsonExtract).
 * - Falls back to parsing (LlmResponseFlexible) if strict parse yields empty.
 */
class OpenRouterLlmClient(
    private val endpoint: String,
    private val appTitle: String = "legacy-agent"
) : LlmClient {

    val apiKey = System.getenv("OPEN_ROUTER_API_KEY") ?: run {
        val f = java.io.File("local.properties")
        if (f.exists()) Properties().apply { load(f.inputStream()) }.getProperty("OPEN_ROUTER_API_KEY") else null
    } ?: error("Missing OPEN_ROUTER_API_KEY (env var или local.properties)")

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Minimal DTOs for OpenRouter response
    data class Msg(val role: String, val content: String)
    data class Choice(val message: Msg?)
    data class ORResp(val choices: List<Choice> = emptyList())

    override fun generatePlan(
        system: String,
        user: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): LlmResponse {
        val payload = mapOf(
            "model" to model,
            "messages" to listOf(Msg("system", system), Msg("user", user)),
            "temperature" to temperature,
            "max_tokens" to maxTokens,
            "response_format" to mapOf("type" to "json_object")
        )
        val jsonPayload = moshi.adapter(Map::class.java).toJson(payload)
        //Trace.save("openrouter", "request", jsonPayload)

        val req = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", appTitle)
            .addHeader("X-Title", appTitle)
            .post(jsonPayload.toRequestBody("application/json".toMediaType()))
            .build()

        val raw = client.newCall(req).execute().use { it.body?.string().orEmpty() }
        //Trace.save("openrouter", "raw_response", raw)

        val or = runCatching { moshi.adapter(ORResp::class.java).fromJson(raw) }.getOrNull()
        val content = or?.choices?.firstOrNull()?.message?.content.orEmpty()
        //Trace.save("openrouter", "message_content", content)

        val extracted = JsonExtract.lastJsonObject(content)
        //if (extracted != null) Trace.save("openrouter", "extracted_json", extracted)
        val toParse = extracted ?: JsonExtract.stripCodeFences(content)

        // 1) strict parse into our schema
        val strict = runCatching { moshi.adapter(LlmResponse::class.java).fromJson(toParse) }.getOrNull()
        if (strict != null && (strict.diffs.isNotEmpty() || strict.rewrites.isNotEmpty())) {
            return strict
        }

        // 2) map of alt key names
        val soft = LlmResponseFlexible.parse(moshi, toParse)
        if (soft != null) return soft

        return LlmResponse() // empty
    }
}
