package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_diaries")
data class VoiceDiaryEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val audioPath: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "Voice Entry"
)
