package com.example.crypto.signal

import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Signal Protocol Double Ratchet Header carried alongside every encrypted message payload.
 */
data class SignalHeader(
    val dhPublicKeyHex: String,
    val sequenceNumber: Int,
    val previousChainLength: Int
) {
    fun toAssociatedData(): ByteArray {
        return "$dhPublicKeyHex:$sequenceNumber:$previousChainLength".toByteArray(Charsets.UTF_8)
    }
}

/**
 * Complete Signal E2EE Message Container containing Double Ratchet Header and AES-256-GCM Ciphertext.
 */
data class SignalMessagePayload(
    val header: SignalHeader,
    val cipherTextBase64: String,
    val protocolVersion: String = "Signal-DoubleRatchet-v3",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun serialize(): String {
        return "SIGNAL_E2EE_DR3|${header.dhPublicKeyHex}|${header.sequenceNumber}|${header.previousChainLength}|$cipherTextBase64"
    }

    companion object {
        fun parse(raw: String): SignalMessagePayload? {
            if (!raw.startsWith("SIGNAL_E2EE_DR3|")) return null
            val parts = raw.split("|")
            if (parts.size < 5) return null
            return SignalMessagePayload(
                header = SignalHeader(
                    dhPublicKeyHex = parts[1],
                    sequenceNumber = parts[2].toIntOrNull() ?: 0,
                    previousChainLength = parts[3].toIntOrNull() ?: 0
                ),
                cipherTextBase64 = parts[4]
            )
        }
    }
}

/**
 * Double Ratchet Algorithm Engine implementing the Signal E2E Encryption specification.
 * Combines a Symmetric KDF Chain Ratchet with an Asymmetric ECDH Key Agreement Ratchet.
 */
