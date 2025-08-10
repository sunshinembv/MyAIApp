package com.example.myaiapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.myaiapp.R
import com.example.myaiapp.ui.components.basic.AppMessage
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun LlmMessage(
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
            text = text,
            color = color,
            pending = pending,
            modifier = modifier
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun UserMessagePreview() {
    MyAIAppTheme {
        LlmMessage(
            text = "Text",
            pending = false,
        )
    }
}