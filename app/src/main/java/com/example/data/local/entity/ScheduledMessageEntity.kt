package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val recipientId: String = "",
    val senderName: String = "Vikram",
    val content: String,
    val cipherTextSnippet: String = "Encrypted Payload",
    val scheduledTimestamp: Long,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, SENT, CANCELLED
    val messageType: String = "TEXT",
    val mediaUrl: String = "",
    val replyToId: String = "",
    val replyToContent: String = "",
    val isEncrypted: Boolean = true
)
