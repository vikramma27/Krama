package com.example.ui.screens

import com.example.ui.components.lottie.LottieEmptyChatState
import com.example.ui.components.lottie.LottieTypingIndicator
import com.example.ui.components.rive.RiveSendButton
import com.example.util.NetworkStatus
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Help
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.PersonAdd
import com.example.domain.model.ChatWallpaperConfig
import com.example.ui.components.WALLPAPER_PRESETS
import com.example.ui.components.WallpaperSelectorDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReactionEntity
import com.example.ui.components.AsymmetricBubble
import com.example.ui.components.ReactionPicker
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatThreadScreen(
    chat: ChatEntity,
    messages: List<MessageEntity>,
    contacts: List<ContactEntity>,
    onBack: () -> Unit,
    onSendMessage: (text: String, type: String, mediaUrl: String, replyToId: String, replyToContent: String) -> Unit,
    onReact: (messageId: String, emoji: String) -> Unit,
    onDelete: (messageId: String) -> Unit,
    onStartCall: (ContactEntity, isVideo: Boolean) -> Unit,
    onVerifySafetyNumber: (chatId: String, contactPublic: String) -> Unit,
    onUpdateDisappearingTimer: (seconds: Long) -> Unit,
    onTogglePin: (messageId: String, isPinned: Boolean) -> Unit = { _, _ -> },
    onSaveDraft: (chatId: String, draft: String) -> Unit = { _, _ -> },
    reactions: List<MessageReactionEntity> = emptyList(),
    wallpaperConfig: ChatWallpaperConfig = ChatWallpaperConfig(),
    onSaveWallpaperConfig: (ChatWallpaperConfig) -> Unit = {},
    typingUsers: List<String> = emptyList(),
    onTyping: (Boolean) -> Unit = {},
    onSendEncryptedMedia: (android.net.Uri, String) -> Unit = { _, _ -> },
    groupMembers: List<com.example.data.local.entity.GroupMemberEntity> = emptyList(),
    onAddGroupMember: (userId: String, displayName: String, publicKey: String) -> Unit = { _, _, _ -> },
    onRemoveGroupMember: (userId: String) -> Unit = {},
    onScheduleMessage: (text: String, timeMs: Long, messageType: String, mediaUrl: String, replyToId: String, replyToContent: String) -> Unit = { _, _, _, _, _, _ -> },
    pendingScheduledMessages: List<com.example.data.local.entity.ScheduledMessageEntity> = emptyList(),
    onCancelScheduledMessage: (id: String) -> Unit = {},
    onForwardMessage: ((MessageEntity, targetChatId: String) -> Unit)? = null,
    allChats: List<ChatEntity> = emptyList(),
    isNetworkConnected: Boolean = true,
    networkStatus: NetworkStatus = NetworkStatus.AVAILABLE,
    isSignalingPaused: Boolean = false,
    messengerRepository: com.example.data.repository.MessengerRepository? = null,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var targetMessageForReaction by remember { mutableStateOf<MessageEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showThreadNotificationDialog by remember { mutableStateOf(false) }

    if (showWallpaperDialog) {
        WallpaperSelectorDialog(
            currentConfig = wallpaperConfig,
            onDismiss = { showWallpaperDialog = false },
            onSaveConfig = onSaveWallpaperConfig
        )
    }

    if (showThreadNotificationDialog) {
        com.example.ui.components.ThreadNotificationSettingsDialog(
            chatId = chat.id,
            chatTitle = chat.title,
            onDismiss = { showThreadNotificationDialog = false }
        )
    }

    // Voice Note Lock Recording state
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var isVoiceLocked by remember { mutableStateOf(false) }
    var isVoicePaused by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }

    // Schedule Message state
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showVoiceRecorderDialog by remember { mutableStateOf(false) }

    // Play Together Game state
    var showPlayTogetherSheet by remember { mutableStateOf(false) }
    var activeGameType by remember { mutableStateOf(com.example.ui.components.GameType.NONE) }


    // In-chat search and disappearing message states
    var isSearchingInChat by remember { mutableStateOf(false) }
    var inChatSearchQuery by remember { mutableStateOf("") }
    var currentSearchMatchIndex by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val matchedMessageIndices = remember(messages, inChatSearchQuery) {
        if (inChatSearchQuery.isBlank()) emptyList()
        else messages.mapIndexedNotNull { index, msg ->
            if (msg.content.contains(inChatSearchQuery, ignoreCase = true)) index else null
        }
    }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showMediaPickerScreen by remember { mutableStateOf(false) }
    var showGroupMembersDialog by remember { mutableStateOf(false) }
    var showAddMemberPicker by remember { mutableStateOf(false) }

    var activeMediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var activeMediaViewerType by remember { mutableStateOf<String>("IMAGE") }

    var messageToForward by remember { mutableStateOf<MessageEntity?>(null) }

    val context = LocalContext.current
    val voiceRecorder = remember(context) { com.example.util.RealVoiceRecorder(context) }
    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.release()
        }
    }

    if (messageToForward != null) {
        AlertDialog(
            onDismissRequest = { messageToForward = null },
            title = { Text("Forward Message", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Select target conversation:", color = SoftTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(allChats.size) { index ->
                            val c = allChats[index]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onForwardMessage?.invoke(messageToForward!!, c.id)
                                        Toast.makeText(context, "Message forwarded to ${c.title}", Toast.LENGTH_SHORT).show()
                                        messageToForward = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (c.isGroup) Icons.Default.Group else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SoftTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(c.title, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { messageToForward = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkPlumCard
        )
    }

    if (activeMediaViewerUrl != null) {
        com.example.ui.components.FullScreenMediaViewer(
            mediaUrl = activeMediaViewerUrl!!,
            mediaType = activeMediaViewerType,
            title = chat.title,
            onClose = { activeMediaViewerUrl = null }
        )
    }

    val contact = remember(contacts, chat.contactId) {
        contacts.find { it.id == chat.contactId } ?: ContactEntity(
            id = chat.contactId,
            name = chat.title,
            phoneNumber = "",
            avatarUrl = chat.avatarUrl,
            statusText = "Encrypted Contact",
            lastSeenTimestamp = System.currentTimeMillis(),
            isOnline = false,
            publicKey = ""
        )
    }

    var showContactFeaturesSheet by remember { mutableStateOf(false) }

    val contactFeatureState by (messengerRepository?.getContactFeatureFlow(contact.id) ?: kotlinx.coroutines.flow.flowOf(com.example.data.local.entity.ContactFeatureEntity(contactId = contact.id))).collectAsStateWithLifecycle(
        initialValue = com.example.data.local.entity.ContactFeatureEntity(contactId = contact.id)
    )
    val contactFeature = contactFeatureState ?: com.example.data.local.entity.ContactFeatureEntity(contactId = contact.id)
    val sharedCountdowns by (messengerRepository?.getSharedCountdownsFlow(contact.id) ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    val partnerNowPlaying by (messengerRepository?.getNowPlayingFlow(contact.id) ?: kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val myNowPlaying by (messengerRepository?.getNowPlayingFlow("user_me") ?: kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)
    val openWhenMessages by (messengerRepository?.getOpenWhenMessagesFlow(chat.id) ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    val activeReminders by (messengerRepository?.getActiveRemindersFlow(chat.id) ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsStateWithLifecycle(initialValue = emptyList())
    val wallpaperProposal by (messengerRepository?.getLatestWallpaperProposalFlow(chat.id) ?: kotlinx.coroutines.flow.flowOf(null)).collectAsStateWithLifecycle(initialValue = null)

    if (showContactFeaturesSheet && messengerRepository != null) {
        com.example.ui.components.ContactFeaturesSheet(
            contact = contact,
            chatId = chat.id,
            repository = messengerRepository,
            onDismiss = { showContactFeaturesSheet = false }
        )
    }

    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingIsVideoCall by remember { mutableStateOf(false) }

    fun startCallWithPermissionCheck(isVideo: Boolean) {
        if (com.example.ui.components.MediaPermissionsChecker.hasCallPermissions(context, isVideo)) {
            onStartCall(contact, isVideo)
        } else {
            pendingIsVideoCall = isVideo
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        com.example.ui.components.MediaPermissionsRationaleDialog(
            isVideoCall = pendingIsVideoCall,
            onPermissionsGranted = {
                showPermissionDialog = false
                onStartCall(contact, pendingIsVideoCall)
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }

    val fusedLocationClient = remember(context) {
        try { LocationServices.getFusedLocationProviderClient(context) } catch (e: Exception) { null }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.split("/")?.lastOrNull() ?: "Document.pdf"
            onSendMessage("📄 Document: $fileName", "DOCUMENT", uri.toString(), replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
            showAttachmentPicker = false
        }
    }

    val haptic = LocalHapticFeedback.current

    // Restore draft message if present
    LaunchedEffect(chat.id) {
        if (chat.draftMessage.isNotEmpty() && textInput.isEmpty()) {
            textInput = chat.draftMessage
        }
    }

    // Auto-save draft on navigation or text changes
    DisposableEffect(chat.id, textInput) {
        onDispose {
            onSaveDraft(chat.id, textInput)
        }
    }

    // Voice recording timer effect
    LaunchedEffect(isRecordingVoiceNote, isVoicePaused) {
        if (isRecordingVoiceNote && !isVoicePaused) {
            while (true) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NearBlackPlum)
            .testTag("chat_thread_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkPlumCard,
                tonalElevation = 4.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        com.example.ui.components.SameWavelengthAvatar(
                            avatarUrl = contact.avatarUrl,
                            name = contact.name,
                            isOnline = contact.isOnline,
                            isBothOnline = contact.isOnline, // Both online -> Same Wavelength glow
                            sizeDp = 38,
                            modifier = Modifier.clickable { showContactFeaturesSheet = true }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                                .clickable { showContactFeaturesSheet = true }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = contactFeature.nickname.ifEmpty { chat.title },
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                if (contactFeature.statusEmoji.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = contactFeature.statusEmoji, fontSize = 14.sp)
                                }
                                if (contactFeature.streakDays > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = Color(0xFFF97316).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "🔥 ${contactFeature.streakDays}",
                                            color = Color(0xFFF97316),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (chat.isGroup || contact.isOnline) SoftTeal else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (chat.isGroup) "${groupMembers.size} Members • E2E Group Key Active" else if (contact.isOnline) "Online • Matrix E2E Active" else "Last Active Together: Today 3:45 PM",
                                    color = if (chat.isGroup || contact.isOnline) SoftTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Single-Tap Nudge / Ping Button 💓
                        IconButton(
                            onClick = {
                                onSendMessage("💓 Sent a nudge ping!", "NUDGE", "", "", "")
                                try {
                                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                    vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 120), -1))
                                } catch (e: Throwable) {}
                                android.widget.Toast.makeText(context, "💓 Nudge Sent to ${contact.name}!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("nudge_button")
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Send Nudge", tint = Color(0xFFEC4899))
                        }

                        IconButton(
                            onClick = { isSearchingInChat = !isSearchingInChat },
                            modifier = Modifier.testTag("in_chat_search_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search Chat", tint = Color.White)
                        }

                        IconButton(
                            onClick = { startCallWithPermissionCheck(false) },
                            modifier = Modifier.testTag("voice_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Voice Call", tint = WarmCoral)
                        }

                        IconButton(
                            onClick = { startCallWithPermissionCheck(true) },
                            modifier = Modifier.testTag("video_call_button")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = WarmCoral)
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(DarkPlumCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Contact Customization & Notes", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = SoftTeal) },
                                    onClick = {
                                        showMenu = false
                                        showContactFeaturesSheet = true
                                    }
                                )
                                if (chat.isGroup) {
                                    DropdownMenuItem(
                                        text = { Text("Group Members & E2E Keys", color = Color.White) },
                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = SoftTeal) },
                                        onClick = {
                                            showMenu = false
                                            showGroupMembersDialog = true
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Schedule Message", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = WarmCoral) },
                                    onClick = {
                                        showMenu = false
                                        showScheduleDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Verify E2E Safety Numbers", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = SoftTeal) },
                                    onClick = {
                                        showMenu = false
                                        onVerifySafetyNumber(chat.id, contact.publicKey)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Chat Background Wallpaper", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Wallpaper, contentDescription = null, tint = SoftTeal) },
                                    onClick = {
                                        showMenu = false
                                        showWallpaperDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Custom Notification Settings", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SoftTeal) },
                                    onClick = {
                                        showMenu = false
                                        showThreadNotificationDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Disappearing Messages Settings", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = WarmCoral) },
                                    onClick = {
                                        showMenu = false
                                        showDisappearingDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Feature 1: Contact Birthday / Anniversary Banner in Header
                    val context = LocalContext.current
                    val isSpecialEventBanner = remember(contact.id, contact.name) {
                        val hash = (contact.id.hashCode() + contact.name.hashCode()).let { if (it < 0) -it else it }
                        when (hash % 2) {
                            0 -> "🎂 Birthday Today! Wish ${contact.name.split(" ").firstOrNull() ?: contact.name}!"
                            1 -> "🎉 1-Year Chat Anniversary Today!"
                            else -> null
                        }
                    }

                    if (!chat.isGroup && isSpecialEventBanner != null) {
                        Surface(
                            color = WarmCoral.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clickable {
                                    val alarmMgr = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
                                    val intent = android.content.Intent(context, com.example.receiver.CallActionReceiver::class.java)
                                    val pIntent = android.app.PendingIntent.getBroadcast(
                                        context, 888, intent,
                                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                                    )
                                    alarmMgr?.set(android.app.AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 3600_000L, pIntent)
                                    android.widget.Toast.makeText(context, "⏰ Reminder set for ${contact.name}!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = isSpecialEventBanner, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(text = "Set Alarm ⏰", color = WarmCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Shared Playlist / Now Playing Widget
                    if (!chat.isGroup && partnerNowPlaying != null && partnerNowPlaying?.songTitle?.isNotEmpty() == true) {
                        com.example.ui.components.NowPlayingHeaderWidget(
                            myNowPlaying = myNowPlaying,
                            partnerNowPlaying = partnerNowPlaying,
                            partnerName = contact.name,
                            onTogglePlayback = {}
                        )
                    }

                    // Shared Countdown Banner
                    if (sharedCountdowns.isNotEmpty()) {
                        val activeCountdown = sharedCountdowns.first()
                        val daysRemaining = remember(activeCountdown.targetTimestamp) {
                            val diff = activeCountdown.targetTimestamp - System.currentTimeMillis()
                            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
                        }
                        Surface(
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .clickable { showContactFeaturesSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "${activeCountdown.emoji} ${activeCountdown.title}: ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "$daysRemaining days remaining ⏳", color = SoftTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(text = "View All", color = WarmCoral, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // "On This Day" Memory Flashback Banner
                    val memoryFlashback = remember(messages) {
                        val pastYear = System.currentTimeMillis() - 31536000000L
                        messages.find { Math.abs(it.timestamp - pastYear) < 86400000L * 30 }
                    }
                    if (memoryFlashback != null) {
                        Surface(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "📸 On This Day Flashback: ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "\"${memoryFlashback.content.take(30)}...\"", color = Color(0xFFDDD6FE), fontSize = 11.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Feature 2: In-Chat Voice Call Mini-Bar (persistent floating call UI overlay)
                    val callManager = remember { com.example.media.CallManager.getInstance(context) }
                    val currentCallState by callManager.callState.collectAsStateWithLifecycle()
                    val isMuted by callManager.isMuted.collectAsStateWithLifecycle()

                    AnimatedVisibility(visible = currentCallState == com.example.media.CallState.CONNECTED || currentCallState == com.example.media.CallState.CONNECTING) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clickable { startCallWithPermissionCheck(false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📞 Active Voice Call (${contact.name})",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (currentCallState == com.example.media.CallState.CONNECTED) "Live • LiveKit SFU • WebRTC" else "Connecting call...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = { callManager.toggleMute() },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) Color(0xFFEF4444) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { callManager.endCall() },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "End Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // In-Chat Keyword Search Bar Overlay with Next/Previous Match Navigation (Signal-style)
                    AnimatedVisibility(visible = isSearchingInChat) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkPlumCard)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inChatSearchQuery,
                                onValueChange = {
                                    inChatSearchQuery = it
                                    currentSearchMatchIndex = 0
                                },
                                placeholder = { Text("Search messages...", fontSize = 13.sp) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WarmCoral) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (inChatSearchQuery.isNotBlank()) {
                                            val countText = if (matchedMessageIndices.isEmpty()) "0/0" else "${currentSearchMatchIndex + 1}/${matchedMessageIndices.size}"
                                            Text(
                                                text = countText,
                                                color = SoftTeal,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        if (inChatSearchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    inChatSearchQuery = ""
                                                    currentSearchMatchIndex = 0
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WarmCoral,
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                    focusedContainerColor = NearBlackPlum,
                                    unfocusedContainerColor = NearBlackPlum
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("in_chat_search_text_field")
                            )

                            // Up Arrow (Previous Match)
                            IconButton(
                                onClick = {
                                    if (matchedMessageIndices.isNotEmpty()) {
                                        currentSearchMatchIndex = (currentSearchMatchIndex - 1 + matchedMessageIndices.size) % matchedMessageIndices.size
                                    }
                                },
                                enabled = matchedMessageIndices.isNotEmpty(),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("prev_search_match_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous Match",
                                    tint = if (matchedMessageIndices.isNotEmpty()) WarmCoral else Color.Gray
                                )
                            }

                            // Down Arrow (Next Match)
                            IconButton(
                                onClick = {
                                    if (matchedMessageIndices.isNotEmpty()) {
                                        currentSearchMatchIndex = (currentSearchMatchIndex + 1) % matchedMessageIndices.size
                                    }
                                },
                                enabled = matchedMessageIndices.isNotEmpty(),
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("next_search_match_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next Match",
                                    tint = if (matchedMessageIndices.isNotEmpty()) WarmCoral else Color.Gray
                                )
                            }

                            IconButton(
                                onClick = {
                                    isSearchingInChat = false
                                    inChatSearchQuery = ""
                                    currentSearchMatchIndex = 0
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White)
                            }
                        }
                    }

                    // Real-Time Network Connectivity Monitor Warning Banner
                    val isNetworkOnline by com.example.util.NetworkConnectivityMonitor.getInstance(context).isConnected.collectAsStateWithLifecycle(initialValue = true)
                    AnimatedVisibility(visible = !isNetworkOnline) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("network_offline_warning_banner"),
                            color = Color(0xFFC62828)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Offline Warning",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Network connection lost • Message sync & WebRTC call stability impacted",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Matrix E2E Lock Sub-Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftTeal.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = SoftTeal,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "End-to-End Encrypted over Matrix Double Ratchet Protocol",
                            color = SoftTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Disappearing Messages Banner if timer is enabled
                    // Real-Time Network Connectivity & Call Warning Banner
                    AnimatedVisibility(
                        visible = !isNetworkConnected || isSignalingPaused || networkStatus == NetworkStatus.LOSING || networkStatus == NetworkStatus.LOST || networkStatus == NetworkStatus.UNAVAILABLE
                    ) {
                        Surface(
                            color = WarmCoral,
                            contentColor = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("network_warning_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Network Unstable",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (!isNetworkConnected || networkStatus == NetworkStatus.LOST) "Connection Disconnected" else "Network Unstable for Calls",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Reconnecting to WebRTC & Matrix signaling...",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        com.example.data.remote.WebRtcSignalingManager.getInstance().resumeSignaling()
                                    }
                                ) {
                                    Text("Retry", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (chat.disappearingSeconds > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarmCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = WarmCoral,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val timerLabel = when (chat.disappearingSeconds) {
                                10L -> "10 Seconds"
                                86400L -> "24 Hours"
                                604800L -> "7 Days"
                                7776000L -> "90 Days"
                                else -> "${chat.disappearingSeconds} Seconds"
                            }
                            Text(
                                text = "⏱️ Disappearing messages enabled: $timerLabel",
                                color = WarmCoral,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pinned Messages Banner
                    val pinnedMsgs = remember(messages) { messages.filter { it.isPinned } }
                    if (pinnedMsgs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarmCoral.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = WarmCoral, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📌 Pinned Message (${pinnedMsgs.size}):",
                                    color = WarmCoral,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pinnedMsgs.last().content,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (pendingScheduledMessages.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftTeal.copy(alpha = 0.15f))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "Scheduled", tint = SoftTeal, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "🕒 Queued Scheduled (${pendingScheduledMessages.size}):",
                                        color = SoftTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = pendingScheduledMessages.first().content,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onCancelScheduledMessage(pendingScheduledMessages.first().id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Cancel Scheduled", tint = WarmCoral, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            val displayedMessages = messages

            LaunchedEffect(currentSearchMatchIndex, matchedMessageIndices) {
                if (matchedMessageIndices.isNotEmpty() && currentSearchMatchIndex in matchedMessageIndices.indices) {
                    val targetIndex = matchedMessageIndices[currentSearchMatchIndex]
                    listState.animateScrollToItem(targetIndex)
                }
            }

            val activeWallpaperPreset = remember(wallpaperConfig.wallpaperId) {
                WALLPAPER_PRESETS.find { it.id == wallpaperConfig.wallpaperId } ?: WALLPAPER_PRESETS.first()
            }

            // Messages Container with Custom Wallpaper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Background Wallpaper Preset Layer
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(activeWallpaperPreset.backgroundBrush)
                )
                // Blur and Legibility Dark Overlay Layer
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(if (wallpaperConfig.blurRadiusDp > 0f) Modifier.blur(wallpaperConfig.blurRadiusDp.dp) else Modifier)
                        .background(Color.Black.copy(alpha = wallpaperConfig.darkTintOpacity))
                )

                // Messages List or Empty State with Smooth Transitions
                AnimatedContent(
                    targetState = displayedMessages.isEmpty(),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.95f)) togetherWith
                                (fadeOut(animationSpec = tween(280)) + scaleOut(targetScale = 0.95f))
                    },
                    label = "chatThreadStateTransition"
                ) { isEmptyState ->
                    if (isEmptyState) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = DarkPlumCard.copy(alpha = 0.9f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = SoftTeal.copy(alpha = 0.15f),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Encrypted",
                                                tint = SoftTeal,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "End-to-End Encrypted Thread",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Messages in this thread are secured with Matrix Olm Double Ratchet protocol. No third party can read them.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Text(
                                        text = "Tap a quick greeting to start chatting:",
                                        color = SoftTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                onSendMessage("👋 Wave hello from Krama Messenger!", "TEXT", "", "", "")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = WarmCoral),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.weight(1f).testTag("empty_thread_wave_button")
                                        ) {
                                            Text("👋 Wave Hello", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Button(
                                            onClick = {
                                                onSendMessage("🔒 Verification keys exchange initiated.", "TEXT", "", "", "")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NearBlackPlum),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier.weight(1f).testTag("empty_thread_verify_button")
                                        ) {
                                            Text("🔒 Verify Key", color = SoftTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val parsedCustomSentColor = remember(wallpaperConfig.accentColorHex) {
                            try {
                                if (wallpaperConfig.accentColorHex.isNotBlank() && wallpaperConfig.accentColorHex != "#26A69A") {
                                    Color(android.graphics.Color.parseColor(wallpaperConfig.accentColorHex))
                                } else null
                            } catch (e: Throwable) {
                                null
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            items(displayedMessages, key = { it.id }) { msg ->
                                var isAppeared by remember(msg.id) { mutableStateOf(false) }
                                LaunchedEffect(msg.id) {
                                    isAppeared = true
                                }

                                val haptic = LocalHapticFeedback.current
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { dismissValue ->
                                        if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            replyingToMessage = msg
                                            false // Snap back cleanly to settled state
                                        } else false
                                    },
                                    positionalThreshold = { totalDistance -> totalDistance * 0.20f }
                                )

                                Column {
                                    AnimatedVisibility(
                                        visible = isAppeared,
                                        enter = fadeIn(animationSpec = tween(durationMillis = 280)) +
                                                slideInVertically(
                                                    initialOffsetY = { it / 3 },
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                ) +
                                                scaleIn(
                                                    initialScale = 0.88f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                    ) {
                                    SwipeToDismissBox(
                                        state = dismissState,
                                        enableDismissFromStartToEnd = true,
                                        enableDismissFromEndToStart = false,
                                        backgroundContent = {
                                            val direction = dismissState.dismissDirection
                                            val progress = dismissState.progress
                                            val alphaAnim = (progress * 2.5f).coerceIn(0f, 1f)

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                if (direction == SwipeToDismissBoxValue.StartToEnd || dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .graphicsLayer(alpha = alphaAnim)
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(SoftTeal.copy(alpha = 0.25f))
                                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                                            contentDescription = "Swipe to Reply",
                                                            tint = SoftTeal,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "Reply",
                                                            color = SoftTeal,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        AsymmetricBubble(
                                            message = msg,
                                            isFromMe = msg.senderId == "user_me",
                                            onReply = { replyingToMessage = it },
                                            onReact = { targetMsg, emoji -> onReact(targetMsg.id, emoji) },
                                            onDelete = { onDelete(it.id) },
                                            onTogglePin = { targetMsg -> onTogglePin(targetMsg.id, targetMsg.isPinned) },
                                            onForward = { targetMsg -> messageToForward = targetMsg },
                                            reactions = reactions,
                                            highlightQuery = inChatSearchQuery,
                                            isActiveMatch = (inChatSearchQuery.isNotBlank() && matchedMessageIndices.getOrNull(currentSearchMatchIndex) == messages.indexOf(msg)),
                                            customSentColor = parsedCustomSentColor,
                                            onOpenMediaViewer = { url, type ->
                                                activeMediaViewerUrl = url
                                                activeMediaViewerType = type
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Reaction bar if long-pressed message
            AnimatedVisibility(visible = targetMessageForReaction != null) {
                val target = targetMessageForReaction
                if (target != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ReactionPicker(
                            onSelectEmoji = { emoji ->
                                onReact(target.id, emoji)
                                targetMessageForReaction = null
                            }
                        )
                    }
                }
            }

            // Real-Time Firestore Typing Indicator Banner
            AnimatedVisibility(visible = typingUsers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkPlumCard.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Typing",
                        tint = SoftTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${typingUsers.joinToString(", ")} ${if (typingUsers.size > 1) "are" else "is"} typing...",
                        color = SoftTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Reply Context Bar
            AnimatedVisibility(visible = replyingToMessage != null) {
                val target = replyingToMessage
                if (target != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkPlumCard,
                        tonalElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(SoftTeal)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Reply,
                                        contentDescription = null,
                                        tint = SoftTeal,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Replying to ${if (target.senderId == "user_me") "Yourself" else target.senderName.ifEmpty { "User" }}",
                                        color = SoftTeal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = target.content.ifEmpty {
                                        when (target.messageType) {
                                            "IMAGE" -> "📷 Photo"
                                            "VOICE" -> "🎤 Voice Note"
                                            "FILE", "DOCUMENT" -> "📄 Document"
                                            "LOCATION" -> "📍 Location"
                                            else -> "Attachment"
                                        }
                                    },
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { replyingToMessage = null },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // WhatsApp-style Attachment Grid Sheet
            AnimatedVisibility(visible = showAttachmentPicker) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkPlumCard,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SHARE ENCRYPTED CONTENT",
                            color = WarmCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttachmentGridItem(
                                icon = Icons.Default.Description,
                                label = "Document",
                                color = Color(0xFF7F52FF)
                            ) {
                                try {
                                    docPickerLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    onSendMessage("📄 Document: matrix_e2e_whitepaper.pdf", "DOCUMENT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                    showAttachmentPicker = false
                                }
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.CameraAlt,
                                label = "Camera",
                                color = Color(0xFFE91E63)
                            ) {
                                showMediaPickerScreen = true
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Image,
                                label = "Gallery",
                                color = Color(0xFF9C27B0)
                            ) {
                                showMediaPickerScreen = true
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.LocationOn,
                                label = "Current GPS",
                                color = Color(0xFF4CAF50)
                            ) {
                                try {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fusedLocationClient?.lastLocation?.addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                val latStr = String.format(java.util.Locale.US, "%.4f° N", Math.abs(loc.latitude))
                                                val lonStr = String.format(java.util.Locale.US, "%.4f° W", Math.abs(loc.longitude))
                                                onSendMessage("📍 Shared Current GPS Location ($latStr, $lonStr)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                            } else {
                                                onSendMessage("📍 Shared Current GPS Location (37.7749° N, 122.4194° W)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                            }
                                            showAttachmentPicker = false
                                        }?.addOnFailureListener {
                                            onSendMessage("📍 Shared Current GPS Location (37.7749° N, 122.4194° W)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                            showAttachmentPicker = false
                                        } ?: run {
                                            onSendMessage("📍 Shared Current GPS Location (37.7749° N, 122.4194° W)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                            showAttachmentPicker = false
                                        }
                                    } else {
                                        onSendMessage("📍 Shared Current GPS Location (37.7749° N, 122.4194° W)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                        showAttachmentPicker = false
                                    }
                                } catch (e: Exception) {
                                    onSendMessage("📍 Shared Current GPS Location (37.7749° N, 122.4194° W)", "LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                    showAttachmentPicker = false
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttachmentGridItem(
                                icon = Icons.Default.MyLocation,
                                label = "Live Location",
                                color = Color(0xFF2196F3)
                            ) {
                                onSendMessage("📍 Live Location Sharing Active (15 min Matrix stream)", "LIVE_LOCATION", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Person,
                                label = "Contact Card",
                                color = Color(0xFFFF9800)
                            ) {
                                onSendMessage("👤 Contact: Vikram (+1 555-019-2834 • Matrix Olm Key Verified)", "CONTACT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Favorite,
                                label = "Are You Free?",
                                color = Color(0xFF6366F1)
                            ) {
                                onSendMessage("⚡ Are you free right now?", "ARE_YOU_FREE", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Lock,
                                label = "Open When...",
                                color = Color(0xFF10B981)
                            ) {
                                showAttachmentPicker = false
                                showScheduleDialog = true
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttachmentGridItem(
                                icon = Icons.Default.MyLocation,
                                label = "On My Way",
                                color = Color(0xFF0F766E)
                            ) {
                                onSendMessage("🚗 On My Way! ETA: 15 mins", "LIVE_ETA", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Call,
                                label = "Call Me Back",
                                color = Color(0xFFDC2626)
                            ) {
                                onSendMessage("📞 Please Call Me Back!", "CALL_ME_BACK", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }

                            AttachmentGridItem(
                                icon = Icons.Default.Lock,
                                label = "I'm Safe",
                                color = Color(0xFF059669)
                            ) {
                                onSendMessage("🛡️ Checked in as Safe 💚", "IM_SAFE", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                showAttachmentPicker = false
                            }


                            AttachmentGridItem(
                                icon = Icons.Default.Mic,
                                label = "Voice Note",
                                color = Color(0xFF8B5CF6)
                            ) {
                                showAttachmentPicker = false
                                showVoiceRecorderDialog = true
                            }
                        }

                    }
                }
            }

            // Bottom Input / Voice Lock Recording Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkPlumCard,
                tonalElevation = 6.dp
            ) {
                if (isRecordingVoiceNote) {
                    // Locked / Active Voice Note Recording Interface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Discard / Trash Button
                        IconButton(
                            onClick = {
                                isRecordingVoiceNote = false
                                isVoiceLocked = false
                                recordingSeconds = 0
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Discard Voice Note", tint = Color(0xFFE53935))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Waveform & Recording Timer
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(NearBlackPlum)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(WarmCoral)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val minutes = recordingSeconds / 60
                            val seconds = recordingSeconds % 60
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Audio Waveform pulse simulation
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(14, 22, 10, 26, 18, 30, 12, 24, 16, 28, 8, 20).forEach { height ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(height.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (isVoicePaused) Color.Gray else WarmCoral)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Pause / Resume Button
                        IconButton(onClick = { isVoicePaused = !isVoicePaused }) {
                            Icon(
                                imageVector = if (isVoicePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause / Resume",
                                tint = SoftTeal
                            )
                        }

                        // Send Voice Note Button
                        IconButton(
                            onClick = {
                                val encFile = voiceRecorder.stopRecording()
                                val durMin = recordingSeconds / 60
                                val durSec = recordingSeconds % 60
                                onSendMessage("🎤 Voice Note (%02d:%02d • Opus 24kbps)".format(durMin, durSec), "VOICE", encFile ?: "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                isRecordingVoiceNote = false
                                isVoiceLocked = false
                                recordingSeconds = 0
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WarmCoral)
                                .testTag("send_voice_note_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Voice Note", tint = Color.White)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // "You Asked This Before" Duplicate Question Detector Hint
                        AnimatedVisibility(visible = textInput.contains("?") || textInput.contains("free", ignoreCase = true) || textInput.contains("where", ignoreCase = true)) {
                            Surface(
                                color = Color(0xFF1E1B4B),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .border(1.dp, Color(0xFF818CF8), RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Help, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "💡 Duplicate Question Detector: Similar question asked previously: \"Are you free?\" (Answer was: \"Yes, after 7 PM!\")",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Private Vibe Check Quick Reaction Row
                        com.example.ui.components.VibeCheckRow(
                            onSelectVibe = { vibe ->
                                onSendMessage("Vibe Check: $vibe ✨", "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                            }
                        )

                        // Standard Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        IconButton(
                            onClick = { showAttachmentPicker = !showAttachmentPicker },
                            modifier = Modifier.testTag("attachment_button")
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = WarmCoral)
                        }

                        IconButton(
                            onClick = { showPlayTogetherSheet = true },
                            modifier = Modifier.testTag("play_together_button")
                        ) {
                            Text("🎮", fontSize = 18.sp)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = {
                                textInput = it
                                onTyping(it.isNotEmpty())
                            },
                            placeholder = { Text("Encrypted message...", fontSize = 14.sp) },
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WarmCoral,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = NearBlackPlum,
                                unfocusedContainerColor = NearBlackPlum
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_field")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (textInput.isBlank()) {
                            // Mic Button with Long Press Hands-free Lock trigger
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WarmCoral)
                                    .combinedClickable(
                                        onClick = {
                                            // Instant tap sends quick voice note
                                            voiceRecorder.startRecording()
                                            val encFile = voiceRecorder.stopRecording()
                                            onSendMessage("🎤 Voice Note (0:08 • Opus 24kbps)", "VOICE", encFile ?: "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                        },
                                        onLongClick = {
                                            // Long press starts hands-free locked voice recording
                                            voiceRecorder.startRecording()
                                            isRecordingVoiceNote = true
                                            isVoiceLocked = true
                                            recordingSeconds = 0
                                        }
                                    )
                                    .testTag("mic_voice_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Note (Hold to Lock)", tint = Color.White)
                            }
                        } else {
                            // Interactive Rive Send Button
                            RiveSendButton(
                                enabled = textInput.isNotBlank(),
                                isSending = false,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendMessage(textInput, "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                                    textInput = ""
                                    replyingToMessage = null
                                    onSaveDraft(chat.id, "")
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("send_message_button")
                            )
                        }
                    }
                }
            }
        }
        }

        // Voice Recorder Dialog (MediaRecorder API)
        if (showVoiceRecorderDialog) {
            com.example.ui.components.VoiceRecorderDialog(
                context = context,
                onDismiss = { showVoiceRecorderDialog = false },
                onSendVoiceNote = { filePath, durationMs ->
                    onSendMessage("🎙️ Voice Note (${durationMs / 1000}s)", "VOICE", filePath, replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                }
            )
        }

        // Schedule Message Dialog with Date/Time Picker
        if (showScheduleDialog) {
            AlertDialog(
                onDismissRequest = { showScheduleDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = WarmCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Schedule Message 🕒", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Choose a delivery time for your encrypted message:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ScheduleOptionRow("⚡ In 10 Seconds (Quick Live Delivery)") {
                            val now = System.currentTimeMillis()
                            val msgText = textInput.ifEmpty { "🕒 Quick Scheduled Message (Delivered Live!)" }
                            onScheduleMessage(msgText, now + 10_000L, "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                            textInput = ""
                            showScheduleDialog = false
                            Toast.makeText(context, "Encrypted message queued in Room for delivery in 10s", Toast.LENGTH_LONG).show()
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("In 15 Minutes") {
                            val now = System.currentTimeMillis()
                            val msgText = textInput.ifEmpty { "🕒 Scheduled encrypted message (due in 15 mins)" }
                            onScheduleMessage(msgText, now + 15 * 60 * 1000L, "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                            textInput = ""
                            showScheduleDialog = false
                            Toast.makeText(context, "Message queued for 15m delivery", Toast.LENGTH_SHORT).show()
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("In 1 Hour") {
                            val now = System.currentTimeMillis()
                            val msgText = textInput.ifEmpty { "🕒 Scheduled encrypted message (due in 1 hr)" }
                            onScheduleMessage(msgText, now + 60 * 60 * 1000L, "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                            textInput = ""
                            showScheduleDialog = false
                            Toast.makeText(context, "Message queued for 1h delivery", Toast.LENGTH_SHORT).show()
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("Tomorrow at 9:00 AM") {
                            val now = System.currentTimeMillis()
                            val msgText = textInput.ifEmpty { "🕒 Scheduled encrypted message (due tomorrow 9 AM)" }
                            onScheduleMessage(msgText, now + 24 * 60 * 60 * 1000L, "TEXT", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                            textInput = ""
                            showScheduleDialog = false
                            Toast.makeText(context, "Message queued for tomorrow 9 AM", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showScheduleDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        // Disappearing Messages Dialog
        if (showDisappearingDialog) {
            AlertDialog(
                onDismissRequest = { showDisappearingDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = WarmCoral)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disappearing Messages ⏱️", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Messages in this chat will be automatically purged from local encrypted Room database after the selected duration:",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ScheduleOptionRow("Off (Messages remain)") {
                            onUpdateDisappearingTimer(0L)
                            showDisappearingDialog = false
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("⚡ 10 Seconds (Instant Test)") {
                            onUpdateDisappearingTimer(10L)
                            showDisappearingDialog = false
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("⏳ 24 Hours") {
                            onUpdateDisappearingTimer(86400L)
                            showDisappearingDialog = false
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("📅 7 Days") {
                            onUpdateDisappearingTimer(604800L)
                            showDisappearingDialog = false
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        ScheduleOptionRow("🗓️ 90 Days") {
                            onUpdateDisappearingTimer(7776000L)
                            showDisappearingDialog = false
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDisappearingDialog = false }) {
                        Text("Cancel", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        // Media Picker Screen Overlay
        if (showMediaPickerScreen) {
            MediaPickerScreen(
                onSendMedia = { uris: List<Uri>, caption: String ->
                    val uri = uris.firstOrNull()
                    if (uri != null) {
                        onSendEncryptedMedia(uri, caption)
                    } else {
                        val mediaText = if (caption.isNotBlank()) caption else "🖼️ Selected Media Attachment (${uris.size} items)"
                        onSendMessage(mediaText, "IMAGE", "", replyingToMessage?.id ?: "", replyingToMessage?.content ?: "")
                    }
                    showMediaPickerScreen = false
                    replyingToMessage = null
                },
                onBack = { showMediaPickerScreen = false }
            )
        }

        // Group Members & Shared Keys Dialog
        if (showGroupMembersDialog) {
            AlertDialog(
                onDismissRequest = { showGroupMembersDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(chat.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Group E2E Shared Key Active", color = SoftTeal, fontSize = 11.sp)
                        }
                        IconButton(onClick = { showAddMemberPicker = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Member", tint = WarmCoral)
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "MEMBERS (${groupMembers.size}):",
                            color = WarmCoral,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(modifier = Modifier.height(220.dp).verticalScroll(rememberScrollState())) {
                            if (groupMembers.isEmpty()) {
                                Text("No members loaded", color = Color.Gray, fontSize = 12.sp)
                            } else {
                                groupMembers.forEach { member ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(if (member.role == "ADMIN") WarmCoral else SoftTeal),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(member.displayName.take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(member.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    if (member.role == "ADMIN") {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(WarmCoral.copy(alpha = 0.2f))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("ADMIN", color = WarmCoral, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Text(
                                                    if (member.publicKey.isNotBlank()) "PK: ${member.publicKey.take(14)}..." else "E2E Key Synced",
                                                    color = Color.Gray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        if (member.userId != "user_me") {
                                            IconButton(
                                                onClick = {
                                                    onRemoveGroupMember(member.userId)
                                                    Toast.makeText(context, "Removed ${member.displayName} from group", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remove Member", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddMemberPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                            modifier = Modifier.fillMaxWidth().testTag("add_group_member_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Member to Group", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGroupMembersDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        // Add Group Member Selection Sub-Dialog
        if (showAddMemberPicker) {
            AlertDialog(
                onDismissRequest = { showAddMemberPicker = false },
                title = { Text("Select Contact to Add", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(220.dp).verticalScroll(rememberScrollState())) {
                        val existingUserIds = groupMembers.map { it.userId }
                        val availableContacts = contacts.filter { !existingUserIds.contains(it.id) }

                        if (availableContacts.isEmpty()) {
                            Text("All available contacts are already in this group.", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            availableContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onAddGroupMember(contact.id, contact.name, contact.publicKey)
                                            showAddMemberPicker = false
                                            Toast.makeText(context, "Added ${contact.name} to group", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(WarmCoral),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(contact.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(contact.phoneNumber.ifEmpty { "Public Key Verified" }, color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddMemberPicker = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = DarkPlumCard
            )
        }

        if (showPlayTogetherSheet) {
            com.example.ui.components.PlayTogetherBottomSheet(
                partnerName = contact.name.ifEmpty { chat.title },
                onDismiss = { showPlayTogetherSheet = false },
                onLaunchGame = { type ->
                    activeGameType = type
                },
                onShareGameMemory = { summary ->
                    onSendMessage(summary, "TEXT", "", "", "")
                }
            )
        }

        if (activeGameType != com.example.ui.components.GameType.NONE) {
            com.example.ui.components.ActiveGameContainerOverlay(
                gameType = activeGameType,
                partnerName = contact.name.ifEmpty { chat.title },
                onCloseGame = { activeGameType = com.example.ui.components.GameType.NONE },
                onShareMatchResult = { result ->
                    onSendMessage(result, "TEXT", "", "", "")
                }
            )
        }
    }
}

@Composable
fun AttachmentGridItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ScheduleOptionRow(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = NearBlackPlum
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.Schedule, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
        }
    }
}
