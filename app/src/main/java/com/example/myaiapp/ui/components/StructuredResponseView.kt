package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myaiapp.chat.data.model.StructuredResponse

@Composable
fun StructuredResponseView(response: StructuredResponse, modifier: Modifier) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            text = response.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = response.subtitle,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic
        )
        Text(
            text = response.summary,
            fontSize = 14.sp,
            fontStyle = FontStyle.Normal
        )
    }
}