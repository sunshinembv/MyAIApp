package com.example.myaiapp.network

import com.example.myaiapp.chat.data.model.ChatRequestData
import com.example.myaiapp.chat.data.model.ChatResponseData
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AIApi {

    @POST("api/chat")
    @Headers("Content-Type: application/json")
    suspend fun chatOnce(@Body body: ChatRequestData): ChatResponseData
}