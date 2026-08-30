package com.example.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreTypingIndicatorManager private constructor() {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    companion object {
        private const val TAG = "FirestoreTypingIndicator"
        @Volatile
        private var INSTANCE: FirestoreTypingIndicatorManager? = null

        fun getInstance(): FirestoreTypingIndicatorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirestoreTypingIndicatorManager().also { INSTANCE = it }
            }
        }
    }

    /**
     * Publishes current typing state for a chat thread to Firestore.
     */
    fun sendTypingStatus(chatId: String, userId: String, userName: String, isTyping: Boolean) {
        if (chatId.isBlank() || userId.isBlank()) return
        try {
            val typingRef = firestore.collection("chats")
                .document(chatId)
                .collection("typing")
                .document(userId)

            val payload = mapOf(
                "userId" to userId,
                "userName" to userName,
                "isTyping" to isTyping,
                "timestamp" to System.currentTimeMillis()
            )

            typingRef.set(payload)
                .addOnSuccessListener {
                    Log.d(TAG, "Updated typing status for user $userId in chat $chatId: $isTyping")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update typing status: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Typing status error: ${e.message}")
        }
    }

    /**
     * Listens in real-time to active typing users in a chat thread.
     * Emits list of user names currently typing (excluding currentUserId).
     */
    fun getTypingUsersFlow(chatId: String, currentUserId: String): Flow<List<String>> = callbackFlow {
        if (chatId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("typing")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Typing snapshot error for chat $chatId: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val now = System.currentTimeMillis()
                    val typingUsers = mutableListOf<String>()

                    for (doc in snapshot.documents) {
                        val uid = doc.getString("userId") ?: ""
                        val name = doc.getString("userName") ?: "Someone"
                        val isTyping = doc.getBoolean("isTyping") ?: false
                        val timestamp = doc.getLong("timestamp") ?: 0L

                        // Active typing within last 8 seconds
                        if (uid != currentUserId && isTyping && (now - timestamp) < 8000) {
                            typingUsers.add(name)
                        }
                    }
                    trySend(typingUsers)
                }
            }

        awaitClose {
            listener.remove()
        }
    }
}
