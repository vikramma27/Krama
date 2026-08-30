package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ScheduledMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledMessage(message: ScheduledMessageEntity)

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' ORDER BY scheduledTimestamp ASC")
    fun getAllPendingScheduledMessages(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE chatId = :chatId AND status = 'PENDING' ORDER BY scheduledTimestamp ASC")
    fun getPendingScheduledMessagesForChat(chatId: String): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' AND scheduledTimestamp <= :now")
    suspend fun getDueScheduledMessages(now: Long = System.currentTimeMillis()): List<ScheduledMessageEntity>

    @Query("UPDATE scheduled_messages SET status = :status WHERE id = :id")
    suspend fun updateScheduledMessageStatus(id: String, status: String)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteScheduledMessage(id: String)
}
