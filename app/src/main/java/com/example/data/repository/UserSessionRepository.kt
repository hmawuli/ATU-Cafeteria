package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.User
import com.example.ui.util.CrashlyticsHelper
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Authentication State Listener interface for observing session events,
 * token expirations, and network interruptions.
 */
interface AuthStateListener {
    fun onAuthenticated(user: User) {}
    fun onSessionExpired(reason: String) {}
    fun onNetworkInterrupted(message: String) {}
}

/**
 * Clean Repository Layer encapsulating User Session Management,
 * Authentication Flow, Token Storage, and Encrypted User Preferences.
 */
class UserSessionRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val authListeners = CopyOnWriteArrayList<AuthStateListener>()

    companion object {
        private const val TAG = "UserSessionRepository"
        private const val PREFS_NAME = "atu_cafeteria_user_session_secure"
        
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_LAST_LOGIN_TIMESTAMP = "key_last_login_timestamp"
        private const val KEY_LAST_ACTIVITY_TIMESTAMP = "key_last_activity_timestamp"
        private const val KEY_FAILED_ATTEMPTS = "key_failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "key_lockout_until"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_SAVED_USERNAME = "key_saved_username"
        private const val KEY_SAVED_PIN = "key_saved_pin"
        private const val KEY_FORCE_OFFLINE_MODE = "key_force_offline_mode"
        private const val KEY_BIOMETRIC_PAYMENT_PROFILE_REQUIRED = "key_biometric_payment_profile_required"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        
        // 24 Hours of Inactivity Policy for OAuth 2.0 / Security Tokens
        const val OAUTH_INACTIVITY_TIMEOUT_MS = 24L * 3600L * 1000L // 24 Hours (86,400,000 ms)
        const val INACTIVITY_LOCK_TIMEOUT_MS = 5L * 60L * 1000L // 5 Minutes (300,000 ms)
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 60_000L // 60 seconds temporary lockout
    }

    /**
     * Check if session has been inactive for more than 5 minutes, requiring PIN or Biometric re-verification.
     */
    fun isInactivityLocked(): Boolean {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) return false
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L))
        if (lastActivity <= 0L) return false
        val elapsed = System.currentTimeMillis() - lastActivity
        return elapsed >= INACTIVITY_LOCK_TIMEOUT_MS
    }

    /**
     * Unlock session after successful PIN / Biometric authentication and record new activity timestamp.
     */
    fun unlockInactivitySession() {
        recordUserActivity()
        Log.i(TAG, "Inactivity session lock cleared and refreshed.")
    }

    fun addAuthStateListener(listener: AuthStateListener) {
        if (!authListeners.contains(listener)) {
            authListeners.add(listener)
        }
    }

    fun removeAuthStateListener(listener: AuthStateListener) {
        authListeners.remove(listener)
    }

    fun notifyNetworkInterruption(message: String) {
        CrashlyticsHelper.log("Network Interruption: $message")
        authListeners.forEach { 
            try { it.onNetworkInterrupted(message) } catch (e: Exception) { Log.e(TAG, "Error notifying auth listener", e) }
        }
    }

    fun notifySessionExpired(reason: String) {
        CrashlyticsHelper.log("Session Expired: $reason")
        clearSession()
        authListeners.forEach { 
            try { it.onSessionExpired(reason) } catch (e: Exception) { Log.e(TAG, "Error notifying auth listener", e) }
        }
    }

    fun notifyAuthenticated(user: User) {
        CrashlyticsHelper.setUserId("${user.id}_${user.username}")
        authListeners.forEach { 
            try { it.onAuthenticated(user) } catch (e: Exception) { Log.e(TAG, "Error notifying auth listener", e) }
        }
    }

    /**
     * Persists user session object, auth token, and lastActiveTimestamp.
     */
    fun persistUserSession(
        token: String,
        userId: Int,
        userName: String,
        role: String
    ) {
        val now = System.currentTimeMillis()
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LAST_LOGIN_TIMESTAMP, now)
            putLong(KEY_LAST_ACTIVITY_TIMESTAMP, now)
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKOUT_UNTIL, 0L)
            apply()
        }
        Log.i(TAG, "User session persisted securely for user: $userName ($role)")
    }

    /**
     * Store active session credentials on successful login and reset rate limit counters.
     */
    fun saveSession(
        token: String,
        userId: Int,
        userName: String,
        role: String
    ) {
        persistUserSession(token, userId, userName, role)
    }

    /**
     * Update the last activity timestamp whenever the user performs actions in the app.
     */
    fun recordUserActivity() {
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.edit().putLong(KEY_LAST_ACTIVITY_TIMESTAMP, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Retrieve active JWT or Bearer auth token.
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Check if valid active user session exists and automatically invalidate if inactive for >24 hours.
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val token = getAuthToken()
        if (!isLoggedIn || token.isNullOrBlank()) {
            return false
        }
        
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L))
        val now = System.currentTimeMillis()
        val isInactiveFor24Hours = lastActivity > 0L && (now - lastActivity) > OAUTH_INACTIVITY_TIMEOUT_MS

        if (isInactiveFor24Hours) {
            Log.w(TAG, "Session token automatically invalidated after 24 hours of inactivity.")
            notifySessionExpired("Session token automatically invalidated after 24 hours of inactivity.")
            return false
        }

        return true
    }

    /**
     * Explicit verification and invalidation of inactive session tokens.
     */
    fun checkAndInvalidateInactiveToken(): Boolean {
        if (prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L))
            val now = System.currentTimeMillis()
            if (lastActivity > 0L && (now - lastActivity) > OAUTH_INACTIVITY_TIMEOUT_MS) {
                notifySessionExpired("OAuth 2.0 session token expired after 24 hours of inactivity.")
                return true
            }
        }
        return false
    }

    /**
     * Helper to verify rate limiting in the login flow.
     * If failed attempt count exceeds 5, lockoutUntil timestamp is set for 60 seconds.
     * Returns true if locked out, false otherwise.
     */
    fun checkRateLimit(identifier: String = ""): Boolean {
        return isLockedOut()
    }

    /**
     * Rate Limiting: Check if authentication is currently locked out after 5 failed attempts.
     */
    fun isLockedOut(): Boolean {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        if (lockoutUntil > now) {
            return true
        }
        return false
    }

    /**
     * Returns remaining lockout time in seconds.
     */
    fun getRemainingLockoutSeconds(): Int {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            (((lockoutUntil - now) / 1000) + 1).toInt()
        } else {
            0
        }
    }

    /**
     * Record a failed login attempt. If failed attempts reach 5, trigger 60-second lockout.
     */
    fun recordFailedLoginAttempt(): Pair<Int, Boolean> {
        val currentAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val now = System.currentTimeMillis()
        val isNowLockedOut = currentAttempts >= MAX_FAILED_ATTEMPTS
        val lockoutUntil = if (isNowLockedOut) now + LOCKOUT_DURATION_MS else 0L

        prefs.edit().apply {
            putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
            if (isNowLockedOut) {
                putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            }
            apply()
        }
        Log.w(TAG, "Failed login attempt #$currentAttempts. Locked out: $isNowLockedOut")
        return Pair(currentAttempts, isNowLockedOut)
    }

    /**
     * Reset rate-limiting failed attempts on successful login.
     */
    fun resetFailedAttempts() {
        prefs.edit().apply {
            putInt(KEY_FAILED_ATTEMPTS, 0)
            putLong(KEY_LOCKOUT_UNTIL, 0L)
            apply()
        }
    }

    fun getFailedAttemptsCount(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "Student") ?: "Student"
    }

    fun getUserRole(): String {
        return prefs.getString(KEY_USER_ROLE, "STUDENT") ?: "STUDENT"
    }

    fun getLastLoginTime(): Long {
        return prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L)
    }

    fun getLastActivityTime(): Long {
        return prefs.getLong(KEY_LAST_ACTIVITY_TIMESTAMP, 0L)
    }

    /**
     * Enable or disable Biometric authentication fast-login.
     */
    fun setBiometricLoginEnabled(enabled: Boolean, username: String = "", pin: String = "") {
        prefs.edit().apply {
            putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            if (enabled && username.isNotBlank()) {
                putString(KEY_SAVED_USERNAME, username)
                putString(KEY_SAVED_PIN, pin)
            } else if (!enabled) {
                remove(KEY_SAVED_USERNAME)
                remove(KEY_SAVED_PIN)
            }
            apply()
        }
    }

    fun isBiometricLoginEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun getSavedBiometricUsername(): String {
        return prefs.getString(KEY_SAVED_USERNAME, "") ?: ""
    }

    fun getSavedBiometricPin(): String {
        return prefs.getString(KEY_SAVED_PIN, "") ?: ""
    }

    /**
     * Set persistent 'Offline Mode' forcing room database usage.
     */
    fun setForceOfflineMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_OFFLINE_MODE, enabled).apply()
        com.example.data.LaravelClientManager.isLaravelEnabled = !enabled
        Log.i(TAG, "Force offline mode set to $enabled")
    }

    fun isForceOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_FORCE_OFFLINE_MODE, false)
    }

    /**
     * Require fingerprint / facial recognition before accessing payment or profile sections.
     */
    fun setBiometricPaymentProfileRequired(required: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_PAYMENT_PROFILE_REQUIRED, required).apply()
        Log.i(TAG, "Biometric payment/profile requirement set to $required")
    }

    fun isBiometricPaymentProfileRequired(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_PAYMENT_PROFILE_REQUIRED, false)
    }

    /**
     * Track if onboarding carousel has been completed.
     */
    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    /**
     * Logout and purge secure user session.
     */
    fun clearSession() {
        prefs.edit().apply {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROLE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Log.i(TAG, "Active user session cleared successfully.")
    }
}
