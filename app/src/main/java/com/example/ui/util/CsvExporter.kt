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
import com.example.data.Feedback
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
     * Generates a CSV representation of the vendor performance and quality metrics report.
     */
    fun generatePerformanceReportCsv(
        vendorId: Int,
        vendorName: String,
        orders: List<Order>,
        feedbacks: List<Feedback>
    ): String {
        val sb = StringBuilder()
        sb.append("Vendor Performance Audit Report\n")
        sb.append("Vendor Name,ATU-$vendorId - $vendorName\n")
        sb.append("Generated On,${dateFormatter.format(Date())}\n\n")

        val completedOrders = orders.filter { it.status.uppercase() == "COMPLETED" }
        val totalRevenue = completedOrders.sumOf { it.totalPrice }
        
        // Calculate average ratings from feedbacks
        val avgFoodQuality = if (feedbacks.isNotEmpty()) feedbacks.map { it.ratingFoodQuality }.average() else 0.0
        val avgCleanliness = if (feedbacks.isNotEmpty()) feedbacks.map { it.ratingCleanliness }.average() else 0.0
        val avgSpeed = if (feedbacks.isNotEmpty()) feedbacks.map { it.ratingServiceSpeed }.average() else 0.0
        val avgValue = if (feedbacks.isNotEmpty()) feedbacks.map { it.ratingPriceValue }.average() else 0.0
        val avgOverall = if (feedbacks.isNotEmpty()) {
            feedbacks.map { (it.ratingFoodQuality + it.ratingCleanliness + it.ratingServiceSpeed + it.ratingPriceValue) / 4.0 }.average()
        } else 0.0

        sb.append("=== SUMMARY METRICS ===\n")
        sb.append("Indicator,Value\n")
        sb.append("Total Orders Placed,${orders.size}\n")
        sb.append("Total Fulfillments Completed,${completedOrders.size}\n")
        sb.append("Fulfillment Rate (%),${if (orders.isNotEmpty()) "%.1f".format(completedOrders.size.toDouble() / orders.size * 100.0) else "0.0"}\n")
        sb.append("Total Revenue Generated (GH\u20B5),${"%.2f".format(totalRevenue)}\n")
        sb.append("Food Quality Score (Out of 5),${"%.2f".format(avgFoodQuality)}\n")
        sb.append("Cleanliness Score (Out of 5),${"%.2f".format(avgCleanliness)}\n")
        sb.append("Service Speed Score (Out of 5),${"%.2f".format(avgSpeed)}\n")
        sb.append("Price Value Score (Out of 5),${"%.2f".format(avgValue)}\n")
        sb.append("Overall Satisfactory Index (Out of 5),${"%.2f".format(avgOverall)}\n\n")

        sb.append("=== DETAILED ORDER CONTRIBUTIONS ===\n")
        sb.append("Order ID,Food Item,Quantity,Total Sales (GH\u20B5),Status,Time\n")
        orders.sortedByDescending { o -> o.orderTimestamp }.forEach { o ->
            sb.append(escapeCsv(o.id)).append(",")
              .append(escapeCsv(o.foodName)).append(",")
              .append(escapeCsv(o.quantity)).append(",")
              .append(String.format(Locale.US, "%.2f", o.totalPrice)).append(",")
              .append(escapeCsv(o.status)).append(",")
              .append(escapeCsv(dateFormatter.format(Date(o.orderTimestamp)))).append("\n")
        }
        sb.append("\n=== CUSTOMER REVIEWS TRANSCRIPT ===\n")
        sb.append("Review ID,Food Quality Stars,Cleanliness Stars,Service Speed Stars,Price Value Stars,Comment\n")
        feedbacks.sortedByDescending { f -> f.id }.forEach { f ->
            sb.append(escapeCsv(f.id)).append(",")
              .append(escapeCsv(f.ratingFoodQuality)).append(",")
              .append(escapeCsv(f.ratingCleanliness)).append(",")
              .append(escapeCsv(f.ratingServiceSpeed)).append(",")
              .append(escapeCsv(f.ratingPriceValue)).append(",")
              .append(escapeCsv(f.comment)).append("\n")
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

    /**
     * Filters a list of orders by a designated relative time period.
     */
    fun filterOrdersByPeriod(orders: List<Order>, period: String): List<Order> {
        val now = System.currentTimeMillis()
        val limitMs = when (period) {
            "Past 24 Hours" -> 24 * 60 * 60 * 1000L
            "Past 7 Days" -> 7 * 24 * 60 * 60 * 1000L
            "Past 30 Days" -> 30 * 24 * 60 * 60 * 1000L
            else -> Long.MAX_VALUE
        }
        if (limitMs == Long.MAX_VALUE) return orders
        return orders.filter { now - it.orderTimestamp <= limitMs }
    }

    /**
     * Generates and saves a PDF report for detailed order history & sales data to the system Downloads folder.
     */
    fun exportOrdersPdf(
        context: Context,
        fileName: String,
        vendorName: String,
        period: String,
        orders: List<Order>
    ): Boolean {
        try {
            val pdfDocument = android.graphics.pdf.PdfDocument()
            // Standard A4 size is 595 x 842 pt
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = android.graphics.Paint()

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val currentTimeStr = dateFormatter.format(Date())

            val completedOrders = orders.filter { it.status == "COMPLETED" }
            val totalRevenue = completedOrders.sumOf { it.totalPrice }
            val totalQtyOrdered = orders.sumOf { it.quantity }

            // 1. Drawing the Title Header
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(0x1B, 0x5E, 0x20) // Deep dark green compliance color
            paint.textSize = 17f
            paint.isFakeBoldText = true
            canvas.drawText("OFFICIAL ORDER SALES STATEMENT", 40f, 60f, paint)

            paint.textSize = 9f
            paint.isFakeBoldText = false
            paint.color = android.graphics.Color.GRAY
            canvas.drawText("Scope: $period | Compiled On: $currentTimeStr", 40f, 80f, paint)

            paint.color = android.graphics.Color.rgb(0xE0, 0xE0, 0xE0)
            canvas.drawLine(40f, 95f, 555f, 95f, paint)

            // 2. Identity info
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("Vendor Name: $vendorName", 40f, 115f, paint)
            paint.isFakeBoldText = false
            canvas.drawText("Compliance ID Reference: ATU-VND-${vendorName.hashCode().let { if (it < 0) -it else it } % 100000}", 40f, 130f, paint)

            // 3. Highlight Stats grid dashboard
            paint.color = android.graphics.Color.rgb(0xF1, 0xF8, 0xE9) // Subtle light green background
            paint.style = android.graphics.Paint.Style.FILL
            canvas.drawRect(40f, 145f, 555f, 215f, paint)

            paint.color = android.graphics.Color.rgb(0x7C, 0xD1, 0x72)
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(40f, 145f, 555f, 215f, paint)

            paint.style = android.graphics.Paint.Style.FILL
            paint.color = android.graphics.Color.rgb(0x33, 0x69, 0x1E)
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("SALES PERIOD METRICS", 50f, 162f, paint)

            paint.color = android.graphics.Color.BLACK
            paint.isFakeBoldText = false
            canvas.drawText("Total Registered Orders: ${orders.size}", 50f, 180f, paint)
            canvas.drawText("Completions Fulfillments: ${completedOrders.size}", 50f, 195f, paint)

            canvas.drawText("Total Units Sold: $totalQtyOrdered", 300f, 180f, paint)
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.rgb(0x1B, 0x5E, 0x20)
            canvas.drawText("Total Revenue: GH₵ ${"%.2f".format(totalRevenue)}", 300f, 195f, paint)

            // 4. Ledger Table header
            paint.color = android.graphics.Color.rgb(0x42, 0x42, 0x42)
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("COMPENSATED TRANSACTION LEDGER", 40f, 245f, paint)

            paint.color = android.graphics.Color.rgb(0xE0, 0xE0, 0xE0)
            canvas.drawLine(40f, 252f, 555f, 252f, paint)

            paint.color = android.graphics.Color.BLACK
            paint.textSize = 8f
            paint.isFakeBoldText = true
            canvas.drawText("ID", 40f, 267f, paint)
            canvas.drawText("DATE & TIME", 80f, 267f, paint)
            canvas.drawText("ITEM DESCRIPTION", 190f, 267f, paint)
            canvas.drawText("QTY", 370f, 267f, paint)
            canvas.drawText("REVENUE", 415f, 267f, paint)
            canvas.drawText("STATUS", 490f, 267f, paint)

            canvas.drawLine(40f, 273f, 555f, 273f, paint)

            paint.isFakeBoldText = false
            var currentY = 288f
            // Sort orders so recent completed are prominent
            val ordersToDraw = orders.sortedByDescending { it.orderTimestamp }.take(22) // Fits neatly on one A4 page along with footer

            paint.style = android.graphics.Paint.Style.FILL
            for (order in ordersToDraw) {
                paint.color = android.graphics.Color.BLACK
                canvas.drawText("#${order.id}", 40f, currentY, paint)

                val dateStr = dateFormatter.format(Date(order.orderTimestamp))
                canvas.drawText(dateStr, 80f, currentY, paint)

                val shortName = if (order.foodName.length > 25) order.foodName.substring(0, 22) + "..." else order.foodName
                canvas.drawText(shortName, 190f, currentY, paint)

                canvas.drawText("${order.quantity}x", 370f, currentY, paint)
                canvas.drawText("GH₵ ${"%.2f".format(order.totalPrice)}", 415f, currentY, paint)

                // Highlight status color
                when (order.status) {
                    "COMPLETED" -> paint.color = android.graphics.Color.rgb(0x2E, 0x7D, 0x32)
                    "PENDING", "PREPARING" -> paint.color = android.graphics.Color.rgb(0xEF, 0x6C, 0x00)
                    else -> paint.color = android.graphics.Color.rgb(0xC6, 0x28, 0x28)
                }
                canvas.drawText(order.status, 490f, currentY, paint)

                currentY += 21f
            }

            if (orders.size > 22) {
                paint.color = android.graphics.Color.GRAY
                paint.isFakeBoldText = true
                paint.textSize = 8.5f
                canvas.drawText("* Showing 22 most recent entries. View matching CSV file for all ${orders.size} rows.", 40f, currentY + 10f, paint)
            }

            // 5. Drawing the Footer Info
            paint.color = android.graphics.Color.GRAY
            paint.isFakeBoldText = false
            paint.textSize = 8f
            canvas.drawLine(40f, 800f, 555f, 800f, paint)
            canvas.drawText("Accra Technical University Dining Smart-Kiosk System • Audit Compliant Ledger", 40f, 815f, paint)
            canvas.drawText("Page 1 of 1", 510f, 815f, paint)

            pdfDocument.finishPage(page)

            // Save PDF to downloads
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external")
            }

            val uri = resolver.insert(collectionUri, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(context, "Successfully Exported:\n$fileName saved to Downloads Folder", Toast.LENGTH_LONG).show()
                return true
            } else {
                Toast.makeText(context, "Failed to initialize PDF download item.", Toast.LENGTH_LONG).show()
            }
            pdfDocument.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF Export Failure: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return false
    }
}
