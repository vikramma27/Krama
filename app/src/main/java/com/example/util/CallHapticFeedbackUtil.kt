package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Haptic feedback utility utilizing Android VibratorManager (Android 12+) and Vibrator fallback
 * to provide distinct, recurring vibration patterns for incoming call rings and crisp action pulses
 * for calling controls (Mute, Unmute, End Call, Camera Flip, Speaker).
 */
object CallHapticFeedbackUtil {

    private const val TAG = "CallHapticFeedback"
    private var activeRingtoneVibrator: Vibrator? = null

    /**
     * Incoming Call Ring: Distinct, recurring vibration pattern (800ms vibrate, 1000ms pause)
     * repeating continuously until answered or declined.
     */
    fun vibrateIncomingCallRingtone(context: Context) {
        val vibrator = getVibrator(context) ?: return
        activeRingtoneVibrator = vibrator
        try {
            vibrator.cancel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 800, 1000)
                val amplitudes = intArrayOf(0, 255, 0)
                // repeat index 0 -> loops endlessly
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 800, 1000), 0)
            }
            Log.i(TAG, "Started recurring INCOMING_CALL ringtone vibration pattern")
        } catch (e: Throwable) {
            Log.w(TAG, "Incoming call vibration failed: ${e.message}")
        }
    }

    /**
     * Stop incoming call vibration immediately upon call accept, decline, or timeout.
     */
    fun stopIncomingCallVibration(context: Context) {
        try {
            val vibrator = activeRingtoneVibrator ?: getVibrator(context)
            vibrator?.cancel()
            activeRingtoneVibrator = null
            Log.i(TAG, "Stopped incoming call ringtone vibration pattern")
        } catch (e: Throwable) {
            Log.w(TAG, "Error stopping incoming call vibration: ${e.message}")
        }
    }

    /**
     * Short action pulse confirmation for UI buttons (Mute, Unmute, Camera Flip, Speaker).
     */
    fun vibrateShortActionPulse(context: Context) {
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Action pulse vibration failed: ${e.message}")
        }
    }

    /**
     * End Call: Strong double pulse pattern confirming call session termination.
     */
    fun vibrateEndCall(context: Context) {
        stopIncomingCallVibration(context)
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 120, 80, 120)
                val amplitudes = intArrayOf(0, 255, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 120, 80, 120), -1)
            }
        } catch (e: Throwable) {}
    }

    /**
     * Connection Established: Double subtle pulse pattern confirming line connection.
     */
    fun vibrateConnectionEstablished(context: Context) {
        stopIncomingCallVibration(context)
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 60, 80, 100)
                val amplitudes = intArrayOf(0, 160, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 60, 80, 100), -1)
            }
            Log.i(TAG, "Triggered CONNECTION_ESTABLISHED haptic feedback pattern")
        } catch (e: Throwable) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Remote Hangup: Soft descending double pulse pattern confirming remote party ended the call.
     */
    fun vibrateRemoteHangup(context: Context) {
        stopIncomingCallVibration(context)
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 120, 100, 80)
                val amplitudes = intArrayOf(0, 200, 0, 100)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 120, 100, 80), -1)
            }
            Log.i(TAG, "Triggered REMOTE_HANGUP haptic feedback pattern")
        } catch (e: Throwable) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Call Drop / Connection Error: Triple rapid warning vibration pattern indicating line disruption.
     */
    fun vibrateCallDrop(context: Context) {
        stopIncomingCallVibration(context)
        val vibrator = getVibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 100, 60, 100, 60, 200)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 100, 60, 100, 60, 200), -1)
            }
            Log.i(TAG, "Triggered CALL_DROP haptic feedback pattern")
        } catch (e: Throwable) {
            Log.w(TAG, "Vibration failed: ${e.message}")
        }
    }

    /**
     * Control Action (Mute/Camera/Speaker): Light click feedback.
     */
    fun vibrateControlClick(context: Context) {
        vibrateShortActionPulse(context)
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}

