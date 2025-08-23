package com.example.myaiapp.network

import com.example.myaiapp.chat.data.model.OllamaChatRequest
import com.example.myaiapp.chat.data.model.OllamaChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface MistralApi {

    @POST("api/chat")
    suspend fun chatOnce(@Body body: OllamaChatRequest): OllamaChatResponse
}