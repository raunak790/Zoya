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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Standalone WakeWordDetector with safe fallback and permission guardrails.
 */
class WakeWordDetector(
    private val context: Context? = null,
    private val onWakeWordDetected: () -> Unit
) {
    private val isRunning = AtomicBoolean(false)
    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    companion object {
        private const val TAG = "WakeWordDetector"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_SIZE = 512
    }

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        if (context != null) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                Log.w(TAG, "Cannot start WakeWordDetector: RECORD_AUDIO permission not granted")
                isRunning.set(false)
                return
            }
        }

        job = scope.launch(Dispatchers.IO) {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = if (minBufferSize > 0) {
                maxOf(minBufferSize * 2, FRAME_SIZE * 4)
            } else {
                FRAME_SIZE * 4
            }

            try {
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
                    Log.w(TAG, "AudioRecord could not be initialized for WakeWordDetector")
                    isRunning.set(false)
                    stopInternal()
                    return@launch
                }

                audioRecord?.startRecording()
                Log.d(TAG, "WakeWordDetector started listening for 'Zoya'...")

                val buffer = ShortArray(FRAME_SIZE)
                var syllable1DetectedAt: Long = 0
                var cooldownUntil: Long = 0

                while (isActive && isRunning.get()) {
                    val read = audioRecord?.read(buffer, 0, FRAME_SIZE) ?: 0
                    if (read <= 0) continue

                    val currentTime = System.currentTimeMillis()
                    if (currentTime < cooldownUntil) continue

                    var energy = 0L
                    var zeroCrossings = 0
                    for (i in 0 until read) {
                        val sample = buffer[i].toInt()
                        energy += abs(sample)
                        if (i > 0 && ((buffer[i] > 0 && buffer[i - 1] <= 0) || (buffer[i] < 0 && buffer[i - 1] >= 0))) {
                            zeroCrossings++
                        }
                    }
                    val avgEnergy = energy / read
                    val zcr = zeroCrossings.toFloat() / read

                    if (avgEnergy > 1800 && zcr > 0.18 && syllable1DetectedAt == 0L) {
                        syllable1DetectedAt = currentTime
                    } else if (syllable1DetectedAt > 0L) {
                        val elapsed = currentTime - syllable1DetectedAt
                        if (elapsed in 200..850) {
                            if (avgEnergy > 2200 && zcr < 0.16) {
                                Log.i(TAG, "Wake word 'Zoya' detected! (elapsed: ${elapsed}ms)")
                                syllable1DetectedAt = 0L
                                cooldownUntil = currentTime + 2000L
                                onWakeWordDetected()
                            }
                        } else if (elapsed > 850) {
                            syllable1DetectedAt = 0L
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Exception in WakeWordDetector: ${e.message}")
            } finally {
                stopInternal()
            }
        }
    }

    fun stop() {
        isRunning.set(false)
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

    fun triggerWakeManual() {
        onWakeWordDetected()
    }
}
