package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Enforce Dark Theme first for BraxTradings cinematic look
private val BraxDarkColorScheme = darkColorScheme(
    primary = BraxElectricBlue,
    secondary = BraxGreen,
    tertiary = BraxRed,
    background = BraxBackground,
    surface = BraxPanel,
    surfaceVariant = BraxBackground,
    onPrimary = BraxBackground,
    onSecondary = BraxBackground,
    onBackground = BraxTextPrimary,
    onSurface = BraxTextPrimary,
    onSurfaceVariant = BraxTextSecondary,
    outline = BraxDivider,
    error = BraxRed
)

private val BraxLightColorScheme = lightColorScheme(
    primary = BraxElectricBlue,
    secondary = BraxGreen,
    tertiary = BraxRed,
    background = BraxLightBackground,
    surface = BraxLightPanel,
    surfaceVariant = BraxLightBackground,
    onPrimary = BraxLightBackground,
    onSecondary = BraxLightBackground,
    onBackground = BraxLightTextPrimary,
    onSurface = BraxLightTextPrimary,
    onSurfaceVariant = BraxLightTextSecondary,
    outline = BraxLightDivider,
    error = BraxRed
)

@Composable
fun BraxTradingsTheme(
    darkTheme: Boolean = true, // Force Dark Theme as primary per Master Prompt
    content: @Composable () -> Unit
) {
    // We override system theme to strictly enforce the cinematic dark mode
    val colorScheme = if (darkTheme) BraxDarkColorScheme else BraxLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (darkTheme) BraxBackground.toArgb() else BraxLightBackground.toArgb()
            
            // Handle deprecated status bar color setters safely or just use modern edge-to-edge
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
