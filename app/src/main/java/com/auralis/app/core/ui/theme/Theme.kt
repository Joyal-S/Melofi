package com.auralis.app.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = AuralisPrimary,
    onPrimary = AuralisOnPrimary,
    primaryContainer = AuralisPrimaryContainer,
    onPrimaryContainer = AuralisOnPrimaryContainer,
    secondary = AuralisSecondary,
    onSecondary = AuralisOnSecondary,
    secondaryContainer = AuralisSecondaryContainer,
    onSecondaryContainer = AuralisOnSecondaryContainer,
    tertiary = AuralisTertiary,
    onTertiary = AuralisOnTertiary,
    tertiaryContainer = AuralisTertiaryContainer,
    onTertiaryContainer = AuralisOnTertiaryContainer,
    background = AuralisBackground,
    onBackground = AuralisOnBackground,
    surface = AuralisSurface,
    onSurface = AuralisOnSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = AuralisPrimaryDark,
    onPrimary = AuralisOnPrimaryDark,
    primaryContainer = AuralisPrimaryContainerDark,
    onPrimaryContainer = AuralisOnPrimaryContainerDark,
    secondary = AuralisSecondaryDark,
    onSecondary = AuralisOnSecondaryDark,
    secondaryContainer = AuralisSecondaryContainerDark,
    onSecondaryContainer = AuralisOnSecondaryContainerDark,
    tertiary = AuralisTertiaryDark,
    onTertiary = AuralisOnTertiaryDark,
    tertiaryContainer = AuralisTertiaryContainerDark,
    onTertiaryContainer = AuralisOnTertiaryContainerDark,
    background = AuralisBackgroundDark,
    onBackground = AuralisOnBackgroundDark,
    surface = AuralisSurfaceDark,
    onSurface = AuralisOnSurfaceDark,
)

/**
 * Auralis application theme.
 *
 * Uses Material 3 dynamic color on Android 12+ (API 31+),
 * falling back to the custom Auralis palette on older devices.
 */
@Composable
fun AuralisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        typography = AuralisTypography,
        content = content,
    )
}
