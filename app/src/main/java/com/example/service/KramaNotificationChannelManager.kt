package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

object KramaNotificationChannelManager {

    private const val TAG = "KramaNotifChannels"

    const val CHANNEL_DIRECT_MESSAGES = "krama_direct_chats"
    const val CHANNEL_GROUP_MESSAGES = "krama_group_chats"
    const val CHANNEL_INCOMING_CALLS = "krama_incoming_calls"
    const val CHANNEL_SYSTEM_ALERTS = "krama_system_alerts"
    const val CHANNEL_MEDIA_UPLOADS = "krama_media_uploads"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Direct Messages Channel
            val directMsgChannel = NotificationChannel(
                CHANNEL_DIRECT_MESSAGES,
                "Messages - Direct",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for encrypted direct messages from contacts"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Group Messages Channel
            val groupMsgChannel = NotificationChannel(
                CHANNEL_GROUP_MESSAGES,
                "Messages - Groups",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for group chats and team rooms"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Incoming Calls Channel
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val incomingCallChannel = NotificationChannel(
                CHANNEL_INCOMING_CALLS,
                "Calls - Incoming WebRTC",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Ringtone and heads-up alerts for incoming encrypted audio/video calls"
                setSound(ringtoneUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 800, 1000)
                setShowBadge(true)
            }

            // 4. System Alerts Channel
            val systemAlertsChannel = NotificationChannel(
                CHANNEL_SYSTEM_ALERTS,
                "System Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Security status, background sync, and system notices"
                setShowBadge(false)
            }

            // 5. Media Uploads Channel
            val mediaUploadsChannel = NotificationChannel(
                CHANNEL_MEDIA_UPLOADS,
                "Media Sharing & Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progress alerts for status media and chat attachment uploads"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(directMsgChannel, groupMsgChannel, incomingCallChannel, systemAlertsChannel, mediaUploadsChannel)
            )
            Log.i(TAG, "Fine-grained Notification Channels created successfully.")
        }
    }
}
