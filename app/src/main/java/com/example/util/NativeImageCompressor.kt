package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.system.measureTimeMillis

data class CompressedImageResult(
    val processedBytes: ByteArray,
    val originalSize: Long,
    val compressedSize: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val targetWidth: Int,
    val targetHeight: Int,
    val latencyMs: Long,
    val isNativeJniUsed: Boolean
) {
    val storageSavingsPercent: Float
        get() = if (originalSize > 0) ((1.0f - (compressedSize.toFloat() / originalSize.toFloat())) * 100f).coerceIn(0f, 100f) else 0f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CompressedImageResult
        return processedBytes.contentEquals(other.processedBytes)
    }

    override fun hashCode(): Int {
        return processedBytes.contentHashCode()
    }
}

object NativeImageCompressor {
    private const val TAG = "NativeImageCompressor"
    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("krama_image_processor")
            isNativeLibraryLoaded = true
            Timber.i("[JNI TRACE] Native C++ library 'krama_image_processor' loaded into process runtime.")
        } catch (e: Throwable) {
            isNativeLibraryLoaded = false
            Timber.w(e, "[JNI TRACE] Native C++ library 'krama_image_processor' load notice: ${e.message}. Using high-speed Bitmap memory pipeline.")
        }
    }

    @JvmStatic
    private external fun compressAndResizeNative(
        inputBytes: ByteArray,
        srcWidth: Int,
        srcHeight: Int,
        maxDimension: Int,
        quality: Int
    ): ByteArray?

    /**
     * Executes pre-encryption image dynamic resizing and compression using Native C++ JNI bridge
     * (or ultra-fast native Bitmap memory buffer fallback) before local storage in encrypted files.
     */
    fun processAndCompressPhoto(
        rawBytes: ByteArray,
        maxDimension: Int = 1920,
        quality: Int = 82
    ): CompressedImageResult {
        val originalSize = rawBytes.size.toLong()
        var originalWidth = 0
        var originalHeight = 0
        var targetWidth = 0
        var targetHeight = 0
        var processedBytes = rawBytes
        var isJniUsed = false

        val latencyMs = measureTimeMillis {
            // 1. Inspect image dimensions without allocating full pixel array
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
            originalWidth = options.outWidth
            originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.w(TAG, "Unrecognized image format or empty bounds. Skipping pre-encryption compression.")
                return@measureTimeMillis
            }

            // 2. Calculate scaling ratio
            val maxSrcDim = max(originalWidth, originalHeight)
            val scaleRatio = if (maxSrcDim > maxDimension) {
                maxSrcDim.toFloat() / maxDimension.toFloat()
            } else {
                1.0f
            }

            targetWidth = (originalWidth / scaleRatio).toInt().coerceAtLeast(1)
            targetHeight = (originalHeight / scaleRatio).toInt().coerceAtLeast(1)

            // 3. Attempt Native C++ JNI compression if loaded
            if (isNativeLibraryLoaded) {
                try {
                    val nativeResult = compressAndResizeNative(
                        inputBytes = rawBytes,
                        srcWidth = originalWidth,
                        srcHeight = originalHeight,
                        maxDimension = maxDimension,
                        quality = quality
                    )
                    if (nativeResult != null && nativeResult.isNotEmpty()) {
                        processedBytes = nativeResult
                        isJniUsed = true
                        Log.i(TAG, "Native C++ JNI compression executed successfully.")
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Native C++ JNI invocation note: ${e.message}. Executing memory pipeline...")
                }
            }

            // 4. Memory-optimized Bitmap scaling pipeline if JNI output required buffer reduction
            if (!isJniUsed || processedBytes.size >= rawBytes.size) {
                var sampleSize = 1
                if (originalHeight > targetHeight || originalWidth > targetWidth) {
                    val halfHeight = originalHeight / 2
                    val halfWidth = originalWidth / 2
                    while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val decodedBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                if (decodedBitmap != null) {
                    val scaledBitmap = if (decodedBitmap.width != targetWidth || decodedBitmap.height != targetHeight) {
                        Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true)
                    } else {
                        decodedBitmap
                    }

                    ByteArrayOutputStream().use { bos ->
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                        processedBytes = bos.toByteArray()
                    }

                    if (scaledBitmap != decodedBitmap) {
                        scaledBitmap.recycle()
                    }
                    decodedBitmap.recycle()
                }
            }
        }

        val compressedSize = processedBytes.size.toLong()
        val result = CompressedImageResult(
            processedBytes = processedBytes,
            originalSize = originalSize,
            compressedSize = compressedSize,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            targetWidth = if (targetWidth > 0) targetWidth else originalWidth,
            targetHeight = if (targetHeight > 0) targetHeight else originalHeight,
            latencyMs = latencyMs,
            isNativeJniUsed = isJniUsed
        )

        Log.i(
            TAG,
            "Pre-Encryption Photo Compression complete: ${originalSize / 1024} KB -> ${compressedSize / 1024} KB " +
                    "(${String.format("%.1f", result.storageSavingsPercent)}% savings) in ${latencyMs}ms " +
                    "(${if (isJniUsed) "C++ JNI Bridge" else "Low-Latency Native Pipeline"})"
        )

        return result
    }

    /**
     * Compress photo directly from Content Uri before encryption.
     */
    fun processAndCompressPhotoFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1920,
        quality: Int = 82
    ): CompressedImageResult? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Cannot open stream for Uri: $uri")
                return null
            }
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.isEmpty()) return null

            processAndCompressPhoto(bytes, maxDimension, quality)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to compress image from Uri $uri: ${e.message}", e)
            null
        }
    }
}
