package com.example.code_agent.integration

import com.example.code_agent.integration.model.UploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("upload-python-file")
    suspend fun uploadPythonFile(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>
}
