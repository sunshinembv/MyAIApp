package com.example.myaiapp.ui.components

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.myaiapp.ui.components.basic.AppMessage
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun OwnMessage(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MyAIAppTheme.colors.ownMessageBackgroundColor
) {
    AppMessage(
        text = text,
        color = color,
        modifier = modifier
    )
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun OwnMessagePreview() {
    MyAIAppTheme {
        OwnMessage(
            text = "Text",
        )
    }
}