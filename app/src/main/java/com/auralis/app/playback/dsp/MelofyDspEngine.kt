package com.auralis.app.playback.dsp

/**
 * Unified Real-Time Audio DSP Engine for Melofy.
 *
 * Orchestrates the sequential [LoFiDspEngine] and [ReverbDspEngine] stages with:
 * - 4 distinct operational modes:
 *   1. Both OFF (Fast $O(1)$ bypass)
 *   2. Lo-Fi ON, Reverb OFF (Lo-Fi only)
 *   3. Lo-Fi OFF, Reverb ON (Reverb only)
 *   4. Both ON (Sequential Lo-Fi -> Reverb)
 * - Zero per-buffer memory allocations using preallocated intermediate buffers.
 * - Thread-safe atomic parameter switching via [@Volatile] [DspParameters].
 * - Deterministic reset handling on seek, flush, or track transition.
 */
class MelofyDspEngine(
    val loFiEngine: LoFiDspEngine = LoFiDspEngine(),
    val reverbEngine: ReverbDspEngine = ReverbDspEngine()
) {

    @Volatile
    var currentParameters: DspParameters = DspParameters.DEFAULT
        private set

    // Preallocated intermediate buffer for chaining Lo-Fi -> Reverb without heap allocation
    private var intermediateBuffer = FloatArray(4096)

    val isAnyActive: Boolean get() = currentParameters.isAnyActive

    /**
     * Atomically updates DSP parameters from any thread (UI, background service, or controller).
     */
    fun updateParameters(params: DspParameters) {
        currentParameters = params
    }

    fun getParameters(): DspParameters = currentParameters

    /**
     * Activates a predefined [DspPreset] preserving active enabled states.
     */
    fun setPreset(preset: DspPreset) {
        val current = currentParameters
        val configured = DspParameters.createPreset(
            preset = preset,
            enabled = current.isAnyActive
        )
        currentParameters = configured.copy(
            loFi = configured.loFi.copy(enabled = current.loFi.enabled),
            reverb = configured.reverb.copy(enabled = current.reverb.enabled)
        )
    }

    /**
     * Enables or disables Lo-Fi processing.
     */
    fun setLoFiEnabled(enabled: Boolean) {
        currentParameters = currentParameters.copy(
            loFi = currentParameters.loFi.copy(enabled = enabled)
        )
    }

    /**
     * Enables or disables Reverb processing.
     */
    fun setReverbEnabled(enabled: Boolean) {
        currentParameters = currentParameters.copy(
            reverb = currentParameters.reverb.copy(enabled = enabled)
        )
    }

    /**
     * Resets internal DSP filter states, delay lines, and accumulators for both engines.
     * Invoked during seek, pause-flush, and track transitions to eliminate clicks or echoes.
     */
    fun reset() {
        loFiEngine.reset()
        reverbEngine.reset()
    }

    /**
     * Real-time audio processing entry point.
     *
     * @param inputSamples Interleaved PCM samples as normalized floats in [-1.0f..1.0f].
     * @param outputSamples Target array to receive processed float samples (can be identical to [inputSamples]).
     * @param frameCount Number of audio frames to process.
     * @param channelCount Number of audio channels (1 = Mono, 2 = Stereo).
     * @param sampleRate Stream sample rate in Hz (e.g. 44100, 48000, 96000).
     */
    fun process(
        inputSamples: FloatArray,
        outputSamples: FloatArray,
        frameCount: Int,
        channelCount: Int,
        sampleRate: Int
    ) {
        val params = currentParameters
        val totalSamples = frameCount * channelCount

        val isLoFi = params.loFi.enabled && params.loFi.intensity > 0.0f
        val isReverb = params.reverb.enabled && params.reverb.wetLevel > 0.0f

        when {
            // Mode 1: Bypass (Both OFF)
            !isLoFi && !isReverb -> {
                if (inputSamples !== outputSamples) {
                    System.arraycopy(inputSamples, 0, outputSamples, 0, totalSamples)
                }
            }

            // Mode 2: Lo-Fi Only
            isLoFi && !isReverb -> {
                loFiEngine.process(
                    inputSamples = inputSamples,
                    outputSamples = outputSamples,
                    frameCount = frameCount,
                    channelCount = channelCount,
                    sampleRate = sampleRate,
                    params = params.loFi
                )
            }

            // Mode 3: Reverb Only
            !isLoFi && isReverb -> {
                reverbEngine.process(
                    inputSamples = inputSamples,
                    outputSamples = outputSamples,
                    frameCount = frameCount,
                    channelCount = channelCount,
                    sampleRate = sampleRate,
                    params = params.reverb
                )
            }

            // Mode 4: Both ON (Lo-Fi -> Reverb)
            else -> {
                if (intermediateBuffer.size < totalSamples) {
                    val newCapacity = maxOf(totalSamples, intermediateBuffer.size * 2)
                    intermediateBuffer = FloatArray(newCapacity)
                }

                // 1. Process Lo-Fi into preallocated intermediate buffer
                loFiEngine.process(
                    inputSamples = inputSamples,
                    outputSamples = intermediateBuffer,
                    frameCount = frameCount,
                    channelCount = channelCount,
                    sampleRate = sampleRate,
                    params = params.loFi
                )

                // 2. Process Reverb from intermediate buffer into output buffer
                reverbEngine.process(
                    inputSamples = intermediateBuffer,
                    outputSamples = outputSamples,
                    frameCount = frameCount,
                    channelCount = channelCount,
                    sampleRate = sampleRate,
                    params = params.reverb
                )
            }
        }
    }
}
