package com.example.myaiapp.chat.domain.model

import com.example.myaiapp.chat.data.model.ThinkJson
import com.example.myaiapp.chat.data.model.VerifyJson

data class ReasoningTurn(
    val question: String,
    val think: ThinkJson,
    val verify: VerifyJson,
    val finalAnswer: String
)