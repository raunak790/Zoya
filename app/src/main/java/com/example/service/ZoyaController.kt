package com.example.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.audio.AudioCaptureManager
import com.example.audio.CaptureMode
import com.example.live.LiveSessionManager
import com.example.model.AssistantState
import com.example.model.ZoyaUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object ZoyaController {
    private const val TAG = "ZoyaController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ZoyaUiState())
    val uiState: StateFlow<ZoyaUiState> = _uiState.asStateFlow()

    private var liveSessionManager: LiveSessionManager? = null
    private var audioCaptureManager: AudioCaptureManager? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"
        _uiState.update { it.copy(apiKeyConfigured = hasKey) }

        val appContext = context.applicationContext

        val hasMicPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(permissionsGranted = hasMicPermission) }

        // Initialize single centralized audio capture
        audioCaptureManager = AudioCaptureManager(
            context = appContext,
            onWakeWordDetected = {
                Log.i(TAG, "Wake word trigger received in ZoyaController!")
                awakenZoya(appContext)
            },
            onLivePcmChunk = { pcmBytes ->
                liveSessionManager?.sendRealtimeAudioChunk(pcmBytes)
            },
            onRmsLevel = { rms ->
                _uiState.update { it.copy(audioRms = rms) }
            },
            onUserInterrupted = {
                liveSessionManager?.handleUserInterruption()
            },
            onPermissionRequired = {
                _uiState.update {
                    it.copy(
                        permissionsGranted = false,
                        currentSubtitle = "Hold on handsome! I need microphone permission before we can talk. Tap to grant."
                    )
                }
            },
            onError = { errMsg ->
                Log.w(TAG, "Audio capture error: $errMsg")
            }
        )

        // Initialize live session manager with callback to pause/resume speaking state in audio capture
        liveSessionManager = LiveSessionManager(
            context = appContext,
            scope = scope,
            apiKey = apiKey,
            onSpeakingStateChanged = { isSpeaking ->
                audioCaptureManager?.isSpeakingActive = isSpeaking
            }
        )

        // Start wake-word detection ONLY if permission is granted
        if (hasMicPermission && _uiState.value.isWakeWordActive) {
            audioCaptureManager?.setMode(CaptureMode.WAKE_WORD_ONLY, scope)
        }

        // Collect state from LiveSessionManager
        scope.launch {
            liveSessionManager?.assistantState?.collect { state ->
                _uiState.update { it.copy(state = state) }
            }
        }

        scope.launch {
            liveSessionManager?.currentSubtitle?.collect { subtitle ->
                _uiState.update { it.copy(currentSubtitle = subtitle) }
            }
        }

        scope.launch {
            liveSessionManager?.outputAmplitude?.collect { amp ->
                _uiState.update { it.copy(outputAmplitude = amp) }
            }
        }

        scope.launch {
            liveSessionManager?.lastAction?.collect { action ->
                _uiState.update { current ->
                    val updated = (listOf(action) + current.actionHistory).take(15)
                    current.copy(
                        lastToolAction = action.description,
                        actionHistory = updated
                    )
                }
            }
        }
    }

    fun awakenZoya(context: Context) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _uiState.update {
                it.copy(
                    permissionsGranted = false,
                    currentSubtitle = "Hold on handsome! I need microphone permission before we can talk. Tap to grant."
                )
            }
            return
        }

        Log.i(TAG, "Awakening Zoya Live Session!")
        _uiState.update {
            it.copy(
                state = AssistantState.PROCESSING,
                currentSubtitle = "Hey! I'm here. What's on your mind?"
            )
        }

        audioCaptureManager?.setMode(CaptureMode.LIVE_STREAMING, scope)
        liveSessionManager?.startSession()
    }

    fun stopSession() {
        liveSessionManager?.stopSession()
        if (_uiState.value.isWakeWordActive && _uiState.value.permissionsGranted) {
            audioCaptureManager?.setMode(CaptureMode.WAKE_WORD_ONLY, scope)
        } else {
            audioCaptureManager?.setMode(CaptureMode.STOPPED, scope)
        }
        _uiState.update { it.copy(audioRms = 0f) }
    }

    fun toggleSession(context: Context) {
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            _uiState.update {
                it.copy(
                    permissionsGranted = false,
                    currentSubtitle = "Hold on handsome! I need microphone permission before we can talk. Tap to grant."
                )
            }
            return
        }

        if (_uiState.value.state != AssistantState.IDLE && _uiState.value.state != AssistantState.ERROR) {
            stopSession()
        } else {
            awakenZoya(context)
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isWakeWordActive = enabled) }
        val hasMicPermission = _uiState.value.permissionsGranted
        if (enabled && hasMicPermission && _uiState.value.state == AssistantState.IDLE) {
            audioCaptureManager?.setMode(CaptureMode.WAKE_WORD_ONLY, scope)
        } else if (!enabled && _uiState.value.state == AssistantState.IDLE) {
            audioCaptureManager?.setMode(CaptureMode.STOPPED, scope)
        }
    }

    fun startForegroundService(context: Context) {
        val intent = Intent(context, ZoyaVoiceService::class.java).apply {
            action = ZoyaVoiceService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.update { it.copy(isForegroundServiceRunning = true) }

        if (_uiState.value.permissionsGranted && _uiState.value.state == AssistantState.IDLE) {
            audioCaptureManager?.setMode(CaptureMode.WAKE_WORD_ONLY, scope)
        }
    }

    fun stopForegroundService(context: Context) {
        val intent = Intent(context, ZoyaVoiceService::class.java).apply {
            action = ZoyaVoiceService.ACTION_STOP_SERVICE
        }
        context.startService(intent)
        _uiState.update { it.copy(isForegroundServiceRunning = false) }
    }

    fun setPermissionsGranted(granted: Boolean) {
        _uiState.update { it.copy(permissionsGranted = granted) }
        if (granted && _uiState.value.isWakeWordActive && _uiState.value.state == AssistantState.IDLE) {
            audioCaptureManager?.setMode(CaptureMode.WAKE_WORD_ONLY, scope)
        }
    }
}
