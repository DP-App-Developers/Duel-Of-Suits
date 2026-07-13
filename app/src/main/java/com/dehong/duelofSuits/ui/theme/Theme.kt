package com.dehong.duelofSuits.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ActionGreen,
    secondary = TableGreenLight,
    tertiary = SelectedBorder,
    background = TableGreen,
    surface = TableGreenLight,
    onPrimary = CardFace,
    onSecondary = CardFace,
    onBackground = TextOnDark,
    onSurface = TextOnDark,
    error = DangerRed
)

@Composable
fun DuelOfSuitsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
