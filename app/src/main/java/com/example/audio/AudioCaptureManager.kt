package com.example.audio

import android.Manifest
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

enum class CaptureMode {
    STOPPED,
    WAKE_WORD_ONLY,
    LIVE_STREAMING
}

class AudioCaptureManager(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onLivePcmChunk: (ByteArray) -> Unit,
    private val onRmsLevel: (Float) -> Unit,
    private val onUserInterrupted: () -> Unit,
    private val onPermissionRequired: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val TAG = "AudioCaptureManager"
        const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 100ms chunks = 1600 samples = 3200 bytes
        const val SAMPLES_PER_CHUNK = 1600
        const val BYTES_PER_CHUNK = SAMPLES_PER_CHUNK * 2
    }

    private var currentMode = CaptureMode.STOPPED
    private val isRunning = AtomicBoolean(false)
    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    var isSpeakingActive = false

    fun setMode(mode: CaptureMode, scope: CoroutineScope) {
        Log.d(TAG, "setMode called with $mode (currently $currentMode, isRunning=${isRunning.get()})")
        currentMode = mode

        if (mode == CaptureMode.STOPPED) {
            stop()
            return
        }

        // Check RECORD_AUDIO runtime permission before touching AudioRecord
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Cannot start audio capture: RECORD_AUDIO permission not granted")
            onPermissionRequired()
            stop()
            return
        }

        // If already running, mode switch happens instantly in the active loop!
        if (isRunning.get() && audioRecord != null) {
            Log.d(TAG, "Audio capture loop already active. Switched mode to $mode")
            return
        }

        startCaptureLoop(scope)
    }

    private fun startCaptureLoop(scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) return

        job = scope.launch(Dispatchers.IO) {
            val record = initAudioRecordSafely()
            if (record == null) {
                Log.w(TAG, "AudioRecord could not be initialized")
                isRunning.set(false)
                onError("Microphone hardware or audio track not available")
                return@launch
            }

            audioRecord = record

            try {
                record.startRecording()
                Log.i(TAG, "AudioRecord started recording successfully in mode $currentMode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to startRecording()", e)
                isRunning.set(false)
                stopInternal()
                onError("Failed to start audio recording: ${e.localizedMessage}")
                return@launch
            }

            val byteBuffer = ByteArray(BYTES_PER_CHUNK)
            val shortBuffer = ShortArray(SAMPLES_PER_CHUNK)

            var consecutiveLoudFrames = 0
            var syllable1DetectedAt = 0L
            var cooldownUntil = 0L

            while (isActive && isRunning.get()) {
                var totalRead = 0
                while (totalRead < BYTES_PER_CHUNK && isActive && isRunning.get()) {
                    val read = record.read(byteBuffer, totalRead, BYTES_PER_CHUNK - totalRead)
                    if (read <= 0) {
                        if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                            Log.w(TAG, "AudioRecord read returned error code: $read")
                        }
                        break
                    }
                    totalRead += read
                }

                if (totalRead != BYTES_PER_CHUNK) continue

                // Extract short samples for acoustic analysis
                ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)

                // 1. Calculate RMS energy & Zero Crossing Rate
                var sumSquares = 0.0
                var energy = 0L
                var zeroCrossings = 0
                var peak = 0

                for (i in 0 until SAMPLES_PER_CHUNK) {
                    val sampleVal = shortBuffer[i].toInt()
                    val absVal = abs(sampleVal)
                    sumSquares += (sampleVal * sampleVal).toDouble()
                    energy += absVal
                    if (absVal > peak) peak = absVal

                    if (i > 0 && ((shortBuffer[i] > 0 && shortBuffer[i - 1] <= 0) || (shortBuffer[i] < 0 && shortBuffer[i - 1] >= 0))) {
                        zeroCrossings++
                    }
                }

                val rms = sqrt(sumSquares / SAMPLES_PER_CHUNK)
                val normalizedRms = (rms / 8000.0).toFloat().coerceIn(0f, 1f)
                val avgEnergy = energy / SAMPLES_PER_CHUNK
                val zcr = zeroCrossings.toFloat() / SAMPLES_PER_CHUNK

                // 2. Dispatch based on current mode
                when (currentMode) {
                    CaptureMode.LIVE_STREAMING -> {
                        onRmsLevel(normalizedRms)

                        // Voice Activity / Barge-in detection during model speech
                        if (isSpeakingActive) {
                            if (normalizedRms > 0.18f || peak > 4500) {
                                consecutiveLoudFrames++
                                if (consecutiveLoudFrames >= 2) {
                                    Log.i(TAG, "User speech detected during playback -> Interruption triggered!")
                                    onUserInterrupted()
                                    consecutiveLoudFrames = 0
                                }
                            } else {
                                consecutiveLoudFrames = 0
                            }
                        }

                        // Send audio chunk to Gemini Live WebSocket
                        onLivePcmChunk(byteBuffer.copyOf())
                    }

                    CaptureMode.WAKE_WORD_ONLY -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime < cooldownUntil) continue

                        // Phonetic acoustic profile for "Zoya":
                        // Syllable 1 ("Zo"): fricative 'Z' with higher ZCR (> 0.18) and energy > 1800
                        if (avgEnergy > 1800 && zcr > 0.18 && syllable1DetectedAt == 0L) {
                            syllable1DetectedAt = currentTime
                        } else if (syllable1DetectedAt > 0L) {
                            val elapsed = currentTime - syllable1DetectedAt
                            // Syllable 2 ("ya"): follows within 200ms - 850ms with voiced vowel energy and lower ZCR (< 0.16)
                            if (elapsed in 200..850) {
                                if (avgEnergy > 2200 && zcr < 0.16) {
                                    Log.i(TAG, "Wake word 'Zoya' detected! Elapsed: ${elapsed}ms")
                                    syllable1DetectedAt = 0L
                                    cooldownUntil = currentTime + 2000L
                                    onWakeWordDetected()
                                }
                            } else if (elapsed > 850) {
                                syllable1DetectedAt = 0L
                            }
                        }
                    }

                    CaptureMode.STOPPED -> {
                        break
                    }
                }
            }

            stopInternal()
        }
    }

    private fun initAudioRecordSafely(): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = if (minBufferSize > 0) {
            maxOf(minBufferSize * 2, BYTES_PER_CHUNK * 4)
        } else {
            BYTES_PER_CHUNK * 4
        }

        // Try standard AudioSource.MIC first (universally supported across physical devices and emulators)
        val audioSources = listOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )

        for (source in audioSources) {
            try {
                val record = AudioRecord(
                    source,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (record.state == AudioRecord.STATE_INITIALIZED) {
                    Log.d(TAG, "AudioRecord initialized successfully with audio source: $source")
                    return record
                } else {
                    record.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not create AudioRecord with source $source: ${e.message}")
            }
        }

        return null
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
                    try {
                        it.stop()
                    } catch (_: Exception) {}
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioRecord = null
    }
}
