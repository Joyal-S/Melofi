package com.auralis.app.domain.model

/**
 * Supported theme modes for local personalization.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Domain model representing persistent local user personalization preferences.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledBlack: Boolean = false,
    val defaultLibraryTab: LibraryTab = LibraryTab.FOR_YOU,
    val defaultViewMode: LibraryViewMode = LibraryViewMode.LIST,
    val defaultSortOrder: SongSortOrder = SongSortOrder.TITLE
)
