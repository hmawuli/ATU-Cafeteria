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
import com.example.ui.util.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Background Service / WorkManager worker that periodically checks the orders queue
 * and triggers notifications when new orders with 'RECEIVED' status are detected
 * for the active kitchen / cafeteria vendors.
 */
class KitchenOrderQueueNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Checking kitchen order queue for new 'RECEIVED' orders...")
            val repository = ServiceLocator.provideCafeteriaRepository(applicationContext)

            // Fetch all current orders from local repository / cache
            val allOrders = try {
                repository.orderDao.getAllOrdersSync()
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch orders synchronously from orderDao", e)
                emptyList()
            }

            // Look for orders in 'RECEIVED' or 'PENDING' status that need kitchen attention
            val receivedOrders = allOrders.filter { order ->
                val st = order.status.uppercase().trim()
                st == "RECEIVED" || st == "PENDING"
            }

            if (receivedOrders.isNotEmpty()) {
                val prefs = applicationContext.getSharedPreferences(PREFS_KITCHEN_NOTIFICATIONS, Context.MODE_PRIVATE)
                val notifiedOrderIds = prefs.getStringSet(KEY_NOTIFIED_ORDERS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
                var newCount = 0

                receivedOrders.forEach { order ->
                    val orderKey = "${order.id}_${order.status}"
                    if (!notifiedOrderIds.contains(orderKey)) {
                        newCount++
                        notifiedOrderIds.add(orderKey)

                        // Trigger order status notification
                        val title = "🔔 New Kitchen Order #${order.id} [RECEIVED]"
                        val message = "Order: ${order.quantity}x ${order.foodName} (GH₵ ${"%.2f".format(order.totalPrice)}) received for Kitchen #${order.vendorId}. Ready for prep!"

                        NotificationHelper.sendOrderStatusNotification(
                            context = applicationContext,
                            orderId = order.id,
                            title = title,
                            text = message
                        )
                    }
                }

                if (newCount > 0) {
                    prefs.edit().putStringSet(KEY_NOTIFIED_ORDERS, notifiedOrderIds).apply()
                    Log.i(TAG, "Dispatched $newCount notification(s) for newly RECEIVED kitchen orders.")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing kitchen order queue notification worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "KitchenOrderNotificationWorker"
        const val WORK_NAME = "periodic_kitchen_order_queue_check"
        private const val PREFS_KITCHEN_NOTIFICATIONS = "atu_kitchen_notification_prefs"
        private const val KEY_NOTIFIED_ORDERS = "notified_received_order_ids"

        /**
         * Enqueues periodic 15-minute background check for kitchen orders with RECEIVED status.
         */
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            val request = PeriodicWorkRequestBuilder<KitchenOrderQueueNotificationWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Enqueued kitchen order queue notification worker.")
        }

        /**
         * Mark an order ID as acknowledged/notified
         */
        fun markOrderNotified(context: Context, orderId: Int, status: String) {
            val prefs = context.getSharedPreferences(PREFS_KITCHEN_NOTIFICATIONS, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_NOTIFIED_ORDERS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            set.add("${orderId}_$status")
            prefs.edit().putStringSet(KEY_NOTIFIED_ORDERS, set).apply()
        }
    }
}
