package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.media.MediaUploadManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker responsible for uploading queued offline media attachments
 * when network connection is restored.
 */
class BackgroundMediaUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Executing background MediaUploadWorker...")
        return try {
            val pendingMediaCount = MediaUploadManager.instance.getPendingQueueSize()
            Log.i(TAG, "Processing pending media uploads in background WorkManager task. Count = $pendingMediaCount")

            MediaUploadManager.instance.processPendingUploads()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BackgroundMediaUploadWorker error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackgroundMediaUploadWorker"
        const val WORK_NAME = "KramaBackgroundMediaUpload"

        fun scheduleMediaUploadRetry(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadRequest = OneTimeWorkRequestBuilder<BackgroundMediaUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                uploadRequest
            )
        }
    }
}
