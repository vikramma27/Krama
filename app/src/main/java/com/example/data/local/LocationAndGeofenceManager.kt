package com.example.data.local

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserLocationData(
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val accuracy: Float = 5f,
    val timestamp: Long = System.currentTimeMillis()
)

class LocationAndGeofenceManager private constructor(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val _currentLocation = MutableStateFlow(UserLocationData())
    val currentLocation: StateFlow<UserLocationData> = _currentLocation.asStateFlow()

    private val locationListener = LocationListener { location ->
        onLocationUpdated(location)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                onLocationUpdated(location)
            }
        }
    }

    fun startLocationUpdates() {
        try {
            // 1. Try FusedLocationProviderClient first
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onLocationUpdated(loc)
                }
            }

            // 2. Also register standard LocationManager as backup
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
            } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    10f,
                    locationListener
                )
            }
            // Fetch last known location immediately
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = lastGps ?: lastNetwork
            if (best != null) {
                onLocationUpdated(best)
            }
            Log.i(TAG, "Location updates started successfully with FusedLocationProviderClient & LocationManager.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing or denied: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
        }
    }

    fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationManager.removeUpdates(locationListener)
            Log.i(TAG, "Location updates stopped.")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping location updates: ${e.message}")
        }
    }

    private fun onLocationUpdated(location: Location) {
        _currentLocation.value = UserLocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.time
        )
        Log.d(TAG, "Location updated: lat=${location.latitude}, lng=${location.longitude}")
    }

    companion object {
        private const val TAG = "LocationGeofenceMgr"

        @Volatile
        private var INSTANCE: LocationAndGeofenceManager? = null

        fun getInstance(context: Context): LocationAndGeofenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationAndGeofenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
