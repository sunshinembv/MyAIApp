package com.example.myaiapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.myaiapp.chat.presentation.ui_model.item.OwnMessageItem
import com.example.myaiapp.ui.components.basic.AppMessage
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun OwnMessageItemDelegate(
    item: OwnMessageItem,
    modifier: Modifier = Modifier,
    color: Color = MyAIAppTheme.colors.ownMessageBackgroundColor
) {
    AppMessage(
        text = item.text,
        color = color,
        modifier = modifier
    )
}
