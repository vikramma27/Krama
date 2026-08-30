package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contact_features")
data class ContactFeatureEntity(
    @PrimaryKey val contactId: String,
    val streakDays: Int = 0,
    val lastStreakDate: Long = 0L,
    val nickname: String = "",
    val privateNotes: String = "",
    val customRingtoneUri: String = "",
    val customVibrationPattern: String = "DEFAULT", // DEFAULT, HEARTBEAT, DOUBLE_BUZZ, INTENSE
    val statusEmoji: String = "💬",
    val autoReplyDrivingEnabled: Boolean = false,
    val autoReplyMessage: String = "🚗 I'm driving right now. Will reply shortly!",
    val chatCreatedTimestamp: Long = System.currentTimeMillis(),
    val lastActiveTogetherTimestamp: Long = 0L
)
