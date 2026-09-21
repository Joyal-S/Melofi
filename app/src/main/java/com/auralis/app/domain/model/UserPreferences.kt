package com.auralis.app.domain.model

import com.auralis.app.core.ui.theme.MelofyAccent
import com.auralis.app.core.ui.theme.MelofyAnimationMode

/**
 * Supported theme modes for local personalization.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Corner radius styles for album artwork throughout the app.
 */
enum class ArtworkCornerStyle(val displayName: String, val cornerRadiusDp: Int) {
    ROUNDED("Standard (12dp)", 12),
    SUBTLE("Subtle (6dp)", 6),
    SQUIRCLE("Squircle (20dp)", 20)
}

/**
 * Domain model representing persistent local user personalization preferences.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val defaultLibraryTab: LibraryTab = LibraryTab.FOR_YOU,
    val defaultViewMode: LibraryViewMode = LibraryViewMode.LIST,
    val defaultSortOrder: SongSortOrder = SongSortOrder.TITLE,
    val accentColor: MelofyAccent = MelofyAccent.VIOLET,
    val animationMode: MelofyAnimationMode = MelofyAnimationMode.FULL,
    val showQualityBadges: Boolean = true,
    val artworkCornerStyle: ArtworkCornerStyle = ArtworkCornerStyle.ROUNDED
)
