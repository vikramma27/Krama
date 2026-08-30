package com.example.domain.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DetailedAppLifecycleState {
    FOREGROUND,
    BACKGROUND,
    SCREEN_LOCKED,
    SCREEN_OFF,
    PICTURE_IN_PICTURE,
    SPLIT_SCREEN,
    MINIMIZED,
    REMOVED_FROM_RECENTS,
    PROCESS_KILLED,
    PHONE_RESTARTED,
    NETWORK_LOST,
    BATTERY_SAVER,
    DOZE,
    APP_UPDATED
}

data class DetailedLifecycleInfo(
    val primaryState: DetailedAppLifecycleState = DetailedAppLifecycleState.FOREGROUND,
    val isInteractive: Boolean = true,
    val isPowerSaveMode: Boolean = false,
    val isDeviceIdle: Boolean = false,
    val isPipMode: Boolean = false,
    val isMultiWindowMode: Boolean = false,
    val isNetworkConnected: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

class LifecycleEngine private constructor(private val context: Context) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _lifecycleInfo = MutableStateFlow(DetailedLifecycleInfo())
    val lifecycleInfo: StateFlow<DetailedLifecycleInfo> = _lifecycleInfo.asStateFlow()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "LifecycleEngine: SCREEN_OFF detected")
                    updateState { copy(primaryState = DetailedAppLifecycleState.SCREEN_OFF, isInteractive = false) }
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG, "LifecycleEngine: SCREEN_ON detected")
                    updateState { copy(isInteractive = true) }
                }
                Intent.ACTION_USER_PRESENT -> {
                    Log.i(TAG, "LifecycleEngine: User unlocked device (USER_PRESENT)")
                    updateState { copy(primaryState = if (isAppInForeground) DetailedAppLifecycleState.FOREGROUND else DetailedAppLifecycleState.BACKGROUND) }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isPowerSave = pm?.isPowerSaveMode ?: false
                    Log.i(TAG, "LifecycleEngine: Power save mode changed = $isPowerSave")
                    updateState { copy(isPowerSaveMode = isPowerSave, primaryState = if (isPowerSave) DetailedAppLifecycleState.BATTERY_SAVER else primaryState) }
                }
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isIdle = pm?.isDeviceIdleMode ?: false
                    Log.i(TAG, "LifecycleEngine: Doze / Device Idle changed = $isIdle")
                    updateState { copy(isDeviceIdle = isIdle, primaryState = if (isIdle) DetailedAppLifecycleState.DOZE else primaryState) }
                }
            }
        }
    }

    private var isAppInForeground = false

    init {
        try {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            registerScreenAndPowerReceivers()
        } catch (e: Throwable) {
            Log.w(TAG, "LifecycleEngine initialization warning: ${e.message}")
        }
    }

    private fun registerScreenAndPowerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
        }
        try {
            context.registerReceiver(screenReceiver, filter)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register screen/power receiver: ${e.message}")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        isAppInForeground = true
        Log.i(TAG, "LifecycleEngine: App entered FOREGROUND")
        updateState { copy(primaryState = DetailedAppLifecycleState.FOREGROUND) }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInForeground = false
        Log.i(TAG, "LifecycleEngine: App entered BACKGROUND / MINIMIZED")
        updateState { copy(primaryState = DetailedAppLifecycleState.BACKGROUND) }
    }

    fun onPictureInPictureModeChanged(isInPip: Boolean) {
        Log.i(TAG, "LifecycleEngine: Picture-in-Picture mode changed: $isInPip")
        updateState { copy(isPipMode = isInPip, primaryState = if (isInPip) DetailedAppLifecycleState.PICTURE_IN_PICTURE else primaryState) }
    }

    fun onMultiWindowModeChanged(isInMultiWindow: Boolean) {
        Log.i(TAG, "LifecycleEngine: Multi-Window mode changed: $isInMultiWindow")
        updateState { copy(isMultiWindowMode = isInMultiWindow, primaryState = if (isInMultiWindow) DetailedAppLifecycleState.SPLIT_SCREEN else primaryState) }
    }

    fun onNetworkStatusChanged(isConnected: Boolean) {
        Log.i(TAG, "LifecycleEngine: Network connectivity status: $isConnected")
        updateState { copy(isNetworkConnected = isConnected, primaryState = if (!isConnected) DetailedAppLifecycleState.NETWORK_LOST else primaryState) }
    }

    fun notifyAppRestarted() {
        Log.i(TAG, "LifecycleEngine: PHONE_RESTARTED recovery sequence triggered")
        updateState { copy(primaryState = DetailedAppLifecycleState.PHONE_RESTARTED) }
    }

    fun notifyAppUpdated() {
        Log.i(TAG, "LifecycleEngine: APP_UPDATED recovery sequence triggered")
        updateState { copy(primaryState = DetailedAppLifecycleState.APP_UPDATED) }
    }

    private fun updateState(block: DetailedLifecycleInfo.() -> DetailedLifecycleInfo) {
        _lifecycleInfo.value = _lifecycleInfo.value.block()
    }

    companion object {
        private const val TAG = "LifecycleEngine"

        @Volatile
        private var INSTANCE: LifecycleEngine? = null

        fun getInstance(context: Context): LifecycleEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LifecycleEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
