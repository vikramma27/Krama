package com.example.ui.screens

import android.media.AudioManager
import android.media.ToneGenerator
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.ContactEntity
import com.example.service.NetworkCallStatusMonitor
import com.example.service.NetworkTypeLabel
import com.example.service.WebRtcCallState
import com.example.ui.components.WebRtcDiagnosticOverlay
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class AudioOutputMode {
    SPEAKER,
    EARPIECE,
    BLUETOOTH
}

@Composable
fun ActiveCallScreen(
    contact: ContactEntity,
    isVideo: Boolean,
    callState: WebRtcCallState,
    isSignalingPaused: Boolean = false,
    isHeldBySystemCall: Boolean = false,
    holdReason: String? = null,
    isLowPowerModeEnabled: Boolean = false,
    groupParticipants: List<ContactEntity> = emptyList(),
    allContacts: List<ContactEntity> = emptyList(),
    onAddParticipant: ((ContactEntity) -> Unit)? = null,
    onRemoveParticipant: ((String) -> Unit)? = null,
    onAnswerCall: (() -> Unit)? = null,
    onEndCall: () -> Unit,
    onToggleNoiseSuppression: ((Boolean) -> Unit)? = null,
    onToggleEchoCancellation: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    fun playControlBeep(toneType: Int = ToneGenerator.TONE_PROP_BEEP) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            toneGen.startTone(toneType, 80)
        } catch (e: Throwable) {
            android.util.Log.w("ActiveCallScreen", "Audio feedback error: ${e.message}")
        }
    }

    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideo) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var audioOutputMode by remember { mutableStateOf(AudioOutputMode.SPEAKER) }
    var showAudioOutputMenu by remember { mutableStateOf(false) }
    var showDiagnosticOverlay by remember { mutableStateOf(false) }
    var showParticipantsSheet by remember { mutableStateOf(false) }
    var showAddParticipantDialog by remember { mutableStateOf(false) }

    // In-Call Play Together Game state
    var showInCallGamesSheet by remember { mutableStateOf(false) }
    var activeInCallGameType by remember { mutableStateOf(com.example.ui.components.GameType.NONE) }

    var isNoiseSuppressionOn by remember { mutableStateOf(true) }
    var isEchoCancellationOn by remember { mutableStateOf(true) }
    var isAutoCaptionsOn by remember { mutableStateOf(true) }
    var liveCaptionText by remember { mutableStateOf("LIVE CAPTIONS: \"Hey! Are you awake? I'll be there in 15 minutes!\"") }

    val realTimeNetworkStatus by NetworkCallStatusMonitor.instance.networkStatus.collectAsStateWithLifecycle()

    var durationSeconds by remember { mutableIntStateOf(0) }

    // Battery monitoring during call
    var batteryLevel by remember { mutableIntStateOf(85) }
    var isCharging by remember { mutableStateOf(false) }
    var isLowBatteryAlert by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        val pct = (level * 100) / scale
                        batteryLevel = pct
                        isLowBatteryAlert = pct < 15
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    if (isCharging && isLowBatteryAlert && batteryLevel > 15) {
                        isLowBatteryAlert = false
                    }
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }
    }

    // Call duration coroutine ticker
    LaunchedEffect(callState) {
        if (callState == WebRtcCallState.CONNECTED) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            while (true) {
                delay(1000)
                durationSeconds++
            }
        } else {
            durationSeconds = 0
        }
    }

    val formattedDuration = remember(durationSeconds) {
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        "%02d:%02d".format(mins, secs)
    }

    // Real-time audio waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("active_call_screen"),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Background Video Stream Canvas / Remote View
            if (isVideoEnabled && isVideo) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF181C24)),
                    contentAlignment = Alignment.Center
                ) {
                    if (contact.avatarUrl.isNotEmpty()) {
                        AsyncImage(
                            model = contact.avatarUrl,
                            contentDescription = contact.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray)
                        )
                    } else {
                        // High resolution video stream canvas fallback
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .background(WarmCoral),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 64.sp
                            )
                        }
                    }

                    // Low battery / low power resolution adjustment overlay
                    if (isLowBatteryAlert && !isCharging) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmCoral.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Low Battery ($batteryLevel%) • Adaptive Resolution active (Auto-Restores on Charging)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (isCharging && batteryLevel > 15) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftTeal.copy(alpha = 0.85f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Charging ($batteryLevel%) • HD Video Resolution Restored",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // Dark Canvas with Dynamic GridView for Group and Active Call Participants
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NearBlackPlum),
                    contentAlignment = Alignment.Center
                ) {
                    val participantList = remember(groupParticipants, contact) {
                        if (groupParticipants.isNotEmpty()) groupParticipants else listOf(contact)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 90.dp, bottom = 175.dp, start = 14.dp, end = 14.dp)
                    ) {
                        if (participantList.size > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "WebRTC Group Call Mesh (${participantList.size} Active Peers)",
                                    color = SoftTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (participantList.size <= 1) 1 else 2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(participantList, key = { it.id }) { participant ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (participantList.size <= 2) 220.dp else 160.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp)),
                                    color = DarkPlumCard
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size((58 * waveScale).dp)
                                                    .clip(CircleShape)
                                                    .background(WarmCoral.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(WarmCoral),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = participant.name.take(1).uppercase(),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 20.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = participant.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.GraphicEq,
                                                    contentDescription = null,
                                                    tint = SoftTeal,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Opus 48kHz HD",
                                                    color = SoftTeal,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        // Status Pill Badge Top Right
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (callState == WebRtcCallState.CONNECTED) SoftTeal else WarmCoral)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (callState == WebRtcCallState.CONNECTED) "Connected" else "Connecting",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        com.example.ui.components.AudioWaveformVisualizer(
                            isMuted = isMuted,
                            isCallConnected = callState == WebRtcCallState.CONNECTED
                        )
                    }
                }
            }

            // Self View PiP Window (When Video Enabled)
            if (isVideoEnabled && isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 120.dp, end = 20.dp)
                        .size(width = 100.dp, height = 150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(2.dp, WarmCoral, RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFrontCamera) "Front Cam" else "Rear Cam",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Signal Style Top Header Bar Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onEndCall()
                        },
                        modifier = Modifier.testTag("signal_back_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = contact.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (callState == WebRtcCallState.CONNECTED) "$formattedDuration • E2E Signal Lock" else "Calling...",
                                color = SoftTeal,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // ConnectivityManager Real-Time Network Status Overlay Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    !realTimeNetworkStatus.isConnected || isSignalingPaused -> WarmCoral.copy(alpha = 0.85f)
                                    realTimeNetworkStatus.networkType == NetworkTypeLabel.METERED_LOW_BANDWIDTH -> DarkPlumCard.copy(alpha = 0.95f)
                                    else -> DarkPlumCard.copy(alpha = 0.85f)
                                }
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showDiagnosticOverlay = !showDiagnosticOverlay
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("network_signal_overlay_pill")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (realTimeNetworkStatus.networkType) {
                                    NetworkTypeLabel.WIFI -> Icons.Default.Wifi
                                    NetworkTypeLabel.CELLULAR_4G, NetworkTypeLabel.CELLULAR_5G, NetworkTypeLabel.METERED_LOW_BANDWIDTH -> Icons.Default.SignalCellular4Bar
                                    NetworkTypeLabel.RECONNECTING -> Icons.Default.Wifi
                                },
                                contentDescription = "Network Connection Strength",
                                tint = when {
                                    !realTimeNetworkStatus.isConnected || isSignalingPaused -> Color.White
                                    realTimeNetworkStatus.networkType == NetworkTypeLabel.METERED_LOW_BANDWIDTH -> WarmCoral
                                    else -> SoftTeal
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSignalingPaused) "Reconnecting..." else realTimeNetworkStatus.displayLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (showDiagnosticOverlay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    WebRtcDiagnosticOverlay(callState = callState, isLowPowerModeEnabled = isLowPowerModeEnabled)
                }
            }

            // Signal Style Side Floating Buttons (Right: Camera Switch, Left: More Options + Manage Participants)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 28.dp, top = 0.dp, end = 28.dp, bottom = 110.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showDiagnosticOverlay = !showDiagnosticOverlay
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .testTag("more_options_button")
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "More Options", tint = Color.White)
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showParticipantsSheet = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SoftTeal.copy(alpha = 0.3f))
                            .border(1.dp, SoftTeal, CircleShape)
                            .testTag("manage_participants_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.GroupAdd, contentDescription = "Manage Participants", tint = SoftTeal)
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showInCallGamesSheet = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WarmCoral.copy(alpha = 0.3f))
                            .border(1.dp, WarmCoral, CircleShape)
                            .testTag("in_call_play_together_button")
                    ) {
                        Text("🎮", fontSize = 18.sp)
                    }
                }

                if (isVideo) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                            isFrontCamera = !isFrontCamera
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .align(Alignment.CenterEnd)
                            .testTag("camera_switch_button")
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                    }
                }
            }

            // Auto-Captions Live Transcript Banner
            if (isAutoCaptionsOn && callState == WebRtcCallState.CONNECTED) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                        .border(1.dp, SoftTeal.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SoftTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = liveCaptionText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Signal Style Floating Bottom Pill Controls Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 28.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .testTag("signal_call_controls_pill"),
                    color = Color(0xFF222834).copy(alpha = 0.92f),
                    tonalElevation = 12.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Audio Output Selector (Speaker / Earpiece / Bluetooth)
                        Box {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                    showAudioOutputMenu = true
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .testTag("audio_output_selector_button")
                            ) {
                                Icon(
                                    imageVector = when (audioOutputMode) {
                                        AudioOutputMode.SPEAKER -> Icons.Default.VolumeUp
                                        AudioOutputMode.EARPIECE -> Icons.Default.PhoneInTalk
                                        AudioOutputMode.BLUETOOTH -> Icons.Default.BluetoothAudio
                                    },
                                    contentDescription = "Audio Output",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showAudioOutputMenu,
                                onDismissRequest = { showAudioOutputMenu = false },
                                modifier = Modifier.background(DarkPlumCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🔊 Speakerphone", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                        audioOutputMode = AudioOutputMode.SPEAKER
                                        showAudioOutputMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📞 Earpiece", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                        audioOutputMode = AudioOutputMode.EARPIECE
                                        showAudioOutputMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎧 Bluetooth Device", color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                        audioOutputMode = AudioOutputMode.BLUETOOTH
                                        showAudioOutputMenu = false
                                    }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🔇 Noise Suppression (ANS)", color = Color.White, fontSize = 13.sp)
                                            Text(if (isNoiseSuppressionOn) "ON" else "OFF", color = if (isNoiseSuppressionOn) SoftTeal else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                        isNoiseSuppressionOn = !isNoiseSuppressionOn
                                        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(context)
                                        onToggleNoiseSuppression?.invoke(isNoiseSuppressionOn)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🎙️ Echo Cancellation (AEC)", color = Color.White, fontSize = 13.sp)
                                            Text(if (isEchoCancellationOn) "ON" else "OFF", color = if (isEchoCancellationOn) SoftTeal else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    },
                                    onClick = {
                                        playControlBeep(ToneGenerator.TONE_PROP_BEEP)
                                        isEchoCancellationOn = !isEchoCancellationOn
                                        com.example.util.CallHapticFeedbackUtil.vibrateControlClick(context)
                                        onToggleEchoCancellation?.invoke(isEchoCancellationOn)
                                    }
                                )
                            }
                        }

                        // 2. Video Enable / Disable Toggle
                        if (isVideo) {
                            IconButton(
                                onClick = {
                                    com.example.util.CallHapticFeedbackUtil.vibrateShortActionPulse(context)
                                    playControlBeep(ToneGenerator.TONE_PROP_ACK)
                                    isVideoEnabled = !isVideoEnabled
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (isVideoEnabled) Color.White.copy(alpha = 0.15f) else WarmCoral)
                                    .testTag("toggle_video_button")
                            ) {
                                Icon(
                                    imageVector = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                                    contentDescription = "Toggle Video",
                                    tint = Color.White
                                )
                            }
                        }

                        // 3. Microphone Mute Toggle
                        IconButton(
                            onClick = {
                                com.example.util.CallHapticFeedbackUtil.vibrateShortActionPulse(context)
                                playControlBeep(ToneGenerator.TONE_PROP_ACK)
                                isMuted = !isMuted
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (isMuted) WarmCoral else Color.White.copy(alpha = 0.15f))
                                .testTag("mute_call_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mute Microphone",
                                tint = Color.White
                            )
                        }

                        // 4. Signal Style End Call Button (Red Circle)
                        IconButton(
                            onClick = {
                                com.example.util.CallHapticFeedbackUtil.vibrateEndCall(context)
                                playControlBeep(ToneGenerator.TONE_PROP_NACK)
                                onEndCall()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WarmCoral)
                                .testTag("end_call_button")
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
    }

    if (showParticipantsSheet) {
        ConferenceParticipantsDialog(
            groupParticipants = if (groupParticipants.isEmpty()) listOf(contact) else groupParticipants,
            allContacts = allContacts,
            onAddParticipant = { newParticipant ->
                onAddParticipant?.invoke(newParticipant)
            },
            onRemoveParticipant = { participantId ->
                onRemoveParticipant?.invoke(participantId)
            },
            onDismiss = { showParticipantsSheet = false }
        )
    }

    if (showInCallGamesSheet) {
        com.example.ui.components.PlayTogetherBottomSheet(
            partnerName = contact.name,
            onDismiss = { showInCallGamesSheet = false },
            onLaunchGame = { type ->
                activeInCallGameType = type
            },
            onShareGameMemory = { _ -> }
        )
    }

    if (activeInCallGameType != com.example.ui.components.GameType.NONE) {
        com.example.ui.components.ActiveGameContainerOverlay(
            gameType = activeInCallGameType,
            partnerName = contact.name,
            onCloseGame = { activeInCallGameType = com.example.ui.components.GameType.NONE },
            onShareMatchResult = { _ -> }
        )
    }
}

