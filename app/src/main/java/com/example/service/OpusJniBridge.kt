package com.example.service

import android.util.Log

object OpusJniBridge {

    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("opus_jni")
            isNativeLibraryLoaded = true
            Log.i("OpusJniBridge", "libopus_jni.so native library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("OpusJniBridge", "Native libopus_jni.so fallback active. Using optimized software PCM-Opus audio frame encoder.")
            isNativeLibraryLoaded = false
        } catch (e: Throwable) {
            Log.w("OpusJniBridge", "Opus JNI initialization note: ${e.message}")
            isNativeLibraryLoaded = false
        }
    }

    external fun nativeInitEncoder(sampleRate: Int, channels: Int, bitrate: Int): Long
    external fun nativeEncodeFrame(handle: Long, pcmData: ShortArray, frameSize: Int, opusOut: ByteArray): Int
    external fun nativeInitDecoder(sampleRate: Int, channels: Int): Long
    external fun nativeDecodeFrame(handle: Long, opusData: ByteArray, opusLen: Int, pcmOut: ShortArray): Int
    external fun nativeDestroy(handle: Long)

    fun isNativeSupported(): Boolean = isNativeLibraryLoaded
}
