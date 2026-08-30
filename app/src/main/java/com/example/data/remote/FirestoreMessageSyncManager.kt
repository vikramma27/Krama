package com.example.data.remote

import android.util.Log
import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.MessageEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class FirestoreMessageSyncManager private constructor() {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val TAG = "FirestoreMessageSync"

        @Volatile
        private var INSTANCE: FirestoreMessageSyncManager? = null

        fun getInstance(): FirestoreMessageSyncManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreMessageSyncManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Publishes message to Firestore real-time room to notify recipient device, preserving all E2EE headers.
     */
    fun publishMessage(chatId: String, message: MessageEntity) {
        if (chatId.isBlank()) return
        try {
            val docRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(message.id)

            val data = mapOf(
                "id" to message.id,
                "chatId" to message.chatId,
                "senderId" to message.senderId,
                "recipientId" to message.recipientId,
                "senderName" to message.senderName,
                "content" to message.content,
                "timestamp" to message.timestamp,
                "status" to message.status,
                "messageType" to message.messageType,
                "mediaUrl" to message.mediaUrl,
                "mediaSize" to message.mediaSize,
                "reaction" to message.reaction,
                "replyToId" to message.replyToId,
                "replyToContent" to message.replyToContent,
                "isEncrypted" to message.isEncrypted,
                "cipherTextSnippet" to message.cipherTextSnippet,
                "iv" to message.iv,
                "senderKeyId" to message.senderKeyId,
                "ratchetStep" to message.ratchetStep,
                "algorithmVersion" to message.algorithmVersion,
                "isMetadataEncrypted" to message.isMetadataEncrypted,
                "isDeletedForEveryone" to message.isDeletedForEveryone,
                "isPinned" to message.isPinned,
                "isForwarded" to message.isForwarded,
                "originalTimestamp" to message.originalTimestamp
            )

            docRef.set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Published E2EE message ${message.id} to Firestore with algorithm ${message.algorithmVersion}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to publish message ${message.id}: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Publish message exception: ${e.message}")
        }
    }

    /**
     * Listens for incoming and multi-device synced chat messages from Firestore, deserializes all E2EE headers, and saves to Room.
     */
    fun syncChatMessagesFromFirestore(chatId: String, chatDao: ChatDao): Flow<List<MessageEntity>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Sync messages error for chat $chatId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messagesList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val sChatId = doc.getString("chatId") ?: chatId
                            val senderId = doc.getString("senderId") ?: ""
                            val recipientId = doc.getString("recipientId") ?: ""
                            val senderName = doc.getString("senderName") ?: "Contact"
                            val content = doc.getString("content") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val status = doc.getString("status") ?: "SENT"
                            val messageType = doc.getString("messageType") ?: "TEXT"
                            val mediaUrl = doc.getString("mediaUrl") ?: ""
                            val mediaSize = doc.getString("mediaSize") ?: ""
                            val reaction = doc.getString("reaction") ?: ""
                            val replyToId = doc.getString("replyToId") ?: ""
                            val replyToContent = doc.getString("replyToContent") ?: ""
                            val isEncrypted = doc.getBoolean("isEncrypted") ?: true
                            val cipherTextSnippet = doc.getString("cipherTextSnippet") ?: ""
                            val iv = doc.getString("iv") ?: ""
                            val senderKeyId = doc.getString("senderKeyId") ?: ""
                            val ratchetStep = (doc.getLong("ratchetStep") ?: 0L).toInt()
                            val algorithmVersion = doc.getString("algorithmVersion") ?: "AES-256-GCM+DoubleRatchet"
                            val isMetadataEncrypted = doc.getBoolean("isMetadataEncrypted") ?: true
                            val isDeletedForEveryone = doc.getBoolean("isDeletedForEveryone") ?: false
                            val isPinned = doc.getBoolean("isPinned") ?: false
                            val isForwarded = doc.getBoolean("isForwarded") ?: false
                            val originalTimestamp = doc.getLong("originalTimestamp") ?: 0L

                            MessageEntity(
                                id = id,
                                chatId = sChatId,
                                senderId = senderId,
                                recipientId = recipientId,
                                senderName = senderName,
                                content = content,
                                timestamp = timestamp,
                                status = status,
                                messageType = messageType,
                                mediaUrl = mediaUrl,
                                mediaSize = mediaSize,
                                reaction = reaction,
                                replyToId = replyToId,
                                replyToContent = replyToContent,
                                isEncrypted = isEncrypted,
                                cipherTextSnippet = cipherTextSnippet,
                                iv = iv,
                                senderKeyId = senderKeyId,
                                ratchetStep = ratchetStep,
                                algorithmVersion = algorithmVersion,
                                isMetadataEncrypted = isMetadataEncrypted,
                                isDeletedForEveryone = isDeletedForEveryone,
                                isPinned = isPinned,
                                isForwarded = isForwarded,
                                originalTimestamp = originalTimestamp
                            )
                        } catch (e: Throwable) {
                            Log.e(TAG, "Error deserializing message ${doc.id}: ${e.message}")
                            null
                        }
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        if (messagesList.isNotEmpty()) {
                            chatDao.insertMessages(messagesList)
                        }
                    }
                    trySend(messagesList)
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Publishes a message deletion event to Firestore and updates local Room DB.
     */
    fun publishMessageDeletion(chatId: String, messageId: String, chatDao: ChatDao) {
        if (chatId.isBlank() || messageId.isBlank()) return
        try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "content" to "This message was deleted",
                        "isDeletedForEveryone" to true
                    )
                )
                .addOnSuccessListener {
                    Log.d(TAG, "Published message deletion for $messageId to Firestore")
                }

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.deleteMessageForEveryone(messageId)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish message deletion: ${e.message}")
        }
    }

    /**
     * Updates message status (e.g. SENT -> DELIVERED -> READ) in Firestore and local Room database.
     */
    fun updateMessageStatus(chatId: String, messageId: String, status: String, chatDao: ChatDao) {
        if (chatId.isBlank() || messageId.isBlank()) return
        try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("status", status)
                .addOnSuccessListener {
                    Log.d(TAG, "Updated message $messageId status to $status in Firestore")
                }

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.updateMessageStatus(messageId, status)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to update status: ${e.message}")
        }
    }

    /**
     * Marks all messages in a chat thread as READ when user views the chat thread screen.
     */
    fun markChatAsReadInFirestore(chatId: String, currentUserId: String, chatDao: ChatDao) {
        if (chatId.isBlank()) return
        try {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUserId)
                .whereNotEqualTo("status", "READ")
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val batch = firestore.batch()
                        for (doc in snapshot.documents) {
                            batch.update(doc.reference, "status", "READ")
                        }
                        batch.commit()
                    }
                }

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.markMessagesAsReadForChat(chatId, currentUserId)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Mark chat read exception: ${e.message}")
        }
    }

    /**
     * Real-time listener for message delivery and read status updates for sent messages in a chat.
     */
    fun observeMessageStatusUpdates(chatId: String, chatDao: ChatDao): Flow<Map<String, String>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val statusMap = mutableMapOf<String, String>()
                    for (doc in snapshot.documents) {
                        val msgId = doc.getString("id") ?: doc.id
                        val status = doc.getString("status") ?: "SENT"
                        statusMap[msgId] = status

                        CoroutineScope(Dispatchers.IO).launch {
                            chatDao.updateMessageStatus(msgId, status)
                            val reaction = doc.getString("reaction")
                            if (!reaction.isNullOrEmpty()) {
                                chatDao.updateReaction(msgId, reaction)
                            }
                        }
                    }
                    trySend(statusMap)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Publishes emoji reaction to Firestore real-time room for a message.
     */
    fun publishReaction(
        chatId: String,
        messageId: String,
        emoji: String,
        userId: String,
        userName: String,
        isAdded: Boolean,
        chatDao: ChatDao
    ) {
        if (chatId.isBlank() || messageId.isBlank()) return
        try {
            val reactionRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .collection("reactions")
                .document(userId)

            val msgRef = firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)

            if (isAdded) {
                val data = mapOf(
                    "emoji" to emoji,
                    "userId" to userId,
                    "userName" to userName,
                    "timestamp" to System.currentTimeMillis()
                )
                reactionRef.set(data)
                msgRef.update("reaction", emoji)
            } else {
                reactionRef.delete()
                msgRef.update("reaction", "")
            }

            CoroutineScope(Dispatchers.IO).launch {
                val reactionEntity = com.example.data.local.entity.MessageReactionEntity(
                    messageId = messageId,
                    emoji = emoji,
                    userId = userId,
                    userName = userName
                )
                if (isAdded) {
                    chatDao.insertMessageReaction(reactionEntity)
                    chatDao.updateReaction(messageId, emoji)
                } else {
                    chatDao.deleteMessageReaction(messageId, emoji, userId)
                    chatDao.updateReaction(messageId, "")
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish reaction: ${e.message}")
        }
    }

    /**
     * Publishes a new group member addition to Firestore and local Room DB.
     */
    fun publishGroupMemberAdd(
        groupId: String,
        member: com.example.data.local.entity.GroupMemberEntity,
        chatDao: ChatDao
    ) {
        if (groupId.isBlank() || member.userId.isBlank()) return
        try {
            val memberRef = firestore.collection("chats")
                .document(groupId)
                .collection("members")
                .document(member.userId)

            val memberData = mapOf(
                "groupId" to groupId,
                "userId" to member.userId,
                "displayName" to member.displayName,
                "avatarUrl" to member.avatarUrl,
                "role" to member.role,
                "joinedAt" to member.joinedAt,
                "publicKey" to member.publicKey
            )
            memberRef.set(memberData)

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.insertGroupMember(member)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish group member addition: ${e.message}")
        }
    }

    /**
     * Publishes a group member removal to Firestore and local Room DB.
     */
    fun publishGroupMemberRemove(
        groupId: String,
        userId: String,
        chatDao: ChatDao
    ) {
        if (groupId.isBlank() || userId.isBlank()) return
        try {
            firestore.collection("chats")
                .document(groupId)
                .collection("members")
                .document(userId)
                .delete()

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.deleteGroupMember(groupId, userId)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish group member removal: ${e.message}")
        }
    }

    /**
     * Publishes group shared E2E key to Firestore and local Room DB.
     */
    fun publishGroupKey(
        groupId: String,
        groupKey: com.example.data.local.entity.GroupKeyEntity,
        chatDao: ChatDao
    ) {
        if (groupId.isBlank()) return
        try {
            val keyRef = firestore.collection("chats")
                .document(groupId)
                .collection("keys")
                .document("sharedKey")

            val keyData = mapOf(
                "groupId" to groupId,
                "encryptedGroupKey" to groupKey.encryptedGroupKey,
                "keyVersion" to groupKey.keyVersion,
                "updatedAt" to groupKey.updatedAt
            )
            keyRef.set(keyData)

            CoroutineScope(Dispatchers.IO).launch {
                chatDao.insertGroupKey(groupKey)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to publish group key: ${e.message}")
        }
    }

    /**
     * Listens to real-time group member updates from Firestore and syncs to local Room DB.
     */
    fun observeGroupMembers(
        groupId: String,
        chatDao: ChatDao
    ): Flow<List<com.example.data.local.entity.GroupMemberEntity>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(groupId)
            .collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val members = snapshot.documents.mapNotNull { doc ->
                        val uId = doc.getString("userId") ?: doc.id
                        val dName = doc.getString("displayName") ?: "Member"
                        val avatar = doc.getString("avatarUrl") ?: ""
                        val role = doc.getString("role") ?: "MEMBER"
                        val joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                        val pubKey = doc.getString("publicKey") ?: ""

                        com.example.data.local.entity.GroupMemberEntity(
                            groupId = groupId,
                            userId = uId,
                            displayName = dName,
                            avatarUrl = avatar,
                            role = role,
                            joinedAt = joinedAt,
                            publicKey = pubKey
                        )
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        chatDao.insertGroupMembers(members)
                    }
                    trySend(members)
                }
            }

        awaitClose { listener.remove() }
    }
}

