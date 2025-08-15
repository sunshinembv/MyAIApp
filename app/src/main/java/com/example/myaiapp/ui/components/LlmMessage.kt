package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.example.myaiapp.R
import com.example.myaiapp.ui.components.basic.AppMessage
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun LlmMessage(
    agentName: String,
    text: String,
    pending: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MyAIAppTheme.colors.messageBackgroundColor
) {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.indent_4dp)))
        AppMessage(
            agentName = agentName,
            text = text,
            color = color,
            pending = pending,
            modifier = modifier
        )
    }
}
