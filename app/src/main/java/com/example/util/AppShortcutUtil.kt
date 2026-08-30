package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.ChatEntity

object AppShortcutUtil {
    fun updateAppShortcuts(context: Context, chats: List<ChatEntity>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        try {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
            
            // Take top 3 most recent active chats
            val topChats = chats.take(3)
            val shortcuts = topChats.map { chat ->
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("EXTRA_CHAT_ID", chat.id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }

                ShortcutInfo.Builder(context, "shortcut_${chat.id}")
                    .setShortLabel(chat.title)
                    .setLongLabel("Chat with ${chat.title}")
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(intent)
                    .build()
            }

            shortcutManager.dynamicShortcuts = shortcuts
        } catch (e: Throwable) {
            android.util.Log.e("AppShortcutUtil", "Failed to update dynamic app shortcuts: ${e.message}")
        }
    }
}
