package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Local Full-Text Search (FTS4) Virtual Table for Room / SQLCipher encrypted message searching.
 * All index data is stored within the SQLCipher-encrypted SQLite database file.
 */
@Fts4
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowid: Int = 0,
    val messageId: String,
    val chatId: String,
    val content: String,
    val senderName: String
)
