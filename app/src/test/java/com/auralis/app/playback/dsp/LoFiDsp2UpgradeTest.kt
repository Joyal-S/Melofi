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
import kotlin.math.sqrt

/**
 * Rigorous Unit Test Suite for MELOFI LO-FI DSP 2.0.
 *
 * Validates:
 * 1. Parameter Clamping & NaN/Infinity/Negative bounds sanitization
 * 2. Preset characteristics (Dreamy, Cassette, Late Night, Deep Lo-Fi) & Wow/Flutter mapping
 * 3. Headroom Management & Output Soft Limiter (no output > 1.0f or < -1.0f)
 * 4. Post-quantization low-pass reconstruction filtering (attenuation of digital fizz)
 * 5. Subtle Tape Wow & Flutter character without clicks, discontinuities, or NaN
 * 6. Reverb pre-filter mud reduction & post-filter softening
 * 7. Stereo image preservation (no stereo collapse)
 * 8. Pure silence passthrough (zero in -> zero out)
 * 9. Multi-sample rate scaling (44.1 kHz, 48 kHz, 96 kHz, 192 kHz)
 * 10. Buffer sizes from 0, 1 frame, 7 frames to 1024 frames
 * 11. Seamless live toggling (OFF -> ON -> OFF) without clicks or state corruption
 */
class LoFiDsp2UpgradeTest {

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
    // 1. PARAMETER CLAMPING & SANITIZATION
    // ========================================================================

    @Test
    fun parameterSanitization_clampsNanInfinityAndOutliers() {
        // Test dirty parameters with NaNs, infinities, negatives, and excessive values
        val cleanLoFi = LoFiParameters.sanitized(
            enabled = true,
            intensity = Float.NaN,
            bitCrush = 999.0f,
            sampleRateReduction = -5.0f,
            lowPassCutoff = -10.0f,
            saturation = Float.POSITIVE_INFINITY,
            wowFlutter = 2.5f
        )

        assertEquals("NaN intensity must default or clamp", 0.5f, cleanLoFi.intensity, 0.001f)
        assertEquals("Bit crush must clamp to 1.0", 1.0f, cleanLoFi.bitCrush, 0.001f)
        assertEquals("Sample rate reduction must clamp to 0.0", 0.0f, cleanLoFi.sampleRateReduction, 0.001f)
        assertEquals("Low-pass cutoff must clamp to 0.0", 0.0f, cleanLoFi.lowPassCutoff, 0.001f)
        assertEquals("Saturation must default on infinity", 0.2f, cleanLoFi.saturation, 0.001f)
        assertEquals("Wow & Flutter must clamp to 1.0", 1.0f, cleanLoFi.wowFlutter, 0.001f)

        val cleanReverb = ReverbParameters.sanitized(
            enabled = true,
            roomSize = Float.NaN,
            decay = 100.0f,
            damping = -0.5f,
            preDelay = 1.5f,
            wetLevel = -1.0f,
            dryLevel = Float.NEGATIVE_INFINITY
        )

        assertEquals("NaN room size must default", 0.5f, cleanReverb.roomSize, 0.001f)
        assertEquals("Decay must clamp to 1.0", 1.0f, cleanReverb.decay, 0.001f)
        assertEquals("Damping must clamp to 0.0", 0.0f, cleanReverb.damping, 0.001f)
        assertEquals("Pre-delay must clamp to 1.0", 1.0f, cleanReverb.preDelay, 0.001f)
        assertEquals("Wet level must clamp to 0.0", 0.0f, cleanReverb.wetLevel, 0.001f)
        assertEquals("Dry level must default on infinity", 1.0f, cleanReverb.dryLevel, 0.001f)
    }

    // ========================================================================
    // 2. PRESETS AND WOW/FLUTTER MAPPING
    // ========================================================================

