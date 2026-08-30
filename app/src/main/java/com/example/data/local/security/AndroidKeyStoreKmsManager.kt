package com.example.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature

/**
 * Hardware-backed Key Management System (KMS) leveraging the Android Keystore system.
 * Generates and stores Elliptic Curve (P-256) identity key pairs securely inside
 * device hardware (Secure Element / StrongBox) and manages public key sharing via Firestore.
 */
class AndroidKeyStoreKmsManager private constructor() {

    companion object {
        private const val TAG = "AndroidKeyStoreKMS"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val IDENTITY_KEY_ALIAS = "krama_e2ee_hardware_identity_key"

        val instance: AndroidKeyStoreKmsManager by lazy {
            AndroidKeyStoreKmsManager()
        }
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Retrieves existing local identity KeyPair or generates a new hardware-backed EC KeyPair.
     */
    fun getOrCreateIdentityKeyPair(): KeyPair {
        return try {
            if (keyStore.containsAlias(IDENTITY_KEY_ALIAS)) {
                val privateKey = keyStore.getKey(IDENTITY_KEY_ALIAS, null) as PrivateKey
                val publicKey = keyStore.getCertificate(IDENTITY_KEY_ALIAS).publicKey
                KeyPair(publicKey, privateKey)
            } else {
                generateHardwareKeyPair()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error accessing KeyStore identity key: ${e.message}. Re-generating...", e)
            generateHardwareKeyPair()
        }
    }

    private fun generateHardwareKeyPair(): KeyPair {
        Log.i(TAG, "Generating hardware-backed EC (P-256) keypair in AndroidKeyStore...")
        val kpg = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            IDENTITY_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(false) // Set true when Biometric prompt binding is enforced
            .build()

        kpg.initialize(parameterSpec)
        val keyPair = kpg.generateKeyPair()
        Log.i(TAG, "Successfully generated hardware EC keypair with alias: $IDENTITY_KEY_ALIAS")
        return keyPair
    }

    /**
     * Returns local public key encoded as Hex string.
     */
    fun getLocalPublicKeyHex(): String {
        val keyPair = getOrCreateIdentityKeyPair()
        return bytesToHex(keyPair.public.encoded)
    }

    /**
     * Signs data using the hardware-backed private key.
     */
    fun signData(data: ByteArray): ByteArray {
        val keyPair = getOrCreateIdentityKeyPair()
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data)
        return signature.sign()
    }

    /**
     * Verifies data signature using a public key.
     */
    fun verifySignature(data: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Throwable) {
            Log.w(TAG, "Signature verification failed: ${e.message}")
            false
        }
    }

    /**
     * Publishes local hardware public key to Firestore under `users/{userId}/keys/identity_key`.
     */
    suspend fun sharePublicKeyToFirestore(userId: String): Result<String> {
        if (userId.isBlank()) return Result.failure(IllegalArgumentException("User ID cannot be blank"))

        return try {
            val publicKeyHex = getLocalPublicKeyHex()
            val firestore = FirebaseFirestore.getInstance()

            val keyData = mapOf(
                "userId" to userId,
                "publicKeyHex" to publicKeyHex,
                "algorithm" to "EC_SECP256R1_HARDWARE",
                "provider" to KEYSTORE_PROVIDER,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(userId)
                .collection("keys")
                .document("identity_key")
                .set(keyData)
                .await()

            Log.d(TAG, "Successfully shared AndroidKeyStore public key to Firestore for user $userId")
            Result.success(publicKeyHex)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to share public key to Firestore for user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
