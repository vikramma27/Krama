package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.WebRtcCallState
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

data class WebRtcDiagnosticData(
    val jitterMs: Float = 12f,
    val packetLossPercent: Float = 0.2f,
    val rttMs: Float = 38f,
    val networkStabilityIndex: Float = 98f,
    val throughputKbps: Float = 280f,
    val audioCodec: String = "Opus 48kHz (Dynamic Bitrate)",
    val iceConnectionState: String = "STUN Direct P2P Active",
    val isNoiseSuppressionEnabled: Boolean = true,
    val isEchoCancellationEnabled: Boolean = true
)

@Composable
fun WebRtcDiagnosticOverlay(
    callState: WebRtcCallState,
    isLowPowerModeEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentMetrics by com.example.service.WebRtcDiagnosticCollector.instance.diagnosticFlow.collectAsStateWithLifecycle()

    val jitterHistory = remember { mutableStateListOf<Float>() }
    val lossHistory = remember { mutableStateListOf<Float>() }
    val stabilityHistory = remember { mutableStateListOf<Float>() }

    val maxSamples = 28

    // Real-Time WebRTC stats collector history updates
    LaunchedEffect(currentMetrics) {
        if (callState == WebRtcCallState.CONNECTED || callState == WebRtcCallState.RINGING) {
            if (jitterHistory.size >= maxSamples) jitterHistory.removeAt(0)
            if (lossHistory.size >= maxSamples) lossHistory.removeAt(0)
            if (stabilityHistory.size >= maxSamples) stabilityHistory.removeAt(0)

            jitterHistory.add(currentMetrics.jitterMs)
            lossHistory.add(currentMetrics.packetLossPercent)
            stabilityHistory.add(currentMetrics.networkStabilityIndex)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Diagnostic Floating Badge Toggle Bar
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkPlumCard.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isExpanded = !isExpanded }
                    .border(1.dp, SoftTeal.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .testTag("webrtc_diagnostic_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    currentMetrics.networkStabilityIndex > 90f -> Color(0xFF4CAF50)
                                    currentMetrics.networkStabilityIndex > 75f -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }
                            )
                    )

                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Diagnostics",
                        tint = SoftTeal,
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = "Realtime WebRTC Diagnostics",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Jitter: ${String.format("%.1f", currentMetrics.jitterMs)}ms | Loss: ${String.format("%.1f", currentMetrics.packetLossPercent)}%",
                        color = SoftTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expandable Real-Time Diagnostic Dashboard Card with D3-inspired Chart
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NearBlackPlum.copy(alpha = 0.95f),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(1.dp, WarmCoral.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .testTag("webrtc_diagnostic_panel")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WebRTC Network Quality & Stability",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid Metrics Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricBox(
                                label = "Jitter Latency",
                                value = "${String.format("%.1f", currentMetrics.jitterMs)} ms",
                                subtext = "Target < 30ms",
                                tint = if (currentMetrics.jitterMs < 25f) Color(0xFF4CAF50) else Color(0xFFFFC107)
                            )

                            MetricBox(
                                label = "Packet Loss",
                                value = "${String.format("%.2f", currentMetrics.packetLossPercent)} %",
                                subtext = "Target < 1.0%",
                                tint = if (currentMetrics.packetLossPercent < 1.0f) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )

                            MetricBox(
                                label = "Stability Index",
                                value = "${currentMetrics.networkStabilityIndex.toInt()} / 100",
                                subtext = "Health Ratio",
                                tint = SoftTeal
                            )

                            MetricBox(
                                label = "Round-Trip",
                                value = "${currentMetrics.rttMs.toInt()} ms",
                                subtext = "${currentMetrics.throughputKbps.toInt()} kbps",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // D3-inspired Smooth Curved Chart
                        Text(
                            text = "Real-Time Stability & Jitter Stream (D3 Bezier Sparkline)",
                            fontSize = 11.sp,
                            color = SoftTeal,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        D3WebRtcSparklineChart(
                            jitterValues = jitterHistory,
                            stabilityValues = stabilityHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkPlumCard)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transport: ${currentMetrics.iceConnectionState}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "Codec: ${currentMetrics.audioCodec}",
                                fontSize = 11.sp,
                                color = SoftTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    subtext: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkPlumCard)
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, color = tint, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun D3WebRtcSparklineChart(
    jitterValues: List<Float>,
    stabilityValues: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (width <= 0 || height <= 0 || jitterValues.size < 2) return@Canvas

        // Draw background grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = (height / gridLines) * i
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // Render Stability Area Graph (Gradient Fill under curve)
        val stabilityPath = Path()
        val stepX = width / (maxOf(1, stabilityValues.size - 1))

        val maxStability = 100f
        val minStability = 50f

        for (i in stabilityValues.indices) {
            val valNorm = (stabilityValues[i] - minStability) / (maxStability - minStability)
            val y = height - (valNorm.coerceIn(0f, 1f) * (height * 0.85f))
            val x = i * stepX

            if (i == 0) {
                stabilityPath.moveTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevValNorm = (stabilityValues[i - 1] - minStability) / (maxStability - minStability)
                val prevY = height - (prevValNorm.coerceIn(0f, 1f) * (height * 0.85f))

                val controlX1 = prevX + (stepX / 2f)
                val controlY1 = prevY
                val controlX2 = prevX + (stepX / 2f)
                val controlY2 = y

                stabilityPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        // Draw Stability stroke & area fill
        val areaPath = Path().apply {
            addPath(stabilityPath)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(SoftTeal.copy(alpha = 0.35f), SoftTeal.copy(alpha = 0.02f))
            )
        )

        drawPath(
            path = stabilityPath,
            color = SoftTeal,
            style = Stroke(width = 3.5f)
        )

        // Render Jitter Line Graph
        val jitterPath = Path()
        val maxJitter = 50f

        for (i in jitterValues.indices) {
            val valNorm = jitterValues[i] / maxJitter
            val y = height - (valNorm.coerceIn(0f, 1f) * (height * 0.75f))
            val x = i * stepX

            if (i == 0) {
                jitterPath.moveTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNorm = jitterValues[i - 1] / maxJitter
                val prevY = height - (prevNorm.coerceIn(0f, 1f) * (height * 0.75f))

                val controlX1 = prevX + (stepX / 2f)
                val controlY1 = prevY
                val controlX2 = prevX + (stepX / 2f)
                val controlY2 = y

                jitterPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        drawPath(
            path = jitterPath,
            color = WarmCoral,
            style = Stroke(width = 2.5f)
        )
    }
}
