package com.example.worker

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.KramaDatabase

class MissYouCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = KramaDatabase.getDatabase(context)
            val chats = db.chatDao().getRawChatList()
            val now = System.currentTimeMillis()
            val silenceThresholdMs = 24 * 3600 * 1000L // 24 hours

            chats.forEach { chat ->
                val silenceDuration = now - chat.lastMessageTimestamp
                if (chat.lastMessageTimestamp > 0 && silenceDuration > silenceThresholdMs) {
                    val daysSilent = (silenceDuration / (24 * 3600 * 1000L)).coerceAtLeast(1)
                    showMissYouNotification(context, chat.id, chat.title, daysSilent)
                }
            }
            Result.success()
        } catch (e: Throwable) {
            Log.e("MissYouWorker", "Error running miss you check: ${e.message}")
            Result.failure()
        }
    }

    private fun showMissYouNotification(context: Context, chatId: String, chatTitle: String, daysSilent: Long) {
        try {
            val channelId = "krama_miss_you_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Miss You Suggestions",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT
                )
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                manager?.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("💭 Miss $chatTitle?")
                .setContentText("It's been $daysSilent day(s) since you last chatted. Tap to send a quick nudge 💓")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(chatId.hashCode(), notification)
        } catch (e: Throwable) {
            Log.e("MissYouWorker", "Failed posting notification: ${e.message}")
        }
    }
}
