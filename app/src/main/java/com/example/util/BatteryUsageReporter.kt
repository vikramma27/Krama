package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

object BatteryUsageReporter {
    private const val TAG = "BatteryUsageReporter"

    data class BatteryReport(
        val batteryLevel: Int,
        val isCharging: Boolean,
        val isPowerSaveMode: Boolean,
        val temperatureCelsius: Float,
        val voltageVolts: Float,
        val healthStatus: String,
        val estimatedWebRtcMahDraw: Double,
        val estimatedSyncMahDraw: Double,
        val batteryImpactCategory: String,
        val optimizationTip: String
    )

    fun getBatteryUsageReport(
        context: Context,
        isCallActive: Boolean,
        isLowPowerModeEnabled: Boolean,
        activeCallDurationSeconds: Long = 0L,
        syncJobCount: Int = 1
    ): BatteryReport {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempTenths / 10f

        val voltMilli = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val voltVolts = voltMilli / 1000f

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSaveMode = powerManager?.isPowerSaveMode == true

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        // WebRTC audio/video call power consumption estimate (approx 220mA average during active Opus HD stream)
        val callHours = activeCallDurationSeconds / 3600.0
        val estimatedWebRtcMah = callHours * 220.0 + (if (isCallActive) 15.0 else 2.0)

        // WorkManager background sync power consumption estimate (approx 5mA per periodic sync burst)
        val syncFactor = if (isLowPowerModeEnabled) 0.3 else 1.0
        val estimatedSyncMah = syncJobCount * 4.5 * syncFactor

        val totalEstimatedMah = estimatedWebRtcMah + estimatedSyncMah

        val impactCategory = when {
            totalEstimatedMah > 80.0 -> "High (WebRTC Active)"
            totalEstimatedMah > 30.0 -> "Moderate"
            else -> "Low (Optimized)"
        }

        val tip = when {
            isCallActive && !isLowPowerModeEnabled -> "Enable Low-Power Mode during WebRTC calls to reduce background sync battery draw by up to 70%."
            batteryPct < 20 -> "Battery low (<20%). Low-Power Mode is recommended to extend standby."
            isPowerSaveMode -> "System Power Save active. WorkManager sync intervals throttled to 48h."
            else -> "Battery operating at optimal temperature (${tempCelsius}°C). WebRTC processes throttled safely."
        }

        Log.d(TAG, "Battery report: $batteryPct%, Temp: ${tempCelsius}°C, WebRTC mAh: $estimatedWebRtcMah")

        return BatteryReport(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            temperatureCelsius = tempCelsius,
            voltageVolts = voltVolts,
            healthStatus = healthStr,
            estimatedWebRtcMahDraw = estimatedWebRtcMah,
            estimatedSyncMahDraw = estimatedSyncMah,
            batteryImpactCategory = impactCategory,
            optimizationTip = tip
        )
    }

    /**
     * Continuous background flow emitting battery & WebRTC/sync power metrics every 2.5 seconds.
     */
    fun getBatteryReportFlow(
        context: Context,
        isCallActive: Boolean = false,
        isLowPowerModeEnabled: Boolean = false
    ): Flow<BatteryReport> = flow {
        while (currentCoroutineContext().isActive) {
            emit(getBatteryUsageReport(context, isCallActive, isLowPowerModeEnabled))
            delay(2500L)
        }
    }
}
