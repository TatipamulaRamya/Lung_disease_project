package com.respiratoryai.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE8FF),
    onPrimaryContainer = NightBlue,
    secondary = NightBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF4FF),
    onSecondaryContainer = NightBlue,
    background = Mist,
    onBackground = NightBlue,
    surface = ColorTokens.Surface,
    onSurface = NightBlue,
    surfaceContainer = ColorTokens.SurfaceContainer,
    surfaceContainerHigh = ColorTokens.SurfaceContainerHigh,
    surfaceVariant = SurfaceLight,
    errorContainer = Coral,
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = NightBlue,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = Color.White,
    secondary = SurfaceLight,
    onSecondary = NightBlue,
    secondaryContainer = NightBlue,
    onSecondaryContainer = Color.White,
    background = NightBlue,
    onBackground = Mist,
    surface = ColorTokens.DarkSurface,
    onSurface = Mist,
    surfaceContainer = ColorTokens.DarkSurfaceContainer,
    surfaceContainerHigh = ColorTokens.DarkSurfaceContainerHigh,
    surfaceVariant = SurfaceBlue,
    errorContainer = ColorTokens.DarkError,
)

@Composable
fun RespiratoryAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

private object ColorTokens {
    val Surface = Color.White
    val SurfaceContainer = Color(0xFFFFFFFF)
    val SurfaceContainerHigh = Color(0xFFF4F8FF)
    val DarkSurface = Color(0xFF0A163A)
    val DarkSurfaceContainer = Color(0xFF10224F)
    val DarkSurfaceContainerHigh = Color(0xFF16306F)
    val DarkError = Color(0xFF7B2E2E)
}
