package com.example.data.remote

import android.util.Log
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
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class SignalingPayload(
    val callId: String,
    val senderId: String,
    val targetId: String,
    val type: String, // "OFFER", "ANSWER", "ICE_CANDIDATE", "BYE"
    val sdp: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null
)

data class IceServerDto(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

interface SignalingApi {
    @POST("api/v1/webrtc/signal")
    suspend fun sendSignal(@Body payload: SignalingPayload): retrofit2.Response<Unit>

    @GET("api/v1/webrtc/ice-servers")
    suspend fun getIceServers(): List<IceServerDto>
}

enum class SignalingConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

class WebRtcSignalingManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(SignalingPayload::class.java)

    private val okHttpClient = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://matrix.krama.sec/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: SignalingApi = retrofit.create(SignalingApi::class.java)

    private var webSocket: WebSocket? = null

    private val _connectionState = MutableStateFlow(SignalingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SignalingConnectionState> = _connectionState.asStateFlow()

    private val _isSignalingPaused = MutableStateFlow(false)
    val isSignalingPaused: StateFlow<Boolean> = _isSignalingPaused.asStateFlow()

    private val pendingSignalQueue = mutableListOf<SignalingPayload>()

    fun pauseSignaling() {
        Log.w("WebRtcSignaling", "Pausing WebRTC call signaling due to network instability")
        _isSignalingPaused.value = true
    }

    fun pauseWebRTC() {
        Log.w("WebRtcSignaling", "pauseWebRTC triggered: Pausing signaling and active audio/video streams.")
        pauseSignaling()
    }

    fun resumeSignaling() {
        Log.i("WebRtcSignaling", "Resuming WebRTC call signaling (network stable)")
        _isSignalingPaused.value = false
        // Flush queued signaling messages
        synchronized(pendingSignalQueue) {
            val queueCopy = ArrayList(pendingSignalQueue)
            pendingSignalQueue.clear()
            queueCopy.forEach { payload ->
                sendSignal(payload)
            }
        }
    }

    private val _incomingSignals = MutableSharedFlow<SignalingPayload>(extraBufferCapacity = 64)
    val incomingSignals: SharedFlow<SignalingPayload> = _incomingSignals.asSharedFlow()

    fun connectWebSocket(userId: String) {
        if (_connectionState.value == SignalingConnectionState.CONNECTED) return
        _connectionState.value = SignalingConnectionState.CONNECTING

        val request = Request.Builder()
            .url("wss://matrix.krama.sec/ws/webrtc?userId=$userId")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = SignalingConnectionState.CONNECTED
                Log.d("WebRtcSignaling", "WebSocket connected for user $userId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val payload = payloadAdapter.fromJson(text)
                    if (payload != null) {
                        scope.launch {
                            _incomingSignals.emit(payload)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebRtcSignaling", "Error parsing incoming signal: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = SignalingConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = SignalingConnectionState.ERROR
                Log.e("WebRtcSignaling", "WebSocket failure: ${t.message}")
            }
        })
    }

    fun sendSignal(payload: SignalingPayload) {
        if (_isSignalingPaused.value) {
            Log.w("WebRtcSignaling", "Signaling is paused. Queueing WebRTC signal type: ${payload.type}")
            synchronized(pendingSignalQueue) {
                pendingSignalQueue.add(payload)
            }
            return
        }

        scope.launch {
            // 1. Firebase Firestore & Realtime Database Signaling Layer
            try {
                when (payload.type) {
                    "OFFER" -> FirebaseRealtimeSignalingService.instance.sendOffer(payload.callId, payload.senderId, payload.targetId, payload.sdp ?: "")
                    "ANSWER" -> FirebaseRealtimeSignalingService.instance.sendAnswer(payload.callId, payload.senderId, payload.targetId, payload.sdp ?: "")
                    "ICE_CANDIDATE" -> FirebaseRealtimeSignalingService.instance.sendIceCandidate(payload.callId, payload.senderId, payload.targetId, payload.candidate ?: "", payload.sdpMid, payload.sdpMLineIndex)
                    "BYE" -> FirebaseRealtimeSignalingService.instance.sendBye(payload.callId, payload.senderId, payload.targetId)
                }
            } catch (e: Throwable) {
                Log.w("WebRtcSignaling", "Firebase Realtime Database signal notice: ${e.message}")
            }

            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val signalMap = mapOf(
                    "callId" to payload.callId,
                    "senderId" to payload.senderId,
                    "targetId" to payload.targetId,
                    "type" to payload.type,
                    "sdp" to (payload.sdp ?: ""),
                    "candidate" to (payload.candidate ?: ""),
                    "sdpMid" to (payload.sdpMid ?: ""),
                    "sdpMLineIndex" to (payload.sdpMLineIndex ?: 0),
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("webrtc_calls")
                    .document(payload.callId)
                    .collection("signals")
                    .add(signalMap)
                    .addOnSuccessListener {
                        Log.i("WebRtcSignaling", "Firebase Firestore WebRTC signal posted successfully: ${payload.type} for call ${payload.callId}")
                    }
                    .addOnFailureListener { e ->
                        Log.w("WebRtcSignaling", "Firebase Firestore WebRTC signal notice: ${e.message}")
                    }
            } catch (e: Throwable) {
                Log.w("WebRtcSignaling", "Firebase Firestore unavailable, using WebSocket/REST transport: ${e.message}")
            }

            // 2. Attempt Socket.io & WebSocket transmission
            try {
                SocketIoSignalingClient.instance.emitSignal(payload)
            } catch (e: Throwable) {}

            val json = payloadAdapter.toJson(payload)
            val sentWs = webSocket?.send(json) ?: false
            
            // 3. Fallback via Retrofit REST signaling layer if WS unavailable
            if (!sentWs) {
                try {
                    api.sendSignal(payload)
                } catch (e: Exception) {
                    Log.e("WebRtcSignaling", "Retrofit signal transmission fallback exception: ${e.message}")
                }
            }
        }
    }

    /**
     * Initializes multi-transport WebRTC signaling engine (Socket.io + Firestore + WebSocket).
     */
    fun initializeSignalingEngine(userId: String, callId: String? = null) {
        connectWebSocket(userId)
        SocketIoSignalingClient.instance.connectSocketIoServer(currentUserId = userId)
        if (!callId.isNullOrEmpty()) {
            listenForFirebaseSignals(callId, userId)
        }
    }

    /**
     * Initiates peer discovery & session negotiation across Socket.io & Firestore.
     */
    fun initiateSessionNegotiation(callId: String, callerId: String, recipientId: String, isVideo: Boolean) {
        SocketIoSignalingClient.instance.initiateCallNegotiation(callId, callerId, recipientId, isVideo)
        
        // Post session negotiation document in Firestore as well for cross-client discovery
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val sessionMap = mapOf(
                "callId" to callId,
                "callerId" to callerId,
                "recipientId" to recipientId,
                "status" to "RINGING",
                "isVideo" to isVideo,
                "createdAt" to System.currentTimeMillis()
            )
            db.collection("webrtc_sessions").document(callId).set(sessionMap)
        } catch (e: Throwable) {
            Log.w("WebRtcSignaling", "Firestore call session notice: ${e.message}")
        }
    }

    /**
     * Responds to call session negotiation (ACCEPT / REJECT).
     */
    fun respondSessionNegotiation(callId: String, callerId: String, recipientId: String, action: String) {
        SocketIoSignalingClient.instance.respondCallNegotiation(callId, callerId, recipientId, action)
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("webrtc_sessions").document(callId).update("status", action)
        } catch (e: Throwable) {}
    }

    /**
     * Subscribes to real-time SDP offers, answers, and ICE candidates via Firebase Firestore for a call session.
     */
    fun listenForFirebaseSignals(callId: String, currentUserId: String) {
        scope.launch {
            try {
                FirebaseRealtimeSignalingService.instance.listenForSignals(callId, currentUserId).collect { payload ->
                    _incomingSignals.emit(payload)
                }
            } catch (e: Throwable) {
                Log.w("WebRtcSignaling", "Firebase Realtime DB listener notice: ${e.message}")
            }
        }

        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("webrtc_calls")
                .document(callId)
                .collection("signals")
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    for (doc in snapshot.documentChanges) {
                        if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val data = doc.document.data
                            val senderId = data["senderId"] as? String ?: ""
                            val targetId = data["targetId"] as? String ?: ""
                            
                            // Process signals meant for this user or broadcast
                            if (senderId != currentUserId && (targetId.isEmpty() || targetId == currentUserId)) {
                                val type = data["type"] as? String ?: ""
                                val sdp = data["sdp"] as? String
                                val candidate = data["candidate"] as? String
                                val sdpMid = data["sdpMid"] as? String
                                val sdpMLineIndex = (data["sdpMLineIndex"] as? Long)?.toInt()

                                val payload = SignalingPayload(
                                    callId = callId,
                                    senderId = senderId,
                                    targetId = targetId,
                                    type = type,
                                    sdp = sdp,
                                    candidate = candidate,
                                    sdpMid = sdpMid,
                                    sdpMLineIndex = sdpMLineIndex
                                )

                                scope.launch {
                                    _incomingSignals.emit(payload)
                                }
                            }
                        }
                    }
                }
        } catch (e: Throwable) {
            Log.w("WebRtcSignaling", "Firebase Firestore real-time listener notice: ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Call ended")
        webSocket = null
        _connectionState.value = SignalingConnectionState.DISCONNECTED
    }

    companion object {
        @Volatile
        private var instance: WebRtcSignalingManager? = null

        fun getInstance(): WebRtcSignalingManager {
            return instance ?: synchronized(this) {
                instance ?: WebRtcSignalingManager().also { instance = it }
            }
        }
    }
}
