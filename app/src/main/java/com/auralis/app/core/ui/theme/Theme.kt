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
import androidx.compose.ui.platform.LocalContext

val LightColorScheme: ColorScheme = lightColorScheme(
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

val DarkColorScheme: ColorScheme = darkColorScheme(
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

val AmoledColorScheme: ColorScheme = darkColorScheme(
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
    background = AuralisBackgroundAmoled,
    onBackground = AuralisOnBackgroundAmoled,
    surface = AuralisSurfaceAmoled,
    onSurface = AuralisOnSurfaceAmoled,
    surfaceVariant = AuralisSurfaceVariantAmoled,
    onSurfaceVariant = AuralisOnSurfaceVariantAmoled
)

/**
 * Auralis application theme.
 * Supports light, dark, and pure AMOLED black modes,
 * along with dynamic color on Android 12+ where appropriate.
 */
@Composable
fun AuralisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme && amoledBlack -> AmoledColorScheme
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
