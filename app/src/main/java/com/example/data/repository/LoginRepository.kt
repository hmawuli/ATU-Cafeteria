package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.CafeteriaRepository
import com.example.data.LaravelClientManager
import com.example.data.User
import com.example.ui.util.CrashlyticsHelper
import com.example.ui.util.FirebaseAnalyticsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LoginRepository wraps authentication and Retrofit / Laravel authentication network calls
 * with exception handling and structured logging to Logcat.
 */
class LoginRepository(
    private val context: Context,
    private val cafeteriaRepository: CafeteriaRepository
) {

    suspend fun authenticateUser(usernameInput: String, pinCode: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanUsername = usernameInput.trim().lowercase()
        val formattedEmail = if (cleanUsername.contains("@")) cleanUsername else "$cleanUsername@atu.edu.gh"

        Log.i(TAG, "========== LOGIN REPOSITORY AUTH ATTEMPT ==========")
        Log.i(TAG, "Target Username : '$cleanUsername'")
        Log.i(TAG, "Formatted Email : '$formattedEmail'")
        Log.i(TAG, "PIN Length      : ${pinCode.length}")

        // 1. Attempt Local Room Database Auth (always fast & reliable)
        val localUser = try {
            cafeteriaRepository.authenticateUser(cleanUsername, pinCode)
        } catch (e: Exception) {
            Log.e(TAG, "Local Database Auth Exception: ${e.message}", e)
            CrashlyticsHelper.recordException(e)
            null
        }

        if (localUser != null) {
            Log.i(TAG, "SUCCESS: Local Database authenticated user '${localUser.username}' (Role: ${localUser.role})")
            return@withContext Result.success(localUser)
        }

        // 2. Attempt Remote Backend Auth
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val isHealthy = LaravelClientManager.pingBackendHealth()
                Log.i(TAG, "Laravel Backend Ping Result: $isHealthy")
            } catch (e: Exception) {
                Log.e(TAG, "Laravel Auth Exception: ${e.message}", e)
            }
        }

        Log.w(TAG, "FAILURE: Invalid credentials for '$cleanUsername'")
        return@withContext Result.failure(Exception("Invalid username or Access PIN. Please verify your credentials or use quick-fill options."))
    }

    companion object {
        private const val TAG = "LoginRepository"
    }
}
