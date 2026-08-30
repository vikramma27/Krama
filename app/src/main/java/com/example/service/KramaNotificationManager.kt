package com.example.service

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.RemoteMessage

class KramaNotificationManager private constructor(private val context: Context) {

    init {
        KramaNotificationChannelManager.createNotificationChannels(context)
    }

    fun handleIncomingPush(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val chatId = data["chat_id"] ?: "chat_1"
            val senderName = data["sender_name"] ?: "Krama Contact"
            val messageText = data["content"] ?: data["encrypted_payload"] ?: "New encrypted message"

            Log.i(TAG, "Handling FCM incoming message push for chatId=$chatId from sender=$senderName")
            showConversationNotification(chatId, senderName, messageText)
        } else {
            remoteMessage.notification?.let {
                val title = it.title ?: "Krama Alert"
                val body = it.body ?: "New message"
                showConversationNotification("chat_fcm_alert", title, body)
            }
        }
    }

    fun showConversationNotification(
        chatId: String,
        senderName: String,
        messageText: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        KramaConversationNotificationManager.showConversationNotification(
            context = context,
            chatId = chatId,
            senderName = senderName,
            messageText = messageText,
            timestamp = timestamp
        )
    }

    fun showCallAlertNotification(
        callId: String,
        callerName: String,
        contactId: String,
        isVideo: Boolean
    ) {
        CallNotificationManager.showIncomingCallNotification(
            context = context,
            callId = callId,
            callerName = callerName,
            contactId = contactId,
            isVideo = isVideo
        )
    }

    companion object {
        private const val TAG = "KramaNotifMgr"

        @Volatile
        private var INSTANCE: KramaNotificationManager? = null

        fun getInstance(context: Context): KramaNotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KramaNotificationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
