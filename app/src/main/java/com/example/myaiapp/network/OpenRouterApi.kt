package com.example.myaiapp.network

import com.example.myaiapp.chat.data.open_router.OpenRouterChatRequest
import com.example.myaiapp.chat.data.open_router.OpenRouterChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenRouterApi {
    // Base URL must be https://openrouter.ai/api/v1/
    @POST("chat/completions")
    suspend fun chat(@Body body: OpenRouterChatRequest): OpenRouterChatResponse
}
