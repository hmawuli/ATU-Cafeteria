package com.example.data.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Sync Manager for Order Tracking via Firebase Firestore Stream Listeners.
 * Updates student food order statuses in real-time (e.g., 'PENDING', 'PREPARING', 'READY', 'COMPLETED', 'DELIVERED').
 */
object FirestoreOrderTrackingManager {

    private const val TAG = "FirestoreOrderTracking"
    private const val COLLECTION_NAME = "order_tracking"

    // Real-time map of orderId -> order status ("PENDING", "PREPARING", "READY", "COMPLETED", "DELIVERED")
    private val _orderStatusMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val orderStatusMap: StateFlow<Map<Int, String>> = _orderStatusMap.asStateFlow()

    // Map of orderId -> estimated pickup time (e.g. "10-15 Min")
    private val _orderEstimatedTimeMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val orderEstimatedTimeMap: StateFlow<Map<Int, String>> = _orderEstimatedTimeMap.asStateFlow()

    private var isListening = false

    /**
     * Publishes or updates an order status in Firestore for real-time tracking stream listeners.
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

        try {
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection(COLLECTION_NAME).document("order_$orderId")

            val payload = hashMapOf(
                "orderId" to orderId,
                "status" to uppercaseStatus,
                "vendorId" to vendorId,
                "studentId" to studentId,
                "estimatedTime" to (estimatedTime ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(payload)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully synced order #$orderId status to '$uppercaseStatus' on Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync order #$orderId to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore uninitialized or unavailable for order tracking update: ${e.message}")
        }
    }

    /**
     * Starts real-time stream listener for order status updates from Firestore.
     */
    fun startRealtimeOrderTrackingListener(
        onOrderUpdate: ((orderId: Int, newStatus: String, estimatedTime: String?) -> Unit)? = null
    ) {
        if (isListening) return

        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_NAME)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore order tracking snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newStatusMap = _orderStatusMap.value.toMutableMap()
                        val newEstMap = _orderEstimatedTimeMap.value.toMutableMap()

                        for (doc in snapshot.documents) {
                            val oId = doc.getLong("orderId")?.toInt() ?: continue
                            val status = doc.getString("status") ?: continue
                            val estTime = doc.getString("estimatedTime")

                            newStatusMap[oId] = status
                            if (!estTime.isNullOrBlank()) {
                                newEstMap[oId] = estTime
                            }

                            onOrderUpdate?.invoke(oId, status, estTime)
                        }

                        _orderStatusMap.value = newStatusMap
                        _orderEstimatedTimeMap.value = newEstMap
                        Log.d(TAG, "Real-time order statuses updated via Firestore: $newStatusMap")
                    }
                }
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Firestore order tracking listener: ${e.message}")
        }
    }

    /**
     * Get live status for an order or fallback to default.
     */
    fun getLiveStatus(orderId: Int, defaultStatus: String): String {
        return _orderStatusMap.value[orderId] ?: defaultStatus
    }
}
