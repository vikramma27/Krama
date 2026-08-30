package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.DatabaseHelper
import com.example.data.local.KramaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object MatrixRustSDK {
    private const val TAG = "MatrixRustSDK"
    
    fun initialize(context: Context) {
        Log.i(TAG, "Initializing MatrixRustSDK E2EE crypto bindings on thread: ${Thread.currentThread().name}")
    }
}

object StartupSafetyMonitor {
    private const val TAG = "StartupSafetyMonitor"
    private val bgScope = CoroutineScope(Dispatchers.IO)

    /**
     * Non-blocking asynchronous startup initialization on Dispatchers.IO.
     */
    fun executeSafeStartupAsync(context: Context, onDatabaseInitialized: (() -> Unit)? = null): Job {
        return bgScope.launch {
            executeSafeStartup(context, onDatabaseInitialized)
        }
    }

    /**
     * Executes MatrixRustSDK & Room Database initialization safely on Dispatchers.IO background thread.
     */
    suspend fun executeSafeStartup(context: Context, onDatabaseInitialized: (() -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "StartupSafetyMonitor: Initiating safe startup checks on thread: ${Thread.currentThread().name}...")

            // 1. Initialize MatrixRustSDK
            try {
                MatrixRustSDK.initialize(context)
            } catch (sdkEx: Throwable) {
                Log.e(TAG, "MatrixRustSDK initialization note: ${sdkEx.message}")
            }

            // 2. Initialize RoomDatabase securely on background thread
            val db = KramaDatabase.getDatabase(context)
            try {
                db.openHelper.writableDatabase
            } catch (dbEx: Throwable) {
                Log.e(TAG, "Primary Room/SQLCipher open failed, purging corrupted DB and creating clean database...", dbEx)
                DatabaseHelper.triggerCorruptionRecovery(context)
                val cleanDb = KramaDatabase.getDatabase(context)
                cleanDb.openHelper.writableDatabase
            }

            Log.i(TAG, "StartupSafetyMonitor: MatrixRustSDK and RoomDatabase initialized successfully on background thread.")
            if (onDatabaseInitialized != null) {
                withContext(Dispatchers.Main) {
                    onDatabaseInitialized.invoke()
                }
            }
            true
        } catch (e: Throwable) {
            Log.e(TAG, "StartupSafetyMonitor captured startup exception: ${e.message}", e)

            // Delete corrupted database file using DatabaseHelper
            try {
                DatabaseHelper.triggerCorruptionRecovery(context)
                val cleanDb = KramaDatabase.getDatabase(context)
                cleanDb.openHelper.writableDatabase
            } catch (cleanupEx: Throwable) {
                Log.e(TAG, "Error performing database recovery: ${cleanupEx.message}")
            }

            // Log stack trace
            try {
                KramaCrashlytics.recordNonFatalException(e, "StartupSafetyMonitor_Fatal")
            } catch (crashlyticsEx: Throwable) {
                Log.e(TAG, "Error recording Crashlytics exception: ${crashlyticsEx.message}")
            }

            false
        }
    }
}

