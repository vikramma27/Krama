package com.example.data.repository

import com.example.crypto.signal.SignalSessionManager
import com.example.domain.model.E2ESafetyNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class SecurityRepository {

    private val signalSessionManager = SignalSessionManager.getInstance()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _userPin = MutableStateFlow("1234")
    val userPin: StateFlow<String> = _userPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutSeconds = MutableStateFlow(30L)
    val autoLockTimeoutSeconds: StateFlow<Long> = _autoLockTimeoutSeconds.asStateFlow()

    fun setAutoLockSeconds(seconds: Long) {
        _autoLockTimeoutSeconds.value = seconds
    }

    fun setAppLocked(locked: Boolean) {
        _isAppLocked.value = locked
    }

    fun toggleBiometric(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun unlockAppWithPin(pin: String): Boolean {
        if (pin == _userPin.value) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun unlockAppWithBiometric() {
        _isAppLocked.value = false
    }

    fun setPin(newPin: String) {
        _userPin.value = newPin
    }

    fun setBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
    }

    fun encryptSignalDoubleRatchet(chatId: String, plainText: String, remotePublicKeyHex: String = ""): String {
        return signalSessionManager.encryptMessage(chatId, plainText, remotePublicKeyHex)
    }

    fun decryptSignalDoubleRatchet(chatId: String, cipherPayload: String, remotePublicKeyHex: String = ""): String {
        return signalSessionManager.decryptMessage(chatId, cipherPayload, remotePublicKeyHex)
    }

    fun generateSafetyNumber(chatId: String, senderPublic: String, recipientPublic: String): E2ESafetyNumber {
        return signalSessionManager.generateSignalSafetyNumber(chatId, recipientPublic)
    }

    fun simulateCiphertext(plainText: String): String {
        return signalSessionManager.encryptMessage("default_sim_chat", plainText)
    }

    fun encryptTextWithAES(plainText: String, secretKey: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val keyBytes = md.digest(secretKey.toByteArray())
            val sks = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, sks)
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            "enc_err_${plainText.hashCode()}"
        }
    }

    fun decryptTextWithAES(cipherText: String, secretKey: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val keyBytes = md.digest(secretKey.toByteArray())
            val sks = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
            val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, sks)
            val decoded = android.util.Base64.decode(cipherText, android.util.Base64.NO_WRAP)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            "decryption_error"
        }
    }
}

