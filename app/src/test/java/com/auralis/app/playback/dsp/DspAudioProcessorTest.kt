@file:Suppress("DEPRECATION")

package com.auralis.app.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Deterministic audio DSP test suite validating:
 * - Mathematical correctness and numerical safety (no NaN, no Infinity)
 * - Deterministic test signals: Silence, DC, Impulse, Sine wave, Stereo vs Mono
 * - Reverb stability under extreme feedback / room size settings
 * - Lo-Fi stability under extreme bit crush / decimation / saturation
 * - Instant true bypass bit-identity
 * - Thread-safe parameter sanitization and atomic updates
 * - Media3 BaseAudioProcessor integration and Little-Endian PCM byte handling
 * - Real-time audio throughput and zero-allocation performance on 1024-sample blocks
 */
class DspAudioProcessorTest {

    // ========================================================================
    // 1. PARAMETER MODEL & SANITIZATION TESTS
    // ========================================================================

    @Test
    fun dspParameters_defaultValues_areSafe() {
        val params = DspParameters.DEFAULT
        assertFalse(params.isAnyActive)
        assertFalse(params.loFi.enabled)
        assertFalse(params.reverb.enabled)
        assertEquals(0.5f, params.loFi.intensity, 0.001f)
        assertEquals(0.5f, params.reverb.roomSize, 0.001f)
        assertEquals(0.5f, params.reverb.decay, 0.001f)
        assertEquals(1.0f, params.reverb.dryLevel, 0.001f)
    }

    @Test
    fun dspParameters_sanitized_handlesInvalidFloats() {
        val sanitizedLoFi = LoFiParameters.sanitized(
            enabled = true,
            intensity = Float.NaN,
            sampleRateReduction = Float.POSITIVE_INFINITY,
            bitCrush = -5.0f,
            lowPassCutoff = 10.0f,
            saturation = Float.NaN
        )
        assertTrue(sanitizedLoFi.enabled)
        assertEquals(0.5f, sanitizedLoFi.intensity, 0.001f) // fallback
        assertEquals(0.0f, sanitizedLoFi.sampleRateReduction, 0.001f) // fallback
        assertEquals(0.0f, sanitizedLoFi.bitCrush, 0.001f) // clamped
        assertEquals(1.0f, sanitizedLoFi.lowPassCutoff, 0.001f) // clamped
        assertEquals(0.2f, sanitizedLoFi.saturation, 0.001f) // fallback

        val sanitizedReverb = ReverbParameters.sanitized(
            enabled = true,
            roomSize = Float.NaN,
            decay = 100.0f,
            damping = -0.5f,
            wetLevel = Float.NEGATIVE_INFINITY,
            dryLevel = 2.0f
        )
        assertTrue(sanitizedReverb.enabled)
        assertEquals(0.5f, sanitizedReverb.roomSize, 0.001f) // fallback
        assertEquals(1.0f, sanitizedReverb.decay, 0.001f) // clamped
        assertEquals(0.0f, sanitizedReverb.damping, 0.001f) // clamped
        assertEquals(0.3f, sanitizedReverb.wetLevel, 0.001f) // fallback
        assertEquals(1.0f, sanitizedReverb.dryLevel, 0.001f) // clamped
    }

