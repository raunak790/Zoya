package com.example.model

enum class AssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class ActionLog(
    val id: String,
    val toolName: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
)

data class ZoyaUiState(
    val state: AssistantState = AssistantState.IDLE,
    val isWakeWordActive: Boolean = true,
    val isForegroundServiceRunning: Boolean = false,
    val isMuted: Boolean = false,
    val currentSubtitle: String = "Say \"Hey Zoya\" or tap my orb to talk to me.",
    val audioRms: Float = 0f,
    val outputAmplitude: Float = 0f,
    val lastToolAction: String? = null,
    val apiKeyConfigured: Boolean = true,
    val permissionsGranted: Boolean = false,
    val actionHistory: List<ActionLog> = emptyList()
)
