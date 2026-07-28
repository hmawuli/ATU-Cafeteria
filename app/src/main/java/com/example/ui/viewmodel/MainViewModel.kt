package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.User
import com.example.data.repository.AuthStateListener
import com.example.data.repository.UserSessionRepository
import com.example.ui.util.CrashlyticsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class SessionExpired(val reason: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val userSessionRepo = UserSessionRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authListener = object : AuthStateListener {
        override fun onAuthenticated(user: User) {
            Log.i(TAG, "MainViewModel: User authenticated -> ${user.username}")
            _authState.value = AuthState.Authenticated(user)
        }

        override fun onSessionExpired(reason: String) {
            Log.w(TAG, "MainViewModel: Session expired -> $reason")
            CrashlyticsHelper.log("MainViewModel: Session expired ($reason)")
            _authState.value = AuthState.SessionExpired(reason)
        }

        override fun onNetworkInterrupted(message: String) {
            Log.i(TAG, "MainViewModel: Network interrupted -> $message")
        }
    }

    init {
        userSessionRepo.addAuthStateListener(authListener)
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        viewModelScope.launch {
            if (userSessionRepo.isLoggedIn()) {
                val user = User(
                    id = userSessionRepo.getUserId(),
                    username = userSessionRepo.getUserName(),
                    passwordHash = "",
                    role = userSessionRepo.getUserRole(),
                    fullName = userSessionRepo.getUserName(),
                    info = "Session Restored"
                )
                _authState.value = AuthState.Authenticated(user)
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun handleSessionTimeout(onResetNavigationToLogin: () -> Unit) {
        viewModelScope.launch {
            Log.w(TAG, "Handling session timeout - clearing tokens & resetting navigation to LoginScreen")
            userSessionRepo.clearSession()
            _authState.value = AuthState.SessionExpired("Session timed out")
            onResetNavigationToLogin()
        }
    }

    override fun onCleared() {
        super.onCleared()
        userSessionRepo.removeAuthStateListener(authListener)
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
