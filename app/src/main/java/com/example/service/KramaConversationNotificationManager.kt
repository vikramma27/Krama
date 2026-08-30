package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity

object KramaConversationNotificationManager {

    private const val TAG = "KramaConversationNotif"
    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_REPLY_MESSAGE = "com.example.action.REPLY_MESSAGE"

    fun showConversationNotification(
        context: Context,
        chatId: String,
        senderName: String,
        messageText: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        KramaNotificationChannelManager.createNotificationChannels(context)

        val notificationId = (chatId.hashCode() and 0x7FFFFFFF) % 10000 + 1000

        // User Person
        val userPerson = Person.Builder()
            .setName("Me")
            .setKey("user_me")
            .build()

        // Sender Person
        val senderAvatarBitmap = createAvatarBitmap(senderName)
        val senderPerson = Person.Builder()
            .setName(senderName)
            .setKey("sender_$senderName")
            .setIcon(IconCompat.createWithBitmap(senderAvatarBitmap))
            .build()

        // Content Pending Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_CHAT_ID", chatId)
            putExtra("EXTRA_SENDER_NAME", senderName)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // RemoteInput for Inline Direct Reply
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $senderName...")
            .build()

        val replyIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_REPLY_MESSAGE
            putExtra("EXTRA_CHAT_ID", chatId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val replyPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 5,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Heart Reaction Action (One-tap heart reaction from notification)
        val heartIntent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
            action = "com.example.action.HEART_REACTION"
            putExtra("EXTRA_CHAT_ID", chatId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val heartPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10,
            heartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val heartAction = NotificationCompat.Action.Builder(
            android.R.drawable.btn_star,
            "❤️ Heart",
            heartPendingIntent
        ).build()

        // Voice Note Reply Action (Directly from lock screen)
        val voiceRemoteInput = RemoteInput.Builder("key_voice_reply")
            .setLabel("Record or type voice note...")
            .build()
        val voiceIntent = Intent(context, com.example.receiver.CallActionReceiver::class.java).apply {
            action = "com.example.action.VOICE_REPLY"
            putExtra("EXTRA_CHAT_ID", chatId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }
        val voicePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 15,
            voiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val voiceAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_btn_speak_now,
            "🎤 Voice Reply",
            voicePendingIntent
        ).addRemoteInput(voiceRemoteInput).build()

        // MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle("Chat with $senderName")
            .addMessage(messageText, timestamp, senderPerson)

        val builder = NotificationCompat.Builder(context, KramaNotificationChannelManager.CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)
            .addAction(heartAction)
            .addAction(voiceAction)
            .setGroup("com.example.krama.CHAT_MESSAGES")
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())

        // Smart Notification Grouping Summary
        val summaryNotification = NotificationCompat.Builder(context, KramaNotificationChannelManager.CHANNEL_DIRECT_MESSAGES)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("Krama Messages"))
            .setGroup("com.example.krama.CHAT_MESSAGES")
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(9999, summaryNotification)
        Log.i(TAG, "Conversation notification posted for chatId=$chatId from sender=$senderName")
    }

    private fun createAvatarBitmap(name: String): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val initial = if (name.isNotEmpty()) name.take(1).uppercase() else "K"
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(initial, size / 2f, yPos, textPaint)
        return bitmap
    }
}
