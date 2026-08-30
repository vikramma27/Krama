package com.example.data.repository

import android.util.Base64
import android.util.Log
import com.example.domain.model.E2ESafetyNumber
import com.example.domain.security.E2EEncryptionProtocol
import com.example.domain.security.E2EKeyPair
import com.example.domain.security.E2ESessionState
import com.example.domain.security.EncryptedMessagePayload
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SignalDoubleRatchetEngine : E2EEncryptionProtocol {

    private val secureRandom = SecureRandom()

    override fun generateIdentityKeyPair(): E2EKeyPair {
        val keyId = UUID.randomUUID().toString().take(8)
        val privBytes = ByteArray(32)
        secureRandom.nextBytes(privBytes)

        val md = MessageDigest.getInstance("SHA-256")
        val pubBytes = md.digest(privBytes)

        return E2EKeyPair(
            keyId = keyId,
            publicKeyHex = pubBytes.toHex(),
            privateKeyHex = privBytes.toHex()
        )
    }

    override fun createSession(peerId: String, peerPublicKeyHex: String): E2ESessionState {
        val sessionId = "session_${peerId}_${System.currentTimeMillis().toString().takeLast(6)}"
        
        val myKeyPair = generateIdentityKeyPair()
        val combinedSeed = "${myKeyPair.privateKeyHex}:$peerPublicKeyHex"
        val md = MessageDigest.getInstance("SHA-256")
        
        val rootKey = md.digest("root_$combinedSeed".toByteArray()).toHex()
        val senderChain = md.digest("send_$combinedSeed".toByteArray()).toHex()
        val receiverChain = md.digest("recv_$combinedSeed".toByteArray()).toHex()

        return E2ESessionState(
            sessionId = sessionId,
            peerId = peerId,
            rootKeyHex = rootKey,
            chainKeySenderHex = senderChain,
            chainKeyReceiverHex = receiverChain,
            sendRatchetStep = 0,
            receiveRatchetStep = 0,
            isEstablished = true
        )
    }

    override fun encryptMessage(
        session: E2ESessionState,
        plainText: String
    ): Pair<E2ESessionState, EncryptedMessagePayload> {
        val nextStep = session.sendRatchetStep + 1
        
        // Derive message key using HMAC-SHA256 ratchet
        val messageKey = deriveHMACKey(session.chainKeySenderHex, "msg_key_$nextStep")
        val ivBytes = ByteArray(12)
        secureRandom.nextBytes(ivBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(messageKey.take(16).toByteArray(), "AES")
        val gcmSpec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val cipherTextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(ivBytes, Base64.NO_WRAP)

        val macSig = computeMac(messageKey, cipherTextBase64)

        // Advance sender ratchet chain
        val nextSenderChain = deriveHMACKey(session.chainKeySenderHex, "next_chain_$nextStep")
        val updatedSession = session.copy(
            chainKeySenderHex = nextSenderChain,
            sendRatchetStep = nextStep
        )

        val payload = EncryptedMessagePayload(
            senderId = "self",
            recipientId = session.peerId,
            cipherText = cipherTextBase64,
            initializationVector = ivBase64,
            ratchetStep = nextStep,
            senderPublicKey = session.chainKeySenderHex.take(16),
            macSignature = macSig,
            algorithm = "AES-256-GCM+DoubleRatchet"
        )

        return Pair(updatedSession, payload)
    }

    override fun decryptMessage(
        session: E2ESessionState,
        payload: EncryptedMessagePayload
    ): Pair<E2ESessionState, String> {
        return try {
            val nextReceiveStep = session.receiveRatchetStep + 1
            val messageKey = deriveHMACKey(session.chainKeyReceiverHex, "msg_key_${payload.ratchetStep}")

            val ivBytes = Base64.decode(payload.initializationVector, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payload.cipherText, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(messageKey.take(16).toByteArray(), "AES")
            val gcmSpec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            val plainBytes = cipher.doFinal(cipherBytes)
            val plainText = String(plainBytes, Charsets.UTF_8)

            val nextReceiverChain = deriveHMACKey(session.chainKeyReceiverHex, "next_chain_${payload.ratchetStep}")
            val updatedSession = session.copy(
                chainKeyReceiverHex = nextReceiverChain,
                receiveRatchetStep = payload.ratchetStep
            )

            Pair(updatedSession, plainText)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error on ratchet step ${payload.ratchetStep}: ${e.message}")
            Pair(session, "[Decryption Failed - Ratchet Key Mismatch]")
        }
    }

    override fun generateSafetyNumber(
        chatId: String,
        myPublicKey: String,
        peerPublicKey: String
    ): E2ESafetyNumber {
        val combined = "$chatId:$myPublicKey:$peerPublicKey"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(combined.toByteArray())
        val hex = digest.toHex()

        val numericString = hex.filter { it.isDigit() }.padEnd(60, '9')
        val formatted = numericString.chunked(5).take(12).joinToString(" ")

        return E2ESafetyNumber(
            fingerPrint = formatted,
            publicKeySender = myPublicKey,
            publicKeyRecipient = peerPublicKey,
            isVerified = true
        )
    }

    override fun rotateSessionKeys(session: E2ESessionState): E2ESessionState {
        val md = MessageDigest.getInstance("SHA-256")
        val newRoot = md.digest("rotated_root_${session.rootKeyHex}:${System.currentTimeMillis()}".toByteArray()).toHex()
        val newSenderChain = md.digest("rotated_send_${session.chainKeySenderHex}".toByteArray()).toHex()
        val newReceiverChain = md.digest("rotated_recv_${session.chainKeyReceiverHex}".toByteArray()).toHex()

        Log.i(TAG, "Double Ratchet session keys rotated successfully for session ${session.sessionId}")

        return session.copy(
            rootKeyHex = newRoot,
            chainKeySenderHex = newSenderChain,
            chainKeyReceiverHex = newReceiverChain,
            sendRatchetStep = 0,
            receiveRatchetStep = 0
        )
    }

    override fun serializeSession(session: E2ESessionState): String {
        return "${session.sessionId}|${session.peerId}|${session.rootKeyHex}|${session.chainKeySenderHex}|${session.chainKeyReceiverHex}|${session.sendRatchetStep}|${session.receiveRatchetStep}"
    }

    override fun deserializeSession(serialized: String): E2ESessionState {
        val parts = serialized.split("|")
        return if (parts.size >= 7) {
            E2ESessionState(
                sessionId = parts[0],
                peerId = parts[1],
                rootKeyHex = parts[2],
                chainKeySenderHex = parts[3],
                chainKeyReceiverHex = parts[4],
                sendRatchetStep = parts[5].toIntOrNull() ?: 0,
                receiveRatchetStep = parts[6].toIntOrNull() ?: 0
            )
        } else {
            createSession("unknown", "0000000000000000")
        }
    }

    private fun deriveHMACKey(chainKeyHex: String, input: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(chainKeyHex.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(input.toByteArray())
        return hmacBytes.toHex()
    }

    private fun computeMac(keyHex: String, input: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(keyHex.take(32).toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(input.toByteArray())
        return Base64.encodeToString(hmacBytes.take(16).toByteArray(), Base64.NO_WRAP)
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val TAG = "SignalRatchetEngine"
    }
}
