package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shared_countdowns")
data class SharedCountdownEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val title: String,
    val targetTimestamp: Long,
    val emoji: String = "✈️"
)
