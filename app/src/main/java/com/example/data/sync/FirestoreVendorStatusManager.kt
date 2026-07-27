package com.example.data.sync

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real-time Sync Manager for Vendor Operational Statuses (OPEN / BUSY / CLOSED) via Firebase Firestore.
 * Ensures administrative toggles on vendor state sync immediately across all student devices.
 */
object FirestoreVendorStatusManager {

    private const val TAG = "FirestoreVendorStatus"
    private const val COLLECTION_NAME = "vendor_statuses"

    // Real-time map of vendorId -> status ("OPEN", "BUSY", "CLOSED")
    private val _vendorStatusMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val vendorStatusMap: StateFlow<Map<Int, String>> = _vendorStatusMap.asStateFlow()

    private var isListening = false

    /**
     * Updates vendor status ("OPEN", "BUSY", "CLOSED") in Firestore for real-time synchronization.
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

        try {
            try { FirebaseApp.getInstance() } catch (e: Exception) {
                Log.w(TAG, "FirebaseApp is not initialized. Skipping Firestore vendor status update.")
                return
            }
            val firestore = FirebaseFirestore.getInstance()
            val docRef = firestore.collection(COLLECTION_NAME).document("vendor_$vendorId")

            val payload = hashMapOf(
                "vendorId" to vendorId,
                "status" to uppercaseStatus,
                "vendorName" to vendorName,
                "updatedAt" to System.currentTimeMillis()
            )

            docRef.set(payload)
                .addOnSuccessListener {
                    Log.i(TAG, "Successfully synced vendor $vendorId status to '$uppercaseStatus' on Firestore.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync vendor $vendorId status to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore uninitialized or unavailable for status update: ${e.message}")
        }
    }

    /**
     * Initializes real-time listener for vendor status updates from Firestore.
     */
    fun startRealtimeStatusListener() {
        if (isListening) return

        try {
            try { FirebaseApp.getInstance() } catch (e: Exception) {
                Log.w(TAG, "FirebaseApp is not initialized. Skipping Firestore vendor status listener.")
                return
            }
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_NAME)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore vendor status snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val newMap = _vendorStatusMap.value.toMutableMap()
                        for (doc in snapshot.documents) {
                            val vId = doc.getLong("vendorId")?.toInt() ?: continue
                            val status = doc.getString("status") ?: "OPEN"
                            newMap[vId] = status
                        }
                        _vendorStatusMap.value = newMap
                        Log.d(TAG, "Real-time vendor statuses updated via Firestore: $newMap")
                    }
                }
            isListening = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Firestore real-time listener: ${e.message}")
        }
    }

    /**
     * Get the current status ("OPEN", "BUSY", "CLOSED") for a vendor.
     */
    fun getVendorStatus(vendorId: Int, defaultIsOpen: Boolean = true): String {
        return _vendorStatusMap.value[vendorId] ?: if (defaultIsOpen) "OPEN" else "CLOSED"
    }
}
