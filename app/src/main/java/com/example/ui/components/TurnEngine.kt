package com.example.ui.components

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameSessionData(
    val sessionId: String = "",
    val gameType: String = "LUDO",
    val currentPlayerId: String = "",
    val hostId: String = "Vikram",
    val guestId: String = "Shivani",
    val turnTimerSeconds: Int = 15,
    val moveCount: Int = 0,
    val lastAction: String = "Match Started",
    val isGameOver: Boolean = false,
    val winnerId: String = "",
    val myScore: Int = 0,
    val partnerScore: Int = 0
)

data class ActionPayload(
    val playerId: String = "",
    val actionType: String = "",
    val detail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class TurnEngine(
    val sessionId: String,
    val myPlayerId: String,
    val partnerPlayerId: String,
    val gameType: GameType,
    private val scope: CoroutineScope
) {
    private val TAG = "TurnEngine"

    private val _sessionState = MutableStateFlow(
        GameSessionData(
            sessionId = sessionId,
            gameType = gameType.name,
            currentPlayerId = myPlayerId,
            hostId = myPlayerId,
            guestId = partnerPlayerId
        )
    )
    val sessionState: StateFlow<GameSessionData> = _sessionState.asStateFlow()

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private var timerJob: Job? = null
    private var firebaseDb: FirebaseDatabase? = null
    private var sessionRefListener: ValueEventListener? = null
    private var connectionRefListener: ValueEventListener? = null

    init {
        setupFirebaseRealtimeSync()
        startTurnTimer()
    }

    private fun setupFirebaseRealtimeSync() {
        try {
            firebaseDb = FirebaseDatabase.getInstance()
            
            // Monitor network connection status via Firebase .info/connected
            val connectedRef = firebaseDb?.getReference(".info/connected")
            connectionRefListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val connected = snapshot.getValue(Boolean::class.java) ?: true
                    _isConnected.value = connected
                    _isReconnecting.value = !connected
                    Log.d(TAG, "Firebase network connection status changed: connected=$connected")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Connection listener cancelled: ${error.message}")
                }
            }
            connectedRef?.addValueEventListener(connectionRefListener!!)

            // Monitor game session node
            val sessionRef = firebaseDb?.getReference("game_sessions")?.child(sessionId)
            sessionRefListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentTurn = snapshot.child("currentPlayerId").getValue(String::class.java) ?: myPlayerId
                        val lastAct = snapshot.child("lastAction").getValue(String::class.java) ?: "Updated turn"
                        val moves = snapshot.child("moveCount").getValue(Int::class.java) ?: 0
                        val isOver = snapshot.child("isGameOver").getValue(Boolean::class.java) ?: false
                        val winner = snapshot.child("winnerId").getValue(String::class.java) ?: ""

                        _sessionState.value = _sessionState.value.copy(
                            currentPlayerId = currentTurn,
                            lastAction = lastAct,
                            moveCount = moves,
                            isGameOver = isOver,
                            winnerId = winner
                        )

                        // Reset timer on turn change
                        if (currentTurn != _sessionState.value.currentPlayerId) {
                            startTurnTimer()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Session listener cancelled: ${error.message}")
                }
            }
            sessionRef?.addValueEventListener(sessionRefListener!!)

        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Realtime DB init fallback (Offline mode active): ${e.message}")
        }
    }

    fun startTurnTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Main) {
            var remaining = 15
            while (remaining >= 0) {
                _sessionState.value = _sessionState.value.copy(turnTimerSeconds = remaining)
                if (remaining == 0) {
                    // Time expired - switch turn automatically
                    switchTurn("Timeout - turn skipped")
                    break
                }
                delay(1000)
                remaining--
            }
        }
    }

    fun executeAction(actionType: String, detail: String) {
        val nextPlayer = if (_sessionState.value.currentPlayerId == myPlayerId) partnerPlayerId else myPlayerId
        val newCount = _sessionState.value.moveCount + 1
        val updatedState = _sessionState.value.copy(
            currentPlayerId = nextPlayer,
            lastAction = "$actionType: $detail",
            moveCount = newCount,
            turnTimerSeconds = 15
        )
        _sessionState.value = updatedState

        // Sync action payload to Firebase Realtime Database
        try {
            val sessionRef = firebaseDb?.getReference("game_sessions")?.child(sessionId)
            sessionRef?.child("currentPlayerId")?.setValue(nextPlayer)
            sessionRef?.child("lastAction")?.setValue("$actionType: $detail")
            sessionRef?.child("moveCount")?.setValue(newCount)
            sessionRef?.child("lastActionPayload")?.setValue(
                ActionPayload(myPlayerId, actionType, detail)
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to push action to Firebase DB: ${e.message}")
        }

        startTurnTimer()
    }

    fun switchTurn(reason: String = "Turn ended") {
        executeAction("SWITCH_TURN", reason)
    }

    fun simulateConnectionLoss(lost: Boolean) {
        _isConnected.value = !lost
        _isReconnecting.value = lost
    }

    fun finishGame(winnerId: String) {
        timerJob?.cancel()
        _sessionState.value = _sessionState.value.copy(
            isGameOver = true,
            winnerId = winnerId,
            lastAction = "Game Over! Winner: $winnerId"
        )
        try {
            val sessionRef = firebaseDb?.getReference("game_sessions")?.child(sessionId)
            sessionRef?.child("isGameOver")?.setValue(true)
            sessionRef?.child("winnerId")?.setValue(winnerId)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to finish game on Firebase: ${e.message}")
        }
    }

    fun cleanup() {
        timerJob?.cancel()
        try {
            connectionRefListener?.let {
                firebaseDb?.getReference(".info/connected")?.removeEventListener(it)
            }
            sessionRefListener?.let {
                firebaseDb?.getReference("game_sessions")?.child(sessionId)?.removeEventListener(it)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Cleanup listeners error: ${e.message}")
        }
    }
}
