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

private val LightColorScheme = lightColorScheme(
    primary = ImmersivePrimary,
    secondary = ImmersiveTextSecondary,
    tertiary = ImmersiveAccent,
    background = ImmersiveBackground,
    surface = ImmersiveBackground,
    onBackground = ImmersiveTextPrimary,
    onSurface = ImmersiveTextPrimary,
    onPrimary = Color.White,
    surfaceVariant = ImmersiveCardSurface,
    onSurfaceVariant = ImmersiveTextSecondary,
    outline = ImmersiveBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val trooperColors = if (darkTheme) {
        TrooperColors(
            orange = Color(0xFFEF4444),       // Bright Orange/Red Accent
            black = Color(0xFF0F172A),        // Deep Navy Slate 900
            darkGray = Color(0xFF1E293B),     // Slate 800
            border = Color(0xFF334155),       // Slate 700
            gray = Color(0xFF94A3B8),         // Slate 400
            lightGray = Color(0xFF64748B),    // Slate 500
            accent = Color(0xFF10B981)        // Bright Green
        )
    } else {
        TrooperColors(
            orange = Color(0xFFEF4444),
            black = Color(0xFFFFFFFF),
            darkGray = Color(0xFFF8FAFC),
            border = Color(0xFFE2E8F0),
            gray = Color(0xFF64748B),
            lightGray = Color(0xFF94A3B8),
            accent = Color(0xFF10B981)
        )
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = trooperColors.orange,
            secondary = trooperColors.gray,
            tertiary = trooperColors.accent,
            background = trooperColors.black,
            surface = trooperColors.darkGray,
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onPrimary = Color.White,
            surfaceVariant = trooperColors.darkGray,
            onSurfaceVariant = trooperColors.lightGray,
            outline = trooperColors.border
        )
    } else {
        lightColorScheme(
            primary = trooperColors.orange,
            secondary = trooperColors.gray,
            tertiary = trooperColors.accent,
            background = trooperColors.black,
            surface = trooperColors.darkGray,
            onBackground = Color(0xFF0F172A),
            onSurface = Color(0xFF0F172A),
            onPrimary = Color.White,
            surfaceVariant = trooperColors.darkGray,
            onSurfaceVariant = trooperColors.lightGray,
            outline = trooperColors.border
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalTrooperColors provides trooperColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
