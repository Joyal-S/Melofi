package com.auralis.app.playback.dsp

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Media3 [AudioProcessor] integrating [LoFiDspEngine] directly into ExoPlayer's audio pipeline.
 *
 * Supports thread-safe real-time parameter updates via [updateParameters].
 * When disabled ([LoFiParameters.enabled] = false), operates as an O(1) direct buffer passthrough
 * with zero audio processing and zero memory allocations.
 */
@Deprecated(
    message = "Legacy standalone audio processor retained for test compatibility. Authoritative production pipeline is MelofyDspAudioProcessor -> MelofyDspEngine.",
    level = DeprecationLevel.WARNING
)
class LoFiAudioProcessor(
    private val engine: LoFiDspEngine = LoFiDspEngine()
) : BaseAudioProcessor() {

    @Volatile
    private var currentParams: LoFiParameters = LoFiParameters.DEFAULT

    // Work buffer for converting 16-bit PCM bytes to floats and back without per-buffer heap allocations
    private var workBuffer = FloatArray(4096)

    /**
     * Atomically updates DSP parameters from any thread (e.g. UI or service).
     */
    fun updateParameters(params: LoFiParameters) {
        currentParams = params
    }

    fun getParameters(): LoFiParameters = currentParams

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remainingBytes = inputBuffer.remaining()
        if (remainingBytes == 0) return

        val params = currentParams
        if (!params.enabled || params.intensity <= 0.0f) {
            // Direct bypass: copy input directly to output buffer with zero DSP overhead
            val outputBuffer = replaceOutputBuffer(remainingBytes)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Each 16-bit sample is 2 bytes
        val totalSamples = remainingBytes / 2
        if (workBuffer.size < totalSamples) {
            val newCapacity = maxOf(totalSamples, workBuffer.size * 2)
            workBuffer = FloatArray(newCapacity)
        }

        val channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        val frameCount = totalSamples / channelCount

        // 1. Read 16-bit PCM samples into normalized floats [-1.0f..1.0f]
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val shortVal = inputBuffer.short
            workBuffer[i] = shortVal / 32768.0f
        }

        // 2. Process in-place through the DSP engine
        engine.process(
            inputSamples = workBuffer,
            outputSamples = workBuffer,
            frameCount = frameCount,
            channelCount = channelCount,
            sampleRate = sampleRate,
            params = params
        )

        // 3. Write processed floats back to output buffer as 16-bit PCM
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
