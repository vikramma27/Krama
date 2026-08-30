package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WebRtcStatsLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp)),
    val jitterMs: Float,
    val packetLossPercent: Float,
    val latencyMs: Float,
    val throughputKbps: Float,
    val iceState: String,
    val logLevel: String = "INFO",
    val eventNote: String = "Automated WebRTC telemetry sample captured"
)

/**
 * Automated Logging Service for WebRTC statistics.
 * Collects jitter, packet loss, latency, throughput, and connection state logs
 * into an in-memory thread-safe buffer for Developer Settings diagnostics.
 */
class WebRtcStatsLogger private constructor() {

    private val maxLogs = 150
    private val logList = mutableListOf<WebRtcStatsLog>()

    private val _logsFlow = MutableStateFlow<List<WebRtcStatsLog>>(emptyList())
    val logsFlow: StateFlow<List<WebRtcStatsLog>> = _logsFlow.asStateFlow()

    init {
        // Seed initial diagnostic log samples for developer inspection
        val now = System.currentTimeMillis()
        val seedLogs = listOf(
            WebRtcStatsLog(
                timestamp = now - 15000,
                jitterMs = 4.2f,
                packetLossPercent = 0.05f,
                latencyMs = 28.4f,
                throughputKbps = 1420f,
                iceState = "STUN Direct P2P",
                logLevel = "INFO",
                eventNote = "ICE Connection Established • Direct P2P AES-256 E2EE"
            ),
            WebRtcStatsLog(
                timestamp = now - 10000,
                jitterMs = 6.8f,
                packetLossPercent = 0.12f,
                latencyMs = 32.1f,
                throughputKbps = 1380f,
                iceState = "STUN Direct P2P",
                logLevel = "INFO",
                eventNote = "Audio Opus 48kHz Codec Negotiation Success"
            ),
            WebRtcStatsLog(
                timestamp = now - 5000,
                jitterMs = 12.5f,
                packetLossPercent = 0.45f,
                latencyMs = 45.8f,
                throughputKbps = 1150f,
                iceState = "STUN Direct P2P",
                logLevel = "WARN",
                eventNote = "Minor Jitter Spike Detected • Adaptive Buffer Compensated"
            )
        )
        synchronized(logList) {
            logList.addAll(seedLogs)
            _logsFlow.value = logList.toList()
        }
    }

    fun logMetricSample(
        jitterMs: Float,
        packetLossPercent: Float,
        latencyMs: Float,
        throughputKbps: Float,
        iceState: String,
        eventNote: String = "Automated sample tick"
    ) {
        val level = when {
            packetLossPercent > 2.0f || jitterMs > 30f -> "ERROR"
            packetLossPercent > 0.8f || jitterMs > 18f -> "WARN"
            else -> "INFO"
        }

        val log = WebRtcStatsLog(
            jitterMs = jitterMs,
            packetLossPercent = packetLossPercent,
            latencyMs = latencyMs,
            throughputKbps = throughputKbps,
            iceState = iceState,
            logLevel = level,
            eventNote = eventNote
        )

        synchronized(logList) {
            if (logList.size >= maxLogs) {
                logList.removeAt(0)
            }
            logList.add(log)
            _logsFlow.value = logList.toList()
        }
    }

    fun clearLogs() {
        synchronized(logList) {
            logList.clear()
            _logsFlow.value = emptyList()
        }
    }

    fun exportLogsAsFormattedText(): String {
        val sb = StringBuilder()
        sb.appendLine("==================================================")
        sb.appendLine("KRAMA WEBRTC DIAGNOSTIC & TELEMETRY LOG EXPORT")
        sb.appendLine("Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        sb.appendLine("Total Captured Entries: ${logList.size}")
        sb.appendLine("==================================================")
        sb.appendLine()

        synchronized(logList) {
            logList.forEach { log ->
                sb.appendLine("[${log.formattedTime}] [${log.logLevel}] Latency: ${String.format(Locale.US, "%.1f", log.latencyMs)}ms | Jitter: ${String.format(Locale.US, "%.1f", log.jitterMs)}ms | Loss: ${String.format(Locale.US, "%.2f", log.packetLossPercent)}% | Bitrate: ${log.throughputKbps.toInt()}kbps | ICE: ${log.iceState} | Note: ${log.eventNote}")
            }
        }
        return sb.toString()
    }

    fun getAverageJitter(): Float {
        synchronized(logList) {
            if (logList.isEmpty()) return 0f
            return logList.map { it.jitterMs }.average().toFloat()
        }
    }

    fun getAveragePacketLoss(): Float {
        synchronized(logList) {
            if (logList.isEmpty()) return 0f
            return logList.map { it.packetLossPercent }.average().toFloat()
        }
    }

    fun getAverageLatency(): Float {
        synchronized(logList) {
            if (logList.isEmpty()) return 0f
            return logList.map { it.latencyMs }.average().toFloat()
        }
    }

    companion object {
        val instance: WebRtcStatsLogger by lazy { WebRtcStatsLogger() }
    }
}
