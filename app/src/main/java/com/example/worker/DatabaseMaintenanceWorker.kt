package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppInitializerWrapper
import java.util.concurrent.TimeUnit

class DatabaseMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic SQLCipher Room Database maintenance worker...")
        return try {
            val db = AppInitializerWrapper.safeInitializeDatabase(applicationContext)
            
            // Execute PRAGMA quick_check and WAL checkpoint / vacuum via database helper
            val isVerified = com.example.data.local.KramaDatabase.verifySqlCipherIntegrity(applicationContext, db)
            
            if (isVerified) {
                Log.i(TAG, "PRAGMA database maintenance & quick_check passed successfully.")
                Result.success()
            } else {
                Log.w(TAG, "PRAGMA database maintenance check returned non-ok result. Requesting retry...")
                Result.retry()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Database maintenance worker exception: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DatabaseMaintenance"
        const val WORK_NAME = "KramaDatabaseMaintenanceTask"

        fun schedulePeriodicMaintenance(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .build()

            val maintenanceRequest = PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                maintenanceRequest
            )
            Log.i(TAG, "Enqueued periodic database maintenance worker (every 24 hours, while charging & battery not low).")
        }
    }
}
