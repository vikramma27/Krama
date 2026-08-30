package com.example.media

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.MainActivity

class MediaDownloadShareManager private constructor(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun downloadFile(url: String, fileName: String, title: String): Long {
        return try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(title)
                setDescription("Downloading attachment: $fileName")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val downloadId = downloadManager.enqueue(request)
            Log.i(TAG, "Enqueued download ID=$downloadId for file $fileName")
            downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Download enqueue error: ${e.message}")
            -1L
        }
    }

    fun shareText(text: String, title: String = "Share via Krama") {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Android Sharesheet: ${e.message}")
        }
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Log.i(TAG, "Copied to clipboard: $label")
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying to clipboard: ${e.message}")
        }
    }

    fun saveMediaToGallery(mediaUrlOrPath: String, title: String): Uri? {
        return try {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "Krama_${System.currentTimeMillis()}.jpg")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Krama Messenger")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (mediaUrlOrPath.startsWith("http://") || mediaUrlOrPath.startsWith("https://")) {
                        java.net.URL(mediaUrlOrPath).openStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } else if (mediaUrlOrPath.startsWith("content://")) {
                        resolver.openInputStream(Uri.parse(mediaUrlOrPath))?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    } else if (mediaUrlOrPath.isNotEmpty()) {
                        java.io.File(mediaUrlOrPath).inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
            Log.i(TAG, "Successfully saved media to MediaStore Gallery: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving media to MediaStore: ${e.message}")
            null
        }
    }

    fun updateDynamicShortcuts(chatTitle: String, chatId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            try {
                val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("EXTRA_CHAT_ID", chatId)
                }

                val shortcut = ShortcutInfo.Builder(context, "shortcut_$chatId")
                    .setShortLabel(chatTitle)
                    .setLongLabel("Open conversation with $chatTitle")
                    .setIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_info))
                    .setIntent(intent)
                    .build()

                shortcutManager?.dynamicShortcuts = listOf(shortcut)
                Log.i(TAG, "Registered dynamic shortcut for chatId=$chatId")
            } catch (e: Exception) {
                Log.w(TAG, "Dynamic shortcut creation notice: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "MediaDownloadShareMgr"

        @Volatile
        private var INSTANCE: MediaDownloadShareManager? = null

        fun getInstance(context: Context): MediaDownloadShareManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaDownloadShareManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
