package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.KramaDatabase
import com.example.data.repository.MessengerRepository
import com.example.data.repository.SecurityRepository
import java.util.concurrent.TimeUnit

class ScheduledMessageWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val scheduledMessageId = inputData.getString(KEY_SCHEDULED_MESSAGE_ID)
        Log.i(TAG, "ScheduledMessageWorker executing for message ID: $scheduledMessageId")

        return try {
            val db = KramaDatabase.getDatabase(context)
            val securityRepo = SecurityRepository()
            val repository = MessengerRepository(
                chatDao = db.chatDao(),
                contactDao = db.contactDao(),
                statusDao = db.statusDao(),
                callDao = db.callDao(),
                scheduledMessageDao = db.scheduledMessageDao(),
                securityRepository = securityRepo,
                contactFeatureDao = db.contactFeatureDao(),
                coupleFeaturesDao = db.coupleFeaturesDao()
            )

            val sentCount = repository.sendDueScheduledMessagesNow()
            Log.i(TAG, "Scheduled message delivery completed successfully. Sent $sentCount due message(s).")
            Result.success()
        } catch (e: Throwable) {
            Log.e(TAG, "Error delivering scheduled message: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ScheduledMessageWorker"
        const val KEY_SCHEDULED_MESSAGE_ID = "scheduled_message_id"

        fun enqueueScheduledMessageWork(context: Context, scheduledMessageId: String, delayMillis: Long) {
            try {
                val inputData = Data.Builder()
                    .putString(KEY_SCHEDULED_MESSAGE_ID, scheduledMessageId)
                    .build()

                val initialDelay = delayMillis.coerceAtLeast(0L)

                val workRequest = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(inputData)
                    .addTag("sched_tag_$scheduledMessageId")
                    .build()

                WorkManager.getInstance(context).enqueue(workRequest)
                Log.i(TAG, "Enqueued ScheduledMessageWorker for ID $scheduledMessageId with $initialDelay ms delay")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to enqueue WorkManager task for $scheduledMessageId: ${e.message}")
            }
        }

        fun cancelScheduledMessageWork(context: Context, scheduledMessageId: String) {
            try {
                WorkManager.getInstance(context).cancelAllWorkByTag("sched_tag_$scheduledMessageId")
                Log.i(TAG, "Cancelled ScheduledMessageWorker for ID $scheduledMessageId")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to cancel WorkManager task for $scheduledMessageId: ${e.message}")
            }
        }

        fun schedulePeriodicCheck(context: Context) {
            try {
                val periodicWorkRequest = PeriodicWorkRequestBuilder<ScheduledMessageWorker>(
                    15, TimeUnit.MINUTES
                )
                    .addTag("periodic_scheduled_msg_check")
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "periodic_scheduled_msg_check",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicWorkRequest
                )
                Log.i(TAG, "Scheduled 15-min periodic ScheduledMessageWorker check")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to schedule periodic ScheduledMessageWorker check: ${e.message}")
            }
        }
    }
}
