package com.example.media

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val currentMediaUri: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isCompleted: Boolean = false
)

/**
 * Media3 ExoPlayer Audio Manager for Voice Notes and Audio Attachments.
 */
class Media3AudioPlayerManager private constructor(context: Context) {

    private val applicationContext = context.applicationContext
    private var exoPlayer: ExoPlayer? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    init {
        initExoPlayer()
    }

    private fun initExoPlayer() {
        if (exoPlayer == null) {
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SPEECH)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build()

            exoPlayer = ExoPlayer.Builder(applicationContext)
                .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) {
                            startPositionUpdates()
                        } else {
                            stopPositionUpdates()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _playbackState.value = _playbackState.value.copy(
                                    durationMs = duration.coerceAtLeast(0L)
                                )
                            }
                            Player.STATE_ENDED -> {
                                _playbackState.value = _playbackState.value.copy(
                                    isPlaying = false,
                                    isCompleted = true,
                                    currentPositionMs = _playbackState.value.durationMs
                                )
                                stopPositionUpdates()
                            }
                        }
                    }
                })
            }
        }
    }

    fun playAudio(audioUriOrPath: String) {
        val player = exoPlayer ?: return
        if (_playbackState.value.currentMediaUri == audioUriOrPath && player.playbackState != Player.STATE_ENDED) {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            return
        }

        try {
            val mediaItem = if (audioUriOrPath.startsWith("http://") || audioUriOrPath.startsWith("https://") || audioUriOrPath.startsWith("content://")) {
                MediaItem.fromUri(Uri.parse(audioUriOrPath))
            } else {
                MediaItem.fromUri(Uri.fromFile(java.io.File(audioUriOrPath)))
            }

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            _playbackState.value = AudioPlaybackState(
                currentMediaUri = audioUriOrPath,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = 0L,
                isCompleted = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Media3 ExoPlayer playAudio failed for $audioUriOrPath: ${e.message}", e)
        }
    }

    fun pauseAudio() {
        exoPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
    }

    fun stopAudio() {
        exoPlayer?.stop()
        stopPositionUpdates()
        _playbackState.value = AudioPlaybackState()
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playbackState.value = _playbackState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = player.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(100L)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    fun release() {
        stopAudio()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        private const val TAG = "Media3AudioPlayer"
        @Volatile
        private var INSTANCE: Media3AudioPlayerManager? = null

        fun getInstance(context: Context): Media3AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Media3AudioPlayerManager(context).also { INSTANCE = it }
            }
        }
    }
}
