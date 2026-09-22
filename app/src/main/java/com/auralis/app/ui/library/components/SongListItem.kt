package com.auralis.app.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.Song
import com.auralis.app.ui.components.MelofyArtwork

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions
import com.auralis.app.core.ui.theme.MelofyTypographyTokens

/**
 * High-performance, premium Material 3 song item component.
 * Features rich deterministic artwork fallback, subtle quality badge,
 * reactive favorite toggle with spring micro-interaction, graphicsLayer press feedback,
 * and accessible touch targets.
 */
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    isCompact: Boolean = false,
    showQualityBadge: Boolean = true,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onPlay: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onChangeArtwork: (() -> Unit)? = null,
    onEditMetadata: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShowDetails: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    val animationMode = LocalAnimationMode.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.985f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "songItemPressScale"
    )

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite && animationMode != MelofyAnimationMode.OFF) 1.2f else 1.0f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "favoriteBounce"
    )

    // Compute optional quality text
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

    val artworkSize = if (isCompact) 40.dp else 50.dp
    val artworkRadius = if (isCompact) 8.dp else 10.dp
    val verticalPadding = if (isCompact) MelofySpacing.xxs else MelofySpacing.xs
    val buttonSize = MelofyDimensions.minTouchTarget
    val iconSize = if (isCompact) 18.dp else 22.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(horizontal = MelofySpacing.lg, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Deterministic Artwork with initials / gradient fallback
        MelofyArtwork(
            artworkUri = song.albumArtUri,
            title = song.title,
            artist = song.artistName,
            modifier = Modifier.size(artworkSize),
            cornerRadius = artworkRadius
        )

        Spacer(modifier = Modifier.width(MelofySpacing.md))

        // Title and Artist/Album
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = MelofySpacing.xs)
        ) {
            Text(
                text = song.title,
                style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showQualityBadge && qualityTag != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = qualityTag,
                            style = MelofyTypographyTokens.qualityBadge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Text(
                    text = "${song.artistName} • ${song.albumName}",
                    style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Formatted track duration
        Text(
            text = TimeUtils.formatDuration(song.duration),
            style = MelofyTypographyTokens.durationText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = MelofySpacing.xxs)
        )

        // Favorite Toggle Action
        if (onToggleFavorite != null) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(buttonSize)
                    .scale(heartScale)
            ) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    ),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // Context Actions Menu (Play, Add to Queue, Add to Playlist, Favorite, Details, Edit, Change Artwork, Delete)
        if (onPlay != null || onAddToQueue != null || onPlayNext != null || onAddToPlaylist != null || onShowDetails != null || onChangeArtwork != null || onEditMetadata != null || onDelete != null) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(buttonSize)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options for ${song.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(iconSize)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onPlay != null) {
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
                    }
                    if (onAddToQueue != null || onPlayNext != null) {
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
                                (onAddToQueue ?: onPlayNext)?.invoke()
                            }
                        )
                    }
                    if (onAddToPlaylist != null) {
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
                    }
                    if (onToggleFavorite != null) {
                        DropdownMenuItem(
                            text = { Text(if (isFavorite) "Remove from Favorites" else "Favorite") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                                    contentDescription = null,
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                showMenu = false
                                onToggleFavorite()
                            }
                        )
                    }
                    if (onShowDetails != null) {
                        DropdownMenuItem(
                            text = { Text("Song Details") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onShowDetails()
                            }
                        )
                    }
                    if (onEditMetadata != null) {
                        DropdownMenuItem(
                            text = { Text("Edit Metadata") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_edit),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditMetadata()
                            }
                        )
                    }
                    if (onChangeArtwork != null) {
                        DropdownMenuItem(
                            text = { Text("Change Artwork") },
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
                    }
                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
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
