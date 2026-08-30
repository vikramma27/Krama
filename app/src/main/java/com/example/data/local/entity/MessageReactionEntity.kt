package com.example.data.local.entity

import androidx.room.Entity

@Entity(tableName = "message_reactions", primaryKeys = ["messageId", "emoji", "userId"])
data class MessageReactionEntity(
    val messageId: String,
    val emoji: String,
    val userId: String = "user_me",
    val userName: String = "Vikram",
    val timestamp: Long = System.currentTimeMillis()
)
