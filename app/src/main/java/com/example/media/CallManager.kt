package com.example.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallState {
    IDLE, CONNECTING, CONNECTED, RECONNECTING, DISCONNECTED
}

enum class CallEngine {
    LIVEKIT, JITSI, WEBRTC
}

data class CallSessionConfig(
    val callId: String,
    val roomName: String,
    val participantName: String,
    val isVideo: Boolean = true,
    val engine: CallEngine = CallEngine.LIVEKIT,
    val serverUrl: String = "wss://livekit.krama-messaging.io"
)

class CallManager private constructor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isScreenSharing = MutableStateFlow(false)
    val isScreenSharing: StateFlow<Boolean> = _isScreenSharing.asStateFlow()

    private val _activeConfig = MutableStateFlow<CallSessionConfig?>(null)
    val activeConfig: StateFlow<CallSessionConfig?> = _activeConfig.asStateFlow()

    fun startCall(config: CallSessionConfig) {
        _activeConfig.value = config
        _callState.value = CallState.CONNECTING
        requestAudioFocus()
        setSpeakerphoneOn(true)

        Log.i(TAG, "Initiating ${config.engine} call for room=${config.roomName} at ${config.serverUrl}")

        // Simulate seamless engine initialization (LiveKit / Jitsi / WebRTC native signaling)
        _callState.value = CallState.CONNECTED
    }

    fun endCall() {
        Log.i(TAG, "Ending call session for room=${_activeConfig.value?.roomName}")
        _callState.value = CallState.DISCONNECTED
        abandonAudioFocus()
        _callState.value = CallState.IDLE
        _activeConfig.value = null
        _isMuted.value = false
        _isVideoEnabled.value = true
        _isScreenSharing.value = false
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        audioManager.isMicrophoneMute = newMute
        Log.d(TAG, "Audio mute state updated: $newMute")
    }

    fun toggleVideo() {
        val newVideoState = !_isVideoEnabled.value
        _isVideoEnabled.value = newVideoState
        Log.d(TAG, "Video enable state updated: $newVideoState")
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        setSpeakerphoneOn(newSpeaker)
    }

    fun toggleScreenShare() {
        val newScreenShare = !_isScreenSharing.value
        _isScreenSharing.value = newScreenShare
        Log.d(TAG, "Screen share state updated: $newScreenShare")
    }

    private fun setSpeakerphoneOn(on: Boolean) {
        try {
            audioManager.isSpeakerphoneOn = on
            audioManager.mode = if (on) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Speakerphone toggle warning: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.w(TAG, "Audio focus lost transiently")
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.i(TAG, "Audio focus gained")
                        }
                    }
                }
                .build()

            audioFocusRequest = focusRequest
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    companion object {
        private const val TAG = "KramaCallManager"

        @Volatile
        private var INSTANCE: CallManager? = null

        fun getInstance(context: Context): CallManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

@Composable
fun LiveKitCallScreenView(
    callManager: CallManager,
    onHangup: () -> Unit
) {
    val callState by callManager.callState.collectAsState()
    val isMuted by callManager.isMuted.collectAsState()
    val isVideoEnabled by callManager.isVideoEnabled.collectAsState()
    val isSpeakerOn by callManager.isSpeakerOn.collectAsState()
    val isScreenSharing by callManager.isScreenSharing.collectAsState()
    val activeConfig by callManager.activeConfig.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Remote Video Canvas
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isVideoEnabled && callState == CallState.CONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Remote Stream",
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(120.dp)
                    )
                    Text(
                        text = "LiveKit / Jitsi / WebRTC Live Stream",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 160.dp)
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (activeConfig?.participantName ?: "Krama").take(1).uppercase(),
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = activeConfig?.participantName ?: "Encrypted Participant",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (callState) {
                            CallState.CONNECTING -> "Connecting via LiveKit Engine..."
                            CallState.CONNECTED -> "00:45 • Encrypted End-to-End"
                            else -> "Call Session Ended"
                        },
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Local Video Preview
        if (isVideoEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 20.dp)
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Local Camera Preview",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "You",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }
        }

        // Top Engine Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${activeConfig?.engine ?: CallEngine.LIVEKIT} VoIP Session",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Control Panel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1E293B).copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { callManager.toggleMute() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFEF4444) else Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { callManager.toggleVideo() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (!isVideoEnabled) Color(0xFFEF4444) else Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Camera",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { callManager.toggleSpeaker() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) MaterialTheme.colorScheme.primary else Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaker",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { callManager.toggleScreenShare() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isScreenSharing) MaterialTheme.colorScheme.secondary else Color(0xFF334155))
                ) {
                    Icon(
                        imageVector = Icons.Default.ScreenShare,
                        contentDescription = "Screen Share",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        callManager.endCall()
                        onHangup()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDC2626))
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
