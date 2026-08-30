package com.example.data.repository

import android.content.Context
import com.example.domain.model.ChatWallpaperConfig
import com.example.domain.model.UserProfile
import com.example.worker.EncryptedChatSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository {

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _matrixServerStatus = MutableStateFlow("Connected to Synapse (matrix.krama.internal:8448)")
    val matrixServerStatus: StateFlow<String> = _matrixServerStatus.asStateFlow()

    fun updateNameAndPhone(name: String, phone: String, email: String = "") {
        _userProfile.value = _userProfile.value.copy(
            name = name,
            phoneNumber = phone,
            email = if (email.isNotBlank()) email else _userProfile.value.email
        )
    }

    fun updateUserProfile(name: String, username: String, avatarUrl: String, statusText: String) {
        val formattedUsername = if (username.startsWith("@")) username else "@$username"
        _userProfile.value = _userProfile.value.copy(
            name = name,
            username = formattedUsername,
            avatarUrl = avatarUrl,
            statusText = statusText
        )
    }

    fun updateProfilePhoto(avatarUrl: String) {
        _userProfile.value = _userProfile.value.copy(avatarUrl = avatarUrl)
    }

    fun toggleReadReceipts(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(readReceiptsEnabled = enabled)
    }

    fun setLastSeenPrivacy(privacy: String) {
        _userProfile.value = _userProfile.value.copy(lastSeenPrivacy = privacy)
    }

    fun toggleLowDataBatteryMode(context: Context, enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(isLowDataBatteryMode = enabled)
        EncryptedChatSyncWorker.schedulePeriodicSync(context, isLowDataBatteryMode = enabled)
        com.example.service.PowerSaverManager.applyPowerSaverMode(context, enabled)
    }

    fun toggleScreenLockPrivacy(enabled: Boolean) {
        _userProfile.value = _userProfile.value.copy(isScreenLockPrivacyEnabled = enabled)
    }

    fun updateWallpaperConfig(config: ChatWallpaperConfig) {
        _userProfile.value = _userProfile.value.copy(wallpaperConfig = config)
    }
}
