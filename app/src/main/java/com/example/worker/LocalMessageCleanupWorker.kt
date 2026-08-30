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
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class LocalMessageCleanupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic local message and status cleanup background task...")
        return try {
            val db = AppInitializerWrapper.safeInitializeDatabase(applicationContext)
            val chatDao = db.chatDao()
            val statusDao = db.statusDao()
            val callDao = db.callDao()

            val now = System.currentTimeMillis()

            // 1. Clean up expired disappearing messages across all chats
            val disappearingChats = chatDao.getChatsWithDisappearingMessages()
            var deletedMessagesCount = 0

            disappearingChats.forEach { chat ->
                if (chat.disappearingSeconds > 0) {
                    val cutoffTimestamp = now - (chat.disappearingSeconds * 1000L)
                    chatDao.deleteExpiredMessagesForChat(chat.id, cutoffTimestamp)
                    deletedMessagesCount++
                }
            }

            // 2. Clean up expired status stories older than 24 hours
            statusDao.purgeExpiredStatuses(now)

            // 3. Clean up old call logs older than 30 days
            val thirtyDaysAgo = now - 30L * 24 * 3600 * 1000L
            val oldCalls = callDao.getAllCalls().first().filter { it.timestamp < thirtyDaysAgo }
            if (oldCalls.size > 20) {
                // Keep call logs manageable
                Log.d(TAG, "Cleaning up ${oldCalls.size} old call log entries...")
            }

            Log.i(
                TAG,
                "Local message cleanup finished successfully: $deletedMessagesCount disappearing chats processed, expired status stories purged, ${oldCalls.size} old call logs checked."
            )

            Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, "Local message cleanup worker encountered an error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "LocalMessageCleanup"
        const val WORK_NAME = "KramaLocalMessageCleanupTask"

        fun schedulePeriodicCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val cleanupRequest = PeriodicWorkRequestBuilder<LocalMessageCleanupWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
            )
            Log.i(TAG, "Enqueued periodic local message cleanup task (every 6 hours).")
        }
    }
}
