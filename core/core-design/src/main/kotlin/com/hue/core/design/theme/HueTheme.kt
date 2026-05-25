package com.hue.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ── Brand accent colours (achromatic chrome — only swatches bring colour) ─────

val HueLightColorScheme = lightColorScheme(
    primary            = Color(0xFF1A1A1A),
    onPrimary          = Color(0xFFF8F5F2),
    primaryContainer   = Color(0xFFF0EDE8),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary          = Color(0xFF5A5652),
    onSecondary        = Color(0xFFF8F5F2),
    tertiary           = Color(0xFF8A8580),
    background         = Color(0xFFFAF8F5),
    onBackground       = Color(0xFF1A1A1A),
    surface            = Color(0xFFFAF8F5),
    onSurface          = Color(0xFF1A1A1A),
    surfaceVariant     = Color(0xFFEEEBE6),
    onSurfaceVariant   = Color(0xFF4A4642),
    outline            = Color(0xFFB0ABA6),
    outlineVariant     = Color(0xFFD6D1CC),
    error              = Color(0xFFBA1A1A),
    onError            = Color.White,
    errorContainer     = Color(0xFFFFDAD6),
    onErrorContainer   = Color(0xFF410002)
)

val HueDarkColorScheme = darkColorScheme(
    primary            = Color(0xFFECE8E3),
    onPrimary          = Color(0xFF2A2826),
    primaryContainer   = Color(0xFF3A3835),
    onPrimaryContainer = Color(0xFFECE8E3),
    secondary          = Color(0xFFBBB6B1),
    onSecondary        = Color(0xFF2A2826),
    tertiary           = Color(0xFF8A8580),
    background         = Color(0xFF1A1816),
    onBackground       = Color(0xFFECE8E3),
    surface            = Color(0xFF1A1816),
    onSurface          = Color(0xFFECE8E3),
    surfaceVariant     = Color(0xFF2E2C2A),
    onSurfaceVariant   = Color(0xFFBBB6B1),
    outline            = Color(0xFF5A5652),
    outlineVariant     = Color(0xFF3A3835),
    error              = Color(0xFFFFB4AB),
    onError            = Color(0xFF690005),
    errorContainer     = Color(0xFF93000A),
    onErrorContainer   = Color(0xFFFFDAD6)
)

// Season accent colours — used ONLY for badges/accents, never for chrome
object SeasonColors {
    val Spring = Color(0xFFF4A261)   // warm peach
    val Summer = Color(0xFFA8C5DA)   // cool powder blue
    val Autumn = Color(0xFFC47A3A)   // warm amber
    val Winter = Color(0xFF5B6FA6)   // cool indigo
    val Neutral = Color(0xFF9E9E9E)
}

val LocalHueDarkTheme = staticCompositionLocalOf { false }

@Composable
fun HueTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Seed colour from identified PANTONE for dynamic theming (optional)
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        seedColor != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Dynamic theme seeded from the identified fabric colour
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> HueDarkColorScheme
        else      -> HueLightColorScheme
    }

    CompositionLocalProvider(LocalHueDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = HueTypography,
            shapes      = HueShapes,
            content     = content
        )
    }
}
