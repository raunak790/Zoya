package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ZoyaUiState
import com.example.service.ZoyaController
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class ZoyaViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<ZoyaUiState> = ZoyaController.uiState

    init {
        ZoyaController.initialize(application)
    }

    fun onOrbClicked(context: Context) {
        ZoyaController.toggleSession(context)
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        ZoyaController.setWakeWordEnabled(enabled)
    }

    fun toggleForegroundService(context: Context) {
        val current = uiState.value.isForegroundServiceRunning
        if (current) {
            ZoyaController.stopForegroundService(context)
        } else {
            ZoyaController.startForegroundService(context)
        }
    }

    fun onPermissionsUpdated(granted: Boolean) {
        ZoyaController.setPermissionsGranted(granted)
    }

    fun executeQuickTestTool(toolName: String, context: Context) {
        viewModelScope.launch {
            val engine = ToolExecutionEngine(context)
            when (toolName) {
                "openYouTube" -> {
                    engine.executeTool("openApp", JSONObject().put("packageName", "youtube"))
                }
                "openCalculator" -> {
                    engine.executeTool("openApp", JSONObject().put("packageName", "calculator"))
                }
                "callContact" -> {
                    engine.executeTool("searchAndCallContact", JSONObject().put("contactName", "Mom"))
                }
                "whatsappMsg" -> {
                    engine.executeTool(
                        "sendWhatsAppMessage",
                        JSONObject().put("contactName", "Friend").put("message", "Hey from Zoya Assistant!")
                    )
                }
                "sendGmail" -> {
                    engine.executeTool(
                        "sendGmail",
                        JSONObject().put("recipientEmail", "test@example.com")
                            .put("subject", "Hello from Zoya")
                            .put("body", "Hey there! Zoya is doing her magic.")
                    )
                }
            }
        }
    }
}
