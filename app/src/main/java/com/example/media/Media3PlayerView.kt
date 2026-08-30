package com.example.media

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Compose wrapper for Media3 ExoPlayer PlayerView for video rendering in chat threads.
 */
@Composable
fun Media3VideoPlayerView(
    videoUrlOrPath: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    useController: Boolean = true
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoUrlOrPath) {
        ExoPlayer.Builder(context).build().apply {
            val mediaUri = if (videoUrlOrPath.startsWith("http://") || videoUrlOrPath.startsWith("https://") || videoUrlOrPath.startsWith("content://")) {
                Uri.parse(videoUrlOrPath)
            } else {
                Uri.fromFile(java.io.File(videoUrlOrPath))
            }
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = autoPlay
        }
    }

    DisposableEffect(videoUrlOrPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                setUseController(useController)
            }
        },
        update = { view ->
            view.player = exoPlayer
        },
        modifier = modifier.fillMaxSize()
    )
}
