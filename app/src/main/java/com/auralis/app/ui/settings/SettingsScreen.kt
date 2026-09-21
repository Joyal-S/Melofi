package com.auralis.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.domain.model.LibraryTab
import com.auralis.app.domain.model.LibraryViewMode
import com.auralis.app.domain.model.SongSortOrder
import com.auralis.app.domain.model.ThemeMode

/**
 * Unified Settings screen organizing Audio Effects (Equalizer, Bass Boost, ReplayGain)
 * and Personalization (Theme, AMOLED black, Library defaults, Privacy, About Melofy).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    audioViewModel: AudioSettingsViewModel = hiltViewModel(),
    initialTab: Int = 0
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }
    val tabs = listOf("Audio Effects", "Personalization & About")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
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
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> {
                AudioSettingsScreen(
                    viewModel = audioViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            1 -> {
                PersonalizationAndAboutContent(
                    viewModel = settingsViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalizationAndAboutContent(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(MelofySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MelofySpacing.lg)
    ) {
        // 1. Appearance Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MelofySpacing.md))

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
                            onClick = { viewModel.setThemeMode(mode) },
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
                        onCheckedChange = { viewModel.setAmoledBlack(it) }
                    )
                }
            }
        }

        // 2. Library Personalization Defaults
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Text(
                    text = "Library Defaults",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // Default Tab
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
                            onClick = { viewModel.setDefaultLibraryTab(tab) },
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
                            onClick = { viewModel.setDefaultViewMode(viewMode) },
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
                            onClick = { viewModel.setDefaultSortOrder(sort) },
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

        // 3. Privacy Guarantee Card
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
                    text = "Melofy has ZERO network permissions. Your music, listening history, favorites, metadata edits, and recommendations are strictly on-device. No accounts, no cloud sync, no telemetry, no tracking, and no external advertising.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 4. About Melofy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MelofyShapes.card,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
        ) {
            Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                Text(
                    text = "MELOFY",
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
    }
}
