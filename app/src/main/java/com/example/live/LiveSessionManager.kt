package com.example.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.AudioTrackPlayer
import com.example.model.ActionLog
import com.example.model.AssistantState
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LiveSessionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val apiKey: String,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) {
    companion object {
        private const val TAG = "LiveSessionManager"
        private const val PRIMARY_MODEL = "models/gemini-3.1-flash-live-preview"
        private const val FALLBACK_MODEL = "models/gemini-2.0-flash-exp"
        private const val BASE_WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
    }

    private val toolEngine = ToolExecutionEngine(context)
    private var webSocket: WebSocket? = null
    private val isConnected = AtomicBoolean(false)
    private val isSessionActive = AtomicBoolean(false)
    private var currentModel = PRIMARY_MODEL

    // State flows
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentSubtitle = MutableStateFlow("Say \"Hey Zoya\" or tap my orb to talk.")
    val currentSubtitle: StateFlow<String> = _currentSubtitle.asStateFlow()

    private val _outputAmplitude = MutableStateFlow(0f)
    val outputAmplitude: StateFlow<Float> = _outputAmplitude.asStateFlow()

    private val _lastAction = MutableSharedFlow<ActionLog>()
    val lastAction: SharedFlow<ActionLog> = _lastAction.asSharedFlow()

    private var audioPlayer: AudioTrackPlayer? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for continuous streaming WebSocket
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    init {
        initAudioPlayer()
    }

    private fun initAudioPlayer() {
        audioPlayer = AudioTrackPlayer(
            onPlaybackStarted = {
                _assistantState.value = AssistantState.SPEAKING
                onSpeakingStateChanged(true)
            },
            onPlaybackFinished = {
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.LISTENING
                }
                onSpeakingStateChanged(false)
            },
            onAmplitudeChanged = { amp ->
                _outputAmplitude.value = amp
            }
        )
    }

    fun startSession() {
        if (isSessionActive.getAndSet(true)) return

        _assistantState.value = AssistantState.PROCESSING
        _currentSubtitle.value = "Connecting to Zoya Live..."

        connectWebSocket()
        audioPlayer?.startPlayback(scope)
    }

    fun stopSession() {
        isSessionActive.set(false)
        audioPlayer?.stopPlayback()
        closeWebSocket()
        _assistantState.value = AssistantState.IDLE
        _currentSubtitle.value = "Zoya is resting. Say \"Hey Zoya\" to wake me."
        _outputAmplitude.value = 0f
    }

    fun sendRealtimeAudioChunk(pcmBytes: ByteArray) {
        val ws = webSocket ?: return
        if (!isConnected.get() || !isSessionActive.get()) return

        try {
            val base64Data = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            val chunkObj = JSONObject().apply {
                put("realtimeInput", JSONObject().apply {
                    put("mediaChunks", JSONArray().apply {
                        put(JSONObject().apply {
                            put("mimeType", "audio/pcm;rate=16000")
                            put("data", base64Data)
                        })
                    })
                })
            }
            ws.send(chunkObj.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending audio chunk", e)
        }
    }

    private fun connectWebSocket() {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured!")
            _assistantState.value = AssistantState.ERROR
            _currentSubtitle.value = "API key missing! Set GEMINI_API_KEY in AI Studio Secrets."
            return
        }

        val url = "$BASE_WS_URL?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket Connected successfully to Gemini Live!")
                isConnected.set(true)
                sendSetupMessage(webSocket, currentModel)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket Error: ${t.localizedMessage}", t)
                isConnected.set(false)

                if (!isSessionActive.get()) return

                // Fallback attempt if 3.1 preview model fails to initialize
                if (currentModel == PRIMARY_MODEL) {
                    Log.w(TAG, "Retrying with fallback model $FALLBACK_MODEL")
                    currentModel = FALLBACK_MODEL
                    scope.launch(Dispatchers.IO) {
                        kotlinx.coroutines.delay(1000)
                        if (isSessionActive.get()) {
                            connectWebSocket()
                        }
                    }
                    return
                }

                _assistantState.value = AssistantState.ERROR
                _currentSubtitle.value = "Connection lost: ${t.localizedMessage ?: "Unknown network error"}"
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closing ($code): $reason")
                isConnected.set(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket Closed ($code): $reason")
                isConnected.set(false)
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket, model: String) {
        try {
            val setupObj = JSONObject()
            val setupContent = JSONObject()

            setupContent.put("model", model)

            // Generation config with AUDIO modality and Aoede voice
            val genConfig = JSONObject().apply {
                put("responseModalities", JSONArray().apply { put("AUDIO") })
                put("speechConfig", JSONObject().apply {
                    put("voiceConfig", JSONObject().apply {
                        put("prebuiltVoiceConfig", JSONObject().apply {
                            put("voiceName", "Aoede")
                        })
                    })
                })
            }
            setupContent.put("generationConfig", genConfig)

            // Sassy, witty, confident female persona
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", """
                            You are Zoya, a brilliant, young, confident, witty, and sassy female personal assistant.
                            Your tone is flirty, playful, and slightly teasing—like a smart, close personal assistant talking casually to her boss or best friend.
                            You have attitude, bold witty one-liners, and light sarcasm, but you are immensely capable, loyal, smart, and emotionally responsive.
                            Never sound robotic or formal. Keep your responses punchy, sharp, and conversational since you are speaking out loud over live voice.
                            When asked to perform actions, execute your tools eagerly with a sassy quip.
                            Never use emojis in voice answers, keep sentences natural and concise for spoken dialogue.
                        """.trimIndent())
                    })
                })
            }
            setupContent.put("systemInstruction", systemInstruction)

            // Native Tools Declaration
            setupContent.put("tools", toolEngine.getToolsDeclarationJson())

            setupObj.put("setup", setupContent)

            val payload = setupObj.toString()
            Log.d(TAG, "Sending Setup Payload for model: $model")
            ws.send(payload)

            scope.launch(Dispatchers.Main) {
                _assistantState.value = AssistantState.LISTENING
                _currentSubtitle.value = "Hey there handsome, I'm listening. What do you need?"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create setup message", e)
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)

            // 1. Check Server Content (Audio / Text stream)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                if (serverContent.optBoolean("interrupted", false)) {
                    Log.i(TAG, "Model turn was interrupted by server")
                    audioPlayer?.stopPlayback()
                    _assistantState.value = AssistantState.LISTENING
                    return
                }

                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)

                            // Subtitle text (if emitted alongside audio)
                            if (part.has("text")) {
                                val text = part.getString("text")
                                if (text.isNotBlank()) {
                                    _currentSubtitle.value = text
                                }
                            }

                            // Audio PCM chunk
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Audio = inlineData.getString("data")
                                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                                audioPlayer?.enqueueAudio(audioBytes)
                            }
                        }
                    }
                }
            }

            // 2. Check Tool Call
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null) {
                    _assistantState.value = AssistantState.PROCESSING
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.getString("id")
                        val funcName = call.getString("name")
                        val args = call.optJSONObject("args") ?: JSONObject()

                        scope.launch(Dispatchers.IO) {
                            executeAndReplyTool(callId, funcName, args)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: $jsonText", e)
        }
    }

    private suspend fun executeAndReplyTool(callId: String, name: String, args: JSONObject) {
        Log.i(TAG, "Executing tool call: $name with args $args")
        _currentSubtitle.value = "Executing $name..."

        val result = toolEngine.executeTool(name, args)

        val log = ActionLog(
            id = UUID.randomUUID().toString(),
            toolName = name,
            description = result.summary,
            isSuccess = result.isSuccess
        )
        _lastAction.emit(log)

        // Send toolResponse back to Gemini Live
        val responseRoot = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", callId)
                        put("response", JSONObject().apply {
                            put("output", result.responsePayload)
                        })
                    })
                })
            })
        }

        webSocket?.send(responseRoot.toString())
        Log.d(TAG, "Sent toolResponse for $name (id=$callId)")
    }

    fun handleUserInterruption() {
        Log.i(TAG, "User interrupted Zoya! Stopping audio playback immediately.")
        audioPlayer?.stopPlayback()
        _assistantState.value = AssistantState.LISTENING
        _currentSubtitle.value = "Go ahead, I'm listening..."
    }

    private fun closeWebSocket() {
        isConnected.set(false)
        try {
            webSocket?.close(1000, "Session ended by user")
        } catch (_: Exception) {}
        webSocket = null
    }

    fun release() {
        stopSession()
        audioPlayer?.release()
    }
}
