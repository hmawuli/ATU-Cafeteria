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
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_SAVED_USERNAME = "key_saved_username"
        private const val KEY_SAVED_PIN = "key_saved_pin"
        private const val KEY_FORCE_OFFLINE_MODE = "key_force_offline_mode"
        private const val KEY_BIOMETRIC_PAYMENT_PROFILE_REQUIRED = "key_biometric_payment_profile_required"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val SESSION_TIMEOUT_MS = 30L * 24L * 3600L * 1000L // 30 days session validity
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
     * Store active session credentials on successful login.
     */
    fun saveSession(
        token: String,
        userId: Int,
        userName: String,
        role: String
    ) {
        prefs.edit().apply {
            putString(KEY_AUTH_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_ROLE, role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_LAST_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        Log.i(TAG, "User session stored securely for user: $userName ($role)")
    }

    /**
     * Retrieve active JWT or Bearer auth token.
     */
    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    /**
     * Check if valid active user session exists and has not expired.
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val token = getAuthToken()
        val lastLogin = getLastLoginTime()
        val isExpired = lastLogin > 0L && (System.currentTimeMillis() - lastLogin) > SESSION_TIMEOUT_MS
        if (isLoggedIn && isExpired) {
            notifySessionExpired("Token expired after maximum session duration.")
            return false
        }
        return isLoggedIn && !token.isNullOrBlank()
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
