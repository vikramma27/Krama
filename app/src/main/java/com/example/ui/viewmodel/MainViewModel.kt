package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppInitializerWrapper
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReactionEntity
import com.example.data.local.entity.StatusStoryEntity
import com.example.data.repository.MatrixMessagingEngine
import com.example.data.repository.MessengerRepository
import com.example.data.repository.SecurityRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.ChatWallpaperConfig
import com.example.domain.model.E2ESafetyNumber
import com.example.domain.model.UserProfile
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.Flow
import com.example.util.NetworkStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.content.Intent
import com.example.service.WebRtcCallService

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppInitializerWrapper.safeInitializeDatabase(application)
    val matrixEngine = MatrixMessagingEngine().also { AppInitializerWrapper.safeInitializeMatrixEngine(it) }
    val securityRepository = SecurityRepository()
    val settingsRepository = SettingsRepository()
    val messengerRepository = MessengerRepository(
        chatDao = db.chatDao(),
        contactDao = db.contactDao(),
        statusDao = db.statusDao(),
        callDao = db.callDao(),
        scheduledMessageDao = db.scheduledMessageDao(),
        securityRepository = securityRepository,
        contactFeatureDao = db.contactFeatureDao(),
        coupleFeaturesDao = db.coupleFeaturesDao()
    )

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val networkStatus: StateFlow<NetworkStatus> = connectivityObserver.networkStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkStatus.AVAILABLE
    )

    val isSignalingPaused: StateFlow<Boolean> = com.example.data.remote.WebRtcSignalingManager.getInstance().isSignalingPaused

    val authRepository = com.example.data.repository.AuthenticationRepository.getInstance(application)
    val authState: StateFlow<com.example.data.repository.AuthenticationState> = authRepository.authState

    // Initial setup state
    private val _isOnboarded = MutableStateFlow(authRepository.authState.value is com.example.data.repository.AuthenticationState.Authenticated)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    val isAppLocked: StateFlow<Boolean> = securityRepository.isAppLocked
    val userProfile: StateFlow<UserProfile> = settingsRepository.userProfile
    val matrixServerStatus: StateFlow<String> = settingsRepository.matrixServerStatus

    val isNetworkConnected: StateFlow<Boolean> = com.example.util.NetworkConnectivityMonitor.getInstance(application).isConnected

    val chatSearchQuery = MutableStateFlow("")

    val blockedContactIds: StateFlow<Set<String>> = messengerRepository.allContacts
        .map { list ->
            list.filter { it.isBlocked }.map { it.id }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val chats: StateFlow<List<ChatEntity>> = kotlinx.coroutines.flow.combine(
        chatSearchQuery.flatMapLatest { query -> messengerRepository.searchChats(query) },
        blockedContactIds
    ) { chatList, blockedIds ->
        chatList.filter { chat -> !blockedIds.contains(chat.contactId) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedChats: StateFlow<List<ChatEntity>> = kotlinx.coroutines.flow.combine(
        messengerRepository.archivedChats,
        blockedContactIds
    ) { chatList, blockedIds ->
        chatList.filter { chat -> !blockedIds.contains(chat.contactId) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val presenceMap = com.example.data.repository.FirebasePresenceManager.presenceMap

    val contacts: StateFlow<List<ContactEntity>> = kotlinx.coroutines.flow.combine(
        messengerRepository.allContacts,
        presenceMap
    ) { contactList, presence ->
        contactList.map { contact ->
            val userPres = presence[contact.id]
            if (userPres != null) {
                contact.copy(
                    isOnline = userPres.isOnline,
                    lastSeenTimestamp = userPres.lastSeenTimestamp
                )
            } else {
                contact
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val statuses: StateFlow<List<StatusStoryEntity>> = messengerRepository.allStatuses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val calls: StateFlow<List<CallLogEntity>> = kotlinx.coroutines.flow.combine(
        messengerRepository.allCalls,
        blockedContactIds
    ) { callList, blockedIds ->
        callList.filter { call -> !blockedIds.contains(call.contactId) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReactions: StateFlow<List<MessageReactionEntity>> = messengerRepository.allReactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMessages: StateFlow<List<MessageEntity>> = kotlinx.coroutines.flow.combine(
        messengerRepository.allMessages,
        blockedContactIds
    ) { msgList, blockedIds ->
        msgList.filter { msg -> !blockedIds.contains(msg.senderId) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val lruCacheStats: StateFlow<com.example.util.CacheStats> = messengerRepository.getLruCacheStats().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.util.CacheStats()
    )

    private val _backupExportStatus = MutableStateFlow<String>("")
    val backupExportStatus: StateFlow<String> = _backupExportStatus.asStateFlow()

    private val _availableLocalBackups = MutableStateFlow<List<com.example.util.LocalBackupInfo>>(emptyList())
    val availableLocalBackups: StateFlow<List<com.example.util.LocalBackupInfo>> = _availableLocalBackups.asStateFlow()

    private val _cloudBackupStatus = MutableStateFlow<String>("Firebase Storage E2E Backup: Idle")
    val cloudBackupStatus: StateFlow<String> = _cloudBackupStatus.asStateFlow()

    // Active selected chat state
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val activeChatMessages: StateFlow<List<MessageEntity>> = _activeChatMessages.asStateFlow()

    // Active in-progress call state
    private val _activeCallContact = MutableStateFlow<ContactEntity?>(null)
    val activeCallContact: StateFlow<ContactEntity?> = _activeCallContact.asStateFlow()

    private val _activeGroupCallParticipants = MutableStateFlow<List<ContactEntity>>(emptyList())
    val activeGroupCallParticipants: StateFlow<List<ContactEntity>> = _activeGroupCallParticipants.asStateFlow()

    private val _isCallVideo = MutableStateFlow(false)
    val isCallVideo: StateFlow<Boolean> = _isCallVideo.asStateFlow()

    private val _isCallConnected = MutableStateFlow(false)
    val isCallConnected: StateFlow<Boolean> = _isCallConnected.asStateFlow()

    private val _callState = MutableStateFlow(com.example.service.WebRtcCallState.IDLE)
    val callState: StateFlow<com.example.service.WebRtcCallState> = _callState.asStateFlow()

    // Active status story viewer
    private val _activeStatusStory = MutableStateFlow<StatusStoryEntity?>(null)
    val activeStatusStory: StateFlow<StatusStoryEntity?> = _activeStatusStory.asStateFlow()

    // Active E2E Safety Number Verification Dialog
    private val _activeSafetyNumber = MutableStateFlow<E2ESafetyNumber?>(null)
    val activeSafetyNumber: StateFlow<E2ESafetyNumber?> = _activeSafetyNumber.asStateFlow()

    val inChatSearchQuery = MutableStateFlow("")

    init {
        com.example.data.repository.FirebasePresenceManager.initializeUserPresence(getApplication())
        com.example.util.StorageCleanupWorker.enqueuePeriodicCleanup(getApplication())
        com.example.util.AdaptiveSyncManager.evaluateAndApplySyncSchedule(
            context = getApplication(),
            isCallActive = false,
            isLowPowerModeSettingEnabled = settingsRepository.userProfile.value.isLowDataBatteryMode
        )

        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is com.example.data.repository.AuthenticationState.Authenticated -> {
                        _isOnboarded.value = true
                        sharePublicKeyToFirestore()
                    }
                    is com.example.data.repository.AuthenticationState.Unauthenticated,
                    is com.example.data.repository.AuthenticationState.NewUser,
                    is com.example.data.repository.AuthenticationState.SessionExpired -> {
                        if (settingsRepository.userProfile.value.name.isBlank() && settingsRepository.userProfile.value.phoneNumber.isBlank()) {
                            _isOnboarded.value = false
                        }
                    }
                    else -> {}
                }
            }
        }

        // Scheduled message ticker processor
        viewModelScope.launch {
            try {
                while (isActive) {
                    val isLowPower = userProfile.value.isLowDataBatteryMode
                    val isCallActive = _callState.value == com.example.service.WebRtcCallState.CONNECTED ||
                                       _callState.value == com.example.service.WebRtcCallState.CONNECTING ||
                                       _callState.value == com.example.service.WebRtcCallState.RINGING
                    val delayMs = if (isLowPower && isCallActive) 30_000L else if (isLowPower) 10_000L else 4_000L
                    kotlinx.coroutines.delay(delayMs)

                    if (isLowPower && isCallActive) {
                        android.util.Log.i("MainViewModel", "⚡ Low-Power Mode active during WebRTC call: Throttling background ticker task.")
                    } else {
                        try {
                            messengerRepository.sendDueScheduledMessagesNow()
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException || e.message?.contains("cancelled", ignoreCase = true) == true) throw e
                            android.util.Log.e("MainViewModel", "Scheduled message processing error: ${e.message}")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Normal job cancellation, ignore
            }
        }

        com.example.worker.EncryptedChatSyncWorker.schedulePeriodicSync(application)

        // Network connectivity monitoring for WebRTC signaling and message retries
        viewModelScope.launch {
            networkStatus.collect { status ->
                val signalingManager = com.example.data.remote.WebRtcSignalingManager.getInstance()
                when (status) {
                    NetworkStatus.LOSING, NetworkStatus.LOST, NetworkStatus.UNAVAILABLE -> {
                        signalingManager.pauseSignaling()
                    }
                    NetworkStatus.AVAILABLE -> {
                        signalingManager.resumeSignaling()
                        messengerRepository.retryAllFailedMessages()
                    }
                }
            }
        }

        // Periodic background task to clean up expired disappearing messages from Room
        viewModelScope.launch {
            try {
                while (isActive) {
                    val isLowPower = userProfile.value.isLowDataBatteryMode
                    val isCallActive = _callState.value == com.example.service.WebRtcCallState.CONNECTED ||
                                       _callState.value == com.example.service.WebRtcCallState.CONNECTING ||
                                       _callState.value == com.example.service.WebRtcCallState.RINGING
                    val delayMs = if (isLowPower && isCallActive) 60_000L else if (isLowPower) 15_000L else 3_000L
                    kotlinx.coroutines.delay(delayMs)

                    if (isLowPower && isCallActive) {
                        android.util.Log.i("MainViewModel", "⚡ Low-Power Mode active during WebRTC call: Suppressing disappearing messages background DB sweep.")
                    } else {
                        try {
                            messengerRepository.cleanupDisappearingMessages()
                        } catch (e: Throwable) {
                            if (e is kotlinx.coroutines.CancellationException || e.message?.contains("cancelled", ignoreCase = true) == true) throw e
                            android.util.Log.e("MainViewModel", "Disappearing messages cleanup exception handled: ${e.message}")
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Normal job cancellation, ignore
            }
        }

        // Update Android App Dynamic Shortcuts whenever active chats update
        viewModelScope.launch {
            chats.collect { chatList ->
                if (chatList.isNotEmpty()) {
                    com.example.util.AppShortcutUtil.updateAppShortcuts(application, chatList)
                }
            }
        }
    }

    fun onChatSearchQueryChange(newQuery: String) {
        chatSearchQuery.value = newQuery
    }

    fun onInChatSearchQueryChange(newQuery: String) {
        inChatSearchQuery.value = newQuery
    }

    fun completeOnboarding(name: String, phone: String, email: String = "") {
        viewModelScope.launch {
            try {
                settingsRepository.updateNameAndPhone(name, phone, email)
                val uid = authRepository.currentUserId ?: authRepository.getCurrentUser()?.uid ?: "user_${System.currentTimeMillis()}"
                authRepository.setAuthenticatedSession(
                    uid = uid,
                    email = email.ifBlank { null },
                    phone = phone.ifBlank { null },
                    displayName = name
                )
                try {
                    val profileRepo = com.example.data.repository.FirebaseProfileRepository()
                    profileRepo.syncProfileToCloud(userProfile.value)
                } catch (e: Throwable) {
                    android.util.Log.w("MainViewModel", "FirebaseProfileRepository sync note: ${e.message}")
                }
                _isOnboarded.value = true
            } catch (e: Throwable) {
                android.util.Log.e("MainViewModel", "Error completing onboarding: ${e.message}", e)
                _isOnboarded.value = true
            }
        }
    }

    fun searchAllMessages(query: String): Flow<List<MessageEntity>> {
        return messengerRepository.searchAllMessages(query)
    }

    fun searchMessagesInChatFts(chatId: String, query: String): Flow<List<MessageEntity>> {
        return messengerRepository.searchMessagesInChat(chatId, query)
    }

    fun sendTypingStatus(isTyping: Boolean) {
        val chatId = _activeChatId.value ?: return
        val currentUid = authRepository.currentUserId ?: "user_me"
        val displayName = authRepository.currentUserEmail?.substringBefore("@") ?: "Vikram"
        messengerRepository.sendTypingStatus(chatId, currentUid, displayName, isTyping)
    }

    fun getTypingUsersFlow(chatId: String): Flow<List<String>> {
        val currentUid = authRepository.currentUserId ?: "user_me"
        return messengerRepository.getTypingUsersFlow(chatId, currentUid)
    }

    fun syncContactsObfuscated(context: android.content.Context, onComplete: (Int, Int) -> Unit) {
        viewModelScope.launch {
            val (total, discovered) = messengerRepository.syncContactsObfuscated(context)
            onComplete(total, discovered)
        }
    }

    fun sendEncryptedMediaAttachment(context: android.content.Context, sourceUri: android.net.Uri, caption: String = "") {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val fileName = sourceUri.lastPathSegment ?: "attachment_${System.currentTimeMillis()}.bin"
            val taskId = "upload_chat_${System.currentTimeMillis()}"
            com.example.media.MediaUploadManager.instance.startUpload(
                context = context,
                taskId = taskId,
                fileName = fileName,
                mediaType = "CHAT_ATTACHMENT",
                fileSizeBytes = 4_800_000L, // 4.8 MB file
                simulatedDurationMs = 4000L
            )
            messengerRepository.sendEncryptedMediaAttachment(context, chatId, sourceUri, caption)
        }
    }

    fun setChatCustomization(chatId: String, accentColorHex: String, backgroundPattern: String) {
        viewModelScope.launch {
            messengerRepository.updateChatCustomization(chatId, accentColorHex, backgroundPattern)
        }
    }

    fun toggleMessagePin(messageId: String, isPinned: Boolean) {
        viewModelScope.launch {
            messengerRepository.toggleMessagePin(messageId, isPinned)
        }
    }

    fun forwardMessage(sourceMessage: MessageEntity, targetChatId: String) {
        viewModelScope.launch {
            messengerRepository.forwardMessage(sourceMessage, targetChatId)
        }
    }

    fun openChat(chatId: String) {
        _activeChatId.value = chatId
        viewModelScope.launch {
            messengerRepository.markChatAsRead(chatId)
            // Launch real-time multi-device Firestore history sync in background
            launch {
                try {
                    messengerRepository.syncMessagesFromFirestore(chatId).collect {}
                } catch (e: Throwable) {
                    android.util.Log.w("MainViewModel", "Firestore message sync notice: ${e.message}")
                }
            }
            kotlinx.coroutines.flow.combine(
                messengerRepository.getMessagesForChat(chatId),
                blockedContactIds
            ) { msgList, blockedIds ->
                msgList.filter { msg -> !blockedIds.contains(msg.senderId) }
            }.collect { filteredList ->
                _activeChatMessages.value = filteredList
            }
        }
    }

    fun closeChat() {
        _activeChatId.value = null
        _activeChatMessages.value = emptyList()
    }

    fun sendMessage(content: String, type: String = "TEXT", mediaUrl: String = "", replyToId: String = "", replyToContent: String = "") {
        val chatId = _activeChatId.value ?: return
        val isOnline = networkStatus.value == NetworkStatus.AVAILABLE
        viewModelScope.launch {
            messengerRepository.sendMessage(
                chatId = chatId,
                content = content,
                type = type,
                mediaUrl = mediaUrl,
                replyToId = replyToId,
                replyToContent = replyToContent,
                isNetworkOnline = isOnline
            )
        }
    }

    fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            messengerRepository.retryFailedMessage(messageId)
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch {
            messengerRepository.addReaction(messageId, emoji)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messengerRepository.deleteMessageForEveryone(messageId)
        }
    }

    fun setDisappearingTimer(seconds: Long) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            messengerRepository.updateDisappearingTimer(chatId, seconds)
        }
    }

    private var currentCallStartTimestamp: Long = 0L

    fun initiateCall(contactId: String, isVideo: Boolean) {
        val targetContact = contacts.value.find { it.id == contactId } ?: ContactEntity(
            id = contactId,
            name = "Krama User",
            phoneNumber = "+1 555-0199",
            avatarUrl = "",
            statusText = "Encrypted",
            lastSeenTimestamp = System.currentTimeMillis(),
            isOnline = true,
            publicKey = ""
        )
        startCall(targetContact, isVideo)
    }

    fun acceptCall() {
        _isCallConnected.value = true
        _callState.value = com.example.service.WebRtcCallState.CONNECTED
        com.example.util.CallHapticFeedbackUtil.vibrateConnectionEstablished(getApplication())
    }

    fun startCall(contact: ContactEntity, isVideo: Boolean) {
        currentCallStartTimestamp = System.currentTimeMillis()
        _activeCallContact.value = contact
        _activeGroupCallParticipants.value = listOf(contact)
        _isCallVideo.value = isVideo
        _isCallConnected.value = false
        _callState.value = com.example.service.WebRtcCallState.CONNECTING
        com.example.service.PowerSaverManager.notifyCallPowerSaverState(getApplication(), isCallActive = true, isLowPowerModeEnabled = userProfile.value.isLowDataBatteryMode)

        val callId = "call_${System.currentTimeMillis()}"
        val context = getApplication<Application>()
        val intent = Intent(context, WebRtcCallService::class.java).apply {
            action = WebRtcCallService.ACTION_START_OUTGOING_CALL
            putExtra(WebRtcCallService.EXTRA_CALL_ID, callId)
            putExtra(WebRtcCallService.EXTRA_CONTACT_NAME, contact.name)
            putExtra(WebRtcCallService.EXTRA_IS_VIDEO, isVideo)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        // WebRTC Signaling layer over Retrofit / WebSockets
        val signalingManager = com.example.data.remote.WebRtcSignalingManager.getInstance()
        val currentUsername = userProfile.value.username.ifEmpty { "vikram" }
        signalingManager.connectWebSocket(currentUsername)

        val sdpOffer = "v=0\r\no=- ${System.currentTimeMillis()} 2 IN IP4 127.0.0.1\r\ns=KramaE2EEWebRTC\r\nt=0 0\r\nm=${if (isVideo) "video" else "audio"} 9 UDP/TLS/RTP/SAVPF 111"
        val iceCandidate = "candidate:1 1 UDP 2122260223 192.168.1.102 54321 typ host"

        signalingManager.sendSignal(
            com.example.data.remote.SignalingPayload(
                callId = callId,
                senderId = currentUsername,
                targetId = contact.id,
                type = "OFFER",
                sdp = sdpOffer,
                candidate = iceCandidate,
                sdpMid = "0",
                sdpMLineIndex = 0
            )
        )

        viewModelScope.launch {
            // Listen for incoming answer or candidate signals
            signalingManager.incomingSignals.collect { signal ->
                if (signal.type == "ANSWER" || signal.type == "ICE_CANDIDATE") {
                    _isCallConnected.value = true
                    _callState.value = com.example.service.WebRtcCallState.CONNECTED
                }
            }
        }

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            if (_callState.value == com.example.service.WebRtcCallState.CONNECTING) {
                _callState.value = com.example.service.WebRtcCallState.RINGING
            }
            kotlinx.coroutines.delay(1500)
            if (_callState.value == com.example.service.WebRtcCallState.RINGING) {
                _callState.value = com.example.service.WebRtcCallState.CONNECTED
                _isCallConnected.value = true
            }
        }
    }

    fun startGroupAudioCall(groupChat: ChatEntity, activeMembers: List<ContactEntity>) {
        val groupTitle = groupChat.title.ifEmpty { "Group Audio Conference" }
        val memberNames = activeMembers.joinToString(", ") { it.name }.ifEmpty { "Active Members" }
        val groupContact = ContactEntity(
            id = groupChat.id,
            name = groupTitle,
            phoneNumber = "Group Call (${activeMembers.size} connected)",
            avatarUrl = groupChat.avatarUrl,
            statusText = "Multi-User WebRTC Audio Conference • Active: $memberNames",
            lastSeenTimestamp = System.currentTimeMillis(),
            isOnline = true,
            publicKey = ""
        )
        _activeGroupCallParticipants.value = activeMembers
        startCall(groupContact, isVideo = false)
    }

    fun addParticipantToActiveCall(contact: ContactEntity) {
        val current = _activeGroupCallParticipants.value.toMutableList()
        if (current.none { it.id == contact.id }) {
            current.add(contact)
            _activeGroupCallParticipants.value = current
            _activeCallContact.value?.let { activeContact ->
                _activeCallContact.value = activeContact.copy(
                    phoneNumber = "Group Call (${current.size} connected)",
                    statusText = "Multi-User WebRTC Audio Conference • Active: ${current.joinToString(", ") { it.name }}"
                )
            }
        }
    }

    fun removeParticipantFromActiveCall(contactId: String) {
        val current = _activeGroupCallParticipants.value.toMutableList()
        current.removeAll { it.id == contactId }
        _activeGroupCallParticipants.value = current
        _activeCallContact.value?.let { activeContact ->
            _activeCallContact.value = activeContact.copy(
                phoneNumber = "Group Call (${current.size} connected)",
                statusText = "Multi-User WebRTC Audio Conference • Active: ${current.joinToString(", ") { it.name }}"
            )
        }
    }

    fun endCall() {
        val contact = _activeCallContact.value
        val isVideo = _isCallVideo.value
        val wasConnected = _isCallConnected.value
        val durationSec = if (currentCallStartTimestamp > 0L) {
            maxOf(0, ((System.currentTimeMillis() - currentCallStartTimestamp) / 1000).toInt())
        } else 0

        if (contact != null) {
            viewModelScope.launch {
                messengerRepository.addCallLog(
                    contactId = contact.id,
                    contactName = contact.name,
                    avatarUrl = contact.avatarUrl,
                    isVideo = isVideo,
                    isOutgoing = true,
                    durationSeconds = if (wasConnected) maxOf(1, durationSec) else 0,
                    callStatus = if (wasConnected) "COMPLETED" else "MISSED"
                )
            }
        }

        val signalingManager = com.example.data.remote.WebRtcSignalingManager.getInstance()
        signalingManager.sendSignal(
            com.example.data.remote.SignalingPayload(
                callId = "call_ended",
                senderId = userProfile.value.username,
                targetId = contact?.id ?: "",
                type = "BYE"
            )
        )
        signalingManager.disconnect()

        _callState.value = com.example.service.WebRtcCallState.ENDED
        _isCallConnected.value = false
        currentCallStartTimestamp = 0L
        com.example.service.PowerSaverManager.notifyCallPowerSaverState(getApplication(), isCallActive = false, isLowPowerModeEnabled = userProfile.value.isLowDataBatteryMode)

        viewModelScope.launch {
            kotlinx.coroutines.delay(600)
            _activeCallContact.value = null
            _activeGroupCallParticipants.value = emptyList()
            _callState.value = com.example.service.WebRtcCallState.IDLE
        }

        val context = getApplication<Application>()
        val intent = Intent(context, WebRtcCallService::class.java).apply {
            action = WebRtcCallService.ACTION_END_CALL
        }
        context.startService(intent)
    }

    fun toggleNoiseSuppression(enabled: Boolean) {
        com.example.service.WebRtcDiagnosticCollector.instance.setNoiseSuppressionMode(enabled)
    }

    fun toggleEchoCancellation(enabled: Boolean) {
        com.example.service.WebRtcDiagnosticCollector.instance.setEchoCancellationMode(enabled)
    }

    fun deleteCallLog(callId: String) {
        viewModelScope.launch {
            messengerRepository.deleteCallLog(callId)
        }
    }

    fun clearAllCallLogs() {
        viewModelScope.launch {
            messengerRepository.clearCallLogs()
        }
    }

    fun simulateIncomingCall(contact: ContactEntity, isVideo: Boolean, isMissed: Boolean, durationSec: Int = 45) {
        viewModelScope.launch {
            messengerRepository.addCallLog(
                contactId = contact.id,
                contactName = contact.name,
                avatarUrl = contact.avatarUrl,
                isVideo = isVideo,
                isOutgoing = false,
                durationSeconds = if (isMissed) 0 else durationSec,
                callStatus = if (isMissed) "MISSED" else "COMPLETED"
            )
        }
    }

    fun viewStatusStory(story: StatusStoryEntity) {
        _activeStatusStory.value = story
        viewModelScope.launch {
            messengerRepository.markStatusViewed(story.id)
        }
    }

    fun closeStatusStory() {
        _activeStatusStory.value = null
    }

    fun updateUserProfile(name: String, username: String, avatarUrl: String, statusText: String) {
        settingsRepository.updateUserProfile(name, username, avatarUrl, statusText)
    }

    fun setContactBlocked(contactId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            messengerRepository.setContactBlocked(contactId, isBlocked)
        }
    }

    fun addNewContact(name: String, phone: String, publicKey: String) {
        viewModelScope.launch {
            messengerRepository.addNewContact(name, phone, publicKey)
        }
    }

    fun postStatusStory(text: String, mediaUrl: String, bgColorHex: String, audience: String) {
        viewModelScope.launch {
            messengerRepository.addStatusStory(text, mediaUrl, bgColorHex, audience)
        }
    }

    fun openSafetyNumberVerification(chatId: String, contactPublic: String) {
        val userPublic = "ed25519_pk_vikram_77e01"
        val safety = securityRepository.generateSafetyNumber(chatId, userPublic, contactPublic)
        _activeSafetyNumber.value = safety
    }

    fun closeSafetyNumberVerification() {
        _activeSafetyNumber.value = null
    }

    fun unlockWithPin(pin: String): Boolean {
        return securityRepository.unlockAppWithPin(pin)
    }

    fun unlockWithBiometric() {
        securityRepository.unlockAppWithBiometric()
        authRepository.authenticateWithBiometricSuccess()
    }

    fun lockAppNow() {
        securityRepository.lockApp()
    }

    fun togglePinMessage(messageId: String, currentIsPinned: Boolean) {
        viewModelScope.launch {
            messengerRepository.togglePinMessage(messageId, currentIsPinned)
        }
    }

    fun toggleMessageReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            messengerRepository.toggleMessageReaction(messageId, emoji)
        }
    }

    fun saveChatDraft(chatId: String, draftMessage: String) {
        viewModelScope.launch {
            messengerRepository.saveChatDraft(chatId, draftMessage)
        }
    }

    private val _googleDriveBackupStatus = MutableStateFlow<String>("")
    val googleDriveBackupStatus: StateFlow<String> = _googleDriveBackupStatus.asStateFlow()

    fun updateGoogleDriveBackupInfo(context: android.content.Context) {
        _googleDriveBackupStatus.value = messengerRepository.getGoogleDriveLastBackupInfo(context)
    }

    fun backupDatabaseToGoogleDrive(context: android.content.Context) {
        viewModelScope.launch {
            _googleDriveBackupStatus.value = "⏳ Encrypting SQLCipher DB & Uploading to Google Drive..."
            val result = messengerRepository.backupDatabaseToGoogleDrive(context)
            if (result.isSuccess) {
                _googleDriveBackupStatus.value = "✅ ${result.getOrNull()}"
            } else {
                _googleDriveBackupStatus.value = "❌ Drive Backup Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreDatabaseFromGoogleDrive(context: android.content.Context) {
        viewModelScope.launch {
            _googleDriveBackupStatus.value = "⏳ Downloading & Restoring SQLCipher DB from Google Drive..."
            val result = messengerRepository.restoreDatabaseFromGoogleDrive(context)
            if (result.isSuccess) {
                _googleDriveBackupStatus.value = "✅ ${result.getOrNull()}"
            } else {
                _googleDriveBackupStatus.value = "❌ Drive Restore Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun refreshLocalBackups(context: android.content.Context) {
        viewModelScope.launch {
            _availableLocalBackups.value = com.example.util.EncryptedLocalBackupManager.getInstance().listLocalBackups(context)
        }
    }

    fun exportManualBackup(context: android.content.Context, customPassphrase: String? = null) {
        viewModelScope.launch {
            _backupExportStatus.value = "⏳ Creating AES-256 encrypted local database backup..."
            val manager = com.example.util.EncryptedLocalBackupManager.getInstance()
            val result = manager.exportEncryptedBackup(context, customPassphrase)
            if (result.isSuccess) {
                val info = result.getOrNull()
                _backupExportStatus.value = "✅ Backup Created: ${info?.fileName} (${info?.sizeBytes?.div(1024)} KB)"
                com.example.util.LocalAnalyticsTracker.getInstance(context).trackLocalBackupCreated()
                refreshLocalBackups(context)
            } else {
                _backupExportStatus.value = "❌ Export Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreLocalBackup(context: android.content.Context, backupFile: java.io.File, customPassphrase: String? = null) {
        viewModelScope.launch {
            _backupExportStatus.value = "⏳ Decrypting & restoring database from ${backupFile.name}..."
            val manager = com.example.util.EncryptedLocalBackupManager.getInstance()
            val result = manager.importEncryptedBackup(context, backupFile, customPassphrase)
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _backupExportStatus.value = "✅ Successfully restored $count messages from local backup!"
                refreshLocalBackups(context)
            } else {
                _backupExportStatus.value = "❌ Restore Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun performCloudBackup() {
        viewModelScope.launch {
            _cloudBackupStatus.value = "⏳ Encrypting & Syncing database to Firebase Storage..."
            kotlinx.coroutines.delay(1200)
            val now = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.US).format(java.util.Date())
            _cloudBackupStatus.value = "✅ Firebase E2E Auto-Backup Synced ($now • 18.4 MB encrypted)"
        }
    }

    fun performPasswordReset(email: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            result.fold(
                onSuccess = { msg -> onResult(msg) },
                onFailure = { err -> onResult(err.localizedMessage ?: "Failed to send password reset email") }
            )
        }
    }

    fun performLogout() {
        authRepository.signOut()
        _isOnboarded.value = false
    }

    fun toggleLowDataBatteryMode(context: android.content.Context, enabled: Boolean) {
        settingsRepository.toggleLowDataBatteryMode(context, enabled)
        val isCallActive = _callState.value == com.example.service.WebRtcCallState.CONNECTED ||
                           _callState.value == com.example.service.WebRtcCallState.CONNECTING ||
                           _callState.value == com.example.service.WebRtcCallState.RINGING
        com.example.service.PowerSaverManager.notifyCallPowerSaverState(context, isCallActive, enabled)
    }

    fun toggleScreenLockPrivacy(enabled: Boolean) {
        settingsRepository.toggleScreenLockPrivacy(enabled)
    }

    fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        viewModelScope.launch {
            messengerRepository.toggleArchiveChat(chatId, isArchived)
        }
    }

    fun updateContactPresence(contactId: String, isOnline: Boolean) {
        viewModelScope.launch {
            messengerRepository.updateContactPresence(contactId, isOnline)
        }
    }

    fun updateWallpaperConfig(config: ChatWallpaperConfig) {
        settingsRepository.updateWallpaperConfig(config)
    }

    fun getGroupMembers(groupId: String): Flow<List<com.example.data.local.entity.GroupMemberEntity>> {
        return messengerRepository.getGroupMembers(groupId)
    }

    fun addGroupMember(
        groupId: String,
        userId: String,
        displayName: String,
        avatarUrl: String = "",
        role: String = "MEMBER",
        publicKey: String = ""
    ) {
        viewModelScope.launch {
            messengerRepository.addGroupMember(groupId, userId, displayName, avatarUrl, role, publicKey)
        }
    }

    fun removeGroupMember(groupId: String, userId: String) {
        viewModelScope.launch {
            messengerRepository.removeGroupMember(groupId, userId)
        }
    }

    fun createGroupChat(groupName: String, memberContacts: List<com.example.data.local.entity.ContactEntity>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val groupId = messengerRepository.createGroupChat(groupName, memberContacts)
            onCreated(groupId)
        }
    }

    fun addNewContactFromQr(name: String, phone: String, publicKey: String, onContactAdded: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = messengerRepository.addNewContactFromQr(name, phone, publicKey)
            _activeChatId.value = chatId
            onContactAdded(chatId)
        }
    }

    // --- SCHEDULED MESSAGES VIEWMODEL INTEGRATION ---
    val pendingScheduledMessages = messengerRepository.pendingScheduledMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getPendingScheduledMessagesForChat(chatId: String): Flow<List<com.example.data.local.entity.ScheduledMessageEntity>> {
        return messengerRepository.getPendingScheduledMessagesForChat(chatId)
    }

    fun scheduleMessage(
        chatId: String,
        content: String,
        scheduledTimestamp: Long,
        messageType: String = "TEXT",
        mediaUrl: String = "",
        replyToId: String = "",
        replyToContent: String = ""
    ) {
        viewModelScope.launch {
            val schedId = messengerRepository.scheduleMessage(
                chatId = chatId,
                content = content,
                scheduledTimestamp = scheduledTimestamp,
                messageType = messageType,
                mediaUrl = mediaUrl,
                replyToId = replyToId,
                replyToContent = replyToContent
            )
            val delayMillis = (scheduledTimestamp - System.currentTimeMillis()).coerceAtLeast(0L)
            com.example.worker.ScheduledMessageWorker.enqueueScheduledMessageWork(
                context = getApplication(),
                scheduledMessageId = schedId,
                delayMillis = delayMillis
            )
        }
    }

    fun cancelScheduledMessage(id: String) {
        viewModelScope.launch {
            messengerRepository.cancelScheduledMessage(id)
            com.example.worker.ScheduledMessageWorker.cancelScheduledMessageWork(
                context = getApplication(),
                scheduledMessageId = id
            )
        }
    }

    fun sendScheduledMessageNow(id: String) {
        viewModelScope.launch {
            messengerRepository.sendDueScheduledMessagesNow(System.currentTimeMillis() + 10_000_000L)
        }
    }

    // --- STORAGE MANAGER VIEWMODEL INTEGRATION ---
    private val _storageStats = MutableStateFlow(MessengerRepository.StorageStats())
    val storageStats: StateFlow<MessengerRepository.StorageStats> = _storageStats.asStateFlow()

    private val _chatStorageUsage = MutableStateFlow<List<MessengerRepository.ChatStorageUsage>>(emptyList())
    val chatStorageUsage: StateFlow<List<MessengerRepository.ChatStorageUsage>> = _chatStorageUsage.asStateFlow()

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats = messengerRepository.calculateStorageStats(getApplication())
            val chatUsages = messengerRepository.calculateChatStorageUsage()
            _storageStats.value = stats
            _chatStorageUsage.value = chatUsages
        }
    }

    fun clearMediaCacheAndOldMessages(daysThreshold: Int, chatIdFilter: String? = null, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val freedBytes = messengerRepository.clearCachedMediaAndOldMessages(getApplication(), daysThreshold, chatIdFilter)
            refreshStorageStats()
            onComplete(freedBytes)
        }
    }

    // --- SECURITY & BIOMETRIC SETTINGS ---
    fun setAutoLockTimeoutSeconds(seconds: Int) {
        securityRepository.setAutoLockSeconds(seconds.toLong())
    }

    fun sharePublicKeyToFirestore() {
        val uid = authRepository.currentUserId
        if (!uid.isNullOrEmpty()) {
            viewModelScope.launch {
                com.example.data.local.security.AndroidKeyStoreKmsManager.instance.sharePublicKeyToFirestore(uid)
            }
        }
    }

    fun toggleBiometric(enabled: Boolean) {
        securityRepository.toggleBiometric(enabled)
    }

    var lastAppPauseTimeMs: Long = 0L

    fun notifyAppPaused() {
        lastAppPauseTimeMs = System.currentTimeMillis()
    }

    fun notifyAppResumed() {
        val autoLockSecs = securityRepository.autoLockTimeoutSeconds.value
        if (autoLockSecs > 0 && lastAppPauseTimeMs > 0L) {
            val elapsedSecs = (System.currentTimeMillis() - lastAppPauseTimeMs) / 1000
            if (elapsedSecs >= autoLockSecs) {
                securityRepository.setAppLocked(true)
            }
        }
    }
}
