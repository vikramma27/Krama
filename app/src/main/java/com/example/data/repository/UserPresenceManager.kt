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

data class PresenceState(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class UserPresenceManager(private val context: Context) {

    private val TAG = "UserPresenceManager"

    private val _myPresence = MutableStateFlow(PresenceState())
    val myPresence: StateFlow<PresenceState> = _myPresence.asStateFlow()

    private val _partnerPresence = MutableStateFlow(PresenceState())
    val partnerPresence: StateFlow<PresenceState> = _partnerPresence.asStateFlow()

    private val _isSameWavelength = MutableStateFlow(false)
    val isSameWavelength: StateFlow<Boolean> = _isSameWavelength.asStateFlow()

    init {
        initializePresenceTracking()
    }

    fun initializePresenceTracking(partnerUserId: String = "partner_krama_user") {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }

            val app = try { FirebaseApp.getInstance() } catch (e: Throwable) { null }
            if (app == null) {
                Log.w(TAG, "FirebaseApp not available for UserPresenceManager")
                return
            }

            val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val userId = currentUser?.uid ?: "local_krama_user"

            val database = try { FirebaseDatabase.getInstance() } catch (e: Throwable) { null } ?: return

            val connectedRef = database.getReference(".info/connected")
            val myPresenceRef = database.getReference("status/$userId")
            val partnerRef = database.getReference("status/$partnerUserId")

            connectedRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: false
                    if (connected) {
                        val offlineMap = mapOf(
                            "isOnline" to false,
                            "lastSeenTimestamp" to ServerValue.TIMESTAMP
                        )
                        myPresenceRef.onDisconnect().setValue(offlineMap)

                        val onlineMap = mapOf(
                            "isOnline" to true,
                            "lastSeenTimestamp" to ServerValue.TIMESTAMP
                        )
                        myPresenceRef.setValue(onlineMap)
                        _myPresence.value = PresenceState(userId = userId, isOnline = true, lastSeenTimestamp = System.currentTimeMillis())
                        updateSameWavelength()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Connected listener cancelled: ${error.message}")
                }
            })

            partnerRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isOnline = snapshot.child("isOnline").getValue(Boolean::class.java) ?: false
                    val lastSeen = snapshot.child("lastSeenTimestamp").getValue(Long::class.java) ?: System.currentTimeMillis()
                    _partnerPresence.value = PresenceState(userId = partnerUserId, isOnline = isOnline, lastSeenTimestamp = lastSeen)
                    updateSameWavelength()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Partner presence listener cancelled: ${error.message}")
                }
            })

        } catch (e: Throwable) {
            Log.e(TAG, "Failed initializing UserPresenceManager: ${e.message}")
        }
    }

    private fun updateSameWavelength() {
        val sameWavelength = _myPresence.value.isOnline && _partnerPresence.value.isOnline
        _isSameWavelength.value = sameWavelength
    }

    fun setOffline() {
        try {
            val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val userId = currentUser?.uid ?: "local_krama_user"
            val database = try { FirebaseDatabase.getInstance() } catch (e: Throwable) { null } ?: return
            database.getReference("status/$userId").setValue(
                mapOf("isOnline" to false, "lastSeenTimestamp" to System.currentTimeMillis())
            )
            _myPresence.value = PresenceState(userId = userId, isOnline = false, lastSeenTimestamp = System.currentTimeMillis())
            updateSameWavelength()
        } catch (e: Throwable) {
            Log.e(TAG, "Failed setting offline: ${e.message}")
        }
    }
}
