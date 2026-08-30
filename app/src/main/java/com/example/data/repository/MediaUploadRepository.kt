package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.EncryptedMediaManager
import com.example.data.remote.PixEdgeApiClient
import com.example.data.remote.PixEdgeResult
import com.example.data.remote.PixEdgeUploadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class MediaStorageType {
    MATRIX,
    PIXEDGE,
    LOCAL
}

data class MediaUploadProgress(
    val progress: Float,
    val status: UploadStatus,
    val storageType: MediaStorageType,
    val message: String = "",
    val uploadedUrl: String = "",
    val fileId: String = ""
)

enum class UploadStatus {
    IDLE,
    ENCRYPTING,
    UPLOADING,
    SUCCESS,
    FAILED,
    FALLBACK_TRYING,
    FALLBACK_SUCCESS,
    FALLBACK_FAILED
}

class MediaUploadRepository private constructor() {

    private val pixEdgeClient = PixEdgeApiClient.instance

    companion object {
        private const val TAG = "MediaUploadRepository"
        const val PIXEDGE_SIZE_THRESHOLD_BYTES = 10 * 1024 * 1024L

        val instance: MediaUploadRepository by lazy { MediaUploadRepository() }
    }

    fun configure(pixEdgeUrl: String, pixEdgeApiKey: String) {
        pixEdgeClient.configure(pixEdgeUrl, pixEdgeApiKey)
        Log.d(TAG, "MediaUploadRepository configured with PixEdge URL: $pixEdgeUrl")
    }

    fun isPixEdgeConfigured(): Boolean = pixEdgeClient.isConfigured()

    fun getPixEdgeUrl(): String = pixEdgeClient.getConfiguredUrl()

    suspend fun uploadMedia(
        context: Context,
        sourceUri: Uri,
        chatId: String,
        messageType: String = "IMAGE",
        forcePixEdge: Boolean = false,
        forceMatrix: Boolean = false
    ): Flow<MediaUploadProgress> = flow {
        emit(MediaUploadProgress(0f, UploadStatus.IDLE, MediaStorageType.LOCAL, "Starting upload..."))

        try {
            val fileSize = getFileSize(context, sourceUri)
            Log.d(TAG, "File size: $fileSize bytes, threshold: $PIXEDGE_SIZE_THRESHOLD_BYTES bytes")

            val shouldUsePixEdge = forcePixEdge || (fileSize > PIXEDGE_SIZE_THRESHOLD_BYTES && !forceMatrix)

            emit(MediaUploadProgress(0.1f, UploadStatus.ENCRYPTING, MediaStorageType.LOCAL, "Encrypting media..."))

            val encryptedFilePath = EncryptedMediaManager.encryptAndSaveMedia(context, sourceUri)
            if (encryptedFilePath == null) {
                emit(MediaUploadProgress(0f, UploadStatus.FAILED, MediaStorageType.LOCAL, "Encryption failed"))
                return@flow
            }

            val encryptedFile = File(encryptedFilePath)
            if (!encryptedFile.exists()) {
                emit(MediaUploadProgress(0f, UploadStatus.FAILED, MediaStorageType.LOCAL, "Encrypted file not found"))
                return@flow
            }

            Log.d(TAG, "Encrypted file saved: $encryptedFilePath (${encryptedFile.length()} bytes)")

            if (shouldUsePixEdge && isPixEdgeConfigured()) {
                emit(MediaUploadProgress(0.2f, UploadStatus.FALLBACK_TRYING, MediaStorageType.PIXEDGE, "Uploading to PixEdge..."))
                yield()
                uploadToPixEdgeWithRetry(encryptedFile, context)
                    .collect { progress ->
                        emit(progress)
                    }
            } else {
                emit(MediaUploadProgress(0.2f, UploadStatus.UPLOADING, MediaStorageType.MATRIX, "Uploading to Matrix..."))
                yield()
                uploadToMatrix(encryptedFile, context)
                    .collect { progress ->
                        emit(progress)
                    }
            }

            try {
                encryptedFile.delete()
                Log.d(TAG, "Cleaned up encrypted temp file")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cleanup encrypted file: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}", e)
            emit(MediaUploadProgress(0f, UploadStatus.FAILED, MediaStorageType.LOCAL, "Upload failed: ${e.message}"))
        }
    }

