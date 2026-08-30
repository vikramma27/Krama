package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import kotlinx.coroutines.flow.collect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.domain.model.ChatWallpaperConfig
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.BiometricPromptHelper
import com.example.ui.components.FlowNavigationPill
import com.example.ui.components.FlowTab
import com.example.ui.components.SafetyNumberDialog
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ChatThreadScreen
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileEditScreen
import com.example.ui.screens.QrCodeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusComposerScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.screens.StatusViewerScreen
import com.example.ui.screens.SteganographySentinelScreen
import com.example.ui.theme.KramaTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val networkViewModel: com.example.ui.viewmodel.NetworkStateViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                android.util.Log.i("MainActivity", "POST_NOTIFICATIONS permission granted for FCM real-time push alerts.")
            } else {
                android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission denied by user.")
            }
        }

    private fun checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShortcutIntent(intent)
        checkAndRequestNotificationPermission()

        // Asynchronous database integrity check off main thread
        com.example.data.local.DatabaseInitializer.initializeAndCheckIntegrity(applicationContext) {
            viewModel.performLogout()
        }

        setContent {
            KramaTheme {
                val initCompleted by com.example.KramaApplication.instance.initializationState.collectAsStateWithLifecycle()
                val isOnboarded by viewModel.isOnboarded.collectAsStateWithLifecycle()
                val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
                val isOffline by networkViewModel.isOffline.collectAsStateWithLifecycle()

                val chatSearchQuery by viewModel.chatSearchQuery.collectAsStateWithLifecycle()
                val chats by viewModel.chats.collectAsStateWithLifecycle()
                val archivedChats by viewModel.archivedChats.collectAsStateWithLifecycle()
                val contacts by viewModel.contacts.collectAsStateWithLifecycle()
                val statuses by viewModel.statuses.collectAsStateWithLifecycle()
                val calls by viewModel.calls.collectAsStateWithLifecycle()

                val activeChatId by viewModel.activeChatId.collectAsStateWithLifecycle()
                val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()

                val activeCallContact by viewModel.activeCallContact.collectAsStateWithLifecycle()
                val isCallVideo by viewModel.isCallVideo.collectAsStateWithLifecycle()
                val isCallConnected by viewModel.isCallConnected.collectAsStateWithLifecycle()
                val callState by viewModel.callState.collectAsStateWithLifecycle()
                val isSignalingPaused by viewModel.isSignalingPaused.collectAsStateWithLifecycle()
                val isNetworkConnected by viewModel.isNetworkConnected.collectAsStateWithLifecycle()
                val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
                val activeGroupCallParticipants by viewModel.activeGroupCallParticipants.collectAsStateWithLifecycle()

                val activeStatusStory by viewModel.activeStatusStory.collectAsStateWithLifecycle()
                val activeSafetyNumber by viewModel.activeSafetyNumber.collectAsStateWithLifecycle()

                val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
                val matrixServerStatus by viewModel.matrixServerStatus.collectAsStateWithLifecycle()

                val allReactions by viewModel.allReactions.collectAsStateWithLifecycle()
                val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
                val backupExportStatus by viewModel.backupExportStatus.collectAsStateWithLifecycle()
                val availableLocalBackups by viewModel.availableLocalBackups.collectAsStateWithLifecycle()
                val cloudBackupStatus by viewModel.cloudBackupStatus.collectAsStateWithLifecycle()
                val googleDriveBackupStatus by viewModel.googleDriveBackupStatus.collectAsStateWithLifecycle()

                val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
                val chatStorageUsage by viewModel.chatStorageUsage.collectAsStateWithLifecycle()

                val aiViewModel: com.example.ai.AISettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                val isAiPrivacyConsented by aiViewModel.isPrivacyConsented.collectAsStateWithLifecycle()

                var currentTab by remember { mutableStateOf(FlowTab.CHATS) }
                var isSettingsOpen by remember { mutableStateOf(false) }
                var isProfileEditOpen by remember { mutableStateOf(false) }
                var isContactsOpen by remember { mutableStateOf(false) }
                var isComposingStatus by remember { mutableStateOf(false) }
                var isSentinelOpen by remember { mutableStateOf(false) }
                var isAnalyticsOpen by remember { mutableStateOf(false) }
                var isQrCodeOpen by remember { mutableStateOf(false) }
                var isRoomInspectorOpen by remember { mutableStateOf(false) }
                var isStorageManagerOpen by remember { mutableStateOf(false) }

                // Screen Lock Privacy Flag handling
                val isScreenLockPrivacyEnabled = userProfile.isScreenLockPrivacyEnabled
                androidx.compose.runtime.LaunchedEffect(isScreenLockPrivacyEnabled) {
                    if (isScreenLockPrivacyEnabled) {
                        window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }

                // Active Chat Typing State Flow collection
                val activeChatTypingUsers by androidx.compose.runtime.produceState<List<String>>(initialValue = emptyList(), key1 = activeChatId) {
                    if (activeChatId != null) {
                        viewModel.getTypingUsersFlow(activeChatId!!).collect { value = it }
                    } else {
                        value = emptyList()
                    }
                }

                // App Switcher Privacy Blur & FLAG_SECURE handling
                var isAppInBgAndLocked by remember { mutableStateOf(false) }
                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner, isScreenLockPrivacyEnabled) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
                            isAppInBgAndLocked = true
                            viewModel.notifyAppPaused()
                            com.example.data.repository.FirebasePresenceManager.setOffline(this@MainActivity)
                        } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME || event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                            isAppInBgAndLocked = false
                            if (!isScreenLockPrivacyEnabled) {
                                window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                            }
                            viewModel.notifyAppResumed()
                            com.example.data.repository.FirebasePresenceManager.initializeUserPresence(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        initCompleted.isInitializing || (initCompleted.error != null && !initCompleted.isSuccess) -> {
                            com.example.ui.screens.AppLoadingScreen(
                                state = initCompleted,
                                onRetry = {
                                    com.example.KramaApplication.instance.retryInitialization()
                                },
                                onContinueOffline = {
                                    com.example.KramaApplication.instance.bypassInitializationForOffline()
                                }
                            )
                        }

                        isAppLocked -> {
                            AppLockScreen(
                                onUnlockWithPin = { viewModel.unlockWithPin(it) },
                                onUnlockWithBiometric = {
                                    BiometricPromptHelper.showBiometricPrompt(
                                        activity = this@MainActivity,
                                        onSuccess = { viewModel.unlockWithBiometric() },
                                        onError = { err ->
                                            android.widget.Toast.makeText(this@MainActivity, "Biometric verification: $err", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            )
                        }

                    !isOnboarded -> {
                        val authState by viewModel.authState.collectAsStateWithLifecycle()
                        OnboardingScreen(
                            initialAuthState = authState,
                            onOnboardingComplete = { name, phone, email ->
                                viewModel.completeOnboarding(name, phone, email)
                            }
                        )
                    }

                    activeCallContact != null -> {
                        val contact = activeCallContact!!
                        ActiveCallScreen(
                            contact = contact,
                            isVideo = isCallVideo,
                            callState = callState,
                            isSignalingPaused = isSignalingPaused,
                            isLowPowerModeEnabled = userProfile.isLowDataBatteryMode,
                            groupParticipants = activeGroupCallParticipants,
                            allContacts = contacts,
                            onAddParticipant = { viewModel.addParticipantToActiveCall(it) },
                            onRemoveParticipant = { viewModel.removeParticipantFromActiveCall(it) },
                            onEndCall = { viewModel.endCall() },
                            onToggleNoiseSuppression = { enabled -> viewModel.toggleNoiseSuppression(enabled) },
                            onToggleEchoCancellation = { enabled -> viewModel.toggleEchoCancellation(enabled) }
                        )
                    }

                    activeStatusStory != null -> {
                        StatusViewerScreen(
                            story = activeStatusStory!!,
                            onClose = { viewModel.closeStatusStory() }
                        )
                    }

                    isComposingStatus -> {
                        StatusComposerScreen(
                            onPostStatus = { text, mediaUrl, bgColor, audience ->
                                viewModel.postStatusStory(text, mediaUrl, bgColor, audience)
                                isComposingStatus = false
                            },
                            onBack = { isComposingStatus = false }
                        )
                    }

                    isProfileEditOpen -> {
                        ProfileEditScreen(
                            userProfile = userProfile,
                            onSaveProfile = { name, username, avatarUrl, statusText ->
                                viewModel.updateUserProfile(name, username, avatarUrl, statusText)
                            },
                            onBack = { isProfileEditOpen = false }
                        )
                    }

                    isContactsOpen -> {
                        ContactsScreen(
                            contacts = contacts,
                            onBack = { isContactsOpen = false },
                            onStartChat = { contact ->
                                isContactsOpen = false
                                val existing = chats.find { it.contactId == contact.id }
                                if (existing != null) {
                                    viewModel.openChat(existing.id)
                                } else {
                                    viewModel.openChat("chat_${contact.id}")
                                }
                            },
                            onToggleBlockContact = { contactId, isBlocked ->
                                viewModel.setContactBlocked(contactId, isBlocked)
                            },
                            onAddNewContact = { name, phone, pubKey ->
                                viewModel.addNewContact(name, phone, pubKey)
                            },
                            onOpenQrCode = { isQrCodeOpen = true },
                            onSyncContactsObfuscated = {
                                viewModel.syncContactsObfuscated(this@MainActivity) { total, discovered ->
                                    android.widget.Toast.makeText(
                                        this@MainActivity,
                                        "Synced $total contacts ($discovered active Krama users discovered via SHA-256)",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }

                    isQrCodeOpen -> {
                        QrCodeScreen(
                            userProfile = userProfile,
                            onBack = { isQrCodeOpen = false },
                            onAddContactFromQr = { name, phone, pubKey ->
                                viewModel.addNewContactFromQr(name, phone, pubKey) { newChatId ->
                                    viewModel.openChat(newChatId)
                                    isQrCodeOpen = false
                                }
                            }
                        )
                    }

                    isSentinelOpen -> {
                        SteganographySentinelScreen(
                            onBack = { isSentinelOpen = false },
                            onTriggerPanicLock = {
                                isSentinelOpen = false
                                viewModel.lockAppNow()
                            }
                        )
                    }

                    isAnalyticsOpen -> {
                        AnalyticsScreen(
                            chats = chats,
                            messages = allMessages,
                            onBack = { isAnalyticsOpen = false }
                        )
                    }

                    isRoomInspectorOpen -> {
                        com.example.ui.screens.RoomInspectorScreen(
                            onBack = { isRoomInspectorOpen = false }
                        )
                    }

                    isStorageManagerOpen -> {
                        com.example.ui.screens.StorageManagerScreen(
                            storageStats = storageStats,
                            chatStorageUsage = chatStorageUsage,
                            onBack = { isStorageManagerOpen = false },
                            onRefreshStats = { viewModel.refreshStorageStats() },
                            onClearMediaCache = { days, chatIdFilter, onComplete ->
                                viewModel.clearMediaCacheAndOldMessages(days, chatIdFilter, onComplete)
                            }
                        )
                    }

                    isSettingsOpen -> {
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            viewModel.updateGoogleDriveBackupInfo(this@MainActivity)
                            viewModel.refreshLocalBackups(this@MainActivity)
                        }
                        SettingsScreen(
                            userProfile = userProfile,
                            matrixServerStatus = matrixServerStatus,
                            onBack = { isSettingsOpen = false },
                            onToggleReadReceipts = { viewModel.settingsRepository.toggleReadReceipts(it) },
                            onToggleBiometric = { viewModel.toggleBiometric(it) },
                            onLockAppNow = { viewModel.lockAppNow() },
                            onOpenProfileEdit = { isProfileEditOpen = true },
                            onOpenContacts = { isContactsOpen = true },
                            onOpenSentinel = { isSentinelOpen = true },
                            onOpenAnalytics = { isAnalyticsOpen = true },
                            onOpenQrCode = { isQrCodeOpen = true },
                            onOpenRoomInspector = { isRoomInspectorOpen = true },
                            onOpenStorageManager = { isStorageManagerOpen = true },
                            onSetAutoLockTimeout = { viewModel.setAutoLockTimeoutSeconds(it) },
                            backupExportStatus = backupExportStatus,
                            availableLocalBackups = availableLocalBackups,
                            cloudBackupStatus = cloudBackupStatus,
                            googleDriveBackupStatus = googleDriveBackupStatus,
                            onExportManualBackup = { viewModel.exportManualBackup(this@MainActivity) },
                            onExportLocalBackupWithPassphrase = { passphrase -> viewModel.exportManualBackup(this@MainActivity, passphrase) },
                            onRestoreLocalBackupWithPassphrase = { file, passphrase -> viewModel.restoreLocalBackup(this@MainActivity, file, passphrase) },
                            onRefreshLocalBackups = { viewModel.refreshLocalBackups(this@MainActivity) },
                            onPerformCloudBackup = { viewModel.performCloudBackup() },
                            onBackupToGoogleDrive = { viewModel.backupDatabaseToGoogleDrive(this@MainActivity) },
                            onRestoreFromGoogleDrive = { viewModel.restoreDatabaseFromGoogleDrive(this@MainActivity) },
                            onResetPassword = { email ->
                                viewModel.performPasswordReset(email) { msg ->
                                    android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onToggleLowDataBatteryMode = { viewModel.toggleLowDataBatteryMode(this@MainActivity, it) },
                            onToggleScreenLockPrivacy = { viewModel.toggleScreenLockPrivacy(it) },
                            onSaveWallpaperConfig = { viewModel.updateWallpaperConfig(it) },
                            contactsList = contacts,
                            chatsList = chats,
                            onToggleBlockContact = { contactId, isBlocked -> viewModel.setContactBlocked(contactId, isBlocked) },
                            onSignOut = {
                                viewModel.performLogout()
                                isSettingsOpen = false
                            }
                        )
                    }

                    activeChatId != null -> {
                        val activeChat = chats.find { it.id == activeChatId }
                        if (activeChat != null) {
                            val activeGroupMembers by viewModel.getGroupMembers(activeChat.id).collectAsStateWithLifecycle(emptyList())
                            val activeChatScheduledMessages by viewModel.getPendingScheduledMessagesForChat(activeChat.id).collectAsStateWithLifecycle(emptyList())

                            ChatThreadScreen(
                                chat = activeChat,
                                messages = activeChatMessages,
                                contacts = contacts,
                                groupMembers = activeGroupMembers,
                                onBack = { viewModel.closeChat() },
                                onSendMessage = { text, type, mediaUrl, replyToId, replyToContent ->
                                    viewModel.sendMessage(text, type, mediaUrl, replyToId, replyToContent)
                                },
                                onReact = { msgId, emoji -> viewModel.toggleMessageReaction(msgId, emoji) },
                                onDelete = { msgId -> viewModel.deleteMessage(msgId) },
                                onStartCall = { contact, isVideo -> viewModel.startCall(contact, isVideo) },
                                onVerifySafetyNumber = { chatId, pubKey ->
                                    viewModel.openSafetyNumberVerification(chatId, pubKey)
                                },
                                onUpdateDisappearingTimer = { seconds ->
                                    viewModel.setDisappearingTimer(seconds)
                                },
                                onTogglePin = { msgId, currentIsPinned ->
                                    viewModel.togglePinMessage(msgId, currentIsPinned)
                                },
                                onSaveDraft = { chatId, draft ->
                                    viewModel.saveChatDraft(chatId, draft)
                                },
                                onAddGroupMember = { userId, displayName, pubKey ->
                                    viewModel.addGroupMember(activeChat.id, userId, displayName, pubKey)
                                },
                                onRemoveGroupMember = { userId ->
                                    viewModel.removeGroupMember(activeChat.id, userId)
                                },
                                onScheduleMessage = { text, timeMs, msgType, mediaUrl, replyId, replyContent ->
                                    viewModel.scheduleMessage(activeChat.id, text, timeMs, msgType, mediaUrl, replyId, replyContent)
                                },
                                pendingScheduledMessages = activeChatScheduledMessages,
                                onCancelScheduledMessage = { id -> viewModel.cancelScheduledMessage(id) },
                                reactions = allReactions,
                                wallpaperConfig = userProfile.wallpaperConfig,
                                onSaveWallpaperConfig = { viewModel.updateWallpaperConfig(it) },
                                typingUsers = activeChatTypingUsers,
                                onTyping = { isTyping -> viewModel.sendTypingStatus(isTyping) },
                                onSendEncryptedMedia = { uri, caption ->
                                    viewModel.sendEncryptedMediaAttachment(this@MainActivity, uri, caption)
                                },
                                onForwardMessage = { msg, targetChatId ->
                                    viewModel.forwardMessage(msg, targetChatId)
                                },
                                allChats = chats,
                                messengerRepository = viewModel.messengerRepository,
                                isNetworkConnected = isNetworkConnected,
                                networkStatus = networkStatus,
                                isSignalingPaused = isSignalingPaused
                            )
                        } else {
                            viewModel.closeChat()
                        }
                    }

                    else -> {
                        // Main Tab Feed Container with HorizontalPager tab swipe support & floating navigation pill
                        val tabs = remember { FlowTab.values() }
                        val pagerState = rememberPagerState(initialPage = tabs.indexOf(currentTab).coerceAtLeast(0)) { tabs.size }
                        val coroutineScope = rememberCoroutineScope()

                        androidx.compose.runtime.LaunchedEffect(pagerState.currentPage) {
                            if (tabs[pagerState.currentPage] != currentTab) {
                                currentTab = tabs[pagerState.currentPage]
                            }
                        }

                        androidx.compose.runtime.LaunchedEffect(currentTab) {
                            val targetPage = tabs.indexOf(currentTab)
                            if (targetPage >= 0 && targetPage != pagerState.currentPage) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    when (tabs[page]) {
                                         FlowTab.CHATS -> {
                                            ChatsScreen(
                                                chats = chats,
                                                archivedChats = archivedChats,
                                                contacts = contacts,
                                                statuses = statuses,
                                                allMessages = allMessages,
                                                searchQuery = chatSearchQuery,
                                                onSearchQueryChange = { viewModel.onChatSearchQueryChange(it) },
                                                onChatClick = { viewModel.openChat(it) },
                                                onViewStatusStory = { viewModel.viewStatusStory(it) },
                                                onToggleArchiveChat = { id, isArchived -> viewModel.toggleArchiveChat(id, isArchived) },
                                                onOpenSettings = { isSettingsOpen = true },
                                                onOpenCamera = { isComposingStatus = true },
                                                onOpenSentinel = { isSentinelOpen = true },
                                                onCreateGroupChat = { groupName, selectedContacts ->
                                                    viewModel.createGroupChat(groupName, selectedContacts) { newGroupId ->
                                                        viewModel.openChat(newGroupId)
                                                    }
                                                },
                                                onStartNewChat = { contact ->
                                                    val existing = chats.find { it.contactId == contact.id }
                                                    if (existing != null) {
                                                        viewModel.openChat(existing.id)
                                                    } else {
                                                        val newChatId = "chat_${contact.id}"
                                                        viewModel.openChat(newChatId)
                                                    }
                                                }
                                            )
                                        }

                                        FlowTab.CALLS -> {
                                            CallsScreen(
                                                calls = calls,
                                                contacts = contacts,
                                                chats = chats,
                                                onStartCall = { contact, isVideo ->
                                                    viewModel.startCall(contact, isVideo)
                                                },
                                                onStartGroupCall = { groupChat, members ->
                                                    viewModel.startGroupAudioCall(groupChat, members)
                                                },
                                                onDeleteCallLog = { viewModel.deleteCallLog(it) },
                                                onClearAllCalls = { viewModel.clearAllCallLogs() },
                                                onSimulateCall = { contact, isVideo, isMissed, durationSec ->
                                                    viewModel.simulateIncomingCall(contact, isVideo, isMissed, durationSec)
                                                }
                                            )
                                        }

                                        FlowTab.CONTACTS -> {
                                            ContactsScreen(
                                                contacts = contacts,
                                                onBack = { currentTab = FlowTab.CHATS },
                                                onStartChat = { contact ->
                                                    val existing = chats.find { it.contactId == contact.id }
                                                    if (existing != null) {
                                                        viewModel.openChat(existing.id)
                                                    } else {
                                                        viewModel.openChat("chat_${contact.id}")
                                                    }
                                                },
                                                onToggleBlockContact = { contactId, isBlocked ->
                                                    viewModel.setContactBlocked(contactId, isBlocked)
                                                },
                                                onAddNewContact = { name, phone, pubKey ->
                                                    viewModel.addNewContact(name, phone, pubKey)
                                                },
                                                onOpenQrCode = { isQrCodeOpen = true },
                                                onSyncContactsObfuscated = {
                                                    viewModel.syncContactsObfuscated(this@MainActivity) { total, discovered ->
                                                        android.widget.Toast.makeText(
                                                            this@MainActivity,
                                                            "Synced $total contacts ($discovered active Krama users discovered via SHA-256)",
                                                            android.widget.Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            )
                                        }

                                        FlowTab.AI -> {
                                            if (isAiPrivacyConsented) {
                                                com.example.ui.screens.AIScreen(
                                                    viewModel = aiViewModel,
                                                    recentMessages = allMessages,
                                                    onRevokeConsent = { aiViewModel.revokePrivacyConsent() }
                                                )
                                            } else {
                                                com.example.ui.screens.AIPrivacyConsentScreen(
                                                    onAcceptConsent = { aiViewModel.grantPrivacyConsent() },
                                                    onDeclineConsent = { currentTab = FlowTab.CHATS }
                                                )
                                            }
                                        }

                                        FlowTab.STATUS -> {
                                            StatusScreen(
                                                statuses = statuses,
                                                onStatusClick = { viewModel.viewStatusStory(it) },
                                                onComposeClick = { isComposingStatus = true }
                                            )
                                        }
                                    }
                                }

                                // Floating Navigation Pill at bottom
                                FlowNavigationPill(
                                    selectedTab = currentTab,
                                    onTabSelected = { currentTab = it },
                                    unreadChatsCount = chats.sumOf { it.unreadCount },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                        .padding(bottom = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Reactive Network Connectivity Offline Banner Overlay
                if (isOffline) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkPlumCard)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Offline Mode — End-to-End Encrypted local queue active",
                            color = SoftTeal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Media Sharing & Status Upload Progress Overlay Pill
                com.example.ui.components.MediaUploadProgressPill(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                )

                // E2E Safety Number Verification Dialog overlay
                if (activeSafetyNumber != null) {

                    SafetyNumberDialog(
                        safetyNumber = activeSafetyNumber!!,
                        onDismiss = { viewModel.closeSafetyNumberVerification() }
                    )
                }

                // Global Offline Network Status Overlay Banner
                androidx.compose.animation.AnimatedVisibility(
                    visible = isOffline,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = WarmCoral,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Offline Mode - Reconnecting to Krama Encrypted Network...",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // App Switcher Background Privacy Shield
                if (isAppInBgAndLocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NearBlackPlum.copy(alpha = 0.96f))
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = DarkPlumCard.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SoftTeal.copy(alpha = 0.15f),
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                                            contentDescription = "Privacy Shield",
                                            tint = SoftTeal,
                                            modifier = Modifier.size(38.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = "Krama E2E Privacy Shield Active",
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 18.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "App content obscured for Android task switcher & background privacy",
                                    color = SoftTeal,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        com.example.domain.engine.LifecycleEngine.getInstance(applicationContext)
            .onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        com.example.domain.engine.LifecycleEngine.getInstance(applicationContext)
            .onMultiWindowModeChanged(isInMultiWindowMode)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    private fun handleShortcutIntent(intent: android.content.Intent?) {
        val chatId = intent?.getStringExtra("EXTRA_CHAT_ID")
        if (!chatId.isNullOrEmpty()) {
            viewModel.openChat(chatId)
        }

        val incomingCallId = intent?.getStringExtra("EXTRA_INCOMING_CALL_ID")
        if (!incomingCallId.isNullOrEmpty()) {
            val contactId = intent.getStringExtra("EXTRA_CONTACT_ID") ?: "contact_1"
            val isVideo = intent.getBooleanExtra("EXTRA_IS_VIDEO", false)
            val autoAccept = intent.getBooleanExtra("EXTRA_AUTO_ACCEPT_CALL", false)
            viewModel.initiateCall(contactId, isVideo)
            if (autoAccept) {
                viewModel.acceptCall()
            }
        }
    }
}
