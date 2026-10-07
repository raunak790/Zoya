package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

class AudioTrackPlayer(
    private val onPlaybackStarted: () -> Unit,
    private val onPlaybackFinished: () -> Unit,
    private val onAmplitudeChanged: (Float) -> Unit
) {
    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private val isPlaying = AtomicBoolean(false)
    private var playbackJob: Job? = null

    companion object {
        private const val TAG = "AudioTrackPlayer"
        const val OUTPUT_SAMPLE_RATE = 24000 // Gemini Live outputs 24kHz PCM 16-bit mono
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun initTrack() {
        if (audioTrack != null) return

        val minBufferSize = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        val bufferSize = maxOf(minBufferSize * 2, 8192)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(OUTPUT_SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .setEncoding(AUDIO_FORMAT)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            format,
            bufferSize,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    fun startPlayback(scope: CoroutineScope) {
        if (isPlaying.getAndSet(true)) return

        initTrack()
        try {
            audioTrack?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioTrack", e)
        }

        playbackJob = scope.launch(Dispatchers.IO) {
            var activeSession = false

            while (isActive && isPlaying.get()) {
                val chunk = audioQueue.poll(200, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (chunk != null) {
                    if (!activeSession) {
                        activeSession = true
                        onPlaybackStarted()
                    }

                    // Compute amplitude
                    val shortCount = chunk.size / 2
                    val shortBuffer = ShortArray(shortCount)
                    ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

                    var sumSquares = 0.0
                    for (sample in shortBuffer) {
                        val s = sample.toInt()
                        sumSquares += (s * s).toDouble()
                    }
                    val rms = sqrt(sumSquares / maxOf(1, shortCount))
                    val normAmp = (rms / 7500.0).toFloat().coerceIn(0f, 1f)
                    onAmplitudeChanged(normAmp)

                    // Write to AudioTrack
                    audioTrack?.write(chunk, 0, chunk.size)
                } else {
                    if (activeSession && audioQueue.isEmpty()) {
                        activeSession = false
                        onAmplitudeChanged(0f)
                        onPlaybackFinished()
                    }
                }
            }
        }
    }

    fun enqueueAudio(pcmData: ByteArray) {
        if (pcmData.isNotEmpty()) {
            audioQueue.offer(pcmData)
        }
    }

    fun stopPlayback() {
        audioQueue.clear()
        onAmplitudeChanged(0f)
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (_: Exception) {}
        onPlaybackFinished()
    }

    fun release() {
        isPlaying.set(false)
        playbackJob?.cancel()
        playbackJob = null
        audioQueue.clear()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