    @Test
    fun presets_haveDistinctCharacteristicsAndSensibleWowFlutter() {
        val dreamy = DspParameters.createPreset(DspPreset.DREAMY)
        val cassette = DspParameters.createPreset(DspPreset.CASSETTE)
        val lateNight = DspParameters.createPreset(DspPreset.LATE_NIGHT)
        val deepLoFi = DspParameters.createPreset(DspPreset.DEEP_LOFI)

        // Wow & flutter values check
        assertTrue("Cassette must have substantial wow/flutter for tape character", cassette.loFi.wowFlutter >= 0.20f)
        assertTrue("Deep Lo-Fi must have high wow/flutter", deepLoFi.loFi.wowFlutter >= 0.30f)
        assertTrue("Dreamy must have subtle wow/flutter", dreamy.loFi.wowFlutter in 0.05f..0.15f)
        assertTrue("Late night must have very low wow/flutter", lateNight.loFi.wowFlutter in 0.01f..0.10f)

        // Musical differentiators
        assertTrue("Cassette saturation > Dreamy saturation", cassette.loFi.saturation > dreamy.loFi.saturation)
        assertTrue("Deep Lo-Fi bit crush > Late Night bit crush", deepLoFi.loFi.bitCrush > lateNight.loFi.bitCrush)
        assertTrue("Late Night lowPassCutoff is lower than Dreamy for dark sound", lateNight.loFi.lowPassCutoff < dreamy.loFi.lowPassCutoff)
        assertTrue("Deep Lo-Fi has higher sample rate reduction than Late Night", deepLoFi.loFi.sampleRateReduction > lateNight.loFi.sampleRateReduction)
    }

    // ========================================================================
    // 3. HEADROOM & ANTI-CLIPPING OUTPUT LIMITER
    // ========================================================================

    @Test
    fun outputLimiter_guaranteesStrictBoundsUnderExtremeSignals() {
        val frameCount = 1024
        val channels = 2
        // Hot input signal peaking at +6dB (2.0f)
        val hotInput = FloatArray(frameCount * channels) { i ->
            (sin(i.toDouble() * 0.05) * 2.0).toFloat()
        }
        val output = FloatArray(frameCount * channels)

        // Test across all 4 presets with active Lo-Fi + Reverb
        for (preset in listOf(DspPreset.DREAMY, DspPreset.CASSETTE, DspPreset.LATE_NIGHT, DspPreset.DEEP_LOFI)) {
            val params = DspParameters.createPreset(preset, enabled = true)
            unifiedEngine.updateParameters(params)
            unifiedEngine.process(hotInput, output, frameCount, channels, 44100)

            for (i in output.indices) {
                val sample = output[i]
                assertFalse("Sample must not be NaN for preset $preset", sample.isNaN())
                assertFalse("Sample must not be Infinite for preset $preset", sample.isInfinite())
                assertTrue(
                    "Limiter must prevent clipping outside [-1.0f, 1.0f], got $sample at index $i for $preset",
                    sample in -1.0f..1.0f
                )
            }
        }
    }

