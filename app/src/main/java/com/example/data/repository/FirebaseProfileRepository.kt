package com.example.data.repository

import android.util.Log
import com.example.domain.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseProfileRepository {
    private val TAG = "FirebaseProfileRepo"

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth not initialized: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore not initialized: ${e.message}")
            null
        }
    }

    suspend fun syncProfileToCloud(profile: UserProfile): Boolean {
        return try {
            val db = firestore ?: return false
            val uid = auth?.currentUser?.uid ?: "local_user_${profile.phoneNumber.replace("[^0-9]".toRegex(), "")}"

            val profileMap = mapOf(
                "uid" to uid,
                "name" to profile.name,
                "username" to profile.username,
                "email" to profile.email,
                "phoneNumber" to profile.phoneNumber,
                "avatarUrl" to profile.avatarUrl,
                "statusText" to profile.statusText,
                "lastSeenPrivacy" to profile.lastSeenPrivacy,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users").document(uid).set(profileMap).await()
            Log.i(TAG, "User profile successfully synced to Firestore for $uid")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to sync user profile to Firestore: ${e.message}")
            false
        }
    }

    suspend fun fetchProfileFromCloud(uid: String): UserProfile? {
        return try {
            val db = firestore ?: return null
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile(
                    name = doc.getString("name") ?: "",
                    username = doc.getString("username") ?: "",
                    email = doc.getString("email") ?: "",
                    phoneNumber = doc.getString("phoneNumber") ?: "",
                    avatarUrl = doc.getString("avatarUrl") ?: "",
                    statusText = doc.getString("statusText") ?: "",
                    lastSeenPrivacy = doc.getString("lastSeenPrivacy") ?: "Contacts Only"
                )
            } else null
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to fetch cloud profile: ${e.message}")
            null
        }
    }
}
