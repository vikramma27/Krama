package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Real-time microphone audio amplitude monitor that reads PCM samples from AudioRecord
 * and calculates live volume amplitude (0.0f to 1.0f) and frequency bar distributions
 * for the calling UI waveform visualizer.
 */
class AudioAmplitudeMonitor private constructor() {

    private val monitorScope = CoroutineScope(Dispatchers.Default + Job())
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _waveformBars = MutableStateFlow(List(9) { 0.1f })
    val waveformBars: StateFlow<List<Float>> = _waveformBars.asStateFlow()

    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun startMonitoring(context: Context) {
        if (isRecording) return

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            android.util.Log.w("AudioAmplitudeMonitor", "RECORD_AUDIO permission missing. Using simulated audio feedback fallback.")
            startFallbackSimulation()
            return
        }

        recordingJob = monitorScope.launch {
            isRecording = true
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, 2048)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    android.util.Log.w("AudioAmplitudeMonitor", "AudioRecord initialization failed. Falling back to simulated input.")
                    startFallbackSimulation()
                    return@launch
                }

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize / 2)

                while (isRecording && isActive) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sumSquares = 0.0
                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sumSquares += sample * sample
                        }
                        val rms = sqrt(sumSquares / readSize)
                        // Normalize RMS (0 .. 32768) to 0.0f .. 1.0f
                        val normalizedAmplitude = (rms / 3000.0).coerceIn(0.02, 1.0).toFloat()
                        _amplitude.value = normalizedAmplitude

                        // Generate 9 dynamic waveform bar heights based on live microphone amplitude
                        val currentBars = MutableList(9) { idx ->
                            val phaseShift = (idx - 4) * 0.35
                            val factor = maxOf(0.15f, 1.0f - kotlin.math.abs(idx - 4) * 0.15f)
                            val barVal = (normalizedAmplitude * factor * (0.8f + (kotlin.math.sin((System.currentTimeMillis() * 0.01) + phaseShift).toFloat() * 0.2f))).coerceIn(0.08f, 1.0f)
                            barVal
                        }
                        _waveformBars.value = currentBars
                    }
                    delay(50) // ~20fps visual updates
                }
            } catch (e: Throwable) {
                android.util.Log.e("AudioAmplitudeMonitor", "Error during microphone recording: ${e.message}")
                startFallbackSimulation()
            } finally {
                stopAudioRecord()
            }
        }
    }

    private fun startFallbackSimulation() {
        recordingJob?.cancel()
        recordingJob = monitorScope.launch {
            isRecording = true
            var tick = 0
            while (isRecording && isActive) {
                tick++
                val simulatedAmplitude = (0.15f + (kotlin.math.sin(tick * 0.3).toFloat() * 0.25f)).coerceAtLeast(0.0f)
                _amplitude.value = simulatedAmplitude
                val bars = List(9) { idx ->
                    val factor = maxOf(0.2f, 1.0f - kotlin.math.abs(idx - 4) * 0.18f)
                    (simulatedAmplitude * factor * (0.8f + (kotlin.math.sin((tick * 0.4) + idx).toFloat() * 0.2f))).coerceIn(0.08f, 1.0f)
                }
                _waveformBars.value = bars
                delay(60)
            }
        }
    }

    fun stopMonitoring() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        stopAudioRecord()
        _amplitude.value = 0f
        _waveformBars.value = List(9) { 0.08f }
    }

    private fun stopAudioRecord() {
        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Throwable) {
            android.util.Log.w("AudioAmplitudeMonitor", "AudioRecord release note: ${e.message}")
        } finally {
            audioRecord = null
        }
    }

    companion object {
        val instance: AudioAmplitudeMonitor by lazy { AudioAmplitudeMonitor() }
    }
}