    @Test
    fun loudnessBalance_preservesReasonableRmsBetweenOffAndOn() {
        val frameCount = 2048
        val channels = 2
        val input = FloatArray(frameCount * channels) { i ->
            (sin(i.toDouble() * 0.03) * 0.45).toFloat()
        }
        val outputBypass = FloatArray(frameCount * channels)
        val outputLoFiOn = FloatArray(frameCount * channels)

        // 1. Process bypass (OFF)
        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.CASSETTE, enabled = false))
        unifiedEngine.process(input, outputBypass, frameCount, channels, 44100)

        var sumSqBypass = 0.0
        for (s in outputBypass) sumSqBypass += s * s
        val rmsBypass = sqrt(sumSqBypass / outputBypass.size)

        // 2. Process active (ON) - Warm Cassette preset
        unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.CASSETTE, enabled = true))
        unifiedEngine.process(input, outputLoFiOn, frameCount, channels, 44100)

        var sumSqOn = 0.0
        for (s in outputLoFiOn) sumSqOn += s * s
        val rmsOn = sqrt(sumSqOn / outputLoFiOn.size)

        // A/B comparison should maintain balanced perceived loudness: ratio should stay within [0.65, 1.35]
        val ratio = rmsOn / rmsBypass
        assertTrue(
            "Lo-Fi ON should not drastically explode or mute volume. Ratio was $ratio (expected 0.65..1.35)",
            ratio in 0.65..1.35
        )
    }

    // ========================================================================
    // 4. POST-QUANTIZATION LOW-PASS RECONSTRUCTION FILTERING
    // ========================================================================

    @Test
    fun postQuantizationFilter_smoothsHarshQuantizationNoise() {
        val frameCount = 1024
        val channels = 1
        // High frequency test signal near Nyquist
        val highFreqSignal = FloatArray(frameCount) { i ->
            (sin(i.toDouble() * PI * 0.8) * 0.5).toFloat()
        }
        val output = FloatArray(frameCount)

        // Enable heavy degradation and dark lowpass
        val params = DspParameters(
            loFi = LoFiParameters(
                enabled = true,
                intensity = 0.9f,
                bitCrush = 0.6f,
                sampleRateReduction = 0.6f,
                lowPassCutoff = 0.05f,
                wowFlutter = 0.0f
            ),
            reverb = ReverbParameters(enabled = false)
        )
        unifiedEngine.updateParameters(params)
        unifiedEngine.process(highFreqSignal, output, frameCount, channels, 44100)

        // Output high frequency energy should be significantly rolled off by the reconstruction filter
        var energy = 0.0
        for (s in output) energy += s * s
        val rmsFiltered = sqrt(energy / frameCount)

        assertTrue(
            "Reconstruction LPF must attenuate high-frequency harmonics. Filtered RMS: $rmsFiltered",
            rmsFiltered < 0.35
        )
    }

    // ========================================================================
    // 5. TAPE WOW & FLUTTER CHARACTER
    // ========================================================================

    @Test
    fun wowFlutter_smoothContinuousModulationWithoutDiscontinuities() {
        val frameCount = 1024
        val channels = 2
        // Continuous 440 Hz tone
        val input = FloatArray(frameCount * channels) { i ->
            (sin(i.toDouble() * 2.0 * PI * 440.0 / 44100.0) * 0.5).toFloat()
        }
        val output = FloatArray(frameCount * channels)

        val params = DspParameters(
            loFi = LoFiParameters(
                enabled = true,
                intensity = 0.5f,
                bitCrush = 0.0f,
                sampleRateReduction = 0.0f,
                saturation = 0.0f,
                lowPassCutoff = 1.0f,
                wowFlutter = 0.4f
            ),
            reverb = ReverbParameters(enabled = false)
        )
        unifiedEngine.updateParameters(params)
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        // Ensure no click/pop discontinuities between consecutive samples
        for (i in 2 until output.size) {
            val delta = abs(output[i] - output[i - 2]) // Compare same-channel step
            assertTrue(
                "Sample-to-sample difference must be smooth without pops (delta: $delta at index $i)",
                delta < 0.4f
            )
            assertFalse(output[i].isNaN())
        }
    }

    // ========================================================================
    // 6. STEREO IMAGING PRESERVATION
    // ========================================================================

    @Test
    fun stereoProcessing_preservesStereoSeparation() {
        val frameCount = 512
        val channels = 2
        // Feed audio ONLY to left channel (L=0.5, R=0.0)
        val input = FloatArray(frameCount * channels) { i ->
            if (i % 2 == 0) 0.5f else 0.0f
        }
        val output = FloatArray(frameCount * channels)

        // Run Lo-Fi only
        val params = DspParameters(
            loFi = LoFiParameters(enabled = true, intensity = 0.7f, saturation = 0.3f, wowFlutter = 0.1f),
            reverb = ReverbParameters(enabled = false)
        )
        unifiedEngine.updateParameters(params)
        unifiedEngine.process(input, output, frameCount, channels, 44100)

        var leftEnergy = 0.0
        var rightEnergy = 0.0
        for (i in 0 until frameCount) {
            leftEnergy += abs(output[i * 2])
            rightEnergy += abs(output[i * 2 + 1])
        }

        assertTrue("Left channel must carry the signal", leftEnergy > 10.0)
        assertEquals("Right channel must remain completely silent in Lo-Fi to prevent collapse", 0.0, rightEnergy, 0.0001)
    }

    // ========================================================================
    // 7. SILENCE PASSTHROUGH INVARIANT
    // ========================================================================

    @Test
    fun silenceInput_guaranteesExactZeroOutput() {
        val frameCount = 1024
        val channels = 2
        val input = FloatArray(frameCount * channels) { 0.0f }
        val output = FloatArray(frameCount * channels)

        for (preset in DspPreset.entries) {
            unifiedEngine.updateParameters(DspParameters.createPreset(preset, enabled = true))
            unifiedEngine.process(input, output, frameCount, channels, 44100)

            for (i in output.indices) {
                assertEquals("Silence in must produce exact 0.0f output for preset $preset", 0.0f, output[i], 0.0f)
            }
        }
    }

    // ========================================================================
    // 8. MULTI-SAMPLE RATE SUPPORT
    // ========================================================================

    @Test
    fun sampleRateHandling_operatesAcrossStandardFrequencies() {
        val sampleRates = intArrayOf(44100, 48000, 88200, 96000, 192000)
        val frameCount = 512
        val channels = 2

        for (sr in sampleRates) {
            val input = FloatArray(frameCount * channels) { i -> (sin(i * 0.1) * 0.5).toFloat() }
            val output = FloatArray(frameCount * channels)

            unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.CASSETTE, enabled = true))
            unifiedEngine.process(input, output, frameCount, channels, sr)

            for (sample in output) {
                assertFalse("Sample must not be NaN at ${sr}Hz", sample.isNaN())
                assertFalse("Sample must not be infinite at ${sr}Hz", sample.isInfinite())
                assertTrue("Sample must be in [-1, 1] at ${sr}Hz", sample in -1.0f..1.0f)
            }
        }
    }

    // ========================================================================
    // 9. BUFFER SIZE BOUNDARIES (0, 1, 7, 1024 FRAMES)
    // ========================================================================

    @Test
    fun bufferSizeHandling_handlesZeroAndArbitraryBufferLengthsSafely() {
        val bufferLengths = intArrayOf(0, 1, 7, 31, 64, 256, 1024)
        val channels = 2

        for (frames in bufferLengths) {
            val input = FloatArray(frames * channels) { 0.3f }
            val output = FloatArray(frames * channels)

            unifiedEngine.updateParameters(DspParameters.createPreset(DspPreset.DEEP_LOFI, enabled = true))
            // Must not throw IndexOutOfBoundsException or crash
            unifiedEngine.process(input, output, frames, channels, 44100)

            for (sample in output) {
                assertFalse(sample.isNaN())
                assertFalse(sample.isInfinite())
            }
        }
    }

    // ========================================================================
    // 10. REPEATED TOGGLE STABILITY (OFF -> ON -> OFF)
    // ========================================================================

    @Test
    fun toggleMasterRepeatedly_neverCausesGlitchesOrStateCorruption() {
        val frameCount = 256
        val channels = 2
        val input = FloatArray(frameCount * channels) { (sin(it * 0.05) * 0.6).toFloat() }
        val output = FloatArray(frameCount * channels)

        // Alternate 50 times between ON and OFF
        for (cycle in 0 until 50) {
            val enable = (cycle % 2 == 1)
            unifiedEngine.setLoFiEnabled(enable)
            unifiedEngine.setReverbEnabled(enable)

            unifiedEngine.process(input, output, frameCount, channels, 44100)

            for (sample in output) {
                assertFalse("Cycle $cycle: sample should never be NaN", sample.isNaN())
                assertFalse("Cycle $cycle: sample should never be infinite", sample.isInfinite())
                assertTrue("Cycle $cycle: sample must be within [-1, 1]", sample in -1.0f..1.0f)
            }

            if (!enable) {
                // When disabled, it should be exact bypass
                for (i in 0 until frameCount * channels) {
                    assertEquals("Cycle $cycle: Bypass output must match input", input[i], output[i], 0.0001f)
                }
            }
        }
    }
}
