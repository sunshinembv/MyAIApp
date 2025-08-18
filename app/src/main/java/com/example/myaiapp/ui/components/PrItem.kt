package com.example.myaiapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myaiapp.chat.data.model.PrBrief
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PrItem(
    pr: PrBrief,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier.padding(bottom = 8.dp)
            .fillMaxWidth()
            .border(1.dp, Color(0x1F000000), containerShape)
            .background(Color(0xFFF9F9FB), containerShape)
            .padding(14.dp)
    ) {
        BasicText(
            text = pr.title,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111),
                lineHeight = 24.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(6.dp))

        BasicText(
            text = "Author: ${pr.author}",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )
        )

        Spacer(Modifier.height(10.dp))

        LabeledNumber(label = "State", value = pr.state.asArg())

        Spacer(Modifier.height(4.dp))

        LabeledNumber(label = "Comments", value = pr.comments.toString())

        Spacer(Modifier.height(4.dp))

        LabeledNumber(label = "Additions", value = pr.additions.toString(), valueColor = Color(0xFF2E7D32))

        Spacer(Modifier.height(4.dp))

        LabeledNumber(label = "Deletions", value = pr.deletions.toString(), valueColor = Color(0xFFC62828))

        Spacer(Modifier.height(10.dp))

        val timeText = formatTime(pr.createdAt)
        LabeledNumber(label = "Time", value = timeText, valueColor = Color(0xFF7A7A7A))
    }
}

@Composable
private fun LabeledNumber(label: String, value: String, valueColor: Color = Color.Unspecified) {
    val text = buildAnnotatedString {
        append("$label: ")
        pushStyle(SpanStyle(color = valueColor, fontWeight = FontWeight.Normal))
        append(value)
        pop()
    }
    BasicText(
        text = text,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = Color(0xFF222222)
        )
    )
}

private fun formatTime(iso: String): String = try {
    val instant = Instant.parse(iso)
    val zoned = instant.atZone(ZoneId.systemDefault())
    DateTimeFormatter.ofPattern("HH:mm dd:MM:yyyy").format(zoned)
} catch (_: Exception) {
    iso
}