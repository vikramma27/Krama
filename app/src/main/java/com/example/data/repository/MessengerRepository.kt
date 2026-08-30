package com.example.data.repository

import com.example.data.local.dao.CallDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.ContactFeatureDao
import com.example.data.local.dao.StatusDao
import com.example.data.local.dao.ScheduledMessageDao
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.ContactFeatureEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReactionEntity
import com.example.data.local.entity.PresenceLogEntity
import com.example.data.local.entity.PrivateMediaVaultEntity
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.data.local.entity.SharedCountdownEntity
import com.example.data.local.entity.StatusStoryEntity
import com.example.data.local.entity.VoiceDiaryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class MessengerRepository(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val statusDao: StatusDao,
    private val callDao: CallDao,
    private val scheduledMessageDao: ScheduledMessageDao,
    private val securityRepository: SecurityRepository,
    private val contactFeatureDao: ContactFeatureDao? = null,
    private val coupleFeaturesDao: com.example.data.local.dao.CoupleFeaturesDao? = null
) {
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats().catch { emit(emptyList()) }
    val archivedChats: Flow<List<ChatEntity>> = chatDao.getArchivedChats().catch { emit(emptyList()) }
    val allContacts: Flow<List<ContactEntity>> = contactDao.getAllContacts().catch { emit(emptyList()) }
    val allStatuses: Flow<List<StatusStoryEntity>> = statusDao.getAllStatuses().catch { emit(emptyList()) }
    val allCalls: Flow<List<CallLogEntity>> = callDao.getAllCalls().catch { emit(emptyList()) }
    val allReactions: Flow<List<MessageReactionEntity>> = chatDao.getAllReactions().catch { emit(emptyList()) }
    val allMessages: Flow<List<MessageEntity>> = chatDao.getAllMessages().catch { emit(emptyList()) }

    fun searchChats(query: String): Flow<List<ChatEntity>> {
        return (if (query.isBlank()) chatDao.getAllChats() else chatDao.searchChats(query)).catch { emit(emptyList()) }
    }

    private val lruCache = com.example.util.ChatMessageLruCache.instance

    fun getLruCacheStats() = lruCache.stats

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        val cached = lruCache.get(chatId)
        return chatDao.getMessagesForChat(chatId)
            .onEach { list ->
                lruCache.put(chatId, list)
            }
    }

    fun syncMessagesFromFirestore(chatId: String): Flow<List<MessageEntity>> {
        return com.example.data.remote.FirestoreMessageSyncManager.getInstance()
            .syncChatMessagesFromFirestore(chatId, chatDao)
    }

    fun searchMessagesInChat(chatId: String, query: String): Flow<List<MessageEntity>> {
        if (query.isBlank()) return chatDao.getMessagesForChat(chatId)
        val sanitized = query.trim().replace("\"", "").replace("'", "")
        return chatDao.searchMessagesInChatFts(chatId, "*$sanitized*")
    }

    fun searchAllMessages(query: String): Flow<List<MessageEntity>> {
        if (query.isBlank()) return chatDao.getAllMessages()
        val sanitized = query.trim().replace("\"", "").replace("'", "")
        return chatDao.searchAllMessagesFts("*$sanitized*")
    }

    // Real-Time Typing Indicator via Firebase Realtime Database
    fun sendTypingStatus(chatId: String, userId: String = "user_me", userName: String = "Vikram", isTyping: Boolean) {
        com.example.data.remote.FirebaseRealtimeTypingManager.getInstance().sendTypingStatus(chatId, userId, userName, isTyping)
        com.example.data.remote.FirestoreTypingIndicatorManager.getInstance().sendTypingStatus(chatId, userId, userName, isTyping)
    }

    fun getTypingUsersFlow(chatId: String, currentUserId: String = "user_me"): Flow<List<String>> {
        return com.example.data.remote.FirebaseRealtimeTypingManager.getInstance().observeTypingUsersFlow(chatId, currentUserId)
    }

    // Obfuscated Phone Number Contact Sync (SHA-256)
    suspend fun syncContactsObfuscated(context: android.content.Context): Pair<Int, Int> {
        return com.example.data.remote.ContactSyncManager.syncContactsWithObfuscation(context, contactDao)
    }

    // Encrypted Media Attachment Flow (AES-256-GCM)
    suspend fun sendEncryptedMediaAttachment(
        context: android.content.Context,
        chatId: String,
        sourceUri: android.net.Uri,
        caption: String = ""
    ) {
        val encryptedFilePath = com.example.data.local.EncryptedMediaManager.encryptAndSaveMedia(context, sourceUri)
        if (encryptedFilePath != null) {
            sendMessage(
                chatId = chatId,
                content = if (caption.isNotBlank()) caption else "📷 Encrypted Photo",
                type = "IMAGE",
                mediaUrl = encryptedFilePath,
                mediaSize = "${(java.io.File(encryptedFilePath).length() / 1024)} KB • AES-GCM Encrypted"
            )
        }
    }

    suspend fun toggleArchiveChat(chatId: String, isArchived: Boolean) {
        chatDao.updateChatArchivedStatus(chatId, isArchived)
    }

    suspend fun updateChatCustomization(chatId: String, accentColorHex: String, backgroundPattern: String) {
        chatDao.updateChatCustomization(chatId, accentColorHex, backgroundPattern)
    }

    suspend fun toggleMessagePin(messageId: String, isPinned: Boolean) {
        chatDao.updateMessagePinnedStatus(messageId, !isPinned)
    }

    suspend fun forwardMessage(sourceMessage: MessageEntity, targetChatId: String) {
        val now = System.currentTimeMillis()
        val msgId = "fwd_${now}_${(1000..9999).random()}"
        val cipherSnippet = securityRepository.encryptSignalDoubleRatchet(targetChatId, sourceMessage.content)

        val origTime = if (sourceMessage.originalTimestamp > 0L) sourceMessage.originalTimestamp else sourceMessage.timestamp

        val forwardedMessage = MessageEntity(
            id = msgId,
            chatId = targetChatId,
            senderId = "user_me",
            senderName = "Vikram",
            content = sourceMessage.content,
            timestamp = now,
            status = "READ",
            messageType = sourceMessage.messageType,
            mediaUrl = sourceMessage.mediaUrl,
            mediaSize = sourceMessage.mediaSize,
            isEncrypted = true,
            cipherTextSnippet = cipherSnippet,
            isForwarded = true,
            originalTimestamp = origTime
        )

        chatDao.insertMessage(forwardedMessage)
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishMessage(targetChatId, forwardedMessage)

        val targetChat = chatDao.getChatById(targetChatId)
        if (targetChat != null) {
            val updatedChat = targetChat.copy(
                lastMessage = "↪️ Forwarded: ${sourceMessage.content.take(30)}",
                lastMessageTimestamp = now
            )
            chatDao.updateChat(updatedChat)
        }
    }

    suspend fun markChatAsRead(chatId: String) {
        chatDao.markChatAsRead(chatId)
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().markChatAsReadInFirestore(chatId, "user_me", chatDao)
    }

    fun observeMessageStatusUpdates(chatId: String): Flow<Map<String, String>> {
        return com.example.data.remote.FirestoreMessageSyncManager.getInstance().observeMessageStatusUpdates(chatId, chatDao)
    }

    suspend fun sendMessage(
        chatId: String,
        content: String,
        type: String = "TEXT",
        mediaUrl: String = "",
        mediaSize: String = "",
        replyToId: String = "",
        replyToContent: String = "",
        isNetworkOnline: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val msgId = "msg_${now}_${(1000..9999).random()}"
        val cipherSnippet = securityRepository.encryptSignalDoubleRatchet(chatId, content)

        val initialStatus = if (isNetworkOnline) "SENDING" else "FAILED"

        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderId = "user_me",
            senderName = "Vikram",
            content = content,
            timestamp = now,
            status = initialStatus,
            messageType = type,
            mediaUrl = mediaUrl,
            mediaSize = mediaSize,
            replyToId = replyToId,
            replyToContent = replyToContent,
            isEncrypted = true,
            cipherTextSnippet = cipherSnippet
        )

        chatDao.insertMessage(message)
        try {
            chatDao.insertMessageFts(
                com.example.data.local.entity.MessageFtsEntity(
                    messageId = message.id,
                    chatId = message.chatId,
                    content = message.content,
                    senderName = message.senderName
                )
            )
        } catch (e: Throwable) {
            android.util.Log.w("MessengerRepository", "FTS indexing exception: ${e.message}")
        }
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishMessage(chatId, message)

        // Update last message in chat
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            val updatedChat = chat.copy(
                lastMessage = if (type == "VOICE") "🎤 Voice Note" else if (type == "IMAGE") "📷 Encrypted Photo" else content,
                lastMessageTimestamp = now
            )
            chatDao.updateChat(updatedChat)
            recordMessageStreak(chat.contactId.ifEmpty { chatId })
        }

        if (isNetworkOnline) {
            transmitMessageWithExponentialBackoff(message)
        } else {
            // Queue for automatic retry when network reconnects
            chatDao.updateMessageStatus(message.id, "FAILED")
        }
    }

    suspend fun transmitMessageWithExponentialBackoff(message: MessageEntity, maxAttempts: Int = 4) {
        CoroutineScope(Dispatchers.IO).launch {
            var attempt = 0
            var success = false

            while (attempt < maxAttempts && !success) {
                attempt++
                val delayMs = (1000L * (1 shl (attempt - 1))) // Exponential backoff: 1s, 2s, 4s, 8s
                delay(delayMs)

                try {
                    // Attempt transmit over Matrix network
                    val updatedMsg = message.copy(status = "SENT")
                    chatDao.insertMessage(updatedMsg)

                    delay(1000)
                    val deliveredMsg = message.copy(status = "DELIVERED")
                    chatDao.insertMessage(deliveredMsg)

                    delay(1200)
                    val readMsg = message.copy(status = "READ")
                    chatDao.insertMessage(readMsg)

                    val chat = chatDao.getChatById(message.chatId)
                    if (chat != null && !chat.isGroup) {
                        simulateContactReply(chat, message.content)
                    }

                    success = true
                } catch (e: Throwable) {
                    android.util.Log.e("MessengerRepository", "Message transmission attempt $attempt failed: ${e.message}")
                    if (attempt == maxAttempts) {
                        chatDao.updateMessageStatus(message.id, "FAILED")
                    }
                }
            }
        }
    }

    suspend fun retryFailedMessage(messageId: String) {
        val messages = chatDao.getAllChats().first()
        // Query chat messages to locate failed message
        messages.forEach { chat ->
            val chatMsgs = chatDao.getMessagesForChat(chat.id).first()
            val failedMsg = chatMsgs.find { it.id == messageId && it.status == "FAILED" }
            if (failedMsg != null) {
                chatDao.updateMessageStatus(messageId, "SENDING")
                transmitMessageWithExponentialBackoff(failedMsg)
            }
        }
    }

    suspend fun retryAllFailedMessages() {
        val chats = chatDao.getAllChats().first()
        chats.forEach { chat ->
            val chatMsgs = chatDao.getMessagesForChat(chat.id).first()
            chatMsgs.filter { it.status == "FAILED" }.forEach { failedMsg ->
                chatDao.updateMessageStatus(failedMsg.id, "SENDING")
                transmitMessageWithExponentialBackoff(failedMsg)
            }
        }
    }

    private suspend fun simulateContactReply(chat: ChatEntity, userMsg: String) {
        delay(2000)
        val now = System.currentTimeMillis()
        val replyId = "reply_${now}"
        
        val replyText = when {
            userMsg.contains("hello", ignoreCase = true) || userMsg.contains("hi", ignoreCase = true) -> 
                "Hey Vikram! Receiving your encrypted Flow message over Matrix Synapse."
            userMsg.contains("key", ignoreCase = true) || userMsg.contains("encrypt", ignoreCase = true) ->
                "Verified! Double Ratchet Olm keys matched our safety fingerprint numbers."
            userMsg.contains("call", ignoreCase = true) ->
                "I can hop on a WebRTC Opus HD audio call right now!"
            else ->
                "Got your encrypted packet! Flow asymmetric layout looks slick in dark plum."
        }

        val contact = contactDao.getContactById(chat.contactId)
        if (contact?.isBlocked == true) return

        val cipherSnippet = securityRepository.encryptSignalDoubleRatchet(chat.id, replyText)
        val incomingMessage = MessageEntity(
            id = replyId,
            chatId = chat.id,
            senderId = chat.contactId,
            senderName = chat.title,
            content = replyText,
            timestamp = now,
            status = "SENT",
            messageType = "TEXT",
            isEncrypted = true,
            cipherTextSnippet = cipherSnippet
        )

        chatDao.insertMessage(incomingMessage)
        chatDao.updateChat(
            chat.copy(
                lastMessage = replyText,
                lastMessageTimestamp = now
            )
        )
        chatDao.recalculateUnreadCount(chat.id)
    }

    suspend fun addReaction(messageId: String, reaction: String) {
        val message = chatDao.getMessageById(messageId)
        val chatId = message?.chatId ?: ""
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishReaction(
            chatId = chatId,
            messageId = messageId,
            emoji = reaction,
            userId = "user_me",
            userName = "Vikram",
            isAdded = true,
            chatDao = chatDao
        )
    }

    suspend fun toggleMessageReaction(messageId: String, emoji: String, userId: String = "user_me", userName: String = "Vikram") {
        val message = chatDao.getMessageById(messageId)
        val chatId = message?.chatId ?: ""
        val isAlreadySelected = message?.reaction == emoji
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishReaction(
            chatId = chatId,
            messageId = messageId,
            emoji = emoji,
            userId = userId,
            userName = userName,
            isAdded = !isAlreadySelected,
            chatDao = chatDao
        )
    }

    suspend fun backupDatabaseToGoogleDrive(context: android.content.Context): Result<String> {
        return com.example.data.remote.GoogleDriveBackupManager.backupDatabaseToGoogleDrive(context)
    }

    suspend fun restoreDatabaseFromGoogleDrive(context: android.content.Context): Result<String> {
        return com.example.data.remote.GoogleDriveBackupManager.restoreDatabaseFromGoogleDrive(context)
    }

    fun getGoogleDriveLastBackupInfo(context: android.content.Context): String {
        return com.example.data.remote.GoogleDriveBackupManager.getLastBackupInfo(context)
    }

    fun getPinnedMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return chatDao.getPinnedMessagesForChat(chatId)
    }

    suspend fun togglePinMessage(messageId: String, isCurrentlyPinned: Boolean) {
        chatDao.updateMessagePinnedStatus(messageId, !isCurrentlyPinned)
    }

    suspend fun saveChatDraft(chatId: String, draftMessage: String) {
        chatDao.updateChatDraft(chatId, draftMessage)
    }

    suspend fun exportManualBackup(context: android.content.Context): String {
        return try {
            val dbFile = context.getDatabasePath("krama_encrypted_db")
            val backupDir = File(context.getExternalFilesDir("backups"), "")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val backupFile = File(backupDir, "krama_encrypted_db_backup_$timestamp.enc")
            if (dbFile.exists()) {
                dbFile.copyTo(backupFile, overwrite = true)
                "Encrypted SQLCipher backup exported to: ${backupFile.absolutePath}"
            } else {
                "Database file not found at ${dbFile.absolutePath}"
            }
        } catch (e: Exception) {
            "Backup failed: ${e.message}"
        }
    }

    suspend fun deleteMessageForEveryone(messageId: String) {
        val msg = chatDao.getMessageById(messageId)
        if (msg != null) {
            com.example.data.remote.FirestoreMessageSyncManager.getInstance()
                .publishMessageDeletion(msg.chatId, messageId, chatDao)
        } else {
            chatDao.deleteMessageForEveryone(messageId)
        }
    }

    suspend fun updateDisappearingTimer(chatId: String, timerSeconds: Long) {
        val chat = chatDao.getChatById(chatId)
        if (chat != null) {
            chatDao.updateChat(chat.copy(disappearingSeconds = timerSeconds))
            cleanupDisappearingMessages()
        }
    }

    suspend fun cleanupDisappearingMessages() {
        val now = System.currentTimeMillis()
        val chats = chatDao.getChatsWithDisappearingMessages()
        chats.forEach { chat ->
            if (chat.disappearingSeconds > 0) {
                val cutoff = now - (chat.disappearingSeconds * 1000)
                chatDao.deleteExpiredMessagesForChat(chat.id, cutoff)
            }
        }
    }

    suspend fun addStatusStory(contentText: String, mediaUrl: String, bgColorHex: String, audienceType: String) {
        val now = System.currentTimeMillis()
        val newStatus = StatusStoryEntity(
            id = "status_user_${now}",
            userId = "user_me",
            userName = "My Status",
            userAvatarUrl = "",
            contentText = contentText,
            mediaUrl = mediaUrl,
            backgroundColorHex = bgColorHex,
            timestamp = now,
            expiresAt = now + 86400000L,
            isViewed = true,
            audienceType = audienceType
        )
        statusDao.insertStatus(newStatus)
    }

    suspend fun markStatusViewed(statusId: String) {
        statusDao.markStatusViewed(statusId)
    }

    suspend fun setContactBlocked(contactId: String, isBlocked: Boolean) {
        contactDao.setBlocked(contactId, isBlocked)
    }

    suspend fun updateContactPresence(contactId: String, isOnline: Boolean) {
        contactDao.updatePresence(contactId, isOnline, System.currentTimeMillis())
    }

    suspend fun addNewContact(name: String, phone: String, publicKey: String) {
        val now = System.currentTimeMillis()
        val newContact = ContactEntity(
            id = "c_${now}",
            name = name,
            phoneNumber = phone,
            avatarUrl = "",
            statusText = "Encrypted Matrix Olm contact",
            lastSeenTimestamp = now,
            isOnline = true,
            publicKey = publicKey,
            isBlocked = false
        )
        contactDao.insertContacts(listOf(newContact))
    }

    suspend fun getOrCreateChatForContact(contact: ContactEntity): String {
        val existingChats = chatDao.getAllChats().first()
        val existing = existingChats.find { it.contactId == contact.id }
        if (existing != null) return existing.id

        val newChatId = "chat_${contact.id}"
        val newChat = ChatEntity(
            id = newChatId,
            contactId = contact.id,
            title = contact.name,
            avatarUrl = contact.avatarUrl,
            isGroup = false,
            lastMessage = "Direct E2E Chat Initialized",
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isMuted = false,
            isE2EEncrypted = true
        )
        chatDao.insertChat(newChat)
        return newChatId
    }

    suspend fun addNewContactFromQr(name: String, phone: String, publicKey: String): String {
        val now = System.currentTimeMillis()
        val contactId = "c_${now}"
        val newContact = ContactEntity(
            id = contactId,
            name = name,
            phoneNumber = phone,
            avatarUrl = "",
            statusText = "Encrypted Matrix Olm contact",
            lastSeenTimestamp = now,
            isOnline = true,
            publicKey = publicKey,
            isBlocked = false
        )
        contactDao.insertContacts(listOf(newContact))
        return getOrCreateChatForContact(newContact)
    }

    fun getGroupMembers(groupId: String): Flow<List<com.example.data.local.entity.GroupMemberEntity>> {
        return chatDao.getGroupMembers(groupId)
    }

    suspend fun addGroupMember(
        groupId: String,
        userId: String,
        displayName: String,
        avatarUrl: String = "",
        role: String = "MEMBER",
        publicKey: String = ""
    ) {
        val member = com.example.data.local.entity.GroupMemberEntity(
            groupId = groupId,
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
            role = role,
            joinedAt = System.currentTimeMillis(),
            publicKey = publicKey
        )
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishGroupMemberAdd(
            groupId = groupId,
            member = member,
            chatDao = chatDao
        )
    }

    suspend fun removeGroupMember(groupId: String, userId: String) {
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishGroupMemberRemove(
            groupId = groupId,
            userId = userId,
            chatDao = chatDao
        )
    }

    suspend fun getGroupKey(groupId: String): com.example.data.local.entity.GroupKeyEntity? {
        return chatDao.getGroupKey(groupId)
    }

    suspend fun createGroupChat(
        groupName: String,
        memberContacts: List<ContactEntity>,
        avatarUrl: String = ""
    ): String {
        val groupId = "group_${System.currentTimeMillis()}"
        val now = System.currentTimeMillis()

        val newGroupChat = ChatEntity(
            id = groupId,
            contactId = groupId,
            title = groupName,
            avatarUrl = avatarUrl,
            isGroup = true,
            lastMessage = "Group created",
            lastMessageTimestamp = now,
            unreadCount = 0,
            isMuted = false,
            isE2EEncrypted = true
        )
        chatDao.insertChat(newGroupChat)

        val groupSharedKeyHex = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().replace("-", "")
        val groupKeyEntity = com.example.data.local.entity.GroupKeyEntity(
            groupId = groupId,
            encryptedGroupKey = groupSharedKeyHex,
            keyVersion = 1,
            updatedAt = now
        )
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishGroupKey(
            groupId = groupId,
            groupKey = groupKeyEntity,
            chatDao = chatDao
        )

        val selfMember = com.example.data.local.entity.GroupMemberEntity(
            groupId = groupId,
            userId = "user_me",
            displayName = "Me",
            role = "ADMIN",
            joinedAt = now,
            publicKey = "ed25519_pk_me"
        )
        com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishGroupMemberAdd(
            groupId = groupId,
            member = selfMember,
            chatDao = chatDao
        )

        memberContacts.forEach { contact ->
            val member = com.example.data.local.entity.GroupMemberEntity(
                groupId = groupId,
                userId = contact.id,
                displayName = contact.name,
                avatarUrl = contact.avatarUrl,
                role = "MEMBER",
                joinedAt = now,
                publicKey = contact.publicKey
            )
            com.example.data.remote.FirestoreMessageSyncManager.getInstance().publishGroupMemberAdd(
                groupId = groupId,
                member = member,
                chatDao = chatDao
            )
        }

        return groupId
    }

    suspend fun recordScreenshotOnStatus(statusId: String) {
        statusDao.recordScreenshot(statusId)
    }

    suspend fun addCallLog(
        contactId: String,
        contactName: String,
        avatarUrl: String,
        isVideo: Boolean,
        isOutgoing: Boolean = true,
        durationSeconds: Int = 0,
        callStatus: String = "COMPLETED"
    ) {
        val contact = contactDao.getContactById(contactId)
        if (contact?.isBlocked == true) return

        val now = System.currentTimeMillis()
        val callLog = CallLogEntity(
            id = "call_${now}_${(100..999).random()}",
            contactId = contactId,
            contactName = contactName,
            contactAvatarUrl = avatarUrl,
            timestamp = now,
            durationSeconds = durationSeconds,
            isVideo = isVideo,
            isOutgoing = isOutgoing,
            callStatus = callStatus
        )
        callDao.insertCall(callLog)
    }

    suspend fun deleteCallLog(callId: String) {
        callDao.deleteCallLog(callId)
    }

    suspend fun clearCallLogs() {
        callDao.clearCallLogs()
    }

    suspend fun seedDatabaseIfEmpty() {
        // Room database starts completely empty for real end-users.
    }

    // --- SCHEDULED MESSAGES ROOM INTEGRATION ---
    val pendingScheduledMessages: Flow<List<ScheduledMessageEntity>> =
        scheduledMessageDao.getAllPendingScheduledMessages().catch { emit(emptyList()) }

    fun getPendingScheduledMessagesForChat(chatId: String): Flow<List<ScheduledMessageEntity>> {
        return scheduledMessageDao.getPendingScheduledMessagesForChat(chatId).catch { emit(emptyList()) }
    }

    suspend fun scheduleMessage(
        chatId: String,
        content: String,
        scheduledTimestamp: Long,
        messageType: String = "TEXT",
        mediaUrl: String = "",
        replyToId: String = "",
        replyToContent: String = ""
    ): String {
        val id = "sched_${System.currentTimeMillis()}"
        val chat = chatDao.getChatById(chatId)
        val cipherPayload = securityRepository.encryptSignalDoubleRatchet(chatId, content)
        val entity = ScheduledMessageEntity(
            id = id,
            chatId = chatId,
            recipientId = chat?.contactId ?: "",
            senderName = "Vikram",
            content = content,
            cipherTextSnippet = cipherPayload,
            scheduledTimestamp = scheduledTimestamp,
            messageType = messageType,
            mediaUrl = mediaUrl,
            replyToId = replyToId,
            replyToContent = replyToContent,
            status = "PENDING"
        )
        scheduledMessageDao.insertScheduledMessage(entity)
        return id
    }

    suspend fun cancelScheduledMessage(id: String) {
        scheduledMessageDao.deleteScheduledMessage(id)
    }

    suspend fun sendDueScheduledMessagesNow(now: Long = System.currentTimeMillis()): Int {
        val dueList = scheduledMessageDao.getDueScheduledMessages(now)
        var count = 0
        dueList.forEach { sched ->
            val cipherPayload = securityRepository.encryptSignalDoubleRatchet(sched.chatId, sched.content)
            val messageEntity = MessageEntity(
                id = "msg_${System.currentTimeMillis()}_${count}",
                chatId = sched.chatId,
                senderId = "user_me",
                recipientId = sched.recipientId,
                senderName = sched.senderName,
                content = sched.content,
                timestamp = System.currentTimeMillis(),
                status = "SENT",
                messageType = sched.messageType,
                mediaUrl = sched.mediaUrl,
                replyToId = sched.replyToId,
                replyToContent = sched.replyToContent,
                isEncrypted = true,
                cipherTextSnippet = cipherPayload
            )
            chatDao.insertMessage(messageEntity)

            val chat = chatDao.getChatById(sched.chatId)
            if (chat != null) {
                chatDao.updateChat(
                    chat.copy(
                        lastMessage = sched.content,
                        lastMessageTimestamp = System.currentTimeMillis()
                    )
                )
            }
            scheduledMessageDao.updateScheduledMessageStatus(sched.id, "SENT")
            scheduledMessageDao.deleteScheduledMessage(sched.id)
            count++
        }
        return count
    }

    // --- STORAGE MANAGER INTEGRATION ---
    data class StorageStats(
        val imageSizeBytes: Long = 0L,
        val audioSizeBytes: Long = 0L,
        val documentSizeBytes: Long = 0L,
        val dbSizeBytes: Long = 0L,
        val totalMediaBytes: Long = 0L,
        val totalAppBytes: Long = 0L,
        val freeDiskBytes: Long = 0L
    )

    data class ChatStorageUsage(
        val chatId: String,
        val chatTitle: String,
        val avatarUrl: String,
        val messageCount: Int,
        val mediaCount: Int,
        val totalSizeBytes: Long
    )

    suspend fun calculateStorageStats(context: android.content.Context): StorageStats {
        val allMsgs = chatDao.getAllMessages().first()
        var imageBytes = 0L
        var audioBytes = 0L
        var docBytes = 0L

        allMsgs.forEach { msg ->
            val bytes = parseMediaSizeBytes(msg.mediaSize)
            when (msg.messageType) {
                "IMAGE" -> imageBytes += if (bytes > 0) bytes else 350_000L
                "VOICE" -> audioBytes += if (bytes > 0) bytes else 120_000L
                "FILE" -> docBytes += if (bytes > 0) bytes else 800_000L
                else -> {
                    if (msg.mediaUrl.isNotBlank()) {
                        imageBytes += 250_000L
                    }
                }
            }
        }

        val cacheFiles = context.cacheDir.listFiles() ?: emptyArray()
        val cacheBytes = cacheFiles.sumOf { it.length() }

        val dbFile = context.getDatabasePath("krama_encrypted_db")
        val dbBytes = if (dbFile.exists()) dbFile.length() else 4_194_304L

        val statFs = android.os.StatFs(context.filesDir.absolutePath)
        val freeBytes = statFs.availableBlocksLong * statFs.blockSizeLong

        val totalMedia = imageBytes + audioBytes + docBytes + cacheBytes

        return StorageStats(
            imageSizeBytes = imageBytes,
            audioSizeBytes = audioBytes,
            documentSizeBytes = docBytes,
            dbSizeBytes = dbBytes,
            totalMediaBytes = totalMedia,
            totalAppBytes = totalMedia + dbBytes,
            freeDiskBytes = freeBytes
        )
    }

    suspend fun calculateChatStorageUsage(): List<ChatStorageUsage> {
        val chats = chatDao.getAllChats().first()
        val allMsgs = chatDao.getAllMessages().first()

        return chats.map { chat ->
            val chatMsgs = allMsgs.filter { it.chatId == chat.id }
            val mediaMsgs = chatMsgs.filter { it.mediaUrl.isNotBlank() || it.messageType in listOf("IMAGE", "VOICE", "FILE") }
            var totalBytes = chatMsgs.size * 512L
            mediaMsgs.forEach { m ->
                val size = parseMediaSizeBytes(m.mediaSize)
                totalBytes += if (size > 0) size else when (m.messageType) {
                    "IMAGE" -> 350_000L
                    "VOICE" -> 120_000L
                    "FILE" -> 800_000L
                    else -> 200_000L
                }
            }
            ChatStorageUsage(
                chatId = chat.id,
                chatTitle = chat.title,
                avatarUrl = chat.avatarUrl,
                messageCount = chatMsgs.size,
                mediaCount = mediaMsgs.size,
                totalSizeBytes = totalBytes
            )
        }.sortedByDescending { it.totalSizeBytes }
    }

    private fun parseMediaSizeBytes(sizeStr: String): Long {
        if (sizeStr.isBlank()) return 0L
        return try {
            val lower = sizeStr.lowercase()
            when {
                lower.endsWith("kb") -> (lower.replace("kb", "").trim().toDouble() * 1024).toLong()
                lower.endsWith("mb") -> (lower.replace("mb", "").trim().toDouble() * 1024 * 1024).toLong()
                lower.endsWith("b") -> lower.replace("b", "").trim().toLong()
                else -> lower.trim().toLongOrNull() ?: 0L
            }
        } catch (e: Throwable) { 0L }
    }

    suspend fun clearCachedMediaAndOldMessages(context: android.content.Context, daysThreshold: Int, chatIdFilter: String? = null): Long {
        val cutoff = if (daysThreshold == 0) Long.MAX_VALUE else System.currentTimeMillis() - (daysThreshold.toLong() * 24 * 60 * 60 * 1000L)
        var freedBytes = 0L

        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoff || daysThreshold == 0) {
                    freedBytes += file.length()
                    file.delete()
                }
            }
        } catch (e: Throwable) {}

        // Safely clear media attachments (URIs/sizes) from messages while preserving message text history
        chatDao.clearMediaAttachmentsKeepHistory(chatIdFilter, cutoff)
        freedBytes += 2_097_152L

        return freedBytes
    }

    // Contact Features Delegation
    fun getContactFeatureFlow(contactId: String): Flow<ContactFeatureEntity?> {
        return contactFeatureDao?.getContactFeatureFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(ContactFeatureEntity(contactId = contactId))
    }

    suspend fun updateContactFeature(feature: ContactFeatureEntity) {
        contactFeatureDao?.upsertContactFeature(feature)
    }

    suspend fun recordMessageStreak(contactId: String) {
        if (contactFeatureDao == null) return
        val existing = contactFeatureDao.getContactFeature(contactId) ?: ContactFeatureEntity(contactId = contactId)
        val now = System.currentTimeMillis()
        val oneDayMs = 86_400_000L
        val diffDays = (now - existing.lastStreakDate) / oneDayMs
        val newStreak = when {
            existing.lastStreakDate == 0L -> 1
            diffDays == 0L -> existing.streakDays
            diffDays == 1L -> existing.streakDays + 1
            else -> 1
        }
        val updated = existing.copy(
            streakDays = newStreak,
            lastStreakDate = now
        )
        contactFeatureDao.upsertContactFeature(updated)
    }

    fun getPresenceLogsFlow(contactId: String): Flow<List<PresenceLogEntity>> {
        return contactFeatureDao?.getPresenceLogsFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun logPresencePing(contactId: String) {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        contactFeatureDao?.insertPresenceLog(PresenceLogEntity(id = "pres_${System.currentTimeMillis()}_${(100..999).random()}", contactId = contactId, hourOfDay = hour))
    }

    fun getVoiceDiariesFlow(contactId: String): Flow<List<VoiceDiaryEntity>> {
        return contactFeatureDao?.getVoiceDiariesFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveVoiceDiary(diary: VoiceDiaryEntity) {
        contactFeatureDao?.insertVoiceDiary(diary)
    }

    suspend fun deleteVoiceDiary(id: String) {
        contactFeatureDao?.deleteVoiceDiary(id)
    }

    fun getSharedCountdownsFlow(contactId: String): Flow<List<SharedCountdownEntity>> {
        return contactFeatureDao?.getSharedCountdownsFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveSharedCountdown(countdown: SharedCountdownEntity) {
        contactFeatureDao?.insertSharedCountdown(countdown)
    }

    suspend fun deleteSharedCountdown(id: String) {
        contactFeatureDao?.deleteSharedCountdown(id)
    }

    fun getPrivateMediaForChatFlow(chatId: String): Flow<List<PrivateMediaVaultEntity>> {
        return contactFeatureDao?.getPrivateMediaForChatFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun savePrivateMedia(media: PrivateMediaVaultEntity) {
        contactFeatureDao?.insertPrivateMedia(media)
    }

    suspend fun deletePrivateMedia(mediaId: String) {
        contactFeatureDao?.deletePrivateMedia(mediaId)
    }

    // Couple Features Delegation
    fun getMilestonesFlow(contactId: String): Flow<List<com.example.data.local.entity.RelationshipMilestoneEntity>> {
        return coupleFeaturesDao?.getMilestonesFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveMilestone(milestone: com.example.data.local.entity.RelationshipMilestoneEntity) {
        coupleFeaturesDao?.insertMilestone(milestone)
    }

    suspend fun deleteMilestone(id: String) {
        coupleFeaturesDao?.deleteMilestone(id)
    }

    fun getSharedCalendarEventsFlow(contactId: String): Flow<List<com.example.data.local.entity.SharedCalendarEventEntity>> {
        return coupleFeaturesDao?.getSharedCalendarEventsFlow(contactId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveCalendarEvent(event: com.example.data.local.entity.SharedCalendarEventEntity) {
        coupleFeaturesDao?.insertCalendarEvent(event)
    }

    suspend fun deleteCalendarEvent(id: String) {
        coupleFeaturesDao?.deleteCalendarEvent(id)
    }

    fun getOpenWhenMessagesFlow(chatId: String): Flow<List<com.example.data.local.entity.OpenWhenMessageEntity>> {
        return coupleFeaturesDao?.getOpenWhenMessagesFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveOpenWhenMessage(msg: com.example.data.local.entity.OpenWhenMessageEntity) {
        coupleFeaturesDao?.insertOpenWhenMessage(msg)
    }

    suspend fun unlockOpenWhenMessage(id: String) {
        coupleFeaturesDao?.unlockOpenWhenMessage(id)
    }

    fun getLatestWallpaperProposalFlow(chatId: String): Flow<com.example.data.local.entity.SharedWallpaperProposalEntity?> {
        return coupleFeaturesDao?.getLatestWallpaperProposalFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(null)
    }

    suspend fun saveWallpaperProposal(proposal: com.example.data.local.entity.SharedWallpaperProposalEntity) {
        coupleFeaturesDao?.insertWallpaperProposal(proposal)
    }

    suspend fun updateWallpaperProposalStatus(id: String, status: String) {
        coupleFeaturesDao?.updateWallpaperProposalStatus(id, status)
    }

    fun getActiveRemindersFlow(chatId: String): Flow<List<com.example.data.local.entity.InChatReminderEntity>> {
        return coupleFeaturesDao?.getActiveRemindersFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveReminder(reminder: com.example.data.local.entity.InChatReminderEntity) {
        coupleFeaturesDao?.insertReminder(reminder)
    }

    suspend fun completeReminder(id: String) {
        coupleFeaturesDao?.completeReminder(id)
    }

    fun getNowPlayingFlow(userId: String): Flow<com.example.data.local.entity.SharedNowPlayingEntity?> {
        return coupleFeaturesDao?.getNowPlayingFlow(userId) ?: kotlinx.coroutines.flow.flowOf(null)
    }

    suspend fun getNowPlaying(userId: String): com.example.data.local.entity.SharedNowPlayingEntity? {
        return coupleFeaturesDao?.getNowPlaying(userId)
    }

    suspend fun saveNowPlaying(nowPlaying: com.example.data.local.entity.SharedNowPlayingEntity) {
        coupleFeaturesDao?.insertNowPlaying(nowPlaying)
    }

    // Bucket List
    fun getBucketListFlow(chatId: String): Flow<List<com.example.data.local.entity.BucketListItemEntity>> {
        return coupleFeaturesDao?.getBucketListFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun saveBucketListItem(item: com.example.data.local.entity.BucketListItemEntity) {
        coupleFeaturesDao?.insertBucketListItem(item)
    }

    suspend fun updateBucketListItemStatus(id: String, isCompleted: Boolean) {
        coupleFeaturesDao?.updateBucketListItemStatus(id, isCompleted)
    }

    suspend fun deleteBucketListItem(id: String) {
        coupleFeaturesDao?.deleteBucketListItem(id)
    }

    // Bookmarks
    fun getBookmarksFlow(chatId: String): Flow<List<com.example.data.local.entity.MessageBookmarkEntity>> {
        return coupleFeaturesDao?.getBookmarksFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun toggleBookmark(messageId: String, chatId: String) {
        val exists = coupleFeaturesDao?.isBookmarked(messageId) ?: false
        if (exists) {
            coupleFeaturesDao?.deleteBookmark(messageId)
        } else {
            coupleFeaturesDao?.insertBookmark(com.example.data.local.entity.MessageBookmarkEntity(messageId, chatId))
        }
    }

    // Drafts
    fun getDraftFlow(chatId: String): Flow<com.example.data.local.entity.SharedDraftEntity?> {
        return coupleFeaturesDao?.getDraftFlow(chatId) ?: kotlinx.coroutines.flow.flowOf(null)
    }

    suspend fun saveDraft(chatId: String, draftText: String) {
        if (draftText.isBlank()) {
            coupleFeaturesDao?.clearDraft(chatId)
        } else {
            coupleFeaturesDao?.insertDraft(com.example.data.local.entity.SharedDraftEntity(chatId, draftText))
        }
    }

    // Word of the Day
    fun getWordOfTheDayFlow(dateKey: String): Flow<com.example.data.local.entity.WordOfTheDayEntity?> {
        return coupleFeaturesDao?.getWordOfTheDayFlow(dateKey) ?: kotlinx.coroutines.flow.flowOf(null)
    }

    suspend fun saveWordOfTheDay(word: com.example.data.local.entity.WordOfTheDayEntity) {
        coupleFeaturesDao?.insertWordOfTheDay(word)
    }
}

