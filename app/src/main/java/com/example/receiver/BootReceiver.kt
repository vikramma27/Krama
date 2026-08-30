package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.domain.engine.LifecycleEngine
import com.example.domain.engine.RecoveryEngine

/**
 * BroadcastReceiver triggered when phone reboots (BOOT_COMPLETED) or app is updated (MY_PACKAGE_REPLACED).
 * Automatically executes the full RecoveryEngine sequence without relying on UI.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "BootReceiver received system broadcast action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.i(TAG, "Device booted successfully. Triggering system recovery engine...")
                LifecycleEngine.getInstance(context).notifyAppRestarted()
                RecoveryEngine.getInstance(context).executeBootOrUpdateRecoverySequence(isBoot = true)
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "App updated/replaced. Triggering app-update recovery engine...")
                LifecycleEngine.getInstance(context).notifyAppUpdated()
                RecoveryEngine.getInstance(context).executeBootOrUpdateRecoverySequence(isBoot = false)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
