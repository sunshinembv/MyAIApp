package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.myaiapp.R
import com.example.myaiapp.chat.data.model.Summary
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun LlmSummary(
    agentName: String,
    pending: Boolean,
    response: Summary,
    modifier: Modifier = Modifier,
    color: Color = MyAIAppTheme.colors.messageBackgroundColor
) {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.indent_4dp)))
        Surface(
            color = color,
            shape = MyAIAppTheme.shapes.cornersStyle
        ) {
            ConstraintLayout(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.indent_10dp))
            ) {
                val (structuredId, progressId) = createRefs()

                val structuredModifier = if (pending) {
                    Modifier.constrainAs(structuredId) {
                        start.linkTo(progressId.end, margin = 8.dp)
                    }
                } else {
                    Modifier.constrainAs(structuredId) {
                        start.linkTo(parent.start)
                    }
                }

                if (pending) {
                    SummaryViewPlaceholder(
                        modifier = structuredModifier
                    )
                } else {
                    SummaryView(
                        agentName = agentName,
                        modifier = structuredModifier,
                        response = response
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryViewPlaceholder(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Surface(
            color = MyAIAppTheme.colors.primaryTextColor.copy(alpha = 0.15f),
            shape = MyAIAppTheme.shapes.cornersStyle
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MyAIAppTheme.colors.primaryTextColor.copy(alpha = 0.15f),
            shape = MyAIAppTheme.shapes.cornersStyle
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            color = MyAIAppTheme.colors.primaryTextColor.copy(alpha = 0.15f),
            shape = MyAIAppTheme.shapes.cornersStyle
        ) {
            Spacer(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp)
            )
        }
    }
}
