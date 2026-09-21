package com.auralis.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Builds an enhanced Light ColorScheme based on the selected accent.
 */
fun buildLightColorScheme(accent: MelofyAccent): ColorScheme {
    return lightColorScheme(
        primary = accent.primaryLight,
        onPrimary = Color.White,
        primaryContainer = accent.containerLight,
        onPrimaryContainer = accent.primaryLight,
        secondary = Color(0xFF475569),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFF1F5F9),
        onSecondaryContainer = Color(0xFF0F172A),
        tertiary = Color(0xFF0D9488),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCCFBF1),
        onTertiaryContainer = Color(0xFF115E59),
        background = MelofyBackgroundLight,
        onBackground = Color(0xFF0F172A),
        surface = MelofySurfaceLight,
        onSurface = Color(0xFF0F172A),
        surfaceVariant = MelofySurfaceVariantLight,
        onSurfaceVariant = Color(0xFF475569),
        surfaceContainer = MelofySurfaceContainerLight,
        surfaceContainerHigh = MelofySurfaceContainerHighLight,
        surfaceContainerHighest = MelofySurfaceContainerHighestLight,
        outline = MelofyOutlineLight,
        outlineVariant = MelofyOutlineVariantLight
    )
}

/**
 * Builds an enhanced Dark ColorScheme with rich obsidian/slate surfaces and vibrant accent.
 */
fun buildDarkColorScheme(accent: MelofyAccent): ColorScheme {
    return darkColorScheme(
        primary = accent.primaryDark,
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = accent.containerDark,
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF94A3B8),
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = Color(0xFF1E293B),
        onSecondaryContainer = Color(0xFFF1F5F9),
        tertiary = Color(0xFF2DD4BF),
        onTertiary = Color(0xFF042F2C),
        tertiaryContainer = Color(0xFF115E59),
        onTertiaryContainer = Color(0xFFCCFBF1),
        background = MelofyBackgroundDark,
        onBackground = Color(0xFFF8FAFC),
        surface = MelofySurfaceDark,
        onSurface = Color(0xFFF8FAFC),
        surfaceVariant = MelofySurfaceVariantDark,
        onSurfaceVariant = Color(0xFF94A3B8),
        surfaceContainer = MelofySurfaceContainerDark,
        surfaceContainerHigh = MelofySurfaceContainerHighDark,
        surfaceContainerHighest = MelofySurfaceContainerHighestDark,
        outline = MelofyOutlineDark,
        outlineVariant = MelofyOutlineVariantDark
    )
}

/**
 * Builds a true AMOLED Pure Black ColorScheme for OLED battery optimization.
 */
fun buildAmoledColorScheme(accent: MelofyAccent): ColorScheme {
    return darkColorScheme(
        primary = accent.primaryDark,
        onPrimary = Color(0xFF1E1B4B),
        primaryContainer = accent.containerDark,
        onPrimaryContainer = Color(0xFFEDE9FE),
        secondary = Color(0xFF94A3B8),
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = Color(0xFF1E293B),
        onSecondaryContainer = Color(0xFFF1F5F9),
        tertiary = Color(0xFF2DD4BF),
        onTertiary = Color(0xFF042F2C),
        tertiaryContainer = Color(0xFF115E59),
        onTertiaryContainer = Color(0xFFCCFBF1),
        background = MelofyBackgroundAmoled,
        onBackground = Color(0xFFEDEDED),
        surface = MelofySurfaceAmoled,
        onSurface = Color(0xFFEDEDED),
        surfaceVariant = MelofySurfaceVariantAmoled,
        onSurfaceVariant = Color(0xFFA0A0A0),
        surfaceContainer = MelofySurfaceContainerAmoled,
        surfaceContainerHigh = MelofySurfaceContainerHighAmoled,
        surfaceContainerHighest = MelofySurfaceContainerHighestAmoled,
        outline = MelofyOutlineAmoled,
        outlineVariant = Color(0xFF1F1F1F)
    )
}

val LightColorScheme: ColorScheme = buildLightColorScheme(MelofyAccent.VIOLET)
val DarkColorScheme: ColorScheme = buildDarkColorScheme(MelofyAccent.VIOLET)
val AmoledColorScheme: ColorScheme = buildAmoledColorScheme(MelofyAccent.VIOLET)

/**
 * Melofy application theme.
 * Supports light, dark, pure AMOLED black modes, user-selected accent palettes,
 * and adaptive motion accessibility.
 */
@Composable
fun AuralisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
    accent: MelofyAccent = MelofyAccent.VIOLET,
    animationMode: MelofyAnimationMode = MelofyAnimationMode.FULL,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme && amoledBlack -> buildAmoledColorScheme(accent)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> buildDarkColorScheme(accent)
        else -> buildLightColorScheme(accent)
    }

    CompositionLocalProvider(
        LocalAnimationMode provides animationMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AuralisTypography,
            content = content,
        )
    }
}
