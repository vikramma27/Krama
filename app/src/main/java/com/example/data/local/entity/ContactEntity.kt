package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["phoneNumber"]),
        Index(value = ["isOnline"])
    ]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val avatarUrl: String,
    val statusText: String,
    val lastSeenTimestamp: Long,
    val isOnline: Boolean,
    val publicKey: String,
    val isBlocked: Boolean = false
)
