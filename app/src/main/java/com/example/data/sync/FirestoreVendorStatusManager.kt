package com.example.data.sync

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Sync Manager for Vendor Operational Statuses (OPEN / BUSY / CLOSED).
 * Ensures administrative toggles on vendor state sync immediately across all views in real-time.
 */
object FirestoreVendorStatusManager {

    private const val TAG = "VendorStatusManager"

    // Real-time map of vendorId -> status ("OPEN", "BUSY", "CLOSED")
    private val _vendorStatusMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val vendorStatusMap: StateFlow<Map<Int, String>> = _vendorStatusMap.asStateFlow()

    private var isListening = false
    private val statusListeners = mutableListOf<(Map<Int, String>) -> Unit>()

    /**
     * Updates vendor status ("OPEN", "BUSY", "CLOSED") for real-time synchronization.
     */
    fun updateVendorStatus(vendorId: Int, status: String, vendorName: String = "") {
        val uppercaseStatus = when (status.uppercase()) {
            "BUSY" -> "BUSY"
            "CLOSED" -> "CLOSED"
            else -> "OPEN"
        }

        // Update local state immediately
        val currentMap = _vendorStatusMap.value.toMutableMap()
        currentMap[vendorId] = uppercaseStatus
        _vendorStatusMap.value = currentMap

        Log.i(TAG, "Successfully updated vendor #$vendorId ($vendorName) status to '$uppercaseStatus'.")

        statusListeners.forEach { listener ->
            try {
                listener(currentMap)
            } catch (e: Exception) {
                Log.w(TAG, "Error invoking status listener: ${e.message}")
            }
        }
    }

    /**
     * Initializes real-time listener for vendor status updates.
     */
    fun startRealtimeStatusListener(onStatusChange: ((Map<Int, String>) -> Unit)? = null) {
        if (onStatusChange != null && !statusListeners.contains(onStatusChange)) {
            statusListeners.add(onStatusChange)
        }
        if (isListening) return
        isListening = true
        Log.i(TAG, "Vendor status real-time sync manager active.")
    }

    /**
     * Get the current status ("OPEN", "BUSY", "CLOSED") for a vendor.
     */
    fun getVendorStatus(vendorId: Int, defaultIsOpen: Boolean = true): String {
        return _vendorStatusMap.value[vendorId] ?: if (defaultIsOpen) "OPEN" else "CLOSED"
    }
}
