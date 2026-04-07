package com.poppost.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CoralRed,
    secondary = SunsetOrange,
    background = Cream,
    surface = Sand,
    surfaceContainerHighest = Mist,
    onPrimary = Cream,
    onSecondary = Graphite,
    onBackground = Graphite,
    onSurface = Graphite,
    onSurfaceVariant = Graphite.copy(alpha = 0.76f),
)

private val DarkColors = darkColorScheme(
    primary = SunsetOrange,
    secondary = CoralRed,
    background = Graphite,
    surface = Color(0xFF11161C),
    surfaceContainerHighest = Color(0xFF202A33),
    onPrimary = Graphite,
    onSecondary = Cream,
    onBackground = Cream,
    onSurface = Cream,
    onSurfaceVariant = Cream.copy(alpha = 0.72f),
)

@Composable
fun PopPostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PopPostTypography,
        content = content,
    )
}
