package com.example.media

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.service.KramaNotificationChannelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class UploadStatus {
    IDLE,
    UPLOADING,
    PAUSED_NO_NETWORK,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class MediaUploadTask(
    val taskId: String,
    val fileName: String,
    val mediaType: String, // "CHAT_ATTACHMENT", "STATUS_STORY", "VOICE_NOTE"
    val fileSizeBytes: Long,
    val uploadedBytes: Long = 0L,
    val progressPercent: Float = 0f,
    val speedKbps: Float = 0f,
    val status: UploadStatus = UploadStatus.IDLE,
    val errorMessage: String? = null
)

/**
 * Manages media file uploads for status stories, chat attachments, and voice notes.
 * Provides system notifications, real-time progress state, and edge-case handling
 * (network dropouts, cancellation, retry, chunking, large file validation).
 */
class MediaUploadManager private constructor() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val activeTasks = mutableMapOf<String, Job>()

    private val _uploadTasks = MutableStateFlow<List<MediaUploadTask>>(emptyList())
    val uploadTasks: StateFlow<List<MediaUploadTask>> = _uploadTasks.asStateFlow()

    fun startUpload(
        context: Context,
        taskId: String,
        fileName: String,
        mediaType: String,
        fileSizeBytes: Long,
        simulatedDurationMs: Long = 4000L
    ) {
        val appContext = context.applicationContext

        // Edge case: empty or invalid file size
        if (fileSizeBytes <= 0) {
            val failedTask = MediaUploadTask(
                taskId = taskId,
                fileName = fileName,
                mediaType = mediaType,
                fileSizeBytes = fileSizeBytes,
                status = UploadStatus.FAILED,
                errorMessage = "Invalid or empty media file."
            )
            updateTaskInState(failedTask)
            showNotification(appContext, failedTask)
            return
        }

        // Edge case: File exceeds maximum allowed E2EE single upload threshold (100 MB)
        if (fileSizeBytes > 100 * 1024 * 1024) {
            val failedTask = MediaUploadTask(
                taskId = taskId,
                fileName = fileName,
                mediaType = mediaType,
                fileSizeBytes = fileSizeBytes,
                status = UploadStatus.FAILED,
                errorMessage = "File size exceeds 100MB limit for E2EE transit."
            )
            updateTaskInState(failedTask)
            showNotification(appContext, failedTask)
            return
        }

        val initialTask = MediaUploadTask(
            taskId = taskId,
            fileName = fileName,
            mediaType = mediaType,
            fileSizeBytes = fileSizeBytes,
            status = UploadStatus.UPLOADING
        )

        updateTaskInState(initialTask)
        showNotification(appContext, initialTask)

        // Launch resilient chunk upload loop
        val uploadJob = scope.launch {
            val totalSteps = 20
            val stepDelay = maxOf(100L, simulatedDurationMs / totalSteps)
            val stepBytes = fileSizeBytes / totalSteps

            var currentUploaded = 0L

            for (step in 1..totalSteps) {
                // Check if cancelled
                val currentTask = getTask(taskId) ?: break
                if (currentTask.status == UploadStatus.CANCELLED) {
                    cancelNotification(appContext, taskId)
                    break
                }

                // Simulate network connectivity check
                if (!isNetworkAvailable(appContext)) {
                    val pausedTask = currentTask.copy(
                        status = UploadStatus.PAUSED_NO_NETWORK,
                        errorMessage = "Network connection lost. Waiting to auto-resume..."
                    )
                    updateTaskInState(pausedTask)
                    showNotification(appContext, pausedTask)

                    // Wait for network reconnection simulation
                    delay(3000)
                    if (!isNetworkAvailable(appContext)) {
                        val failedTask = currentTask.copy(
                            status = UploadStatus.FAILED,
                            errorMessage = "Upload timed out due to unstable network."
                        )
                        updateTaskInState(failedTask)
                        showNotification(appContext, failedTask)
                        break
                    }
                }

                delay(stepDelay)

                currentUploaded += stepBytes
                val progress = (currentUploaded.toFloat() / fileSizeBytes.toFloat()).coerceIn(0f, 1f)
                val speed = (stepBytes / 1024f) / (stepDelay / 1000f)

                val updatedTask = currentTask.copy(
                    uploadedBytes = currentUploaded,
                    progressPercent = progress,
                    speedKbps = speed,
                    status = if (progress >= 1f) UploadStatus.COMPLETED else UploadStatus.UPLOADING
                )

                updateTaskInState(updatedTask)
                showNotification(appContext, updatedTask)
            }

            // Cleanup completed/failed task notification after delay
            val finalTask = getTask(taskId)
            if (finalTask?.status == UploadStatus.COMPLETED) {
                delay(2500)
                cancelNotification(appContext, taskId)
                removeTaskFromState(taskId)
            }
        }

        activeTasks[taskId] = uploadJob
    }

    fun cancelUpload(context: Context, taskId: String) {
        activeTasks[taskId]?.cancel()
        activeTasks.remove(taskId)

        val task = getTask(taskId) ?: return
        val cancelled = task.copy(status = UploadStatus.CANCELLED)
        updateTaskInState(cancelled)

        cancelNotification(context, taskId)
        scope.launch {
            delay(1000)
            removeTaskFromState(taskId)
        }
    }

    fun retryUpload(context: Context, taskId: String) {
        val task = getTask(taskId) ?: return
        startUpload(
            context = context,
            taskId = task.taskId,
            fileName = task.fileName,
            mediaType = task.mediaType,
            fileSizeBytes = task.fileSizeBytes
        )
    }

    private fun updateTaskInState(task: MediaUploadTask) {
        val currentList = _uploadTasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.taskId == task.taskId }
        if (index != -1) {
            currentList[index] = task
        } else {
            currentList.add(task)
        }
        _uploadTasks.value = currentList
    }

    private fun removeTaskFromState(taskId: String) {
        _uploadTasks.value = _uploadTasks.value.filter { it.taskId != taskId }
    }

    private fun getTask(taskId: String): MediaUploadTask? {
        return _uploadTasks.value.find { it.taskId == taskId }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNet = cm?.activeNetworkInfo
            activeNet != null && activeNet.isConnected
        } catch (e: Exception) {
            true
        }
    }

    private fun showNotification(context: Context, task: MediaUploadTask) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = task.taskId.hashCode()

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val percentInt = (task.progressPercent * 100).toInt()
            val speedMb = task.speedKbps / 1024f

            val title = when (task.mediaType) {
                "STATUS_STORY" -> "Uploading Status Story"
                "VOICE_NOTE" -> "Sending Voice Note"
                else -> "Uploading Attachment"
            }

            val body = when (task.status) {
                UploadStatus.UPLOADING -> "${task.fileName} • $percentInt% (${String.format("%.1f", speedMb)} MB/s)"
                UploadStatus.PAUSED_NO_NETWORK -> "Network offline • Upload paused"
                UploadStatus.COMPLETED -> "Upload completed successfully"
                UploadStatus.FAILED -> task.errorMessage ?: "Upload failed"
                UploadStatus.CANCELLED -> "Upload cancelled"
                else -> task.fileName
            }

            val builder = NotificationCompat.Builder(context, KramaNotificationChannelManager.CHANNEL_MEDIA_UPLOADS)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(pendingIntent)
                .setOngoing(task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK)
                .setOnlyAlertOnce(true)

            if (task.status == UploadStatus.UPLOADING || task.status == UploadStatus.PAUSED_NO_NETWORK) {
                builder.setProgress(100, percentInt, task.status == UploadStatus.PAUSED_NO_NETWORK)
            } else if (task.status == UploadStatus.COMPLETED) {
                builder.setProgress(0, 0, false)
                builder.setSmallIcon(android.R.drawable.stat_sys_upload_done)
            }

            manager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.w("MediaUploadManager", "Notification exception: ${e.message}")
        }
    }

    private fun cancelNotification(context: Context, taskId: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(taskId.hashCode())
        } catch (e: Exception) {}
    }

    fun getPendingQueueSize(): Int {
        return _uploadTasks.value.count { it.status == UploadStatus.IDLE || it.status == UploadStatus.PAUSED_NO_NETWORK || it.status == UploadStatus.FAILED }
    }

    fun processPendingUploads(context: Context = com.example.KramaApplication.instance) {
        val pending = _uploadTasks.value.filter { it.status == UploadStatus.IDLE || it.status == UploadStatus.PAUSED_NO_NETWORK || it.status == UploadStatus.FAILED }
        for (task in pending) {
            retryUpload(context, task.taskId)
        }
    }

    companion object {
        val instance: MediaUploadManager by lazy { MediaUploadManager() }
    }
}
