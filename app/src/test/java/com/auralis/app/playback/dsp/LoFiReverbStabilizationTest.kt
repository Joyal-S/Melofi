package com.auralis.app.playback.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Audio Addon 4 — Comprehensive Testing, Tuning & Stabilization Suite.
 *
 * Provides rigorous verification for:
 * - Numerical stability under extreme inputs (silence, impulses, sine, DC, denormals, clipping)
 * - Schroeder reverberator BIBO stability under maximum feedback and long-running stress
 * - Lo-Fi quality progression from intensity 0 to 1 with musical saturation
 * - Preset parameter boundaries and invariant safety
 * - Bit-for-bit bypass identity
 * - All 4 operational states (Both OFF, Lo-Fi only, Reverb only, Both ON)
 * - Live parameter smoothing without audio discontinuities
 * - Seek and track-change state flushes (zero residual echo)
 * - Multi-sample-rate scaling (44.1 kHz, 48 kHz, 88.2 kHz, 96 kHz, 192 kHz)
 * - Mono and stereo channel compatibility
 * - ReplayGain coexistence headroom validation
 */
class LoFiReverbStabilizationTest {

    private lateinit var loFiEngine: LoFiDspEngine
    private lateinit var reverbEngine: ReverbDspEngine
    private lateinit var unifiedEngine: MelofyDspEngine

    @Before
    fun setUp() {
        loFiEngine = LoFiDspEngine()
        reverbEngine = ReverbDspEngine()
        unifiedEngine = MelofyDspEngine(loFiEngine, reverbEngine)
    }

    // ========================================================================
    // 1. NUMERICAL STABILITY STRESS TESTS (PART 3)
    // ========================================================================

