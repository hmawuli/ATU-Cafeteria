package com.example.ui.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import android.content.ClipboardManager
import android.content.ClipData
import com.example.data.Order
import com.example.data.User
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val dateOnlyFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayOfWeekFormatter = SimpleDateFormat("EEEE", Locale.US)

    /**
     * Helper to escape CSV values correctly to avoid breaking on commas, line breaks or quotes.
     */
    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        if (str.contains(",") || str.contains("\n") || str.contains("\"") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\""
        }
        return str
    }

    /**
     * Generates a CSV representation of the orders.
     */
    fun generateOrdersCsv(orders: List<Order>, users: List<User>): String {
        val userMap = users.associate { it.id to it.fullName }
        val sb = StringBuilder()
        
        // Headers
        sb.append("Order ID,Customer ID,Customer Name,Food Name,Quantity,Unit Price (GH₵),Total Price (GH₵),Status,Pickup PIN,Estimated Pickup Time,Timestamp\n")
        
        orders.sortedByDescending { it.orderTimestamp }.forEach { order ->
            val customerName = userMap[order.customerId] ?: "Student"
            val readableDate = dateFormatter.format(Date(order.orderTimestamp))
            
            sb.append(escapeCsv(order.id)).append(",")
              .append(escapeCsv(order.customerId)).append(",")
              .append(escapeCsv(customerName)).append(",")
              .append(escapeCsv(order.foodName)).append(",")
              .append(escapeCsv(order.quantity)).append(",")
              .append(escapeCsv(order.unitPrice)).append(",")
              .append(escapeCsv(order.totalPrice)).append(",")
              .append(escapeCsv(order.status)).append(",")
              .append(escapeCsv(order.pickupPin)).append(",")
              .append(escapeCsv(order.estimatedPickupTime)).append(",")
              .append(escapeCsv(readableDate)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Generates a CSV representation of the 7-day revenue trend report.
     */
    fun generateRevenueReportCsv(orders: List<Order>): String {
        val sb = StringBuilder()
        
        // Headers
        sb.append("Date,Day of Week,Total Orders,Completed Orders,Total Revenue (GH₵),Average Order Value (GH₵)\n")
        
        // Aggregate completed orders by date for the last 7 calendar days
        val calendar = Calendar.getInstance()
        val daysList = mutableListOf<Date>()
        
        for (i in 0 until 7) {
            val d = calendar.time
            daysList.add(d)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        // Let's sort descending (starting with today)
        daysList.forEach { date ->
            val dateStr = dateOnlyFormatter.format(date)
            val dayName = dayOfWeekFormatter.format(date)
            
            // Filter orders for this day
            val dayOrders = orders.filter { order ->
                val orderDateStr = dateOnlyFormatter.format(Date(order.orderTimestamp))
                orderDateStr == dateStr
            }
            
            val completedOrders = dayOrders.filter { it.status == "COMPLETED" }
            val totalOrdersCount = dayOrders.size
            val completedCount = completedOrders.size
            val totalRevenue = completedOrders.sumOf { it.totalPrice }
            val averageOrderValue = if (completedCount > 0) totalRevenue / completedCount else 0.0
            
            sb.append(escapeCsv(dateStr)).append(",")
              .append(escapeCsv(dayName)).append(",")
              .append(escapeCsv(totalOrdersCount)).append(",")
              .append(escapeCsv(completedCount)).append(",")
              .append(String.format(Locale.US, "%.2f", totalRevenue)).append(",")
              .append(String.format(Locale.US, "%.2f", averageOrderValue)).append("\n")
        }
        return sb.toString()
    }

    /**
     * Saves CSV file to the system Downloads folder.
     */
    fun saveCsvToDownloads(context: Context, fileName: String, csvContent: String): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        
        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        try {
            val uri = resolver.insert(collectionUri, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Successfully Exported:\n$fileName saved to Downloads Folder", Toast.LENGTH_LONG).show()
                return true
            } else {
                Toast.makeText(context, "Failed to initialize download item.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export Failure: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return false
    }

    /**
     * Shares CSV content via local system share sheet or falls back to system clipboard.
     */
    fun shareCsvData(context: Context, subject: String, csvContent: String) {
        try {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, csvContent)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Report via:")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            copyToClipboard(context, "Exported Data", csvContent)
        }
    }

    /**
     * Copies CSV contents to user's system clipboard.
     */
    fun copyToClipboard(context: Context, label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Copied report data to Clipboard successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to copy: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
