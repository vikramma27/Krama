package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

object TelemetryManager {
    private const val TAG = "KramaTelemetry"
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            Log.d(TAG, "TelemetryManager initialized cleanly with privacy safeguards.")
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAnalytics init note: ${e.message}")
        }
    }

    /**
     * Log non-PII app performance metric.
     * Content of messages or encryption keys MUST NOT be included.
     */
    fun logPerformanceMetric(eventName: String, durationMs: Long, category: String = "performance") {
        try {
            val bundle = Bundle().apply {
                putLong("duration_ms", durationMs)
                putString("category", category)
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
            Log.i(TAG, "[Telemetry] Metric logged -> Event: $eventName, Duration: ${durationMs}ms, Category: $category")
        } catch (e: Throwable) {
            Log.w(TAG, "Telemetry logging exception: ${e.message}")
        }
    }

    /**
     * Log non-PII exception/error stacktrace for diagnostics.
     */
    fun logNonPiiException(tag: String, errorMsg: String, throwable: Throwable? = null) {
        try {
            val sanitizeMsg = errorMsg.take(100)
            val bundle = Bundle().apply {
                putString("tag", tag)
                putString("error_summary", sanitizeMsg)
                putString("exception_type", throwable?.javaClass?.simpleName ?: "UnknownException")
            }
            firebaseAnalytics?.logEvent("non_pii_app_error", bundle)
            Log.e(TAG, "[Telemetry Non-PII Error] $tag: $sanitizeMsg", throwable)
        } catch (e: Throwable) {
            Log.w(TAG, "Telemetry error logging exception: ${e.message}")
        }
    }
}
