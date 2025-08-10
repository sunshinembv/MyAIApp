package com.example.myaiapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val shapes = MyAIAppShape(
    padding = 16.dp,
    cornersStyle = RoundedCornerShape(8.dp),
    topCornersStyle = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    bottomCornersStyle = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
    dateCornersStyle = RoundedCornerShape(16.dp),
    reactionsCornersStyle = RoundedCornerShape(24.dp),
    shimmerCornersStyle = RoundedCornerShape(24.dp),
)