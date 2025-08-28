package com.example.code_agent

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApi {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: ChatRequest): ChatResponse
}

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = 0.2,
    val stream: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val choices: List<Choice>,
    val usage: Usage?
)

@JsonClass(generateAdapter = true)
data class Choice(val index: Int, val message: ChatMessage)

@JsonClass(generateAdapter = true)
data class Usage(
    @Json(name = "prompt_tokens") val promptTokens: Int,
    @Json(name = "completion_tokens") val completionTokens: Int,
    @Json(name = "total_tokens") val totalTokens: Int
)

class OpenRouterAuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("X-Title", "MyAIApp Tools")
            .build()
        println("OpenRouterAuth: → ${req.method}${req.url}")
        return chain.proceed(req)
    }
}

fun createOpenRouterApi(apiKey: String): OpenRouterApi {
    val client = OkHttpClient.Builder()
        .addInterceptor(OpenRouterAuthInterceptor(apiKey))
        .build()

    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    return retrofit.create(OpenRouterApi::class.java)
}