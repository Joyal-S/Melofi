package com.auralis.app.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.auralis.app.domain.model.EditableMetadata
import com.auralis.app.domain.model.MetadataEditValidator
import com.auralis.app.domain.model.SongDetails

@Composable
fun EditMetadataDialog(
    songDetails: SongDetails,
    onDismiss: () -> Unit,
    onSave: (EditableMetadata) -> Unit
) {
    var title by remember { mutableStateOf(songDetails.basicInfo.title) }
    var artist by remember { mutableStateOf(songDetails.basicInfo.artist) }
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

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it; errorMessage = null },
                    label = { Text("Artist") },
                    enabled = isFormatEditable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                        artist = artist,
                        album = album,
                        albumArtist = albumArtist,
                        genre = genre,
                        composer = composer,
                        yearText = yearText,
                        trackNumberText = trackText,
                        discNumberText = discText
                    )
                    if (result.isValid && result.sanitizedMetadata != null) {
                        onSave(result.sanitizedMetadata)
                    } else {
                        errorMessage = result.errorMessage ?: "Validation failed."
                    }
                }
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
