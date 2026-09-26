package com.auralis.app.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.ui.theme.MelofyTypographyTokens
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.components.MelofyArtwork

/**
 * Polished, touch-target compliant Grid card for individual songs in Library Grid mode.
 * Includes tactile press feedback, audio quality tag, duration readout,
 * favorite toggle, and comprehensive context actions.
 */
@Composable
fun SongGridItem(
    song: Song,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onChangeArtwork: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier,
    onHideFromLibrary: (() -> Unit)? = null
) {
    val animationMode = LocalAnimationMode.current
    var showMenu by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.97f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "gridItemScale"
    )

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite && animationMode != MelofyAnimationMode.OFF) 1.2f else 1.0f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "gridFavoriteBounce"
    )

    val qualityTag = remember(song.mimeType, song.bitrate, song.sampleRate) {
        val mime = song.mimeType
        when {
            mime != null && mime.contains("flac", ignoreCase = true) -> "FLAC"
            mime != null && mime.contains("wav", ignoreCase = true) -> "WAV"
            mime != null && (mime.contains("ogg", ignoreCase = true) || mime.contains("opus", ignoreCase = true)) -> "OGG"
            song.bitrate != null && song.bitrate >= 320 -> "320k"
            song.bitrate != null && song.bitrate in 256..319 -> "256k"
            song.sampleRate != null && song.sampleRate >= 96000 -> "96k"
            else -> null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = MelofyElevation.low)
    ) {
        Column(modifier = Modifier.padding(MelofySpacing.sm)) {
            // Artwork with overlay duration pill and optional quality tag
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                MelofyArtwork(
                    artworkUri = song.albumArtUri,
                    title = song.title,
                    artist = song.artistName,
                    modifier = Modifier.matchParentSize(),
                    cornerRadius = 10.dp
                )

                // Top quality badge
                qualityTag?.let { tag ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MelofyTypographyTokens.qualityBadge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom duration tag
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDuration(song.duration),
                        style = MelofyTypographyTokens.durationText,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MelofySpacing.xs))

            // Title & Artist
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Action row: Favorite & More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(MelofyDimensions.minTouchTarget)
                        .scale(heartScale)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                        ),
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = "Song options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Play") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_play_arrow),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPlay()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Play Next") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_skip_next),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPlayNext()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Queue") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_queue_music),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onAddToQueue()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_playlist_play),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onAddToPlaylist()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Song Details") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tune),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onShowDetails()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change Cover") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_image),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onChangeArtwork()
                            }
                        )
                        if (onHideFromLibrary != null) {
                            DropdownMenuItem(
                                text = { Text("Hide from Music Library") },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onHideFromLibrary()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Track") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
