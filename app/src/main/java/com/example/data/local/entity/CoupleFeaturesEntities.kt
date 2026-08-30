package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relationship_milestones")
data class RelationshipMilestoneEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val title: String,
    val description: String = "",
    val timestamp: Long,
    val iconEmoji: String = "💖",
    val category: String = "MILESTONE" // e.g., First Message, First Call, Anniversary
)

@Entity(tableName = "shared_calendar_events")
data class SharedCalendarEventEntity(
    @PrimaryKey val id: String,
    val contactId: String,
    val title: String,
    val dateTimestamp: Long,
    val locationOrNote: String = "",
    val emoji: String = "📅",
    val createdBy: String = "user_me"
)

@Entity(tableName = "open_when_messages")
data class OpenWhenMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val title: String, // e.g. "Open when you miss me" or "Open on your Birthday"
    val content: String,
    val unlockTimestamp: Long,
    val isUnlocked: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shared_wallpaper_proposals")
data class SharedWallpaperProposalEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val wallpaperUrl: String,
    val proposedBy: String,
    val proposedByName: String,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
)

@Entity(tableName = "in_chat_reminders")
data class InChatReminderEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val messageId: String,
    val reminderTimestamp: Long,
    val note: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "shared_now_playing")
data class SharedNowPlayingEntity(
    @PrimaryKey val userId: String,
    val songTitle: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val mood: String = "😊 Happy",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "bucket_list_items")
data class BucketListItemEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val title: String,
    val category: String = "✈️ Travel",
    val isCompleted: Boolean = false,
    val createdBy: String = "user_me",
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "message_bookmarks")
data class MessageBookmarkEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shared_drafts")
data class SharedDraftEntity(
    @PrimaryKey val chatId: String,
    val draftText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "word_of_the_day")
data class WordOfTheDayEntity(
    @PrimaryKey val dateKey: String,
    val word: String,
    val phonetic: String,
    val meaning: String,
    val example: String
)

