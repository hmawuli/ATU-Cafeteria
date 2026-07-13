package com.example.ui.util

import com.example.data.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WaitTimeService {
    /**
     * Calculates the estimated wait time in minutes for a new order.
     * Formula: Base Prep Time (8 mins) + (Queue Size * 3 mins) + (Total Quantity of Active Items * 2 mins)
     */
    fun calculateEstimatedWaitTime(activeOrders: List<Order>, newItemQuantity: Int = 1): Int {
        val queueSize = activeOrders.count { isOrderActiveInQueue(it.status) }
        val activeQuantities = activeOrders.filter { isOrderActiveInQueue(it.status) }.sumOf { it.quantity }
        
        val basePrepTime = 8
        val queueMultiplier = 3
        val quantityMultiplier = 2
        
        val estimatedMinutes = basePrepTime + (queueSize * queueMultiplier) + ((activeQuantities + newItemQuantity) * quantityMultiplier)
        return estimatedMinutes.coerceIn(5, 120) // bounded between 5 mins and 2 hours
    }

    /**
     * Formats the estimated pickup time based on the wait time in minutes.
     */
    fun calculatePickupTimeText(waitTimeMinutes: Int): String {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val futureTime = System.currentTimeMillis() + (waitTimeMinutes * 60 * 1000)
        return formatter.format(Date(futureTime))
    }

    fun isOrderActiveInQueue(status: String): Boolean {
        val upper = status.uppercase()
        return upper == "PENDING" || upper == "PREPARING" || upper == "PROCESSING" || upper == "PAID"
    }
}