    @Test
    fun numericalStability_pureSilence_producesExactZeroes() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.0f }
        val output = FloatArray(frameCount * channels)

        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.DEEP_LOFI, enabled = true))
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        for (sample in output) {
            assertEquals("Silence input must yield silence output", 0.0f, sample, 0.0f)
        }
    }

    @Test
    fun numericalStability_denormalInputs_suppressedWithoutNanOrInfinity() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { 1e-25f } // Subnormal/denormal float
        val output = FloatArray(frameCount * channels)

        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.DREAMY, enabled = true))
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        for (sample in output) {
            assertFalse("Sample must not be NaN", sample.isNaN())
            assertFalse("Sample must not be Infinite", sample.isInfinite())
            assertTrue("Sample must be bounded", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun numericalStability_extremeClippingInputs_softSaturatedSafely() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { i -> if (i % 2 == 0) 5.0f else -5.0f }
        val output = FloatArray(frameCount * channels)

        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.CASSETTE, enabled = true))
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        for (sample in output) {
            assertFalse("Sample must not be NaN", sample.isNaN())
            assertFalse("Sample must not be Infinite", sample.isInfinite())
            assertTrue("Sample must be clamped in [-1.0f..1.0f]", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun numericalStability_constantDcSignal_stabilizesWithoutRunaway() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.75f }
        val output = FloatArray(frameCount * channels)

        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.LATE_NIGHT, enabled = true))
        
        // Process multiple consecutive buffers
        for (b in 0 until 10) {
            unifiedEngine.process(input, output, frameCount, channels, 44100)
            for (sample in output) {
                assertFalse("Sample must not be NaN at buffer $b", sample.isNaN())
                assertTrue("DC output must remain bounded at buffer $b", sample in -1.0f..1.0f)
            }
        }
    }

    // ========================================================================
    // 2. REVERB BIBO STABILITY UNDER EXTREME SETTINGS (PART 4)
    // ========================================================================

    @Test
    fun reverbStability_maximumDecayAndMinimumDamping_remainsStableOverLongDuration() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.0f }
        input[0] = 1.0f
        input[1] = 1.0f // Initial impulse
        val output = FloatArray(frameCount * channels)

        val extremeReverb = ReverbParameters(
            enabled = true,
            roomSize = 1.0f, // Max room size
            decay = 1.0f,    // Max decay (clamped strictly to feedback < 0.975)
            damping = 0.0f,  // Zero damping (highest feedback persistence)
            wetLevel = 1.0f,
            dryLevel = 0.0f
        )

        // Block 0 with impulse
        reverbEngine.process(input, output, frameCount, channels, 44100, extremeReverb)

        // Run 100 consecutive blocks of silence (51,200 samples = ~1.16 seconds of decay)
        input[0] = 0.0f
        input[1] = 0.0f
        var maxObservedAmplitude = 0.0f

        for (block in 1..100) {
            reverbEngine.process(input, output, frameCount, channels, 44100, extremeReverb)
            for (sample in output) {
                val absVal = abs(sample)
                if (absVal > maxObservedAmplitude) maxObservedAmplitude = absVal
                assertFalse("Sample must not be NaN in block $block", sample.isNaN())
                assertFalse("Sample must not be Infinite in block $block", sample.isInfinite())
                assertTrue("Sample $sample must not exceed 1.0 in block $block", absVal <= 1.0f)
            }
        }

        assertTrue("Reverb must sustain reflections under max decay", maxObservedAmplitude > 0.0f)
    }

    @Test
    fun reverbStability_maximumPreDelay_indexesSafely() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.5f }
        val output = FloatArray(frameCount * channels)

        val params = ReverbParameters(
            enabled = true,
            preDelay = 1.0f, // Max 100ms
            roomSize = 0.5f,
            decay = 0.5f,
            wetLevel = 0.5f
        )

        // Process at maximum sample rate (192 kHz) to test pre-delay buffer boundaries
        for (i in 0 until 20) {
            reverbEngine.process(input, output, frameCount, channels, 192000, params)
            for (sample in output) {
                assertFalse(sample.isNaN())
                assertTrue(sample in -1.0f..1.0f)
            }
        }
    }

    @Test
    fun reverbStability_denseImpulseTrain_doesNotExplode() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels)
        val output = FloatArray(frameCount * channels)

        val params = ReverbParameters(
            enabled = true,
            roomSize = 0.9f,
            decay = 0.95f,
            damping = 0.1f,
            wetLevel = 0.8f,
            dryLevel = 0.5f
        )

        // 50 blocks of rapid impulses every 64 samples
        for (block in 0 until 50) {
            for (i in input.indices) {
                input[i] = if (i % 64 == 0) 0.7f else 0.0f
            }
            reverbEngine.process(input, output, frameCount, channels, 44100, params)

            for (sample in output) {
                assertFalse(sample.isNaN())
                assertFalse(sample.isInfinite())
                assertTrue(sample in -1.0f..1.0f)
            }
        }
    }

    // ========================================================================
    // 3. LO-FI QUALITY & PROGRESSION TESTS (PART 5)
    // ========================================================================

    @Test
    fun loFiQuality_intensityZero_isExactBypass() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { i -> (sin(i.toDouble() * 0.1) * 0.8).toFloat() }
        val output = FloatArray(frameCount * channels)

        val params = LoFiParameters(
            enabled = true,
            intensity = 0.0f, // Zero intensity
            bitCrush = 0.8f,
            sampleRateReduction = 0.8f,
            saturation = 0.8f
        )

        loFiEngine.process(input, output, frameCount, channels, 44100, params)

        for (i in input.indices) {
            assertEquals("Intensity 0 must produce identical output", input[i], output[i], 0.0f)
        }
    }

    @Test
    fun loFiQuality_intensityProgression_scalesEffectively() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { i -> (sin(i.toDouble() * 0.05) * 0.7).toFloat() }

        val outLow = FloatArray(frameCount * channels)
        val outMed = FloatArray(frameCount * channels)
        val outHigh = FloatArray(frameCount * channels)

        val engineLow = LoFiDspEngine()
        val engineMed = LoFiDspEngine()
        val engineHigh = LoFiDspEngine()

        val baseConfig = LoFiParameters(
            enabled = true,
            bitCrush = 0.5f,
            sampleRateReduction = 0.4f,
            saturation = 0.4f
        )

        engineLow.process(input, outLow, frameCount, channels, 44100, baseConfig.copy(intensity = 0.2f))
        engineMed.process(input, outMed, frameCount, channels, 44100, baseConfig.copy(intensity = 0.6f))
        engineHigh.process(input, outHigh, frameCount, channels, 44100, baseConfig.copy(intensity = 1.0f))

        var diffLow = 0.0
        var diffMed = 0.0
        var diffHigh = 0.0

        for (i in input.indices) {
            diffLow += abs(outLow[i] - input[i])
            diffMed += abs(outMed[i] - input[i])
            diffHigh += abs(outHigh[i] - input[i])
        }

        assertTrue("Medium intensity must deviate more than low intensity", diffMed > diffLow)
        assertTrue("High intensity must deviate more than medium intensity", diffHigh > diffMed)
    }

    @Test
    fun loFiQuality_cubicSaturation_compressesMusicalKnee() {
        val frameCount = 256
        val channels = 1
        val input = FloatArray(frameCount) { 0.5f }
        val output = FloatArray(frameCount)

        val params = LoFiParameters(
            enabled = true,
            intensity = 1.0f,
            saturation = 0.8f,
            bitCrush = 0.0f,
            sampleRateReduction = 0.0f,
            lowPassCutoff = 1.0f // Fully open LPF
        )

        loFiEngine.process(input, output, frameCount, channels, 44100, params)

        val saturated = output[frameCount - 1]
        // Tape saturation adds harmonic drive warmth to signals while strictly bounding peaks at 1.0f
        assertTrue("Saturated signal must have harmonic drive warmth (was $saturated)", saturated > 0.5f)
        assertTrue("Saturated signal must remain strictly bounded <= 1.0f", saturated <= 1.0f)

        // Verify overdriven peak is soft-limited to 1.0f without runaway
        val overdrivenInput = FloatArray(frameCount) { 1.5f }
        val overdrivenOutput = FloatArray(frameCount)
        loFiEngine.process(overdrivenInput, overdrivenOutput, frameCount, channels, 44100, params)
        assertEquals("Overdriven peak must be soft-limited to 1.0f", 1.0f, overdrivenOutput[frameCount - 1], 0.001f)
    }

    // ========================================================================
    // 4. PRESET INVARIANT VALIDATION (PART 6)
    // ========================================================================

    @Test
    fun presets_allFourPresets_haveSafeInvariants() {
        val presets = listOf(DspPreset.DREAMY, DspPreset.CASSETTE, DspPreset.LATE_NIGHT, DspPreset.DEEP_LOFI)

        for (preset in presets) {
            val params = DspParameters.createPreset(preset, enabled = true)

            // General bounds
            assertTrue("${preset.displayName} Lo-Fi intensity in [0, 1]", params.loFi.intensity in 0.0f..1.0f)
            assertTrue("${preset.displayName} Bit crush in [0, 1]", params.loFi.bitCrush in 0.0f..1.0f)
            assertTrue("${preset.displayName} Decimation in [0, 1]", params.loFi.sampleRateReduction in 0.0f..1.0f)
            assertTrue("${preset.displayName} Cutoff in [0, 1]", params.loFi.lowPassCutoff in 0.0f..1.0f)
            assertTrue("${preset.displayName} Saturation in [0, 1]", params.loFi.saturation in 0.0f..1.0f)

            assertTrue("${preset.displayName} Room size in [0, 1]", params.reverb.roomSize in 0.0f..1.0f)
            assertTrue("${preset.displayName} Decay in [0, 1]", params.reverb.decay in 0.0f..1.0f)
            assertTrue("${preset.displayName} Damping in [0, 1]", params.reverb.damping in 0.0f..1.0f)
            assertTrue("${preset.displayName} Pre-delay in [0, 1]", params.reverb.preDelay in 0.0f..1.0f)
            assertTrue("${preset.displayName} Wet level in [0, 1]", params.reverb.wetLevel in 0.0f..1.0f)
            assertTrue("${preset.displayName} Dry level in [0, 1]", params.reverb.dryLevel in 0.0f..1.0f)
        }
    }

    // ========================================================================
    // 5. BIT-FOR-BIT BYPASS VALIDATION (PART 7)
    // ========================================================================

    @Test
    fun bypassValidation_loFiOffReverbOff_guaranteesBitIdentityOverTenThousandSamples() {
        val totalSamples = 10000
        val input = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.03) * 0.85).toFloat() }
        val output = FloatArray(totalSamples)

        unifiedEngine.updateParameters(DspParameters.DEFAULT)
        assertFalse(unifiedEngine.isAnyActive)

        unifiedEngine.process(input, output, totalSamples / 2, 2, 44100)

        for (i in 0 until totalSamples) {
            assertEquals("Bypass must preserve 100% bit-identity at index $i", input[i], output[i], 0.0f)
        }
    }

    // ========================================================================
    // 6. ALL 4 OPERATIONAL MODES INDEPENDENCE (PART 8)
    // ========================================================================

    @Test
    fun operationalModes_stateTransitions_operateIndependently() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.6f }
        val output = FloatArray(frameCount * channels)

        // State 1: Both OFF
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = false), reverb = ReverbParameters(enabled = false)))
        assertFalse(unifiedEngine.currentParameters.isAnyActive)
        unifiedEngine.process(input, output, frameCount, channels, 44100)
        assertEquals(input[0], output[0], 0.0f)

        // State 2: Lo-Fi ON, Reverb OFF
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.8f, bitCrush = 0.5f), reverb = ReverbParameters(enabled = false)))
        assertTrue(unifiedEngine.currentParameters.isLoFiOnly)
        unifiedEngine.process(input, output, frameCount, channels, 44100)
        assertNotEquals(input[0], output[0], 0.0001f)

        // State 3: Lo-Fi OFF, Reverb ON
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = false), reverb = ReverbParameters(enabled = true, wetLevel = 0.5f)))
        assertTrue(unifiedEngine.currentParameters.isReverbOnly)
        unifiedEngine.process(input, output, frameCount, channels, 44100)
        assertTrue(output[0] in -1.0f..1.0f)

        // State 4: Both ON
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.7f), reverb = ReverbParameters(enabled = true, wetLevel = 0.4f)))
        assertTrue(unifiedEngine.currentParameters.isBothActive)
        unifiedEngine.process(input, output, frameCount, channels, 44100)
        assertTrue(output[0] in -1.0f..1.0f)
    }

    // ========================================================================
    // 7. LIVE PARAMETER CHANGES & DISCONTINUITY (PART 9)
    // ========================================================================

    @Test
    fun liveParameterChanges_continuousPlayback_producesSmoothContinuity() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.5f }
        val out1 = FloatArray(frameCount * channels)
        val out2 = FloatArray(frameCount * channels)

        // Block 1: Low intensity
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.1f)))
        unifiedEngine.process(input, out1, frameCount, channels, 44100)

        // Block 2: Live change to high intensity
        unifiedEngine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.9f)))
        unifiedEngine.process(input, out2, frameCount, channels, 44100)

        // Difference between last sample of Block 1 and first sample of Block 2 must be smooth (< 0.12)
        val jump = abs(out2[0] - out1[out1.size - 2])
        assertTrue("Jump between blocks ($jump) must be smoothed to prevent audio pop", jump < 0.12f)
    }

    // ========================================================================
    // 8. SEEK & TRACK TRANSITIONS STATE FLUSH (PART 10 - 12)
    // ========================================================================

    @Test
    fun seekAndTrackTransitions_reset_eliminatesStaleReverbTailsImmediately() {
        val frameCount = 512
        val channels = 2
        val input = FloatArray(frameCount * channels) { 1.0f }
        val output = FloatArray(frameCount * channels)

        // 1. Play with rich reverb
        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.DREAMY, enabled = true))
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        // 2. User seeks or track transitions -> reset()
        unifiedEngine.reset()

        // 3. Immediately feed silence -> output must be pure silence
        val silence = FloatArray(frameCount * channels) { 0.0f }
        val postResetOutput = FloatArray(frameCount * channels)
        unifiedEngine.process(silence, postResetOutput, frameCount, channels, 44100)

        for (sample in postResetOutput) {
            assertEquals("Post-seek silence must be zero", 0.0f, sample, 0.00001f)
        }
    }

    // ========================================================================
    // 9. MULTI-SAMPLE RATE & CHANNELS (PART 14 - 15)
    // ========================================================================

    @Test
    fun multiSampleRate_allStandardRates_processWithoutExceptions() {
        val sampleRates = intArrayOf(44100, 48000, 88200, 96000, 192000)
        val frameCount = 256
        val channels = 2
        val input = FloatArray(frameCount * channels) { i -> (sin(i.toDouble() * 0.1) * 0.7).toFloat() }
        val output = FloatArray(frameCount * channels)

        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.DEEP_LOFI, enabled = true))

        for (rate in sampleRates) {
            unifiedEngine.process(input, output, frameCount, channels, rate)
            for (sample in output) {
                assertFalse("Sample rate $rate produced NaN", sample.isNaN())
                assertFalse("Sample rate $rate produced Infinite", sample.isInfinite())
                assertTrue("Sample rate $rate produced clipping: $sample", sample in -1.0f..1.0f)
            }
        }
    }

    @Test
    fun channelConfigurations_monoAndStereo_processCorrectly() {
        val frameCount = 512
        val params = DspParameters.createPreset(DspPreset.CASSETTE, enabled = true)
        unifiedEngine.updateParameters(params)

        // Mono
        val monoInput = FloatArray(frameCount) { 0.5f }
        val monoOutput = FloatArray(frameCount)
        unifiedEngine.process(monoInput, monoOutput, frameCount, 1, 48000)
        for (sample in monoOutput) {
            assertFalse(sample.isNaN())
            assertTrue(sample in -1.0f..1.0f)
        }

        // Stereo
        val stereoInput = FloatArray(frameCount * 2) { 0.5f }
        val stereoOutput = FloatArray(frameCount * 2)
        unifiedEngine.process(stereoInput, stereoOutput, frameCount, 2, 48000)
        for (sample in stereoOutput) {
            assertFalse(sample.isNaN())
            assertTrue(sample in -1.0f..1.0f)
        }
    }

    // ========================================================================
    // 10. REPLAYGAIN COMPATIBILITY (PART 24)
    // ========================================================================

    @Test
    fun replayGainCoexistence_scaledVolumes_remainBounded() {
        val frameCount = 512
        val channels = 2
        val sampleRate = 44100

        // Simulate ReplayGain track mode volume scaling: 0.5x, 1.0x, 1.4x (loud track boost)
        val multipliers = floatArrayOf(0.5f, 1.0f, 1.4f)
        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.CASSETTE, enabled = true))

        for (multiplier in multipliers) {
            val input = FloatArray(frameCount * channels) { i ->
                val base = (sin(i.toDouble() * 0.05) * 0.7).toFloat()
                (base * multiplier).coerceIn(-1.0f, 1.0f)
            }
            val output = FloatArray(frameCount * channels)

            unifiedEngine.process(input, output, frameCount, channels, sampleRate)

            for (sample in output) {
                assertFalse(sample.isNaN())
                assertFalse(sample.isInfinite())
                assertTrue("ReplayGain scaled sample must remain clamped in [-1.0, 1.0]", sample in -1.0f..1.0f)
            }
        }
    }
}
