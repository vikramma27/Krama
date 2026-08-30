package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptedMediaManager {
    private const val TAG = "EncryptedMediaManager"
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private var masterKey: SecretKey? = null

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        if (masterKey == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE)
            masterKey = keyGen.generateKey()
        }
        return masterKey!!
    }

    /**
     * Encrypts media stream fromUri using AES-256-GCM and saves to local app encrypted file storage.
     * Before encryption, high-resolution photos undergo native C++ JNI dynamic resizing & compression.
     * Returns absolute path of encrypted file.
     */
    fun encryptAndSaveMedia(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open input stream for URI: $sourceUri")
                return null
            }

            var rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) return null

            val mimeType = context.contentResolver.getType(sourceUri) ?: ""
            if (mimeType.startsWith("image") || sourceUri.toString().contains("photo") || sourceUri.toString().contains("image")) {
                Log.i(TAG, "Detected image upload. Executing Native JNI pre-encryption image compression...")
                val compressedResult = com.example.util.NativeImageCompressor.processAndCompressPhoto(
                    rawBytes = rawBytes,
                    maxDimension = 1920,
                    quality = 82
                )
                rawBytes = compressedResult.processedBytes
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val secretKey = getOrCreateMasterKey()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val encryptedBytes = cipher.doFinal(rawBytes)

            // Save encrypted file: IV (12 bytes) + Encrypted Payload
            val mediaDir = File(context.filesDir, "encrypted_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()

            val fileName = "enc_media_${System.currentTimeMillis()}_${(1000..9999).random()}.kramae2e"
            val outputFile = File(mediaDir, fileName)

            FileOutputStream(outputFile).use { fos ->
                fos.write(iv)
                fos.write(encryptedBytes)
            }

            Log.d(TAG, "Successfully encrypted media file (${rawBytes.size} -> ${outputFile.length()} bytes) at ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt media: ${e.message}", e)
            null
        }
    }

    fun encryptRawBytes(context: Context, rawBytes: ByteArray, filePrefix: String = "voice"): String? {
        return try {
            if (rawBytes.isEmpty()) return null

            var payloadBytes = rawBytes
            if (filePrefix.contains("photo") || filePrefix.contains("image")) {
                Log.i(TAG, "Executing Native JNI pre-encryption compression on raw image bytes...")
                val compressedResult = com.example.util.NativeImageCompressor.processAndCompressPhoto(
                    rawBytes = rawBytes,
                    maxDimension = 1920,
                    quality = 82
                )
                payloadBytes = compressedResult.processedBytes
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val secretKey = getOrCreateMasterKey()
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

            val encryptedBytes = cipher.doFinal(payloadBytes)

            val mediaDir = File(context.filesDir, "encrypted_media")
            if (!mediaDir.exists()) mediaDir.mkdirs()

            val fileName = "${filePrefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.kramae2e"
            val outputFile = File(mediaDir, fileName)

            FileOutputStream(outputFile).use { fos ->
                fos.write(iv)
                fos.write(encryptedBytes)
            }

            Log.d(TAG, "Successfully encrypted raw bytes (${payloadBytes.size} -> ${outputFile.length()} bytes) at ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt raw bytes: ${e.message}", e)
            null
        }
    }

    fun decryptMediaToBytes(filePath: String): ByteArray? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val fileBytes = FileInputStream(file).use { it.readBytes() }
            if (fileBytes.size <= GCM_IV_LENGTH) return null

            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = fileBytes.copyOfRange(GCM_IV_LENGTH, fileBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val secretKey = getOrCreateMasterKey()
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt bytes file at $filePath: ${e.message}", e)
            null
        }
    }

    /**
     * Decrypts an encrypted .kramae2e file path and decodes into a Bitmap in memory.
     */
    fun decryptMediaToBitmap(filePath: String): Bitmap? {
        return try {
            val file = File(filePath)
            if (!file.exists()) return null

            val fileBytes = FileInputStream(file).use { it.readBytes() }
            if (fileBytes.size <= GCM_IV_LENGTH) return null

            val iv = fileBytes.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = fileBytes.copyOfRange(GCM_IV_LENGTH, fileBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val secretKey = getOrCreateMasterKey()
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt media file at $filePath: ${e.message}", e)
            null
        }
    }
}
