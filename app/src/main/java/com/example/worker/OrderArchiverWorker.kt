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
 * Android WorkManager CoroutineWorker that automatically archives order history
 * older than 6 months into a local SQLite database table for long-term storage and privacy.
 */
class OrderArchiverWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting WorkManager automated order history archiving task...")
            val repository = ServiceLocator.provideCafeteriaRepository(applicationContext)

            // Cutoff: 6 months ago (180 days in milliseconds)
            val sixMonthsInMillis = 180L * 24 * 60 * 60 * 1000
            val cutoffTimestamp = System.currentTimeMillis() - sixMonthsInMillis

            val archivedCount = repository.archiveOrdersOlderThan(cutoffTimestamp)
            Log.i(TAG, "Successfully archived $archivedCount past orders older than 6 months into SQLite archive database.")

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background order archiving worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "OrderArchiverWorker"
        const val WORK_NAME = "periodic_order_archiver_work"

        /**
         * Enqueues a 24-hour periodic WorkManager task for order history archiving.
         */
        fun schedulePeriodicArchive(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val archiveRequest = PeriodicWorkRequestBuilder<OrderArchiverWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                archiveRequest
            )
            Log.i(TAG, "Successfully enqueued periodic background order archiving worker.")
        }
    }
}
