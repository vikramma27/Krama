package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import java.util.concurrent.TimeUnit

data class SocketIoPresence(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

data class SocketIoCallSessionNegotiation(
    val callId: String,
    val callerId: String,
    val recipientId: String,
    val action: String, // "INITIATE", "ACCEPT", "REJECT", "RINGING", "BUSY"
    val isVideo: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Socket.io Signaling Client using OkHttp WebSocket under the hood with Socket.io v4 frame parsing
 * (Engine.IO / Socket.IO text protocol: '42["event", data]').
 * Provides peer discovery, real-time presence broadcasting, and session negotiation for WebRTC calls.
 */
class SocketIoSignalingClient private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(SignalingPayload::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow(SignalingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SignalingConnectionState> = _connectionState.asStateFlow()

    private val _incomingSignals = MutableSharedFlow<SignalingPayload>(extraBufferCapacity = 64)
    val incomingSignals: SharedFlow<SignalingPayload> = _incomingSignals.asSharedFlow()

    private val _onlinePeers = MutableStateFlow<Map<String, SocketIoPresence>>(emptyMap())
    val onlinePeers: StateFlow<Map<String, SocketIoPresence>> = _onlinePeers.asStateFlow()

    private val _sessionNegotiations = MutableSharedFlow<SocketIoCallSessionNegotiation>(extraBufferCapacity = 32)
    val sessionNegotiations: SharedFlow<SocketIoCallSessionNegotiation> = _sessionNegotiations.asSharedFlow()

    fun connectSocketIoServer(serverUrl: String = "wss://matrix.krama.sec/socket.io/?EIO=4&transport=websocket", currentUserId: String) {
        if (_connectionState.value == SignalingConnectionState.CONNECTED || _connectionState.value == SignalingConnectionState.CONNECTING) {
            return
        }

        _connectionState.value = SignalingConnectionState.CONNECTING
        Log.i(TAG, "🔌 Connecting Socket.io signaling client for user $currentUserId to $serverUrl")

        val request = Request.Builder()
            .url("$serverUrl&userId=$currentUserId")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // Socket.io handshakes start with Engine.IO open packet '0' or direct connect
                ws.send("40") // Engine.IO message + Socket.IO CONNECT
                _connectionState.value = SignalingConnectionState.CONNECTED
                Log.i(TAG, "✅ Socket.io signaling connected successfully")

                // Broadcast online presence for peer discovery
                publishPresence(currentUserId, true)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    handleSocketIoFrame(text)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error parsing Socket.io message frame ($text): ${e.message}")
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                _connectionState.value = SignalingConnectionState.DISCONNECTED
                Log.w(TAG, "Socket.io connection closing: $reason")
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = SignalingConnectionState.ERROR
                Log.e(TAG, "Socket.io WebSocket failure: ${t.message}")
            }
        })
    }

    private fun handleSocketIoFrame(frame: String) {
        // Socket.IO event frame format: 42["event_name", payload_obj]
        if (frame.startsWith("42")) {
            val jsonArrayStr = frame.substring(2)
            val jsonArray = JSONArray(jsonArrayStr)
            val eventName = jsonArray.optString(0)
            val eventData = jsonArray.opt(1)

            when (eventName) {
                "webrtc_signal" -> {
                    val payloadObj = eventData as? JSONObject ?: return
                    val payload = payloadAdapter.fromJson(payloadObj.toString())
                    if (payload != null) {
                        scope.launch { _incomingSignals.emit(payload) }
                    }
                }
                "peer_discovery" -> {
                    val peerObj = eventData as? JSONObject ?: return
                    val userId = peerObj.optString("userId")
                    val isOnline = peerObj.optBoolean("isOnline", true)
                    if (userId.isNotEmpty()) {
                        val current = _onlinePeers.value.toMutableMap()
                        current[userId] = SocketIoPresence(userId = userId, isOnline = isOnline)
                        _onlinePeers.value = current
                    }
                }
                "session_negotiate" -> {
                    val negObj = eventData as? JSONObject ?: return
                    val neg = SocketIoCallSessionNegotiation(
                        callId = negObj.optString("callId"),
                        callerId = negObj.optString("callerId"),
                        recipientId = negObj.optString("recipientId"),
                        action = negObj.optString("action"),
                        isVideo = negObj.optBoolean("isVideo", false)
                    )
                    scope.launch { _sessionNegotiations.emit(neg) }
                }
                "ping" -> webSocket?.send("3") // Engine.IO pong
            }
        } else if (frame == "2") {
            webSocket?.send("3") // Ping-pong response
        }
    }

    fun emitSignal(payload: SignalingPayload) {
        try {
            val jsonPayload = payloadAdapter.toJson(payload)
            val socketIoPacket = "42[\"webrtc_signal\", $jsonPayload]"
            val sent = webSocket?.send(socketIoPacket) ?: false
            if (sent) {
                Log.d(TAG, "Emitted Socket.io signal: ${payload.type} for call ${payload.callId}")
            } else {
                Log.w(TAG, "Socket.io socket not ready. Signal fallback engaged for ${payload.type}")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error emitting Socket.io signal: ${e.message}")
        }
    }

    fun publishPresence(userId: String, isOnline: Boolean) {
        try {
            val obj = JSONObject().apply {
                put("userId", userId)
                put("isOnline", isOnline)
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send("42[\"peer_discovery\", $obj]")
        } catch (e: Throwable) {}
    }

    fun initiateCallNegotiation(callId: String, callerId: String, recipientId: String, isVideo: Boolean) {
        try {
            val obj = JSONObject().apply {
                put("callId", callId)
                put("callerId", callerId)
                put("recipientId", recipientId)
                put("action", "INITIATE")
                put("isVideo", isVideo)
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send("42[\"session_negotiate\", $obj]")
        } catch (e: Throwable) {}
    }

    fun respondCallNegotiation(callId: String, callerId: String, recipientId: String, action: String) {
        try {
            val obj = JSONObject().apply {
                put("callId", callId)
                put("callerId", callerId)
                put("recipientId", recipientId)
                put("action", action)
                put("timestamp", System.currentTimeMillis())
            }
            webSocket?.send("42[\"session_negotiate\", $obj]")
        } catch (e: Throwable) {}
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        _connectionState.value = SignalingConnectionState.DISCONNECTED
    }

    companion object {
        private const val TAG = "SocketIoSignaling"

        val instance: SocketIoSignalingClient by lazy { SocketIoSignalingClient() }
    }
}
