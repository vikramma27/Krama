package com.example.util

import android.util.Log
import android.util.LruCache
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CacheStats(
    val hitCount: Int = 0,
    val missCount: Int = 0,
    val putCount: Int = 0,
    val evictionCount: Int = 0,
    val currentSize: Int = 0,
    val maxSize: Int = 50
)

class ChatMessageLruCache(maxEntries: Int = 50) {

    private val cache = object : LruCache<String, List<MessageEntity>>(maxEntries) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: List<MessageEntity>?,
            newValue: List<MessageEntity>?
        ) {
            super.entryRemoved(evicted, key, oldValue, newValue)
            if (evicted) {
                Log.d("ChatMessageLruCache", "LRU cache evicted chat: $key (Message count: ${oldValue?.size ?: 0})")
            }
            updateStats()
        }
    }

    private val _stats = MutableStateFlow(CacheStats(maxSize = maxEntries))
    val stats: StateFlow<CacheStats> = _stats.asStateFlow()

    @Synchronized
    fun get(chatId: String): List<MessageEntity>? {
        val result = cache.get(chatId)
        updateStats()
        if (result != null) {
            Log.d("ChatMessageLruCache", "CACHE HIT for chatId: $chatId (${result.size} messages)")
        } else {
            Log.d("ChatMessageLruCache", "CACHE MISS for chatId: $chatId")
        }
        return result
    }

    @Synchronized
    fun put(chatId: String, messages: List<MessageEntity>) {
        cache.put(chatId, messages)
        updateStats()
        Log.d("ChatMessageLruCache", "CACHE PUT for chatId: $chatId (${messages.size} messages cached)")
    }

    @Synchronized
    fun invalidate(chatId: String) {
        cache.remove(chatId)
        updateStats()
        Log.d("ChatMessageLruCache", "CACHE INVALIDATED for chatId: $chatId")
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
        updateStats()
        Log.d("ChatMessageLruCache", "CACHE CLEARED completely")
    }

    private fun updateStats() {
        _stats.value = CacheStats(
            hitCount = cache.hitCount(),
            missCount = cache.missCount(),
            putCount = cache.putCount(),
            evictionCount = cache.evictionCount(),
            currentSize = cache.size(),
            maxSize = cache.maxSize()
        )
    }

    companion object {
        val instance by lazy { ChatMessageLruCache() }
    }
}
