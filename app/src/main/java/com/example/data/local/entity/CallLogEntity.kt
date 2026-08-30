package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_logs",
    indices = [
        Index(value = ["contactId"]),
        Index(value = ["timestamp"])
    ]
)
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val contactName: String,
    val contactAvatarUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isVideo: Boolean = false,
    val isOutgoing: Boolean = true,
    val callStatus: String = "COMPLETED" // COMPLETED, MISSED, REJECTED
)
