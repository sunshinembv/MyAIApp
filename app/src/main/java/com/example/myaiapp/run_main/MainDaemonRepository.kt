package com.example.myaiapp.run_main

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainDaemonRepository @Inject constructor(
    private val mainDaemonApi: MainDaemonApi,
) {

    suspend fun run(className: String, args: List<String>): RunMainRes {
        return withContext(Dispatchers.IO) {
            mainDaemonApi.run(RunMainReq(className, args))
        }
    }
}