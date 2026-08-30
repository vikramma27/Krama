package com.example.data.remote

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

data class PixEdgeUploadResponse(
    val id: String,
    val url: String,
    val filename: String,
    val size: Long,
    val contentType: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class PixEdgeFileInfo(
    val id: String,
    val filename: String,
    val url: String,
    val size: Long,
    val contentType: String,
    val createdAt: Long,
    val createdBy: String? = null
)

data class PixEdgeDeleteResponse(
    val success: Boolean,
    val message: String
)

data class PixEdgeErrorResponse(
    val error: String,
    val code: Int
)

sealed class PixEdgeResult<out T> {
    data class Success<T>(val data: T) : PixEdgeResult<T>()
    data class Error(val message: String, val code: Int = -1) : PixEdgeResult<Nothing>()
}

class PixEdgeApiClient private constructor() {

    private var baseUrl: String = ""
    private var apiKey: String = ""

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun configure(url: String, key: String) {
        this.baseUrl = url.trimEnd('/')
        this.apiKey = key
        Log.d(TAG, "PixEdge configured with base URL: $baseUrl")
    }

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank()

    fun getConfiguredUrl(): String = baseUrl

    suspend fun uploadFile(
        context: Context,
        file: File,
        progressCallback: ((Float) -> Unit)? = null
    ): PixEdgeResult<PixEdgeUploadResponse> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext PixEdgeResult.Error("PixEdge not configured. Please set PIXEDGE_API_URL and PIXEDGE_API_KEY", -1)
        }

        try {
            val mimeType = getMimeType(file)
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    file.name,
                    file.asRequestBody(mimeType.toMediaType())
                )
                .addFormDataPart("filename", file.name)
                .addFormDataPart("contentType", mimeType)
                .addFormDataPart("size", file.length().toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/upload")
                .post(requestBody)
                .addHeader("X-API-Key", apiKey)
                .addHeader("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(PixEdgeUploadResponse::class.java)
                    val uploadResponse = adapter.fromJson(responseBody)
                    if (uploadResponse != null) {
                        Log.d(TAG, "Upload successful: ${uploadResponse.id}")
                        PixEdgeResult.Success(uploadResponse)
                    } else {
                        PixEdgeResult.Error("Failed to parse upload response", response.code)
                    }
                } else {
                    val errorMsg = parseErrorMessage(responseBody, response.code)
                    Log.e(TAG, "Upload failed: $errorMsg (code: ${response.code})")
                    PixEdgeResult.Error(errorMsg, response.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            PixEdgeResult.Error(e.message ?: "Unknown upload error", -1)
        }
    }

    suspend fun uploadFileWithBytes(
        context: Context,
        data: ByteArray,
        filename: String,
        mimeType: String,
        progressCallback: ((Float) -> Unit)? = null
    ): PixEdgeResult<PixEdgeUploadResponse> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext PixEdgeResult.Error("PixEdge not configured", -1)
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    filename,
                    data.toRequestBody(mimeType.toMediaType())
                )
                .addFormDataPart("filename", filename)
                .addFormDataPart("contentType", mimeType)
                .addFormDataPart("size", data.size.toString())
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/upload")
                .post(requestBody)
                .addHeader("X-API-Key", apiKey)
                .addHeader("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(PixEdgeUploadResponse::class.java)
                    val uploadResponse = adapter.fromJson(responseBody)
                    if (uploadResponse != null) {
                        PixEdgeResult.Success(uploadResponse)
                    } else {
                        PixEdgeResult.Error("Failed to parse upload response", response.code)
                    }
                } else {
                    PixEdgeResult.Error(parseErrorMessage(responseBody, response.code), response.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload bytes exception: ${e.message}", e)
            PixEdgeResult.Error(e.message ?: "Unknown upload error", -1)
        }
    }

    suspend fun getFileInfo(fileId: String): PixEdgeResult<PixEdgeFileInfo> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext PixEdgeResult.Error("PixEdge not configured", -1)
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/info/$fileId")
                .get()
                .addHeader("X-API-Key", apiKey)
                .addHeader("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(PixEdgeFileInfo::class.java)
                    val fileInfo = adapter.fromJson(responseBody)
                    if (fileInfo != null) {
                        PixEdgeResult.Success(fileInfo)
                    } else {
                        PixEdgeResult.Error("Failed to parse file info response", response.code)
                    }
                } else {
                    PixEdgeResult.Error(parseErrorMessage(responseBody, response.code), response.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get file info exception: ${e.message}", e)
            PixEdgeResult.Error(e.message ?: "Unknown error", -1)
        }
    }

    suspend fun deleteFile(fileId: String): PixEdgeResult<PixEdgeDeleteResponse> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext PixEdgeResult.Error("PixEdge not configured", -1)
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/delete/$fileId")
                .delete()
                .addHeader("X-API-Key", apiKey)
                .addHeader("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val adapter = moshi.adapter(PixEdgeDeleteResponse::class.java)
                    val deleteResponse = adapter.fromJson(responseBody)
                    if (deleteResponse != null) {
                        PixEdgeResult.Success(deleteResponse)
                    } else {
                        PixEdgeResult.Error("Failed to parse delete response", response.code)
                    }
                } else {
                    PixEdgeResult.Error(parseErrorMessage(responseBody, response.code), response.code)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete file exception: ${e.message}", e)
            PixEdgeResult.Error(e.message ?: "Unknown error", -1)
        }
    }

    suspend fun checkHealth(): PixEdgeResult<Boolean> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext PixEdgeResult.Error("PixEdge not configured", -1)
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/health")
                .get()
                .addHeader("X-API-Key", apiKey)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    PixEdgeResult.Success(true)
                } else {
                    PixEdgeResult.Error("Health check failed", response.code)
                }
            }
        } catch (e: Exception) {
            PixEdgeResult.Error(e.message ?: "Health check error", -1)
        }
    }

    private fun parseErrorMessage(responseBody: String, code: Int): String {
        return try {
            val adapter = moshi.adapter(PixEdgeErrorResponse::class.java)
            val errorResponse = adapter.fromJson(responseBody)
            errorResponse?.error ?: "Upload failed with code: $code"
        } catch (e: Exception) {
            if (responseBody.isNotBlank()) {
                responseBody.take(200)
            } else {
                "Upload failed with code: $code"
            }
        }
    }

    private fun getMimeType(file: File): String {
        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "heic", "heif" -> "image/heic"
            "mp4" -> "video/mp4"
            "3gp" -> "video/3gpp"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    companion object {
        private const val TAG = "PixEdgeApiClient"
        val instance: PixEdgeApiClient by lazy { PixEdgeApiClient() }
    }
}
