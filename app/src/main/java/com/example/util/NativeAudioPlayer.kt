package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

object NativeAudioPlayer {
    private const val TAG = "NativeAudioPlayer"

    data class PlayerState(
        val isPlaying: Boolean = false,
        val currentPositionMs: Int = 0,
        val durationMs: Int = 0,
        val currentFile: String? = null,
        val playbackSpeed: Float = 1.0f,
        val isMuted: Boolean = false
    )

    private var mediaPlayer: MediaPlayer? = null
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun playAudio(context: Context, audioPathOrUrl: String) {
        try {
            if (_playerState.value.currentFile == audioPathOrUrl && mediaPlayer != null) {
                if (_playerState.value.isPlaying) {
                    pause()
                } else {
                    resume()
                }
                return
            }

            stop()

            mediaPlayer = MediaPlayer().apply {
                if (audioPathOrUrl.startsWith("http://") || audioPathOrUrl.startsWith("https://")) {
                    setDataSource(audioPathOrUrl)
                } else {
                    val file = File(audioPathOrUrl)
                    if (file.exists()) {
                        setDataSource(file.absolutePath)
                    } else {
                        Log.w(TAG, "Audio file not found at $audioPathOrUrl, attempting URI/fallback")
                        setDataSource(audioPathOrUrl)
                    }
                }
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    applySpeedAndVolume()
                    _playerState.value = _playerState.value.copy(
                        isPlaying = true,
                        durationMs = mp.duration,
                        currentFile = audioPathOrUrl,
                        currentPositionMs = 0
                    )
                    startProgressTracker()
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stop()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer for $audioPathOrUrl: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            progressJob?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing audio: ${e.message}")
        }
    }

    fun resume() {
        try {
            mediaPlayer?.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming audio: ${e.message}")
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking audio: ${e.message}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        applySpeedAndVolume()
    }

    fun toggleMute() {
        val newMute = !_playerState.value.isMuted
        _playerState.value = _playerState.value.copy(isMuted = newMute)
        applySpeedAndVolume()
    }

    private fun applySpeedAndVolume() {
        val mp = mediaPlayer ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = mp.playbackParams ?: PlaybackParams()
                params.speed = _playerState.value.playbackSpeed
                mp.playbackParams = params
            }
            if (_playerState.value.isMuted) {
                mp.setVolume(0f, 0f)
            } else {
                mp.setVolume(1f, 1f)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying speed/volume: ${e.message}")
        }
    }

    fun stop() {
        try {
            progressJob?.cancel()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio player: ${e.message}")
        } finally {
            mediaPlayer = null
            _playerState.value = PlayerState()
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (_playerState.value.isPlaying) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = mp.currentPosition,
                            durationMs = mp.duration
                        )
                    }
                }
                delay(250)
            }
        }
    }
}
