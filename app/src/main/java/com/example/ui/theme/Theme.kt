package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MintGreenPrimary,
    onPrimary = Color(0xFF042F1A),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = MintGreenLight,
    secondary = MintAccent,
    onSecondary = Color(0xFF042F1A),
    secondaryContainer = Color(0xFF132E22),
    onSecondaryContainer = MintAccent,
    tertiary = TerminalCyan,
    onTertiary = Color(0xFF083344),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E293B),
    error = DangerRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MintGreenDark,
    onPrimary = Color.White,
    primaryContainer = MintGreenLight,
    onPrimaryContainer = Color.Black,
    secondary = MintAccent,
    onSecondary = Color.Black,
    tertiary = InfoBlue,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun OmniDeskTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode suited for remote control
    dynamicColor: Boolean = false, // Keep consistent cyber/dark branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
