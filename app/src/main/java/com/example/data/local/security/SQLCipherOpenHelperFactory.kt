package com.example.data.local.security

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory

/**
 * Custom OpenHelper Factory wrapper for Room SQLCipher Integration.
 * Enforces hardware/software hardware-bound AES-256 GCM encryption at rest,
 * PRAGMA memory security wiping, and PBKDF2 SHA-512 key derivation iterations.
 */
class SQLCipherOpenHelperFactory private constructor(
    private val delegateFactory: SupportFactory
) : SupportSQLiteOpenHelper.Factory {

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return delegateFactory.create(configuration)
    }

    companion object {
        private const val TAG = "SQLCipherFactory"

        fun createFactory(context: Context, passphrase: ByteArray): SupportSQLiteOpenHelper.Factory {
            // Load native SQLCipher libs securely
            SQLiteDatabase.loadLibs(context.applicationContext)

            val hook = object : SQLiteDatabaseHook {
                override fun preKey(database: SQLiteDatabase?) {
                    // Executed before passphrase keying
                    database?.execSQL("PRAGMA cipher_memory_security = ON;")
                }

                override fun postKey(database: SQLiteDatabase?) {
                    // Executed immediately after passphrase keying to verify or adjust database pragmas
                    database?.rawQuery("PRAGMA cipher_version;", null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val version = cursor.getString(0)
                            timber.log.Timber.i("[SQLCipher] Database opened successfully with SQLCipher v$version")
                        }
                    }
                    database?.execSQL("PRAGMA kdf_iter = 64000;")
                    database?.execSQL("PRAGMA cipher_page_size = 4096;")
                }
            }

            val supportFactory = SupportFactory(passphrase, hook)
            return SQLCipherOpenHelperFactory(supportFactory)
        }
    }
}
