package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.KramaDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.security.SQLCipherKeyRotationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class LocalBackupInfo(
    val file: File,
    val fileName: String,
    val timestampMs: Long,
    val sizeBytes: Long,
    val formattedDate: String
)

/**
 * Encrypted Local Backup & Portability Manager for Krama.
 * Handles exporting and importing encrypted local backups (.kramabackup) using AES-256-GCM.
 */
class EncryptedLocalBackupManager private constructor() {

    companion object {
        private const val TAG = "EncryptedBackupManager"
        private const val BACKUP_DIR_NAME = "KramaEncryptedBackups"

        @Volatile
        private var instance: EncryptedLocalBackupManager? = null

        fun getInstance(): EncryptedLocalBackupManager {
            return instance ?: synchronized(this) {
                instance ?: EncryptedLocalBackupManager().also { instance = it }
            }
        }
    }

    private fun getBackupFolder(context: Context): File {
        val backupDir = File(context.getExternalFilesDir(null), BACKUP_DIR_NAME)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    fun listLocalBackups(context: Context): List<LocalBackupInfo> {
        val folder = getBackupFolder(context)
        val files = folder.listFiles()?.filter { it.name.endsWith(".kramabackup") } ?: emptyList()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        return files.map { file ->
            LocalBackupInfo(
                file = file,
                fileName = file.name,
                timestampMs = file.lastModified(),
                sizeBytes = file.length(),
                formattedDate = dateFormat.format(Date(file.lastModified()))
            )
        }.sortedByDescending { it.timestampMs }
    }

    /**
     * Export complete encrypted chat history (Room DB snapshot + AES-256 GCM encrypted JSON archive).
     */
    suspend fun exportEncryptedBackup(
        context: Context,
        customPassphrase: String? = null
    ): Result<LocalBackupInfo> = withContext(Dispatchers.IO) {
        try {
            val db = KramaDatabase.getDatabase(context)
            val chatDao = db.chatDao()
            val contactDao = db.contactDao()

            val chats = chatDao.getRawChatList()
            val contacts = contactDao.getRawContactList()
            val messages = chatDao.getRawMessageList()

            // 1. Build JSON Payload
            val backupJsonObject = JSONObject().apply {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())

                val contactsArray = JSONArray()
                contacts.forEach { c ->
                    contactsArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("phoneNumber", c.phoneNumber)
                        put("avatarUrl", c.avatarUrl)
                        put("statusText", c.statusText)
                        put("lastSeenTimestamp", c.lastSeenTimestamp)
                        put("isOnline", c.isOnline)
                        put("publicKey", c.publicKey)
                        put("isBlocked", c.isBlocked)
                    })
                }
                put("contacts", contactsArray)

                val chatsArray = JSONArray()
                chats.forEach { ch ->
                    chatsArray.put(JSONObject().apply {
                        put("id", ch.id)
                        put("contactId", ch.contactId)
                        put("title", ch.title)
                        put("avatarUrl", ch.avatarUrl)
                        put("lastMessage", ch.lastMessage)
                        put("lastMessageTimestamp", ch.lastMessageTimestamp)
                        put("unreadCount", ch.unreadCount)
                        put("isGroup", ch.isGroup)
                        put("isMuted", ch.isMuted)
                        put("isArchived", ch.isArchived)
                    })
                }
                put("chats", chatsArray)

                val messagesArray = JSONArray()
                messages.forEach { m ->
                    messagesArray.put(JSONObject().apply {
                        put("id", m.id)
                        put("chatId", m.chatId)
                        put("senderId", m.senderId)
                        put("senderName", m.senderName)
                        put("content", m.content)
                        put("timestamp", m.timestamp)
                        put("status", m.status)
                        put("messageType", m.messageType)
                        put("mediaUrl", m.mediaUrl)
                    })
                }
                put("messages", messagesArray)
            }

            val rawJsonBytes = backupJsonObject.toString().toByteArray(Charsets.UTF_8)

            // 2. Derive Encryption Key (User Passphrase or System Key)
            val keyManager = SQLCipherKeyRotationManager(context.applicationContext)
            val masterSecret = customPassphrase?.ifBlank { null } ?: keyManager.getActivePassphrase()
            val derivedKey = deriveAesKey(masterSecret)

