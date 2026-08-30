package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.launch
import kotlin.random.Random

data class WebRtcTelemetrySample(
    val jitterMs: Float,       // e.g. 2ms - 25ms
    val packetLossPct: Float,  // e.g. 0.0% - 4.5%
    val bitrateKbps: Float     // e.g. 120kbps - 320kbps
)

@Composable
fun WebRtcDiagnosticsChart(
    modifier: Modifier = Modifier
) {
    val samples = remember {
        mutableStateListOf(
            WebRtcTelemetrySample(4.2f, 0.1f, 240f),
            WebRtcTelemetrySample(6.1f, 0.2f, 256f),
            WebRtcTelemetrySample(8.5f, 0.4f, 210f),
            WebRtcTelemetrySample(12.0f, 1.2f, 180f),
            WebRtcTelemetrySample(7.3f, 0.3f, 230f),
            WebRtcTelemetrySample(5.0f, 0.0f, 250f),
            WebRtcTelemetrySample(6.8f, 0.1f, 245f),
            WebRtcTelemetrySample(14.2f, 1.8f, 160f),
            WebRtcTelemetrySample(9.1f, 0.5f, 220f),
            WebRtcTelemetrySample(4.5f, 0.0f, 256f)
        )
    }

    val chartAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isPinging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        chartAnim.animateTo(1f, animationSpec = tween(1000))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("webrtc_diagnostics_card"),
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
                    Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = SoftTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "WebRTC Connection Diagnostics",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Real-time Jitter (ms) & Packet Loss (%) telemetry",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D3-inspired Canvas Line/Bar Combined Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NearBlackPlum)
                    .padding(12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val pointCount = samples.size
                    if (pointCount < 2) return@Canvas

                    val stepX = width / (pointCount - 1)
                    val maxJitter = 25f

                    // Draw Horizontal Gridlines (D3 Style)
                    for (i in 0..3) {
                        val y = height * (i / 3f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 1. Draw Packet Loss Bar Chart Background
                    samples.forEachIndexed { index, sample ->
                        val barHeight = (sample.packetLossPct / 5.0f).coerceIn(0f, 1f) * height * chartAnim.value
                        val barX = index * stepX - 6.dp.toPx()
                        drawRect(
                            color = WarmCoral.copy(alpha = 0.35f),
                            topLeft = Offset(barX.coerceAtLeast(0f), height - barHeight),
                            size = Size(12.dp.toPx(), barHeight)
                        )
                    }

                    // 2. Build Smooth Jitter Curve Path
                    val jitterPath = Path()
                    val areaPath = Path()

                    samples.forEachIndexed { index, sample ->
                        val x = index * stepX
                        val normalizedY = (1f - (sample.jitterMs / maxJitter).coerceIn(0f, 1f)) * height * chartAnim.value
                        val y = height - (height - normalizedY)

                        if (index == 0) {
                            jitterPath.moveTo(x, y)
                            areaPath.moveTo(x, height)
                            areaPath.lineTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevSample = samples[index - 1]
                            val prevY = height - ((1f - (prevSample.jitterMs / maxJitter).coerceIn(0f, 1f)) * height * chartAnim.value)
                            val controlX1 = prevX + (x - prevX) / 2f
                            val controlX2 = prevX + (x - prevX) / 2f

                            jitterPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                            areaPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        }

                        if (index == pointCount - 1) {
                            areaPath.lineTo(x, height)
                            areaPath.close()
                        }
                    }

                    // Draw Gradient Area below Jitter curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(SoftTeal.copy(alpha = 0.35f), Color.Transparent)
                        )
                    )

                    // Draw Jitter Stroke Path
                    drawPath(
                        path = jitterPath,
                        color = SoftTeal,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    // Draw Data Points
                    samples.forEachIndexed { index, sample ->
                        val x = index * stepX
                        val y = height - ((1f - (sample.jitterMs / maxJitter).coerceIn(0f, 1f)) * height * chartAnim.value)
                        drawCircle(
                            color = SoftTeal,
                            radius = 3.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Legend & Telemetry Indicators
            val avgJitter = samples.map { it.jitterMs }.average()
            val avgLoss = samples.map { it.packetLossPct }.average()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(SoftTeal))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Jitter: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${String.format("%.1f", avgJitter)} ms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(WarmCoral.copy(alpha = 0.7f)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Packet Loss: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("${String.format("%.2f", avgLoss)}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Opus Codec: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text("24kbps", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    scope.launch {
                        isPinging = true
                        val newJitter = (3f + Random.nextFloat() * 12f)
                        val newLoss = (if (Random.nextBoolean()) 0.0f else Random.nextFloat() * 1.5f)
                        val newBitrate = (200f + Random.nextFloat() * 80f)
                        if (samples.size >= 12) samples.removeAt(0)
                        samples.add(WebRtcTelemetrySample(newJitter, newLoss, newBitrate))
                        chartAnim.snapTo(0f)
                        chartAnim.animateTo(1f, animationSpec = tween(600))
                        isPinging = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("run_ping_diagnostic_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = NearBlackPlum)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isPinging) "Sampling WebRTC STUN/TURN Pings..." else "Run STUN/TURN Ping Test", color = NearBlackPlum, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
