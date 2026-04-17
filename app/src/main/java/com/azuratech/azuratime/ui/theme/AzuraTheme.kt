package com.azuratech.azuratime.ui.theme

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ==========================================
// 1. AZURA COLORS (The Brand DNA - Beautified)
// ==========================================

// Light Mode (Inspirasi: WhatsApp & Material 3)
val AzuraPrimary = Color(0xFF006766) // Teal WhatsApp yang lebih tajam
val AzuraSecondary = Color(0xFF435B5B) // Muted Teal (Gemini vibe)
val AzuraBackgroundLight = Color(0xFFFBFDFA) // Off-white sedikit kehijauan (Fresh)
val AzuraSurfaceLight = Color(0xFFFFFFFF)

// Dark Mode (Inspirasi: Gemini Dark)
val AzuraPrimaryDark = Color(0xFF4DB6AC) // Minty Teal
val AzuraSurfaceDark = Color(0xFF1A1C1E) // Deep Charcoal (Bukan hitam pekat, lebih modern)
val AzuraBackgroundDark = Color(0xFF101214) // Hampir Hitam

// Warna Status
val AzuraSuccess = Color(0xFF2E7D32) // Hijau Daun (Natural)
val AzuraWarning = Color(0xFFF57C00) // Oranye Hangat
val AzuraError = Color(0xFFB00020)   // Deep Red

private val AzuraLightColorScheme = lightColorScheme(
    primary = AzuraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E8E6), // Soft Teal Container
    onPrimaryContainer = Color(0xFF002020),
    secondary = AzuraSecondary,
    background = AzuraBackgroundLight,
    surface = AzuraSurfaceLight,
    surfaceVariant = Color(0xFFF0F4F3), // Gemini-style subtle gray-teal
    outline = Color(0xFF707978),
    error = AzuraError
)

private val AzuraDarkColorScheme = darkColorScheme(
    primary = AzuraPrimaryDark,
    onPrimary = Color(0xFF003736),
    primaryContainer = Color(0xFF00504E),
    secondary = AzuraSecondary,
    background = AzuraBackgroundDark,
    surface = AzuraSurfaceDark,
    surfaceVariant = Color(0xFF3F4948), // Dark Mode Greyish Teal
    onSurface = Color(0xFFE1E3E2),
    error = AzuraError
)

// Rahasia Kecantikan: "The Gradient Touch" 💎
object AzuraGradients {
    @Composable
    fun primaryGradient() = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )

    @Composable
    fun surfaceGradient() = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
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
    content: @Composable () -> Unit
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
            @Suppress("DEPRECATION") window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Ensure your Typography.kt remains in the theme folder
        shapes = Shapes(
            small = AzuraShapes.small,
            medium = AzuraShapes.medium,
            large = AzuraShapes.large
        ),
        content = content
    )
}