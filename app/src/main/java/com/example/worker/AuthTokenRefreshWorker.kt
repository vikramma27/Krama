package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker responsible for periodically refreshing
 * Firebase Auth and Matrix security tokens to ensure offline-first seamless session persistence.
 */
class AuthTokenRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Executing background AuthTokenRefreshWorker...")
        return try {
            val firebaseAuth = FirebaseAuth.getInstance()
            val currentUser = firebaseAuth.currentUser

            if (currentUser != null) {
                val tokenResult = currentUser.getIdToken(true).await()
                val token = tokenResult.token
                if (!token.isNullOrEmpty()) {
                    Log.i(TAG, "Firebase Auth token successfully refreshed in background WorkManager task.")
                    applicationContext.getSharedPreferences("krama_auth_tokens", Context.MODE_PRIVATE)
                        .edit()
                        .putString("cached_id_token", token)
                        .putLong("token_refreshed_at", System.currentTimeMillis())
                        .apply()
                }
            } else {
                Log.d(TAG, "No active user logged in. Skipping background token refresh.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "AuthTokenRefreshWorker execution note: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "AuthTokenRefreshWorker"
        const val WORK_NAME = "KramaAuthTokenPeriodicRefresh"

        fun schedulePeriodicRefresh(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val refreshRequest = PeriodicWorkRequestBuilder<AuthTokenRefreshWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                refreshRequest
            )
        }
    }
}
