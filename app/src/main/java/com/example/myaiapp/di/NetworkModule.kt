package com.example.myaiapp.di

import com.example.myaiapp.BuildConfig
import com.example.myaiapp.network.GitHubActionsApi
import com.example.myaiapp.network.MistralApi
import com.example.myaiapp.network.OpenRouterApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideMistralApi(): MistralApi {

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

    @Provides
    @Singleton
    fun provideGitHubActionsApi(): GitHubActionsApi {
        val token = BuildConfig.GITHUB_PAT
        require(token.isNotBlank()) { "GITHUB_PAT is missing. Put it in local.properties" }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val auth = Interceptor { chain ->
            val reqBuilder = chain.request().newBuilder()
                .addHeader("Accept", "application/vnd.github+json")
                .addHeader("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(reqBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor(logging)
            .connectTimeout(30.seconds.toJavaDuration())
            .readTimeout(60.seconds.toJavaDuration())
            .build()

        val moshi: Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(GitHubActionsApi::class.java)
    }

    companion object {

        var BASE_URL = "http://10.0.2.2:11434/"
        private const val TIMEOUT = 0L
    }
}