package com.example.myaiapp.run_main

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RunMainReq(val className: String, val args: List<String> = emptyList(), val timeoutSec: Long = 300)