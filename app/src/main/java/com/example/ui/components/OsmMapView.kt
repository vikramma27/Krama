package com.example.ui.components

import android.content.Context
import android.preference.PreferenceManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.LocationAndGeofenceManager
import com.example.data.local.UserLocationData
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    initialLat: Double = 37.7749,
    initialLng: Double = -122.4194,
    markerTitle: String = "Location Pin",
    onLocationShareClick: ((UserLocationData) -> Unit)? = null
) {
    val context = LocalContext.current
    val locationAndGeofenceManager = remember { LocationAndGeofenceManager.getInstance(context) }
    val userLocation by locationAndGeofenceManager.currentLocation.collectAsState()

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(context) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        Configuration.getInstance().userAgentValue = context.packageName
        locationAndGeofenceManager.startLocationUpdates()

        onDispose {
            locationAndGeofenceManager.stopLocationUpdates()
            mapViewRef?.onDetach()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)

                    val startPoint = GeoPoint(initialLat, initialLng)
                    controller.setCenter(startPoint)

                    val startMarker = Marker(this).apply {
                        position = startPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = markerTitle
                        snippet = "Lat: ${String.format("%.4f", initialLat)}, Lng: ${String.format("%.4f", initialLng)}"
                    }
                    overlays.add(startMarker)
                    mapViewRef = this
                }
            },
            update = { mapView ->
                if (userLocation.latitude != 37.7749 || userLocation.longitude != -122.4194) {
                    val geoPoint = GeoPoint(userLocation.latitude, userLocation.longitude)
                    mapView.overlays.removeAll { it is Marker && it.title == "My Live Location" }
                    val liveMarker = Marker(mapView).apply {
                        position = geoPoint
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "My Live Location"
                        snippet = "Accuracy: ${userLocation.accuracy}m"
                    }
                    mapView.overlays.add(liveMarker)
                    mapView.invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Floating Recenter & Share Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    mapViewRef?.controller?.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
                    mapViewRef?.controller?.setZoom(16.0)
                },
                modifier = Modifier.size(44.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center Location",
                    modifier = Modifier.size(20.dp)
                )
            }

            if (onLocationShareClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = { onLocationShareClick(userLocation) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.ShareLocation,
                        contentDescription = "Share Live Location",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "OpenStreetMap (OSMDroid)",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
