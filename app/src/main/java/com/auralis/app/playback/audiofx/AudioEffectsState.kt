package com.auralis.app.playback.audiofx

/**
 * Composite state of all audio effects bound to the playback engine.
 */
data class AudioEffectsState(
    val equalizer: EqualizerState = EqualizerState(),
    val bassBoost: BassBoostState = BassBoostState(),
    val virtualizer: VirtualizerState = VirtualizerState(),
    val activeAudioSessionId: Int = 0
)
