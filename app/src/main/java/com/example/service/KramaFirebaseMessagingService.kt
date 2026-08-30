package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import android.app.ActivityManager

class KramaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Registration Token: $token")
        // Persist token locally
        getSharedPreferences("krama_fcm_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_registration_token", token)
            .apply()

        // Sync token to user profile if user is logged in
        try {
            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .update("fcmToken", token)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FCM token sync warning: ${e.message}")
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Received FCM Push payload from: ${remoteMessage.from}")

        // Check if there is an explicit notification payload (e.g. from Firebase Console)
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "Krama Encrypted Alert"
            val body = notification.body ?: "New message received"
            showMessageNotification("chat_fcm_notice", title, body)
            return
        }

        // Only suppress custom data notifications if app is in foreground and active
        if (!isAppInBackground()) {
            Log.d(TAG, "App is currently in foreground; processing real-time message event in app.")
            return
        }

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "MESSAGE"
            val chatId = data["chat_id"] ?: "chat_1"
            val senderName = data["sender_name"] ?: "Krama Contact"
            val rawContent = data["content"] ?: data["encrypted_payload"] ?: ""

            val safePreviewMessage = if (rawContent.isNotBlank() && !rawContent.startsWith("SIGNAL_E2EE")) {
                rawContent
            } else {
                "🔒 New Encrypted Message (Authenticate to view)"
            }

            if (type == "CALL_REQUEST" || type == "CALL_OFFER") {
                val isVideo = data["is_video"] == "true"
                val contactId = data["contact_id"] ?: "contact_1"
                CallNotificationManager.showIncomingCallNotification(
                    context = this,
                    callId = chatId,
                    callerName = senderName,
                    contactId = contactId,
                    isVideo = isVideo
                )
            } else {
                showMessageNotification(chatId, senderName, safePreviewMessage)
            }
        }
    }

    private fun isAppInBackground(): Boolean {
        return try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return true
            val appProcesses = activityManager.runningAppProcesses ?: return true
            val pkgName = packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName == pkgName) {
                    return false
                }
            }
            true
        } catch (e: Throwable) {
            true
        }
    }

    private fun decryptPayloadLocally(ciphertext: String): String {
        return try {
            if (ciphertext.startsWith("ENC:")) {
                ciphertext.removePrefix("ENC:")
            } else if (ciphertext.isNotEmpty()) {
                ciphertext
            } else {
                "🔒 Encrypted Krama Signal Message"
            }
        } catch (e: Exception) {
            "🔒 Encrypted Message"
        }
    }

    private fun showMessageNotification(chatId: String, senderName: String, messageText: String) {
        KramaConversationNotificationManager.showConversationNotification(
            context = this,
            chatId = chatId,
            senderName = senderName,
            messageText = messageText
        )
    }

    private fun showIncomingCallNotification(callId: String, senderName: String) {
        KramaNotificationChannelManager.createNotificationChannels(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_INCOMING_CALL_ID", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            callId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, KramaNotificationChannelManager.CHANNEL_INCOMING_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("📞 Incoming E2EE Call from $senderName")
            .setContentText("Tap to accept or join WebRTC session")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(callId.hashCode() + 1000, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Krama Encrypted Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Encrypted push notifications for incoming chat messages"
            }

            val callChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Krama Incoming Calls",
                NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Encrypted high-priority push alerts for incoming voice/video calls"
            }

            manager.createNotificationChannel(msgChannel)
            manager.createNotificationChannel(callChannel)
        }
    }

    companion object {
        private const val TAG = "KramaFCMService"
        const val CHANNEL_MESSAGES = "krama_fcm_messages"
        const val CHANNEL_CALLS = "krama_fcm_calls"
    }
}
