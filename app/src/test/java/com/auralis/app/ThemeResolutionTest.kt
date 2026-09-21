package com.auralis.app

import androidx.compose.ui.graphics.Color
import com.auralis.app.core.ui.theme.AmoledColorScheme
import com.auralis.app.core.ui.theme.DarkColorScheme
import com.auralis.app.core.ui.theme.LightColorScheme
import com.auralis.app.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying theme mode resolution and AMOLED pure black scheme properties.
 */
class ThemeResolutionTest {

    private fun resolveColorScheme(
        themeMode: ThemeMode,
        systemDark: Boolean,
        amoledBlack: Boolean
    ) = when {
        themeMode == ThemeMode.DARK && amoledBlack -> AmoledColorScheme
        (themeMode == ThemeMode.SYSTEM && systemDark) && amoledBlack -> AmoledColorScheme
        themeMode == ThemeMode.DARK -> DarkColorScheme
        themeMode == ThemeMode.SYSTEM && systemDark -> DarkColorScheme
        else -> LightColorScheme
    }

    @Test
    fun lightMode_alwaysResolvesToLight() {
        assertEquals(LightColorScheme, resolveColorScheme(ThemeMode.LIGHT, systemDark = true, amoledBlack = false))
        assertEquals(LightColorScheme, resolveColorScheme(ThemeMode.LIGHT, systemDark = false, amoledBlack = false))
    }

    @Test
    fun darkMode_resolvesToDarkOrAmoled() {
        assertEquals(DarkColorScheme, resolveColorScheme(ThemeMode.DARK, systemDark = false, amoledBlack = false))
        assertEquals(AmoledColorScheme, resolveColorScheme(ThemeMode.DARK, systemDark = false, amoledBlack = true))
    }

    @Test
    fun systemMode_followsSystemDark() {
        assertEquals(LightColorScheme, resolveColorScheme(ThemeMode.SYSTEM, systemDark = false, amoledBlack = false))
        assertEquals(DarkColorScheme, resolveColorScheme(ThemeMode.SYSTEM, systemDark = true, amoledBlack = false))
        assertEquals(AmoledColorScheme, resolveColorScheme(ThemeMode.SYSTEM, systemDark = true, amoledBlack = true))
    }

    @Test
    fun amoledScheme_hasPureBlackBackgroundAndSurface() {
        assertEquals(Color(0xFF000000), AmoledColorScheme.background)
        assertEquals(Color(0xFF000000), AmoledColorScheme.surface)
    }
}
