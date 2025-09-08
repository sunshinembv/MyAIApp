package com.example.myaiapp.run_main

import retrofit2.http.Body
import retrofit2.http.POST

interface MainDaemonApi {
    @POST("run-main")
    suspend fun run(@Body body: RunMainReq): RunMainRes
}