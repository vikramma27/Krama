package com.example.data.local.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.sqlcipher.database.SQLiteDatabase
import java.io.File
import java.security.SecureRandom

class SQLCipherKeyRotationManager(private val context: Context) {

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "krama_secure_key_store",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w(TAG, "EncryptedSharedPreferences creation note: ${e.message}. Active fallback: Standard SharedPreferences.")
            context.getSharedPreferences("krama_key_store_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getActivePassphrase(): String {
        return try {
            var key = prefs.getString(KEY_PASSPHRASE, null)
            if (key.isNullOrEmpty()) {
                key = DEFAULT_PASSPHRASE
                prefs.edit().putString(KEY_PASSPHRASE, key).apply()
            }
            key ?: DEFAULT_PASSPHRASE
        } catch (e: Throwable) {
            Log.e(TAG, "Error fetching active passphrase: ${e.message}")
            DEFAULT_PASSPHRASE
        }
    }

    fun rotateDatabaseKey(): Boolean {
        return try {
            SQLiteDatabase.loadLibs(context.applicationContext)
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                Log.i(TAG, "Database file does not exist yet. Generating new key in secure preferences...")
                val newKey = generateSecurePassphrase()
                prefs.edit().putString(KEY_PASSPHRASE, newKey).apply()
                return true
            }

            val currentPassphrase = getActivePassphrase()
            val newPassphrase = generateSecurePassphrase()

            Log.i(TAG, "Initiating SQLCipher database key rotation & re-encryption migration...")

            val db = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                currentPassphrase,
                null,
                SQLiteDatabase.OPEN_READWRITE
            )

            // Execute SQLCipher PRAGMA rekey to re-encrypt database pages on disk
            db.execSQL("PRAGMA rekey = '${escapeSingleQuotes(newPassphrase)}';")
            db.close()

            // Update active passphrase in EncryptedSharedPreferences
            prefs.edit()
                .putString(KEY_PASSPHRASE, newPassphrase)
                .putLong(KEY_LAST_ROTATION_TIMESTAMP, System.currentTimeMillis())
                .apply()

            // Reset Room Database instance in memory so new connection uses new passphrase
            com.example.data.local.KramaDatabase.resetInstance()

            Log.i(TAG, "SQLCipher database key rotation migration completed successfully.")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error during SQLCipher key rotation migration: ${e.message}", e)
            false
        }
    }

    fun getLastKeyRotationTimestamp(): Long {
        return prefs.getLong(KEY_LAST_ROTATION_TIMESTAMP, 0L)
    }

    private fun generateSecurePassphrase(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString("") { "%02x".format(it) }
    }

    private fun escapeSingleQuotes(input: String): String {
        return input.replace("'", "''")
    }

    private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()

    companion object {
        private const val TAG = "KeyRotationManager"
        private const val DB_NAME = "krama_encrypted_db"
        private const val KEY_PASSPHRASE = "sqlcipher_active_passphrase"
        private const val KEY_LAST_ROTATION_TIMESTAMP = "sqlcipher_last_rotation_timestamp"
        private const val DEFAULT_PASSPHRASE = "krama_sqlcipher_e2e_secret_key_2026"
    }
}
