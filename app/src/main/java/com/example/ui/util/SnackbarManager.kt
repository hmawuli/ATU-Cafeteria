package com.example.ui.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global Snackbar Manager for emitting user feedback messages across the Compose UI layer.
 */
object SnackbarManager {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val messages = _messages.asSharedFlow()

    /**
     * Emits a user feedback message to be displayed via the global SnackbarHost.
     */
    fun showMessage(message: String) {
        _messages.tryEmit(message)
    }
}
