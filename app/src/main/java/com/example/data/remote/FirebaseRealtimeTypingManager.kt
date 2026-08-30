package com.example.data.remote

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages real-time 'is typing' status broadcasting and listening using Firebase Realtime Database.
 */
class FirebaseRealtimeTypingManager private constructor() {

    private val realtimeDb: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    companion object {
        private const val TAG = "RealtimeTypingManager"

        @Volatile
        private var INSTANCE: FirebaseRealtimeTypingManager? = null

        fun getInstance(): FirebaseRealtimeTypingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseRealtimeTypingManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Broadcasts current typing status for a chat thread via Firebase Realtime Database.
     */
    fun sendTypingStatus(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val typingRef = realtimeDb.getReference("typing_status")
                .child(chatId)
                .child(userId)

            if (isTyping) {
                val payload = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "isTyping" to true,
                    "timestamp" to System.currentTimeMillis()
                )
                typingRef.setValue(payload)
                typingRef.onDisconnect().removeValue()
            } else {
                typingRef.removeValue()
            }
            Log.d(TAG, "Broadcasting typing status via Realtime Database for $userId in $chatId: $isTyping")
        } catch (e: Throwable) {
            Log.e(TAG, "Error updating typing status in Realtime Database: ${e.message}")
        }
    }

    /**
     * Listens in real-time to active typing users in a chat thread via Firebase Realtime Database.
     * Emits list of user names currently typing (excluding currentUserId).
     */
    fun observeTypingUsersFlow(chatId: String, currentUserId: String): Flow<List<String>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val typingRef = realtimeDb.getReference("typing_status").child(chatId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val typingUsers = mutableListOf<String>()

                for (child in snapshot.children) {
                    val uid = child.child("userId").getValue(String::class.java) ?: child.key ?: ""
                    val name = child.child("userName").getValue(String::class.java) ?: "Someone"
                    val isTyping = child.child("isTyping").getValue(Boolean::class.java) ?: false
                    val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

                    // Active typing within last 10 seconds
                    if (uid != currentUserId && isTyping && (now - timestamp) < 10000) {
                        typingUsers.add(name)
                    }
                }
                trySend(typingUsers)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Realtime Database typing listener cancelled: ${error.message}")
                trySend(emptyList())
            }
        }

        typingRef.addValueEventListener(listener)

        awaitClose {
            typingRef.removeEventListener(listener)
        }
    }
}
