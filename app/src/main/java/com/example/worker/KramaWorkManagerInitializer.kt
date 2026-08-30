package com.example.worker

import android.content.Context
import android.util.Log

object KramaWorkManagerInitializer {

    private const val TAG = "WorkManagerInit"

    fun initializeAllWorkers(context: Context, isLowDataBatteryMode: Boolean = false) {
        try {
            Log.i(TAG, "Configuring WorkManager periodic background tasks...")

            // 1. Encrypted Chat Sync Worker
            EncryptedChatSyncWorker.schedulePeriodicSync(context, isLowDataBatteryMode)

            // 2. Local Message Cleanup Worker
            LocalMessageCleanupWorker.schedulePeriodicCleanup(context)

            // 3. Database Maintenance & PRAGMA Integrity Worker
            DatabaseMaintenanceWorker.schedulePeriodicMaintenance(context)

            // 4. Room Database Index & Expired Call Logs/Media Cleanup Worker
            DataCleanupWorker.schedulePeriodicCleanup(context)

            // 5. Scheduled Message WorkManager Delivery Check
            ScheduledMessageWorker.schedulePeriodicCheck(context)

            // 6. Auth Token Periodic Refresh Worker
            AuthTokenRefreshWorker.schedulePeriodicRefresh(context)

            // 7. Background Media Upload Retry Worker
            BackgroundMediaUploadWorker.scheduleMediaUploadRetry(context)

            // 8. Miss You Nudge Worker
            MissYouWorker.schedulePeriodicCheck(context)

            Log.i(TAG, "All WorkManager background tasks scheduled with optimal battery constraints.")
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing WorkManager periodic workers: ${e.message}", e)
        }
    }
}
