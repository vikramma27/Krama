package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "group_keys")
data class GroupKeyEntity(
    @PrimaryKey val groupId: String,
    val encryptedGroupKey: String,
    val keyVersion: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
)
