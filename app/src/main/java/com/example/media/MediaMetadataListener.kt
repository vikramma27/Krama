package com.example.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncedTrackMetadata(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

class MediaMetadataListener(private val context: Context) {

    private val TAG = "MediaMetadataListener"

    private val _currentTrack = MutableStateFlow(SyncedTrackMetadata())
    val currentTrack: StateFlow<SyncedTrackMetadata> = _currentTrack.asStateFlow()

    private var sessionManager: MediaSessionManager? = null
    private var activeControllers = mutableListOf<MediaController>()

    fun startListening() {
        try {
            sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(context, MediaMetadataListener::class.java)

            val sessions = try {
                sessionManager?.getActiveSessions(componentName)
            } catch (e: SecurityException) {
                Log.w(TAG, "Notification listener permission needed for full MediaSessionManager access; fallback mode active.")
                emptyList<MediaController>()
            } catch (e: Throwable) {
                Log.w(TAG, "MediaSessionManager query error: ${e.message}")
                emptyList<MediaController>()
            }

            if (!sessions.isNullOrEmpty()) {
                activeControllers.clear()
                activeControllers.addAll(sessions)
                sessions.firstOrNull()?.let { controller ->
                    attachControllerCallback(controller)
                }
            } else {
                Log.i(TAG, "No active system media sessions found currently. Listening for new playback events.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error starting MediaMetadataListener: ${e.message}")
        }
    }

    private fun attachControllerCallback(controller: MediaController) {
        try {
            val metadata = controller.metadata
            val playbackState = controller.playbackState

            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Track"
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: "Music"
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
            val position = playbackState?.position ?: 0L
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

            val track = SyncedTrackMetadata(
                title = title,
                artist = artist,
                album = album,
                isPlaying = isPlaying,
                positionMs = position,
                durationMs = duration,
                timestamp = System.currentTimeMillis()
            )

            _currentTrack.value = track
            syncTrackToFirebase(track)

            controller.registerCallback(object : MediaController.Callback() {
                override fun onMetadataChanged(meta: MediaMetadata?) {
                    val updatedTitle = meta?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: title
                    val updatedArtist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: artist
                    val updatedAlbum = meta?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: album
                    val updatedDur = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: duration

                    val updated = _currentTrack.value.copy(
                        title = updatedTitle,
                        artist = updatedArtist,
                        album = updatedAlbum,
                        durationMs = updatedDur,
                        timestamp = System.currentTimeMillis()
                    )
                    _currentTrack.value = updated
                    syncTrackToFirebase(updated)
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    val playing = state?.state == PlaybackState.STATE_PLAYING
                    val pos = state?.position ?: 0L

                    val updated = _currentTrack.value.copy(
                        isPlaying = playing,
                        positionMs = pos,
                        timestamp = System.currentTimeMillis()
                    )
                    _currentTrack.value = updated
                    syncTrackToFirebase(updated)
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Error attaching MediaController callback: ${e.message}")
        }
    }

    fun syncTrackToFirebase(track: SyncedTrackMetadata) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) return
            val currentUser = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val userId = currentUser?.uid ?: "local_krama_user"
            val database = try { FirebaseDatabase.getInstance() } catch (e: Throwable) { null } ?: return

            val nowPlayingRef = database.getReference("now_playing/$userId")
            nowPlayingRef.setValue(
                mapOf(
                    "title" to track.title,
                    "artist" to track.artist,
                    "album" to track.album,
                    "isPlaying" to track.isPlaying,
                    "positionMs" to track.positionMs,
                    "durationMs" to track.durationMs,
                    "timestamp" to track.timestamp
                )
            )
            Log.i(TAG, "Synced track metadata to Firebase Realtime DB: ${track.title} - ${track.artist}")
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase track sync handled safely: ${e.message}")
        }
    }
}
