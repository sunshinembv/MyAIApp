package com.example.myaiapp.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

data class MyAIAppColors(
    val appBarColor: Color,
    val tabSelectedColor: Color,
    val tabNotSelectedColor: Color,
    val floatingActionButtonColor: Color,
    val drawerTitleColor: Color,
    val drawerColor: Color,
    val buttonColor: Color,
    val outlinedEditTextColor: Color,
    val messageBackgroundColor: Color,
    val ownMessageBackgroundColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val hintColor: Color,
    val iconTintColor: Color,
    val contentColor: Color,
    val backgroundColor: Color,
    val ownReactionBackgroundColor: Color,
    val userReactionBackgroundColor: Color,
    val logoProgressBarColor: Color,
    val chatBackgroundColor: Color,
    val dateBackgroundColor: Color,
    val shimmerColor: Color,
    val userNameColor: Color,
    val ownMessageTimeColor: Color,
    val userMessageTimeColor: Color,
    val errorColor: Color
)

data class MyAIAppTypography(
    val heading1: TextStyle,
    val heading2: TextStyle,
    val body: TextStyle,
    val toolbar: TextStyle,
    val caption: TextStyle
)

data class MyAIAppShape(
    val padding: Dp,
    val cornersStyle: Shape,
    val topCornersStyle: Shape,
    val bottomCornersStyle: Shape,
    val dateCornersStyle: Shape,
    val reactionsCornersStyle: Shape,
    val shimmerCornersStyle: Shape,
)

data class MyAIAppImage(
    @param:DrawableRes val iconId: Int
)

object MyAIAppTheme {
    internal val colors: MyAIAppColors
        @Composable get() = LocalMyAIAppColors.current

    internal val typography: MyAIAppTypography
        @Composable get() = LocalMyAIAppTypography.current

    internal val shapes: MyAIAppShape
        @Composable get() = LocalMyAIAppShape.current

    internal val images: MyAIAppImage
        @Composable get() = LocalMyAIAppImage.current
}

internal val LocalMyAIAppColors = staticCompositionLocalOf<MyAIAppColors> {
    error("No colors provided")
}

internal val LocalMyAIAppTypography = staticCompositionLocalOf<MyAIAppTypography> {
    error("No font provided")
}

internal val LocalMyAIAppShape = staticCompositionLocalOf<MyAIAppShape> {
    error("No shapes provided")
}

internal val LocalMyAIAppImage = staticCompositionLocalOf<MyAIAppImage> {
    error("No images provided")
}