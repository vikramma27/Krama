package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.remote.WebRtcSignalingManager
import com.example.service.CallNotificationManager
import com.example.util.CallHapticFeedbackUtil
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver registered in AndroidManifest to handle notification shade clicks
 * for Accept and Decline on incoming WebRTC call alerts.
 */
class CallActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val callId = intent.getStringExtra("EXTRA_INCOMING_CALL_ID") ?: ""
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 0)

        Log.i(TAG, "CallActionReceiver received action: $action for callId: $callId")

        // Stop incoming ring vibration
        CallHapticFeedbackUtil.stopIncomingCallVibration(context)

        if (notificationId != 0) {
            CallNotificationManager.cancelCallNotification(context, notificationId)
        }

        when (action) {
            ACTION_ACCEPT_CALL -> {
                CallHapticFeedbackUtil.vibrateConnectionEstablished(context)

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_INCOMING_CALL_ID", callId)
                    putExtra("EXTRA_CONTACT_ID", intent.getStringExtra("EXTRA_CONTACT_ID"))
                    putExtra("EXTRA_CALLER_NAME", intent.getStringExtra("EXTRA_CALLER_NAME"))
                    putExtra("EXTRA_IS_VIDEO", intent.getBooleanExtra("EXTRA_IS_VIDEO", false))
                    putExtra("EXTRA_AUTO_ACCEPT_CALL", true)
                }
                context.startActivity(mainIntent)
            }

            ACTION_DECLINE_CALL -> {
                CallHapticFeedbackUtil.vibrateEndCall(context)

                if (callId.isNotEmpty()) {
                    WebRtcSignalingManager.getInstance().respondSessionNegotiation(
                        callId = callId,
                        callerId = intent.getStringExtra("EXTRA_CONTACT_ID") ?: "",
                        recipientId = "user_me",
                        action = "REJECT"
                    )
                }
            }

            "com.example.action.HEART_REACTION" -> {
                val chatId = intent.getStringExtra("EXTRA_CHAT_ID") ?: ""
                Log.i(TAG, "Heart Reaction ❤️ sent from notification for chatId=$chatId")
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val db = com.example.data.local.KramaDatabase.getDatabase(context)
                        val lastMsg = db.chatDao().getLatestMessageForChat(chatId)
                        if (lastMsg != null) {
                            db.chatDao().insertMessageReaction(
                                com.example.data.local.entity.MessageReactionEntity(
                                    messageId = lastMsg.id,
                                    emoji = "❤️",
                                    userId = "user_me"
                                )
                            )
                        }
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed inserting heart reaction from notification: ${e.message}")
                    }
                }
                android.widget.Toast.makeText(context, "❤️ Heart Reaction Sent!", android.widget.Toast.LENGTH_SHORT).show()
            }

            "com.example.action.VOICE_REPLY" -> {
                val chatId = intent.getStringExtra("EXTRA_CHAT_ID") ?: ""
                val remoteInput = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
                val voiceReplyText = remoteInput?.getCharSequence("key_voice_reply")?.toString() ?: "🎙️ [Voice Reply Attached]"
                Log.i(TAG, "Voice Reply 🎤 received from notification: $voiceReplyText for chatId=$chatId")
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        val db = com.example.data.local.KramaDatabase.getDatabase(context)
                        val msgId = "msg_voice_${System.currentTimeMillis()}"
                        db.chatDao().insertMessage(
                            com.example.data.local.entity.MessageEntity(
                                id = msgId,
                                chatId = chatId,
                                senderId = "user_me",
                                senderName = "Me",
                                content = "🎙️ $voiceReplyText",
                                timestamp = System.currentTimeMillis(),
                                status = "DELIVERED",
                                messageType = "VOICE",
                                mediaUrl = "content://voice_reply_lock_screen"
                            )
                        )
                    } catch (e: Throwable) {
                        Log.e(TAG, "Failed saving voice reply: ${e.message}")
                    }
                }
                android.widget.Toast.makeText(context, "🎤 Voice Reply Sent!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val TAG = "CallActionReceiver"

        const val ACTION_ACCEPT_CALL = "com.example.action.ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_CALL"
        const val ACTION_INCOMING_CALL_SCREEN = "com.example.action.INCOMING_CALL_SCREEN"
    }
}
