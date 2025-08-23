package com.example.myaiapp.chat.presentation.ui_model.item

data class RunResultItem(
    val jobId: String,
    val exitStatus: Int,
    val output: String,
): UiItem
