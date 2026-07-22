package com.dehong.duelofSuits.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

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
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = 1f)
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
