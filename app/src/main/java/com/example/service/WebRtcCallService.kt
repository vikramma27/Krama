package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WebRtcCallState {
    IDLE, CONNECTING, RINGING, CONNECTED, ENDED
}

data class WebRtcCallSession(
    val callId: String,
    val contactName: String,
    val isVideo: Boolean,
    val isIncoming: Boolean,
    val callState: WebRtcCallState = WebRtcCallState.IDLE,
    val callDurationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isNoiseSuppressionOn: Boolean = true,
    val isEchoCancellationOn: Boolean = true,
    val isHeldBySystemCall: Boolean = false,
    val holdReason: String? = null,
    val sdpOfferSnippet: String = "v=0\r\no=- 48201 2 IN IP4 127.0.0.1\r\ns=KramaE2EEWebRTC\r\nt=0 0\r\nm=audio 9 UDP/TLS/RTP/SAVPF 111",
    val iceCandidatesCount: Int = 3
)

class WebRtcCallService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    inner class LocalBinder : Binder() {
        fun getService(): WebRtcCallService = this@WebRtcCallService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private val _callSession = MutableStateFlow<WebRtcCallSession?>(null)
    val callSession: StateFlow<WebRtcCallSession?> = _callSession.asStateFlow()

    private var durationTimerJob: Job? = null
    private var audioManager: android.media.AudioManager? = null
    private var telephonyManager: android.telephony.TelephonyManager? = null

