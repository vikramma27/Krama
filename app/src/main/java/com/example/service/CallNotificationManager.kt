package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.receiver.CallActionReceiver
import com.example.util.CallHapticFeedbackUtil

/**
 * Handles incoming call push notifications using Firebase Cloud Messaging (FCM).
 * Provides high-priority heads-up notifications with interactive 'Accept' and 'Decline' actions
 * that directly trigger the calling UI / WebRTC Call Activity.
 */
object CallNotificationManager {

    private const val TAG = "CallNotificationManager"
    const val NOTIFICATION_ID_BASE = 8000

    fun showIncomingCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        contactId: String = "contact_1",
        isVideo: Boolean = false
    ) {
        Log.i(TAG, "Displaying high-priority incoming call notification for callId=$callId, caller=$callerName")
        KramaNotificationChannelManager.createNotificationChannels(context)

        val notificationId = NOTIFICATION_ID_BASE + (callId.hashCode() and 0x7FFFFFFF % 1000)

        // 1. Content Intent - Tapping notification opens call activity
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = CallActionReceiver.ACTION_INCOMING_CALL_SCREEN
            putExtra("EXTRA_INCOMING_CALL_ID", callId)
            putExtra("EXTRA_CONTACT_ID", contactId)
            putExtra("EXTRA_CALLER_NAME", callerName)
            putExtra("EXTRA_IS_VIDEO", isVideo)
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action Intent - ACCEPT
        val acceptIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_ACCEPT_CALL
            putExtra("EXTRA_INCOMING_CALL_ID", callId)
            putExtra("EXTRA_CONTACT_ID", contactId)
            putExtra("EXTRA_CALLER_NAME", callerName)
            putExtra("EXTRA_IS_VIDEO", isVideo)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }

        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action Intent - DECLINE
        val declineIntent = Intent(context, CallActionReceiver::class.java).apply {
            action = CallActionReceiver.ACTION_DECLINE_CALL
            putExtra("EXTRA_INCOMING_CALL_ID", callId)
            putExtra("EXTRA_NOTIFICATION_ID", notificationId)
        }

        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = if (isVideo) "📹 Incoming Video Call" else "📞 Incoming Voice Call"
        val notificationSubtext = "$callerName is calling..."

        val builder = NotificationCompat.Builder(context, KramaNotificationChannelManager.CHANNEL_INCOMING_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(notificationTitle)
            .setContentText(notificationSubtext)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(contentPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_call, "Accept", acceptPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())

        // Trigger recurring ring vibration using VibratorManager
        CallHapticFeedbackUtil.vibrateIncomingCallRingtone(context)
    }

    fun cancelCallNotification(context: Context, notificationId: Int) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)
            CallHapticFeedbackUtil.stopIncomingCallVibration(context)
            Log.i(TAG, "Cancelled call notification id=$notificationId")
        } catch (e: Throwable) {
            Log.w(TAG, "Error cancelling call notification: ${e.message}")
        }
    }
}
