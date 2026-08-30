package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.EncryptedChatSyncWorker
import java.util.concurrent.TimeUnit

object AdaptiveSyncManager {
    private const val TAG = "AdaptiveSyncManager"
    private const val PERIODIC_WORK_NAME = "EncryptedChatSyncWork"

    fun evaluateAndApplySyncSchedule(
        context: Context,
        isCallActive: Boolean,
        isLowPowerModeSettingEnabled: Boolean
    ) {
        try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

            val workManager = WorkManager.getInstance(context)

            val (intervalHours, requiresUnmetered) = when {
                isCallActive && isLowPowerModeSettingEnabled -> {
                    Log.i(TAG, "⚡ Active WebRTC call + Low-Power Mode: Cancelling periodic WorkManager sync to protect call bandwidth & battery.")
                    workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
                    return
                }
                isCallActive -> {
                    Pair(24L, true) // Throttle to 24h on Wi-Fi during call
                }
                batteryPct < 15 || isLowPowerModeSettingEnabled -> {
                    Pair(48L, true) // Extended 48h sync on low battery
                }
                batteryPct < 30 -> {
                    Pair(24L, false)
                }
                else -> {
                    Pair(12L, false)
                }
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (requiresUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(batteryPct < 15)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<EncryptedChatSyncWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )

            Log.i(TAG, "AdaptiveSyncManager enqueued $PERIODIC_WORK_NAME with interval ${intervalHours}h (Battery=$batteryPct%, CallActive=$isCallActive)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply adaptive sync schedule: ${e.message}", e)
        }
    }
}
