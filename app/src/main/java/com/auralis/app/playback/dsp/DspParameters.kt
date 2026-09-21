package com.auralis.app.playback.dsp

/**
 * Thread-safe, immutable parameters for the Real-Time Lo-Fi DSP engine.
 *
 * All parameters are normalized to the `0.0f..1.0f` range where applicable and
 * rigorously validated against NaN and Infinity to guarantee numerical safety.
 *
 * @property enabled Master toggle for the Lo-Fi DSP effect.
 * @property intensity Master wet/dry intensity blend [0.0f..1.0f].
 * @property sampleRateReduction Decimation factor [0.0f..1.0f], downsampling from 1x (no decimation) up to 8x.
 * @property bitCrush Quantization depth reduction [0.0f..1.0f], scaling from 16-bit down to 8-bit resolution.
 * @property lowPassCutoff Analog tape warmth low-pass filter [0.0f..1.0f], rolling off from 20 kHz down to 2.5 kHz.
 * @property saturation Soft saturation harmonic warmth [0.0f..1.0f], adding analog tape-like soft clipping.
 */
data class LoFiParameters(
    val enabled: Boolean = false,
    val intensity: Float = 0.5f,
    val sampleRateReduction: Float = 0.0f,
    val bitCrush: Float = 0.0f,
    val lowPassCutoff: Float = 0.5f,
    val saturation: Float = 0.2f
) {
    init {
        require(!intensity.isNaN() && !intensity.isInfinite()) { "intensity must be finite" }
        require(!sampleRateReduction.isNaN() && !sampleRateReduction.isInfinite()) { "sampleRateReduction must be finite" }
        require(!bitCrush.isNaN() && !bitCrush.isInfinite()) { "bitCrush must be finite" }
        require(!lowPassCutoff.isNaN() && !lowPassCutoff.isInfinite()) { "lowPassCutoff must be finite" }
        require(!saturation.isNaN() && !saturation.isInfinite()) { "saturation must be finite" }
    }

    companion object {
        val DEFAULT = LoFiParameters()

        /**
         * Sanitizes raw or untrusted float inputs by clamping to [0.0f..1.0f] and
         * substituting safe defaults for NaN or infinite values.
         */
        fun sanitized(
            enabled: Boolean = false,
            intensity: Float = 0.5f,
            sampleRateReduction: Float = 0.0f,
            bitCrush: Float = 0.0f,
            lowPassCutoff: Float = 0.5f,
            saturation: Float = 0.2f
        ): LoFiParameters {
            return LoFiParameters(
                enabled = enabled,
                intensity = sanitize(intensity, 0.5f),
                sampleRateReduction = sanitize(sampleRateReduction, 0.0f),
                bitCrush = sanitize(bitCrush, 0.0f),
                lowPassCutoff = sanitize(lowPassCutoff, 0.5f),
                saturation = sanitize(saturation, 0.2f)
            )
        }

        private fun sanitize(value: Float, fallback: Float): Float {
            return if (value.isNaN() || value.isInfinite()) fallback else value.coerceIn(0.0f, 1.0f)
        }
    }
}

/**
 * Thread-safe, immutable parameters for the Real-Time Reverb DSP engine.
 *
 * Implements a Schroeder-Moorer reverberation model with parallel feedback comb
 * filters, series all-pass diffusers, and stereo decorrelation.
 *
 * @property enabled Master toggle for the Reverb DSP effect.
 * @property roomSize Virtual acoustic room volume [0.0f..1.0f], scaling comb filter delay times.
 * @property decay Feedback loop persistence [0.0f..1.0f], governing the reverberation decay time (T60).
 * @property damping High-frequency absorption [0.0f..1.0f], simulating acoustic wall/air damping.
 * @property wetLevel Reverberated signal level [0.0f..1.0f].
 * @property dryLevel Direct un-reverberated signal level [0.0f..1.0f].
 */
data class ReverbParameters(
    val enabled: Boolean = false,
    val roomSize: Float = 0.5f,
    val decay: Float = 0.5f,
    val damping: Float = 0.3f,
    val wetLevel: Float = 0.3f,
    val dryLevel: Float = 1.0f
) {
    init {
        require(!roomSize.isNaN() && !roomSize.isInfinite()) { "roomSize must be finite" }
        require(!decay.isNaN() && !decay.isInfinite()) { "decay must be finite" }
        require(!damping.isNaN() && !damping.isInfinite()) { "damping must be finite" }
        require(!wetLevel.isNaN() && !wetLevel.isInfinite()) { "wetLevel must be finite" }
        require(!dryLevel.isNaN() && !dryLevel.isInfinite()) { "dryLevel must be finite" }
    }

    companion object {
        val DEFAULT = ReverbParameters()

        /**
         * Sanitizes raw or untrusted float inputs by clamping to [0.0f..1.0f] and
         * substituting safe defaults for NaN or infinite values.
         */
        fun sanitized(
            enabled: Boolean = false,
            roomSize: Float = 0.5f,
            decay: Float = 0.5f,
            damping: Float = 0.3f,
            wetLevel: Float = 0.3f,
            dryLevel: Float = 1.0f
        ): ReverbParameters {
            return ReverbParameters(
                enabled = enabled,
                roomSize = sanitize(roomSize, 0.5f),
                decay = sanitize(decay, 0.5f),
                damping = sanitize(damping, 0.3f),
                wetLevel = sanitize(wetLevel, 0.3f),
                dryLevel = sanitize(dryLevel, 1.0f)
            )
        }

        private fun sanitize(value: Float, fallback: Float): Float {
            return if (value.isNaN() || value.isInfinite()) fallback else value.coerceIn(0.0f, 1.0f)
        }
    }
}

/**
 * Unified DSP parameter container combining Lo-Fi and Reverb configurations.
 */
data class DspParameters(
    val loFi: LoFiParameters = LoFiParameters.DEFAULT,
    val reverb: ReverbParameters = ReverbParameters.DEFAULT
) {
    val isAnyActive: Boolean get() = loFi.enabled || reverb.enabled

    companion object {
        val DEFAULT = DspParameters()
    }
}