            // 3. Encrypt with AES-256-GCM
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, derivedKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawJsonBytes)

            // Format: [16-byte IV] + [Encrypted Payload]
            val outputStreamBytes = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, outputStreamBytes, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, outputStreamBytes, iv.size, encryptedBytes.size)

            // 4. Save to Backup File
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(getBackupFolder(context), "krama_local_backup_$timeStamp.kramabackup")

            FileOutputStream(backupFile).use { fos ->
                fos.write(outputStreamBytes)
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val info = LocalBackupInfo(
                file = backupFile,
                fileName = backupFile.name,
                timestampMs = backupFile.lastModified(),
                sizeBytes = backupFile.length(),
                formattedDate = dateFormat.format(Date(backupFile.lastModified()))
            )

            Log.i(TAG, "Encrypted local backup created successfully: ${backupFile.name} (${info.sizeBytes} bytes)")
            Result.success(info)
        } catch (e: Throwable) {
            Log.e(TAG, "Export encrypted backup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Import and restore encrypted chat backup file into local database.
     */
    suspend fun importEncryptedBackup(
        context: Context,
        backupFile: File,
        customPassphrase: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            if (!backupFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Backup file does not exist."))
            }

            val fileBytes = FileInputStream(backupFile).use { it.readBytes() }
            if (fileBytes.size < 28) { // 12-byte IV + tag
                return@withContext Result.failure(IllegalArgumentException("Backup file corrupted or truncated."))
            }

            // 1. Extract IV and Ciphertext
            val iv = ByteArray(12)
            System.arraycopy(fileBytes, 0, iv, 0, 12)

            val ciphertext = ByteArray(fileBytes.size - 12)
            System.arraycopy(fileBytes, 12, ciphertext, 0, ciphertext.size)

            // 2. Derive Encryption Key
            val keyManager = SQLCipherKeyRotationManager(context.applicationContext)
            val masterSecret = customPassphrase?.ifBlank { null } ?: keyManager.getActivePassphrase()
            val derivedKey = deriveAesKey(masterSecret)

            // 3. Decrypt Payload
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, derivedKey, gcmSpec)

            val decryptedBytes = try {
                cipher.doFinal(ciphertext)
            } catch (e: Throwable) {
                return@withContext Result.failure(SecurityException("Invalid decryption passphrase or corrupted backup file."))
            }

            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val rootObj = JSONObject(jsonString)

            // 4. Restore Entities into Room DB
            val db = KramaDatabase.getDatabase(context)
            val chatDao = db.chatDao()
            val contactDao = db.contactDao()

            var restoredCount = 0

            val contactsArr = rootObj.optJSONArray("contacts")
            if (contactsArr != null) {
                for (i in 0 until contactsArr.length()) {
                    val obj = contactsArr.getJSONObject(i)
                    contactDao.insertContact(
                        ContactEntity(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            phoneNumber = obj.optString("phoneNumber", ""),
                            avatarUrl = obj.optString("avatarUrl", ""),
                            statusText = obj.optString("statusText", "Available"),
                            lastSeenTimestamp = obj.optLong("lastSeenTimestamp", System.currentTimeMillis()),
                            isOnline = obj.optBoolean("isOnline", false),
                            publicKey = obj.optString("publicKey", ""),
                            isBlocked = obj.optBoolean("isBlocked", false)
                        )
                    )
                }
            }

            val chatsArr = rootObj.optJSONArray("chats")
            if (chatsArr != null) {
                for (i in 0 until chatsArr.length()) {
                    val obj = chatsArr.getJSONObject(i)
                    chatDao.insertChat(
                        ChatEntity(
                            id = obj.getString("id"),
                            contactId = obj.optString("contactId", obj.getString("id")),
                            title = obj.getString("title"),
                            avatarUrl = obj.optString("avatarUrl", ""),
                            lastMessage = obj.optString("lastMessage", ""),
                            lastMessageTimestamp = obj.optLong("lastMessageTimestamp", System.currentTimeMillis()),
                            unreadCount = obj.optInt("unreadCount", 0),
                            isGroup = obj.optBoolean("isGroup", false),
                            isMuted = obj.optBoolean("isMuted", false),
                            isArchived = obj.optBoolean("isArchived", false)
                        )
                    )
                }
            }

            val messagesArr = rootObj.optJSONArray("messages")
            if (messagesArr != null) {
                for (i in 0 until messagesArr.length()) {
                    val obj = messagesArr.getJSONObject(i)
                    val msgId = obj.getString("id")
                    val chatId = obj.getString("chatId")
                    val content = obj.getString("content")
                    val senderName = obj.optString("senderName", "Contact")

                    chatDao.insertMessage(
                        MessageEntity(
                            id = msgId,
                            chatId = chatId,
                            senderId = obj.getString("senderId"),
                            senderName = senderName,
                            content = content,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            status = obj.optString("status", "DELIVERED"),
                            messageType = obj.optString("messageType", "TEXT"),
                            mediaUrl = obj.optString("mediaUrl", "")
                        )
                    )

                    // Also index FTS
                    try {
                        chatDao.insertMessageFts(
                            com.example.data.local.entity.MessageFtsEntity(
                                messageId = msgId,
                                chatId = chatId,
                                content = content,
                                senderName = senderName
                            )
                        )
                    } catch (e: Throwable) {}

                    restoredCount++
                }
            }

            Log.i(TAG, "Restored $restoredCount messages successfully from encrypted backup ${backupFile.name}")
            Result.success(restoredCount)
        } catch (e: Throwable) {
            Log.e(TAG, "Import encrypted backup error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun deriveAesKey(passphrase: String): SecretKey {
        val salt = "KramaEncryptedLocalBackupSalt2026".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 1000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
