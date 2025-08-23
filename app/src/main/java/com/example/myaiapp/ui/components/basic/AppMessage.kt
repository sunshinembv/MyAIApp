package com.example.myaiapp.ui.components.basic

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.example.myaiapp.R
import com.example.myaiapp.ui.theme.MyAIAppTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppMessage(
    agentName: String? = null,
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color,
        shape = MyAIAppTheme.shapes.cornersStyle
    ) {
        ConstraintLayout(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.indent_10dp))
        ) {
            val (agentId, textId, progressId) = createRefs()
            if (agentName != null) {
                Text(
                    text = agentName,
                    modifier = Modifier.constrainAs(agentId) {
                        start.linkTo(progressId.end, margin = 8.dp)
                        bottom.linkTo(textId.top, margin = 4.dp)
                    },
                    color = MyAIAppTheme.colors.primaryTextColor
                )
            }
            Text(
                text = text,
                modifier = Modifier.constrainAs(textId) {
                    start.linkTo(progressId.end, margin = 8.dp)
                    top.linkTo(agentId.bottom)
                },
                color = MyAIAppTheme.colors.primaryTextColor
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun AppMessagePreview() {
    MyAIAppTheme {
        AppMessage(
            text = "Text",
            color = MyAIAppTheme.colors.messageBackgroundColor,
        )
    }
}
