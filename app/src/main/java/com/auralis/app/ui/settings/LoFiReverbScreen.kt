package com.auralis.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyShapes
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.playback.dsp.DspParameters
import com.auralis.app.playback.dsp.DspPreset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Dedicated Material 3 settings screen for the Real-Time Lo-Fi + Reverb DSP engine.
 *
 * Implements the "Rich App Experience + Simple Effect Controls" philosophy:
 * - Primary tier: Enable / Disable toggle, Preset selection, and master Intensity slider.
 * - Secondary tier: Collapsible "Advanced" drawer exposing individual Lo-Fi and Reverb parameters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoFiReverbScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AudioSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dspParams = uiState.dspParameters
    val isActive = dspParams.isAnyActive

    var showPresetSheet by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.lofi_reverb_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Navigate back to Audio Effects",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.md),
            verticalArrangement = Arrangement.spacedBy(MelofySpacing.lg)
        ) {
            // 1. Header / Description
            Column(modifier = Modifier.padding(horizontal = MelofySpacing.xs)) {
                Text(
                    text = stringResource(R.string.lofi_reverb_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Text(
                    text = stringResource(R.string.lofi_reverb_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 2. Main Effect Card (Master Enable / Disable)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MelofyShapes.card,
                colors = CardDefaults.cardColors(
                    containerColor = if (isActive) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
            ) {
                Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = MelofySpacing.md)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_waves),
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(MelofySpacing.md))
                            Column {
                                Text(
                                    text = stringResource(R.string.lofi_reverb_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(MelofySpacing.xxs))
                                Text(
                                    text = if (isActive) {
                                        stringResource(R.string.lofi_reverb_active) + " • " + dspParams.preset.displayName
                                    } else {
                                        stringResource(R.string.lofi_reverb_disabled)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { viewModel.toggleLoFiReverbMaster(it) },
                            modifier = Modifier.semantics {
                                contentDescription = "Toggle Lo-Fi and Reverb Effect"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(MelofySpacing.sm))
                    Text(
                        text = stringResource(R.string.lofi_reverb_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Primary Controls (Subdued when disabled)
            val controlsModifier = Modifier
                .fillMaxWidth()
                .alpha(if (isActive) 1f else 0.45f)

            Column(
                modifier = controlsModifier,
                verticalArrangement = Arrangement.spacedBy(MelofySpacing.lg)
            ) {
                // Preset Selection Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isActive) { showPresetSheet = true },
                    shape = MelofyShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MelofySpacing.lg),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.lofi_reverb_preset),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(MelofySpacing.xxs))
                            Text(
                                text = getPresetDisplayNameWithIcon(dspParams.preset),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(MelofySpacing.xs))
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "Select Preset",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Intensity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MelofyShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
                ) {
                    Column(modifier = Modifier.padding(MelofySpacing.lg)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.lofi_reverb_intensity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(dspParams.loFi.intensity * 100).roundToInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(MelofySpacing.xs))

                        Slider(
                            value = dspParams.loFi.intensity,
                            onValueChange = { viewModel.setLoFiReverbIntensity(it) },
                            valueRange = 0f..1f,
                            enabled = isActive,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Master Lo-Fi and Reverb intensity percentage" }
                        )
                    }
                }

                // 4. Expandable Advanced Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MelofyShapes.card,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
                ) {
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isAdvancedExpanded) 180f else 0f,
                        label = "advancedChevronRotation"
                    )

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                                .padding(MelofySpacing.lg),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.lofi_reverb_advanced),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isAdvancedExpanded) "Tap to collapse" else "Fine-tune individual parameters",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                painter = painterResource(R.drawable.ic_expand_more),
                                contentDescription = if (isAdvancedExpanded) "Collapse Advanced" else "Expand Advanced",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(chevronRotation)
                            )
                        }

                        AnimatedVisibility(visible = isAdvancedExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = MelofySpacing.lg, end = MelofySpacing.lg, bottom = MelofySpacing.lg)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(MelofySpacing.md))

                                // Lo-Fi Section
                                Text(
                                    text = stringResource(R.string.lofi_character_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(MelofySpacing.xs))

                                DspSliderRow(
                                    label = stringResource(R.string.lofi_bit_crush),
                                    value = dspParams.loFi.bitCrush,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setLoFiBitCrush(it) }
                                )

                                DspSliderRow(
                                    label = stringResource(R.string.lofi_sample_reduction),
                                    value = dspParams.loFi.sampleRateReduction,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setLoFiSampleReduction(it) }
                                )

                                DspSliderRow(
                                    label = stringResource(R.string.lofi_saturation),
                                    value = dspParams.loFi.saturation,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setLoFiSaturation(it) }
                                )

                                DspSliderRow(
                                    label = stringResource(R.string.lofi_wow_flutter),
                                    value = dspParams.loFi.wowFlutter,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setLoFiWowFlutter(it) }
                                )

                                Spacer(modifier = Modifier.height(MelofySpacing.md))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(MelofySpacing.md))

                                // Reverb Section
                                Text(
                                    text = stringResource(R.string.reverb_space_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(MelofySpacing.xs))

                                DspSliderRow(
                                    label = stringResource(R.string.reverb_room_size),
                                    value = dspParams.reverb.roomSize,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setReverbRoomSize(it) }
                                )

                                DspSliderRow(
                                    label = stringResource(R.string.reverb_decay),
                                    value = dspParams.reverb.decay,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setReverbDecay(it) }
                                )

                                DspSliderRow(
                                    label = stringResource(R.string.reverb_wet_level),
                                    value = dspParams.reverb.wetLevel,
                                    enabled = isActive,
                                    onValueChange = { viewModel.setReverbWetLevel(it) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(MelofySpacing.xxl))
        }
    }

    // 5. Preset Selection Bottom Sheet
    if (showPresetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPresetSheet = false },
            sheetState = sheetState,
            shape = MelofyShapes.bottomSheet,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MelofySpacing.xl, vertical = MelofySpacing.md)
            ) {
                Text(
                    text = stringResource(R.string.lofi_reverb_choose_preset),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(MelofySpacing.xs))
                Text(
                    text = "Select a signature sound profile for your listening experience.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(MelofySpacing.lg))

                val presets = listOf(
                    PresetOption(DspPreset.DREAMY, "🌙 Dreamy", "Soft, spacious and atmospheric."),
                    PresetOption(DspPreset.CASSETTE, "📼 Cassette", "Warm tape character with subtle imperfections."),
                    PresetOption(DspPreset.LATE_NIGHT, "🌃 Late Night", "Dark, smooth and intimate."),
                    PresetOption(DspPreset.DEEP_LOFI, "📻 Deep Lo-Fi", "Strong vintage degradation and texture.")
                )

                presets.forEach { option ->
                    val isSelected = dspParams.preset == option.preset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLoFiReverbPreset(option.preset)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showPresetSheet = false
                                }
                            }
                            .padding(vertical = MelofySpacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                viewModel.setLoFiReverbPreset(option.preset)
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    showPresetSheet = false
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(MelofySpacing.sm))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(MelofySpacing.xl))
            }
        }
    }
}

@Composable
private fun DspSliderRow(
    label: String,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = MelofySpacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(value * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label percentage" }
        )
    }
}

private data class PresetOption(
    val preset: DspPreset,
    val title: String,
    val description: String
)

private fun getPresetDisplayNameWithIcon(preset: DspPreset): String {
    return when (preset) {
        DspPreset.DREAMY -> "🌙 Dreamy"
        DspPreset.CASSETTE -> "📼 Cassette"
        DspPreset.LATE_NIGHT -> "🌃 Late Night"
        DspPreset.DEEP_LOFI -> "📻 Deep Lo-Fi"
        DspPreset.CUSTOM -> "✨ Custom"
    }
}
