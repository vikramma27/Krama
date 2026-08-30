package com.example.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.KramaDatabase
import java.util.concurrent.TimeUnit

class MissYouWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = KramaDatabase.getDatabase(context)
            val chats = db.chatDao().getRawChatList()
            val now = System.currentTimeMillis()

            val customThresholdHours = inputData.getLong("threshold_hours", 24L)
            val thresholdMs = customThresholdHours * 3600 * 1000L

            chats.forEach { chat ->
                val durationSilent = now - chat.lastMessageTimestamp
                if (chat.lastMessageTimestamp > 0 && durationSilent >= thresholdMs) {
                    val hoursSilent = durationSilent / (3600 * 1000L)
                    showMissYouNotification(context, chat.id, chat.title, hoursSilent)
                }
            }
            Result.success()
        } catch (e: Throwable) {
            Log.e("MissYouWorker", "Error running MissYouWorker check: ${e.message}")
            Result.failure()
        }
    }

    private fun showMissYouNotification(context: Context, chatId: String, chatTitle: String, hoursSilent: Long) {
        try {
            val channelId = "krama_miss_you_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Miss You Nudges",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.createNotificationChannel(channel)
            }

            val daysSilent = hoursSilent / 24
            val timeString = if (daysSilent >= 1) "$daysSilent day(s)" else "$hoursSilent hour(s)"

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("💭 Thinking of $chatTitle?")
                .setContentText("It's been $timeString since your last message. Tap to send a warm greeting! 💓")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify("miss_you_$chatId".hashCode(), notification)
        } catch (e: Throwable) {
            Log.e("MissYouWorker", "Failed posting Miss You notification: ${e.message}")
        }
    }

    companion object {
        const val WORK_NAME = "KramaMissYouPeriodicWorker"

        fun schedulePeriodicCheck(context: Context, thresholdHours: Long = 24L) {
            try {
                val inputData = androidx.work.Data.Builder()
                    .putLong("threshold_hours", thresholdHours)
                    .build()

                val request = PeriodicWorkRequestBuilder<MissYouWorker>(6, TimeUnit.HOURS)
                    .setInputData(inputData)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                Log.i("MissYouWorker", "Enqueued MissYouWorker periodic check every 6 hours (Threshold: ${thresholdHours}h)")
            } catch (e: Throwable) {
                Log.e("MissYouWorker", "Failed scheduling MissYouWorker: ${e.message}")
            }
        }
    }
}
