package com.example.domain.security

import com.example.domain.model.E2ESafetyNumber

data class E2EKeyPair(
    val keyId: String,
    val publicKeyHex: String,
    val privateKeyHex: String,
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

data class EncryptedMessagePayload(
    val senderId: String,
    val recipientId: String,
    val cipherText: String,
    val initializationVector: String,
    val ratchetStep: Int,
    val senderPublicKey: String,
    val macSignature: String,
    val algorithm: String = "AES-256-GCM+DoubleRatchet",
    val timestamp: Long = System.currentTimeMillis()
)

data class E2ESessionState(
    val sessionId: String,
    val peerId: String,
    val rootKeyHex: String,
    val chainKeySenderHex: String,
    val chainKeyReceiverHex: String,
    val sendRatchetStep: Int = 0,
    val receiveRatchetStep: Int = 0,
    val isEstablished: Boolean = true
)

interface E2EEncryptionProtocol {
    fun generateIdentityKeyPair(): E2EKeyPair
    fun createSession(peerId: String, peerPublicKeyHex: String): E2ESessionState
    fun encryptMessage(session: E2ESessionState, plainText: String): Pair<E2ESessionState, EncryptedMessagePayload>
    fun decryptMessage(session: E2ESessionState, payload: EncryptedMessagePayload): Pair<E2ESessionState, String>
    fun generateSafetyNumber(chatId: String, myPublicKey: String, peerPublicKey: String): E2ESafetyNumber
    fun rotateSessionKeys(session: E2ESessionState): E2ESessionState
    fun serializeSession(session: E2ESessionState): String
    fun deserializeSession(serialized: String): E2ESessionState
}
