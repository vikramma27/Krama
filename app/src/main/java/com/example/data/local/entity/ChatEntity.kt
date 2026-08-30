package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["lastMessageTimestamp"])
    ]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val title: String,
    val avatarUrl: String,
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isE2EEncrypted: Boolean = true,
    val disappearingSeconds: Long = 0L,
    val draftMessage: String = "",
    val isArchived: Boolean = false,
    val accentColorHex: String = "#26A69A",
    val backgroundPattern: String = "DOTS"
)
