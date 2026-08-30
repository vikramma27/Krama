package com.example.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Real-time WebRTC signaling implementation backed by Firebase Realtime Database.
 * Facilitates low-latency offer, answer, ICE candidate, and bye signal exchange for P2P voice & video calls.
 */
class FirebaseRealtimeSignalingService private constructor() : WebRtcSignalingService {

    private val firebaseDatabase: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    override suspend fun sendOffer(callId: String, senderId: String, targetId: String, sdp: String): Result<Unit> {
        return sendSignalInternal(
            SignalingPayload(
                callId = callId,
                senderId = senderId,
                targetId = targetId,
                type = "OFFER",
                sdp = sdp
            )
        )
    }

    override suspend fun sendAnswer(callId: String, senderId: String, targetId: String, sdp: String): Result<Unit> {
        return sendSignalInternal(
            SignalingPayload(
                callId = callId,
                senderId = senderId,
                targetId = targetId,
                type = "ANSWER",
                sdp = sdp
            )
        )
    }

    override suspend fun sendIceCandidate(
        callId: String,
        senderId: String,
        targetId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ): Result<Unit> {
        return sendSignalInternal(
            SignalingPayload(
                callId = callId,
                senderId = senderId,
                targetId = targetId,
                type = "ICE_CANDIDATE",
                candidate = candidate,
                sdpMid = sdpMid,
                sdpMLineIndex = sdpMLineIndex
            )
        )
    }

    override suspend fun sendBye(callId: String, senderId: String, targetId: String): Result<Unit> {
        return sendSignalInternal(
            SignalingPayload(
                callId = callId,
                senderId = senderId,
                targetId = targetId,
                type = "BYE"
            )
        )
    }

    private suspend fun sendSignalInternal(payload: SignalingPayload): Result<Unit> {
        return try {
            val signalRef = firebaseDatabase.getReference("webrtc_calls")
                .child(payload.callId)
                .child("signals")
                .push()

            val signalMap = mapOf(
                "callId" to payload.callId,
                "senderId" to payload.senderId,
                "targetId" to payload.targetId,
                "type" to payload.type,
                "sdp" to payload.sdp,
                "candidate" to payload.candidate,
                "sdpMid" to payload.sdpMid,
                "sdpMLineIndex" to payload.sdpMLineIndex,
                "timestamp" to System.currentTimeMillis()
            )

            signalRef.setValue(signalMap).await()
            Log.d(TAG, "Successfully sent Firebase Realtime signal [${payload.type}] for call ${payload.callId}")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send Firebase Realtime signal for ${payload.callId}: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun listenForSignals(callId: String, userId: String): Flow<SignalingPayload> = callbackFlow {
        val signalsRef = firebaseDatabase.getReference("webrtc_calls")
            .child(callId)
            .child("signals")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnap in snapshot.children) {
                    try {
                        val senderId = childSnap.child("senderId").getValue(String::class.java) ?: ""
                        val targetId = childSnap.child("targetId").getValue(String::class.java) ?: ""
                        val type = childSnap.child("type").getValue(String::class.java) ?: ""

                        // Filter for signals intended for this user or broadcast
                        if (senderId != userId && (targetId == userId || targetId.isEmpty())) {
                            val sdp = childSnap.child("sdp").getValue(String::class.java)
                            val candidate = childSnap.child("candidate").getValue(String::class.java)
                            val sdpMid = childSnap.child("sdpMid").getValue(String::class.java)
                            val sdpMLineIndex = childSnap.child("sdpMLineIndex").getValue(Long::class.java)?.toInt()

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
                            trySend(payload)
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Error parsing Firebase signal node: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Firebase signaling listener cancelled for $callId: ${error.message}")
            }
        }

        signalsRef.addValueEventListener(listener)

        awaitClose {
            signalsRef.removeEventListener(listener)
        }
    }

    override suspend fun getIceServers(): Result<List<IceServerDto>> {
        return try {
            val defaultServers = listOf(
                IceServerDto(urls = listOf("stun:stun.l.google.com:19302")),
                IceServerDto(urls = listOf("stun:stun1.l.google.com:19302")),
                IceServerDto(urls = listOf("stun:stun2.l.google.com:19302")),
                IceServerDto(
                    urls = listOf("turn:turn.krama.sec:3478?transport=udp"),
                    username = "krama_secure_user",
                    credential = "krama_turn_secret_token"
                )
            )
            Result.success(defaultServers)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "FirebaseRTDBSignaling"

        val instance: FirebaseRealtimeSignalingService by lazy {
            FirebaseRealtimeSignalingService()
        }
    }
}
