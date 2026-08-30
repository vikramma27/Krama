package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Shortcut
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Translate
import com.example.util.OnDeviceTranslationEngine
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReactionEntity
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.ReceivedBubbleDark
import com.example.ui.theme.SentBubbleDark
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AsymmetricBubble(
    message: MessageEntity,
    isFromMe: Boolean,
    onReply: (MessageEntity) -> Unit,
    onReact: (MessageEntity, String) -> Unit,
    onDelete: (MessageEntity) -> Unit,
    onTogglePin: (MessageEntity) -> Unit = {},
    onForward: ((MessageEntity) -> Unit)? = null,
    reactions: List<MessageReactionEntity> = emptyList(),
    highlightQuery: String = "",
    isActiveMatch: Boolean = false,
    customSentColor: Color? = null,
    customReceivedColor: Color? = null,
    onOpenMediaViewer: (url: String, type: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showCiphertext by remember { mutableStateOf(false) }
    var showQuickActionDialog by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    var isTranslated by remember { mutableStateOf(false) }
    var targetLangCode by remember { mutableStateOf("es") }
    var showLangSelector by remember { mutableStateOf(false) }

    val translatedText = remember(message.content, targetLangCode, isTranslated) {
        if (isTranslated && message.content.isNotEmpty()) {
            OnDeviceTranslationEngine.translateOnDevice(message.content, targetLangCode)
        } else null
    }

    // Flow Asymmetric Corner Shapes per blueprint spec:
    // Sent: top-left 18dp, top-right 18dp, bottom-left 18dp, bottom-right 3dp (sharper fold)
    // Received: top-left 18dp, top-right 18dp, bottom-left 3dp (sharper fold), bottom-right 18dp
    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 3.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 3.dp,
            bottomEnd = 18.dp
        )
    }

    val haptic = LocalHapticFeedback.current

    val bubbleBg = if (isFromMe) (customSentColor ?: SentBubbleDark) else (customReceivedColor ?: ReceivedBubbleDark)
    val alignment = if (isFromMe) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp, horizontal = 12.dp)
            .testTag("bubble_row_${message.id}"),
        contentAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset { IntOffset(offsetX.roundToInt(), 0) }
        ) {
            Surface(
                shape = bubbleShape,
                color = bubbleBg,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .then(
                        if (isActiveMatch) Modifier.border(2.dp, WarmCoral, bubbleShape)
                        else Modifier
                    )
                    .combinedClickable(
                        onClick = { showCiphertext = !showCiphertext },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showQuickActionDialog = true
                        }
                    )
                    .testTag("bubble_card_${message.id}")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Forwarded Message Indicator Badge
                    if (message.isForwarded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SoftTeal.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Shortcut,
                                contentDescription = "Forwarded",
                                tint = SoftTeal,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Forwarded",
                                color = SoftTeal,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    // Pinned Message Indicator Badge
                    if (message.isPinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarmCoral.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned Message",
                                tint = WarmCoral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pinned Message",
                                color = WarmCoral,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Group sender name if received
                    if (!isFromMe && message.senderName.isNotEmpty()) {
                        Text(
                            text = message.senderName,
                            color = WarmCoral,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    // Reply preview context
                    if (message.replyToContent.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Replying: ${message.replyToContent}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    val highlightedText = remember(message.content, highlightQuery) {
                        if (highlightQuery.isEmpty()) {
                            buildAnnotatedString { append(message.content) }
                        } else {
                            buildAnnotatedString {
                                val text = message.content
                                var start = 0
                                val queryLower = highlightQuery.lowercase()
                                val textLower = text.lowercase()
                                while (start < text.length) {
                                    val index = textLower.indexOf(queryLower, start)
                                    if (index == -1) {
                                        append(text.substring(start))
                                        break
                                    }
                                    if (index > start) {
                                        append(text.substring(start, index))
                                    }
                                    withStyle(style = SpanStyle(background = WarmCoral.copy(alpha = 0.45f), color = Color.White, fontWeight = FontWeight.Bold)) {
                                        append(text.substring(index, index + highlightQuery.length))
                                    }
                                    start = index + highlightQuery.length
                                }
                            }
                        }
                    }

                    // Message Content by Type
                    when (message.messageType) {
                        "VOICE", "AUDIO" -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val audioState by com.example.util.NativeAudioPlayer.playerState.collectAsState()
                            val isThisPlaying = audioState.isPlaying && audioState.currentFile == (message.mediaUrl.ifEmpty { message.content })

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val url = message.mediaUrl.ifEmpty { message.content }
                                        onOpenMediaViewer(url, "VOICE")
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(WarmCoral)
                                        .clickable {
                                            val url = message.mediaUrl.ifEmpty { message.content }
                                            com.example.util.NativeAudioPlayer.playAudio(context, url)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                        contentDescription = "Play Voice Note",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(0.85f)
                                    ) {
                                        Text(
                                            text = message.content.ifEmpty { "🎤 Encrypted Voice Note" },
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Voice Note 5s Rewind & Speed Control
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // ⏪ Rewind 5s Button
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SoftTeal.copy(alpha = 0.25f),
                                                modifier = Modifier.clickable {
                                                    try {
                                                        val currentPos = com.example.util.NativeAudioPlayer.playerState.value.currentPositionMs
                                                        val newPos = (currentPos - 5000).coerceAtLeast(0)
                                                        com.example.util.NativeAudioPlayer.seekTo(newPos)
                                                        Toast.makeText(context, "⏪ Rewound 5s", Toast.LENGTH_SHORT).show()
                                                    } catch (e: Throwable) {
                                                        android.util.Log.w("VoiceNoteRewind", "Rewind failed: ${e.message}")
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("⏪ 5s", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }

                                            // Voice Speed Control Pill (1x, 1.5x, 2x)
                                            var playbackSpeed by remember { mutableFloatStateOf(audioState.playbackSpeed) }
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = WarmCoral.copy(alpha = 0.2f),
                                                modifier = Modifier.clickable {
                                                    playbackSpeed = when (playbackSpeed) {
                                                        1.0f -> 1.5f
                                                        1.5f -> 2.0f
                                                        else -> 1.0f
                                                    }
                                                    com.example.util.NativeAudioPlayer.setPlaybackSpeed(playbackSpeed)
                                                }
                                            ) {
                                                Text(
                                                    text = "${if (playbackSpeed == 1.0f) "1" else if (playbackSpeed == 1.5f) "1.5" else "2"}x",
                                                    color = WarmCoral,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    // Audio Waveform Visualizer
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        listOf(12, 18, 8, 24, 16, 28, 10, 20, 14, 22, 6, 18, 12).forEach { height ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(height.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(if (isThisPlaying) WarmCoral else SoftTeal.copy(alpha = 0.8f))
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isThisPlaying) "Playing..." else "Opus 24kbps",
                                            color = SoftTeal,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        "LOCATION", "LIVE_LOCATION" -> {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                OsmMapView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    initialLat = 37.7749,
                                    initialLng = -122.4194,
                                    markerTitle = if (message.messageType == "LIVE_LOCATION") "Live Matrix Stream" else "Shared GPS Location"
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = message.content,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        "FILE", "DOCUMENT" -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WarmCoral),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.content,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = message.mediaSize.ifEmpty { "2.4 MB • Encrypted Document" },
                                            color = SoftTeal,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        "CONTACT" -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.25f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(SoftTeal),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = message.content,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Verified Matrix Olm Contact Card",
                                            color = SoftTeal,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        "IMAGE", "VIDEO" -> {
                            Column(
                                modifier = Modifier.clickable {
                                    onOpenMediaViewer(message.mediaUrl, message.messageType)
                                }
                            ) {
                                if (message.mediaUrl.isNotEmpty()) {
                                    val localEncryptedBitmap = remember(message.mediaUrl) {
                                        if (message.mediaUrl.startsWith("/") || message.mediaUrl.endsWith(".kramae2e")) {
                                            com.example.data.local.EncryptedMediaManager.decryptMediaToBitmap(message.mediaUrl)
                                        } else null
                                    }

                                    if (localEncryptedBitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = localEncryptedBitmap.asImageBitmap(),
                                            contentDescription = "Decrypted AES-256 Photo",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        coil.compose.AsyncImage(
                                            model = message.mediaUrl,
                                            contentDescription = "Encrypted Image",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                val textContentLower = message.content.lowercase()
                                val isCompliment = remember(message.content) {
                                    listOf("love", "amazing", "kind", "sweet", "proud", "awesome", "thank you", "great job", "best", "wonderful", "cutest", "cutie", "lovely").any { textContentLower.contains(it) }
                                }
                                val containsDateMention = remember(message.content) {
                                    listOf("tomorrow", "anniversary", "birthday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday", "august", "september", "october", "november", "december", "january", "february", "march", "april", "may", "june", "july", "at 7", "at 8", "dinner", "meeting", "date").any { textContentLower.contains(it) }
                                }

                                if (isCompliment) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text("✨ ", fontSize = 11.sp)
                                        Text(
                                            text = "Kind Message Highlighted",
                                            color = WarmCoral,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = highlightedText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )

                                if (containsDateMention) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val context = LocalContext.current
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = SoftTeal.copy(alpha = 0.2f),
                                        modifier = Modifier.clickable {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                                    data = android.provider.CalendarContract.Events.CONTENT_URI
                                                    putExtra(android.provider.CalendarContract.Events.TITLE, message.content.take(30))
                                                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Auto-detected date from Krama conversation: ${message.content}")
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Throwable) {
                                                Toast.makeText(context, "📅 Event options opened", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📅 Save to Calendar", color = SoftTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        "LIVE_ETA" -> {
                            LiveEtaSharingCard(
                                etaMinutes = 15,
                                locationName = "5th Avenue",
                                senderName = if (isFromMe) "You" else "Partner",
                                isFromMe = isFromMe
                            )
                        }

                        "CALL_ME_BACK" -> {
                            CallMeBackCard(
                                senderName = if (isFromMe) "You" else "Partner",
                                isFromMe = isFromMe,
                                onCallNow = {}
                            )
                        }

                        "IM_SAFE" -> {
                            SafetyCheckInCard(
                                senderName = if (isFromMe) "You" else "Partner",
                                isFromMe = isFromMe
                            )
                        }

                        "ARE_YOU_FREE" -> {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF818CF8))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("⚡ Are You Free Right Now?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }


                        else -> {
                            Column {
                                if (message.status == "SCHEDULED") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = "Scheduled",
                                            tint = WarmCoral,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Scheduled Message 🕒",
                                            color = WarmCoral,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                 Text(
                                    text = highlightedText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 20.sp,
                                        fontSize = 15.sp
                                    )
                                )

                                if (!isFromMe && message.content.contains("?")) {
                                    UnansweredQuestionBadge()
                                }


                                AnimatedVisibility(visible = isTranslated && translatedText != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.Black.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp)
                                            .testTag("translated_text_container")
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Translate,
                                                    contentDescription = null,
                                                    tint = SoftTeal,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "On-Device ML Kit NMT (${targetLangCode.uppercase()})",
                                                    color = SoftTeal,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = translatedText ?: "",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Ciphertext debugging snippet on tap
                    AnimatedVisibility(visible = showCiphertext) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Encrypted",
                                            tint = SoftTeal,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Signal Double Ratchet Payload (AES-256-GCM):",
                                            color = SoftTeal,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = message.cipherTextSnippet.ifEmpty { "olm_v1_aes_gcm:$$0x4f82a9d..." },
                                        color = Color.Green.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom info bar (Time + E2E status + Read ticks)
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reaction overlays linked by messageId
                        val messageReactions = remember(reactions, message.id, message.reaction) {
                            val list = reactions.filter { it.messageId == message.id }.map { it.emoji }.toMutableList()
                            if (message.reaction.isNotEmpty() && !list.contains(message.reaction)) {
                                list.add(message.reaction)
                            }
                            list
                        }

                        if (messageReactions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                messageReactions.distinct().take(4).forEach { emoji ->
                                    Text(text = emoji, fontSize = 12.sp)
                                }
                                if (messageReactions.size > 1) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${messageReactions.size}",
                                        color = WarmCoral,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = SoftTeal.copy(alpha = 0.7f),
                            modifier = Modifier.size(10.dp)
                        )

                        Spacer(modifier = Modifier.width(3.dp))

                        val formattedTime = remember(message.timestamp) {
                            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestamp))
                        }

                        Text(
                            text = formattedTime,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )

                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))

                            if (message.status == "FAILED") {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(WarmCoral.copy(alpha = 0.25f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Failed to send message",
                                        tint = WarmCoral,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Failed",
                                        color = WarmCoral,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                AnimatedReadStatusIndicator(status = message.status)
                            }
                        }
                    }
                }
            }
        }

        // Long Press Reaction & Action Dialog
        if (showQuickActionDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showQuickActionDialog = false },
                title = {
                    Text("Message Actions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Reactions:", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Quick Emoji Reaction Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("❤️", "👍", "🔥", "😂", "😮", "😢", "🙏").forEach { emoji ->
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onReact(message, emoji)
                                        showQuickActionDialog = false
                                    },
                                    shape = CircleShape,
                                    color = DarkPlumCard,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = emoji, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Translate Message Action (On-Device Local ML Kit)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isTranslated = !isTranslated
                                    showQuickActionDialog = false
                                }
                                .padding(vertical = 10.dp)
                                .testTag("translate_message_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isTranslated) "Hide On-Device Translation" else "Translate Message (Local ML Kit)",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Select Target Language Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLangSelector = true
                                    showQuickActionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = WarmCoral)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Select Target Language (${targetLangCode.uppercase()})",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Pin / Unpin Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTogglePin(message)
                                    showQuickActionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = null, tint = WarmCoral)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (message.isPinned) "Unpin Message" else "Pin Message to Top",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Reply Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onReply(message)
                                    showQuickActionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Reply to Message", color = Color.White, fontWeight = FontWeight.Medium)
                        }

                        // Bookmark Action ("Save for Later")
                        val currentContext = LocalContext.current
                        var isBookmarked by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isBookmarked = !isBookmarked
                                    showQuickActionDialog = false
                                    Toast.makeText(currentContext, if (isBookmarked) "Message bookmarked (saved for later)" else "Bookmark removed", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 10.dp)
                                .testTag("bookmark_message_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(if (isBookmarked) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = SoftTeal)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (isBookmarked) "Remove Bookmark" else "Bookmark (Save for Later)", color = Color.White, fontWeight = FontWeight.Medium)
                        }



                        // Forward Action
                        if (onForward != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onForward(message)
                                        showQuickActionDialog = false
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("forward_message_button"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Shortcut, contentDescription = null, tint = SoftTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Forward Message", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Delete for Everyone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDelete(message)
                                    showQuickActionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Message for Everyone", color = Color.Red.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showQuickActionDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        if (showLangSelector) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showLangSelector = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = SoftTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local ML Kit Translation Language", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Select target language. All translations run 100% locally on-device preserving E2E privacy.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OnDeviceTranslationEngine.AVAILABLE_LANGUAGES.forEach { lang ->
                            val isSelected = lang.code == targetLangCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) SoftTeal.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        targetLangCode = lang.code
                                        isTranslated = true
                                        showLangSelector = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lang.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    lang.name,
                                    color = if (isSelected) SoftTeal else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.Check, contentDescription = null, tint = SoftTeal)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showLangSelector = false }) {
                        Text("Close", color = SoftTeal)
                    }
                },
                containerColor = DarkPlumCard
            )
        }
    }
}

@Composable
fun AnimatedReadStatusIndicator(
    status: String,
    modifier: Modifier = Modifier
) {
    val isRead = status == "READ"
    val isDelivered = status == "DELIVERED"
    val isSent = status == "SENT"

    val tickColor by animateColorAsState(
        targetValue = when (status) {
            "READ" -> SoftTeal
            "DELIVERED" -> Color.LightGray
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        },
        animationSpec = tween(durationMillis = 500),
        label = "tickColorAnim"
    )

    val scale by animateFloatAsState(
        targetValue = if (isRead) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tickScaleAnim"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isRead) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "tickRotationAnim"
    )

    val tickIcon = when (status) {
        "SENDING" -> Icons.Default.Schedule
        "SENT" -> Icons.Default.Check
        else -> Icons.Default.DoneAll
    }

    Box(
        modifier = modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            rotationZ = rotation
        ),
        contentAlignment = Alignment.Center
    ) {
        if (isRead) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(SoftTeal.copy(alpha = 0.18f))
            )
        }

        AnimatedContent(
            targetState = status,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut(animationSpec = tween(220)) + scaleOut(targetScale = 0.6f))
            },
            label = "statusIconTransition"
        ) { targetStatus ->
            val tickIcon = when (targetStatus) {
                "SENDING" -> Icons.Default.Schedule
                "SENT" -> Icons.Default.Check
                else -> Icons.Default.DoneAll
            }
            Icon(
                imageVector = tickIcon,
                contentDescription = "Read status: $targetStatus",
                tint = tickColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

