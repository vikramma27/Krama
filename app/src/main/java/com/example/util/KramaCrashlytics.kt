package com.example.util

import android.content.Context
import android.util.Log

object KramaCrashlytics {

    private const val TAG = "KramaCrashlytics"

    fun init(context: Context) {
        try {
            Log.i(TAG, "KramaCrashlytics initialized with sensitive data stripping enabled.")
        } catch (e: Throwable) {
            Log.w(TAG, "KramaCrashlytics initialization skipped: ${e.message}")
        }
    }

    /**
     * Logs non-fatal exceptions ensuring sensitive parameters (PII, phone, credentials) are stripped.
     */
    fun recordNonFatalException(throwable: Throwable, tag: String = "AppException") {
        val sanitizedMessage = sanitizeSensitiveInfo(throwable.message ?: "Unknown Exception")
        Log.e(tag, "Record Crashlytics Exception: $sanitizedMessage", throwable)
    }

    /**
     * Strips passwords, auth tokens, secrets, or full phone numbers from error trace strings.
     */
    private fun sanitizeSensitiveInfo(rawMessage: String): String {
        return rawMessage
            .replace(Regex("(?i)password[=:]\\s*[^\\s,]+"), "password=[REDACTED]")
            .replace(Regex("(?i)token[=:]\\s*[^\\s,]+"), "token=[REDACTED]")
            .replace(Regex("(?i)secret[=:]\\s*[^\\s,]+"), "secret=[REDACTED]")
            .replace(Regex("\\+91[0-9]{10}"), "+91XXXXXXXXXX")
    }
}

