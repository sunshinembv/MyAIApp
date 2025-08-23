package com.example.myaiapp.chat.presentation.ui_model.item

data class VerifyItem(
    val mode: String,
    val ok: Boolean,
    val score: Double,
    val notes: String,
    val missingRequired: List<String>? = null,
    val missingOptional: List<String>? = null,
): UiItem
