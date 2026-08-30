package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkPlumCard
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral

object ThreadNotificationPrefs {
    private const val PREF_NAME = "krama_thread_notifications"

    fun getVibrationPattern(context: Context, chatId: String): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("vibration_$chatId", "Pulse") ?: "Pulse"
    }

    fun getSoundProfile(context: Context, chatId: String): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("sound_$chatId", "Crystal Chime") ?: "Crystal Chime"
    }

    fun getPopupBehavior(context: Context, chatId: String): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString("popup_$chatId", "Heads-up Popup") ?: "Heads-up Popup"
    }

    fun saveThreadNotificationPrefs(
        context: Context,
        chatId: String,
        vibration: String,
        sound: String,
        popup: String
    ) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("vibration_$chatId", vibration)
            .putString("sound_$chatId", sound)
            .putString("popup_$chatId", popup)
            .apply()
    }
}

@Composable
fun ThreadNotificationSettingsDialog(
    chatId: String,
    chatTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var vibrationPattern by remember { mutableStateOf(ThreadNotificationPrefs.getVibrationPattern(context, chatId)) }
    var soundProfile by remember { mutableStateOf(ThreadNotificationPrefs.getSoundProfile(context, chatId)) }
    var popupBehavior by remember { mutableStateOf(ThreadNotificationPrefs.getPopupBehavior(context, chatId)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkPlumCard,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SoftTeal)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Custom Thread Notifications",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "For $chatTitle",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Vibration Pattern Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = SoftTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vibration Pattern", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                listOf("Default", "Pulse", "Double Tap", "Heavy Alert", "Silent").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vibrationPattern = option }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = vibrationPattern == option,
                            onClick = { vibrationPattern = option },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Profile Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = WarmCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sound Profile", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                listOf("Default Ringtone", "Crystal Chime", "Cyber Beep", "Soft Bell", "Mute").forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { soundProfile = sound }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = soundProfile == sound,
                            onClick = { soundProfile = sound },
                            colors = RadioButtonDefaults.colors(selectedColor = WarmCoral)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sound, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Popup & Banner Behavior Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = SoftTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Popup & Banner Behavior", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                listOf("Heads-up Popup", "Silent Banner", "Lock Screen Only", "Muted").forEach { behavior ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { popupBehavior = behavior }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = popupBehavior == behavior,
                            onClick = { popupBehavior = behavior },
                            colors = RadioButtonDefaults.colors(selectedColor = SoftTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(behavior, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ThreadNotificationPrefs.saveThreadNotificationPrefs(
                        context = context,
                        chatId = chatId,
                        vibration = vibrationPattern,
                        sound = soundProfile,
                        popup = popupBehavior
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                modifier = Modifier.testTag("save_thread_notification_prefs")
            ) {
                Text("Save Notification Profile", color = NearBlackPlum, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color.LightGray)
            }
        }
    )
}
