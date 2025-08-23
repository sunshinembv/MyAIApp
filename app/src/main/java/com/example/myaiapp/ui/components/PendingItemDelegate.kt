package com.example.myaiapp.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.example.myaiapp.R
import com.example.myaiapp.chat.presentation.ui_model.item.PendingItem
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun PendingItemDelegate(
    item: PendingItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MyAIAppTheme.colors.messageBackgroundColor,
        shape = MyAIAppTheme.shapes.cornersStyle
    ) {
        Row(
            modifier = modifier.padding(dimensionResource(id = R.dimen.indent_10dp)),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.indent_16dp)),
                color = MyAIAppTheme.colors.primaryTextColor,
                strokeWidth = dimensionResource(id = R.dimen.indent_2dp)
            )
            Spacer(Modifier.size(dimensionResource(id = R.dimen.indent_4dp)))
            Text(
                text = item.text,
                color = MyAIAppTheme.colors.primaryTextColor
            )
        }
    }
}
