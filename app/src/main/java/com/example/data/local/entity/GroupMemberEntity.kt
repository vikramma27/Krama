package com.example.data.local.entity

import androidx.room.Entity

@Entity(tableName = "group_members", primaryKeys = ["groupId", "userId"])
data class GroupMemberEntity(
    val groupId: String,
    val userId: String,
    val displayName: String,
    val avatarUrl: String = "",
    val role: String = "MEMBER", // "ADMIN", "MEMBER"
    val joinedAt: Long = System.currentTimeMillis(),
    val publicKey: String = ""
)
