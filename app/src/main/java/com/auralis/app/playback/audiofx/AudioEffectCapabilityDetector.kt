package com.auralis.app.playback.audiofx

import android.media.audiofx.AudioEffect
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects native Android audio effect capabilities by safely querying the system audio effect engine.
 *
 * Catches all runtime and security exceptions to guarantee zero crash risk on custom ROMs
 * or OEM firmware with non-compliant HAL implementations.
 */
@Singleton
class AudioEffectCapabilityDetector @Inject constructor() {

    @Volatile
    private var cachedCapabilities: AudioEffectCapability? = null

    fun detectCapabilities(): AudioEffectCapability {
        cachedCapabilities?.let { return it }
        val detected = try {
            val descriptors = AudioEffect.queryEffects() ?: emptyArray()
            var eqSupported = false
            var bbSupported = false
            var virtSupported = false

            for (desc in descriptors) {
                when (desc.type) {
                    AudioEffect.EFFECT_TYPE_EQUALIZER -> eqSupported = true
                    AudioEffect.EFFECT_TYPE_BASS_BOOST -> bbSupported = true
                    AudioEffect.EFFECT_TYPE_VIRTUALIZER -> virtSupported = true
                }
            }

            AudioEffectCapability(
                isEqualizerSupported = eqSupported,
                isBassBoostSupported = bbSupported,
                isVirtualizerSupported = virtSupported
            )
        } catch (e: Throwable) {
            runCatching { Log.e(TAG, "Failed to query system audio effects: ${e.message}", e) }
            AudioEffectCapability(
                isEqualizerSupported = false,
                isBassBoostSupported = false,
                isVirtualizerSupported = false
            )
        }
        cachedCapabilities = detected
        return detected
    }

    companion object {
        private const val TAG = "AudioEffectDetector"
    }
}
