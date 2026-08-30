package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ContactFeatureEntity
import com.example.data.local.entity.PresenceLogEntity
import com.example.data.local.entity.PrivateMediaVaultEntity
import com.example.data.local.entity.SharedCountdownEntity
import com.example.data.local.entity.VoiceDiaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactFeatureDao {

    @Query("SELECT * FROM contact_features WHERE contactId = :contactId")
    fun getContactFeatureFlow(contactId: String): Flow<ContactFeatureEntity?>

    @Query("SELECT * FROM contact_features WHERE contactId = :contactId")
    suspend fun getContactFeature(contactId: String): ContactFeatureEntity?

    @Query("SELECT * FROM contact_features")
    fun getAllContactFeaturesFlow(): Flow<List<ContactFeatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContactFeature(feature: ContactFeatureEntity)

    // Presence Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresenceLog(log: PresenceLogEntity)

    @Query("SELECT * FROM presence_logs WHERE contactId = :contactId")
    fun getPresenceLogsFlow(contactId: String): Flow<List<PresenceLogEntity>>

    // Voice Diaries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceDiary(diary: VoiceDiaryEntity)

    @Query("SELECT * FROM voice_diaries WHERE contactId = :contactId ORDER BY timestamp DESC")
    fun getVoiceDiariesFlow(contactId: String): Flow<List<VoiceDiaryEntity>>

    @Query("DELETE FROM voice_diaries WHERE id = :id")
    suspend fun deleteVoiceDiary(id: String)

    // Shared Countdowns
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSharedCountdown(countdown: SharedCountdownEntity)

    @Query("SELECT * FROM shared_countdowns WHERE contactId = :contactId")
    fun getSharedCountdownsFlow(contactId: String): Flow<List<SharedCountdownEntity>>

    @Query("DELETE FROM shared_countdowns WHERE id = :id")
    suspend fun deleteSharedCountdown(id: String)

    // Private Media Vault
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivateMedia(media: PrivateMediaVaultEntity)

    @Query("SELECT * FROM private_media_vault WHERE chatId = :chatId ORDER BY dateAdded DESC")
    fun getPrivateMediaForChatFlow(chatId: String): Flow<List<PrivateMediaVaultEntity>>

    @Query("DELETE FROM private_media_vault WHERE mediaId = :mediaId")
    suspend fun deletePrivateMedia(mediaId: String)
}
