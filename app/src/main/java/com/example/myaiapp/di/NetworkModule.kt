package com.example.myaiapp.di

import com.example.myaiapp.BuildConfig
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideAIApi(): MistralApi {

        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder().client(okHttpClient).baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create()).build()

        return retrofit.create()
    }

    @Provides
    @Singleton
    fun provideOpenRouterApi(): OpenRouterApi {
        val apiKey = BuildConfig.OPEN_ROUTER_API_KEY
        require(apiKey.isNotBlank()) { "OPEN_ROUTER_API_KEY is missing. Put it in local.properties" }
        val client = OkHttpClient.Builder()
            .addInterceptor(OpenRouterAuthInterceptor(apiKey))
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl("https://openrouter.ai/api/v1/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenRouterApi::class.java)
    }

    companion object {

        var BASE_URL = "http://10.0.2.2:11434/"
        private const val TIMEOUT = 0L
    }
}