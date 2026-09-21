package com.auralis.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auralis.app.core.ui.theme.MelofyAccent
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.domain.model.ArtworkCornerStyle
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "melofy_user_preferences")

/**
 * Repository managing persistent local user personalization preferences (theme, view mode, default tab, sort order, accent, motion).
 * 100% offline via AndroidX DataStore.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    val preferencesFlow: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    suspend fun setDefaultLibraryTab(tab: LibraryTab) {
        context.userPreferencesDataStore.edit { it[KEY_DEFAULT_LIBRARY_TAB] = tab.name }
    }

    suspend fun setDefaultViewMode(viewMode: LibraryViewMode) {
        context.userPreferencesDataStore.edit { it[KEY_DEFAULT_VIEW_MODE] = viewMode.name }
    }

    suspend fun setDefaultSortOrder(sortOrder: SongSortOrder) {
        context.userPreferencesDataStore.edit { it[KEY_DEFAULT_SORT_ORDER] = sortOrder.name }
    }

    suspend fun setAccentColor(accent: MelofyAccent) {
        context.userPreferencesDataStore.edit { it[KEY_ACCENT_COLOR] = accent.name }
    }

    suspend fun setAnimationMode(mode: MelofyAnimationMode) {
        context.userPreferencesDataStore.edit { it[KEY_ANIMATION_MODE] = mode.name }
    }

    suspend fun setShowQualityBadges(enabled: Boolean) {
        context.userPreferencesDataStore.edit { it[KEY_SHOW_QUALITY_BADGES] = enabled }
    }

    suspend fun setArtworkCornerStyle(style: ArtworkCornerStyle) {
        context.userPreferencesDataStore.edit { it[KEY_ARTWORK_CORNER_STYLE] = style.name }
    }

    companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val KEY_DEFAULT_LIBRARY_TAB = stringPreferencesKey("default_library_tab")
        val KEY_DEFAULT_VIEW_MODE = stringPreferencesKey("default_view_mode")
        val KEY_DEFAULT_SORT_ORDER = stringPreferencesKey("default_sort_order")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_ANIMATION_MODE = stringPreferencesKey("animation_mode")
        val KEY_SHOW_QUALITY_BADGES = booleanPreferencesKey("show_quality_badges")
        val KEY_ARTWORK_CORNER_STYLE = stringPreferencesKey("artwork_corner_style")

        fun mapPreferences(preferences: Preferences): UserPreferences {
            val themeMode = parseThemeMode(preferences[KEY_THEME_MODE])
            val amoledBlack = preferences[KEY_AMOLED_BLACK] ?: false
            val defaultLibraryTab = parseLibraryTab(preferences[KEY_DEFAULT_LIBRARY_TAB])
            val defaultViewMode = parseViewMode(preferences[KEY_DEFAULT_VIEW_MODE])
            val defaultSortOrder = parseSortOrder(preferences[KEY_DEFAULT_SORT_ORDER])
            val accentColor = parseAccentColor(preferences[KEY_ACCENT_COLOR])
            val animationMode = parseAnimationMode(preferences[KEY_ANIMATION_MODE])
            val showQualityBadges = preferences[KEY_SHOW_QUALITY_BADGES] ?: true
            val artworkCornerStyle = parseArtworkCornerStyle(preferences[KEY_ARTWORK_CORNER_STYLE])

            return UserPreferences(
                themeMode = themeMode,
                amoledBlack = amoledBlack,
                defaultLibraryTab = defaultLibraryTab,
                defaultViewMode = defaultViewMode,
                defaultSortOrder = defaultSortOrder,
                accentColor = accentColor,
                animationMode = animationMode,
                showQualityBadges = showQualityBadges,
                artworkCornerStyle = artworkCornerStyle
            )
        }

        fun parseThemeMode(name: String?): ThemeMode {
            return if (name == null) ThemeMode.SYSTEM
            else runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
        }

        fun parseLibraryTab(name: String?): LibraryTab {
            return if (name == null) LibraryTab.FOR_YOU
            else runCatching { LibraryTab.valueOf(name) }.getOrDefault(LibraryTab.FOR_YOU)
        }

        fun parseViewMode(name: String?): LibraryViewMode {
            return if (name == null) LibraryViewMode.LIST
            else runCatching { LibraryViewMode.valueOf(name) }.getOrDefault(LibraryViewMode.LIST)
        }

        fun parseSortOrder(name: String?): SongSortOrder {
            return if (name == null) SongSortOrder.TITLE
            else runCatching { SongSortOrder.valueOf(name) }.getOrDefault(SongSortOrder.TITLE)
        }

        fun parseAccentColor(name: String?): MelofyAccent {
            return if (name == null) MelofyAccent.VIOLET
            else runCatching { MelofyAccent.valueOf(name) }.getOrDefault(MelofyAccent.VIOLET)
        }

        fun parseAnimationMode(name: String?): MelofyAnimationMode {
            return if (name == null) MelofyAnimationMode.FULL
            else runCatching { MelofyAnimationMode.valueOf(name) }.getOrDefault(MelofyAnimationMode.FULL)
        }

        fun parseArtworkCornerStyle(name: String?): ArtworkCornerStyle {
            return if (name == null) ArtworkCornerStyle.ROUNDED
            else runCatching { ArtworkCornerStyle.valueOf(name) }.getOrDefault(ArtworkCornerStyle.ROUNDED)
        }
    }
}
