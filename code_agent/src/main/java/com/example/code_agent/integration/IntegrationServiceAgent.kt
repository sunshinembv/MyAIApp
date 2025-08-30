package com.example.code_agent.integration

import com.example.code_agent.integration.model.UploadResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

private const val BASE_URL = "https://nervous-grasshopper-83.loca.lt/"
private const val DEFAULT_FILE = ""

fun main(args: Array<String>) = runBlocking {
    val filePath = args.getOrNull(0) ?: DEFAULT_FILE

    val client = provideOkHttp()
    val api = provideApi(client)

    val file = File(filePath)
    require(file.exists()) { "Файл не найден: $filePath" }

    val media = when {
        file.extension.equals("py", ignoreCase = true) -> "text/x-python"
        else -> "application/octet-stream"
    }.toMediaTypeOrNull()

    val body = file.asRequestBody(media)
    val part = MultipartBody.Part.createFormData("file", file.name, body)

    println("→ Загружаю файл: ${file.absolutePath}")
    val resp: Response<UploadResponse> = api.uploadPythonFile(part)

    if (resp.isSuccessful) {
        val data = resp.body()
        println("✅ Успех: ${data?.message ?: "OK"}")
        println("• path: ${data?.file_path}")
        val report = data?.test_result?.api_response?.result?.content?.firstOrNull()?.text
        if (!report.isNullOrBlank()) {
            println("\n===== Отчёт =====\n$report")
        }
    } else {
        System.err.println("❌ HTTP ${resp.code()}: ${resp.errorBody()?.string().orEmpty()}")
    }
}

private fun provideOkHttp(): OkHttpClient {
    val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
    return OkHttpClient.Builder()
        .addInterceptor(logger)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(15, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()
}

private fun provideApi(client: OkHttpClient): ApiService {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
    return retrofit.create(ApiService::class.java)
}