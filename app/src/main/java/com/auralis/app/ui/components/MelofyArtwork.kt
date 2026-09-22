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
 * Produces rich, harmonious 3-color gradients based on metadata hash codes.
 */
private val FALLBACK_GRADIENT_PAIRS = listOf(
    listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFFC4B5FD)), // Deep Violet
    listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF93C5FD)), // Electric Blue
    listOf(Color(0xFF064E3B), Color(0xFF10B981), Color(0xFF6EE7B7)), // Emerald
    listOf(Color(0xFF881337), Color(0xFFF43F5E), Color(0xFFFDA4AF)), // Rose/Coral
    listOf(Color(0xFF78350F), Color(0xFFF59E0B), Color(0xFFFDE68A)), // Amber/Sunset
    listOf(Color(0xFF134E4A), Color(0xFF14B8A6), Color(0xFF99F6E4)), // Deep Teal
    listOf(Color(0xFF701A75), Color(0xFFD946EF), Color(0xFFF0ABFC)), // Fuchsia
    listOf(Color(0xFF312E81), Color(0xFF6366F1), Color(0xFFA5B4FC))  // Indigo
)

fun getDeterministicGradient(seed: String): Brush {
    val hash = abs(seed.hashCode())
    val colors = FALLBACK_GRADIENT_PAIRS[hash % FALLBACK_GRADIENT_PAIRS.size]
    return Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}

/**
 * Premium universal local artwork component.
 * Loads artwork via Coil with smooth crossfading, respecting user-selected ArtworkCornerStyle,
 * and provides a rich deterministic gradient/monogram fallback tailored to the entity type.
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
    showInitial: Boolean = true
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
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
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
                        tint = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

/**
 * Playlist cover component with custom cover display and automatic 2x2 collage fallback.
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
                    // 2x2 Artwork Grid Collage
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
                distinctArts.isNotEmpty() -> {
                    // Single or up to 3 covers: display primary cover
                    AsyncImage(
                        model = distinctArts.first(),
                        contentDescription = "Cover for $title",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                else -> {
                    // Gradient + playlist monogram fallback
                    val gradient = remember(title) { getDeterministicGradient(title) }
                    val initial = remember(title) { title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "P" }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    }
                }
            }
        }
    }
}
