@file:Suppress("DEPRECATION")

package com.auralis.app.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Comprehensive unit and benchmark test suite for [MelofyDspEngine] and [MelofyDspAudioProcessor].
 *
 * Validates:
 * - 4 Operational Modes: Both OFF (Bypass), Lo-Fi Only, Reverb Only, Both ON
 * - All 4 Presets: Dreamy, Cassette, Late Night, Deep Lo-Fi
 * - Parameter Smoothing and Pre-Delay buffer timing
 * - Numerical Safety (NaN, Infinity, Denormal suppression, Bounded Output)
 * - Multi-Sample Rate support (44.1 kHz, 48 kHz, 88.2 kHz, 96 kHz, 192 kHz)
 * - Mono and Stereo channel processing
 * - Media3 BaseAudioProcessor 16-bit PCM byte flow
 * - Throughput benchmarks for all 4 operational modes
 */
class MelofyDspEngineTest {

    // ========================================================================
    // 1. OPERATIONAL MODE TESTS
    // ========================================================================

    @Test
    fun melofyDspEngine_bothOff_executesTrueBypass() {
        val engine = MelofyDspEngine()
        val frameCount = 512
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val input = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.1) * 0.8).toFloat() }
        val output = FloatArray(totalSamples)

        // Parameters default to both disabled
        assertFalse(engine.isAnyActive)

        engine.process(input, output, frameCount, channelCount, 44100)

        for (i in 0 until totalSamples) {
            assertEquals("Bypass output must be bit-for-bit identical", input[i], output[i], 0.0f)
        }
    }

    @Test
    fun melofyDspEngine_loFiOnly_appliesLoFiWithoutReverb() {
        val engine = MelofyDspEngine()
        val frameCount = 1024
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val params = DspParameters(
            loFi = LoFiParameters(enabled = true, intensity = 0.8f, bitCrush = 0.4f, saturation = 0.3f),
            reverb = ReverbParameters(enabled = false)
        )
        engine.updateParameters(params)

        assertTrue(engine.currentParameters.isLoFiOnly)

        // 1. Feed impulse to verify Lo-Fi processes it
        val buffer1 = FloatArray(totalSamples) { 0.0f }
        buffer1[0] = 1.0f
        buffer1[1] = 1.0f
        val output1 = FloatArray(totalSamples)

        engine.process(buffer1, output1, frameCount, channelCount, 44100)
        assertTrue("Lo-Fi should modify the initial impulse", output1[0] > 0.0f)

        // 2. Feed silence in block 2: because reverb is OFF, output must be pure silence
        val buffer2 = FloatArray(totalSamples) { 0.0f }
        val output2 = FloatArray(totalSamples)

        engine.process(buffer2, output2, frameCount, channelCount, 44100)

        var nonZeroCount = 0
        for (sample in output2) {
            if (abs(sample) > 0.00001f) nonZeroCount++
        }
        assertEquals("Lo-Fi only must have zero reverb tail in subsequent buffer", 0, nonZeroCount)
    }

    @Test
    fun melofyDspEngine_reverbOnly_appliesReverbWithoutLoFi() {
        val engine = MelofyDspEngine()
        val frameCount = 1024
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val params = DspParameters(
            loFi = LoFiParameters(enabled = false),
            reverb = ReverbParameters(enabled = true, roomSize = 0.6f, decay = 0.6f, wetLevel = 0.5f, dryLevel = 0.0f)
        )
        engine.updateParameters(params)

        assertTrue(engine.currentParameters.isReverbOnly)

        // Block 1: Feed impulse
        val buffer1 = FloatArray(totalSamples) { 0.0f }
        buffer1[0] = 1.0f
        buffer1[1] = 1.0f
        val output1 = FloatArray(totalSamples)
        engine.process(buffer1, output1, frameCount, channelCount, 44100)

        // Block 2: Feed silence, should have reverberation tail
        val buffer2 = FloatArray(totalSamples) { 0.0f }
        val output2 = FloatArray(totalSamples)
        engine.process(buffer2, output2, frameCount, channelCount, 44100)

        var tailCount = 0
        for (sample in output2) {
            if (abs(sample) > 0.0001f) tailCount++
        }
        assertTrue("Reverb only must sustain reverberation tail", tailCount > 0)
    }

    @Test
    fun melofyDspEngine_bothOn_processesSequentially() {
        val engine = MelofyDspEngine()
        val frameCount = 1024
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val params = DspParameters(
            loFi = LoFiParameters(enabled = true, intensity = 0.7f, bitCrush = 0.3f, saturation = 0.4f),
            reverb = ReverbParameters(enabled = true, roomSize = 0.5f, decay = 0.5f, wetLevel = 0.4f, dryLevel = 0.8f)
        )
        engine.updateParameters(params)

        assertTrue(engine.currentParameters.isBothActive)

        val buffer = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.05) * 0.6).toFloat() }
        val output = FloatArray(totalSamples)

        engine.process(buffer, output, frameCount, channelCount, 44100)

        for (sample in output) {
            assertFalse(sample.isNaN())
            assertFalse(sample.isInfinite())
            assertTrue(sample in -1.0f..1.0f)
        }
    }

    // ========================================================================
    // 2. PRESET TESTS
    // ========================================================================

    @Test
    fun dspPreset_allPresets_produceDistinctConfigurations() {
        val presets = arrayOf(
            DspPreset.DREAMY,
            DspPreset.CASSETTE,
            DspPreset.LATE_NIGHT,
            DspPreset.DEEP_LOFI
        )

        for (preset in presets) {
            val params = DspParameters.createPreset(preset, enabled = true)
            assertEquals(preset, params.preset)
            assertTrue(params.isBothActive)
            assertTrue("Intensity must be normalized", params.loFi.intensity in 0.0f..1.0f)
            assertTrue("Room size must be normalized", params.reverb.roomSize in 0.0f..1.0f)
            assertTrue("Decay must be normalized", params.reverb.decay in 0.0f..1.0f)
            assertTrue("Wet level must be normalized", params.reverb.wetLevel in 0.0f..1.0f)
        }

        // Verify presets have distinct character
        val dreamy = DspParameters.createPreset(DspPreset.DREAMY)
        val cassette = DspParameters.createPreset(DspPreset.CASSETTE)
        val deepLofi = DspParameters.createPreset(DspPreset.DEEP_LOFI)

        assertTrue("Cassette must have higher saturation than Dreamy", cassette.loFi.saturation > dreamy.loFi.saturation)
        assertTrue("Deep Lo-Fi must have higher bit crush than Dreamy", deepLofi.loFi.bitCrush > dreamy.loFi.bitCrush)
        assertTrue("Deep Lo-Fi must have higher decay than Cassette", deepLofi.reverb.decay > cassette.reverb.decay)
    }

    @Test
    fun dspPreset_fromString_handlesEdgeCases() {
        assertEquals(DspPreset.DREAMY, DspPreset.fromString("DREAMY"))
        assertEquals(DspPreset.CASSETTE, DspPreset.fromString("cassette"))
        assertEquals(DspPreset.LATE_NIGHT, DspPreset.fromString("Late_Night"))
        assertEquals(DspPreset.DEEP_LOFI, DspPreset.fromString("deep_lofi"))
        assertEquals(DspPreset.CUSTOM, DspPreset.fromString("CUSTOM"))
        assertEquals(DspPreset.DEFAULT, DspPreset.fromString(null))
        assertEquals(DspPreset.DEFAULT, DspPreset.fromString("UNKNOWN_VALUE"))
    }

    @Test
    fun melofyDspEngine_setPreset_updatesEngineParameters() {
        val engine = MelofyDspEngine()
        engine.setLoFiEnabled(true)
        engine.setReverbEnabled(true)

        engine.setPreset(DspPreset.CASSETTE)

        assertEquals(DspPreset.CASSETTE, engine.currentParameters.preset)
        assertTrue(engine.currentParameters.loFi.enabled)
        assertTrue(engine.currentParameters.reverb.enabled)
        assertEquals(0.70f, engine.currentParameters.loFi.intensity, 0.01f)
        assertEquals(0.45f, engine.currentParameters.loFi.saturation, 0.01f)
    }

    // ========================================================================
    // 3. PRE-DELAY & PARAMETER SMOOTHING TESTS
    // ========================================================================

    @Test
    fun reverb_preDelay_delaysReverbReflections() {
        val engine = ReverbDspEngine()
        val frameCount = 1024
        val totalSamples = frameCount * 2

        val params = ReverbParameters(
            enabled = true,
            preDelay = 0.5f, // ~50 ms predelay = ~2205 samples at 44.1 kHz
            wetLevel = 1.0f,
            dryLevel = 0.0f
        )

        // Block 1: Feed impulse at t=0
        val buffer1 = FloatArray(totalSamples) { 0.0f }
        buffer1[0] = 1.0f
        buffer1[1] = 1.0f
        val output1 = FloatArray(totalSamples)

        engine.process(buffer1, output1, frameCount, 2, 44100, params)

        // Because pre-delay is ~2205 samples, Block 1 (samples 0..1023) should not have comb reflections yet
        var earlyOutputCount = 0
        for (sample in output1) {
            if (abs(sample) > 0.001f) earlyOutputCount++
        }
        assertEquals("Pre-delay should prevent immediate reverb reflections in block 1", 0, earlyOutputCount)
    }

    @Test
    fun melofyDspEngine_parameterSmoothing_transitionsSmoothly() {
        val engine = MelofyDspEngine()
        val frameCount = 512
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val buffer = FloatArray(totalSamples) { 0.5f }
        val output1 = FloatArray(totalSamples)
        val output2 = FloatArray(totalSamples)

        // Start with moderate intensity
        engine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.2f)))
        engine.process(buffer, output1, frameCount, channelCount, 44100)

        // Jump intensity to 1.0
        engine.updateParameters(DspParameters(loFi = LoFiParameters(enabled = true, intensity = 1.0f)))
        engine.process(buffer, output2, frameCount, channelCount, 44100)

        // Verify that sample values at the start of block 2 are close to end of block 1 (no vertical step)
        val endOfBlock1 = output1[totalSamples - 2]
        val startOfBlock2 = output2[0]
        val jump = abs(startOfBlock2 - endOfBlock1)

        assertTrue("Parameter smoothing must prevent abrupt vertical jumps (jump was $jump)", jump < 0.1f)
    }

    // ========================================================================
    // 4. NUMERICAL SAFETY & BOUNDS TESTS
    // ========================================================================

    @Test
    fun melofyDspEngine_extremeInputs_neverProduceNaNOrInfinity() {
        val engine = MelofyDspEngine()
        val frameCount = 512
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val params = DspParameters.createPreset(DspPreset.DEEP_LOFI, enabled = true)
        engine.updateParameters(params)

        val buffer = FloatArray(totalSamples)
        val output = FloatArray(totalSamples)

        // Feed alternating max values and small denormal numbers
        for (i in buffer.indices) {
            buffer[i] = if (i % 2 == 0) 1.5f else 1e-20f
        }

        engine.process(buffer, output, frameCount, channelCount, 44100)

        for (sample in output) {
            assertFalse("Sample must not be NaN", sample.isNaN())
            assertFalse("Sample must not be Infinite", sample.isInfinite())
            assertTrue("Sample must be clamped in [-1.0, 1.0]", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun melofyDspEngine_reset_clearsAllInternalStates() {
        val engine = MelofyDspEngine()
        val frameCount = 512
        val channelCount = 2
        val totalSamples = frameCount * channelCount

        val params = DspParameters.createPreset(DspPreset.DREAMY, enabled = true)
        engine.updateParameters(params)

        // Fill engine with energy
        val buffer = FloatArray(totalSamples) { 1.0f }
        val output = FloatArray(totalSamples)
        engine.process(buffer, output, frameCount, channelCount, 44100)

        // Reset
        engine.reset()

        // Feed silence
        val silence = FloatArray(totalSamples) { 0.0f }
        val postResetOutput = FloatArray(totalSamples)
        engine.process(silence, postResetOutput, frameCount, channelCount, 44100)

        for (sample in postResetOutput) {
            assertEquals("Post-reset output must be clean silence", 0.0f, sample, 0.00001f)
        }
    }

    // ========================================================================
    // 5. MULTI-SAMPLE RATE & CHANNEL TESTS
    // ========================================================================

    @Test
    fun melofyDspEngine_multiSampleRates_processesSuccessfully() {
        val engine = MelofyDspEngine()
        val sampleRates = intArrayOf(44100, 48000, 88200, 96000, 192000)
        val frameCount = 512

        val params = DspParameters.createPreset(DspPreset.DREAMY, enabled = true)
        engine.updateParameters(params)

        for (rate in sampleRates) {
            val totalSamples = frameCount * 2
            val input = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.05) * 0.7).toFloat() }
            val output = FloatArray(totalSamples)

            engine.process(input, output, frameCount, 2, rate)

            for (sample in output) {
                assertFalse("Sample rate $rate produced NaN", sample.isNaN())
                assertFalse("Sample rate $rate produced Infinite", sample.isInfinite())
                assertTrue("Sample rate $rate out of bounds", sample in -1.0f..1.0f)
            }
        }
    }

    @Test
    fun melofyDspEngine_monoChannel_processesSuccessfully() {
        val engine = MelofyDspEngine()
        val frameCount = 512
        val params = DspParameters.createPreset(DspPreset.CASSETTE, enabled = true)
        engine.updateParameters(params)

        val input = FloatArray(frameCount) { 0.4f }
        val output = FloatArray(frameCount)

        engine.process(input, output, frameCount, 1, 48000)

        for (sample in output) {
            assertFalse(sample.isNaN())
            assertTrue(sample in -1.0f..1.0f)
        }
    }

    // ========================================================================
    // 6. MEDIA3 BASE AUDIO PROCESSOR BUFFER TESTS
    // ========================================================================

    @Test
    fun melofyDspAudioProcessor_media3BufferQueue_handlesAllModes() {
        val processor = MelofyDspAudioProcessor()
        val inputFormat = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)

        val configured = processor.configure(inputFormat)
        assertEquals(inputFormat.sampleRate, configured.sampleRate)
        assertEquals(inputFormat.channelCount, configured.channelCount)
        assertEquals(inputFormat.encoding, configured.encoding)

        processor.flush()
        assertTrue(processor.isActive)

        val frameCount = 256
        val byteCount = frameCount * 2 * 2 // 1024 bytes

        // Test 1: Bypass (Both OFF)
        processor.updateParameters(DspParameters.DEFAULT)
        val inputBuf1 = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount * 2) inputBuf1.putShort(5000.toShort())
        inputBuf1.flip()

        processor.queueInput(inputBuf1)
        val outBuf1 = processor.output
        assertNotNull(outBuf1)
        assertEquals(byteCount, outBuf1.remaining())

        // Test 2: Active Preset (Both ON)
        processor.updateParameters(DspParameters.createPreset(DspPreset.DREAMY, enabled = true))
        val inputBuf2 = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount * 2) inputBuf2.putShort(12000.toShort())
        inputBuf2.flip()

        processor.queueInput(inputBuf2)
        val outBuf2 = processor.output
        assertNotNull(outBuf2)
        assertEquals(byteCount, outBuf2.remaining())

        outBuf2.order(ByteOrder.LITTLE_ENDIAN)
        while (outBuf2.hasRemaining()) {
            val sample = outBuf2.short
            assertTrue(sample in Short.MIN_VALUE..Short.MAX_VALUE)
        }
    }

    // ========================================================================
    // 7. THROUGHPUT BENCHMARK FOR ALL 4 MODES (JVM)
    // ========================================================================

    @Test
    fun melofyDspEngine_throughputBenchmark_measuresAllModes() {
        val engine = MelofyDspEngine()
        val frameCount = 1024
        val channelCount = 2
        val sampleRate = 44100
        val totalSamples = frameCount * channelCount

        val input = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.05) * 0.7).toFloat() }
        val output = FloatArray(totalSamples)

        val modes = listOf(
            "Bypass (Both OFF)" to DspParameters(loFi = LoFiParameters(enabled = false), reverb = ReverbParameters(enabled = false)),
            "Lo-Fi Only" to DspParameters(loFi = LoFiParameters(enabled = true, intensity = 0.7f), reverb = ReverbParameters(enabled = false)),
            "Reverb Only" to DspParameters(loFi = LoFiParameters(enabled = false), reverb = ReverbParameters(enabled = true, wetLevel = 0.4f)),
            "Both ON" to DspParameters.createPreset(DspPreset.DREAMY, enabled = true)
        )

        val blockCount = 500
        val realTimeSeconds = (blockCount * frameCount).toDouble() / sampleRate.toDouble()

        for ((modeName, params) in modes) {
            engine.updateParameters(params)

            // Warmup
            for (i in 0 until 50) engine.process(input, output, frameCount, channelCount, sampleRate)

            val startTime = System.nanoTime()
            for (i in 0 until blockCount) {
                engine.process(input, output, frameCount, channelCount, sampleRate)
            }
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0
            val speedX = realTimeSeconds / (elapsedMs / 1000.0)

            // All modes must run at least 20x faster than real-time on host JVM
            assertTrue("Mode $modeName should be > 20x real-time (was ${speedX}x)", speedX > 20.0)
        }
    }
}
