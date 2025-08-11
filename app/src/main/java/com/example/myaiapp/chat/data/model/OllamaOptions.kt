package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaOptions(
    val temperature: Double? = null,
    @Json(name = "top_p") val topP: Double? = null,
    @Json(name = "top_k") val topK: Int? = null,
    @Json(name = "num_ctx") val numCtx: Int? = null,
    @Json(name = "num_predict") val numPredict: Int? = null,
    val seed: Int? = null,
    val stop: List<String>? = null,
    @Json(name = "repeat_penalty") val repeatPenalty: Double? = null,
    @Json(name = "repeat_last_n") val repeatLastN: Int? = null,
    val mirostat: Int? = null,               // 0/1/2
    @Json(name = "mirostat_tau") val mirostatTau: Double? = null,
    @Json(name = "mirostat_eta") val mirostatEta: Double? = null,
    @Json(name = "tfs_z") val tfsZ: Double? = null,
    @Json(name = "min_p") val minP: Double? = null
)