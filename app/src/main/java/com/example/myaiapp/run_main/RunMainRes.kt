package com.example.myaiapp.run_main

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RunMainRes(val ok: Boolean, val stdout: String, val stderr: String, val elapsedMs: Long, val error: String?)