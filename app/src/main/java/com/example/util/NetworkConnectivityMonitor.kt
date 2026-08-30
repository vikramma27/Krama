package com.example.util

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

class NetworkConnectivityMonitor private constructor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isConnected = MutableStateFlow(checkInitialConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _networkStatus = MutableStateFlow(
        if (_isConnected.value) NetworkStatus.AVAILABLE else NetworkStatus.UNAVAILABLE
    )
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.i(TAG, "Network connection restored (NET_CAPABILITY_INTERNET).")
            _isConnected.value = true
            _networkStatus.value = NetworkStatus.AVAILABLE
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
            Log.w(TAG, "Network connection losing bandwidth...")
            _networkStatus.value = NetworkStatus.LOSING
        }

        override fun onLost(network: Network) {
            Log.w(TAG, "Network connection lost.")
            _isConnected.value = false
            _networkStatus.value = NetworkStatus.LOST
        }

        override fun onUnavailable() {
            Log.w(TAG, "Network unavailable.")
            _isConnected.value = false
            _networkStatus.value = NetworkStatus.UNAVAILABLE
        }
    }

    init {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register network callback: ${e.message}", e)
        }
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Throwable) {
            Log.w(TAG, "Error checking initial network connectivity: ${e.message}")
            false
        }
    }

    fun isCurrentlyOnline(): Boolean {
        return checkInitialConnectivity()
    }

    companion object {
        private const val TAG = "NetworkMonitor"

        @Volatile
        private var INSTANCE: NetworkConnectivityMonitor? = null

        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectivityMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
