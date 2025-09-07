package com.example.code_agent.legacy_code_review_agent

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Duration

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

class OpenRouterLlmClient(
    private val endpoint: String = "https://openrouter.ai/api/v1/chat/completions",
    private val apiKey: String,
    private val appUrl: String? = null,   // optional: improves attribution on OpenRouter
    private val appTitle: String? = null  // optional: improves attribution on OpenRouter
) : LlmClient {

    private val json = "application/json; charset=utf-8".toMediaType()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val chatReqAdapter = moshi.adapter(ChatRequest::class.java)
    private val chatRespAdapter = moshi.adapter(ChatResponse::class.java)
    private val llmRespAdapter = moshi.adapter(LlmResponse::class.java)

    private val client = OkHttpClient.Builder()
        .callTimeout(Duration.ofSeconds(120))
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(90))
        .build()

    override fun generatePlan(
        system: String,
        user: String,
        model: String,
        temperature: Double,
        maxTokens: Int
    ): LlmResponse {
        val payload = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", system),
                ChatMessage("user", user)
            ),
            temperature = temperature,
            max_tokens = maxTokens,
            response_format = ResponseFormat() // ask for JSON-only output
        )
        val body = chatReqAdapter.toJson(payload).toRequestBody(json)

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
        appUrl?.let { reqBuilder.addHeader("HTTP-Referer", it) }
        appTitle?.let { reqBuilder.addHeader("X-Title", it) }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                val raw = resp.body?.string()
                error("LLM HTTP ${resp.code}: ${raw}")
            }
            val parsed = chatRespAdapter.fromJson(resp.body!!.source())
                ?: error("Empty LLM response")
            val content = parsed.choices.firstOrNull()?.message?.content
                ?: error("No choices from LLM")

            val jsonText = extractJsonObject(content)
            return llmRespAdapter.fromJson(jsonText)
                ?: error("Failed to parse LLM JSON payload")
        }
    }
}