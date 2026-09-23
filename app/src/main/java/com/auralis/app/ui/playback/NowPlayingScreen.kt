package com.auralis.app.ui.playback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.R
import com.auralis.app.core.ui.theme.LocalAnimationMode
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.core.ui.theme.MelofyDimensions
import com.auralis.app.core.ui.theme.MelofyElevation
import com.auralis.app.core.ui.theme.MelofyMotion
import com.auralis.app.core.ui.theme.MelofySpacing
import com.auralis.app.core.ui.theme.MelofyTypographyTokens
import com.auralis.app.domain.model.Song
import com.auralis.app.playback.model.PlaybackRepeatMode
import com.auralis.app.ui.components.MelofyArtwork
import com.auralis.app.ui.playback.components.MelofiWaveformScrubber
import com.auralis.app.ui.playback.components.PlaybackQueueSheet

/**
 * Premium, fluid, and minimal full-screen Now Playing playback experience.
 *
 * Adheres strictly to the architectural constraints:
 * - 100% offline playback; strictly zero online/lyrics/streaming clutter.
 * - Reuses existing Media3 engine, PlaybackViewModel, and DSP pipelines.
 * - Preserves Phase 11 performance invariants: no 500ms recomposition loops at root.
 * - Diamond transport control cluster: Shuffle above, Previous-Play-Next center, Repeat below.
 * - Minimal secondary row: Add to Playlist, Queue, Audio Effects, More options.
 * - Dynamic accent theming across AMOLED, Dark, and Light environments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    onCollapse: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: (Long) -> Unit,
    onOpenAudioSettings: (() -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    onSongDetailsClick: ((Song) -> Unit)? = null,
    onArtistClick: ((Long?, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animationMode = LocalAnimationMode.current
    val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val activeQueue by playbackViewModel.activeQueue.collectAsStateWithLifecycle()
    val dspParams by playbackViewModel.dspParameters.collectAsStateWithLifecycle()
    val song = playbackState.currentSong ?: return

    DisposableEffect(Unit) {
        playbackViewModel.setHighFrequencyTracking(true)
        onDispose {
            playbackViewModel.setHighFrequencyTracking(false)
        }
    }

    var showQueueSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val heartScale by animateFloatAsState(
        targetValue = if (isFavorite && animationMode != MelofyAnimationMode.OFF) 1.22f else 1.0f,
        animationSpec = MelofyMotion.springBouncy(),
        label = "heartScale"
    )

    // Compute audio quality tag
    val qualityTag = remember(song.mimeType, song.bitrate, song.sampleRate) {
        val mime = song.mimeType
        when {
            mime != null && mime.contains("flac", ignoreCase = true) -> {
                if (song.sampleRate != null && song.sampleRate >= 96000) "HI-RES • FLAC ${song.sampleRate / 1000}kHz"
                else "LOSSLESS • FLAC"
            }
            mime != null && mime.contains("wav", ignoreCase = true) -> "LOSSLESS • WAV"
            mime != null && (mime.contains("ogg", ignoreCase = true) || mime.contains("opus", ignoreCase = true)) -> "OGG OPUS"
            song.bitrate != null && song.bitrate >= 320 -> "320 KBPS • MP3"
            song.bitrate != null && song.bitrate in 256..319 -> "256 KBPS • AAC"
            song.sampleRate != null && song.sampleRate >= 96000 -> "HI-RES • ${song.sampleRate / 1000}kHz"
            else -> null
        }
    }

    val playingFromText = remember(song.albumName, isFavorite) {
        when {
            isFavorite -> "LIKED SONGS"
            song.albumName.isNotBlank() && !song.albumName.equals("Unknown", ignoreCase = true) -> song.albumName.uppercase()
            else -> "LIBRARY TRACKS"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Atmospheric subtle radial bloom matching user-selected theme accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ============================================================
            // TOP HEADER: Collapse Button, PLAYING FROM & Indicator, More Menu
            // ============================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MelofySpacing.md, vertical = MelofySpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TactileIconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                    animationMode = animationMode
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more),
                        contentDescription = "Collapse Player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = MelofySpacing.sm)
                ) {
                    Text(
                        text = "PLAYING FROM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        letterSpacing = 1.4.sp
                    )
                    Text(
                        text = playingFromText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Minimal indicator bar
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .width(32.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    )
                }

                Box {
                    TactileIconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Song Details") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_info),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onSongDetailsClick?.invoke(song)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onAddToPlaylist?.invoke(song)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Audio Effects & DSP") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_tune),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            onClick = {
                                showMoreMenu = false
                                onOpenAudioSettings?.invoke()
                            }
                        )
                    }
                }
            }

            // ============================================================
            // SCROLLABLE PLAYBACK BODY
            // ============================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MelofySpacing.xl, vertical = MelofySpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(MelofySpacing.xs))

                // 1. Large Hero Album Artwork with Ambient Shadow & Soft Rounded Corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.92f)
                            .shadow(
                                elevation = MelofyElevation.high,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            )
                    )

                    MelofyArtwork(
                        artworkUri = song.albumArtUri,
                        title = song.title,
                        artist = song.artistName,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(MelofyElevation.medium, RoundedCornerShape(22.dp)),
                        cornerRadius = 22.dp
                    )
                }

                Spacer(modifier = Modifier.height(MelofySpacing.lg))

                // 2. Song Title, Artist & Tactile Favorite Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = MelofySpacing.sm)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artistName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable {
                                onArtistClick?.invoke(song.artistId, song.artistName)
                            }
                        )
                    }

                    TactileIconButton(
                        onClick = { onToggleFavorite(song.id) },
                        modifier = Modifier
                            .size(MelofyDimensions.minTouchTarget)
                            .scale(heartScale),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                            ),
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Audio Format Quality & Active DSP Badges Row
                if (qualityTag != null || dspParams.isAnyActive) {
                    Spacer(modifier = Modifier.height(MelofySpacing.xs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MelofySpacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        qualityTag?.let { tag ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MelofyTypographyTokens.qualityBadge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = MelofySpacing.sm, vertical = 2.dp)
                                )
                            }
                        }

                        if (dspParams.isAnyActive) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { playbackViewModel.toggleDsp() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = MelofySpacing.sm, vertical = 2.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_waves),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(MelofySpacing.xs))
                                    Text(
                                        text = "Lo-Fi • ${dspParams.preset.displayName}",
                                        style = MelofyTypographyTokens.qualityBadge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // 3. Minimal Waveform Scrubber with Flanking Timestamps
                MelofiWaveformScrubber(
                    playbackViewModel = playbackViewModel,
                    durationMs = playbackState.durationMs,
                    songId = song.id
                )

                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // 4. Main Playback Controls (Diamond arrangement):
                //           Shuffle
                //    Previous  PLAY/PAUSE  Next
                //           Repeat
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Shuffle Toggle
                    TactileIconButton(
                        onClick = { playbackViewModel.toggleShuffle() },
                        modifier = Modifier.size(42.dp),
                        animationMode = animationMode
                    ) {
                        val shuffleActive = playbackState.shuffleEnabled
                        Box(contentAlignment = Alignment.Center) {
                            if (shuffleActive) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.ic_shuffle),
                                contentDescription = if (shuffleActive) "Shuffle on" else "Shuffle off",
                                tint = if (shuffleActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Middle Row: Previous, Play/Pause, Next
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Track
                        TactileIconButton(
                            onClick = { playbackViewModel.previous() },
                            modifier = Modifier.size(52.dp),
                            animationMode = animationMode
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Previous Track",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(MelofySpacing.xl))

                        // Central Play/Pause Button with ambient accent glow & rotating morph
                        val playPauseInteraction = remember { MutableInteractionSource() }
                        val isPlayPausePressed by playPauseInteraction.collectIsPressedAsState()
                        val playPauseScale by animateFloatAsState(
                            targetValue = if (isPlayPausePressed && animationMode != MelofyAnimationMode.OFF) 0.93f else 1.0f,
                            animationSpec = MelofyMotion.springSubtle(),
                            label = "playPauseTactile"
                        )
                        val playPauseRotation by animateFloatAsState(
                            targetValue = if (playbackState.isPlaying) 0f else -90f,
                            animationSpec = MelofyMotion.springBouncy(),
                            label = "nowPlayingPlayPauseRotation"
                        )

                        FilledIconButton(
                            onClick = { playbackViewModel.togglePlayPause() },
                            interactionSource = playPauseInteraction,
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = CircleShape,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                )
                                .graphicsLayer {
                                    scaleX = playPauseScale
                                    scaleY = playPauseScale
                                },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            AnimatedContent(
                                targetState = playbackState.isPlaying,
                                transitionSpec = {
                                    if (animationMode == MelofyAnimationMode.OFF) {
                                        androidx.compose.animation.EnterTransition.None togetherWith androidx.compose.animation.ExitTransition.None
                                    } else {
                                        (scaleIn(initialScale = 0.82f, animationSpec = MelofyMotion.springSubtle<Float>()) + fadeIn(tween(140))).togetherWith(
                                            scaleOut(targetScale = 0.82f, animationSpec = MelofyMotion.springSubtle<Float>()) + fadeOut(tween(100))
                                        )
                                    }
                                },
                                label = "nowPlayingPlayPauseMorph"
                            ) { isPlaying ->
                                Icon(
                                    painter = painterResource(
                                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                                    ),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(MelofySpacing.xl))

                        // Next Track
                        TactileIconButton(
                            onClick = { playbackViewModel.next() },
                            modifier = Modifier.size(52.dp),
                            animationMode = animationMode
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_next),
                                contentDescription = "Next Track",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bottom: Repeat Toggle
                    TactileIconButton(
                        onClick = { playbackViewModel.cycleRepeatMode() },
                        modifier = Modifier.size(42.dp),
                        animationMode = animationMode
                    ) {
                        val repeatActive = playbackState.repeatMode != PlaybackRepeatMode.OFF
                        val repeatIcon = if (playbackState.repeatMode == PlaybackRepeatMode.ONE) {
                            R.drawable.ic_repeat_one
                        } else {
                            R.drawable.ic_repeat
                        }
                        Box(contentAlignment = Alignment.Center) {
                            if (repeatActive) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                )
                            }
                            Icon(
                                painter = painterResource(repeatIcon),
                                contentDescription = "Repeat Mode: ${playbackState.repeatMode}",
                                tint = if (repeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.md))

                // 5. Minimal Secondary Actions Row: Add to Playlist, Queue, Audio Effects, More
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MelofySpacing.md),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TactileIconButton(
                        onClick = { onAddToPlaylist?.invoke(song) },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add to Playlist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    TactileIconButton(
                        onClick = { showQueueSheet = true },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_queue_music),
                            contentDescription = "Play Queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Compact Lo-Fi Quick Toggle (Requirement 6)
                    val dspActive = dspParams.isAnyActive
                    TactileIconButton(
                        onClick = { playbackViewModel.toggleDsp() },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (dspActive) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_waves),
                                    contentDescription = if (dspActive) "Lo-Fi ON (Tap to toggle)" else "Lo-Fi OFF (Tap to toggle)",
                                    tint = if (dspActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                                if (dspActive) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 1.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }

                    TactileIconButton(
                        onClick = { onOpenAudioSettings?.invoke() },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = "Audio Effects & DSP",
                            tint = if (dspParams.isAnyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    TactileIconButton(
                        onClick = { onSongDetailsClick?.invoke(song) },
                        modifier = Modifier.size(MelofyDimensions.minTouchTarget),
                        animationMode = animationMode
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_info),
                            contentDescription = "Song Information",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MelofySpacing.sm))
            }
        }

        // Active Playback Queue Bottom Sheet
        if (showQueueSheet) {
            PlaybackQueueSheet(
                queue = activeQueue,
                currentSong = song,
                sheetState = queueSheetState,
                onDismiss = { showQueueSheet = false },
                onSongSelect = { selectedSong, queue ->
                    playbackViewModel.playSong(selectedSong, queue)
                }
            )
        }
    }
}

@Composable
private fun TactileIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationMode: MelofyAnimationMode = MelofyAnimationMode.FULL,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && animationMode != MelofyAnimationMode.OFF) 0.91f else 1.0f,
        animationSpec = MelofyMotion.springSubtle(),
        label = "tactileScale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        content()
    }
}
