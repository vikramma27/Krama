package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "status_stories")
data class StatusStoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String,
    val contentText: String,
    val mediaUrl: String,
    val backgroundColorHex: String = "#3B2E7E",
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24 hours
    val isViewed: Boolean = false,
    val audienceType: String = "ALL_CONTACTS",
    val screenshotTaken: Boolean = false
)
