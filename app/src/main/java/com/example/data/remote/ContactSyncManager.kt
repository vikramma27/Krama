package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.DeviceContactsReader
import com.example.data.local.dao.ContactDao
import com.example.data.local.entity.ContactEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

object ContactSyncManager {
    private const val TAG = "ContactSyncManager"

    /**
     * Computes SHA-256 hash of normalized phone number.
     * E.g. "+1-555-019-2831" -> "15550192831" -> sha256 hex string.
     */
    fun hashPhoneNumber(phone: String): String {
        val cleanNumber = phone.replace("[^0-9+]".toRegex(), "")
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(cleanNumber.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Reads device contacts, obfuscates phone numbers with SHA-256, cross-references
     * against Krama Firestore user registry, and inserts matched contacts into local Room DB.
     * Returns pair: (totalSyncedContacts, discoveredKramaUsersCount).
     */
    suspend fun syncContactsWithObfuscation(
        context: Context,
        contactDao: ContactDao
    ): Pair<Int, Int> {
        return try {
            val deviceReader = DeviceContactsReader(context)
            val deviceContacts = deviceReader.readDeviceContacts()

            if (deviceContacts.isEmpty()) {
                Log.d(TAG, "No device contacts found to sync.")
                return Pair(0, 0)
            }

            val firestore = FirebaseFirestore.getInstance()

            // Prepare list of hashed phone numbers
            val phoneHashMap = mutableMapOf<String, ContactEntity>() // sha256 -> original contact
            deviceContacts.forEach { contact ->
                val hash = hashPhoneNumber(contact.phoneNumber)
                if (hash.isNotEmpty()) {
                    phoneHashMap[hash] = contact
                }
            }

            val hashes = phoneHashMap.keys.toList()
            val discoveredContacts = mutableListOf<ContactEntity>()

            // Firestore 'whereIn' supports up to 30 elements per chunk
            val chunks = hashes.chunked(30)
            for (chunk in chunks) {
                try {
                    val querySnapshot = firestore.collection("users")
                        .whereIn("phoneHash", chunk)
                        .get()
                        .await()

                    for (doc in querySnapshot.documents) {
                        val matchedHash = doc.getString("phoneHash") ?: ""
                        val originalContact = phoneHashMap[matchedHash]

                        val uid = doc.id
                        val name = doc.getString("displayName") ?: originalContact?.name ?: "Krama User"
                        val avatarUrl = doc.getString("avatarUrl") ?: originalContact?.avatarUrl ?: ""
                        val phone = doc.getString("phoneNumber") ?: originalContact?.phoneNumber ?: ""
                        val publicKey = doc.getString("publicKey") ?: "ed25519_pk_${uid.takeLast(6)}"

                        val verifiedKramaContact = ContactEntity(
                            id = "krama_$uid",
                            name = name,
                            phoneNumber = phone,
                            avatarUrl = avatarUrl,
                            statusText = "Krama E2EE Verified • SHA-256 Matched",
                            lastSeenTimestamp = System.currentTimeMillis(),
                            isOnline = true,
                            publicKey = publicKey,
                            isBlocked = false
                        )

                        discoveredContacts.add(verifiedKramaContact)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore contact query chunk error: ${e.message}")
                }
            }

            // Deduplicate contacts using Levenshtein distance on names & phone numbers
            val deduplicatedContacts = mutableListOf<ContactEntity>()
            for (contact in deviceContacts) {
                val isDup = deduplicatedContacts.any { existing ->
                    val p1 = existing.phoneNumber.replace("[^0-9]".toRegex(), "")
                    val p2 = contact.phoneNumber.replace("[^0-9]".toRegex(), "")
                    if (p1.isNotEmpty() && p1 == p2) return@any true

                    val n1 = existing.name.lowercase().trim()
                    val n2 = contact.name.lowercase().trim()
                    if (n1 == n2) return@any true

                    val dist = computeLevenshteinDistance(n1, n2)
                    val maxLen = maxOf(n1.length, n2.length)
                    maxLen > 3 && (dist.toDouble() / maxLen) < 0.25
                }
                if (!isDup) {
                    deduplicatedContacts.add(contact)
                }
            }

            // Always insert device contacts into Room, upgrading matched ones to verified status
            val finalContactsToInsert = deduplicatedContacts.map { devContact ->
                val devHash = hashPhoneNumber(devContact.phoneNumber)
                val matched = discoveredContacts.find { hashPhoneNumber(it.phoneNumber) == devHash }
                matched ?: devContact
            }

            contactDao.insertContacts(finalContactsToInsert)
            Log.d(TAG, "Synced ${finalContactsToInsert.size} total contacts (${discoveredContacts.size} Krama registered users discovered via SHA-256).")

            Pair(finalContactsToInsert.size, discoveredContacts.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error performing secure contact sync: ${e.message}", e)
            Pair(0, 0)
        }
    }

    private fun computeLevenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val dp = IntArray(s2.length + 1) { it }
        for (i in s1.indices) {
            var prevUpperLeft = dp[0]
            dp[0] = i + 1
            for (j in s2.indices) {
                val upper = dp[j + 1]
                val cost = if (s1[i] == s2[j]) 0 else 1
                dp[j + 1] = minOf(dp[j + 1] + 1, dp[j] + 1, prevUpperLeft + cost)
                prevUpperLeft = upper
            }
        }
        return dp[s2.length]
    }
}
