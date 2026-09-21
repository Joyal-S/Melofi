package com.auralis.app.playback.dsp

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * High-performance, zero-allocation Real-Time Stereo Schroeder-Moorer Reverberator with Pre-Delay.
 *
 * Architecture:
 * - Pre-Delay Line (0 ms to 100 ms) separating direct sound from reverb onset
 * - 4 Parallel Feedback Comb Filters (FBCF) per channel with prime delay lines
 * - Low-pass damping within comb feedback loops simulating acoustic air/wall absorption
 * - 2 Series All-Pass Filters (APF) per channel for dense phase diffusion
 * - Stereo decorrelation using prime-offset delay buffers to preserve stereo width
 * - Click-free parameter smoothing for wet/dry levels and decay
 * - Denormal float suppression to eliminate CPU cycles wasted on subnormal numbers
 * - Preallocated circular delay buffers sized to accommodate up to 192 kHz sample rates
 *
 * Real-time audio constraints:
 * - Zero allocations during [process] execution.
 * - Strict feedback gain limiting (< 0.975f) to mathematically prevent runaway feedback.
 * - Bounded input/output range [-1.0f..1.0f].
 */
class ReverbDspEngine {

    companion object {
        // Base delay lengths in samples at 44.1 kHz (mutually prime numbers)
        private val COMB_DELAYS_L = intArrayOf(1116, 1188, 1277, 1356)
        private val COMB_DELAYS_R = intArrayOf(1139, 1211, 1301, 1381)

        private val APF_DELAYS_L = intArrayOf(225, 556)
        private val APF_DELAYS_R = intArrayOf(241, 577)

        // Buffer capacities sized for max sample rate (192 kHz) and max room size scaling
        private const val COMB_BUFFER_SIZE = 16384
        private const val APF_BUFFER_SIZE = 8192
        private const val PRE_DELAY_BUFFER_SIZE = 20480 // > 100 ms at 192 kHz (19200)

        private const val APF_GAIN = 0.5f
        private const val DENORMAL_THRESHOLD = 1e-15f
    }

    // Circular delay buffers for Comb Filters (4 left, 4 right)
    private val combBuffersL = Array(4) { FloatArray(COMB_BUFFER_SIZE) }
    private val combBuffersR = Array(4) { FloatArray(COMB_BUFFER_SIZE) }
    private val combIndicesL = IntArray(4)
    private val combIndicesR = IntArray(4)
    private val combDampStatesL = FloatArray(4)
    private val combDampStatesR = FloatArray(4)

    // Circular delay buffers for All-Pass Filters (2 left, 2 right)
    private val apfBuffersL = Array(2) { FloatArray(APF_BUFFER_SIZE) }
    private val apfBuffersR = Array(2) { FloatArray(APF_BUFFER_SIZE) }
    private val apfIndicesL = IntArray(2)
    private val apfIndicesR = IntArray(2)

    // Circular delay buffers for Pre-Delay
    private val preDelayBufferL = FloatArray(PRE_DELAY_BUFFER_SIZE)
    private val preDelayBufferR = FloatArray(PRE_DELAY_BUFFER_SIZE)
    private var preDelayIndexL = 0
    private var preDelayIndexR = 0

    // Smoothed parameter states
    private var currentWetLevel = 0.0f
    private var currentDryLevel = 1.0f
    private var currentFeedbackGain = 0.80f
    private var isInitialized = false

    /**
     * Clears all circular delay lines and damping filter states.
     * Invoked during seek, pause-flush, and track transitions to eliminate echoes.
     */
    fun reset() {
        for (i in 0 until 4) {
            combBuffersL[i].fill(0.0f)
            combBuffersR[i].fill(0.0f)
            combIndicesL[i] = 0
            combIndicesR[i] = 0
            combDampStatesL[i] = 0.0f
            combDampStatesR[i] = 0.0f
        }
        for (i in 0 until 2) {
            apfBuffersL[i].fill(0.0f)
            apfBuffersR[i].fill(0.0f)
            apfIndicesL[i] = 0
            apfIndicesR[i] = 0
        }
        preDelayBufferL.fill(0.0f)
        preDelayBufferR.fill(0.0f)
        preDelayIndexL = 0
        preDelayIndexR = 0
        isInitialized = false
    }

