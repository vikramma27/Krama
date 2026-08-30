package com.example.domain.engine

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ActiveCallInfo(
    val callId: String = "",
    val contactId: String = "",
    val callerName: String = "",
    val isVideo: Boolean = false,
    val isConnected: Boolean = false,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false
)

data class ActiveGameInfo(
    val gameType: String = "", // "LUDO", "UNO", "BUSINESS"
    val partnerName: String = "",
    val moveCount: Int = 0,
    val isMyTurn: Boolean = true,
    val boardDataJson: String = "{}"
)

data class PendingWakeUpAlarmInfo(
    val alarmId: String = "",
    val scheduledTimeMillis: Long = 0,
    val title: String = "Wake-Up Session",
    val partnerName: String = "Shivani",
    val isRinging: Boolean = false
)

data class FullAppState(
    val lastScreenRoute: String = "chats",
    val activeChatId: String? = null,
    val activeDraftMessage: String = "",
    val activeCall: ActiveCallInfo? = null,
    val activeGame: ActiveGameInfo? = null,
    val pendingWakeUpAlarm: PendingWakeUpAlarmInfo? = null,
    val activeBackgroundTasksCount: Int = 0,
    val pendingNotificationsCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

class AppStateCoordinator private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("krama_app_state_coordinator", Context.MODE_PRIVATE)

    private val _appState = MutableStateFlow(FullAppState())
    val appState: StateFlow<FullAppState> = _appState.asStateFlow()

    init {
        restorePersistedState()
        subscribeToLifecycleEngine()
    }

    private fun subscribeToLifecycleEngine() {
        scope.launch {
            LifecycleEngine.getInstance(context).lifecycleInfo.collect { info ->
                Log.d(TAG, "AppStateCoordinator received lifecycle update: ${info.primaryState}")
                when (info.primaryState) {
                    DetailedAppLifecycleState.BACKGROUND,
                    DetailedAppLifecycleState.MINIMIZED,
                    DetailedAppLifecycleState.SCREEN_OFF -> {
                        persistCurrentState()
                    }
                    DetailedAppLifecycleState.PHONE_RESTARTED,
                    DetailedAppLifecycleState.APP_UPDATED -> {
                        restorePersistedState()
                    }
                    else -> {}
                }
            }
        }
    }

    fun updateScreenRoute(route: String, chatId: String? = null) {
        _appState.value = _appState.value.copy(
            lastScreenRoute = route,
            activeChatId = chatId ?: _appState.value.activeChatId,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        persistCurrentState()
    }

    fun saveDraftMessage(chatId: String, draftText: String) {
        val currentDrafts = getPersistedDrafts().toMutableMap()
        if (draftText.isBlank()) {
            currentDrafts.remove(chatId)
        } else {
            currentDrafts[chatId] = draftText
        }
        savePersistedDrafts(currentDrafts)
        _appState.value = _appState.value.copy(activeDraftMessage = draftText)
        Log.i(TAG, "Draft saved for chatId=$chatId: length=${draftText.length}")
    }

    fun getDraftMessage(chatId: String): String {
        return getPersistedDrafts()[chatId] ?: ""
    }

    fun updateActiveCall(callInfo: ActiveCallInfo?) {
        _appState.value = _appState.value.copy(
            activeCall = callInfo,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        persistCurrentState()
    }

    fun updateActiveGame(gameInfo: ActiveGameInfo?) {
        _appState.value = _appState.value.copy(
            activeGame = gameInfo,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        persistCurrentState()
    }

    fun updatePendingWakeUpAlarm(alarmInfo: PendingWakeUpAlarmInfo?) {
        _appState.value = _appState.value.copy(
            pendingWakeUpAlarm = alarmInfo,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        persistCurrentState()
    }

    fun persistCurrentState() {
        val state = _appState.value
        try {
            prefs.edit()
                .putString("last_screen_route", state.lastScreenRoute)
                .putString("active_chat_id", state.activeChatId ?: "")
                .putString("call_id", state.activeCall?.callId ?: "")
                .putString("call_contact_id", state.activeCall?.contactId ?: "")
                .putString("call_caller_name", state.activeCall?.callerName ?: "")
                .putBoolean("call_is_video", state.activeCall?.isVideo ?: false)
                .putBoolean("call_is_connected", state.activeCall?.isConnected ?: false)
                .putString("game_type", state.activeGame?.gameType ?: "")
                .putString("game_partner_name", state.activeGame?.partnerName ?: "")
                .putInt("game_move_count", state.activeGame?.moveCount ?: 0)
                .putBoolean("game_is_my_turn", state.activeGame?.isMyTurn ?: true)
                .putString("alarm_id", state.pendingWakeUpAlarm?.alarmId ?: "")
                .putLong("alarm_time_millis", state.pendingWakeUpAlarm?.scheduledTimeMillis ?: 0L)
                .putString("alarm_title", state.pendingWakeUpAlarm?.title ?: "")
                .putLong("last_updated_timestamp", System.currentTimeMillis())
                .apply()
            Log.d(TAG, "AppStateCoordinator persisted state successfully: route=${state.lastScreenRoute}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to persist AppState: ${e.message}")
        }
    }

    private fun restorePersistedState() {
        try {
            val route = prefs.getString("last_screen_route", "chats") ?: "chats"
            val chatId = prefs.getString("active_chat_id", "").takeIf { !it.isNull_or_empty() }
            
            val callId = prefs.getString("call_id", "") ?: ""
            val activeCall = if (callId.isNotEmpty()) {
                ActiveCallInfo(
                    callId = callId,
                    contactId = prefs.getString("call_contact_id", "") ?: "",
                    callerName = prefs.getString("call_caller_name", "") ?: "",
                    isVideo = prefs.getBoolean("call_is_video", false),
                    isConnected = prefs.getBoolean("call_is_connected", false)
                )
            } else null

            val gameType = prefs.getString("game_type", "") ?: ""
            val activeGame = if (gameType.isNotEmpty()) {
                ActiveGameInfo(
                    gameType = gameType,
                    partnerName = prefs.getString("game_partner_name", "") ?: "",
                    moveCount = prefs.getInt("game_move_count", 0),
                    isMyTurn = prefs.getBoolean("game_is_my_turn", true)
                )
            } else null

            val alarmId = prefs.getString("alarm_id", "") ?: ""
            val pendingAlarm = if (alarmId.isNotEmpty()) {
                PendingWakeUpAlarmInfo(
                    alarmId = alarmId,
                    scheduledTimeMillis = prefs.getLong("alarm_time_millis", 0L),
                    title = prefs.getString("alarm_title", "Wake-Up Session") ?: "Wake-Up Session"
                )
            } else null

            _appState.value = FullAppState(
                lastScreenRoute = route,
                activeChatId = chatId,
                activeCall = activeCall,
                activeGame = activeGame,
                pendingWakeUpAlarm = pendingAlarm
            )
            Log.i(TAG, "AppStateCoordinator restored persisted state: route=$route, activeCall=${activeCall != null}, activeGame=${activeGame != null}")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to restore persisted AppState: ${e.message}")
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    private fun getPersistedDrafts(): Map<String, String> {
        val draftsJson = prefs.getString("drafts_json", "{}") ?: "{}"
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(draftsJson)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error parsing drafts json: ${e.message}")
        }
        return map
    }

    private fun savePersistedDrafts(drafts: Map<String, String>) {
        try {
            val obj = JSONObject()
            for ((key, value) in drafts) {
                obj.put(key, value)
            }
            prefs.edit().putString("drafts_json", obj.toString()).apply()
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving drafts json: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "AppStateCoordinator"

        @Volatile
        private var INSTANCE: AppStateCoordinator? = null

        fun getInstance(context: Context): AppStateCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppStateCoordinator(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
