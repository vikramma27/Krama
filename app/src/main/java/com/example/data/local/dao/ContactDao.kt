package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: String): ContactEntity?

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getRawContactList(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Query("UPDATE contacts SET isBlocked = :isBlocked WHERE id = :id")
    suspend fun setBlocked(id: String, isBlocked: Boolean)

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeenTimestamp = :lastSeenTimestamp WHERE id = :id")
    suspend fun updatePresence(id: String, isOnline: Boolean, lastSeenTimestamp: Long = System.currentTimeMillis())
}
