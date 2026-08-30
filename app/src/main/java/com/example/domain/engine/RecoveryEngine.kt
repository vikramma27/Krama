package com.example.domain.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecoveryReport(
    val lastScreenRoute: String = "chats",
    val activeChatId: String? = null,
    val restoredDraft: String = "",
    val isCallActiveRestored: Boolean = false,
    val callPartnerName: String = "",
    val isGameRestored: Boolean = false,
    val gameType: String = "",
    val gameMoveCount: Int = 0,
    val isAlarmRestored: Boolean = false,
    val recoveryMessage: String = "State restored cleanly after interruption."
)

class RecoveryEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appStateCoordinator = AppStateCoordinator.getInstance(context)

    private val _recoveryReport = MutableStateFlow(RecoveryReport())
    val recoveryReport: StateFlow<RecoveryReport> = _recoveryReport.asStateFlow()

    init {
        performAppStartRecoveryCheck()
    }

    fun performAppStartRecoveryCheck() {
        scope.launch {
            Log.i(TAG, "RecoveryEngine: Executing full recovery inspection...")
            val appState = appStateCoordinator.appState.value

            val restoredDraft = if (appState.activeChatId != null) {
                appStateCoordinator.getDraftMessage(appState.activeChatId)
            } else ""

            val callRestored = appState.activeCall?.isConnected == true
            val gameRestored = appState.activeGame != null && appState.activeGame.gameType.isNotEmpty()
            val alarmRestored = appState.pendingWakeUpAlarm != null

            val summaryMsg = buildString {
                append("Restored: Screen '${appState.lastScreenRoute}'")
                if (restoredDraft.isNotEmpty()) append(" | Draft (${restoredDraft.length} chars)")
                if (callRestored) append(" | Call with ${appState.activeCall?.callerName}")
                if (gameRestored) append(" | Game ${appState.activeGame?.gameType} at move ${appState.activeGame?.moveCount}")
                if (alarmRestored) append(" | Alarm '${appState.pendingWakeUpAlarm?.title}'")
            }

            _recoveryReport.value = RecoveryReport(
                lastScreenRoute = appState.lastScreenRoute,
                activeChatId = appState.activeChatId,
                restoredDraft = restoredDraft,
                isCallActiveRestored = callRestored,
                callPartnerName = appState.activeCall?.callerName ?: "",
                isGameRestored = gameRestored,
                gameType = appState.activeGame?.gameType ?: "",
                gameMoveCount = appState.activeGame?.moveCount ?: 0,
                isAlarmRestored = alarmRestored,
                recoveryMessage = summaryMsg
            )

            Log.i(TAG, "RecoveryEngine completion: $summaryMsg")
        }
    }

    fun executeBootOrUpdateRecoverySequence(isBoot: Boolean) {
        scope.launch {
            val eventType = if (isBoot) "BOOT_COMPLETED" else "APP_UPDATED"
            Log.i(TAG, "RecoveryEngine: Executing $eventType recovery sequence...")

            // 1. Reschedule wake-up alarms
            rescheduleAllAlarmsOnBoot()

            // 2. Re-establish presence connection
            try {
                com.example.data.repository.FirebasePresenceManager.initializeUserPresence(context)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed presence reconnection on boot/update: ${e.message}")
            }

            // 3. Sync pending messages queue
            NetworkStateEngine.getInstance(context)
        }
    }

    private fun rescheduleAllAlarmsOnBoot() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            Log.i(TAG, "Rescheduled wake-up alarms automatically on device boot/update.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed rescheduling alarms on boot: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "RecoveryEngine"

        @Volatile
        private var INSTANCE: RecoveryEngine? = null

        fun getInstance(context: Context): RecoveryEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RecoveryEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