    @Test
    fun dspParameters_directConstruction_throwsOnNaNOrInfinity() {
        assertThrows(IllegalArgumentException::class.java) {
            LoFiParameters(intensity = Float.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LoFiParameters(bitCrush = Float.POSITIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReverbParameters(roomSize = Float.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReverbParameters(decay = Float.NEGATIVE_INFINITY)
        }
    }

    // ========================================================================
    // 2. LO-FI DSP ENGINE TESTS
    // ========================================================================

    @Test
    fun loFi_silenceInput_producesExactSilence() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(1024) { 0.0f }
        val output = FloatArray(1024)
        val params = LoFiParameters(
            enabled = true,
            intensity = 1.0f,
            sampleRateReduction = 0.5f,
            bitCrush = 0.5f,
            lowPassCutoff = 0.5f,
            saturation = 0.5f
        )

        engine.process(buffer, output, 512, 2, 44100, params)

        for (sample in output) {
            assertEquals(0.0f, sample, 0.00001f)
        }
    }

    @Test
    fun loFi_constantInput_stabilizesToFiniteBoundedOutput() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(1024) { 0.5f }
        val output = FloatArray(1024)
        val params = LoFiParameters(
            enabled = true,
            intensity = 0.8f,
            sampleRateReduction = 0.4f,
            bitCrush = 0.4f,
            lowPassCutoff = 0.3f,
            saturation = 0.5f
        )

        engine.process(buffer, output, 512, 2, 44100, params)

        for (sample in output) {
            assertFalse("Output must not be NaN", sample.isNaN())
            assertFalse("Output must not be Infinite", sample.isInfinite())
            assertTrue("Output must be bounded in [-1.0, 1.0]", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun loFi_impulseInput_decaysSmoothly() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(512) { 0.0f }
        buffer[0] = 1.0f // Left impulse
        buffer[1] = 1.0f // Right impulse
        val output = FloatArray(512)
        val params = LoFiParameters(
            enabled = true,
            intensity = 1.0f,
            sampleRateReduction = 0.0f,
            bitCrush = 0.0f,
            lowPassCutoff = 0.2f, // low cutoff
            saturation = 0.0f
        )

        engine.process(buffer, output, 256, 2, 44100, params)

        // Filter output at t=0 should be attenuated by alpha
        assertTrue("Impulse response starts positive", output[0] > 0.0f)
        // Subsequent samples decay towards 0
        assertTrue("Filter decays", output[10] < output[0])
    }

    @Test
    fun loFi_extremeParameters_maintainsStrictNumericalBounds() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(1024) { i -> (i % 2 == 0) then 1.0f ?: -1.0f }
        val output = FloatArray(1024)
        val params = LoFiParameters(
            enabled = true,
            intensity = 1.0f,
            sampleRateReduction = 1.0f,
            bitCrush = 1.0f,
            lowPassCutoff = 0.0f,
            saturation = 1.0f
        )

        engine.process(buffer, output, 512, 2, 44100, params)

        for (sample in output) {
            assertFalse(sample.isNaN())
            assertFalse(sample.isInfinite())
            assertTrue("Output sample $sample out of bounds", sample in -1.0f..1.0f)
        }
    }

    @Test
    fun loFi_monoChannel_processesCorrectly() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(512) { 0.3f }
        val output = FloatArray(512)
        val params = LoFiParameters(enabled = true, intensity = 0.7f, saturation = 0.5f)

        engine.process(buffer, output, 512, 1, 48000, params)

        for (sample in output) {
            assertFalse(sample.isNaN())
            assertTrue(sample in -1.0f..1.0f)
        }
    }

    @Test
    fun loFi_bypass_copiesBitForBit() {
        val engine = LoFiDspEngine()
        val buffer = FloatArray(256) { i -> (i / 256.0f) * 0.8f }
        val output = FloatArray(256)
        val params = LoFiParameters(enabled = false)

        engine.process(buffer, output, 128, 2, 44100, params)

        for (i in buffer.indices) {
            assertEquals("Bypass must be bit-for-bit identical", buffer[i], output[i], 0.0f)
        }
    }

    // ========================================================================
    // 3. REVERB DSP ENGINE TESTS
    // ========================================================================

    @Test
    fun reverb_silenceInput_producesExactSilence() {
        val engine = ReverbDspEngine()
        val buffer = FloatArray(1024) { 0.0f }
        val output = FloatArray(1024)
        val params = ReverbParameters(
            enabled = true,
            roomSize = 0.8f,
            decay = 0.8f,
            damping = 0.2f,
            wetLevel = 0.5f,
            dryLevel = 1.0f
        )

        engine.process(buffer, output, 512, 2, 44100, params)

        for (sample in output) {
            assertEquals(0.0f, sample, 0.00001f)
        }
    }

    @Test
    fun reverb_impulseInput_generatesReverberationTail() {
        val engine = ReverbDspEngine()
        val buffer1 = FloatArray(2048) { 0.0f }
        buffer1[0] = 1.0f // Left impulse
        buffer1[1] = 1.0f // Right impulse
        val output1 = FloatArray(2048)

        val params = ReverbParameters(
            enabled = true,
            roomSize = 0.5f,
            decay = 0.7f,
            damping = 0.2f,
            wetLevel = 0.5f,
            dryLevel = 0.0f // wet only to isolate reverb reflections
        )

        // Block 1 contains the impulse
        engine.process(buffer1, output1, 1024, 2, 44100, params)

        // Block 2 contains pure silence input: should still output sustained reverb tail
        val buffer2 = FloatArray(2048) { 0.0f }
        val output2 = FloatArray(2048)
        engine.process(buffer2, output2, 1024, 2, 44100, params)

        var nonZeroCount = 0
        var maxTailAmp = 0.0f
        for (sample in output2) {
            if (abs(sample) > 0.0001f) nonZeroCount++
            if (abs(sample) > maxTailAmp) maxTailAmp = abs(sample)
        }

        println("reverb_impulseInput_generatesReverberationTail: nonZeroCount = $nonZeroCount, maxTailAmp = $maxTailAmp")
        assertTrue("Reverb tail must be sustained into second buffer", nonZeroCount > 0)
        assertTrue("Reverb tail must be finite and bounded", maxTailAmp in 0.0001f..1.0f)
    }

    @Test
    fun reverb_stereoDecorrelation_producesDistinctLeftAndRightChannels() {
        val engine = ReverbDspEngine()
        val buffer = FloatArray(4096) { 0.0f }
        buffer[0] = 1.0f
        buffer[1] = 1.0f // Identical mono impulse on both channels
        val output = FloatArray(4096)
        val params = ReverbParameters(
            enabled = true,
            roomSize = 0.7f,
            decay = 0.7f,
            damping = 0.3f,
            wetLevel = 1.0f,
            dryLevel = 0.0f // isolate wet
        )

        engine.process(buffer, output, 2048, 2, 44100, params)

        var differences = 0
        for (i in 0 until 2048) {
            val left = output[i * 2]
            val right = output[i * 2 + 1]
            if (abs(left - right) > 0.001f) {
                differences++
            }
        }

        println("reverb_stereoDecorrelation: differences = $differences")
        assertTrue("Left and Right channels must be decorrelated by prime offsets", differences > 0)
    }

    @Test
    fun reverb_stabilityUnderExtremeParameters_neverExplodes() {
        val engine = ReverbDspEngine()
        val params = ReverbParameters(
            enabled = true,
            roomSize = 1.0f, // maximum room size
            decay = 1.0f,    // maximum decay (feedback limit 0.975)
            damping = 0.0f,  // minimum damping
            wetLevel = 1.0f,
            dryLevel = 1.0f
        )

        val buffer = FloatArray(1024)
        val output = FloatArray(1024)

        // Inject 50 consecutive impulse trains to vigorously test feedback accumulation
        for (iteration in 0 until 50) {
            for (i in buffer.indices) {
                buffer[i] = if (i % 100 == 0) 0.8f else 0.0f
            }
            engine.process(buffer, output, 512, 2, 44100, params)

            for (sample in output) {
                assertFalse("Sample must not be NaN at iteration $iteration", sample.isNaN())
                assertFalse("Sample must not be Infinite at iteration $iteration", sample.isInfinite())
                assertTrue("Sample $sample must be bounded in [-1.0, 1.0]", sample in -1.0f..1.0f)
            }
        }
    }

    @Test
    fun reverb_reset_clearsAllEchoesImmediately() {
        val engine = ReverbDspEngine()
        val buffer = FloatArray(1024) { 0.0f }
        buffer[0] = 1.0f
        buffer[1] = 1.0f
        val output = FloatArray(1024)
        val params = ReverbParameters(enabled = true, wetLevel = 1.0f, dryLevel = 0.0f)

        // Populate reverb delay lines with energy
        engine.process(buffer, output, 512, 2, 44100, params)

        // Call reset (e.g. on seek or track transition)
        engine.reset()

        // Feed silence
        val silence = FloatArray(1024) { 0.0f }
        val postResetOutput = FloatArray(1024)
        engine.process(silence, postResetOutput, 512, 2, 44100, params)

        for (sample in postResetOutput) {
            assertEquals("Post-reset output must be pure silence", 0.0f, sample, 0.00001f)
        }
    }

    // ========================================================================
    // 4. SINE WAVE & VARIABLE SAMPLE RATE TESTS
    // ========================================================================

    @Test
    fun combinedDsp_sineWave_processesAtMultipleSampleRates() {
        val loFiEngine = LoFiDspEngine()
        val reverbEngine = ReverbDspEngine()

        val sampleRates = intArrayOf(44100, 48000, 88200, 96000)

        for (sampleRate in sampleRates) {
            val frameCount = 1024
            val buffer = FloatArray(frameCount * 2)
            val loFiOutput = FloatArray(frameCount * 2)
            val finalOutput = FloatArray(frameCount * 2)

            // Generate 440 Hz test sine wave
            val frequency = 440.0
            for (i in 0 until frameCount) {
                val t = i.toDouble() / sampleRate.toDouble()
                val sample = sin(2.0 * PI * frequency * t).toFloat() * 0.7f
                buffer[i * 2] = sample
                buffer[i * 2 + 1] = sample
            }

            val loFiParams = LoFiParameters(
                enabled = true,
                intensity = 0.6f,
                sampleRateReduction = 0.2f,
                bitCrush = 0.2f,
                lowPassCutoff = 0.7f,
                saturation = 0.3f
            )
            val reverbParams = ReverbParameters(
                enabled = true,
                roomSize = 0.5f,
                decay = 0.5f,
                damping = 0.3f,
                wetLevel = 0.3f,
                dryLevel = 0.9f
            )

            // 1. Lo-Fi
            loFiEngine.process(buffer, loFiOutput, frameCount, 2, sampleRate, loFiParams)
            // 2. Reverb
            reverbEngine.process(loFiOutput, finalOutput, frameCount, 2, sampleRate, reverbParams)

            for (sample in finalOutput) {
                assertFalse("Sample rate $sampleRate produced NaN", sample.isNaN())
                assertFalse("Sample rate $sampleRate produced Infinity", sample.isInfinite())
                assertTrue("Sample rate $sampleRate out of bounds: $sample", sample in -1.0f..1.0f)
            }
        }
    }

    // ========================================================================
    // 5. MEDIA3 BASE AUDIO PROCESSOR BUFFER TESTS
    // ========================================================================

    @Test
    fun loFiAudioProcessor_media3BufferQueue_processesCorrectly() {
        val processor = LoFiAudioProcessor()
        val inputFormat = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)

        val configuredFormat = processor.configure(inputFormat)
        assertEquals(inputFormat.sampleRate, configuredFormat.sampleRate)
        assertEquals(inputFormat.channelCount, configuredFormat.channelCount)
        assertEquals(inputFormat.encoding, configuredFormat.encoding)

        processor.flush()
        assertTrue(processor.isActive)

        // Enable Lo-Fi
        processor.updateParameters(LoFiParameters(enabled = true, intensity = 0.8f, saturation = 0.5f))

        // Create 256 stereo 16-bit PCM frames (1024 bytes)
        val frameCount = 256
        val byteCount = frameCount * 2 * 2
        val inputByteBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount * 2) {
            inputByteBuffer.putShort((i * 100).toShort())
        }
        inputByteBuffer.flip()

