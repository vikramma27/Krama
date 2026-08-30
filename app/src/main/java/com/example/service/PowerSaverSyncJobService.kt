package com.example.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerSaverSyncJobService : JobService() {

    companion object {
        private const val TAG = "PowerSaverSyncJob"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Power Saver Mode active: Executing throttled background sync batch...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Perform lightweight sync task while preserving battery
                Log.i(TAG, "Throttled background network sync finished.")
                jobFinished(params, false)
            } catch (e: Throwable) {
                Log.e(TAG, "Background sync job error: ${e.message}")
                jobFinished(params, false)
            }
        }

        return true // Task is asynchronous
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(TAG, "Power Saver sync job stopped by system.")
        return true // Reschedule if interrupted
    }
}
