package com.auralis.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.playback.audiofx.EqualizerBand
import com.auralis.app.playback.audiofx.EqualizerState
import com.auralis.app.playback.audiofx.ReplayGainMode
import com.auralis.app.playback.audiofx.SleepTimerState

/**
 * Material 3 Audio Settings screen providing direct control over native hardware effects,
 * ReplayGain loudness normalization, sleep timer, and playback engine configurations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomTimerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.audio_settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // 1. Equalizer Card
        EqualizerSection(
            equalizerState = uiState.effectsState.equalizer,
            onToggleEnabled = { viewModel.toggleEqualizer(it) },
            onPresetSelected = { viewModel.selectEqualizerPreset(it) },
            onBandLevelChanged = { bandIndex, level -> viewModel.setEqualizerBandLevel(bandIndex, level) }
        )

        // 2. Bass Boost Card
        BassBoostSection(
            isSupported = uiState.effectsState.bassBoost.isSupported,
            isEnabled = uiState.effectsState.bassBoost.isEnabled,
            strength = uiState.effectsState.bassBoost.strength,
            strengthPercentage = uiState.effectsState.bassBoost.strengthPercentage,
            onToggleEnabled = { viewModel.toggleBassBoost(it) },
            onStrengthChanged = { viewModel.setBassBoostStrength(it) }
        )

        // 3. Virtualizer Card
        VirtualizerSection(
            isSupported = uiState.effectsState.virtualizer.isSupported,
            isEnabled = uiState.effectsState.virtualizer.isEnabled,
            strength = uiState.effectsState.virtualizer.strength,
            strengthPercentage = uiState.effectsState.virtualizer.strengthPercentage,
            onToggleEnabled = { viewModel.toggleVirtualizer(it) },
            onStrengthChanged = { viewModel.setVirtualizerStrength(it) }
        )

        // 4. Loudness Normalization / ReplayGain
        ReplayGainSection(
            currentMode = uiState.replayGainState.mode,
            currentMetadataSummary = uiState.replayGainState.currentMetadata?.displaySummary,
            appliedMultiplier = uiState.replayGainState.appliedVolumeMultiplier,
            onModeSelected = { viewModel.setReplayGainMode(it) }
        )

        // 5. Playback Engine / Gapless & Crossfade Transparency
        PlaybackEngineSection()

        // 6. Sleep Timer
        SleepTimerSection(
            timerState = uiState.sleepTimerState,
            onStartPreset = { viewModel.startSleepTimerMinutes(it) },
            onOpenCustomDialog = { showCustomTimerDialog = true },
            onCancelTimer = { viewModel.cancelSleepTimer() }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCustomTimerDialog) {
        CustomSleepTimerDialog(
            onDismiss = { showCustomTimerDialog = false },
            onConfirm = { minutes ->
                viewModel.startSleepTimerMinutes(minutes)
                showCustomTimerDialog = false
            }
        )
    }
}

@Composable
private fun EqualizerSection(
    equalizerState: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onBandLevelChanged: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.equalizer_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (equalizerState.isSupported) {
                            if (equalizerState.isEnabled) "Active (${equalizerState.bands.size} bands)" else "Disabled"
                        } else {
                            stringResource(R.string.equalizer_not_supported)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = equalizerState.isEnabled && equalizerState.isSupported,
                    onCheckedChange = onToggleEnabled,
                    enabled = equalizerState.isSupported
                )
            }

            if (!equalizerState.isSupported) {
                UnsupportedBadge(message = stringResource(R.string.equalizer_not_supported))
            } else {
                AnimatedVisibility(visible = equalizerState.isEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        // Presets row
                        if (equalizerState.presets.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = equalizerState.currentPresetIndex == EqualizerState.PRESET_CUSTOM,
                                    onClick = { },
                                    label = { Text("Custom") }
                                )
                                equalizerState.presets.forEach { preset ->
                                    FilterChip(
                                        selected = equalizerState.currentPresetIndex == preset.index,
                                        onClick = { onPresetSelected(preset.index) },
                                        label = { Text(preset.name) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Bands
                        equalizerState.bands.forEach { band ->
                            BandSliderRow(
                                band = band,
                                minLevel = equalizerState.minLevelMilliBels,
                                maxLevel = equalizerState.maxLevelMilliBels,
                                onLevelChanged = { onBandLevelChanged(band.index, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BandSliderRow(
    band: EqualizerBand,
    minLevel: Int,
    maxLevel: Int,
    onLevelChanged: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = band.centerFreqFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = band.levelFormatted,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = band.levelMilliBels.toFloat(),
            onValueChange = { onLevelChanged(it.toInt()) },
            valueRange = minLevel.toFloat()..maxLevel.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BassBoostSection(
    isSupported: Boolean,
    isEnabled: Boolean,
    strength: Int,
    strengthPercentage: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onStrengthChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.bass_boost_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isSupported) {
                            if (isEnabled) "$strengthPercentage%" else "Disabled"
                        } else {
                            stringResource(R.string.bass_boost_not_supported)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled && isSupported,
                    onCheckedChange = onToggleEnabled,
                    enabled = isSupported
                )
            }

            if (!isSupported) {
                UnsupportedBadge(message = stringResource(R.string.bass_boost_not_supported))
            } else {
                AnimatedVisibility(visible = isEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Strength", style = MaterialTheme.typography.labelMedium)
                            Text("$strengthPercentage%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = strength.toFloat(),
                            onValueChange = { onStrengthChanged(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VirtualizerSection(
    isSupported: Boolean,
    isEnabled: Boolean,
    strength: Int,
    strengthPercentage: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onStrengthChanged: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.virtualizer_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isSupported) {
                            if (isEnabled) "$strengthPercentage%" else "Disabled"
                        } else {
                            stringResource(R.string.virtualizer_not_supported)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled && isSupported,
                    onCheckedChange = onToggleEnabled,
                    enabled = isSupported
                )
            }

            if (!isSupported) {
                UnsupportedBadge(message = stringResource(R.string.virtualizer_not_supported))
            } else {
                AnimatedVisibility(visible = isEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Strength", style = MaterialTheme.typography.labelMedium)
                            Text("$strengthPercentage%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = strength.toFloat(),
                            onValueChange = { onStrengthChanged(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayGainSection(
    currentMode: ReplayGainMode,
    currentMetadataSummary: String?,
    appliedMultiplier: Float,
    onModeSelected: (ReplayGainMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.loudness_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.replaygain_title),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReplayGainMode.values().forEach { mode ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val explanation = when (currentMode) {
                ReplayGainMode.OFF -> stringResource(R.string.replaygain_desc_off)
                ReplayGainMode.TRACK -> stringResource(R.string.replaygain_desc_track)
                ReplayGainMode.ALBUM -> stringResource(R.string.replaygain_desc_album)
            }
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata info row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Current Track Metadata",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentMetadataSummary ?: "No active track or no ReplayGain metadata",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (currentMode != ReplayGainMode.OFF) {
                        Text(
                            text = String.format(java.util.Locale.US, "Active volume scale: %.2fx", appliedMultiplier),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.replaygain_disclaimer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun PlaybackEngineSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.playback_behavior_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gapless Playback
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.gapless_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Automatic",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.gapless_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Crossfade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.crossfade_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Platform Limitation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.crossfade_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun SleepTimerSection(
    timerState: SleepTimerState,
    onStartPreset: (Int) -> Unit,
    onOpenCustomDialog: () -> Unit,
    onCancelTimer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.sleep_timer_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (timerState) {
                is SleepTimerState.Running -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Countdown",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = timerState.formattedRemaining,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { timerState.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCancelTimer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.sleep_timer_cancel))
                        }
                    }
                }
                is SleepTimerState.Completed -> {
                    Column {
                        Text(
                            text = stringResource(R.string.sleep_timer_completed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onCancelTimer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reset")
                        }
                    }
                }
                is SleepTimerState.Inactive -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { minutes ->
                            OutlinedButton(onClick = { onStartPreset(minutes) }) {
                                Text("$minutes m")
                            }
                        }
                        Button(onClick = onOpenCustomDialog) {
                            Text(stringResource(R.string.sleep_timer_custom))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnsupportedBadge(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun CustomSleepTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var minutesText by remember { mutableStateOf("45") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Sleep Timer") },
        text = {
            Column {
                Text(
                    text = "Enter duration in minutes (1 to 180):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = {
                        minutesText = it.filter { char -> char.isDigit() }
                        val num = minutesText.toIntOrNull()
                        isError = num == null || num <= 0 || num > 180
                    },
                    isError = isError,
                    singleLine = true,
                    label = { Text("Minutes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minutes = minutesText.toIntOrNull()
                    if (minutes != null && minutes in 1..180) {
                        onConfirm(minutes)
                    } else {
                        isError = true
                    }
                },
                enabled = !isError && minutesText.isNotBlank()
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
