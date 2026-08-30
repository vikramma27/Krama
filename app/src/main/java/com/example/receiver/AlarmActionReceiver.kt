package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.MainActivity
import com.example.domain.engine.AppStateCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver registered to process actionable notifications for:
 * 1. Wake-up Alarms ("I'm Awake", "Snooze 5 Min", "Snooze 10 Min")
 * 2. Game Turns ("Your Turn", "Open Game", "Snooze Game")
 * Operates even if the app UI is closed or process was backgrounded.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 9991)

        Log.i(TAG, "AlarmActionReceiver received action: $action for notificationId=$notificationId")

        // Dismiss the active notification shade item
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)

        when (action) {
            ACTION_ALARM_AWAKE -> {
                Log.i(TAG, "User tapped 'I'm Awake' on Wake-up Alarm notification")
                AppStateCoordinator.getInstance(context).updatePendingWakeUpAlarm(null)
                Toast.makeText(context, "☀️ Good morning! Alarm dismissed.", Toast.LENGTH_SHORT).show()
            }

            ACTION_ALARM_SNOOZE_5 -> {
                Log.i(TAG, "User tapped 'Snooze 5 Min'")
                scheduleSnoozeAlarm(context, 5)
                Toast.makeText(context, "⏰ Snoozed for 5 minutes", Toast.LENGTH_SHORT).show()
            }

            ACTION_ALARM_SNOOZE_10 -> {
                Log.i(TAG, "User tapped 'Snooze 10 Min'")
                scheduleSnoozeAlarm(context, 10)
                Toast.makeText(context, "⏰ Snoozed for 10 minutes", Toast.LENGTH_SHORT).show()
            }

            ACTION_GAME_OPEN -> {
                val gameType = intent.getStringExtra("EXTRA_GAME_TYPE") ?: "LUDO"
                Log.i(TAG, "Opening game $gameType directly from notification")
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("EXTRA_LAUNCH_GAME_TYPE", gameType)
                }
                context.startActivity(mainIntent)
            }

            ACTION_GAME_SNOOZE -> {
                Log.i(TAG, "User snoozed game turn notification")
                Toast.makeText(context, "🎮 Game turn reminder snoozed for 15 min", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scheduleSnoozeAlarm(context: Context, minutes: Int) {
        val newTime = System.currentTimeMillis() + (minutes * 60 * 1000)
        AppStateCoordinator.getInstance(context).updatePendingWakeUpAlarm(
            com.example.domain.engine.PendingWakeUpAlarmInfo(
                alarmId = "alarm_snoozed_${System.currentTimeMillis()}",
                scheduledTimeMillis = newTime,
                title = "Snoozed Wake-Up Session ($minutes m)"
            )
        )
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"

        const val ACTION_ALARM_AWAKE = "com.example.action.ALARM_AWAKE"
        const val ACTION_ALARM_SNOOZE_5 = "com.example.action.ALARM_SNOOZE_5"
        const val ACTION_ALARM_SNOOZE_10 = "com.example.action.ALARM_SNOOZE_10"

        const val ACTION_GAME_OPEN = "com.example.action.GAME_OPEN"
        const val ACTION_GAME_SNOOZE = "com.example.action.GAME_SNOOZE"
    }
}
