package com.azuratech.azuratime.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ==========================================
// 1. AZURA COLORS (The Brand DNA - Beautified)
// ==========================================

// Light Mode (Inspirasi: Premium Enterprise & High-Tech)
val AzuraPrimary = Color(0xFF1E3A8A) // Deep Navy Azure (Strong & Authoritative)
val AzuraSecondary = Color(0xFF3B82F6) // Cobalt Blue (Modern Accent)
val AzuraBackgroundLight = Color(0xFFF8FAFC) // Clean Slate Background
val AzuraSurfaceLight = Color(0xFFFFFFFF)

// Dark Mode (Inspirasi: Midnight Modern)
val AzuraPrimaryDark = Color(0xFF93C5FD) // Light Sky Azure (Better contrast for dark)
val AzuraSecondaryDark = Color(0xFF60A5FA)
val AzuraSurfaceDark = Color(0xFF1E293B) // Slate Blue-Gray Surface
val AzuraBackgroundDark = Color(0xFF0F172A) // Midnight Azure Background

// Warna Status
val AzuraSuccess = Color(0xFF10B981) // Emerald Green
val AzuraWarning = Color(0xFFF59E0B) // Amber Warning
val AzuraError = Color(0xFFEF4444) // Vibrant Red

private val AzuraLightColorScheme = lightColorScheme(
    primary = AzuraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), // Soft Blue Container
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = AzuraSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF6FF),
    background = AzuraBackgroundLight,
    surface = AzuraSurfaceLight,
    surfaceVariant = Color(0xFFF1F5F9), // Slate Variant
    outline = Color(0xFF94A3B8),
    error = AzuraError,
)

private val AzuraDarkColorScheme = darkColorScheme(
    primary = AzuraPrimaryDark,
    onPrimary = Color(0xFF1E3A8A),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = AzuraSecondaryDark,
    onSecondary = Color(0xFF1E3A8A),
    background = AzuraBackgroundDark,
    surface = AzuraSurfaceDark,
    surfaceVariant = Color(0xFF334155),
    onSurface = Color(0xFFF1F5F9),
    error = AzuraError,
)

// Rahasia Kecantikan: "The Gradient Touch" 💎
object AzuraGradients {
    @Composable
    fun primaryGradient() = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
        ),
    )

    @Composable
    fun surfaceGradient() = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    )
}

// ==========================================
// 2. AZURA TOKENS (Spacing & Shapes)
// ==========================================
object AzuraSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object AzuraShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(24.dp)
}

// ==========================================
// 3. THE MASTER THEME WRAPPER
// ==========================================
@Composable
fun AzuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Forced false to ensure Azura brand identity across all devices
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AzuraDarkColorScheme
        else -> AzuraLightColorScheme
    }

    // Smooth status bar integration
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure your Typography.kt remains in the theme folder
        shapes = Shapes(
            small = AzuraShapes.small,
            medium = AzuraShapes.medium,
            large = AzuraShapes.large,
        ),
        content = content,
    )
}