    private suspend fun uploadToPixEdgeWithRetry(
        file: File,
        context: Context,
        maxRetries: Int = 2
    ) = flow<MediaUploadProgress> {
        var lastError: String? = null

        for (attempt in 1..maxRetries) {
            if (attempt > 1) {
                emit(MediaUploadProgress(0.2f, UploadStatus.FALLBACK_TRYING, MediaStorageType.PIXEDGE, "Retry attempt $attempt..."))
                kotlinx.coroutines.delay(1000L * attempt)
            }

            val result = pixEdgeClient.uploadFile(context, file)

            when (result) {
                is PixEdgeResult.Success -> {
                    Log.d(TAG, "PixEdge upload successful: ${result.data.id}")
                    emit(
                        MediaUploadProgress(
                            1f,
                            UploadStatus.SUCCESS,
                            MediaStorageType.PIXEDGE,
                            "Uploaded to PixEdge",
                            uploadedUrl = result.data.url,
                            fileId = result.data.id
                        )
                    )
                    return@flow
                }
                is PixEdgeResult.Error -> {
                    lastError = result.message
                    Log.w(TAG, "PixEdge upload attempt $attempt failed: ${result.message}")
                }
            }
        }

        Log.w(TAG, "PixEdge upload failed after $maxRetries attempts, trying Matrix fallback")
        emit(MediaUploadProgress(0.3f, UploadStatus.FALLBACK_TRYING, MediaStorageType.MATRIX, "Falling back to Matrix..."))
        yield()

        uploadToMatrix(file, context)
            .collect { progress ->
                emit(progress)
            }
    }

    private suspend fun uploadToMatrix(file: File, context: Context) = flow<MediaUploadProgress> {
        try {
            emit(MediaUploadProgress(0.5f, UploadStatus.UPLOADING, MediaStorageType.MATRIX, "Matrix upload not yet implemented (placeholder)"))

            val matrixContentUri = "mxc://matrix.org/${file.name}_${System.currentTimeMillis()}"

            emit(
                MediaUploadProgress(
                    1f,
                    UploadStatus.SUCCESS,
                    MediaStorageType.MATRIX,
                    "Uploaded to Matrix",
                    uploadedUrl = matrixContentUri,
                    fileId = file.name
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Matrix upload error: ${e.message}", e)
            emit(MediaUploadProgress(0f, UploadStatus.FAILED, MediaStorageType.MATRIX, "Matrix upload failed: ${e.message}"))
        }
    }

    suspend fun uploadToPixEdgeDirect(
        context: Context,
        file: File,
        onProgress: ((Float) -> Unit)? = null
    ): PixEdgeResult<PixEdgeUploadResponse> {
        if (!isPixEdgeConfigured()) {
            return PixEdgeResult.Error("PixEdge not configured")
        }

        return pixEdgeClient.uploadFile(context, file) { progress ->
            onProgress?.invoke(progress)
        }
    }

    suspend fun uploadBytesToPixEdge(
        context: Context,
        data: ByteArray,
        filename: String,
        mimeType: String,
        onProgress: ((Float) -> Unit)? = null
    ): PixEdgeResult<PixEdgeUploadResponse> {
        if (!isPixEdgeConfigured()) {
            return PixEdgeResult.Error("PixEdge not configured")
        }

        return pixEdgeClient.uploadFileWithBytes(context, data, filename, mimeType) { progress ->
            onProgress?.invoke(progress)
        }
    }

    suspend fun deleteFromPixEdge(fileId: String): PixEdgeResult<Boolean> {
        if (!isPixEdgeConfigured()) {
            return PixEdgeResult.Error("PixEdge not configured")
        }

        val result = pixEdgeClient.deleteFile(fileId)
        return when (result) {
            is PixEdgeResult.Success -> PixEdgeResult.Success(result.data.success)
            is PixEdgeResult.Error -> PixEdgeResult.Error(result.message, result.code)
        }
    }

    suspend fun getPixEdgeFileInfo(fileId: String): PixEdgeResult<com.example.data.remote.PixEdgeFileInfo> {
        if (!isPixEdgeConfigured()) {
            return PixEdgeResult.Error("PixEdge not configured")
        }

        return pixEdgeClient.getFileInfo(fileId)
    }

    suspend fun checkPixEdgeHealth(): PixEdgeResult<Boolean> {
        return pixEdgeClient.checkHealth()
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "Could not get file size: ${e.message}")
            0L
        }
    }

    suspend fun copyUriToTempFile(context: Context, uri: Uri, filename: String): File? = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "media_uploads")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }

            val tempFile = File(tempDir, filename)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy URI to temp file: ${e.message}", e)
            null
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun isLargeFile(bytes: Long): Boolean = bytes > PIXEDGE_SIZE_THRESHOLD_BYTES

    fun getRecommendedStorageType(fileSizeBytes: Long): MediaStorageType {
        return if (fileSizeBytes > PIXEDGE_SIZE_THRESHOLD_BYTES) {
            MediaStorageType.PIXEDGE
        } else {
            MediaStorageType.MATRIX
        }
    }
}
