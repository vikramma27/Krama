package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCalls(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE callStatus = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun getCallsForContact(contactId: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(callLog: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalls(callLogs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :callId")
    suspend fun deleteCallLog(callId: String)

    @Query("DELETE FROM call_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteExpiredCallLogs(cutoffTimestamp: Long): Int

    @Query("DELETE FROM call_logs")
    suspend fun clearCallLogs()
}