    /**
     * Processes interleaved audio samples in real time with parameter smoothing.
     */
    fun process(
        inputSamples: FloatArray,
        outputSamples: FloatArray,
        frameCount: Int,
        channelCount: Int,
        sampleRate: Int,
        params: ReverbParameters
    ) {
        if (!params.enabled || params.wetLevel <= 0.0f) {
            if (inputSamples !== outputSamples) {
                val copyLength = frameCount * channelCount
                for (i in 0 until copyLength) {
                    outputSamples[i] = inputSamples[i] * params.dryLevel
                }
            } else if (params.dryLevel != 1.0f) {
                val copyLength = frameCount * channelCount
                for (i in 0 until copyLength) {
                    outputSamples[i] = inputSamples[i] * params.dryLevel
                }
            }
            currentWetLevel = 0.0f
            currentDryLevel = params.dryLevel
            return
        }

        val targetWet = params.wetLevel
        val targetDry = params.dryLevel
        val safeRate = sampleRate.coerceIn(8000, 192000)
        val rateRatio = safeRate.toFloat() / 44100.0f

        // Delay lengths scaling according to roomSize [0.7f..1.3f] and sample rate
        val roomScale = 0.7f + (params.roomSize * 0.6f)
        val delayScale = rateRatio * roomScale

        // Feedback gain: strictly clamped to [0.70f..0.975f] to prevent runaway oscillation
        val targetFeedback = (0.70f + (params.decay * 0.275f)).coerceIn(0.70f, 0.975f)

        // Pre-delay sample length (up to 100 ms)
        val preDelaySamples = (params.preDelay * 0.10f * safeRate.toFloat()).roundToInt()
            .coerceIn(0, PRE_DELAY_BUFFER_SIZE - 1)

        // Damping factor inside comb filter feedback loop
        val damping = (params.damping * 0.4f).coerceIn(0.0f, 0.4f)
        val dampingInv = 1.0f - damping

        // Dynamic delay lengths for comb filters
        val combLenL0 = ((COMB_DELAYS_L[0] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenL1 = ((COMB_DELAYS_L[1] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenL2 = ((COMB_DELAYS_L[2] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenL3 = ((COMB_DELAYS_L[3] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)

        val combLenR0 = ((COMB_DELAYS_R[0] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenR1 = ((COMB_DELAYS_R[1] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenR2 = ((COMB_DELAYS_R[2] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)
        val combLenR3 = ((COMB_DELAYS_R[3] * delayScale).roundToInt()).coerceIn(16, COMB_BUFFER_SIZE - 1)

        // Dynamic delay lengths for all-pass filters
        val apfLenL0 = ((APF_DELAYS_L[0] * rateRatio).roundToInt()).coerceIn(8, APF_BUFFER_SIZE - 1)
        val apfLenL1 = ((APF_DELAYS_L[1] * rateRatio).roundToInt()).coerceIn(8, APF_BUFFER_SIZE - 1)

        val apfLenR0 = ((APF_DELAYS_R[0] * rateRatio).roundToInt()).coerceIn(8, APF_BUFFER_SIZE - 1)
        val apfLenR1 = ((APF_DELAYS_R[1] * rateRatio).roundToInt()).coerceIn(8, APF_BUFFER_SIZE - 1)

        if (!isInitialized) {
            currentWetLevel = targetWet
            currentDryLevel = targetDry
            currentFeedbackGain = targetFeedback
            isInitialized = true
        }

        val invFrames = 1.0f / frameCount.coerceAtLeast(1).toFloat()
        val stepWet = (targetWet - currentWetLevel) * invFrames
        val stepDry = (targetDry - currentDryLevel) * invFrames
        val stepFeedback = (targetFeedback - currentFeedbackGain) * invFrames

        var wetLevel = currentWetLevel
        var dryLevel = currentDryLevel
        var feedbackGain = currentFeedbackGain

        var sampleIndex = 0

        if (channelCount == 1) {
            // Mono Processing
            for (frame in 0 until frameCount) {
                wetLevel += stepWet
                dryLevel += stepDry
                feedbackGain += stepFeedback

                val input = inputSamples[sampleIndex]

                // Pre-Delay
                val reverbInput = if (preDelaySamples > 0) {
                    val readIdx = (preDelayIndexL - preDelaySamples + PRE_DELAY_BUFFER_SIZE) % PRE_DELAY_BUFFER_SIZE
                    val delayed = preDelayBufferL[readIdx]
                    preDelayBufferL[preDelayIndexL] = input
                    if (++preDelayIndexL >= PRE_DELAY_BUFFER_SIZE) preDelayIndexL = 0
                    delayed
                } else {
                    input
                }

                // Comb Filter 0
                val outC0 = combBuffersL[0][combIndicesL[0]]
                var d0 = outC0 * dampingInv + combDampStatesL[0] * damping
                if (abs(d0) < DENORMAL_THRESHOLD) d0 = 0.0f
                combDampStatesL[0] = d0
                combBuffersL[0][combIndicesL[0]] = reverbInput + d0 * feedbackGain
                if (++combIndicesL[0] >= combLenL0) combIndicesL[0] = 0

                // Comb Filter 1
                val outC1 = combBuffersL[1][combIndicesL[1]]
                var d1 = outC1 * dampingInv + combDampStatesL[1] * damping
                if (abs(d1) < DENORMAL_THRESHOLD) d1 = 0.0f
                combDampStatesL[1] = d1
                combBuffersL[1][combIndicesL[1]] = reverbInput + d1 * feedbackGain
                if (++combIndicesL[1] >= combLenL1) combIndicesL[1] = 0

                // Comb Filter 2
                val outC2 = combBuffersL[2][combIndicesL[2]]
                var d2 = outC2 * dampingInv + combDampStatesL[2] * damping
                if (abs(d2) < DENORMAL_THRESHOLD) d2 = 0.0f
                combDampStatesL[2] = d2
                combBuffersL[2][combIndicesL[2]] = reverbInput + d2 * feedbackGain
                if (++combIndicesL[2] >= combLenL2) combIndicesL[2] = 0

                // Comb Filter 3
                val outC3 = combBuffersL[3][combIndicesL[3]]
                var d3 = outC3 * dampingInv + combDampStatesL[3] * damping
                if (abs(d3) < DENORMAL_THRESHOLD) d3 = 0.0f
                combDampStatesL[3] = d3
                combBuffersL[3][combIndicesL[3]] = reverbInput + d3 * feedbackGain
                if (++combIndicesL[3] >= combLenL3) combIndicesL[3] = 0

                var wet = (outC0 + outC1 + outC2 + outC3) * 0.25f

                // All-Pass Filter 0
                val apfOut0 = apfBuffersL[0][apfIndicesL[0]]
                apfBuffersL[0][apfIndicesL[0]] = wet + apfOut0 * APF_GAIN
                wet = -wet * APF_GAIN + apfOut0
                if (++apfIndicesL[0] >= apfLenL0) apfIndicesL[0] = 0

                // All-Pass Filter 1
                val apfOut1 = apfBuffersL[1][apfIndicesL[1]]
                apfBuffersL[1][apfIndicesL[1]] = wet + apfOut1 * APF_GAIN
                wet = -wet * APF_GAIN + apfOut1
                if (++apfIndicesL[1] >= apfLenL1) apfIndicesL[1] = 0

                outputSamples[sampleIndex] = (input * dryLevel + wet * wetLevel).coerceIn(-1.0f, 1.0f)
                sampleIndex++
            }
        } else {
            // Stereo Processing
            for (frame in 0 until frameCount) {
                wetLevel += stepWet
                dryLevel += stepDry
                feedbackGain += stepFeedback

                val inputL = inputSamples[sampleIndex]
                val inputR = inputSamples[sampleIndex + 1]

                // Pre-Delay
                val reverbInL = if (preDelaySamples > 0) {
                    val readIdx = (preDelayIndexL - preDelaySamples + PRE_DELAY_BUFFER_SIZE) % PRE_DELAY_BUFFER_SIZE
                    val delayed = preDelayBufferL[readIdx]
                    preDelayBufferL[preDelayIndexL] = inputL
                    if (++preDelayIndexL >= PRE_DELAY_BUFFER_SIZE) preDelayIndexL = 0
                    delayed
                } else {
                    inputL
                }

                val reverbInR = if (preDelaySamples > 0) {
                    val readIdx = (preDelayIndexR - preDelaySamples + PRE_DELAY_BUFFER_SIZE) % PRE_DELAY_BUFFER_SIZE
                    val delayed = preDelayBufferR[readIdx]
                    preDelayBufferR[preDelayIndexR] = inputR
                    if (++preDelayIndexR >= PRE_DELAY_BUFFER_SIZE) preDelayIndexR = 0
                    delayed
                } else {
                    inputR
                }

                // LEFT CHANNEL COMBS
                val outL0 = combBuffersL[0][combIndicesL[0]]
                var dL0 = outL0 * dampingInv + combDampStatesL[0] * damping
                if (abs(dL0) < DENORMAL_THRESHOLD) dL0 = 0.0f
                combDampStatesL[0] = dL0
                combBuffersL[0][combIndicesL[0]] = reverbInL + dL0 * feedbackGain
                if (++combIndicesL[0] >= combLenL0) combIndicesL[0] = 0

                val outL1 = combBuffersL[1][combIndicesL[1]]
                var dL1 = outL1 * dampingInv + combDampStatesL[1] * damping
                if (abs(dL1) < DENORMAL_THRESHOLD) dL1 = 0.0f
                combDampStatesL[1] = dL1
                combBuffersL[1][combIndicesL[1]] = reverbInL + dL1 * feedbackGain
                if (++combIndicesL[1] >= combLenL1) combIndicesL[1] = 0

                val outL2 = combBuffersL[2][combIndicesL[2]]
                var dL2 = outL2 * dampingInv + combDampStatesL[2] * damping
                if (abs(dL2) < DENORMAL_THRESHOLD) dL2 = 0.0f
                combDampStatesL[2] = dL2
                combBuffersL[2][combIndicesL[2]] = reverbInL + dL2 * feedbackGain
                if (++combIndicesL[2] >= combLenL2) combIndicesL[2] = 0

                val outL3 = combBuffersL[3][combIndicesL[3]]
                var dL3 = outL3 * dampingInv + combDampStatesL[3] * damping
                if (abs(dL3) < DENORMAL_THRESHOLD) dL3 = 0.0f
                combDampStatesL[3] = dL3
                combBuffersL[3][combIndicesL[3]] = reverbInL + dL3 * feedbackGain
                if (++combIndicesL[3] >= combLenL3) combIndicesL[3] = 0

                var wetL = (outL0 + outL1 + outL2 + outL3) * 0.25f

                // RIGHT CHANNEL COMBS
                val outR0 = combBuffersR[0][combIndicesR[0]]
                var dR0 = outR0 * dampingInv + combDampStatesR[0] * damping
                if (abs(dR0) < DENORMAL_THRESHOLD) dR0 = 0.0f
                combDampStatesR[0] = dR0
                combBuffersR[0][combIndicesR[0]] = reverbInR + dR0 * feedbackGain
                if (++combIndicesR[0] >= combLenR0) combIndicesR[0] = 0

                val outR1 = combBuffersR[1][combIndicesR[1]]
                var dR1 = outR1 * dampingInv + combDampStatesR[1] * damping
                if (abs(dR1) < DENORMAL_THRESHOLD) dR1 = 0.0f
                combDampStatesR[1] = dR1
                combBuffersR[1][combIndicesR[1]] = reverbInR + dR1 * feedbackGain
                if (++combIndicesR[1] >= combLenR1) combIndicesR[1] = 0

                val outR2 = combBuffersR[2][combIndicesR[2]]
                var dR2 = outR2 * dampingInv + combDampStatesR[2] * damping
                if (abs(dR2) < DENORMAL_THRESHOLD) dR2 = 0.0f
                combDampStatesR[2] = dR2
                combBuffersR[2][combIndicesR[2]] = reverbInR + dR2 * feedbackGain
                if (++combIndicesR[2] >= combLenR2) combIndicesR[2] = 0

                val outR3 = combBuffersR[3][combIndicesR[3]]
                var dR3 = outR3 * dampingInv + combDampStatesR[3] * damping
                if (abs(dR3) < DENORMAL_THRESHOLD) dR3 = 0.0f
                combDampStatesR[3] = dR3
                combBuffersR[3][combIndicesR[3]] = reverbInR + dR3 * feedbackGain
                if (++combIndicesR[3] >= combLenR3) combIndicesR[3] = 0

                var wetR = (outR0 + outR1 + outR2 + outR3) * 0.25f

                // LEFT ALL-PASS DIFFUSERS
                val apfOutL0 = apfBuffersL[0][apfIndicesL[0]]
                apfBuffersL[0][apfIndicesL[0]] = wetL + apfOutL0 * APF_GAIN
                wetL = -wetL * APF_GAIN + apfOutL0
                if (++apfIndicesL[0] >= apfLenL0) apfIndicesL[0] = 0

                val apfOutL1 = apfBuffersL[1][apfIndicesL[1]]
                apfBuffersL[1][apfIndicesL[1]] = wetL + apfOutL1 * APF_GAIN
                wetL = -wetL * APF_GAIN + apfOutL1
                if (++apfIndicesL[1] >= apfLenL1) apfIndicesL[1] = 0

                // RIGHT ALL-PASS DIFFUSERS
                val apfOutR0 = apfBuffersR[0][apfIndicesR[0]]
                apfBuffersR[0][apfIndicesR[0]] = wetR + apfOutR0 * APF_GAIN
                wetR = -wetR * APF_GAIN + apfOutR0
                if (++apfIndicesR[0] >= apfLenR0) apfIndicesR[0] = 0

                val apfOutR1 = apfBuffersR[1][apfIndicesR[1]]
                apfBuffersR[1][apfIndicesR[1]] = wetR + apfOutR1 * APF_GAIN
                wetR = -wetR * APF_GAIN + apfOutR1
                if (++apfIndicesR[1] >= apfLenR1) apfIndicesR[1] = 0

                outputSamples[sampleIndex] = (inputL * dryLevel + wetL * wetLevel).coerceIn(-1.0f, 1.0f)
                outputSamples[sampleIndex + 1] = (inputR * dryLevel + wetR * wetLevel).coerceIn(-1.0f, 1.0f)

                sampleIndex += 2
            }
        }

        currentWetLevel = targetWet
        currentDryLevel = targetDry
        currentFeedbackGain = targetFeedback
    }
}
