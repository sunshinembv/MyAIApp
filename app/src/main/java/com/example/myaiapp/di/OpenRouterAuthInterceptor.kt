package com.example.myaiapp.di

import okhttp3.Interceptor
import okhttp3.Response

class OpenRouterAuthInterceptor(
    private val apiKey: String,
    private val appUrl: String? = null,   // e.g. "https://yourapp.example"; optional
    private val appTitle: String? = null, // e.g. "MyAIApp"; optional
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .apply {
                appUrl?.let { addHeader("HTTP-Referer", it) }
                appTitle?.let { addHeader("X-Title", it) }
            }
            .build()
        return chain.proceed(request)
    }
}
