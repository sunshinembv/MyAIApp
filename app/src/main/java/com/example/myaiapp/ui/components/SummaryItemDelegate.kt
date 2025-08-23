package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myaiapp.R
import com.example.myaiapp.chat.presentation.ui_model.item.SummaryItem
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun SummaryItemDelegate(
    item: SummaryItem,
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
            Column(modifier = modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.subtitle,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = item.summary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Normal
                )
            }
        }
    }
}
