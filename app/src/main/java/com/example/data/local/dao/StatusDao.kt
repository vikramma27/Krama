package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StatusStoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM status_stories ORDER BY timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusStoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusStoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusStoryEntity>)

    @Query("UPDATE status_stories SET isViewed = 1 WHERE id = :statusId")
    suspend fun markStatusViewed(statusId: String)

    @Query("UPDATE status_stories SET screenshotTaken = 1 WHERE id = :statusId")
    suspend fun recordScreenshot(statusId: String)

    @Query("DELETE FROM status_stories WHERE expiresAt <= :currentTime")
    suspend fun purgeExpiredStatuses(currentTime: Long)
}
