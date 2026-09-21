package com.auralis.app.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unified Media3 [AudioProcessor] integrating [MelofyDspEngine] into ExoPlayer's audio pipeline.
 *
 * Placed in [DefaultAudioSink.Builder.setAudioProcessors] between the audio decoders and AudioTrack:
 * - When effects are disabled ([MelofyDspEngine.isAnyActive] = false), executes an O(1) direct buffer
 *   passthrough with zero audio degradation and zero heap allocations.
 * - When active, processes 16-bit Little-Endian PCM audio in real time through Lo-Fi and Reverb.
 * - Synchronizes state flushes with ExoPlayer seek and transition events.
 */
class MelofyDspAudioProcessor(
    val engine: MelofyDspEngine = MelofyDspEngine()
) : BaseAudioProcessor() {

    // Reusable work buffer for converting 16-bit PCM bytes to floats and back without per-buffer heap allocations
    private var workBuffer = FloatArray(4096)

    /**
     * Atomically updates DSP parameters.
     */
    fun updateParameters(params: DspParameters) {
        engine.updateParameters(params)
    }

    fun getParameters(): DspParameters = engine.currentParameters

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        if (!engine.isAnyActive) {
            // Direct O(1) bypass: copy input directly to output buffer with zero DSP overhead
            val outputBuffer = replaceOutputBuffer(remainingBytes)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val totalSamples = remainingBytes / 2
        if (workBuffer.size < totalSamples) {
            workBuffer = FloatArray(totalSamples)
        }

        val channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        val frameCount = totalSamples / channelCount

        // 1. Read 16-bit Little-Endian PCM samples into normalized floats [-1.0f..1.0f]
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val shortVal = inputBuffer.short
            workBuffer[i] = shortVal / 32768.0f
        }

        // 2. Process in-place through unified Melofy DSP engine
        engine.process(
            inputSamples = workBuffer,
            outputSamples = workBuffer,
            frameCount = frameCount,
            channelCount = channelCount,
            sampleRate = sampleRate
        )

        // 3. Write processed floats back to output buffer as 16-bit Little-Endian PCM
        val outputBuffer = replaceOutputBuffer(remainingBytes)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val sample = workBuffer[i].coerceIn(-1.0f, 1.0f)
            val shortVal = (sample * 32767.0f).toInt().toShort()
            outputBuffer.putShort(shortVal)
        }
        outputBuffer.flip()
    }

    @Deprecated("Deprecated in Java")
    override fun onFlush() {
        engine.reset()
    }
}
