package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.service.WebRtcDiagnosticCollector

/**
 * BroadcastReceiver monitoring device battery level and charging status during WebRTC calls.
 * Automatically throttles video bitrate and resolution when battery drops below 15%
 * and restores HD bitrate when battery is charged or plugged into a power source.
 */
class CallBatteryMonitorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

            if (level == -1 || scale == -1) return

            val batteryPct = (level * 100f) / scale
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val isLowBattery = batteryPct < 15f && !isCharging

            Log.i(TAG, "Battery Status Update: $batteryPct% (Charging: $isCharging, LowBatteryThrottle: $isLowBattery)")

            if (isLowBattery != lastLowBatteryState) {
                lastLowBatteryState = isLowBattery
                adjustWebRtcVideoQuality(context, isLowBattery, batteryPct)
            }
        }
    }

    private fun adjustWebRtcVideoQuality(context: Context, enableLowPowerMode: Boolean, currentPct: Float) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager

        if (enableLowPowerMode) {
            Log.w(TAG, "🚨 Battery below 15% ($currentPct%). Automatically throttling WebRTC video to 360p SD @ 250 kbps.")
            WebRtcDiagnosticCollector.instance.setLowBatteryMode(true)

            try {
                val notification = androidx.core.app.NotificationCompat.Builder(
                    appContext,
                    com.example.service.KramaNotificationChannelManager.CHANNEL_SYSTEM_ALERTS
                )
                    .setContentTitle("🔋 Battery Below 15% • WebRTC Saver Active")
                    .setContentText("Video stream adjusted to 360p SD @ 250 kbps to preserve battery.")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()
                notificationManager?.notify(9901, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send battery alert notification: ${e.message}")
            }
        } else {
            Log.i(TAG, "🔋 Battery level restored or charging ($currentPct%). Restoring WebRTC video to 720p HD @ 1.5 Mbps.")
            WebRtcDiagnosticCollector.instance.setLowBatteryMode(false)

            try {
                val notification = androidx.core.app.NotificationCompat.Builder(
                    appContext,
                    com.example.service.KramaNotificationChannelManager.CHANNEL_SYSTEM_ALERTS
                )
                    .setContentTitle("⚡ Battery Power Restored")
                    .setContentText("WebRTC video restored to 720p HD clarity.")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                    .setAutoCancel(true)
                    .build()
                notificationManager?.notify(9902, notification)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send battery restoration notification: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "CallBatteryMonitor"
        private var lastLowBatteryState: Boolean = false

        fun register(context: Context, receiver: CallBatteryMonitorReceiver): Intent? {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            return context.registerReceiver(receiver, filter)
        }

        fun unregister(context: Context, receiver: CallBatteryMonitorReceiver) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering battery receiver: ${e.message}")
            }
        }
    }
}
