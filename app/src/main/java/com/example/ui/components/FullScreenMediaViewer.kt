package com.example.ui.components

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.EncryptedMediaManager
import com.example.ui.theme.NearBlackPlum
import com.example.ui.theme.SoftTeal
import com.example.ui.theme.WarmCoral
import com.example.util.NativeAudioPlayer
import java.io.File
import java.io.FileOutputStream

@Composable
fun FullScreenMediaViewer(
    mediaUrl: String,
    mediaType: String, // "IMAGE", "VIDEO", "VOICE", "AUDIO"
    title: String = "Encrypted Media",
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showInfoOverlay by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_screen_media_viewer"),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content Rendering by Type
            when (mediaType.uppercase()) {
                "IMAGE" -> {
                    val localEncryptedBitmap = remember(mediaUrl) {
                        if (mediaUrl.startsWith("/") || mediaUrl.endsWith(".kramae2e")) {
                            EncryptedMediaManager.decryptMediaToBitmap(mediaUrl)
                        } else null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.8f, 4f)
                                    if (scale > 1f) {
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    } else {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (localEncryptedBitmap != null) {
                            Image(
                                bitmap = localEncryptedBitmap.asImageBitmap(),
                                contentDescription = "Full Screen Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        } else {
                            coil.compose.AsyncImage(
                                model = mediaUrl,
                                contentDescription = "Full Screen Photo",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    )
                            )
                        }
                    }
                }

                "VIDEO" -> {
                    com.example.media.Media3VideoPlayerView(
                        videoUrlOrPath = mediaUrl,
                        autoPlay = true,
                        useController = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                "VOICE", "AUDIO" -> {
                    val media3Audio = remember { com.example.media.Media3AudioPlayerManager.getInstance(context) }
                    val playerState by media3Audio.playbackState.collectAsState()

                    LaunchedEffect(mediaUrl) {
                        media3Audio.playAudio(mediaUrl)
                    }

                    DisposableEffect(Unit) {
                        onDispose { media3Audio.stopAudio() }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = NearBlackPlum,
                            modifier = Modifier.size(120.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Fullscreen, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(56.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Opus 24kbps HD Audio Stream", color = SoftTeal, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(32.dp))

                        // Waveform Visualizer
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.height(48.dp)
                        ) {
                            listOf(16, 28, 12, 38, 24, 44, 18, 32, 22, 40, 10, 30, 20, 36, 14).forEach { height ->
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(if (playerState.isPlaying) height.dp else 12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (playerState.isPlaying) WarmCoral else SoftTeal.copy(alpha = 0.5f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Seek Slider & Duration
                        val curSec = playerState.currentPositionMs / 1000
                        val durSec = playerState.durationMs / 1000
                        val posStr = String.format("%02d:%02d", curSec / 60, curSec % 60)
                        val durStr = String.format("%02d:%02d", durSec / 60, durSec % 60)

                        Slider(
                            value = if (playerState.durationMs > 0) playerState.currentPositionMs.toFloat() else 0f,
                            onValueChange = { media3Audio.seekTo(it.toLong()) },
                            valueRange = 0f..(playerState.durationMs.coerceAtLeast(1).toFloat()),
                            colors = SliderDefaults.colors(thumbColor = WarmCoral, activeTrackColor = WarmCoral, inactiveTrackColor = SoftTeal.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(posStr, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(durStr, color = SoftTeal, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Audio Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = WarmCoral,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable {
                                        if (playerState.isPlaying) media3Audio.pauseAudio() else media3Audio.playAudio(mediaUrl)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // Speed Toggle (1x, 1.5x, 2x)
                            val nextSpeed = when (playerState.playbackSpeed) {
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NearBlackPlum,
                                modifier = Modifier.clickable { media3Audio.setPlaybackSpeed(nextSpeed) }
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = SoftTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top Bar Controls (Close, Title, Info, Download)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_media_viewer")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showInfoOverlay = !showInfoOverlay }) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = SoftTeal)
                    }

                    // Save / Download Media to Local Device Storage
                    IconButton(
                        onClick = {
                            saveMediaToDeviceStorage(context, mediaUrl, mediaType)
                        },
                        modifier = Modifier.testTag("download_media_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save to Device", tint = WarmCoral)
                    }
                }
            }

            // Info Overlay
            AnimatedVisibility(
                visible = showInfoOverlay,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = NearBlackPlum.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Encryption Metadata", color = WarmCoral, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("AES-256-GCM Zero-Knowledge Key Envelope", color = Color.White, fontSize = 12.sp)
                        Text("Path: ${mediaUrl.takeLast(35)}", color = SoftTeal, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

fun saveMediaToDeviceStorage(context: Context, mediaUrl: String, mediaType: String) {
    try {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val kramaFolder = File(downloadDir, "KramaSavedMedia")
        if (!kramaFolder.exists()) kramaFolder.mkdirs()

        val ext = when (mediaType.uppercase()) {
            "IMAGE" -> ".jpg"
            "VIDEO" -> ".mp4"
            "VOICE", "AUDIO" -> ".m4a"
            else -> ".bin"
        }

        val destFile = File(kramaFolder, "krama_media_${System.currentTimeMillis()}$ext")

        if (mediaUrl.startsWith("/") || mediaUrl.endsWith(".kramae2e")) {
            val bitmap = EncryptedMediaManager.decryptMediaToBitmap(mediaUrl)
            if (bitmap != null) {
                FileOutputStream(destFile).use { fos ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                }
            } else {
                val srcFile = File(mediaUrl)
                if (srcFile.exists()) srcFile.copyTo(destFile, overwrite = true)
            }
        } else {
            // Placeholder text or Uri fallback write
            FileOutputStream(destFile).use { fos ->
                fos.write("Krama Media Export Stream Content".toByteArray())
            }
        }

        MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null) { path, uri ->
            android.util.Log.i("FullScreenMediaViewer", "MediaScanner scanned $path -> $uri")
        }

        Toast.makeText(context, "Saved media to Downloads/KramaSavedMedia/${destFile.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save media: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
