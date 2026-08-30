package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Krama Modern Design System Theme Implementation
 * Provides a distinctive, premium theme that sets Krama apart from other chat apps
 */

private val DarkColorScheme = darkColorScheme(
    primary = QuantumTeal,
    onPrimary = TextOnPrimary,
    primaryContainer = QuantumTeal.copy(alpha = 0.1f),
    onPrimaryContainer = TextOnPrimary,
    secondary = DeepPlasmaPurple,
    onSecondary = TextOnPrimary,
    secondaryContainer = DeepPlasmaPurple.copy(alpha = 0.1f),
    onSecondaryContainer = TextOnPrimary,
    tertiary = StellarCoral,
    onTertiary = TextOnPrimary,
    background = NearBlackPlum,
    onBackground = TextOnBackground,
    surface = SurfaceDark,
    onSurface = TextOnBackground,
    surfaceVariant = SurfaceDark.copy(alpha = 0.8f),
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = VolcanicRed
)

private val LightColorScheme = lightColorScheme(
    primary = QuantumTeal,
    onPrimary = TextOnPrimary,
    primaryContainer = QuantumTeal.copy(alpha = 0.1f),
    onPrimaryContainer = TextOnPrimary,
    secondary = DeepPlasmaPurple,
    onSecondary = TextOnPrimary,
    secondaryContainer = DeepPlasmaPurple.copy(alpha = 0.1f),
    onSecondaryContainer = TextOnPrimary,
    tertiary = StellarCoral,
    onTertiary = TextOnPrimary,
    background = WhiteOak,
    onBackground = TextOnBackground,
    surface = SurfaceLight,
    onSurface = TextOnBackground,
    surfaceVariant = SurfaceLight.copy(alpha = 0.9f),
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = VolcanicRed
)

@Composable
fun KramaTheme(
    darkTheme: Boolean = true, // Default to dark mode for premium feel
    dynamicColor: Boolean = false, // Set to false to preserve Krama's custom theme identity
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}