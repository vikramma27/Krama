package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.util.WebRtcStabilityAnalytics
import java.util.Locale

/**
 * Visualizes WebRTC connection stability patterns and call quality over time using Canvas line charts.
 */
@Composable
fun WebRtcStabilityChart(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analytics = remember { WebRtcStabilityAnalytics.getInstance(context) }
    val sessions by analytics.sessionsFlow.collectAsStateWithLifecycle()

    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(sessions) {
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    val avgStability = remember(sessions) {
        if (sessions.isEmpty()) 95f else sessions.map { it.stabilityScore }.average().toFloat()
    }

    val avgJitter = remember(sessions) {
        if (sessions.isEmpty()) 5f else sessions.map { it.avgJitterMs }.average().toFloat()
    }

    val avgLatency = remember(sessions) {
        if (sessions.isEmpty()) 30f else sessions.map { it.avgLatencyMs }.average().toFloat()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("webrtc_stability_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SoftTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "WebRTC Call Quality & Stability",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Local Connection Analytics Over Time",
                            color = SoftTeal,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftTeal.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% Local Only",
                            color = SoftTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Summary Stats KPI Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Avg Stability Score
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NearBlackPlum
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVG STABILITY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f%%", avgStability),
                            color = if (avgStability >= 90f) SoftTeal else WarmCoral,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Avg Jitter
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NearBlackPlum
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVG JITTER", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f ms", avgJitter),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Avg Latency
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = NearBlackPlum
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVG LATENCY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.0f ms", avgLatency),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "CALL QUALITY STABILITY TREND (%)",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Line Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NearBlackPlum)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (sessions.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                        val width = size.width
                        val height = size.height

                        val minVal = 50f
                        val maxVal = 100f

                        val points = sessions.mapIndexed { index, session ->
                            val x = (index.toFloat() / (sessions.size - 1).coerceAtLeast(1)) * width
                            val normalizedY = ((session.stabilityScore - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                            val y = height - (normalizedY * height * animProgress.value)
                            Offset(x, y)
                        }

                        // Grid Reference Lines (90% target stability)
                        val targetY = height - (((90f - minVal) / (maxVal - minVal)) * height)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, targetY),
                            end = Offset(width, targetY),
                            strokeWidth = 2f
                        )

                        if (points.size >= 2) {
                            val path = Path().apply {
                                moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    val p0 = points[i - 1]
                                    val p1 = points[i]
                                    val controlX1 = p0.x + (p1.x - p0.x) / 2
                                    val controlY1 = p0.y
                                    val controlX2 = p0.x + (p1.x - p0.x) / 2
                                    val controlY2 = p1.y
                                    cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                }
                            }

                            // Gradient Fill beneath curve
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(SoftTeal.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )

                            // Stroke line
                            drawPath(
                                path = path,
                                color = SoftTeal,
                                style = Stroke(width = 4f)
                            )

                            // Data points
                            points.forEach { point ->
                                drawCircle(
                                    color = SoftTeal,
                                    radius = 5f,
                                    center = point
                                )
                                drawCircle(
                                    color = NearBlackPlum,
                                    radius = 2.5f,
                                    center = point
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recent Call Sessions List
            Text(
                text = "RECENT CALL SESSIONS",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                sessions.takeLast(4).reversed().forEach { session ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(NearBlackPlum)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (session.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = null,
                                tint = SoftTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "${session.formattedDate} • ${session.durationSeconds / 60}m ${session.durationSeconds % 60}s",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Jitter: ${String.format(Locale.US, "%.1f", session.avgJitterMs)}ms | Loss: ${String.format(Locale.US, "%.2f", session.maxPacketLossPct)}%",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (session.networkType) {
                                    "WiFi" -> SoftTeal.copy(alpha = 0.2f)
                                    else -> WarmCoral.copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = session.networkType,
                                    color = if (session.networkType == "WiFi") SoftTeal else WarmCoral,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.US, "%.1f%%", session.stabilityScore),
                                color = if (session.stabilityScore >= 90f) SoftTeal else WarmCoral,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
