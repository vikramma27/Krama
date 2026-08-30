package com.example.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPresence(
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

object FirebasePresenceManager {

    private const val TAG = "FirebasePresenceManager"
    private var isInitialized = false

    private val _presenceMap = MutableStateFlow<Map<String, UserPresence>>(emptyMap())
    val presenceMap: StateFlow<Map<String, UserPresence>> = _presenceMap.asStateFlow()

    /**
     * Initializes presence tracking for the current authenticated user and attaches
     * non-blocking Firebase Realtime Database disconnect handlers.
     */
    fun initializeUserPresence(context: Context? = null) {
        if (isInitialized) return

        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }

            val app = try {
                FirebaseApp.getInstance()
            } catch (e: Throwable) {
                if (context != null) {
                    try { FirebaseApp.initializeApp(context.applicationContext) } catch (ie: Throwable) { null }
                } else null
            }

            if (app == null) {
                Log.w(TAG, "FirebaseApp is not initialized yet. Skipping presence setup.")
                return
            }

            val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val userId = currentUser?.uid ?: "local_krama_user"

            val database = try { FirebaseDatabase.getInstance() } catch (e: Throwable) { null }
            if (database == null) {
                Log.w(TAG, "FirebaseDatabase instance could not be retrieved.")
                return
            }

            val connectedRef = database.getReference(".info/connected")
            val myPresenceRef = database.getReference("status/$userId")

            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        // When client disconnects, automatically update Realtime DB
                        val offlineStatus = mapOf(
                            "isOnline" to false,
                            "lastSeenTimestamp" to ServerValue.TIMESTAMP
                        )
                        myPresenceRef.onDisconnect().setValue(offlineStatus)

                        // Currently online
                        val onlineStatus = mapOf(
                            "isOnline" to true,
                            "lastSeenTimestamp" to ServerValue.TIMESTAMP
                        )
                        myPresenceRef.setValue(onlineStatus)
                        Log.i(TAG, "Realtime presence set to ONLINE for user: $userId")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Presence listener cancelled: ${error.message}")
                }
            })

            // Listen to global contact presence nodes in non-blocking background
            val allStatusRef = database.getReference("status")
            allStatusRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newMap = mutableMapOf<String, UserPresence>()
                    for (child in snapshot.children) {
                        val uid = child.key ?: continue
                        val isOnline = child.child("isOnline").getValue(Boolean::class.java) ?: false
                        val lastSeen = child.child("lastSeenTimestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                        newMap[uid] = UserPresence(isOnline, lastSeen)
                    }
                    _presenceMap.value = newMap
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Global presence listener cancelled: ${error.message}")
                }
            })

            isInitialized = true
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Realtime Database presence setup safely handled: ${e.message}")
        }
    }

    /**
     * Manually updates presence to offline when app goes to background.
     */
    fun setOffline(context: Context? = null) {
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }
            val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val userId = currentUser?.uid ?: "local_krama_user"
            val database = try { FirebaseDatabase.getInstance() } catch (e: Throwable) { null } ?: return
            val myPresenceRef = database.getReference("status/$userId")
            myPresenceRef.setValue(mapOf("isOnline" to false, "lastSeenTimestamp" to System.currentTimeMillis()))
        } catch (e: Throwable) {
            Log.w(TAG, "Failed setting offline presence: ${e.message}")
        }
    }
}

