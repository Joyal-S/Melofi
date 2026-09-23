package com.auralis.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBars
import com.auralis.app.R
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.domain.model.Song
import com.auralis.app.playback.model.PlaybackState
import com.auralis.app.ui.MainTab
import com.auralis.app.ui.playback.components.PlaybackControlsBar

/**
 * Unified, deterministic bottom dock for Melofi's root shell.
 *
 * Enforces strict compositional hierarchy:
 * ┌──────────────────────────────┐
 * │          CONTENT             │
 * ├──────────────────────────────┤
 * │        MINI PLAYER           │
 * ├──────────────────────────────┤
 * │        BOTTOM NAV            │
 * ├──────────────────────────────┤
 * │    SYSTEM NAVIGATION BAR     │
 * └──────────────────────────────┘
 *
 * Guarantees:
 * - Mini-player ALWAYS sits ABOVE the Bottom Navigation.
 * - Zero vertical jump or flickering when switching main tabs.
 * - On sub-screens (where Bottom Navigation is hidden), Mini-Player docks safely
 *   above the system navigation bar via [WindowInsets.navigationBars].
 * - Supports gesture navigation, 3-button navigation, cutouts, and rotation.
 * - Completely hidden during fullscreen Now Playing.
 */
@Composable
fun MelofiBottomDock(
    playbackState: PlaybackState,
    showMiniPlayer: Boolean,
    showBottomNav: Boolean,
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    favoriteSongIds: Set<Long>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    progressProvider: () -> Float,
    animationMode: MelofyAnimationMode,
    modifier: Modifier = Modifier
) {
    if (!showMiniPlayer && !showBottomNav) {
        return
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. Persistent Mini-Player: ALWAYS directly above the Bottom Navigation
            if (showMiniPlayer && playbackState.currentSong != null) {
                PlaybackControlsBar(
                    playbackState = playbackState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekTo = onSeekTo,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    onClick = onMiniPlayerClick,
                    isFavorite = playbackState.currentSong?.id?.let { it in favoriteSongIds } == true,
                    onToggleFavorite = { playbackState.currentSong?.let { onToggleFavorite(it) } },
                    progressProvider = progressProvider
                )
            }

            // 2. Navigation Bar: directly below Mini-Player
            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    windowInsets = WindowInsets.navigationBars.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                ) {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.SONGS,
                        onClick = { onTabSelected(MainTab.SONGS) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_queue_music),
                                contentDescription = "Songs",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Songs",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == MainTab.SONGS) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.FAVORITES,
                        onClick = { onTabSelected(MainTab.FAVORITES) },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    if (selectedTab == MainTab.FAVORITES) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                                ),
                                contentDescription = "Favorites",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == MainTab.FAVORITES) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.PLAYLISTS,
                        onClick = { onTabSelected(MainTab.PLAYLISTS) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_playlist_play),
                                contentDescription = "Playlists",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Playlists",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == MainTab.PLAYLISTS) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.ARTISTS,
                        onClick = { onTabSelected(MainTab.ARTISTS) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_person),
                                contentDescription = "Artists",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Artists",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == MainTab.ARTISTS) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.SETTINGS,
                        onClick = { onTabSelected(MainTab.SETTINGS) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            } else if (showMiniPlayer) {
                // On sub-screens (no bottom nav), lift Mini-Player above system navigation insets safely
                Spacer(
                    modifier = Modifier.windowInsetsBottomHeight(
                        WindowInsets.navigationBars
                    )
                )
            }
        }
    }
}
