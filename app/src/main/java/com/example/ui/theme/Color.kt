package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Immersive UI Design Palette
val ImmersiveBackground = Color(0xFFFFFFFF)
val ImmersiveSurface = Color(0xFFFAFAFA)
val ImmersiveCardSurface = Color(0xFFF8FAFC) // Slate 50
val ImmersiveBorder = Color(0xFFE2E8F0)      // Slate 200
val ImmersiveTextPrimary = Color(0xFF0F172A) // Slate 900
val ImmersiveTextSecondary = Color(0xFF64748B) // Slate 500
val ImmersiveTextTertiary = Color(0xFF94A3B8)  // Slate 400
val ImmersivePrimary = Color(0xFFEF4444)     // Red 500 primary (from Immersive design HTML)
val ImmersiveAccent = Color(0xFF10B981)      // Green online indicator

val ImmersiveActiveGradient = listOf(
    Color(0xFFFBBF24), // Yellow 400
    Color(0xFFEF4444), // Red 500
    Color(0xFF8B5CF6)  // Purple 600
)

data class TrooperColors(
    val orange: Color,
    val black: Color,
    val darkGray: Color,
    val border: Color,
    val gray: Color,
    val lightGray: Color,
    val accent: Color
)

val LocalTrooperColors = staticCompositionLocalOf {
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

// Backward compatibility aliases mapped to Immersive UI
val TrooperOrange: Color
    @Composable get() = LocalTrooperColors.current.orange

val TrooperBlack: Color
    @Composable get() = LocalTrooperColors.current.black

val TrooperDarkGray: Color
    @Composable get() = LocalTrooperColors.current.darkGray

val TrooperBorder: Color
    @Composable get() = LocalTrooperColors.current.border

val TrooperGray: Color
    @Composable get() = LocalTrooperColors.current.gray

val TrooperLightGray: Color
    @Composable get() = LocalTrooperColors.current.lightGray

val TrooperAccent: Color
    @Composable get() = LocalTrooperColors.current.accent


