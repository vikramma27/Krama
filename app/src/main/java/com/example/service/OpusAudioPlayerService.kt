package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.util.NativeAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android Service wrapper around MediaPlayer and AudioTrack specifically for
 * decoding and playback of 48kHz / 24kbps-32kbps Opus audio streams from WebRTC/messages.
 */
class OpusAudioPlayerService : Service() {

    companion object {
        private const val TAG = "OpusAudioPlayerService"
        private const val CHANNEL_ID = "krama_opus_player_channel"
        private const val NOTIFICATION_ID = 2099

        @Volatile
        private var INSTANCE: OpusAudioPlayerService? = null

        fun getInstance(): OpusAudioPlayerService? = INSTANCE
    }

    data class OpusStreamState(
        val isPlaying: Boolean = false,
        val isOpusDecoded: Boolean = true,
        val sampleRateHz: Int = 48000,
        val bitrateKbps: Int = 32,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0,
        val currentSource: String? = null
    )

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _streamState = MutableStateFlow(OpusStreamState())
    val streamState: StateFlow<OpusStreamState> = _streamState.asStateFlow()

    private var audioCodecProcessor: AudioCodecProcessor? = null
    private var pcmAudioTrack: AudioTrack? = null

    inner class LocalBinder : Binder() {
        fun getService(): OpusAudioPlayerService = this@OpusAudioPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        createNotificationChannel()
        audioCodecProcessor = AudioCodecProcessor(sampleRate = 48000, channels = 1, bitrate = 32000)
        initAudioTrack()
        Log.i(TAG, "OpusAudioPlayerService created and initialized 48kHz AudioTrack.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Opus Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Opus voice stream decoding & audio playback"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun initAudioTrack() {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                48000,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            pcmAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(48000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            pcmAudioTrack?.play()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize PCM AudioTrack for Opus playback: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioPath = intent?.getStringExtra("AUDIO_PATH")
        if (!audioPath.isNullOrEmpty()) {
            playOpusAudioFile(this, audioPath)
        }
        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    fun playOpusAudioFile(context: Context, audioPathOrUrl: String) {
        serviceScope.launch {
            _streamState.value = _streamState.value.copy(
                isPlaying = true,
                currentSource = audioPathOrUrl
            )
            NativeAudioPlayer.playAudio(context, audioPathOrUrl)
        }
    }

    /**
     * Feeds raw encoded Opus frame bytes received via WebRTC or voice socket stream.
     */
    fun feedOpusStreamFrame(opusBytes: ByteArray) {
        serviceScope.launch {
            try {
                val processor = audioCodecProcessor ?: return@launch
                val pcmSamples = processor.decodeAudioFrame(opusBytes)
                pcmAudioTrack?.write(pcmSamples, 0, pcmSamples.size)
                
                _streamState.value = _streamState.value.copy(
                    isPlaying = true,
                    isOpusDecoded = true,
                    currentPositionMs = _streamState.value.currentPositionMs + 20
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Error decoding & playing Opus frame: ${e.message}")
            }
        }
    }

    fun pause() {
        NativeAudioPlayer.pause()
        _streamState.value = _streamState.value.copy(isPlaying = false)
    }

    fun stopPlayback() {
        NativeAudioPlayer.stop()
        pcmAudioTrack?.stop()
        _streamState.value = OpusStreamState()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Opus Voice HD")
            .setContentText("Playing Opus encoded voice stream")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        pcmAudioTrack?.release()
        INSTANCE = null
    }

    private fun String?.isNull_orEmpty_safe(): Boolean = this == null || this.trim().isEmpty()
}
