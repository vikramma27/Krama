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

private val DarkColorScheme = darkColorScheme(
    primary = FlowPrimary,
    onPrimary = Color.White,
    primaryContainer = FlowPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = FlowAccentTeal,
    onSecondary = Color.Black,
    secondaryContainer = DarkPlumBorder,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = FlowEncryptedGreen,
    background = NearBlackPlum,
    onBackground = TextPrimaryDark,
    surface = DarkPlumCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkPlumBorder,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkPlumBorder,
    error = FlowCoralWarning
)

private val LightColorScheme = lightColorScheme(
    primary = FlowPrimary,
    onPrimary = Color.White,
    primaryContainer = SentBubbleLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = FlowPrimary,
    onSecondary = Color.White,
    secondaryContainer = LightBorder,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = FlowEncryptedGreen,
    background = WarmOffWhite,
    onBackground = TextPrimaryLight,
    surface = LightCardSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightBorder,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorder,
    error = FlowCoralWarning
)

@Composable
fun KramaTheme(
    darkTheme: Boolean = true, // Default to dark mode
    dynamicColor: Boolean = false, // Set to false to preserve Krama's custom theme identity and high-contrast dark theme
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



