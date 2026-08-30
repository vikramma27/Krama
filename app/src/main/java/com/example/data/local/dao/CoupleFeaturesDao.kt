package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoupleFeaturesDao {

    // Milestones
    @Query("SELECT * FROM relationship_milestones WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMilestonesFlow(contactId: String): Flow<List<RelationshipMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: RelationshipMilestoneEntity)

    @Query("DELETE FROM relationship_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: String)

    // Calendar
    @Query("SELECT * FROM shared_calendar_events WHERE contactId = :contactId ORDER BY dateTimestamp ASC")
    fun getSharedCalendarEventsFlow(contactId: String): Flow<List<SharedCalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: SharedCalendarEventEntity)

    @Query("DELETE FROM shared_calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: String)

    // Open When Messages
    @Query("SELECT * FROM open_when_messages WHERE chatId = :chatId ORDER BY unlockTimestamp ASC")
    fun getOpenWhenMessagesFlow(chatId: String): Flow<List<OpenWhenMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenWhenMessage(msg: OpenWhenMessageEntity)

    @Query("UPDATE open_when_messages SET isUnlocked = 1 WHERE id = :id")
    suspend fun unlockOpenWhenMessage(id: String)

    // Wallpaper proposals
    @Query("SELECT * FROM shared_wallpaper_proposals WHERE chatId = :chatId ORDER BY id DESC LIMIT 1")
    fun getLatestWallpaperProposalFlow(chatId: String): Flow<SharedWallpaperProposalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallpaperProposal(proposal: SharedWallpaperProposalEntity)

    @Query("UPDATE shared_wallpaper_proposals SET status = :status WHERE id = :id")
    suspend fun updateWallpaperProposalStatus(id: String, status: String)

    // In-chat reminders
    @Query("SELECT * FROM in_chat_reminders WHERE chatId = :chatId AND isCompleted = 0 ORDER BY reminderTimestamp ASC")
    fun getActiveRemindersFlow(chatId: String): Flow<List<InChatReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: InChatReminderEntity)

    @Query("UPDATE in_chat_reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun completeReminder(id: String)

    // Now Playing
    @Query("SELECT * FROM shared_now_playing WHERE userId = :userId")
    fun getNowPlayingFlow(userId: String): Flow<SharedNowPlayingEntity?>

    @Query("SELECT * FROM shared_now_playing WHERE userId = :userId")
    suspend fun getNowPlaying(userId: String): SharedNowPlayingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNowPlaying(nowPlaying: SharedNowPlayingEntity)

    // Bucket List
    @Query("SELECT * FROM bucket_list_items WHERE chatId = :chatId ORDER BY createdTimestamp DESC")
    fun getBucketListFlow(chatId: String): Flow<List<BucketListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBucketListItem(item: BucketListItemEntity)

    @Query("UPDATE bucket_list_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateBucketListItemStatus(id: String, isCompleted: Boolean)

    @Query("DELETE FROM bucket_list_items WHERE id = :id")
    suspend fun deleteBucketListItem(id: String)

    // Bookmarks
    @Query("SELECT * FROM message_bookmarks WHERE chatId = :chatId")
    fun getBookmarksFlow(chatId: String): Flow<List<MessageBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: MessageBookmarkEntity)

    @Query("DELETE FROM message_bookmarks WHERE messageId = :messageId")
    suspend fun deleteBookmark(messageId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM message_bookmarks WHERE messageId = :messageId)")
    suspend fun isBookmarked(messageId: String): Boolean

    // Drafts
    @Query("SELECT * FROM shared_drafts WHERE chatId = :chatId")
    fun getDraftFlow(chatId: String): Flow<SharedDraftEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: SharedDraftEntity)

    @Query("DELETE FROM shared_drafts WHERE chatId = :chatId")
    suspend fun clearDraft(chatId: String)

    // Word of the Day
    @Query("SELECT * FROM word_of_the_day WHERE dateKey = :dateKey")
    fun getWordOfTheDayFlow(dateKey: String): Flow<WordOfTheDayEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordOfTheDay(word: WordOfTheDayEntity)
}

