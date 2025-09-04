package com.example.myaiapp.chat.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ThinkJson(
    @Json(name = "think_bullets")
    val thinkBullets: List<String>,  // 2–5 коротких пунктов — публичное, не «внутренний дневник»
    @Json(name = "answer_draft")
    val answerDraft: String,        // черновик ответа
    val claims: List<String>,        // 1–5 атомарных утверждений
    val confidence: Int? = null      // 0–100
)