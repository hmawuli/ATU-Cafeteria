package com.example.ui.util

import java.util.Calendar
import java.util.Locale

data class VendorOperatingHoursInfo(
    val vendorId: Int,
    val scheduleText: String,        // e.g. "07:00 AM - 08:00 PM"
    val isCurrentlyOpen: Boolean,     // True if within schedule and manual isOpen is true
    val statusLabel: String,          // "OPEN NOW" or "CLOSED"
    val statusDetail: String,         // e.g. "Closes at 8:00 PM (12h 47m left)" or "Opens at 7:00 AM (in 11h 48m)"
    val openTimeFormatted: String,
    val closeTimeFormatted: String
)

object VendorOperatingHoursHelper {

    /**
     * Calculates automatic open/closed status based on real system time and vendor schedule.
     */
    fun getOperatingHoursInfo(vendorId: Int, manualIsOpen: Boolean = true): VendorOperatingHoursInfo {
        // Vendor Schedule mappings (openHour, openMin, closeHour, closeMin)
        val (openH, openM, closeH, closeM) = when (vendorId) {
            1 -> listOf(7, 0, 20, 0)   // Royal Kitchen (Main Hall): 7:00 AM - 8:00 PM
            2 -> listOf(8, 0, 19, 30)  // Auntie Mary Local Diner: 8:00 AM - 7:30 PM
            3 -> listOf(9, 0, 22, 0)   // Express Grill & Shawarma: 9:00 AM - 10:00 PM
            4 -> listOf(7, 30, 21, 0)  // Gourmet Bites & Drinks: 7:30 AM - 9:00 PM
            else -> listOf(7, 0, 20, 0)
        }

        val cal = Calendar.getInstance()
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMins = openH * 60 + openM
        val endMins = closeH * 60 + closeM

        val isOpenByTime = currentMins in startMins..endMins
        val effectiveOpen = manualIsOpen && isOpenByTime

        fun formatTime(h: Int, m: Int): String {
            val ampm = if (h >= 12) "PM" else "AM"
            val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
            return String.format(Locale.US, "%d:%02d %s", displayH, m, ampm)
        }

        val openTimeFormatted = formatTime(openH, openM)
        val closeTimeFormatted = formatTime(closeH, closeM)
        val scheduleText = "$openTimeFormatted - $closeTimeFormatted"

        val statusLabel = if (effectiveOpen) "OPEN NOW" else "CLOSED"

        val statusDetail = when {
            !manualIsOpen -> "Temporarily closed by vendor operator"
            isOpenByTime -> {
                val remaining = endMins - currentMins
                val rh = remaining / 60
                val rm = remaining % 60
                val remainingStr = if (rh > 0) "${rh}h ${rm}m" else "${rm}m"
                "Closes at $closeTimeFormatted ($remainingStr left)"
            }
            currentMins < startMins -> {
                val minsToOpen = startMins - currentMins
                val oh = minsToOpen / 60
                val om = minsToOpen % 60
                val timeToOpenStr = if (oh > 0) "${oh}h ${om}m" else "${om}m"
                "Opens at $openTimeFormatted (in $timeToOpenStr)"
            }
            else -> {
                "Closed for the day • Opens tomorrow at $openTimeFormatted"
            }
        }

        return VendorOperatingHoursInfo(
            vendorId = vendorId,
            scheduleText = scheduleText,
            isCurrentlyOpen = effectiveOpen,
            statusLabel = statusLabel,
            statusDetail = statusDetail,
            openTimeFormatted = openTimeFormatted,
            closeTimeFormatted = closeTimeFormatted
        )
    }
}
