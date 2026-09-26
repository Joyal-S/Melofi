package com.auralis.app.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.EditableMetadata
import com.auralis.app.domain.model.MetadataEditValidator
import com.auralis.app.domain.model.SongDetails

@Composable
fun EditMetadataDialog(
    songDetails: SongDetails,
    availableArtists: List<Artist> = emptyList(),
    onCreateNewArtist: (suspend (String) -> Result<Artist>)? = null,
    onDismiss: () -> Unit,
    onSave: (EditableMetadata) -> Unit
) {
    var title by remember { mutableStateOf(songDetails.basicInfo.title) }
    var artistName by remember { mutableStateOf(songDetails.basicInfo.artist) }
    var selectedArtistId by remember { mutableStateOf(songDetails.basicInfo.artistId) }
    var showArtistPicker by remember { mutableStateOf(false) }

    var album by remember { mutableStateOf(songDetails.basicInfo.album) }
    var albumArtist by remember { mutableStateOf(songDetails.basicInfo.albumArtist ?: "") }
    var genre by remember { mutableStateOf(songDetails.basicInfo.genre ?: "") }
    var composer by remember { mutableStateOf(songDetails.basicInfo.composer ?: "") }
    var yearText by remember { mutableStateOf(songDetails.basicInfo.year?.toString() ?: "") }
    var trackText by remember { mutableStateOf(songDetails.basicInfo.trackNumber?.toString() ?: "") }
    var discText by remember { mutableStateOf(songDetails.basicInfo.discNumber?.toString() ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isFormatEditable = remember {
        MetadataEditValidator.isFormatEditable(
            songDetails.audioInfo.mimeType,
            songDetails.fileInfo.contentUri
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Song Metadata",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isFormatEditable) {
                    Text(
                        text = "Metadata editing is not supported for this file format.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMessage = null },
                    label = { Text("Title *") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Artist Picker Selector (Section 4 & 5: persistent artist assignment instead of free-text)
                Surface(
                    onClick = {
                        if (isFormatEditable) {
                            showArtistPicker = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Artist",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = artistName.ifEmpty { "Select Artist..." },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = { showArtistPicker = true },
                            enabled = isFormatEditable,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = "Pick Artist",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(MelofySpacing.xxs))
                            Text("Pick Artist")
                        }
                    }
                }

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it; errorMessage = null },
                    label = { Text("Album") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it; errorMessage = null },
                    label = { Text("Album Artist") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it; errorMessage = null },
                    label = { Text("Genre") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it; errorMessage = null },
                    label = { Text("Composer") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it; errorMessage = null },
                        label = { Text("Year") },
                        enabled = isFormatEditable,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = trackText,
                        onValueChange = { trackText = it; errorMessage = null },
                        label = { Text("Track #") },
                        enabled = isFormatEditable,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = discText,
                        onValueChange = { discText = it; errorMessage = null },
                        label = { Text("Disc #") },
                        enabled = isFormatEditable,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isFormatEditable,
                onClick = {
                    val result = MetadataEditValidator.validate(
                        title = title,
                        artist = artistName,
                        album = album,
                        albumArtist = albumArtist,
                        genre = genre,
                        composer = composer,
                        yearText = yearText,
                        trackNumberText = trackText,
                        discNumberText = discText,
                        artistId = selectedArtistId
                    )
                    if (result.isValid && result.sanitizedMetadata != null) {
                        onSave(result.sanitizedMetadata)
                    } else {
                        errorMessage = result.errorMessage ?: "Validation failed."
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showArtistPicker) {
        ArtistPickerDialog(
            currentArtistName = artistName,
            currentArtistId = selectedArtistId,
            artists = availableArtists,
            onArtistSelected = { selected ->
                selectedArtistId = selected.id
                artistName = selected.name
                errorMessage = null
            },
            onCreateNewArtist = { proposedName ->
                onCreateNewArtist?.invoke(proposedName)
                    ?: Result.failure(IllegalStateException("Create artist handler not provided"))
            },
            onDismiss = { showArtistPicker = false },
            onClearArtist = {
                selectedArtistId = null
                artistName = "Unknown Artist"
            }
        )
    }
}