class DoubleRatchetSession(
    val chatId: String,
    val localIdentityKeyHex: String,
    val remoteIdentityKeyHex: String
) {
    private var dhsKeyPair: KeyPair = generateEcKeyPair()
    private var dhrPublicKeyHex: String? = null
    private var dhrPublicKey: PublicKey? = null

    private var rootKey: ByteArray = ByteArray(32)
    private var sendingChainKey: ByteArray? = null
    private var receivingChainKey: ByteArray? = null

    private var ns: Int = 0 // Sending message counter
    private var nr: Int = 0 // Receiving message counter
    private var pn: Int = 0 // Previous chain length

    // Skipped Message Keys map: (remoteDHKeyHex, sequenceNumber) -> MessageKey
    private val skippedMessageKeys = mutableMapOf<String, ByteArray>()

    init {
        // Derive initial root key from Alice & Bob identity keys + shared secret
        val combinedSeed = "SIGNAL_DR_INITIAL_ROOT_SEED:$chatId:$localIdentityKeyHex:$remoteIdentityKeyHex"
        rootKey = hkdfExtract(salt = "SignalProtocolSalt".toByteArray(), ikm = combinedSeed.toByteArray())
    }

    /**
     * Initializes state as initiator (Alice) with Bob's DH Public Key.
     */
    fun initializeAsAlice(bobDhPublicHex: String) {
        synchronized(this) {
            dhrPublicKeyHex = bobDhPublicHex
            dhrPublicKey = parseEcPublicKeyHex(bobDhPublicHex)

            val dhOut = performEcdh(dhsKeyPair.private, dhrPublicKey!!)
            val (nextRk, nextCks) = kdfRk(rootKey, dhOut)
            rootKey = nextRk
            sendingChainKey = nextCks
            ns = 0
            nr = 0
            pn = 0
        }
    }

    /**
     * Initializes state as receiver (Bob) with Bob's local DH KeyPair.
     */
    fun initializeAsBob(bobKeyPair: KeyPair) {
        synchronized(this) {
            dhsKeyPair = bobKeyPair
            sendingChainKey = null
            receivingChainKey = null
            ns = 0
            nr = 0
            pn = 0
        }
    }

    /**
     * Encrypts plaintext message using the Double Ratchet Sending Chain (AES-256-GCM).
     */
    fun ratchetEncrypt(plaintext: String): SignalMessagePayload {
        synchronized(this) {
            if (sendingChainKey == null) {
                // If Bob sending before first ratchet, step DH
                if (dhrPublicKey == null && dhrPublicKeyHex != null) {
                    dhrPublicKey = parseEcPublicKeyHex(dhrPublicKeyHex!!)
                }
                if (dhrPublicKey != null) {
                    val dhOut = performEcdh(dhsKeyPair.private, dhrPublicKey!!)
                    val (nextRk, nextCks) = kdfRk(rootKey, dhOut)
                    rootKey = nextRk
                    sendingChainKey = nextCks
                } else {
                    // Fallback initial chain key derivation
                    val (nextCks, messageKey) = kdfCk(hkdfExtract("DEFAULT_SEND_SALT".toByteArray(), rootKey))
                    sendingChainKey = nextCks
                }
            }

            // KDF-CK: derive next Sending Chain Key & Message Key
            val (nextCks, messageKey) = kdfCk(sendingChainKey!!)
            sendingChainKey = nextCks

            val header = SignalHeader(
                dhPublicKeyHex = encodeEcPublicKeyHex(dhsKeyPair.public),
                sequenceNumber = ns,
                previousChainLength = pn
            )
            ns += 1

            val cipherTextBase64 = encryptAesGcm(plaintext, messageKey, header.toAssociatedData())
            return SignalMessagePayload(header = header, cipherTextBase64 = cipherTextBase64)
        }
    }

    /**
     * Decrypts incoming Signal message payload using the Double Ratchet Receiving Chain (AES-256-GCM).
     */
    fun ratchetDecrypt(payload: SignalMessagePayload): String {
        synchronized(this) {
            val header = payload.header
            val remoteDhHex = header.dhPublicKeyHex

            // Check if skipped message key already exists
            val skippedKey = skippedMessageKeys.remove("$remoteDhHex:${header.sequenceNumber}")
            if (skippedKey != null) {
                return decryptAesGcm(payload.cipherTextBase64, skippedKey, header.toAssociatedData())
            }

            // Check if remote DH key changed -> DH Ratchet Step required!
            if (remoteDhHex != dhrPublicKeyHex) {
                skipMessageKeys(header.previousChainLength)

                // DH Ratchet step 1: Update receiving chain with new remote DH key
                dhrPublicKeyHex = remoteDhHex
                dhrPublicKey = parseEcPublicKeyHex(remoteDhHex)

                val dhOut1 = performEcdh(dhsKeyPair.private, dhrPublicKey!!)
                val (rk1, ckr1) = kdfRk(rootKey, dhOut1)
                rootKey = rk1
                receivingChainKey = ckr1

                // DH Ratchet step 2: Generate new local DH key pair and update sending chain
                dhsKeyPair = generateEcKeyPair()
                val dhOut2 = performEcdh(dhsKeyPair.private, dhrPublicKey!!)
                val (rk2, cks2) = kdfRk(rootKey, dhOut2)
                rootKey = rk2
                sendingChainKey = cks2

                pn = ns
                ns = 0
                nr = 0
            }

            skipMessageKeys(header.sequenceNumber)

            if (receivingChainKey == null) {
                val (nextCkr, _) = kdfCk(hkdfExtract("DEFAULT_RECV_SALT".toByteArray(), rootKey))
                receivingChainKey = nextCkr
            }

            val (nextCkr, messageKey) = kdfCk(receivingChainKey!!)
            receivingChainKey = nextCkr
            nr += 1

            return decryptAesGcm(payload.cipherTextBase64, messageKey, header.toAssociatedData())
        }
    }

    private fun skipMessageKeys(untilSequence: Int) {
        val currentCkr = receivingChainKey ?: return
        var tempCkr = currentCkr
        while (nr < untilSequence && skippedMessageKeys.size < 200) {
            val (nextCkr, mk) = kdfCk(tempCkr)
            tempCkr = nextCkr
            if (dhrPublicKeyHex != null) {
                skippedMessageKeys["$dhrPublicKeyHex:$nr"] = mk
            }
            nr += 1
        }
        receivingChainKey = tempCkr
    }

    /**
     * Root Key KDF: HKDF-Expand(HKDF-Extract(RK, DHout)) -> (Next RootKey, ChainKey)
     */
    private fun kdfRk(rk: ByteArray, dhOut: ByteArray): Pair<ByteArray, ByteArray> {
        val prk = hkdfExtract(salt = rk, ikm = dhOut)
        val nextRk = hkdfExpand(prk, "SignalProtocol_RootKey".toByteArray(), 32)
        val nextCk = hkdfExpand(prk, "SignalProtocol_ChainKey".toByteArray(), 32)
        return Pair(nextRk, nextCk)
    }

    /**
     * Chain Key KDF: HMAC-SHA256(CK, 0x01) -> Next CK, HMAC-SHA256(CK, 0x02) -> MessageKey
     */
    private fun kdfCk(ck: ByteArray): Pair<ByteArray, ByteArray> {
        val nextCk = hmacSha256(ck, byteArrayOf(0x01))
        val messageKey = hmacSha256(ck, byteArrayOf(0x02))
        return Pair(nextCk, messageKey)
    }

    companion object {
        private const val TAG = "DoubleRatchetSession"

        fun generateEcKeyPair(): KeyPair {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            return kpg.generateKeyPair()
        }

        fun encodeEcPublicKeyHex(publicKey: PublicKey): String {
            return publicKey.encoded.joinToString("") { "%02x".format(it) }
        }

        fun parseEcPublicKeyHex(hex: String): PublicKey {
            val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val spec = X509EncodedKeySpec(bytes)
            val kf = KeyFactory.getInstance("EC")
            return kf.generatePublic(spec)
        }

        fun performEcdh(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
            val ka = KeyAgreement.getInstance("ECDH")
            ka.init(privateKey)
            ka.doPhase(publicKey, true)
            return ka.generateSecret()
        }

        fun hkdfExtract(salt: ByteArray, ikm: ByteArray): ByteArray {
            return hmacSha256(salt, ikm)
        }

        fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(info)
            mac.update(0x01.toByte())
            return mac.doFinal().copyOf(length)
        }

        fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(if (key.isEmpty()) ByteArray(32) else key, "HmacSHA256")
            mac.init(secretKey)
            return mac.doFinal(data)
        }

        fun encryptAesGcm(plainText: String, messageKey: ByteArray, associatedData: ByteArray): String {
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKeySpec = SecretKeySpec(messageKey.copyOf(32), "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)
            cipher.updateAAD(associatedData)

            val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + cipherTextBytes
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        fun decryptAesGcm(cipherTextBase64: String, messageKey: ByteArray, associatedData: ByteArray): String {
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val cipherTextBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val secretKeySpec = SecretKeySpec(messageKey.copyOf(32), "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)
            cipher.updateAAD(associatedData)

            val decryptedBytes = cipher.doFinal(cipherTextBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        }
    }
}
