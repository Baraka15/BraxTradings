package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.material3.lightColorScheme

private val DarkColorScheme = darkColorScheme(
    primary = TvBlue,
    secondary = TvGreen,
    tertiary = TvRed,
    background = TvBackground,
    surface = TvPanel,
    surfaceVariant = TvBackground,
    onPrimary = TvTextPrimary,
    onSecondary = TvTextPrimary,
    onBackground = TvTextPrimary,
    onSurface = TvTextPrimary,
    onSurfaceVariant = TvTextSecondary,
    outline = TvDivider,
    error = TvRed
)

private val LightColorScheme = lightColorScheme(
    primary = TvBlue,
    secondary = TvGreen,
    tertiary = TvRed,
    background = TvLightBackground,
    surface = TvLightPanel,
    surfaceVariant = TvLightBackground,
    onPrimary = TvLightTextPrimary,
    onSecondary = TvLightTextPrimary,
    onBackground = TvLightTextPrimary,
    onSurface = TvLightTextPrimary,
    onSurfaceVariant = TvLightTextSecondary,
    outline = TvLightDivider,
    error = TvRed
)

@Composable
fun BraxTradingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) TvBackground.toArgb() else TvLightBackground.toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
