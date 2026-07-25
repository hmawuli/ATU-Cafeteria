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
 * Android WorkManager CoroutineWorker responsible for periodic background synchronization
 * of menu data and order status updates, maintaining local Room database freshness.
 */
class MenuAndOrdersSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting periodic background sync of menu data and order updates...")
            val repository = ServiceLocator.provideCafeteriaRepository(applicationContext)

            // Ensure database is populated with initial menu/user records if needed
            repository.seedDatabaseIfEmpty()

            // Synchronize pending local Room database order changes with remote server
            try {
                val syncedCount = repository.syncOfflineOrders()
                if (syncedCount > 0) {
                    Log.i(TAG, "Successfully synced $syncedCount pending offline Room order(s) to remote server.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Offline Room order sync skipped/failed: ${e.message}")
            }

            // Attempt background sync of remote notifications and order status updates
            try {
                repository.fetchLaravelNotifications()
                Log.i(TAG, "Background remote notifications/order updates synced.")
            } catch (e: Exception) {
                Log.w(TAG, "Remote notification sync skipped/failed: ${e.message}")
            }

            Log.i(TAG, "Periodic menu & order background sync completed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background menu/order sync worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "MenuOrdersSyncWorker"
        const val WORK_NAME = "periodic_menu_orders_sync_work"

        /**
         * Enqueues a 15-minute periodic WorkManager task requiring network connection.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<MenuAndOrdersSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.i(TAG, "Successfully enqueued periodic background sync worker.")
        }
    }
}
