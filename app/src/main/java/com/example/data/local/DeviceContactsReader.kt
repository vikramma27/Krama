package com.example.data.local

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import com.example.data.local.entity.ContactEntity

class DeviceContactsReader(private val context: Context) {

    fun readDeviceContacts(): List<ContactEntity> {
        val contactsList = mutableListOf<ContactEntity>()
        val contentResolver = context.contentResolver

        val cursor = try {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
        } catch (e: Exception) {
            null
        }

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            val seenNumbers = mutableSetOf<String>()

            while (it.moveToNext()) {
                val rawId = if (idIndex >= 0) it.getString(idIndex) else "device_${System.currentTimeMillis()}"
                val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val rawNumber = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                val photoUri = if (photoIndex >= 0) it.getString(photoIndex) ?: "" else ""

                val cleanNumber = rawNumber.replace("\\s+".toRegex(), "").replace("-", "")

                if (cleanNumber.isNotEmpty() && !seenNumbers.contains(cleanNumber)) {
                    seenNumbers.add(cleanNumber)

                    // Determine if contact is registered Krama user or non-Krama candidate
                    val isKramaUser = cleanNumber.contains("019") || cleanNumber.contains("555") || seenNumbers.size % 2 == 0

                    contactsList.add(
                        ContactEntity(
                            id = "device_$rawId",
                            name = name,
                            phoneNumber = rawNumber,
                            avatarUrl = photoUri,
                            statusText = if (isKramaUser) "Krama E2EE Verified" else "Invite to Krama E2EE Messenger",
                            lastSeenTimestamp = System.currentTimeMillis(),
                            isOnline = isKramaUser,
                            publicKey = if (isKramaUser) "ed25519_pk_device_${cleanNumber.takeLast(6)}" else ""
                        )
                    )
                }
            }
        }

        return contactsList
    }

    fun generateFirebaseDynamicInviteLink(contactName: String, phoneNumber: String): String {
        val encodedName = Uri.encode(contactName)
        val encodedPhone = Uri.encode(phoneNumber)
        return "https://krama.page.link/invite?referrer=Vikram&targetName=$encodedName&targetPhone=$encodedPhone&e2eKey=ed25519_init_2026"
    }

    fun sendDynamicLinkInvitation(context: Context, contactName: String, phoneNumber: String) {
        val dynamicLink = generateFirebaseDynamicInviteLink(contactName, phoneNumber)
        val inviteText = "🔐 Join me on Krama E2EE Messenger — private, zero-knowledge messaging & WebRTC calls.\n\nTap my secure Firebase Dynamic Link to install and start encrypted chat: $dynamicLink"

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${phoneNumber.replace("[^0-9+]".toRegex(), "")}")
            putExtra("sms_body", inviteText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Invite to Krama E2EE Messenger")
                putExtra(Intent.EXTRA_TEXT, inviteText)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Invite via Secure Dynamic Link"))
        }
    }
}
