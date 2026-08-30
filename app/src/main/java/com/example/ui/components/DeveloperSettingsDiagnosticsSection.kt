package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.service.WebRtcDiagnosticCollector
import com.example.service.WebRtcStatsLog
import com.example.service.WebRtcStatsLogger
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import java.util.Locale

/**
 * Dedicated Developer Settings diagnostic section surfacing automated WebRTC telemetry logs,
 * real-time jitter, packet loss, latency stats, and log export utilities.
 */
@Composable
fun DeveloperSettingsDiagnosticsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentData by WebRtcDiagnosticCollector.instance.diagnosticFlow.collectAsStateWithLifecycle()
    val logs by WebRtcStatsLogger.instance.logsFlow.collectAsStateWithLifecycle()

    var isCapturing by remember { mutableStateOf(true) }
    var showRawLogTable by remember { mutableStateOf(true) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_diagnostics_section_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkPlumCard)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Header Row
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
                            .background(WarmCoral.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = WarmCoral, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Developer Diagnostics & WebRTC Logger",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Automated Real-Time Telemetry & Stats Log Engine",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCapturing) SoftTeal.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isCapturing) "LIVE CAPTURE" else "PAUSED",
                        color = if (isCapturing) SoftTeal else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-time metric indicators grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "JITTER",
                    value = String.format(Locale.US, "%.1f ms", currentData.jitterMs),
                    color = if (currentData.jitterMs < 15f) SoftTeal else WarmCoral,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "PACKET LOSS",
                    value = String.format(Locale.US, "%.2f%%", currentData.packetLossPercent),
                    color = if (currentData.packetLossPercent < 0.5f) SoftTeal else WarmCoral,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "LATENCY / RTT",
                    value = String.format(Locale.US, "%.0f ms", currentData.rttMs),
                    color = if (currentData.rttMs < 60f) SoftTeal else WarmCoral,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions bar: Start/Pause, Export Dump, Clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isCapturing) {
                            WebRtcDiagnosticCollector.instance.stopCollecting()
                            isCapturing = false
                        } else {
                            WebRtcDiagnosticCollector.instance.startCollecting()
                            isCapturing = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing) WarmCoral else SoftTeal
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCapturing) "Pause Logging" else "Resume Logging",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        val textDump = WebRtcStatsLogger.instance.exportLogsAsFormattedText()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("WebRTC Diagnostic Log Dump", textDump)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied ${logs.size} WebRTC diagnostic log entries to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Log Dump", color = SoftTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        WebRtcStatsLogger.instance.clearLogs()
                        Toast.makeText(context, "Cleared diagnostic log history.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NearBlackPlum)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Automated Log Stream Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTOMATED WEBRTC STATS LOGS (${logs.size})",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = if (showRawLogTable) "Hide Table" else "Show Table",
                    color = SoftTeal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("toggle_log_table_button")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable Log Entries
            if (showRawLogTable) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NearBlackPlum)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            Text("No diagnostic logs recorded yet.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(logs.reversed()) { log ->
                                LogRowItem(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NearBlackPlum
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LogRowItem(log: WebRtcStatsLog) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = DarkPlumCard.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (log.logLevel) {
                            "ERROR" -> WarmCoral
                            "WARN" -> Color(0xFFFFB74D)
                            else -> SoftTeal.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = log.logLevel,
                            color = if (log.logLevel == "ERROR") Color.White else SoftTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(log.formattedTime, color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Text(log.iceState, color = SoftTeal, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Jitter: ${String.format(Locale.US, "%.1f", log.jitterMs)}ms | Loss: ${String.format(Locale.US, "%.2f", log.packetLossPercent)}% | Latency: ${String.format(Locale.US, "%.1f", log.latencyMs)}ms | Rate: ${log.throughputKbps.toInt()}kbps",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            if (log.eventNote.isNotEmpty()) {
                Text(
                    text = log.eventNote,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
