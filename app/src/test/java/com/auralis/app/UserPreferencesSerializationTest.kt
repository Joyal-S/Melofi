package com.auralis.app

import com.auralis.app.data.local.preferences.UserPreferencesRepository
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for UserPreferences domain model, defaults, and parsing robustness.
 */
class UserPreferencesSerializationTest {

    @Test
    fun defaultUserPreferences_haveSensibleDefaults() {
        val prefs = UserPreferences()
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertFalse(prefs.amoledBlack)
        assertEquals(LibraryTab.FOR_YOU, prefs.defaultLibraryTab)
        assertEquals(LibraryViewMode.LIST, prefs.defaultViewMode)
        assertEquals(SongSortOrder.TITLE, prefs.defaultSortOrder)
    }

    @Test
    fun parseThemeMode_validInput_returnsEnum() {
        assertEquals(ThemeMode.LIGHT, UserPreferencesRepository.parseThemeMode("LIGHT"))
        assertEquals(ThemeMode.DARK, UserPreferencesRepository.parseThemeMode("DARK"))
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository.parseThemeMode("SYSTEM"))
    }

    @Test
    fun parseThemeMode_nullOrInvalidInput_returnsSystemDefault() {
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository.parseThemeMode(null))
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository.parseThemeMode("INVALID_MODE"))
        assertEquals(ThemeMode.SYSTEM, UserPreferencesRepository.parseThemeMode(""))
    }

    @Test
    fun parseLibraryTab_validInput_returnsEnum() {
        assertEquals(LibraryTab.TRACKS, UserPreferencesRepository.parseLibraryTab("TRACKS"))
        assertEquals(LibraryTab.ALBUMS, UserPreferencesRepository.parseLibraryTab("ALBUMS"))
        assertEquals(LibraryTab.ARTISTS, UserPreferencesRepository.parseLibraryTab("ARTISTS"))
        assertEquals(LibraryTab.GENRES, UserPreferencesRepository.parseLibraryTab("GENRES"))
        assertEquals(LibraryTab.FOR_YOU, UserPreferencesRepository.parseLibraryTab("FOR_YOU"))
    }

    @Test
    fun parseLibraryTab_nullOrInvalidInput_returnsForYouDefault() {
        assertEquals(LibraryTab.FOR_YOU, UserPreferencesRepository.parseLibraryTab(null))
        assertEquals(LibraryTab.FOR_YOU, UserPreferencesRepository.parseLibraryTab("NON_EXISTENT"))
    }

    @Test
    fun parseViewMode_validInput_returnsEnum() {
        assertEquals(LibraryViewMode.COMPACT, UserPreferencesRepository.parseViewMode("COMPACT"))
        assertEquals(LibraryViewMode.GRID, UserPreferencesRepository.parseViewMode("GRID"))
        assertEquals(LibraryViewMode.LIST, UserPreferencesRepository.parseViewMode("LIST"))
    }

    @Test
    fun parseViewMode_nullOrInvalidInput_returnsListDefault() {
        assertEquals(LibraryViewMode.LIST, UserPreferencesRepository.parseViewMode(null))
        assertEquals(LibraryViewMode.LIST, UserPreferencesRepository.parseViewMode("XYZ"))
    }

    @Test
    fun parseSortOrder_validInput_returnsEnum() {
        assertEquals(SongSortOrder.DATE_ADDED, UserPreferencesRepository.parseSortOrder("DATE_ADDED"))
        assertEquals(SongSortOrder.ARTIST, UserPreferencesRepository.parseSortOrder("ARTIST"))
        assertEquals(SongSortOrder.MOST_PLAYED, UserPreferencesRepository.parseSortOrder("MOST_PLAYED"))
    }

    @Test
    fun parseSortOrder_nullOrInvalidInput_returnsTitleDefault() {
        assertEquals(SongSortOrder.TITLE, UserPreferencesRepository.parseSortOrder(null))
        assertEquals(SongSortOrder.TITLE, UserPreferencesRepository.parseSortOrder("UNKNOWN"))
    }
}
