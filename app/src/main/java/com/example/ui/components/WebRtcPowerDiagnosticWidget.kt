package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay

@Composable
fun WebRtcPowerDiagnosticWidget(
    isCallActive: Boolean = false,
    isLowPowerModeEnabled: Boolean = false,
    onToggleLowPowerMode: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var batteryLevel by remember { mutableIntStateOf(85) }
    var batteryTempCelsius by remember { mutableFloatStateOf(32.5f) }
    var isCharging by remember { mutableStateOf(false) }

    // Read system battery status
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(cntx: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100) / scale
                    }

                    val tempRaw = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                    batteryTempCelsius = tempRaw / 10.0f

                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {}
        }
    }

    // Dynamic call power simulation based on mode & call state
    var estimatedMahPerHour by remember { mutableIntStateOf(320) }
    var powerDrainPercentPerHour by remember { mutableFloatStateOf(8.5f) }

    LaunchedEffect(isCallActive, isLowPowerModeEnabled, batteryLevel) {
        while (true) {
            val baseDrain = if (isCallActive) 280 else 45
            val videoCodecLoad = if (isCallActive) 140 else 0
            val powerSaverReduction = if (isLowPowerModeEnabled) 130 else 0

            val currentMah = (baseDrain + videoCodecLoad - powerSaverReduction).coerceAtLeast(35)
            estimatedMahPerHour = currentMah
            powerDrainPercentPerHour = (currentMah / 40.0f)

            delay(2000)
        }
    }

    val statusColor = when {
        powerDrainPercentPerHour < 10f -> SoftTeal
        powerDrainPercentPerHour < 20f -> Color(0xFFFFB74D)
        else -> WarmCoral
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .testTag("webrtc_power_diagnostic_widget")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Call Power & Battery Diagnostics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isCallActive) "⚡ WebRTC Active • Real-time Profiler" else "💤 Idle • Battery Baseline Normal",
                            color = if (isCallActive) SoftTeal else Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", powerDrainPercentPerHour)}%/hr",
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricChip(
                    label = "Battery Level",
                    value = "$batteryLevel%",
                    icon = Icons.Default.BatteryAlert,
                    tint = if (batteryLevel < 20) WarmCoral else Color.White
                )

                MetricChip(
                    label = "Power Draw",
                    value = "~$estimatedMahPerHour mAh",
                    icon = Icons.Default.Speed,
                    tint = statusColor
                )

                MetricChip(
                    label = "Temperature",
                    value = "${String.format("%.1f", batteryTempCelsius)}°C",
                    icon = Icons.Default.Thermostat,
                    tint = if (batteryTempCelsius > 38f) WarmCoral else SoftTeal
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Power Consumption Canvas Bar Graph
            Text(
                text = "WebRTC Subsystem Power Allocation",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(NearBlackPlum)
            ) {
                val totalWidth = size.width
                val height = size.height

                val audioWidth = totalWidth * 0.25f
                val videoWidth = if (isCallActive) totalWidth * 0.45f else totalWidth * 0.05f
                val networkWidth = totalWidth * 0.20f

                // Audio subsystem block
                drawRoundRect(
                    color = SoftTeal,
                    topLeft = Offset(0f, 0f),
                    size = Size(audioWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Video/Codec subsystem block
                drawRoundRect(
                    color = WarmCoral,
                    topLeft = Offset(audioWidth + 4f, 0f),
                    size = Size(videoWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Network & Encryption block
                drawRoundRect(
                    color = Color(0xFFFFB74D),
                    topLeft = Offset(audioWidth + videoWidth + 8f, 0f),
                    size = Size(networkWidth, height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem("Opus Audio (25%)", SoftTeal)
                LegendItem(if (isCallActive) "WebRTC Codec (45%)" else "Codec Idle (5%)", WarmCoral)
                LegendItem("DTLS / E2EE (20%)", Color(0xFFFFB74D))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Low Power Battery Saver Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NearBlackPlum)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatterySaver,
                        contentDescription = null,
                        tint = SoftTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Low Power Battery Saver",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reduces video frame rate & audio sampling rate during calls",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isLowPowerModeEnabled,
                    onCheckedChange = onToggleLowPowerMode,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SoftTeal
                    ),
                    modifier = Modifier.testTag("power_saver_toggle")
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NearBlackPlum)
            .padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}
