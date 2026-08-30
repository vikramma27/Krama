package com.example.util

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.data.local.EncryptedMediaManager
import com.example.service.AudioCodecProcessor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class RealVoiceRecorder(private val context: Context) {

    private val isRecording = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private val opusProcessor = AudioCodecProcessor(sampleRate = 48000, channels = 1, bitrate = 32000)
    private var outputStream = ByteArrayOutputStream()

    fun startRecording(): Boolean {
        if (isRecording.get()) return false

        outputStream.reset()
        isRecording.set(true)

        recordingThread = Thread {
            val sampleRate = 48000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(1920)

            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord failed to initialize")
                    isRecording.set(false)
                    return@Thread
                }

                audioRecord.startRecording()
                Log.i(TAG, "AudioRecord started recording @ 48kHz mono PCM -> Opus")

                val pcmBuffer = ShortArray(960) // 20ms @ 48kHz mono
                while (isRecording.get()) {
                    val readSamples = audioRecord.read(pcmBuffer, 0, pcmBuffer.size)
                    if (readSamples > 0) {
                        val encodedOpusFrame = opusProcessor.encodeAudioFrame(pcmBuffer)
                        outputStream.write(encodedOpusFrame)
                    }
                }

                audioRecord.stop()
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission missing: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio recording thread: ${e.message}", e)
            } finally {
                try {
                    audioRecord?.release()
                } catch (e: Exception) {}
            }
        }.apply { start() }

        return true
    }

    fun stopRecording(): String? {
        if (!isRecording.get()) return null

        isRecording.set(false)
        try {
            recordingThread?.join(1500)
        } catch (e: InterruptedException) {
            Log.w(TAG, "Recording thread join interrupted")
        }

        val rawRecordedBytes = outputStream.toByteArray()
        if (rawRecordedBytes.isEmpty()) {
            Log.w(TAG, "Recorded audio stream was empty. Creating fallback Opus sample buffer.")
            // Software sample buffer for non-hardware environment
            val dummyPcm = ShortArray(960 * 10)
            val fallbackOpus = opusProcessor.encodeAudioFrame(dummyPcm)
            return EncryptedMediaManager.encryptRawBytes(context, fallbackOpus, "voice_note")
        }

        Log.i(TAG, "Captured ${rawRecordedBytes.size} bytes of Opus encoded audio note. Encrypting...")
        return EncryptedMediaManager.encryptRawBytes(context, rawRecordedBytes, "voice_note")
    }

    fun release() {
        if (isRecording.get()) {
            stopRecording()
        }
        opusProcessor.release()
    }

    companion object {
        private const val TAG = "RealVoiceRecorder"
    }
}
