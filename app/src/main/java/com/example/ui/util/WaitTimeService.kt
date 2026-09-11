package com.example.ui.util

import com.example.data.Order
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WaitTimeEstimate(
    val estimatedMinutes: Int,
    val pickupTimeFormatted: String,
    val queueSize: Int,
    val activeQuantityTotal: Int,
    val historicalAvgMinutes: Int,
    val isPeakHour: Boolean,
    val summaryText: String
)

object WaitTimeService {

    /**
     * Calculates the estimated wait time in minutes utilizing historical order data and real-time order volume.
     */
    fun calculateDetailedWaitTime(
        activeOrders: List<Order>,
        historicalOrders: List<Order> = emptyList(),
        vendorId: Int = 0,
        newItemQuantity: Int = 1
    ): WaitTimeEstimate {
        val queueOrders = activeOrders.filter { isOrderActiveInQueue(it.status) && (vendorId == 0 || it.vendorId == vendorId) }
        val queueSize = queueOrders.size
        val activeQuantities = queueOrders.sumOf { it.quantity }

        // Calculate historical average prep time for completed orders
        val vendorCompleted = historicalOrders.filter {
            (vendorId == 0 || it.vendorId == vendorId) && it.status.uppercase() == "COMPLETED"
        }
        
        val historicalAvgMinutes = if (vendorCompleted.isNotEmpty()) {
            // Estimate average completion time or fallback based on historical order volume
            val avg = vendorCompleted.map { 6 + (it.quantity * 2) }.average().toInt()
            avg.coerceIn(5, 25)
        } else {
            8 // Default historical baseline
        }

        // Check if current time is during ATU Campus Lunch Rush (11:30 AM - 2:00 PM)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinutesOfDay = hour * 60 + minute
        val isPeakLunchHour = currentMinutesOfDay in (11 * 60 + 30)..(14 * 60)

        val peakMultiplier = if (isPeakLunchHour) 1.25 else 1.0
        val queueAddon = (queueSize * 2.5) + ((activeQuantities + newItemQuantity) * 1.5)

        val totalEstMinutes = ((historicalAvgMinutes + queueAddon) * peakMultiplier).toInt().coerceIn(5, 90)

        val pickupText = calculatePickupTimeText(totalEstMinutes)
        val peakLabel = if (isPeakLunchHour) "🔥 Lunch Rush" else "⚡ Standard Speed"
        val summaryText = "$totalEstMinutes mins ($queueSize in queue • $peakLabel • Ready ~$pickupText)"

        return WaitTimeEstimate(
            estimatedMinutes = totalEstMinutes,
            pickupTimeFormatted = pickupText,
            queueSize = queueSize,
            activeQuantityTotal = activeQuantities,
            historicalAvgMinutes = historicalAvgMinutes,
            isPeakHour = isPeakLunchHour,
            summaryText = summaryText
        )
    }

    /**
     * Legacy simple calculator backward compatibility
     */
    fun calculateEstimatedWaitTime(activeOrders: List<Order>, newItemQuantity: Int = 1): Int {
        return calculateDetailedWaitTime(activeOrders = activeOrders, newItemQuantity = newItemQuantity).estimatedMinutes
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

