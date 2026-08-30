package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.EncryptedMediaManager
import java.io.File
import java.util.concurrent.TimeUnit

class StorageCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            val filesDir = context.filesDir
            val usableSpaceBytes = filesDir.usableSpace
            val criticalThresholdBytes = 500L * 1024 * 1024 // 500 MB

            Log.i("StorageCleanupWorker", "Checking device storage: Usable space = ${usableSpaceBytes / (1024 * 1024)} MB")

            val freedBytes = purgeEncryptedMediaOlderThan(context, daysThreshold = 90)
            Log.i("StorageCleanupWorker", "Storage cleanup worker finished. Freed ${freedBytes / (1024 * 1024)} MB of old encrypted media.")

            Result.success()
        } catch (e: Exception) {
            Log.e("StorageCleanupWorker", "Error executing storage cleanup policy: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "AutomatedStorageCleanupWorker"

        fun purgeEncryptedMediaOlderThan(context: Context, daysThreshold: Int): Long {
            var freedBytes = 0L
            val mediaDir = File(context.filesDir, "encrypted_media")
            if (!mediaDir.exists() || !mediaDir.isDirectory) return 0L

            val cutoffTime = System.currentTimeMillis() - (daysThreshold.toLong() * 24 * 3600 * 1000)
            val files = mediaDir.listFiles() ?: return 0L

            for (file in files) {
                if (file.isFile && file.lastModified() < cutoffTime) {
                    val length = file.length()
                    if (file.delete()) {
                        freedBytes += length
                    }
                }
            }
            return freedBytes
        }

        fun enqueuePeriodicCleanup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<StorageCleanupWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Log.i("StorageCleanupWorker", "Automated storage cleanup policy periodic worker enqueued.")
            } catch (e: Exception) {
                Log.e("StorageCleanupWorker", "Failed to enqueue WorkManager cleanup task: ${e.message}")
            }
        }
    }
}
