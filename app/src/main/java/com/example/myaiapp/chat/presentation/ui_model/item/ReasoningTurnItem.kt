package com.example.myaiapp.chat.presentation.ui_model.item

import com.example.myaiapp.chat.data.model.ThinkJson
import com.example.myaiapp.chat.data.model.VerifyJson

data class ReasoningTurnItem(
    val question: String,
    val think: ThinkJson,
    val verify: VerifyJson,
    val finalAnswer: String
): UiItem