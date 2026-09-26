package com.auralis.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auralis.app.core.ui.theme.MelofyAccent
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.data.local.db.entity.ExcludedFolderEntity
import com.auralis.app.data.local.db.entity.HiddenAudioEntity
import com.auralis.app.data.local.db.dao.ImportedFolderDao
import com.auralis.app.data.local.db.entity.ImportedFolderEntity
import com.auralis.app.data.local.preferences.UserPreferencesRepository
import com.auralis.app.domain.model.ArtworkCornerStyle
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import com.auralis.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val songRepository: SongRepository,
    private val importedFolderDao: ImportedFolderDao
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = userPreferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = songRepository.getExcludedFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val importedFolders: StateFlow<List<ImportedFolderEntity>> = importedFolderDao.getAllFoldersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hiddenAudioList: StateFlow<List<HiddenAudioEntity>> = songRepository.getHiddenAudio()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteImportedFolder(id: Long) {
        viewModelScope.launch {
            importedFolderDao.deleteFolderById(id)
            songRepository.scanLibrary()
        }
    }

    fun updateFolderPermissionStatus(id: Long, isValid: Boolean) {
        viewModelScope.launch {
            importedFolderDao.updatePermissionStatus(id, isValid)
            if (isValid) {
                songRepository.scanLibrary()
            }
        }
    }

    fun toggleExcludedFolder(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            songRepository.toggleExcludedFolder(id, isEnabled)
        }
    }

    fun addExcludedFolder(folderName: String, folderPath: String) {
        viewModelScope.launch {
            songRepository.addExcludedFolder(folderName, folderPath)
        }
    }

    fun deleteExcludedFolder(id: Long) {
        viewModelScope.launch {
            songRepository.deleteExcludedFolder(id)
        }
    }

    fun restoreHiddenAudio(id: Long) {
        viewModelScope.launch {
            songRepository.restoreHiddenAudio(id)
        }
    }

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

    fun setAccentColor(accent: MelofyAccent) {
        viewModelScope.launch {
            userPreferencesRepository.setAccentColor(accent)
        }
    }

    fun setAnimationMode(mode: MelofyAnimationMode) {
        viewModelScope.launch {
            userPreferencesRepository.setAnimationMode(mode)
        }
    }

    fun setShowQualityBadges(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowQualityBadges(show)
        }
    }

    fun setArtworkCornerStyle(style: ArtworkCornerStyle) {
        viewModelScope.launch {
            userPreferencesRepository.setArtworkCornerStyle(style)
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
