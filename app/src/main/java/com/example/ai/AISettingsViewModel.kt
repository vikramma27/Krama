package com.example.ai

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AIPrivacySettings(
    val processDataOnDeviceOnly: Boolean = true,
    val neverUploadConversations: Boolean = true,
    val allowSelectedChatsOnly: Boolean = false,
    val searchArchivedChats: Boolean = true,
    val searchDeletedCache: Boolean = false,
    val includeMedia: Boolean = false,
    val includeVoiceNotes: Boolean = false,
    val includeDocuments: Boolean = false,
    val includeVideos: Boolean = false
)

class AISettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val downloadManager = AIDownloadManager(application)
    private val localEngine = LocalAIEngine.getInstance(application)

    private val prefs = application.getSharedPreferences("krama_ai_prefs", Context.MODE_PRIVATE)

    val modelSpec = downloadManager.modelSpec

    val downloadState: StateFlow<AIModelStatus> = downloadManager.downloadState
    val downloadProgress: StateFlow<Float> = downloadManager.progress
    val statusMessage: StateFlow<String> = downloadManager.statusMessage

    private val _isPrivacyConsented = MutableStateFlow(prefs.getBoolean("ai_privacy_consented", false))
    val isPrivacyConsented: StateFlow<Boolean> = _isPrivacyConsented.asStateFlow()

    private val _isGlobalAiEnabled = MutableStateFlow(prefs.getBoolean("global_ai_enabled", true))
    val isGlobalAiEnabled: StateFlow<Boolean> = _isGlobalAiEnabled.asStateFlow()

    private val _privacySettings = MutableStateFlow(AIPrivacySettings())
    val privacySettings: StateFlow<AIPrivacySettings> = _privacySettings.asStateFlow()

    val activeCapabilities = localEngine.activeCapabilities

    private val _queryResult = MutableStateFlow<String?>(null)
    val queryResult: StateFlow<String?> = _queryResult.asStateFlow()

    private val _isProcessingQuery = MutableStateFlow(false)
    val isProcessingQuery: StateFlow<Boolean> = _isProcessingQuery.asStateFlow()

    fun grantPrivacyConsent() {
        prefs.edit().putBoolean("ai_privacy_consented", true).apply()
        _isPrivacyConsented.value = true
    }

    fun revokePrivacyConsent() {
        prefs.edit().putBoolean("ai_privacy_consented", false).apply()
        _isPrivacyConsented.value = false
        downloadManager.cancelDownload()
    }

    fun setGlobalAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("global_ai_enabled", enabled).apply()
        _isGlobalAiEnabled.value = enabled
    }

    fun startModelDownload() = downloadManager.startDownload()
    fun pauseModelDownload() = downloadManager.pauseDownload()
    fun resumeModelDownload() = downloadManager.resumeDownload()
    fun cancelModelDownload() = downloadManager.cancelDownload()

    fun toggleCapability(id: String, enabled: Boolean) {
        localEngine.toggleCapability(id, enabled)
    }

    fun updatePrivacySettings(newSettings: AIPrivacySettings) {
        _privacySettings.value = newSettings
    }

    fun processQuery(query: String, recentMessages: List<MessageEntity> = emptyList()) {
        viewModelScope.launch {
            _isProcessingQuery.value = true
            val result = localEngine.processQuery(query, recentMessages)
            _queryResult.value = result
            _isProcessingQuery.value = false
        }
    }

    fun wipeModelAndIndices() {
        downloadManager.deleteModelAndIndices()
        _queryResult.value = null
    }

    fun deleteModel() = wipeModelAndIndices()
}
