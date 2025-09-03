package com.example.myaiapp.ui

import VerifyItemDelegate
import androidx.compose.runtime.Composable
import com.example.myaiapp.chat.presentation.ui_model.item.MessageItem
import com.example.myaiapp.chat.presentation.ui_model.item.OwnMessageItem
import com.example.myaiapp.chat.presentation.ui_model.item.PendingItem
import com.example.myaiapp.chat.presentation.ui_model.item.PrBriefItem
import com.example.myaiapp.chat.presentation.ui_model.item.ReasoningTurnItem
import com.example.myaiapp.chat.presentation.ui_model.item.RunResultItem
import com.example.myaiapp.chat.presentation.ui_model.item.SummaryItem
import com.example.myaiapp.chat.presentation.ui_model.item.UiItem
import com.example.myaiapp.chat.presentation.ui_model.item.VerifyItem
import com.example.myaiapp.ui.components.MessageItemDelegate
import com.example.myaiapp.ui.components.OwnMessageItemDelegate
import com.example.myaiapp.ui.components.PendingItemDelegate
import com.example.myaiapp.ui.components.PrBriefItemDelegate
import com.example.myaiapp.ui.components.ReasoningTurnItemDelegate
import com.example.myaiapp.ui.components.RunResultItemDelegate
import com.example.myaiapp.ui.components.SummaryItemDelegate

@Composable
fun UiItemDelegate(item: UiItem) {
    when (item) {
        is RunResultItem -> {
            RunResultItemDelegate(item)
        }

        is PrBriefItem -> {
            PrBriefItemDelegate(item)
        }

        is SummaryItem -> {
            SummaryItemDelegate(item)
        }

        is VerifyItem -> {
            VerifyItemDelegate(item)
        }

        is MessageItem -> {
            MessageItemDelegate(item)
        }

        is OwnMessageItem -> {
            OwnMessageItemDelegate(item)
        }

        is PendingItem -> {
            PendingItemDelegate(item)
        }

        is ReasoningTurnItem -> {
            ReasoningTurnItemDelegate(item)
        }
    }
}
