package com.example.data.sync

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Sync Manager for Order Tracking via Stream Listeners and StateFlow.
 * Updates student food order statuses in real-time (e.g., 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'DELIVERED').
 */
object FirestoreOrderTrackingManager {

    private const val TAG = "OrderTrackingManager"

    // Real-time map of orderId -> order status ("PENDING", "PREPARING", "READY", "COMPLETED", "DELIVERED")
    private val _orderStatusMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val orderStatusMap: StateFlow<Map<Int, String>> = _orderStatusMap.asStateFlow()

    // Map of orderId -> estimated pickup time (e.g. "10-15 Min")
    private val _orderEstimatedTimeMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val orderEstimatedTimeMap: StateFlow<Map<Int, String>> = _orderEstimatedTimeMap.asStateFlow()

    private var isListening = false
    private val orderListeners = mutableListOf<(orderId: Int, newStatus: String, estimatedTime: String?) -> Unit>()

    /**
     * Publishes or updates an order status for real-time tracking stream listeners.
     */
    fun updateOrderStatusInFirestore(
        orderId: Int,
        newStatus: String,
        vendorId: Int = 0,
        studentId: Int = 0,
        estimatedTime: String? = null
    ) {
        val uppercaseStatus = newStatus.uppercase()

        // Update local StateFlow immediately
        val currentStatusMap = _orderStatusMap.value.toMutableMap()
        currentStatusMap[orderId] = uppercaseStatus
        _orderStatusMap.value = currentStatusMap

        if (!estimatedTime.isNullOrBlank()) {
            val currentEstMap = _orderEstimatedTimeMap.value.toMutableMap()
            currentEstMap[orderId] = estimatedTime
            _orderEstimatedTimeMap.value = currentEstMap
        }

        Log.i(TAG, "Order #$orderId status updated to '$uppercaseStatus' (Est: $estimatedTime).")

        orderListeners.forEach { listener ->
            try {
                listener(orderId, uppercaseStatus, estimatedTime)
            } catch (e: Exception) {
                Log.w(TAG, "Error invoking order listener: ${e.message}")
            }
        }
    }

    /**
     * Starts real-time stream listener for order status updates.
     */
    fun startRealtimeOrderTrackingListener(
        onOrderUpdate: ((orderId: Int, newStatus: String, estimatedTime: String?) -> Unit)? = null
    ) {
        if (onOrderUpdate != null && !orderListeners.contains(onOrderUpdate)) {
            orderListeners.add(onOrderUpdate)
        }
        if (isListening) return
        isListening = true
        Log.i(TAG, "Order tracking real-time sync manager active.")
    }

    /**
     * Get live status for an order or fallback to default.
     */
    fun getLiveStatus(orderId: Int, defaultStatus: String): String {
        return _orderStatusMap.value[orderId] ?: defaultStatus
    }
}
