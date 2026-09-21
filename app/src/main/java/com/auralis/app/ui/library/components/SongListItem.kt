package com.auralis.app.ui.library.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.util.TimeUtils
import com.auralis.app.domain.model.Song

/**
 * High-performance, Material 3 song item component with reactive favorite toggle,
 * animated micro-interaction, accessible touch targets, and context actions menu.
 */
@Composable
fun SongListItem(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    onShowDetails: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "favoriteBounce"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = MelofySpacing.lg, vertical = MelofySpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art thumbnail with local vector fallback
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = "Cover art for ${song.title}",
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_music_placeholder),
                error = painterResource(R.drawable.ic_music_placeholder),
                fallback = painterResource(R.drawable.ic_music_placeholder)
            )
        }

        Spacer(modifier = Modifier.width(MelofySpacing.md))

        // Title and Artist/Album
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = MelofySpacing.xs)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${song.artistName} • ${song.albumName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Formatted track duration
        Text(
            text = TimeUtils.formatDuration(song.duration),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = MelofySpacing.xxs)
        )

        // Favorite Toggle Action (Accessible 44dp touch target)
        if (onToggleFavorite != null) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(44.dp)
                    .scale(heartScale)
            ) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                    ),
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Context Actions Menu (Add to Playlist, Play Next, Song Details)
        if (onAddToPlaylist != null || onPlayNext != null || onShowDetails != null) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "More options for ${song.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onPlayNext != null) {
                        DropdownMenuItem(
                            text = { Text("Play Next") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_queue_music),
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPlayNext()
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
                }
            }
        }
    }
}
