package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.KramaDatabase
import java.io.File

/**
 * Background WorkManager worker responsible for pruning expired call logs and cached message media,
 * preventing database bloating and disk space exhaustion as user communication grows.
 */
class DataCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "🧹 DataCleanupWorker starting scheduled database and media cache pruning cycle...")

        return try {
            val db = KramaDatabase.getDatabase(applicationContext)
            val callDao = db.callDao()
            val chatDao = db.chatDao()

            // 1. Prune call logs older than 90 days
            val ninetyDaysInMillis = 90L * 24 * 60 * 60 * 1000
            val callCutoff = System.currentTimeMillis() - ninetyDaysInMillis
            val deletedCallsCount = callDao.deleteExpiredCallLogs(callCutoff)
            Log.i(TAG, "Pruned $deletedCallsCount expired call log entries older than 90 days.")

            // 2. Clear expired media attachments from database keep message text
            val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
            val mediaCutoff = System.currentTimeMillis() - thirtyDaysInMillis
            chatDao.clearMediaAttachmentsKeepHistory(null, mediaCutoff)

            // 3. Clean temporary media cache files on disk
            var ReclaimedBytes = 0L
            val cacheDir = applicationContext.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                val cacheFiles = cacheDir.listFiles()
                cacheFiles?.forEach { file ->
                    if (file.isFile && (System.currentTimeMillis() - file.lastModified()) > thirtyDaysInMillis) {
                        val len = file.length()
                        if (file.delete()) {
                            ReclaimedBytes += len
                        }
                    }
                }
            }

            val mediaDir = File(applicationContext.filesDir, "media")
            if (mediaDir.exists()) {
                mediaDir.listFiles()?.forEach { file ->
                    if (file.isFile && (System.currentTimeMillis() - file.lastModified()) > thirtyDaysInMillis) {
                        val len = file.length()
                        if (file.delete()) {
                            ReclaimedBytes += len
                        }
                    }
                }
            }

            Log.i(TAG, "DataCleanupWorker successfully completed! Reclaimed ${(ReclaimedBytes / 1024 / 1024)} MB disk cache.")
            Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, "DataCleanupWorker execution failure: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "DataCleanupWorker"
        const val WORK_NAME = "KramaDataCleanupWorker"

        fun schedulePeriodicCleanup(context: Context) {
            try {
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = androidx.work.PeriodicWorkRequestBuilder<DataCleanupWorker>(
                    24, java.util.concurrent.TimeUnit.HOURS
                )
                .setConstraints(constraints)
                .build()

                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.i(TAG, "Scheduled 24-hour periodic DataCleanupWorker task successfully.")
            } catch (e: Throwable) {
                Log.w(TAG, "DataCleanupWorker scheduling note: ${e.message}")
            }
        }
    }
}
