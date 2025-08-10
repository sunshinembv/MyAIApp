package com.example.myaiapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val lightColorPalette = MyAIAppColors(
    appBarColor = theme_light_blue_yonder,
    tabSelectedColor = theme_light_white,
    tabNotSelectedColor = theme_light_azureish_white,
    floatingActionButtonColor = theme_light_blue_steel,
    drawerTitleColor = theme_light_silver_lake_blue,
    drawerColor = theme_light_white,
    buttonColor = theme_light_blue_jeans,
    outlinedEditTextColor = theme_light_blue_jeans,
    messageBackgroundColor = theme_light_white,
    ownMessageBackgroundColor = theme_light_nyanza,
    primaryTextColor = theme_light_primary_text_color,
    secondaryTextColor = theme_light_secondary_text_color,
    hintColor = theme_light_hint_color,
    iconTintColor = theme_light_roman_silver,
    contentColor = theme_light_white,
    backgroundColor = theme_light_background,
    logoProgressBarColor = theme_light_logo_progress_bar,
    chatBackgroundColor = theme_light_chat_background,
    ownReactionBackgroundColor = theme_light_own_reaction_background,
    userReactionBackgroundColor = theme_light_user_reaction_background,
    dateBackgroundColor = theme_light_roman_silver,
    shimmerColor = theme_light_shimmer,
    userNameColor = theme_light_user_name,
    ownMessageTimeColor = theme_light_own_message_time,
    userMessageTimeColor = theme_light_user_message_time,
    errorColor = theme_light_error,
)

private val darkColorPalette = MyAIAppColors(
    appBarColor = theme_dark_gunmetal,
    tabSelectedColor = theme_dark_french_sky_blue,
    tabNotSelectedColor = theme_dark_cool_grey,
    floatingActionButtonColor = theme_dark_carolina_blue,
    drawerTitleColor = theme_dark_gunmetal_midnight,
    drawerColor = theme_dark_dark_gunmetal,
    buttonColor = theme_dark_blue_jeans,
    outlinedEditTextColor = theme_dark_blue_jeans,
    messageBackgroundColor = theme_dark_gunmetal,
    ownMessageBackgroundColor = theme_dark_royal_purple,
    primaryTextColor = theme_dark_primary_text_color,
    secondaryTextColor = theme_dark_secondary_text_color,
    hintColor = theme_dark_hint_color,
    iconTintColor = theme_dark_roman_silver,
    contentColor = theme_dark_white,
    backgroundColor = theme_dark_background,
    logoProgressBarColor = theme_dark_logo_progress_bar,
    chatBackgroundColor = theme_dark_chat_background,
    ownReactionBackgroundColor = theme_dark_own_reaction_background,
    userReactionBackgroundColor = theme_dark_user_reaction_background,
    dateBackgroundColor = theme_dark_roman_silver,
    shimmerColor = theme_dark_shimmer,
    userNameColor = theme_dark_user_name,
    ownMessageTimeColor = theme_dark_own_message_time,
    userMessageTimeColor = theme_dark_user_message_time,
    errorColor = theme_dark_error,
)

@Composable
fun MyAIAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        darkColorPalette
    } else {
        lightColorPalette
    }

    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = if (darkTheme) darkColorPalette.appBarColor else lightColorPalette.appBarColor,
            darkIcons = !darkTheme
        )
    }

    CompositionLocalProvider(
        LocalMyAIAppColors provides colors,
        LocalMyAIAppTypography provides typography,
        LocalMyAIAppShape provides shapes,
        content = content
    )
}