package com.example.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * Interface defining WebRTC signaling service operations for voice & video call session establishment.
 */
interface WebRtcSignalingService {
    suspend fun sendOffer(callId: String, senderId: String, targetId: String, sdp: String): Result<Unit>
    suspend fun sendAnswer(callId: String, senderId: String, targetId: String, sdp: String): Result<Unit>
    suspend fun sendIceCandidate(
        callId: String,
        senderId: String,
        targetId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int?
    ): Result<Unit>
    suspend fun sendBye(callId: String, senderId: String, targetId: String): Result<Unit>
    fun listenForSignals(callId: String, userId: String): Flow<SignalingPayload>
    suspend fun getIceServers(): Result<List<IceServerDto>>
}
