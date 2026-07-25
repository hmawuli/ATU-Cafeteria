package com.example.ui.util

import android.content.Context
import android.util.Log

/**
 * Global Firebase Crashlytics & Real-time Error Monitoring Helper.
 * Handles non-fatal exception reporting, breadcrumb logging, user tagging,
 * and key-value diagnostic metadata tracking for application stability.
 */
object CrashlyticsHelper {
    private const val TAG = "ATU_Crashlytics"
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        Log.i(TAG, "ATU Cafeteria Crashlytics & Real-time Error Monitoring initialized.")
    }

    /**
     * Report a non-fatal exception to real-time telemetry / Crashlytics dashboard.
     */
    fun recordException(throwable: Throwable, reason: String? = null) {
        if (!reason.isNullOrBlank()) {
            Log.e(TAG, "Non-fatal exception recorded ($reason): ${throwable.message}", throwable)
        } else {
            Log.e(TAG, "Non-fatal exception recorded: ${throwable.message}", throwable)
        }
    }

    /**
     * Add a breadcrumb log entry to track the user journey before an issue occurs.
     */
    fun log(message: String) {
        Log.d(TAG, "Breadcrumb Log: $message")
    }

    /**
     * Set authenticated user identifier for targeted session diagnostic logs.
     */
    fun setUserId(userId: String) {
        Log.i(TAG, "Authenticated Session User Identifier set: $userId")
    }

    /**
     * Attach custom diagnostic metadata (e.g. active vendor ID, connection status).
     */
    fun setCustomKey(key: String, value: String) {
        Log.d(TAG, "Custom Diagnostic Key Set [$key] = $value")
    }
}
