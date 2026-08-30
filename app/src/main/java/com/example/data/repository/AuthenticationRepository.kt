package com.example.data.repository

import android.content.Context
import timber.log.Timber
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthenticationState {
    object Unauthenticated : AuthenticationState()
    object NewUser : AuthenticationState()
    object SessionExpired : AuthenticationState()
    object Loading : AuthenticationState()
    data class Authenticated(
        val uid: String,
        val email: String?,
        val phoneNumber: String?,
        val displayName: String?
    ) : AuthenticationState()
    data class BiometricLocked(
        val uid: String,
        val email: String?
    ) : AuthenticationState()
    data class Error(val message: String) : AuthenticationState()
}

class AuthenticationRepository(private val context: Context) {

    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "krama_secure_session_prefs"
        private const val KEY_SESSION_ACTIVE = "key_session_active"
        private const val KEY_HAS_EVER_LOGGED_IN = "key_has_ever_logged_in"
        private const val KEY_SESSION_EXPIRED = "key_session_expired"
        private const val KEY_USER_UID = "key_user_uid"
        private const val KEY_USER_EMAIL = "key_user_email"

        @Volatile
        private var INSTANCE: AuthenticationRepository? = null

        fun getInstance(context: Context): AuthenticationRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthenticationRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    private val safeAuth: FirebaseAuth?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    Timber.tag(TAG).i("[FIREBASE INIT] Initializing FirebaseApp for context...")
                    FirebaseApp.initializeApp(context)
                }
                val instance = FirebaseAuth.getInstance()
                Timber.tag(TAG).d("[FIREBASE HANDSHAKE] FirebaseAuth instance active. Current user: %s", instance.currentUser?.uid)
                instance
            } catch (e: Throwable) {
                Timber.tag(TAG).w(e, "[FIREBASE HANDSHAKE NOTE] Handshake or init exception: %s", e.message)
                null
            }
        }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _authState = MutableStateFlow<AuthenticationState>(evaluateInitialAuthState())
    val authState: StateFlow<AuthenticationState> = _authState.asStateFlow()

    private fun updateAuthState(newState: AuthenticationState) {
        val oldState = _authState.value
        Timber.tag(TAG).i("[AUTH STATE TRANSITION] %s -> %s", oldState::class.simpleName, newState::class.simpleName)
        _authState.value = newState
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    val currentUserId: String?
        get() = safeAuth?.currentUser?.uid ?: prefs.getString(KEY_USER_UID, null)

    val currentUserEmail: String?
        get() = safeAuth?.currentUser?.email ?: prefs.getString(KEY_USER_EMAIL, null)

    init {
        Timber.tag(TAG).i("[AUTH REPO INIT] AuthenticationRepository initialized. Initial state: %s", _authState.value::class.simpleName)
        setupFirebaseAuthStateListener()
        verifySessionTokenOnColdStart()
    }

    private fun verifySessionTokenOnColdStart() {
        scope.launch {
            try {
                Timber.tag(TAG).d("[COLD START TOKEN VERIFICATION] Verifying local auth token freshness...")
                val user = safeAuth?.currentUser
                if (user != null) {
                    try {
                        val tokenResult = user.getIdToken(false).await()
                        Timber.tag(TAG).i("[COLD START TOKEN VERIFICATION] Token verified successfully. Token expiration time: %d", tokenResult.expirationTimestamp)
                        saveLocalSession(user)
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "[COLD START TOKEN VERIFICATION FAILED] Token verification failed: %s", e.message)
                        if (e.message?.contains("EXPIRED") == true || e.message?.contains("REVOKED") == true || e.message?.contains("user-disabled") == true) {
                            triggerSessionExpired()
                        }
                    }
                } else if (prefs.getBoolean(KEY_SESSION_EXPIRED, false)) {
                    updateAuthState(AuthenticationState.SessionExpired)
                }
            } catch (e: Throwable) {
                Timber.tag(TAG).w(e, "[COLD START VERIFICATION EXCEPTION] Error during cold start token verification: %s", e.message)
            }
        }
    }

    private fun evaluateInitialAuthState(): AuthenticationState {
        Timber.tag(TAG).d("[EVALUATE AUTH] Starting initial auth state evaluation...")
        val user = safeAuth?.currentUser
        if (user != null) {
            Timber.tag(TAG).i("[EVALUATE AUTH] Active Firebase user found: UID=%s, email=%s", user.uid, user.email)
            saveLocalSession(user)
            return AuthenticationState.Authenticated(
                uid = user.uid,
                email = user.email,
                phoneNumber = user.phoneNumber,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Krama User"
            )
        } else {
            val wasSessionActive = prefs.getBoolean(KEY_SESSION_ACTIVE, false)
            val hasEverLoggedIn = prefs.getBoolean(KEY_HAS_EVER_LOGGED_IN, false)
            val isExplicitlyExpired = prefs.getBoolean(KEY_SESSION_EXPIRED, false)
            val storedUid = prefs.getString(KEY_USER_UID, null)
            val storedEmail = prefs.getString(KEY_USER_EMAIL, null)

            Timber.tag(TAG).d("[EVALUATE AUTH] Prefs state: active=%b, everLogged=%b, expired=%b, uid=%s",
                wasSessionActive, hasEverLoggedIn, isExplicitlyExpired, storedUid)

            return when {
                wasSessionActive && !storedUid.isNullOrEmpty() -> {
                    Timber.tag(TAG).i("[EVALUATE AUTH] Recovered active local session for UID=%s", storedUid)
                    AuthenticationState.Authenticated(
                        uid = storedUid!!,
                        email = storedEmail,
                        phoneNumber = null,
                        displayName = storedEmail?.substringBefore("@") ?: "Encrypted User"
                    )
                }
                isExplicitlyExpired -> {
                    Timber.tag(TAG).w("[EVALUATE AUTH] Session is marked explicitly expired.")
                    AuthenticationState.SessionExpired
                }
                !hasEverLoggedIn -> {
                    Timber.tag(TAG).i("[EVALUATE AUTH] New user detected (no prior login history).")
                    AuthenticationState.NewUser
                }
                else -> {
                    Timber.tag(TAG).i("[EVALUATE AUTH] Defaulting to Unauthenticated state.")
                    AuthenticationState.Unauthenticated
                }
            }
        }
    }

    private fun setupFirebaseAuthStateListener() {
        try {
            val firebaseAuth = safeAuth
            if (firebaseAuth == null) {
                Timber.tag(TAG).w("[AUTH LISTENER] Firebase auth instance unavailable. Preserving local state: %s", _authState.value::class.simpleName)
                return
            }
            firebaseAuth.addAuthStateListener { fa ->
                val evalState = evaluateInitialAuthState()
                Timber.tag(TAG).i("[AUTH LISTENER EVENT] AuthStateListener fired. Resolved state: %s", evalState::class.simpleName)
                updateAuthState(evalState)
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "[AUTH LISTENER ERROR] Error attaching AuthStateListener: %s", e.message)
        }
    }

    private fun saveLocalSession(user: FirebaseUser) {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putBoolean(KEY_HAS_EVER_LOGGED_IN, true)
            .putBoolean(KEY_SESSION_EXPIRED, false)
            .putString(KEY_USER_UID, user.uid)
            .putString(KEY_USER_EMAIL, user.email)
            .apply()
        Timber.tag(TAG).d("[LOCAL SESSION] Saved session for user UID=%s", user.uid)
    }

    private fun clearLocalSession(isExpired: Boolean = false) {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .putBoolean(KEY_SESSION_EXPIRED, isExpired)
            .remove(KEY_USER_UID)
            .remove(KEY_USER_EMAIL)
            .apply()
        Timber.tag(TAG).d("[LOCAL SESSION] Cleared session. isExpired=%b", isExpired)
    }

    suspend fun signUpWithEmail(email: String, pass: String, name: String): Result<String> {
        updateAuthState(AuthenticationState.Loading)
        return try {
            Timber.tag(TAG).i("[EMAIL SIGN UP] Attempting user registration for %s...", email)
            val auth = safeAuth ?: throw IllegalStateException("Firebase Authentication is not available")
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw IllegalStateException("Firebase User creation returned null")
            
            // Set display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            saveLocalSession(user)
            updateAuthState(AuthenticationState.Authenticated(
                uid = user.uid,
                email = user.email,
                phoneNumber = user.phoneNumber,
                displayName = name
            ))
            Result.success(user.uid)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[EMAIL SIGN UP FAILED] Error: %s", e.message)
            val friendlyMsg = parseAuthException(e)
            updateAuthState(AuthenticationState.Error(friendlyMsg))
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<String> {
        updateAuthState(AuthenticationState.Loading)
        return try {
            Timber.tag(TAG).i("[EMAIL SIGN IN] Attempting sign in for %s...", email)
            val auth = safeAuth ?: throw IllegalStateException("Firebase Authentication is not available")
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw IllegalStateException("Firebase User sign in returned null")

            saveLocalSession(user)
            updateAuthState(AuthenticationState.Authenticated(
                uid = user.uid,
                email = user.email,
                phoneNumber = user.phoneNumber,
                displayName = user.displayName ?: email.substringBefore("@")
            ))
            Result.success(user.uid)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[EMAIL SIGN IN FAILED] Error: %s", e.message)
            val friendlyMsg = parseAuthException(e)
            updateAuthState(AuthenticationState.Error(friendlyMsg))
            Result.failure(e)
        }
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<String> {
        updateAuthState(AuthenticationState.Loading)
        return try {
            Timber.tag(TAG).i("[PHONE SIGN IN] Attempting sign in with PhoneAuthCredential...")
            val auth = safeAuth ?: throw IllegalStateException("Firebase Authentication is not available")
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw IllegalStateException("Phone credential authentication returned null")

            saveLocalSession(user)
            updateAuthState(AuthenticationState.Authenticated(
                uid = user.uid,
                email = user.email,
                phoneNumber = user.phoneNumber,
                displayName = user.displayName ?: "Krama User"
            ))
            Result.success(user.uid)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[PHONE SIGN IN FAILED] Error: %s", e.message)
            val friendlyMsg = parseAuthException(e)
            updateAuthState(AuthenticationState.Error(friendlyMsg))
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        return try {
            Timber.tag(TAG).i("[PASSWORD RESET] Sending reset email to %s...", email)
            val auth = safeAuth ?: throw IllegalStateException("Firebase Authentication is not available")
            auth.sendPasswordResetEmail(email).await()
            Result.success("Password reset email successfully sent to $email.")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[PASSWORD RESET FAILED] Error: %s", e.message)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPass: String): Result<String> {
        return try {
            Timber.tag(TAG).i("[UPDATE PASSWORD] Attempting password update...")
            val user = safeAuth?.currentUser ?: throw IllegalStateException("No active authenticated session")
            user.updatePassword(newPass).await()
            Result.success("Password updated successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "[UPDATE PASSWORD FAILED] Error: %s", e.message)
            Result.failure(e)
        }
    }

    fun triggerSessionExpired() {
        Timber.tag(TAG).w("[EXPIRE SESSION] Triggering explicit session expiry...")
        try {
            safeAuth?.signOut()
        } catch (e: Throwable) {
            Timber.tag(TAG).w(e, "[EXPIRE SESSION NOTE] Firebase signout note: %s", e.message)
        }
        clearLocalSession(isExpired = true)
        updateAuthState(AuthenticationState.SessionExpired)
    }

    fun signOut() {
        Timber.tag(TAG).i("[SIGN OUT] Signing out user...")
        try {
            safeAuth?.signOut()
        } catch (e: Throwable) {
            Timber.tag(TAG).w(e, "[SIGN OUT NOTE] Firebase signout note: %s", e.message)
        }
        clearLocalSession(isExpired = false)
        updateAuthState(AuthenticationState.Unauthenticated)
    }

    fun setAuthenticatedSession(uid: String, email: String? = null, phone: String? = null, displayName: String? = null) {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putBoolean(KEY_HAS_EVER_LOGGED_IN, true)
            .putBoolean(KEY_SESSION_EXPIRED, false)
            .putString(KEY_USER_UID, uid)
            .apply()
        if (!email.isNullOrBlank()) {
            prefs.edit().putString(KEY_USER_EMAIL, email).apply()
        }

        updateAuthState(
            AuthenticationState.Authenticated(
                uid = uid,
                email = email,
                phoneNumber = phone,
                displayName = displayName ?: email?.substringBefore("@") ?: "Krama User"
            )
        )
    }

    fun getCurrentUser(): FirebaseUser? = safeAuth?.currentUser

    fun authenticateWithBiometricSuccess() {
        val user = safeAuth?.currentUser
        val uid = user?.uid ?: prefs.getString(KEY_USER_UID, null) ?: "user_me"
        val email = user?.email ?: prefs.getString(KEY_USER_EMAIL, null)
        updateAuthState(
            AuthenticationState.Authenticated(
                uid = uid,
                email = email,
                phoneNumber = user?.phoneNumber,
                displayName = user?.displayName ?: email?.substringBefore("@") ?: "Krama User"
            )
        )
    }

    fun lockWithBiometric() {
        val user = safeAuth?.currentUser
        val uid = user?.uid ?: prefs.getString(KEY_USER_UID, null) ?: "user_me"
        val email = user?.email ?: prefs.getString(KEY_USER_EMAIL, null)
        updateAuthState(AuthenticationState.BiometricLocked(uid, email))
    }

    private fun parseAuthException(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("FirebaseAuthInvalidUserException", ignoreCase = true) ||
            msg.contains("user-not-found", ignoreCase = true) ->
                "No registered account found with this email."

            msg.contains("FirebaseAuthInvalidCredentialsException", ignoreCase = true) ||
            msg.contains("wrong-password", ignoreCase = true) ->
                "Invalid password or credentials entered."

            msg.contains("FirebaseAuthUserCollisionException", ignoreCase = true) ||
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "An account with this email address already exists. Please sign in instead."

            msg.contains("network", ignoreCase = true) ->
                "Network connection error during authentication."

            else -> e.localizedMessage ?: "Authentication failed."
        }
    }

    private fun String?.isNull_orEmpty_safe(): Boolean {
        return this == null || this.trim().isEmpty()
    }
}

