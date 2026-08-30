package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["senderId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val recipientId: String = "",
    val senderName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "READ", // SENDING, SENT, DELIVERED, READ
    val messageType: String = "TEXT", // TEXT, IMAGE, VOICE, FILE, LOCATION, CONTACT, CALL_LOG
    val mediaUrl: String = "",
    val mediaSize: String = "",
    val reaction: String = "",
    val replyToId: String = "",
    val replyToContent: String = "",
    val isEncrypted: Boolean = true,
    val cipherTextSnippet: String = "",
    val iv: String = "",
    val senderKeyId: String = "",
    val ratchetStep: Int = 0,
    val algorithmVersion: String = "AES-256-GCM+DoubleRatchet",
    val isMetadataEncrypted: Boolean = true,
    val isDeletedForEveryone: Boolean = false,
    val isPinned: Boolean = false,
    val isForwarded: Boolean = false,
    val originalTimestamp: Long = 0L
)
