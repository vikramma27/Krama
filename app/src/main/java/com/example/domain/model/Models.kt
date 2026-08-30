package com.example.domain.model

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}

enum class MessageType {
    TEXT, IMAGE, VOICE, FILE, LOCATION, CONTACT, CALL_LOG
}

enum class DisappearingTimer(val label: String, val seconds: Long) {
    OFF("Off", 0L),
    TWENTY_FOUR_HOURS("24 Hours", 86400L),
    SEVEN_DAYS("7 Days", 604800L),
    NINETY_DAYS("90 Days", 7776000L)
}

enum class AudienceType(val label: String) {
    ALL_CONTACTS("All Contacts"),
    EXCEPT("Contacts except..."),
    ONLY_SHARE("Only share with...")
}

data class E2ESafetyNumber(
    val fingerPrint: String,
    val publicKeySender: String,
    val publicKeyRecipient: String,
    val cipherAlgorithm: String = "Matrix Olm/Megolm (Double Ratchet AES-256-GCM)",
    val isVerified: Boolean = true
)

data class ChatWallpaperConfig(
    val wallpaperId: String = "DEFAULT",
    val customUri: String = "",
    val blurRadiusDp: Float = 8f,
    val darkTintOpacity: Float = 0.45f,
    val accentColorHex: String = "#26A69A",
    val backgroundPattern: String = "DOTS"
)

data class UserProfile(
    val name: String = "Vikram",
    val username: String = "@vikram",
    val email: String = "vikram@krama.sec",
    val phoneNumber: String = "+1 (555) 019-2834",
    val avatarUrl: String = "",
    val statusText: String = "Encrypted & Connected over Krama Matrix",
    val isBiometricEnabled: Boolean = true,
    val isAppLocked: Boolean = false,
    val pinCode: String = "1234",
    val readReceiptsEnabled: Boolean = true,
    val lastSeenPrivacy: String = "Contacts Only",
    val autoLockSeconds: Int = 30,
    val isLowDataBatteryMode: Boolean = false,
    val isScreenLockPrivacyEnabled: Boolean = false,
    val wallpaperConfig: ChatWallpaperConfig = ChatWallpaperConfig()
)
