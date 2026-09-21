package com.auralis.app.playback.audiofx

import androidx.media3.common.Metadata
import androidx.media3.common.Player
import com.auralis.app.data.local.preferences.AudioPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages runtime loudness normalization during playback by observing track metadata
 * and adjusting ExoPlayer's volume multiplier without altering source audio files.
 */
@Singleton
class ReplayGainManager @Inject constructor(
    private val preferencesRepository: AudioPreferencesRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _replayGainState = MutableStateFlow(ReplayGainState())
    val replayGainState: StateFlow<ReplayGainState> = _replayGainState.asStateFlow()

    private var activePlayer: Player? = null

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _replayGainState.update { it.copy(mode = prefs.replayGainMode) }
                activePlayer?.let { applyGainToPlayer(it) }
            }
        }
    }

    /**
     * Binds or updates the active playback engine reference.
     */
    fun bindPlayer(player: Player) {
        activePlayer = player
        applyGainToPlayer(player)
    }

    /**
     * Called by playback service when new metadata is decoded for the playing media.
     */
    fun onMetadata(metadata: Metadata, player: Player) {
        activePlayer = player
        val extracted = ReplayGainParser.extractFromMetadata(metadata)
        if (extracted.hasMetadata) {
            _replayGainState.update { it.copy(currentMetadata = extracted) }
            applyGainToPlayer(player)
        }
    }

    /**
     * Called on track change to reset metadata and reapply defaults until new metadata arrives.
     */
    fun onMediaItemTransition(player: Player) {
        activePlayer = player
        _replayGainState.update { it.copy(currentMetadata = null) }
        applyGainToPlayer(player)
    }

    fun setMode(mode: ReplayGainMode) {
        _replayGainState.update { it.copy(mode = mode) }
        activePlayer?.let { applyGainToPlayer(it) }
        scope.launch { preferencesRepository.setReplayGainMode(mode) }
    }

    private fun applyGainToPlayer(player: Player) {
        val currentState = _replayGainState.value
        val multiplier = ReplayGainParser.calculateVolumeMultiplier(
            metadata = currentState.currentMetadata,
            mode = currentState.mode
        )
        player.volume = multiplier
        _replayGainState.update { it.copy(appliedVolumeMultiplier = multiplier) }
    }

    fun release() {
        activePlayer?.volume = 1.0f
        activePlayer = null
        _replayGainState.update {
            it.copy(
                currentMetadata = null,
                appliedVolumeMultiplier = 1.0f
            )
        }
    }
}
