package com.auralis.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.LocalArtworkCornerStyle
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.domain.model.ArtworkCornerStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.auralis.app.R
import com.auralis.app.domain.model.ArtworkEntityType
import com.auralis.app.domain.model.Playlist
import com.auralis.app.domain.model.Song
import kotlin.math.abs

/**
 * Deterministic color palette generator for fallback artwork.
 * Produces subtle, deep atmospheric gradients in harmony with Melofi's dark aesthetic.
 * Low-contrast, non-distracting, and intentional.
 */
private val FALLBACK_ATMOSPHERIC_PAIRS = listOf(
    listOf(Color(0xFF161424), Color(0xFF1E1A33), Color(0xFF2B2446)), // Deep Melofi Violet Slate
    listOf(Color(0xFF121726), Color(0xFF172036), Color(0xFF1F2D4A)), // Deep Indigo Obsidian
    listOf(Color(0xFF111E1C), Color(0xFF172B28), Color(0xFF1F3A36)), // Deep Emerald Slate
    listOf(Color(0xFF221419), Color(0xFF2E1922), Color(0xFF3F212E)), // Deep Crimson Slate
    listOf(Color(0xFF201811), Color(0xFF2C2116), Color(0xFF3C2C1D)), // Deep Amber Slate
    listOf(Color(0xFF111D1F), Color(0xFF16272A), Color(0xFF1E373A)), // Deep Teal Slate
    listOf(Color(0xFF1A1524), Color(0xFF241C33), Color(0xFF312547)), // Atmospheric Midnight
    listOf(Color(0xFF14141E), Color(0xFF1A1A2A), Color(0xFF24243B))  // Obsidian Slate
)

fun getDeterministicGradient(seed: String): Brush {
    val hash = abs(seed.hashCode())
    val colors = FALLBACK_ATMOSPHERIC_PAIRS[hash % FALLBACK_ATMOSPHERIC_PAIRS.size]
    return Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

/**
 * Premium universal local artwork component.
 * Loads artwork via Coil with smooth crossfading, respecting user-selected ArtworkCornerStyle,
 * and provides a refined, low-contrast atmospheric fallback tailored to the entity type.
 */
@Composable
fun MelofyArtwork(
    artworkUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    artist: String? = null,
    entityType: ArtworkEntityType? = null,
    cornerRadius: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
    showInitial: Boolean = false
) {
    val cornerStyle = LocalArtworkCornerStyle.current
    val animationMode = LocalAnimationMode.current
    val effectiveRadius = cornerRadius ?: when (entityType) {
        ArtworkEntityType.ARTIST -> 70.dp
        else -> cornerStyle.cornerRadiusDp.dp
    }
    val shape = remember(effectiveRadius) { RoundedCornerShape(effectiveRadius) }
    val gradient = remember(title, artist) { getDeterministicGradient(title + (artist ?: "")) }
    val initial = remember(title, entityType) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty()) {
            trimmed.first().uppercaseChar().toString()
        } else {
            when (entityType) {
                ArtworkEntityType.ARTIST -> "A"
                ArtworkEntityType.ALBUM -> "O"
                ArtworkEntityType.GENRE -> "G"
                ArtworkEntityType.PLAYLIST -> "P"
                else -> "♪"
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = artworkUri,
            animationSpec = MelofyMotion.adaptiveTween<Float>(animationMode, MelofyMotion.DURATION_NORMAL),
            label = "artworkCrossfade"
        ) { uri ->
            if (!uri.isNullOrBlank()) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Cover for $title",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            } else if (showInitial) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = initial,
                        color = Color.White.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(
                            when (entityType) {
                                ArtworkEntityType.PLAYLIST -> R.drawable.ic_playlist_play
                                ArtworkEntityType.ARTIST -> R.drawable.ic_person
                                else -> R.drawable.ic_music_placeholder
                            }
                        ),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxSize(0.45f)
                    )
                }
            }
        }
    }
}

/**
 * Playlist cover component with custom cover display and automatic multi-panel collage fallback.
 */
@Composable
fun PlaylistCover(
    playlist: Playlist,
    songs: List<Song> = emptyList(),
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    PlaylistCover(
        customCoverUri = playlist.customCoverUri,
        memberArtUris = songs.mapNotNull { it.albumArtUri },
        title = playlist.name,
        modifier = modifier,
        cornerRadius = cornerRadius
    )
}

@Composable
fun PlaylistCover(
    customCoverUri: String?,
    memberArtUris: List<String> = emptyList(),
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!customCoverUri.isNullOrBlank()) {
            // Custom user-selected local cover
            AsyncImage(
                model = customCoverUri,
                contentDescription = "Custom cover for $title",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            val distinctArts = remember(memberArtUris) {
                memberArtUris
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(4)
            }

            when {
                distinctArts.size >= 4 -> {
                    // 2x2 Artwork Grid Collage (4 items)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = distinctArts[0],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = distinctArts[1],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = distinctArts[2],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = distinctArts[3],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                distinctArts.size == 3 -> {
                    // Balanced 3-panel collage (top row 2 images, bottom row 1 spanning image)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            AsyncImage(
                                model = distinctArts[0],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                            AsyncImage(
                                model = distinctArts[1],
                                contentDescription = null,
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        AsyncImage(
                            model = distinctArts[2],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                distinctArts.size == 2 -> {
                    // 2-panel split layout (side-by-side vertical panels)
                    Row(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = distinctArts[0],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        AsyncImage(
                            model = distinctArts[1],
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                distinctArts.size == 1 -> {
                    // Single cover takes the full container
                    AsyncImage(
                        model = distinctArts[0],
                        contentDescription = "Cover for $title",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // Subtle Melofi fallback with playlist symbol
                    val gradient = remember(title) { getDeterministicGradient(title) }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_playlist_play),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxSize(0.42f)
                        )
                    }
                }
            }
        }
    }
}
