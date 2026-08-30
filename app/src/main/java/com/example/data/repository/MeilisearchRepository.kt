package com.example.data.repository

import com.example.data.local.dao.ChatDao
import com.example.data.local.entity.MessageEntity
import com.example.data.remote.MeiliMessageDocument
import com.example.data.remote.MeilisearchClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Meilisearch Repository that integrates Room FTS local offline search
 * with Meilisearch remote fast full-text search engine.
 */
class MeilisearchRepository(
    private val chatDao: ChatDao,
    private val meilisearchClient: MeilisearchClient = MeilisearchClient.instance
) {

    suspend fun indexMessage(message: MessageEntity) {
        meilisearchClient.indexMessage(message)
    }

    suspend fun syncAllMessagesToMeiliIndex() = withContext(Dispatchers.IO) {
        val messages = chatDao.getRawMessageList()
        if (messages.isNotEmpty()) {
            meilisearchClient.indexMessagesBatch(messages)
        }
    }

    suspend fun searchMessages(
        query: String,
        chatId: String? = null,
        limit: Int = 30
    ): List<MessageEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // 1. Primary: Meilisearch remote search
        val remoteHits = meilisearchClient.searchMessages(query, chatId, limit)
        if (remoteHits.isNotEmpty()) {
            val messageIds = remoteHits.map { it.id }.toSet()
            val allMessages = chatDao.getRawMessageList()
            val matched = allMessages.filter { it.id in messageIds }
            if (matched.isNotEmpty()) return@withContext matched
        }

        // 2. Offline Fallback: Room SQLite FTS search
        if (!chatId.isNullOrEmpty()) {
            chatDao.searchMessagesInChat(chatId, query).first()
        } else {
            chatDao.searchAllMessages(query).first()
        }
    }
}
