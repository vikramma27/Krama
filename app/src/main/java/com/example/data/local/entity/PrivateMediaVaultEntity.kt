package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "private_media_vault")
data class PrivateMediaVaultEntity(
    @PrimaryKey val mediaId: String,
    val chatId: String,
    val mediaUrlOrPath: String,
    val mediaType: String, // IMAGE, VIDEO, VOICE
    val isLocked: Boolean = true,
    val dateAdded: Long = System.currentTimeMillis()
)
