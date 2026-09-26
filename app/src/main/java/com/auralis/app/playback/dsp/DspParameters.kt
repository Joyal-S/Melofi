package com.auralis.app.playback.dsp

/**
 * Built-in presets for the Real-Time Lo-Fi + Reverb DSP engine.
 *
 * Each preset represents a distinctive sonic aesthetic by configuring
 * both the Lo-Fi character and the Reverb acoustic environment.
 */
enum class DspPreset(val displayName: String) {
    /**
     * Dreamy: Moderate Lo-Fi, lush atmospheric reverb, gentle high-frequency softening.
     */
    DREAMY("Dreamy"),

    /**
     * Cassette: Prominent tape character, warm analog low-pass filtering, subtle magnetic saturation, restrained room.
     */
    CASSETTE("Cassette"),

    /**
     * Late Night: Dark, intimate low-pass cutoff, subtle bit-depth texture, restrained nocturnal room reverb.
     */
    LATE_NIGHT("Late Night"),

    /**
     * Deep Lo-Fi: Distinctive vintage sampler crunch, heavy tape saturation, deep spacious reverberant tail.
     */
    DEEP_LOFI("Deep Lo-Fi"),

    /**
     * Custom: User-tailored parameters outside predefined presets.
     */
    CUSTOM("Custom");

    companion object {
        val DEFAULT = DREAMY

        fun fromString(name: String?): DspPreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }
}

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
 * @property wowFlutter Subtle tape speed irregularity / pitch modulation [0.0f..1.0f].
 */
