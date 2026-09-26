package com.auralis.app.ui.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.util.ArtistNameNormalizer
import com.auralis.app.domain.model.Artist
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.DuplicateArtistException
import com.auralis.app.ui.components.MelofyArtwork
import kotlinx.coroutines.launch

/**
 * Dedicated Artist Picker modal dialog.
 *
 * Implements Melofi's Artist Identity and Assignment specification:
 * - Direct selection of existing persistent artists (avoids free-text divergence)
 * - Real-time client-side search across all persistent artists
 * - "+ Create New Artist" workflow with strict normalized uniqueness validation
 * - Duplicate conflict detection with "[ Use Existing Artist ]" recovery action
 */
@Composable
fun ArtistPickerDialog(
    currentArtistName: String,
    currentArtistId: Long?,
    artists: List<Artist>,
    onArtistSelected: (Artist) -> Unit,
    onCreateNewArtist: suspend (String) -> Result<Artist>,
    onDismiss: () -> Unit,
    onClearArtist: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filteredArtists = remember(artists, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            artists.sortedBy { it.name.lowercase() }
        } else {
            artists.filter {
                it.name.contains(q, ignoreCase = true) ||
                ArtistNameNormalizer.normalize(it.name).contains(ArtistNameNormalizer.normalize(q))
            }.sortedBy { it.name.lowercase() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(MelofySpacing.sm))
                Text(
                    text = "Select Artist",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MelofySpacing.sm)
            ) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search artists...") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Create New Artist Button
                FilledTonalButton(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(MelofySpacing.xs))
                    Text(
                        text = "Create New Artist",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Artists List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    if (filteredArtists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MelofySpacing.xl),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "No artists available" else "No matching artists found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredArtists, key = { it.id }) { artist ->
                                val isSelected = if (currentArtistId != null) {
                                    artist.id == currentArtistId
                                } else {
                                    ArtistNameNormalizer.areEquivalent(artist.name, currentArtistName)
                                }

                                Surface(
                                    onClick = {
                                        onArtistSelected(artist)
                                        onDismiss()
                                    },
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = MelofySpacing.sm, vertical = MelofySpacing.xs)
                                    ) {
                                        MelofyArtwork(
                                            artworkUri = artist.artUri,
                                            title = artist.name,
                                            entityType = ArtworkEntityType.ARTIST,
                                            modifier = Modifier.size(40.dp),
                                            cornerRadius = 20.dp
                                        )

                                        Spacer(modifier = Modifier.width(MelofySpacing.sm))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = artist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${artist.songCount} ${if (artist.songCount == 1) "song" else "songs"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                onArtistSelected(artist)
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(MelofySpacing.xs)) {
                if (onClearArtist != null) {
                    TextButton(onClick = {
                        onClearArtist()
                        onDismiss()
                    }) {
                        Text("Clear Artist")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showCreateDialog) {
        CreateNewArtistDialog(
            existingArtists = artists,
            onCreate = { newArtist ->
                showCreateDialog = false
                onArtistSelected(newArtist)
                onDismiss()
            },
            onUseExisting = { existing ->
                showCreateDialog = false
                onArtistSelected(existing)
                onDismiss()
            },
            onCreateArtistRequest = onCreateNewArtist,
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun CreateNewArtistDialog(
    existingArtists: List<Artist>,
    onCreate: (Artist) -> Unit,
    onUseExisting: (Artist) -> Unit,
    onCreateArtistRequest: suspend (String) -> Result<Artist>,
    onDismiss: () -> Unit
) {
    var artistNameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var duplicateArtistFound by remember { mutableStateOf<Artist?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Artist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MelofySpacing.sm)
            ) {
                Text(
                    text = "Enter the new artist's name. If an equivalent artist profile already exists, you will be prompted to use the existing profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = artistNameInput,
                    onValueChange = {
                        artistNameInput = it
                        errorMessage = null
                        duplicateArtistFound = null
                    },
                    label = { Text("Artist Name") },
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(MelofySpacing.xs)) {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (duplicateArtistFound != null) {
                            Button(
                                onClick = { onUseExisting(duplicateArtistFound!!) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Use Existing Artist (${duplicateArtistFound!!.name})")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = ArtistNameNormalizer.cleanDisplayName(artistNameInput)
                    val norm = ArtistNameNormalizer.normalize(artistNameInput)
                    if (norm.isEmpty()) {
                        errorMessage = "Artist name cannot be blank."
                        return@Button
                    }

                    // Check for existing equivalent artist profile
                    val duplicate = existingArtists.firstOrNull {
                        ArtistNameNormalizer.normalize(it.name) == norm
                    }
                    if (duplicate != null) {
                        duplicateArtistFound = duplicate
                        errorMessage = "An artist with this name already exists."
                        return@Button
                    }

                    scope.launch {
                        val result = onCreateArtistRequest(clean)
                        result.onSuccess { created ->
                            onCreate(created)
                        }.onFailure { err ->
                            if (err is DuplicateArtistException) {
                                duplicateArtistFound = err.existingArtist
                                errorMessage = "An artist with this name already exists."
                            } else {
                                errorMessage = err.message ?: "Failed to create artist."
                            }
                        }
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