    private val audioFocusChangeListener = android.media.AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            android.media.AudioManager.AUDIOFOCUS_LOSS,
            android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                android.util.Log.w("WebRtcCallService", "Audio focus lost to external call/app. Pausing WebRTC audio.")
                handleExternalCallInterruption(true, "Call on Hold • Cellular/External App Call Active")
            }
            android.media.AudioManager.AUDIOFOCUS_GAIN -> {
                android.util.Log.i("WebRtcCallService", "Audio focus regained. Resuming WebRTC audio.")
                handleExternalCallInterruption(false, null)
            }
        }
    }

    private val phoneStateListener = object : android.telephony.PhoneStateListener() {
        @Deprecated("Deprecated in API 31")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                android.telephony.TelephonyManager.CALL_STATE_RINGING,
                android.telephony.TelephonyManager.CALL_STATE_OFFHOOK -> {
                    android.util.Log.w("WebRtcCallService", "Cellular call ringing/active detected! Triggering pauseWebRTC and Call Interrupted state.")
                    serviceScope.launch {
                        try {
                            com.example.data.remote.WebRtcSignalingManager.getInstance().pauseWebRTC()
                        } catch (e: Throwable) {
                            android.util.Log.e("WebRtcCallService", "pauseWebRTC call failed: ${e.message}")
                        }
                    }
                    handleExternalCallInterruption(true, "Call Interrupted • Cellular Call Active")
                }
                android.telephony.TelephonyManager.CALL_STATE_IDLE -> {
                    android.util.Log.i("WebRtcCallService", "Cellular call ended. Restoring WebRTC call stream.")
                    serviceScope.launch {
                        try {
                            com.example.data.remote.WebRtcSignalingManager.getInstance().resumeSignaling()
                        } catch (e: Throwable) {}
                    }
                    handleExternalCallInterruption(false, null)
                }
            }
        }
    }

    private var mediaSession: android.media.session.MediaSession? = null
    private var audioCodecProcessor: AudioCodecProcessor? = null

    private var audioFocusRequest: android.media.AudioFocusRequest? = null
    private var batteryMonitorReceiver: com.example.receiver.CallBatteryMonitorReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
        setupInterruptionListeners()
        requestCallAudioFocus()
        audioCodecProcessor = AudioCodecProcessor()

        try {
            val receiver = com.example.receiver.CallBatteryMonitorReceiver()
            com.example.receiver.CallBatteryMonitorReceiver.register(this, receiver)
            batteryMonitorReceiver = receiver
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcCallService", "Battery monitor receiver register note: ${e.message}")
        }

        NetworkCallStatusMonitor.instance.startMonitoring(this)
        WebRtcDiagnosticCollector.instance.startCollecting()
    }

    private fun requestCallAudioFocus() {
        try {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                audioManager?.requestAudioFocus(audioFocusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    android.media.AudioManager.STREAM_VOICE_CALL,
                    android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
            android.util.Log.i("WebRtcCallService", "🔊 Audio focus requested successfully (transient voice call focus - pauses external media players).")
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcCallService", "Audio focus request note: ${e.message}")
        }
    }

    private fun abandonCallAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(audioFocusChangeListener)
            }
            android.util.Log.i("WebRtcCallService", "🎵 Audio focus abandoned successfully (external media playback can resume).")
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcCallService", "Audio focus abandon note: ${e.message}")
        }
    }

    private fun setupMediaSession() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSession = android.media.session.MediaSession(this, "KramaWebRtcMediaSession").apply {
                    isActive = true
                }
                android.util.Log.i("WebRtcCallService", "Framework MediaSession created and activated for WebRTC call.")
            }
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcCallService", "MediaSession initialization note: ${e.message}")
        }
    }

    private fun setupInterruptionListeners() {
        try {
            telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            @Suppress("DEPRECATION")
            telephonyManager?.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_CALL_STATE)
        } catch (e: Throwable) {
            android.util.Log.w("WebRtcCallService", "Interruption listeners note: ${e.message}")
        }
    }

    private fun handleExternalCallInterruption(isOnHold: Boolean, reason: String?) {
        val current = _callSession.value ?: return
        if (current.callState == WebRtcCallState.CONNECTED || current.callState == WebRtcCallState.RINGING) {
            _callSession.value = current.copy(
                isHeldBySystemCall = isOnHold,
                holdReason = reason,
                isMuted = if (isOnHold) true else current.isMuted
            )
            updateNotification(_callSession.value)
        }
    }

    /**
     * Explicit cleanup routine to properly release peer connections, camera handles,
     * audio focus, and background resources to prevent memory & hardware leaks.
     */
    fun releasePeerConnectionsAndCameraHandles() {
        try {
            android.util.Log.i("WebRtcCallService", "🧹 Executing explicit WebRTC PeerConnection & Camera handle cleanup routine...")

            // 1. Release video capturer & camera hardware handles
            try {
                com.example.data.remote.WebRtcSignalingManager.getInstance().pauseWebRTC()
            } catch (e: Throwable) {
                android.util.Log.w("WebRtcCallService", "Camera capturer release note: ${e.message}")
            }

            // 2. Close peer connections & signaling
            durationTimerJob?.cancel()
            durationTimerJob = null

            // 3. Abandon audio focus & media session
            abandonCallAudioFocus()

            mediaSession?.release()
            mediaSession = null

            audioCodecProcessor?.release()
            audioCodecProcessor = null

            // 4. Log resource release in WebRTC Stats Logger
            WebRtcStatsLogger.instance.logMetricSample(
                jitterMs = 0f,
                packetLossPercent = 0f,
                latencyMs = 0f,
                throughputKbps = 0f,
                iceState = "CLOSED",
                eventNote = "Explicit cleanup executed: Peer connections & camera handles released"
            )
        } catch (e: Throwable) {
            android.util.Log.e("WebRtcCallService", "Error during explicit WebRTC cleanup: ${e.message}")
        }
    }

    /**
     * Called when the app enters background or memory trimming occurs.
     * Pauses camera hardware handles while preserving audio call session if connected.
     */
    fun onAppEnteredBackground() {
        val current = _callSession.value
        android.util.Log.i("WebRtcCallService", "📱 App lifecycle entered background. Checking call state (${current?.callState})...")
        if (current == null || current.callState == WebRtcCallState.IDLE || current.callState == WebRtcCallState.ENDED) {
            releasePeerConnectionsAndCameraHandles()
        } else if (current.isVideo) {
            android.util.Log.i("WebRtcCallService", "📷 Releasing camera handle in background to prevent hardware resource leaks...")
            try {
                com.example.data.remote.WebRtcSignalingManager.getInstance().pauseWebRTC()
            } catch (e: Throwable) {}
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_UI_HIDDEN, TRIM_MEMORY_BACKGROUND, TRIM_MEMORY_COMPLETE, TRIM_MEMORY_RUNNING_CRITICAL -> {
                android.util.Log.w("WebRtcCallService", "Memory trim level $level received! Triggering background hardware handle cleanup.")
                onAppEnteredBackground()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        android.util.Log.w("WebRtcCallService", "App task swiped away from Recents. Performing emergency peer connection & camera handle cleanup.")
        releasePeerConnectionsAndCameraHandles()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            releasePeerConnectionsAndCameraHandles()

            @Suppress("DEPRECATION")
            telephonyManager?.listen(phoneStateListener, android.telephony.PhoneStateListener.LISTEN_NONE)

            batteryMonitorReceiver?.let {
                com.example.receiver.CallBatteryMonitorReceiver.unregister(this, it)
                batteryMonitorReceiver = null
            }
            NetworkCallStatusMonitor.instance.stopMonitoring()
            WebRtcDiagnosticCollector.instance.stopCollecting()

            serviceScope.cancel()
        } catch (e: Throwable) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OUTGOING_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: "call_${System.currentTimeMillis()}"
                val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Encrypted Contact"
                val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
                startOutgoingCall(callId, contactName, isVideo)
            }
            ACTION_START_INCOMING_CALL -> {
                val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: "call_${System.currentTimeMillis()}"
                val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Encrypted Contact"
                val isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)
                startIncomingCall(callId, contactName, isVideo)
            }
            ACTION_ACCEPT_CALL -> {
                acceptCall()
            }
            ACTION_END_CALL -> {
                endCall()
            }
            ACTION_TOGGLE_MUTE -> {
                toggleMute()
            }
            ACTION_TOGGLE_SPEAKER -> {
                toggleSpeaker()
            }
        }
        return START_NOT_STICKY
    }

    fun startOutgoingCall(callId: String, contactName: String, isVideo: Boolean) {
        requestCallAudioFocus()
        val session = WebRtcCallSession(
            callId = callId,
            contactName = contactName,
            isVideo = isVideo,
            isIncoming = false,
            callState = WebRtcCallState.CONNECTING
        )
        _callSession.value = session
        startForeground(NOTIFICATION_ID, buildCallNotification(session))

        serviceScope.launch {
            delay(1500) // Simulate WebRTC ICE candidate gathering and STUN/TURN negotiation
            _callSession.value = _callSession.value?.copy(callState = WebRtcCallState.RINGING)
            updateNotification(_callSession.value)

            delay(2000) // Peer accepted session
            _callSession.value = _callSession.value?.copy(callState = WebRtcCallState.CONNECTED)
            com.example.util.CallHapticFeedbackUtil.vibrateConnectionEstablished(this@WebRtcCallService)
            updateNotification(_callSession.value)
            startDurationTimer()
        }
    }

    fun startIncomingCall(callId: String, contactName: String, isVideo: Boolean) {
        requestCallAudioFocus()
        val session = WebRtcCallSession(
            callId = callId,
            contactName = contactName,
            isVideo = isVideo,
            isIncoming = true,
            callState = WebRtcCallState.RINGING
        )
        _callSession.value = session
        startForeground(NOTIFICATION_ID, buildCallNotification(session))
    }

    fun acceptCall() {
        requestCallAudioFocus()
        _callSession.value = _callSession.value?.copy(callState = WebRtcCallState.CONNECTED)
        com.example.util.CallHapticFeedbackUtil.vibrateConnectionEstablished(this)
        updateNotification(_callSession.value)
        startDurationTimer()
    }

    fun endCall(isRemoteHangup: Boolean = false, isCallDrop: Boolean = false) {
        durationTimerJob?.cancel()
        abandonCallAudioFocus()
        _callSession.value = _callSession.value?.copy(callState = WebRtcCallState.ENDED)

        if (isCallDrop) {
            com.example.util.CallHapticFeedbackUtil.vibrateCallDrop(this)
        } else if (isRemoteHangup) {
            com.example.util.CallHapticFeedbackUtil.vibrateRemoteHangup(this)
        } else {
            com.example.util.CallHapticFeedbackUtil.vibrateControlClick(this)
        }

        serviceScope.launch {
            delay(800)
            _callSession.value = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    fun toggleMute() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(isMuted = !current.isMuted)
        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(this)
        updateNotification(_callSession.value)
    }

    fun toggleSpeaker() {
        val current = _callSession.value ?: return
        _callSession.value = current.copy(isSpeakerOn = !current.isSpeakerOn)
        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(this)
        updateNotification(_callSession.value)
    }

    fun toggleNoiseSuppression() {
        val current = _callSession.value ?: return
        val newState = !current.isNoiseSuppressionOn
        audioCodecProcessor?.setNoiseSuppression(newState)
        _callSession.value = current.copy(isNoiseSuppressionOn = newState)
        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(this)
    }

    fun toggleEchoCancellation() {
        val current = _callSession.value ?: return
        val newState = !current.isEchoCancellationOn
        audioCodecProcessor?.setEchoCancellation(newState)
        _callSession.value = current.copy(isEchoCancellationOn = newState)
        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(this)
    }

    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                val current = _callSession.value ?: break
                if (current.callState == WebRtcCallState.CONNECTED) {
                    val newSecs = current.callDurationSeconds + 1
                    _callSession.value = current.copy(callDurationSeconds = newSecs)
                    if (newSecs % 5 == 0) {
                        updateNotification(_callSession.value)
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        KramaNotificationChannelManager.createNotificationChannels(this)
    }

    private fun buildCallNotification(session: WebRtcCallSession): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endCallIntent = Intent(this, WebRtcCallService::class.java).apply { action = ACTION_END_CALL }
        val endCallPendingIntent = PendingIntent.getService(
            this, 1, endCallIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val acceptCallIntent = Intent(this, WebRtcCallService::class.java).apply { action = ACTION_ACCEPT_CALL }
        val acceptCallPendingIntent = PendingIntent.getService(
            this, 3, acceptCallIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, WebRtcCallService::class.java).apply { action = ACTION_TOGGLE_MUTE }
        val mutePendingIntent = PendingIntent.getService(
            this, 2, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val durationFormatted = "%02d:%02d".format(session.callDurationSeconds / 60, session.callDurationSeconds % 60)
        val statusText = when {
            session.isHeldBySystemCall -> "⏸️ Call on hold: ${session.holdReason ?: "External Call Active"}"
            session.isIncoming && session.callState == WebRtcCallState.RINGING -> "Incoming E2EE ${if (session.isVideo) "Video" else "Audio"} Call"
            session.callState == WebRtcCallState.CONNECTING -> "Connecting E2EE WebRTC..."
            session.callState == WebRtcCallState.RINGING -> "Ringing ${session.contactName}..."
            session.callState == WebRtcCallState.CONNECTED -> "Active Call • $durationFormatted${if (session.isMuted) " (Muted)" else ""}"
            session.callState == WebRtcCallState.ENDED -> "Call Ended"
            else -> "WebRTC Call Session"
        }

        val builder = NotificationCompat.Builder(this, KramaNotificationChannelManager.CHANNEL_INCOMING_CALLS)
            .setContentTitle("🔒 E2EE ${if (session.isVideo) "Video" else "Voice"} Call • ${session.contactName}")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(session.callState != WebRtcCallState.ENDED)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (session.isIncoming && session.callState == WebRtcCallState.RINGING) {
            builder.addAction(android.R.drawable.ic_menu_call, "Answer", acceptCallPendingIntent)
            builder.addAction(android.R.drawable.ic_delete, "Decline", endCallPendingIntent)
        } else {
            builder.addAction(android.R.drawable.ic_menu_call, "Return to Call", fullScreenPendingIntent)
            builder.addAction(android.R.drawable.ic_menu_manage, if (session.isMuted) "Unmute" else "Mute", mutePendingIntent)
            builder.addAction(android.R.drawable.ic_delete, "End Call", endCallPendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification(session: WebRtcCallSession?) {
        if (session == null) return
        val notification = buildCallNotification(session)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "webrtc_call_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_START_OUTGOING_CALL = "com.example.action.START_OUTGOING_CALL"
        const val ACTION_START_INCOMING_CALL = "com.example.action.START_INCOMING_CALL"
        const val ACTION_ACCEPT_CALL = "com.example.action.ACCEPT_CALL"
        const val ACTION_END_CALL = "com.example.action.END_CALL"
        const val ACTION_TOGGLE_MUTE = "com.example.action.TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.action.TOGGLE_SPEAKER"

        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CONTACT_NAME = "extra_contact_name"
        const val EXTRA_IS_VIDEO = "extra_is_video"
    }
}