data class LoFiParameters(
    val enabled: Boolean = false,
    val intensity: Float = 0.5f,
    val sampleRateReduction: Float = 0.0f,
    val bitCrush: Float = 0.0f,
    val lowPassCutoff: Float = 0.5f,
    val saturation: Float = 0.2f,
    val wowFlutter: Float = 0.0f
) {
    init {
        require(!intensity.isNaN() && !intensity.isInfinite()) { "intensity must be finite" }
        require(!sampleRateReduction.isNaN() && !sampleRateReduction.isInfinite()) { "sampleRateReduction must be finite" }
        require(!bitCrush.isNaN() && !bitCrush.isInfinite()) { "bitCrush must be finite" }
        require(!lowPassCutoff.isNaN() && !lowPassCutoff.isInfinite()) { "lowPassCutoff must be finite" }
        require(!saturation.isNaN() && !saturation.isInfinite()) { "saturation must be finite" }
        require(!wowFlutter.isNaN() && !wowFlutter.isInfinite()) { "wowFlutter must be finite" }
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
            saturation: Float = 0.2f,
            wowFlutter: Float = 0.0f
        ): LoFiParameters {
            return LoFiParameters(
                enabled = enabled,
                intensity = sanitize(intensity, 0.5f),
                sampleRateReduction = sanitize(sampleRateReduction, 0.0f),
                bitCrush = sanitize(bitCrush, 0.0f),
                lowPassCutoff = sanitize(lowPassCutoff, 0.5f),
                saturation = sanitize(saturation, 0.2f),
                wowFlutter = sanitize(wowFlutter, 0.0f)
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
 * filters, series all-pass diffusers, pre-delay buffer, and stereo decorrelation.
 *
 * @property enabled Master toggle for the Reverb DSP effect.
 * @property roomSize Virtual acoustic room volume [0.0f..1.0f], scaling comb filter delay times.
 * @property decay Feedback loop persistence [0.0f..1.0f], governing the reverberation decay time (T60).
 * @property damping High-frequency absorption [0.0f..1.0f], simulating acoustic wall/air damping.
 * @property preDelay Initial delay before reverb reflections begin [0.0f..1.0f] (0 ms to 100 ms).
 * @property wetLevel Reverberated signal level [0.0f..1.0f].
 * @property dryLevel Direct un-reverberated signal level [0.0f..1.0f].
 */
data class ReverbParameters(
    val enabled: Boolean = false,
    val roomSize: Float = 0.5f,
    val decay: Float = 0.5f,
    val damping: Float = 0.3f,
    val preDelay: Float = 0.0f,
    val wetLevel: Float = 0.3f,
    val dryLevel: Float = 1.0f
) {
    init {
        require(!roomSize.isNaN() && !roomSize.isInfinite()) { "roomSize must be finite" }
        require(!decay.isNaN() && !decay.isInfinite()) { "decay must be finite" }
        require(!damping.isNaN() && !damping.isInfinite()) { "damping must be finite" }
        require(!preDelay.isNaN() && !preDelay.isInfinite()) { "preDelay must be finite" }
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
            preDelay: Float = 0.0f,
            wetLevel: Float = 0.3f,
            dryLevel: Float = 1.0f
        ): ReverbParameters {
            return ReverbParameters(
                enabled = enabled,
                roomSize = sanitize(roomSize, 0.5f),
                decay = sanitize(decay, 0.5f),
                damping = sanitize(damping, 0.3f),
                preDelay = sanitize(preDelay, 0.0f),
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
 * Unified DSP parameter container combining Lo-Fi, Reverb, and Preset configurations.
 */
data class DspParameters(
    val preset: DspPreset = DspPreset.DEFAULT,
    val loFi: LoFiParameters = LoFiParameters.DEFAULT,
    val reverb: ReverbParameters = ReverbParameters.DEFAULT
) {
    val isAnyActive: Boolean get() = loFi.enabled || reverb.enabled
    val isLoFiOnly: Boolean get() = loFi.enabled && !reverb.enabled
    val isReverbOnly: Boolean get() = !loFi.enabled && reverb.enabled
    val isBothActive: Boolean get() = loFi.enabled && reverb.enabled

    companion object {
        val DEFAULT = DspParameters()

        /**
         * Creates a [DspParameters] instance configured with the specified [preset].
         * The [enabled] flag specifies whether both effects are enabled or disabled by default.
         */
        fun createPreset(preset: DspPreset, enabled: Boolean = false): DspParameters {
            return when (preset) {
                DspPreset.DREAMY -> DspParameters(
                    preset = DspPreset.DREAMY,
                    loFi = LoFiParameters(
                        enabled = enabled,
                        intensity = 0.45f,
                        sampleRateReduction = 0.15f,
                        bitCrush = 0.10f,
                        lowPassCutoff = 0.65f,
                        saturation = 0.20f,
                        wowFlutter = 0.08f
                    ),
                    reverb = ReverbParameters(
                        enabled = enabled,
                        roomSize = 0.60f,
                        decay = 0.65f,
                        damping = 0.25f,
                        preDelay = 0.20f,
                        wetLevel = 0.35f,
                        dryLevel = 0.95f
                    )
                )
                DspPreset.CASSETTE -> DspParameters(
                    preset = DspPreset.CASSETTE,
                    loFi = LoFiParameters(
                        enabled = enabled,
                        intensity = 0.70f,
                        sampleRateReduction = 0.30f,
                        bitCrush = 0.25f,
                        lowPassCutoff = 0.40f,
                        saturation = 0.45f,
                        wowFlutter = 0.25f
                    ),
                    reverb = ReverbParameters(
                        enabled = enabled,
                        roomSize = 0.35f,
                        decay = 0.40f,
                        damping = 0.50f,
                        preDelay = 0.05f,
                        wetLevel = 0.20f,
                        dryLevel = 1.0f
                    )
                )
                DspPreset.LATE_NIGHT -> DspParameters(
                    preset = DspPreset.LATE_NIGHT,
                    loFi = LoFiParameters(
                        enabled = enabled,
                        intensity = 0.50f,
                        sampleRateReduction = 0.10f,
                        bitCrush = 0.15f,
                        lowPassCutoff = 0.30f,
                        saturation = 0.25f,
                        wowFlutter = 0.06f
                    ),
                    reverb = ReverbParameters(
                        enabled = enabled,
                        roomSize = 0.50f,
                        decay = 0.50f,
                        damping = 0.40f,
                        preDelay = 0.15f,
                        wetLevel = 0.25f,
                        dryLevel = 0.95f
                    )
                )
                DspPreset.DEEP_LOFI -> DspParameters(
                    preset = DspPreset.DEEP_LOFI,
                    loFi = LoFiParameters(
                        enabled = enabled,
                        intensity = 0.85f,
                        sampleRateReduction = 0.50f,
                        bitCrush = 0.50f,
                        lowPassCutoff = 0.25f,
                        saturation = 0.55f,
                        wowFlutter = 0.35f
                    ),
                    reverb = ReverbParameters(
                        enabled = enabled,
                        roomSize = 0.75f,
                        decay = 0.80f,
                        damping = 0.20f,
                        preDelay = 0.30f,
                        wetLevel = 0.45f,
                        dryLevel = 0.90f
                    )
                )
                DspPreset.CUSTOM -> DspParameters(
                    preset = DspPreset.CUSTOM,
                    loFi = LoFiParameters.DEFAULT.copy(enabled = enabled),
                    reverb = ReverbParameters.DEFAULT.copy(enabled = enabled)
                )
            }
        }
    }
}
