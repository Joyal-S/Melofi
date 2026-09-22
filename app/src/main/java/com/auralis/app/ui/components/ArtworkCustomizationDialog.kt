package com.auralis.app.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.domain.model.ArtworkEntityType

/**
 * Universal offline artwork customization dialog for Songs, Albums, Artists, Genres, and Playlists.
 *
 * Provides:
 * - [ Choose Image ] — launches the system image picker (JPG, PNG, WebP)
 * - [ Preview ] — displays high-fidelity preview of chosen picture with Crop vs Fit preview modes
 * - [ Save ] — persists image downsampled in app-private storage
 * - [ Reset ] — removes custom override with confirmation, returning immediately to original embedded/fallback art
 */
@Composable
fun ArtworkCustomizationDialog(
    entityType: ArtworkEntityType,
    onDismiss: () -> Unit,
    onSave: (Uri) -> Unit,
    onReset: () -> Unit,
    title: String = "",
    entityId: String = "",
    entityName: String = "",
    currentArtworkUri: String? = null,
    hasCustomOverride: Boolean = !currentArtworkUri.isNullOrBlank(),
    entityTitle: String = ""
) {
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isCropMode by remember { mutableStateOf(true) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val displayTitle = title.ifBlank {
        entityName.ifBlank {
            entityTitle.ifBlank {
                when (entityType) {
                    ArtworkEntityType.SONG -> "Song Artwork"
                    ArtworkEntityType.ALBUM -> "Album Cover"
                    ArtworkEntityType.ARTIST -> "Artist Image"
                    ArtworkEntityType.GENRE -> "Genre Cover"
                    ArtworkEntityType.PLAYLIST -> "Playlist Cover"
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    val previewUri = selectedUri?.toString() ?: currentArtworkUri

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Preview Card
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!previewUri.isNullOrBlank()) {
                        AsyncImage(
                            model = previewUri,
                            contentDescription = "Artwork Preview",
                            modifier = Modifier.size(220.dp),
                            contentScale = if (isCropMode) ContentScale.Crop else ContentScale.Fit
                        )
                    } else {
                        MelofyArtwork(
                            artworkUri = null,
                            title = entityTitle.ifEmpty { title },
                            entityType = entityType,
                            modifier = Modifier.size(220.dp),
                            cornerRadius = 16.dp
                        )
                    }
                }

                // Crop (Fill) vs Fit (Center) Preview Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isCropMode,
                        onClick = { isCropMode = true },
                        label = { Text("Crop (Fill)") },
                        leadingIcon = if (isCropMode) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = !isCropMode,
                        onClick = { isCropMode = false },
                        label = { Text("Fit (Center)") },
                        leadingIcon = if (!isCropMode) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }

                // Status message
                when {
                    selectedUri != null -> {
                        Text(
                            text = "New image selected — tap Save to apply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    hasCustomOverride -> {
                        Text(
                            text = "Custom local artwork override is active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Text(
                            text = "Using original embedded / local fallback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Offline Guarantee Notice
                Text(
                    text = "100% offline. Images are safely downsampled and kept in app storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // [ Choose Image ] Button
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_image),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedUri != null) "Choose Different Image" else "Choose Image")
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedUri != null) {
                    Button(
                        onClick = {
                            selectedUri?.let { onSave(it) }
                            onDismiss()
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasCustomOverride) {
                    TextButton(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (entityType == ArtworkEntityType.PLAYLIST) "Reset to Automatic" else "Reset to Original")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text(if (entityType == ArtworkEntityType.PLAYLIST) "Reset to Automatic Cover" else "Reset Artwork") },
            text = {
                Text(
                    if (entityType == ArtworkEntityType.PLAYLIST)
                        "Remove the custom playlist cover and restore the automatic collage?"
                    else
                        "Remove the custom artwork and restore the original image?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        onReset()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
