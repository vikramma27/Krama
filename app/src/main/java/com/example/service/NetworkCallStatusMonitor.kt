package com.example.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NetworkTypeLabel {
    WIFI,
    CELLULAR_4G,
    CELLULAR_5G,
    METERED_LOW_BANDWIDTH,
    RECONNECTING
}

data class NetworkCallStatus(
    val networkType: NetworkTypeLabel = NetworkTypeLabel.WIFI,
    val displayLabel: String = "WiFi • HD 1080p",
    val signalStrengthPercent: Int = 95,
    val isConnected: Boolean = true,
    val isMetered: Boolean = false
)

/**
 * Monitors network connection status during WebRTC calls using Android ConnectivityManager NetworkCallbacks.
 * Dynamically provides real-time connection strength (WiFi, 4G, 5G, Reconnecting, Low Bandwidth) as a StateFlow.
 */
class NetworkCallStatusMonitor private constructor() {

    private val _networkStatus = MutableStateFlow(NetworkCallStatus())
    val networkStatus: StateFlow<NetworkCallStatus> = _networkStatus.asStateFlow()

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun startMonitoring(context: Context) {
        if (networkCallback != null) return

        val appContext = context.applicationContext
        connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        // Initial state assessment
        updateCurrentNetworkState()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateCurrentNetworkState()
            }

            override fun onLost(network: Network) {
                _networkStatus.value = NetworkCallStatus(
                    networkType = NetworkTypeLabel.RECONNECTING,
                    displayLabel = "Reconnecting...",
                    signalStrengthPercent = 20,
                    isConnected = false
                )
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                evaluateCapabilities(networkCapabilities)
            }
        }

        try {
            networkCallback?.let {
                connectivityManager?.registerNetworkCallback(request, it)
            }
        } catch (e: Throwable) {
            android.util.Log.w("NetworkCallMonitor", "Error registering network callback: ${e.message}")
        }
    }

    private fun updateCurrentNetworkState() {
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork
        if (activeNetwork == null) {
            _networkStatus.value = NetworkCallStatus(
                networkType = NetworkTypeLabel.RECONNECTING,
                displayLabel = "Reconnecting...",
                signalStrengthPercent = 10,
                isConnected = false
            )
            return
        }

        val caps = cm.getNetworkCapabilities(activeNetwork)
        if (caps != null) {
            evaluateCapabilities(caps)
        } else {
            _networkStatus.value = NetworkCallStatus(
                networkType = NetworkTypeLabel.RECONNECTING,
                displayLabel = "Reconnecting...",
                signalStrengthPercent = 15,
                isConnected = false
            )
        }
    }

    private fun evaluateCapabilities(caps: NetworkCapabilities) {
        val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

        val downstreamKbps = caps.linkDownstreamBandwidthKbps

        when {
            hasWifi -> {
                _networkStatus.value = NetworkCallStatus(
                    networkType = NetworkTypeLabel.WIFI,
                    displayLabel = "WiFi • HD Quality",
                    signalStrengthPercent = 95,
                    isConnected = true,
                    isMetered = false
                )
            }
            hasCellular -> {
                val is5G = downstreamKbps > 50_000
                val isLowBandwidth = downstreamKbps < 1_000
                if (isLowBandwidth) {
                    _networkStatus.value = NetworkCallStatus(
                        networkType = NetworkTypeLabel.METERED_LOW_BANDWIDTH,
                        displayLabel = "4G • Low Bandwidth",
                        signalStrengthPercent = 45,
                        isConnected = true,
                        isMetered = isMetered
                    )
                } else if (is5G) {
                    _networkStatus.value = NetworkCallStatus(
                        networkType = NetworkTypeLabel.CELLULAR_5G,
                        displayLabel = "5G • Ultra HD",
                        signalStrengthPercent = 98,
                        isConnected = true,
                        isMetered = isMetered
                    )
                } else {
                    _networkStatus.value = NetworkCallStatus(
                        networkType = NetworkTypeLabel.CELLULAR_4G,
                        displayLabel = "4G • Clear Stream",
                        signalStrengthPercent = 80,
                        isConnected = true,
                        isMetered = isMetered
                    )
                }
            }
            else -> {
                _networkStatus.value = NetworkCallStatus(
                    networkType = NetworkTypeLabel.RECONNECTING,
                    displayLabel = "Reconnecting...",
                    signalStrengthPercent = 25,
                    isConnected = false
                )
            }
        }
    }

    fun stopMonitoring() {
        try {
            networkCallback?.let {
                connectivityManager?.unregisterNetworkCallback(it)
            }
        } catch (e: Throwable) {}
        networkCallback = null
        connectivityManager = null
    }

    companion object {
        val instance: NetworkCallStatusMonitor by lazy { NetworkCallStatusMonitor() }
    }
}
