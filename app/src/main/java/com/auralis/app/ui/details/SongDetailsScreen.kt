package com.auralis.app.ui.details

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.AudioFormatClassifier
import com.auralis.app.domain.model.Song
import com.auralis.app.domain.model.SongDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SongDetailsScreen(
    songId: Long,
    viewModel: SongDetailsViewModel,
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onFindDuplicates: (Song) -> Unit,
    onArtistClick: ((Long?, String) -> Unit)? = null,
    onAlbumClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf(false) }
    var showArtworkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(songId) {
        viewModel.loadSong(songId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Song Information", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshMetadata() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_refresh),
                            contentDescription = "Refresh Metadata"
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = "Edit Metadata"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is SongDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is SongDetailsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is SongDetailsUiState.Success -> {
                val details = state.details
                val song = remember(details) {
                    Song(
                        id = details.id,
                        title = details.basicInfo.title,
                        artistId = null,
                        artistName = details.basicInfo.artist,
                        albumId = null,
                        albumName = details.basicInfo.album,
                        genreId = null,
                        genreName = details.basicInfo.genre,
                        duration = details.basicInfo.durationMs,
                        trackNumber = details.basicInfo.trackNumber,
                        discNumber = details.basicInfo.discNumber,
                        year = details.basicInfo.year,
                        contentUri = details.technicalInfo.contentUri,
                        mimeType = details.audioInfo.mimeType,
                        size = details.fileInfo.sizeBytes,
                        bitrate = details.audioInfo.bitrateKbps,
                        sampleRate = details.audioInfo.sampleRateHz,
                        channelCount = details.audioInfo.channelCount,
                        dateAdded = 0L,
                        dateModified = details.fileInfo.dateModifiedMs / 1000L,
                        albumArtUri = details.technicalInfo.albumArtUri
                    )
                }

                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbarHostState.showSnackbar(it)
                        viewModel.clearMessage()
                    }
                }

                if (showEditDialog) {
                    EditMetadataDialog(
                        songDetails = details,
                        onDismiss = { showEditDialog = false },
                        onSave = { updated ->
                            showEditDialog = false
                            viewModel.updateMetadata(updated)
                        }
                    )
                }

                if (showArtworkDialog) {
                    com.auralis.app.ui.components.ArtworkCustomizationDialog(
                        entityType = com.auralis.app.domain.model.ArtworkEntityType.SONG,
                        entityId = songId.toString(),
                        entityName = details.basicInfo.title,
                        currentArtworkUri = details.technicalInfo.albumArtUri,
                        onDismiss = { showArtworkDialog = false },
                        onSave = { uri ->
                            showArtworkDialog = false
                            viewModel.setCustomArtwork(uri)
                        },
                        onReset = {
                            showArtworkDialog = false
                            viewModel.clearCustomArtwork()
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Artwork & Titles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showArtworkDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = details.technicalInfo.albumArtUri,
                                contentDescription = "Album Artwork",
                                modifier = Modifier.size(110.dp),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_music_placeholder),
                                error = painterResource(R.drawable.ic_music_placeholder),
                                fallback = painterResource(R.drawable.ic_music_placeholder)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = details.basicInfo.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = details.basicInfo.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (onArtistClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (onArtistClick != null) {
                                    Modifier.clickable {
                                        onArtistClick.invoke(null, details.basicInfo.artist)
                                    }
                                } else Modifier
                            )
                            val isAlbumClickable = onAlbumClick != null && details.basicInfo.album.isNotBlank() && !details.basicInfo.album.equals("Unknown", ignoreCase = true)
                            Text(
                                text = details.basicInfo.album,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isAlbumClickable) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = if (isAlbumClickable) {
                                    Modifier.clickable { onAlbumClick?.invoke(details.basicInfo.album) }
                                } else Modifier
                            )
                        }
                    }

                    // Quality Badges
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(if (details.audioInfo.isLossless) "Lossless" else "Lossy")
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (details.audioInfo.isLossless)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(details.audioInfo.formatName) }
                        )
                        if (details.audioInfo.bitrateKbps != null && details.audioInfo.bitrateKbps > 0) {
                            AssistChip(
                                onClick = {},
                                label = { Text(AudioFormatClassifier.formatBitrate(details.audioInfo.bitrateKbps)) }
                            )
                        }
                        if (details.audioInfo.sampleRateHz != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text(AudioFormatClassifier.formatSampleRate(details.audioInfo.sampleRateHz)) }
                            )
                        }
                        if (details.audioInfo.bitDepth != null) {
                            AssistChip(
                                onClick = {},
                                label = { Text(AudioFormatClassifier.formatBitDepth(details.audioInfo.bitDepth)) }
                            )
                        }
                    }

                    // Actions Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onPlaySong(song) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_play_arrow), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play")
                        }
                        FilledTonalButton(
                            onClick = { onAddToQueue(song) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_queue_music), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Queue")
                        }
                        OutlinedButton(
                            onClick = { viewModel.toggleFavorite() }
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (state.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                                ),
                                contentDescription = "Favorite",
                                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddToPlaylist(song) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_playlist_play), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Playlist")
                        }
                        OutlinedButton(
                            onClick = { onFindDuplicates(song) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_file_copy), contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duplicates")
                        }
                    }

                    // Change Artwork Button
                    OutlinedButton(
                        onClick = { showArtworkDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_image), contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change Artwork")
                    }

                    // Audio Specifications Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Audio Specifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            DetailRow("Format", details.audioInfo.formatName)
                            DetailRow("Codec / MIME", details.audioInfo.codec ?: details.audioInfo.mimeType ?: "Unknown")
                            DetailRow("Duration", TimeUtils.formatDuration(details.basicInfo.durationMs))
                            DetailRow("Bitrate", AudioFormatClassifier.formatBitrate(details.audioInfo.bitrateKbps))
                            DetailRow("Sample Rate", AudioFormatClassifier.formatSampleRate(details.audioInfo.sampleRateHz))
                            DetailRow("Channels", AudioFormatClassifier.formatChannels(details.audioInfo.channelCount))
                            DetailRow("Bit Depth", AudioFormatClassifier.formatBitDepth(details.audioInfo.bitDepth))
                            DetailRow("Compression", if (details.audioInfo.isLossless) "Lossless (Original audio preserved)" else "Lossy")
                        }
                    }

                    // Storage & File Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Storage & File Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            DetailRow("File Name", details.fileInfo.fileName)
                            DetailRow("File Size", AudioFormatClassifier.formatFileSize(details.fileInfo.sizeBytes))
                            val dateStr = remember(details.fileInfo.dateModifiedMs) {
                                if (details.fileInfo.dateModifiedMs > 0) {
                                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(details.fileInfo.dateModifiedMs))
                                } else "Unknown"
                            }
                            DetailRow("Date Modified", dateStr)
                            DetailRow("Location", details.fileInfo.filePath ?: "Scoped Storage (Protected)")
                            DetailRow("Content URI", details.fileInfo.contentUri)
                        }
                    }

                    // Metadata Tags Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Metadata Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            DetailRow("Track Number", details.basicInfo.trackNumber?.toString() ?: "Unknown")
                            DetailRow("Disc Number", details.basicInfo.discNumber?.toString() ?: "Unknown")
                            DetailRow("Release Year", details.basicInfo.year?.toString() ?: "Unknown")
                            DetailRow("Genre", details.basicInfo.genre ?: "Unknown")
                            DetailRow("Album Artist", details.basicInfo.albumArtist ?: "Unknown")
                            DetailRow("Composer", details.basicInfo.composer ?: "Unknown")
                        }
                    }

                    // Technical Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Technical Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            HorizontalDivider()
                            DetailRow("MediaStore ID", details.technicalInfo.mediaStoreId.toString())
                            DetailRow("Writable Status", if (details.technicalInfo.isWritable) "Read / Write" else "Read Only (Protected by Scoped Storage)")
                            DetailRow("Privacy Mode", "100% Local / Offline (0 Network Access)")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
