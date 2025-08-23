package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myaiapp.chat.presentation.ui_model.item.RunResultItem
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun RunResultItemDelegate(
    item: RunResultItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MyAIAppTheme.colors.messageBackgroundColor,
        shape = MyAIAppTheme.shapes.cornersStyle
    ) {
        Column(modifier = modifier.padding(8.dp)) {
            Text(
                text = "JobID: ${item.jobId}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Status: ${item.exitStatus}",
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )
            Text(
                text = "Result: ${item.output}",
                fontSize = 14.sp,
                fontStyle = FontStyle.Normal
            )
        }
    }
}
