package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presence_logs")
data class PresenceLogEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val hourOfDay: Int, // 0..23
    val timestamp: Long = System.currentTimeMillis()
)
