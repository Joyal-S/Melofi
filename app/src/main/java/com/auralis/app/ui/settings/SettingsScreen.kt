package com.auralis.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofyAccent
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.domain.model.ArtworkCornerStyle
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode

import com.auralis.app.playback.audiofx.ReplayGainMode
import androidx.activity.compose.BackHandler

/**
 * Unified Settings screen organizing Playback, Appearance, Library, Statistics, and About Melofi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    audioViewModel: AudioSettingsViewModel = hiltViewModel(),
    initialTab: Int = 0,
    onNavigateToStatistics: () -> Unit = {}
) {
    var showAudioEffectsScreen by remember { mutableStateOf(false) }
    var showLoFiReverbScreen by remember { mutableStateOf(false) }
    var showMusicSourcesScreen by remember { mutableStateOf(false) }
    var showHiddenAudioScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = showLoFiReverbScreen || showAudioEffectsScreen || showMusicSourcesScreen || showHiddenAudioScreen) {
        when {
            showLoFiReverbScreen -> showLoFiReverbScreen = false
            showAudioEffectsScreen -> showAudioEffectsScreen = false
            showMusicSourcesScreen -> showMusicSourcesScreen = false
            showHiddenAudioScreen -> showHiddenAudioScreen = false
        }
    }

    if (showLoFiReverbScreen) {
        LoFiReverbScreen(
            onBackClick = { showLoFiReverbScreen = false },
            viewModel = audioViewModel,
            modifier = modifier.fillMaxSize()
        )
    } else if (showAudioEffectsScreen) {
        AudioSettingsScreen(
            viewModel = audioViewModel,
            onNavigateToLoFiReverb = { showLoFiReverbScreen = true },
            onBackClick = { showAudioEffectsScreen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (showMusicSourcesScreen) {
        MusicSourcesScreen(
            viewModel = settingsViewModel,
            onBackClick = { showMusicSourcesScreen = false },
            modifier = modifier.fillMaxSize()
        )
    } else if (showHiddenAudioScreen) {
        HiddenAudioScreen(
            viewModel = settingsViewModel,
            onBackClick = { showHiddenAudioScreen = false },
            modifier = modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { innerPadding ->
            SettingsMainContent(
                settingsViewModel = settingsViewModel,
                audioViewModel = audioViewModel,
                onNavigateToAudioEffects = { showAudioEffectsScreen = true },
                onNavigateToLoFiReverb = { showLoFiReverbScreen = true },
                onNavigateToMusicSources = { showMusicSourcesScreen = true },
                onNavigateToHiddenAudio = { showHiddenAudioScreen = true },
                onNavigateToStatistics = onNavigateToStatistics,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsMainContent(
    settingsViewModel: SettingsViewModel,
    audioViewModel: AudioSettingsViewModel,
    onNavigateToAudioEffects: () -> Unit,
    onNavigateToLoFiReverb: () -> Unit,
    onNavigateToMusicSources: () -> Unit,
    onNavigateToHiddenAudio: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences by settingsViewModel.preferences.collectAsStateWithLifecycle()
    val audioUiState by audioViewModel.uiState.collectAsStateWithLifecycle()
    val hiddenAudioList by settingsViewModel.hiddenAudioList.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(MelofySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MelofySpacing.md)
    ) {
        // 1. PLAYBACK
        SettingsSectionHeader(title = "Playback")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                // Audio Effects Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAudioEffects() }
                        .padding(vertical = MelofySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tune),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(MelofySpacing.md))
                        Column {
                            Text(
                                text = "Audio Effects & Equalizer",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "5-band hardware EQ, Bass Boost, Virtualizer & Sleep Timer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = "Open Audio Effects",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // Lo-Fi + Reverb Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToLoFiReverb() }
                        .padding(vertical = MelofySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_waves),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(MelofySpacing.md))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Lo-Fi & Reverb Processor",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (audioUiState.dspParameters.isAnyActive) {
                                    Spacer(modifier = Modifier.width(MelofySpacing.xs))
                                    Text(
                                        text = "• Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "Warm tape saturation, vinyl crackle & spatial reverb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = "Open Lo-Fi & Reverb",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // ReplayGain Loudness
                Text(
                    text = "ReplayGain Loudness Normalization",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Normalizes perceived loudness across tracks using embedded gain tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    listOf(
                        ReplayGainMode.OFF to "Off",
                        ReplayGainMode.TRACK to "Track Gain",
                        ReplayGainMode.ALBUM to "Album Gain"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = audioUiState.replayGainState.mode == mode,
                            onClick = { audioViewModel.setReplayGainMode(mode) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MelofySpacing.xs))

        // 2. APPEARANCE
        SettingsSectionHeader(title = "Appearance")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { settingsViewModel.setThemeMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "System Default"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // AMOLED Pure Black
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = MelofySpacing.md)) {
                        Text(
                            text = "AMOLED Pure Black",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Uses pure #000000 black in dark mode for maximum contrast and battery efficiency on OLED displays.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.amoledBlack,
                        onCheckedChange = { settingsViewModel.setAmoledBlack(it) }
                    )
                }

                // Accent Color
                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                Text(
                    text = "Accent Color",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    MelofyAccent.entries.forEach { accent ->
                        FilterChip(
                            selected = preferences.accentColor == accent,
                            onClick = { settingsViewModel.setAccentColor(accent) },
                            label = { Text(accent.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Motion & Animations
                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                Text(
                    text = "Motion & Animations",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    MelofyAnimationMode.entries.forEach { animMode ->
                        FilterChip(
                            selected = preferences.animationMode == animMode,
                            onClick = { settingsViewModel.setAnimationMode(animMode) },
                            label = {
                                Text(
                                    when (animMode) {
                                        MelofyAnimationMode.FULL -> "Fluid Motion"
                                        MelofyAnimationMode.REDUCED -> "Subtle Transitions"
                                        MelofyAnimationMode.OFF -> "Off (Battery Saver)"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Artwork Corner Style
                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                Text(
                    text = "Artwork Corner Style",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Controls curvature of artwork tiles across library, playlists, and playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    ArtworkCornerStyle.entries.forEach { cornerStyle ->
                        FilterChip(
                            selected = preferences.artworkCornerStyle == cornerStyle,
                            onClick = { settingsViewModel.setArtworkCornerStyle(cornerStyle) },
                            label = {
                                Text(
                                    when (cornerStyle) {
                                        ArtworkCornerStyle.ROUNDED -> "Standard Rounded"
                                        ArtworkCornerStyle.SUBTLE -> "Subtle (Sharp)"
                                        ArtworkCornerStyle.SQUIRCLE -> "Squircle (Smooth)"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // Audio Quality Badges
                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = MelofySpacing.md)) {
                        Text(
                            text = "Show Audio Quality Badges",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Displays FLAC, WAV, and 320k badges on track list items.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferences.showQualityBadges,
                        onCheckedChange = { settingsViewModel.setShowQualityBadges(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MelofySpacing.xs))

        // 3. MUSIC LIBRARY & SOURCES
        SettingsSectionHeader(title = "Music Library & Sources")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                // Music Sources & Folders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToMusicSources() }
                        .padding(vertical = MelofySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(MelofySpacing.md))
                        Column {
                            Text(
                                text = "Music Sources & Folders",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Folder exclusions, system audio & WhatsApp filtering",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = "Open Music Sources",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // Hidden Audio Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToHiddenAudio() }
                        .padding(vertical = MelofySpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_music_placeholder),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(MelofySpacing.md))
                        Column {
                            Text(
                                text = "Hidden from Music Library",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (hiddenAudioList.isEmpty()) "No hidden tracks" else "${hiddenAudioList.size} hidden track${if (hiddenAudioList.size > 1) "s" else ""} • Review & restore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = "Open Hidden Audio",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(MelofySpacing.xs))

        // 4. LIBRARY DEFAULTS
        SettingsSectionHeader(title = "Library Defaults")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                // Startup Library Tab
                Text(
                    text = "Startup Library Tab",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    LibraryTab.entries.forEach { tab ->
                        FilterChip(
                            selected = preferences.defaultLibraryTab == tab,
                            onClick = { settingsViewModel.setDefaultLibraryTab(tab) },
                            label = { Text(tab.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // Default View Mode
                Text(
                    text = "Default View Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    LibraryViewMode.entries.forEach { viewMode ->
                        FilterChip(
                            selected = preferences.defaultViewMode == viewMode,
                            onClick = { settingsViewModel.setDefaultViewMode(viewMode) },
                            label = {
                                Text(
                                    when (viewMode) {
                                        LibraryViewMode.LIST -> "Standard List"
                                        LibraryViewMode.COMPACT -> "Compact List"
                                        LibraryViewMode.GRID -> "2-Column Grid"
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // Default Sort Order
                Text(
                    text = "Default Sort Order",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)
                ) {
                    val commonSorts = listOf(
                        SongSortOrder.TITLE,
                        SongSortOrder.ARTIST,
                        SongSortOrder.ALBUM,
                        SongSortOrder.DATE_ADDED,
                        SongSortOrder.MOST_PLAYED
                    )
                    commonSorts.forEach { sort ->
                        FilterChip(
                            selected = preferences.defaultSortOrder == sort,
                            onClick = { settingsViewModel.setDefaultSortOrder(sort) },
                            label = { Text(sort.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MelofySpacing.xs))

        // 4. ABOUT
        SettingsSectionHeader(title = "About")

        // Statistics Entry Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToStatistics() },
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MelofySpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_insights),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(MelofySpacing.md))
                    Column {
                        Text(
                            text = "Listening Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "View top tracks, artists, and listening history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = "Open Statistics",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Privacy Guarantee Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(MelofySpacing.sm))
                    Text(
                        text = "100% Offline & Private",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(MelofySpacing.sm))

                Text(
                    text = "Melofi has ZERO network permissions. Your music, listening history, favorites, metadata edits, and recommendations are strictly on-device. No accounts, no cloud sync, no telemetry, no tracking, and no external advertising.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // About Melofi Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Text(
                    text = "MELOFI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "\"Your music. Your device. Your control.\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(MelofySpacing.md))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("1.0.0 (Offline Release)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Playback Engine", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("AndroidX Media3 ExoPlayer", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Database", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Room SQLite (auralis.db v4)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("UI Toolkit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Jetpack Compose & Material 3", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Developer Credit / Product Signature
        Spacer(modifier = Modifier.height(MelofySpacing.xl))

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .align(Alignment.CenterHorizontally),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            thickness = 1.dp
        )

        Spacer(modifier = Modifier.height(MelofySpacing.md))

        Text(
            text = "Developed by Joyal",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(MelofySpacing.xl))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = MelofySpacing.xs, top = MelofySpacing.xs)
    )
}