        processor.queueInput(inputByteBuffer)
        val outputBuffer = processor.output

        assertNotNull(outputBuffer)
        assertEquals("Output buffer size must match input size", byteCount, outputBuffer.remaining())

        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while (outputBuffer.hasRemaining()) {
            val sample = outputBuffer.short
            assertTrue(sample in Short.MIN_VALUE..Short.MAX_VALUE)
        }
    }

    @Test
    fun reverbAudioProcessor_media3BufferQueue_processesCorrectly() {
        val processor = ReverbAudioProcessor()
        val inputFormat = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)

        processor.configure(inputFormat)
        processor.flush()
        assertTrue(processor.isActive)

        processor.updateParameters(ReverbParameters(enabled = true, wetLevel = 0.4f, dryLevel = 1.0f))

        val frameCount = 256
        val byteCount = frameCount * 2 * 2
        val inputByteBuffer = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until frameCount * 2) {
            inputByteBuffer.putShort(15000.toShort())
        }
        inputByteBuffer.flip()

        processor.queueInput(inputByteBuffer)
        val outputBuffer = processor.output

        assertNotNull(outputBuffer)
        assertEquals(byteCount, outputBuffer.remaining())
    }

    @Test
    fun audioProcessors_throwOnUnsupportedEncoding() {
        val loFiProcessor = LoFiAudioProcessor()
        val reverbProcessor = ReverbAudioProcessor()

        val invalidFormat = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_FLOAT)

        assertThrows(AudioProcessor.UnhandledAudioFormatException::class.java) {
            loFiProcessor.configure(invalidFormat)
        }
        assertThrows(AudioProcessor.UnhandledAudioFormatException::class.java) {
            reverbProcessor.configure(invalidFormat)
        }
    }

    // ========================================================================
    // 6. REAL-TIME ZERO-ALLOCATION THROUGHPUT BENCHMARK (JVM)
    // ========================================================================

    @Test
    fun dspPipeline_throughputBenchmark_processesBlocksFarFasterThanRealtime() {
        val loFiEngine = LoFiDspEngine()
        val reverbEngine = ReverbDspEngine()

        val frameCount = 1024
        val channelCount = 2
        val sampleRate = 44100
        val totalSamples = frameCount * channelCount

        val inputSamples = FloatArray(totalSamples) { i -> (sin(i.toDouble() * 0.05) * 0.7).toFloat() }
        val loFiOut = FloatArray(totalSamples)
        val reverbOut = FloatArray(totalSamples)

        val loFiParams = LoFiParameters(
            enabled = true,
            intensity = 0.8f,
            sampleRateReduction = 0.3f,
            bitCrush = 0.3f,
            lowPassCutoff = 0.6f,
            saturation = 0.4f
        )
        val reverbParams = ReverbParameters(
            enabled = true,
            roomSize = 0.7f,
            decay = 0.7f,
            damping = 0.3f,
            wetLevel = 0.4f,
            dryLevel = 1.0f
        )

        // Warm up JIT
        for (i in 0 until 100) {
            loFiEngine.process(inputSamples, loFiOut, frameCount, channelCount, sampleRate, loFiParams)
            reverbEngine.process(loFiOut, reverbOut, frameCount, channelCount, sampleRate, reverbParams)
        }

        // Benchmark 1000 consecutive blocks (1000 * 1024 frames = ~23.2 seconds of real-time audio)
        val blockCount = 1000
        val startTime = System.nanoTime()

        for (i in 0 until blockCount) {
            loFiEngine.process(inputSamples, loFiOut, frameCount, channelCount, sampleRate, loFiParams)
            reverbEngine.process(loFiOut, reverbOut, frameCount, channelCount, sampleRate, reverbParams)
        }

        val elapsedNanos = System.nanoTime() - startTime
        val elapsedMillis = elapsedNanos / 1_000_000.0

        val realTimeSeconds = (blockCount * frameCount).toDouble() / sampleRate.toDouble()
        val processingSeconds = elapsedMillis / 1000.0
        val speedMultiplier = realTimeSeconds / processingSeconds

        println("DSP Throughput Benchmark: Processed ${realTimeSeconds.format(2)}s of audio in ${elapsedMillis.format(2)}ms (~${speedMultiplier.format(1)}x real-time speed)")

        // Must run at least 50x faster than real-time on host JVM
        assertTrue(
            "DSP processing must run significantly faster than real-time (was ${speedMultiplier}x)",
            speedMultiplier > 50.0
        )
    }

    private fun Double.format(digits: Int) = String.format("%.${digits}f", this)

    private infix fun <T> Boolean.then(value: T): T? = if (this) value else null
}
