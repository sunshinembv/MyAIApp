package com.example.myaiapp.ui.components.basic

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.myaiapp.R
import com.example.myaiapp.ui.theme.MyAIAppTheme

@Composable
fun AppTopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    navigationIcon: AppTopAppBarIconItem? = null,
    actions: List<AppTopAppBarIconItem>? = null,
    backgroundColor: Color = MyAIAppTheme.colors.appBarColor,
    contentColor: Color = MyAIAppTheme.colors.contentColor,
    elevation: Dp = dimensionResource(id = R.dimen.indent_12dp)
) {
    TopAppBar(
        title = {
            title?.let {
                Text(
                    text = title,
                    style = MyAIAppTheme.typography.toolbar
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            navigationIcon?.let {
                IconButton(onClick = navigationIcon.onClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = navigationIcon.iconId),
                        contentDescription = navigationIcon.contentDescription
                    )
                }
            }
        },
        actions = {
            actions?.map { action ->
                IconButton(onClick = action.onClick) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = action.iconId),
                        contentDescription = action.contentDescription
                    )
                }
            }
        },
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    )
}

data class AppTopAppBarIconItem(
    @param:DrawableRes val iconId: Int,
    val contentDescription: String? = null,
    val onClick: () -> Unit
)

@Preview
@Composable
fun AppTopAppBarPreview() {
    MyAIAppTheme {
        AppTopAppBar(
            title = "TopAppBar",
            navigationIcon = AppTopAppBarIconItem(
                iconId = R.drawable.baseline_arrow_back_24,
            ) {

            },
            actions = listOf(
                AppTopAppBarIconItem(
                    iconId = R.drawable.baseline_arrow_back_24
                ) {

                }
            )
        )
    }
}