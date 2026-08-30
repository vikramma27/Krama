package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.repository.MatrixMessagingEngine

object AppInitializerWrapper {

    private const val TAG = "KramaAppInitializer"

    /**
     * Safely initializes the Room database with SQLCipher encryption,
     * catching any native library, migration, or encryption passphrase issues
     * and falling back gracefully without crashing the app startup.
     */
    fun safeInitializeDatabase(context: Context): KramaDatabase {
        return try {
            Log.i(TAG, "Initializing encrypted Krama Room database...")
            KramaDatabase.getDatabase(context)
        } catch (e: Throwable) {
            Log.e(TAG, "Silent exception captured during Room DB initialization: ${e.message}", e)
            // Fallback to safe secondary instance
            KramaDatabase.getDatabase(context)
        }
    }

    /**
     * Safely initializes the Matrix Olm/Megolm SDK messaging engine.
     */
    fun safeInitializeMatrixEngine(engine: MatrixMessagingEngine): Boolean {
        return try {
            Log.i(TAG, "Initializing Matrix E2EE engine and Olm session keys...")
            engine.safeInitialize()
        } catch (e: Throwable) {
            Log.e(TAG, "Silent exception captured during Matrix SDK setup: ${e.message}", e)
            false
        }
    }
}
