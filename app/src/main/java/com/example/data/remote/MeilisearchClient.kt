package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.MessageEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class MeiliMessageDocument(
    val id: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val timestamp: Long,
    val type: String,
    val senderName: String? = null
)

data class MeiliSearchResult(
    val hits: List<MeiliMessageDocument> = emptyList(),
    val query: String = "",
    val processingTimeMs: Int = 0,
    val limit: Int = 20,
    val estimatedTotalHits: Int = 0
)

/**
 * Native OkHttp client for Meilisearch instance.
 * Enables ultra-fast full-text search, typo-tolerance, and indexing.
 */
class MeilisearchClient private constructor() {

    private var hostUrl: String = "https://search.kramamessenger.internal"
    private var apiKey: String = ""

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun configure(url: String, key: String) {
        if (url.isNotBlank()) this.hostUrl = url.trimEnd('/')
        if (key.isNotBlank()) this.apiKey = key
    }

    suspend fun indexMessage(message: MessageEntity, senderName: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = MeiliMessageDocument(
                id = message.id,
                chatId = message.chatId,
                senderId = message.senderId,
                text = message.content,
                timestamp = message.timestamp,
                type = message.messageType,
                senderName = senderName
            )
            val jsonAdapter = moshi.adapter(Array<MeiliMessageDocument>::class.java)
            val jsonBody = jsonAdapter.toJson(arrayOf(doc))

            val request = Request.Builder()
                .url("$hostUrl/indexes/messages/documents")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .apply {
                    if (apiKey.isNotEmpty()) header("Authorization", "Bearer $apiKey")
                }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                Log.d(TAG, "Indexed message ${message.id} to Meilisearch: code=${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Meilisearch indexing notice: ${e.message}")
            false
        }
    }

    suspend fun indexMessagesBatch(messages: List<MessageEntity>): Boolean = withContext(Dispatchers.IO) {
        try {
            val docs = messages.map {
                MeiliMessageDocument(
                    id = it.id,
                    chatId = it.chatId,
                    senderId = it.senderId,
                    text = it.content,
                    timestamp = it.timestamp,
                    type = it.messageType
                )
            }
            val jsonAdapter = moshi.adapter(List::class.java)
            val jsonBody = moshi.adapter(Array<MeiliMessageDocument>::class.java).toJson(docs.toTypedArray())

            val request = Request.Builder()
                .url("$hostUrl/indexes/messages/documents")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .apply {
                    if (apiKey.isNotEmpty()) header("Authorization", "Bearer $apiKey")
                }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "Meilisearch batch index notice: ${e.message}")
            false
        }
    }

    suspend fun searchMessages(query: String, chatId: String? = null, limit: Int = 20): List<MeiliMessageDocument> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val payload = mutableMapOf<String, Any>(
                "q" to query,
                "limit" to limit
            )
            if (!chatId.isNullOrEmpty()) {
                payload["filter"] = "chatId = '$chatId'"
            }

            val jsonBody = moshi.adapter(Map::class.java).toJson(payload)
            val request = Request.Builder()
                .url("$hostUrl/indexes/messages/search")
                .post(jsonBody.toRequestBody(jsonMediaType))
                .apply {
                    if (apiKey.isNotEmpty()) header("Authorization", "Bearer $apiKey")
                }
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val responseBody = response.body?.string() ?: return@withContext emptyList()
                
                val resultAdapter = moshi.adapter(MeiliSearchResult::class.java)
                val searchResult = resultAdapter.fromJson(responseBody)
                searchResult?.hits ?: emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Meilisearch query note: ${e.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "MeilisearchClient"
        val instance: MeilisearchClient by lazy { MeilisearchClient() }
    }
}
