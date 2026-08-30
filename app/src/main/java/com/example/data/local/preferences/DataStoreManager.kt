package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "krama_user_preferences")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode_enabled")
        private val KEY_NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        private val KEY_AUTO_REPLY_DRIVING = booleanPreferencesKey("auto_reply_driving")
        private val KEY_AUTO_REPLY_MSG = stringPreferencesKey("auto_reply_msg")
        private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
        private val KEY_ACTIVE_THEME = stringPreferencesKey("active_color_theme")
        private val KEY_MUSIC_SYNC_ENABLED = booleanPreferencesKey("music_sync_enabled")
        private val KEY_DEFAULT_REACTION = stringPreferencesKey("default_quick_reaction")
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_DARK_MODE] ?: true
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[KEY_DARK_MODE] = enabled
        }
    }

    val autoReplyDrivingFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_AUTO_REPLY_DRIVING] ?: false
    }

    val autoReplyMsgFlow: Flow<String> = context.dataStore.data.map { pref ->
        pref[KEY_AUTO_REPLY_MSG] ?: "🚗 I'm driving right now, will get back to you soon!"
    }

    suspend fun setAutoReplyDriving(enabled: Boolean, message: String) {
        context.dataStore.edit { pref ->
            pref[KEY_AUTO_REPLY_DRIVING] = enabled
            pref[KEY_AUTO_REPLY_MSG] = message
        }
    }

    val biometricLockFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_BIOMETRIC_LOCK] ?: false
    }

    suspend fun setBiometricLock(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[KEY_BIOMETRIC_LOCK] = enabled
        }
    }

    val musicSyncEnabledFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_MUSIC_SYNC_ENABLED] ?: true
    }

    suspend fun setMusicSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[KEY_MUSIC_SYNC_ENABLED] = enabled
        }
    }

    val defaultQuickReactionFlow: Flow<String> = context.dataStore.data.map { pref ->
        pref[KEY_DEFAULT_REACTION] ?: "❤️"
    }

    suspend fun setDefaultQuickReaction(reaction: String) {
        context.dataStore.edit { pref ->
            pref[KEY_DEFAULT_REACTION] = reaction
        }
    }
}
