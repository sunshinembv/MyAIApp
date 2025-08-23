package com.example.myaiapp.chat.presentation.ui_model.item

import com.example.myaiapp.chat.data.model.PrState

data class PrBriefItem(
    val title: String,
    val number: Int,
    val state: PrState,
    val author: String,
    val createdAt: String,
    val comments: Int,
    val additions: Int,
    val deletions: Int
): UiItem
