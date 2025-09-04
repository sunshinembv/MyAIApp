package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myaiapp.chat.presentation.ui_model.item.ReasoningTurnItem
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun ReasoningTurnItemDelegate(
    item: ReasoningTurnItem,
    modifier: Modifier = Modifier,
)  {
    Surface(
        color = MyAIAppTheme.colors.messageBackgroundColor,
        shape = MyAIAppTheme.shapes.cornersStyle
    ) {
        Column(modifier = modifier.padding(12.dp)) {
            Text("Q: ${item.question}", style = MyAIAppTheme.typography.body)
            Spacer(Modifier.height(8.dp))
            Text("🤔 THINK", style = MyAIAppTheme.typography.caption)
            item.think.thinkBullets.forEach { bullet ->
                Text("• $bullet", style = MyAIAppTheme.typography.caption)
            }
            if (item.think.claims.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Тезисы:", style = MyAIAppTheme.typography.body)
                item.think.claims.forEach { Text("— $it") }
            }

            Spacer(Modifier.height(8.dp))
            Text("🔎 VERIFY (${item.verify.score}/100)", style = MyAIAppTheme.typography.caption)
            item.verify.checked.forEach {
                val icon = if (it.passed) "✅" else "❌"
                Text("$icon ${it.claim}${it.note?.let { n -> " — $n" } ?: ""}")
            }

            Spacer(Modifier.height(8.dp))
            Text("💬 ANSWER", style = MyAIAppTheme.typography.body)
            Text(item.finalAnswer, style = MyAIAppTheme.typography.heading2)
        }
    }
}