@Composable
private fun ConferenceParticipantsDialog(
    groupParticipants: List<ContactEntity>,
    allContacts: List<ContactEntity>,
    onAddParticipant: ((ContactEntity) -> Unit)?,
    onRemoveParticipant: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    var showAddPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Group, contentDescription = null, tint = SoftTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Conference Mesh", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("${groupParticipants.size} Active Members • E2EE Opus", color = SoftTeal, fontSize = 11.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "Active Call Participants:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(groupParticipants, key = { it.id }) { member ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
                            color = NearBlackPlum
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(SoftTeal.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(member.name.take(1), color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(member.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Connected • Opus 48kHz HD", color = SoftTeal, fontSize = 10.sp)
                                    }
                                }
                                if (groupParticipants.size > 1 && onRemoveParticipant != null) {
                                    IconButton(
                                        onClick = { onRemoveParticipant(member.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = "Remove", tint = WarmCoral, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showAddPicker = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = NearBlackPlum, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Participant", color = NearBlackPlum, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.White)
            }
        },
        containerColor = DarkPlumCard,
        shape = RoundedCornerShape(20.dp)
    )

    if (showAddPicker) {
        val availableContacts = remember(groupParticipants, allContacts) {
            val existingIds = groupParticipants.map { it.id }.toSet()
            allContacts.filter { !existingIds.contains(it.id) }
        }
        AlertDialog(
            onDismissRequest = { showAddPicker = false },
            title = { Text("Add to Call Session", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                if (availableContacts.isEmpty()) {
                    Text("All available contacts are already in this call.", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(availableContacts, key = { it.id }) { contact ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onAddParticipant?.invoke(contact)
                                        showAddPicker = false
                                    },
                                color = NearBlackPlum
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(WarmCoral.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(contact.name.take(1), color = WarmCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(contact.phoneNumber, color = Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddPicker = false }) {
                    Text("Cancel", color = SoftTeal)
                }
            },
            containerColor = DarkPlumCard,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
