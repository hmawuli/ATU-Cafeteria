package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.di.ServiceLocator
import java.util.concurrent.TimeUnit

/**
 * Battery-optimized WorkManager CoroutineWorker designed specifically for non-critical
 * background data synchronization. Executes ONLY when the device is actively charging
 * AND connected to an unmetered Wi-Fi network to conserve student mobile data and battery life.
 */
class ChargingWifiDataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting non-critical background data sync (Charging + Wi-Fi conditions met)...")
            val repository = ServiceLocator.provideCafeteriaRepository(applicationContext)

            // 1. Seed database and initialize default cache tables if empty
            repository.seedDatabaseIfEmpty()

            // 2. Synchronize any queued offline Room orders to remote backend/Firestore
            val syncedCount = repository.syncOfflineOrders()
            if (syncedCount > 0) {
                Log.i(TAG, "ChargingWifiSyncWorker: Synced $syncedCount queued offline orders.")
            }

            // 3. Pre-fetch remote database notifications & menu updates
            try {
                repository.fetchLaravelNotifications()
                Log.i(TAG, "ChargingWifiSyncWorker: Pre-fetched database notifications & menu updates.")
            } catch (e: Exception) {
                Log.w(TAG, "ChargingWifiSyncWorker notification fetch skipped/failed: ${e.message}")
            }

            // 4. Log audit log for background battery-friendly sync
            repository.insertAuditLog(
                userId = 1,
                action = "BATTERY_WIFI_SYNC",
                details = "Executed background data sync under battery-friendly conditions (Charging + Wi-Fi)."
            )

            Log.i(TAG, "Non-critical background data sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing ChargingWifiDataSyncWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ChargingWifiSyncWorker"
        const val WORK_NAME = "periodic_charging_wifi_data_sync_work"

        /**
         * Schedules a periodic 1-hour background sync constrained strictly to charging and Wi-Fi.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true) // Device must be plugged in / charging
                .setRequiredNetworkType(NetworkType.UNMETERED) // Must be connected to Wi-Fi
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ChargingWifiDataSyncWorker>(
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.i(TAG, "Successfully enqueued battery-friendly (Charging + Wi-Fi) background sync worker.")
        }
    }
}
