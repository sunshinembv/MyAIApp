package com.example.myaiapp.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val typography = MyAIAppTypography(
    heading1 = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    heading2 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    ),
    body = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    ),
    toolbar = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium
    ),
    caption = TextStyle(
        fontSize = 12.sp
    )
)