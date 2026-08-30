package com.example.service

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.util.Log

object PowerSaverManager {

    private const val TAG = "PowerSaverManager"
    private const val JOB_ID = 98701

    fun applyPowerSaverMode(context: Context, enabled: Boolean) {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return

        if (enabled) {
            val component = ComponentName(context, PowerSaverSyncJobService::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED) // Throttle: only sync on unmetered Wi-Fi in Power Saver
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true)
                .setPersisted(true)

            val result = jobScheduler.schedule(builder.build())
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.i(TAG, "Power Saver Mode enabled: Non-urgent background sync scheduled via JobScheduler.")
            } else {
                Log.w(TAG, "Failed scheduling JobScheduler power saver sync.")
            }
        } else {
            jobScheduler.cancel(JOB_ID)
            Log.i(TAG, "Power Saver Mode disabled: Unconstrained background sync restored.")
        }
    }

    fun notifyCallPowerSaverState(context: Context, isCallActive: Boolean, isLowPowerModeEnabled: Boolean) {
        if (isCallActive && isLowPowerModeEnabled) {
            Log.i(TAG, "⚡ Active WebRTC Call + Low-Power Mode: Cancelling non-essential background sync jobs & throttling background sync frequency to preserve battery.")
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            jobScheduler?.cancel(JOB_ID)
        } else if (!isCallActive && isLowPowerModeEnabled) {
            applyPowerSaverMode(context, true)
        }
    }
}
