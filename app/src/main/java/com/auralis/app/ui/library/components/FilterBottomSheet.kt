package com.auralis.app.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auralis.app.domain.model.DurationFilter
import com.auralis.app.domain.model.FormatFilter
import com.auralis.app.domain.model.LibraryFilter
import com.auralis.app.domain.model.MetadataFilter
import com.auralis.app.domain.model.PlaybackFilter
import com.auralis.app.domain.model.QualityFilter
import com.auralis.app.domain.model.SongFilterState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    filterState: SongFilterState,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onApply: (SongFilterState) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var draftState by remember(filterState) { mutableStateOf(filterState) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (draftState.activeFilterCount > 0)
                        "Filters (${draftState.activeFilterCount})"
                    else
                        "Filter Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (draftState.activeFilterCount > 0) {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            draftState = SongFilterState()
                            onClearAll()
                        }
                    ) {
                        Text("Reset All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // 1. Format Filter
            FilterSectionHeader("Audio Format")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FormatFilter.entries.forEach { format ->
                    FilterChip(
                        selected = draftState.formatFilter == format,
                        onClick = { draftState = draftState.copy(formatFilter = format) },
                        label = { Text(format.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Quality Filter
            FilterSectionHeader("Quality / Bitrate")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QualityFilter.entries.forEach { quality ->
                    FilterChip(
                        selected = draftState.qualityFilter == quality,
                        onClick = { draftState = draftState.copy(qualityFilter = quality) },
                        label = { Text(quality.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Duration Filter
            FilterSectionHeader("Track Duration")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DurationFilter.entries.forEach { duration ->
                    FilterChip(
                        selected = draftState.durationFilter == duration,
                        onClick = { draftState = draftState.copy(durationFilter = duration) },
                        label = { Text(duration.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Playback / User State Filter
            FilterSectionHeader("Listening State")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PlaybackFilter.entries.forEach { playback ->
                    FilterChip(
                        selected = draftState.playbackFilter == playback,
                        onClick = { draftState = draftState.copy(playbackFilter = playback) },
                        label = { Text(playback.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Metadata Filter
            FilterSectionHeader("Metadata & Artwork")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MetadataFilter.entries.forEach { metadata ->
                    FilterChip(
                        selected = draftState.metadataFilter == metadata,
                        onClick = { draftState = draftState.copy(metadataFilter = metadata) },
                        label = { Text(metadata.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Library Date Filter
            FilterSectionHeader("Library Added / Modified")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LibraryFilter.entries.forEach { library ->
                    FilterChip(
                        selected = draftState.libraryFilter == library,
                        onClick = { draftState = draftState.copy(libraryFilter = library) },
                        label = { Text(library.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onApply(draftState)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
