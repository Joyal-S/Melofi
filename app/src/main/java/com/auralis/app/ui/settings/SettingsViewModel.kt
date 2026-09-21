package com.auralis.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.data.local.preferences.UserPreferencesRepository
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAmoledBlack(enabled)
        }
    }

    fun setDefaultLibraryTab(tab: LibraryTab) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultLibraryTab(tab)
        }
    }

    fun setDefaultViewMode(viewMode: LibraryViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultViewMode(viewMode)
        }
    }

    fun setDefaultSortOrder(sortOrder: SongSortOrder) {
        viewModelScope.launch {
            userPreferencesRepository.setDefaultSortOrder(sortOrder)
        }
    }
}
