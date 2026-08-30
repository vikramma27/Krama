package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageReactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY lastMessageTimestamp DESC")
    suspend fun getRawChatList(): List<ChatEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getRawMessageList(): List<MessageEntity>

    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastMessageTimestamp DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR lastMessage LIKE '%' || :query || '%') ORDER BY lastMessageTimestamp DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateChatArchivedStatus(chatId: String, isArchived: Boolean)

    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId AND senderId != 'user_me' AND status != 'READ'")
    suspend fun getUnreadCountForChat(chatId: String): Int

    @Query("UPDATE chats SET unreadCount = (SELECT COUNT(*) FROM messages WHERE messages.chatId = chats.id AND messages.senderId != 'user_me' AND messages.status != 'READ') WHERE id = :chatId")
    suspend fun recalculateUnreadCount(chatId: String)

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE disappearingSeconds > 0")
    suspend fun getChatsWithDisappearingMessages(): List<ChatEntity>

    @Query("UPDATE chats SET draftMessage = :draftMessage WHERE id = :chatId")
    suspend fun updateChatDraft(chatId: String, draftMessage: String)

    @Query("UPDATE chats SET accentColorHex = :accentColor, backgroundPattern = :pattern WHERE id = :chatId")
    suspend fun updateChatCustomization(chatId: String, accentColor: String, pattern: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForChat(chatId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND isPinned = 1 ORDER BY timestamp DESC")
    fun getPinnedMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND content LIKE '%' || :query || '%' ORDER BY timestamp ASC")
    fun searchMessagesInChat(chatId: String, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAllMessages(query: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageFts(ftsEntity: com.example.data.local.entity.MessageFtsEntity)

    @Query("SELECT messages.* FROM messages JOIN messages_fts ON messages.id = messages_fts.messageId WHERE messages_fts MATCH :query ORDER BY messages.timestamp DESC")
    fun searchAllMessagesFts(query: String): Flow<List<MessageEntity>>

    @Query("SELECT messages.* FROM messages JOIN messages_fts ON messages.id = messages_fts.messageId WHERE messages.chatId = :chatId AND messages_fts MATCH :query ORDER BY messages.timestamp ASC")
    fun searchMessagesInChatFts(chatId: String, query: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = 'READ' WHERE chatId = :chatId AND senderId != :currentUserId AND status != 'READ'")
    suspend fun markMessagesAsReadForChat(chatId: String, currentUserId: String = "user_me")

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateReaction(messageId: String, reaction: String)

    @Query("UPDATE messages SET isPinned = :isPinned WHERE id = :messageId")
    suspend fun updateMessagePinnedStatus(messageId: String, isPinned: Boolean)

    @Query("UPDATE messages SET content = 'This message was deleted', isDeletedForEveryone = 1 WHERE id = :messageId")
    suspend fun deleteMessageForEveryone(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND timestamp < :cutoffTimestamp")
    suspend fun deleteExpiredMessagesForChat(chatId: String, cutoffTimestamp: Long)

    @Query("DELETE FROM messages WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteAllMessagesOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM messages WHERE mediaUrl != '' AND timestamp < :cutoffTimestamp")
    suspend fun deleteMediaMessagesOlderThan(cutoffTimestamp: Long)

    @Query("UPDATE messages SET mediaUrl = '', mediaSize = '0 B' WHERE (:chatId IS NULL OR chatId = :chatId) AND mediaUrl != '' AND timestamp < :cutoffTimestamp")
    suspend fun clearMediaAttachmentsKeepHistory(chatId: String?, cutoffTimestamp: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    // Message Reactions Table Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageReaction(reaction: MessageReactionEntity)

    @Query("DELETE FROM message_reactions WHERE messageId = :messageId AND emoji = :emoji AND userId = :userId")
    suspend fun deleteMessageReaction(messageId: String, emoji: String, userId: String)

    @Query("SELECT * FROM message_reactions WHERE messageId = :messageId")
    fun getReactionsForMessage(messageId: String): Flow<List<MessageReactionEntity>>

    @Query("SELECT * FROM messages")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM message_reactions")
    fun getAllReactions(): Flow<List<MessageReactionEntity>>

    // Group Members and Shared Keys Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: com.example.data.local.entity.GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(members: List<com.example.data.local.entity.GroupMemberEntity>)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun deleteGroupMember(groupId: String, userId: String)

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    fun getGroupMembers(groupId: String): Flow<List<com.example.data.local.entity.GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY joinedAt ASC")
    suspend fun getGroupMembersSync(groupId: String): List<com.example.data.local.entity.GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupKey(groupKey: com.example.data.local.entity.GroupKeyEntity)

    @Query("SELECT * FROM group_keys WHERE groupId = :groupId")
    suspend fun getGroupKey(groupId: String): com.example.data.local.entity.GroupKeyEntity?
}
