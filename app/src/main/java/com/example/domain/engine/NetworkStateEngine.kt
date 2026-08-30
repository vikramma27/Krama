package com.example.domain.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NetworkConnectionState {
    ONLINE,
    WEAK,
    OFFLINE,
    SYNCING,
    CONNECTED
}

data class PendingNetworkAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String, // "SEND_MESSAGE", "REACTION", "GAME_MOVE", "CALL_SIGNAL"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

class NetworkStateEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _networkState = MutableStateFlow(NetworkConnectionState.ONLINE)
    val networkState: StateFlow<NetworkConnectionState> = _networkState.asStateFlow()

    private val _pendingQueue = MutableStateFlow<List<PendingNetworkAction>>(emptyList())
    val pendingQueue: StateFlow<List<PendingNetworkAction>> = _pendingQueue.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "NetworkStateEngine: Network became AVAILABLE -> CONNECTED")
            _networkState.value = NetworkConnectionState.CONNECTED
            scope.launch {
                replayPendingActions()
            }
        }

        override fun onLost(network: Network) {
            Log.w(TAG, "NetworkStateEngine: Network lost -> OFFLINE")
            _networkState.value = NetworkConnectionState.OFFLINE
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            
            val isSignalWeak = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                networkCapabilities.signalStrength < -80
            } else false

            _networkState.value = when {
                !hasInternet || !isValidated -> NetworkConnectionState.OFFLINE
                isSignalWeak -> NetworkConnectionState.WEAK
                else -> NetworkConnectionState.ONLINE
            }
            Log.i(TAG, "NetworkStateEngine: Capabilities changed state = ${_networkState.value}")
        }
    }

    init {
        registerNetworkMonitoring()
    }

    private fun registerNetworkMonitoring() {
        try {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager?.registerNetworkCallback(builder.build(), networkCallback)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register NetworkCallback: ${e.message}")
        }
    }

    fun queueAction(actionType: String, payloadJson: String) {
        val action = PendingNetworkAction(actionType = actionType, payloadJson = payloadJson)
        _pendingQueue.value = _pendingQueue.value + action
        Log.i(TAG, "Action queued while offline/weak: type=$actionType, total queued=${_pendingQueue.value.size}")

        if (_networkState.value == NetworkConnectionState.ONLINE || _networkState.value == NetworkConnectionState.CONNECTED) {
            scope.launch {
                replayPendingActions()
            }
        }
    }

    private suspend fun replayPendingActions() {
        val currentQueue = _pendingQueue.value
        if (currentQueue.isEmpty()) return

        Log.i(TAG, "Replaying ${currentQueue.size} pending network actions after reconnection...")
        _networkState.value = NetworkConnectionState.SYNCING

        for (action in currentQueue) {
            try {
                Log.d(TAG, "Processing queued action: ${action.actionType}")
                // Simulated execution - replace with database or server sync dispatch
                kotlinx.coroutines.delay(200)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed replaying queued action ${action.id}: ${e.message}")
            }
        }

        _pendingQueue.value = emptyList()
        _networkState.value = NetworkConnectionState.ONLINE
        Log.i(TAG, "All queued actions synced successfully!")
    }

    companion object {
        private const val TAG = "NetworkStateEngine"

        @Volatile
        private var INSTANCE: NetworkStateEngine? = null

        fun getInstance(context: Context): NetworkStateEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkStateEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
