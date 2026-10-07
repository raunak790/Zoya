package com.example.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Standalone AudioRecordStreamer with safe fallback and permission guardrails.
 */
class AudioRecordStreamer(
    private val onPcmChunk: (ByteArray) -> Unit,
    private val onRmsLevel: (Float) -> Unit,
    private val onUserInterrupted: () -> Unit,
    private val context: Context? = null
) {
    private val isRecording = AtomicBoolean(false)
    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    var isSpeakingActive = false

    companion object {
        private const val TAG = "AudioRecordStreamer"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val SAMPLES_PER_CHUNK = 1600
        const val BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * 2
    }

    @SuppressLint("MissingPermission")
    fun startStreaming(scope: CoroutineScope) {
        if (isRecording.getAndSet(true)) return

        if (context != null) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.w(TAG, "Cannot start AudioRecord: RECORD_AUDIO permission not granted")
                isRecording.set(false)
                return
            }
        }

        job = scope.launch(Dispatchers.IO) {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = if (minBufferSize > 0) {
                maxOf(minBufferSize * 2, BYTES_PER_CHUNK * 4)
            } else {
                BYTES_PER_CHUNK * 4
            }

            try {
                // Try standard AudioSource.MIC first (best supported across emulators and hardware)
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.release()
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize
                    )
                }

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "AudioRecord could not be initialized on current hardware/emulator")
                    isRecording.set(false)
                    stopInternal()
                    return@launch
                }

                audioRecord?.startRecording()
                Log.d(TAG, "AudioRecordStreamer started successfully")

                val byteBuffer = ByteArray(BYTES_PER_CHUNK)
                val shortBuffer = ShortArray(SAMPLES_PER_CHUNK)

                var consecutiveLoudFrames = 0

                while (isActive && isRecording.get()) {
                    var totalRead = 0
                    while (totalRead < BYTES_PER_CHUNK && isActive && isRecording.get()) {
                        val read = audioRecord?.read(byteBuffer, totalRead, BYTES_PER_CHUNK - totalRead) ?: -1
                        if (read <= 0) break
                        totalRead += read
                    }

                    if (totalRead == BYTES_PER_CHUNK) {
                        ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

                        var sumSquares = 0.0
                        var peak = 0
                        for (sample in shortBuffer) {
                            val sampleVal = sample.toInt()
                            sumSquares += (sampleVal * sampleVal).toDouble()
                            val absVal = abs(sampleVal)
                            if (absVal > peak) peak = absVal
                        }
                        val rms = sqrt(sumSquares / SAMPLES_PER_CHUNK)
                        val normalizedRms = (rms / 8000.0).toFloat().coerceIn(0f, 1f)

                        onRmsLevel(normalizedRms)

                        if (isSpeakingActive) {
                            if (normalizedRms > 0.18f || peak > 4500) {
                                consecutiveLoudFrames++
                                if (consecutiveLoudFrames >= 2) {
                                    Log.i(TAG, "User speech detected during model output -> Interruption triggered!")
                                    onUserInterrupted()
                                    consecutiveLoudFrames = 0
                                }
                            } else {
                                consecutiveLoudFrames = 0
                            }
                        }

                        onPcmChunk(byteBuffer.copyOf())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception in AudioRecordStreamer: ${e.message}")
            } finally {
                stopInternal()
            }
        }
    }

    fun stopStreaming() {
        isRecording.set(false)
        job?.cancel()
        job = null
        stopInternal()
    }

    private fun stopInternal() {
        try {
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    try { it.stop() } catch (_: Exception) {}
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioRecord = null
    }
}
