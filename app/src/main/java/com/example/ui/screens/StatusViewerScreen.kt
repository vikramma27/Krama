package com.example.ui.screens

import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EncryptedMediaManager
import com.example.data.local.entity.StatusStoryEntity
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import java.io.File
import java.io.FileOutputStream

@Composable
fun StatusViewerScreen(
    story: StatusStoryEntity,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgColor = try {
        Color(android.graphics.Color.parseColor(story.backgroundColorHex))
    } catch (e: Exception) {
        Color(0xFF3B2E7E)
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("status_viewer_screen"),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress & User Info
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.75f)
                            .background(WarmCoral)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WarmCoral),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(story.userName.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(story.userName, color = Color.White, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("24h E2E Encrypted • Depleting", color = SoftTeal, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Download Status Media to Internal/Public Local Storage
                        IconButton(
                            onClick = {
                                try {
                                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val statusFolder = File(downloadsDir, "KramaStatus")
                                    if (!statusFolder.exists()) statusFolder.mkdirs()

                                    val isVideo = story.mediaUrl.endsWith(".mp4", ignoreCase = true) || story.mediaUrl.endsWith(".webm", ignoreCase = true)
                                    val fileName = "status_${story.userName}_${System.currentTimeMillis()}${if (isVideo) ".mp4" else ".jpg"}"
                                    val outputFile = File(statusFolder, fileName)

                                    if (story.mediaUrl.isNotEmpty() && (story.mediaUrl.startsWith("/") || story.mediaUrl.endsWith(".kramae2e"))) {
                                        val bitmap = EncryptedMediaManager.decryptMediaToBitmap(story.mediaUrl)
                                        if (bitmap != null) {
                                            FileOutputStream(outputFile).use { fos ->
                                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                                            }
                                        } else {
                                            val srcFile = File(story.mediaUrl)
                                            if (srcFile.exists()) srcFile.copyTo(outputFile, overwrite = true)
                                        }
                                    } else {
                                        // Save status text image or file export
                                        FileOutputStream(outputFile).use { fos ->
                                            val textExport = "Krama Status Story Export\nUser: ${story.userName}\nContent: ${story.contentText}"
                                            fos.write(textExport.toByteArray())
                                        }
                                    }

                                    MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null) { path, uri ->
                                        android.util.Log.i("StatusViewer", "MediaScanner registered status media $path")
                                    }

                                    Toast.makeText(context, "Downloaded status to Downloads/KramaStatus/${outputFile.name}", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to download status media: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("download_status_media_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download Status", tint = SoftTeal)
                        }

                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }
            }

            // Story Text Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.contentText.ifEmpty { "📷 Encrypted Status Photo" },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )
            }

            // Bottom Views & Screenshot Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("14 Encrypted Views", color = Color.White, fontSize = 12.sp)
                }

                if (story.screenshotTaken) {
                    Text("⚠️ Screenshot Detected", color = WarmCoral, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("🔒 Server-side 24h Purge Active", color = SoftTeal, fontSize = 12.sp)
                }
            }
        }
    }
}
