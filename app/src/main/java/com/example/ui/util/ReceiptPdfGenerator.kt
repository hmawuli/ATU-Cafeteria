package com.example.ui.util

import com.example.data.Order

fun generatePdfReceipt(context: android.content.Context, order: com.example.data.Order) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(300, 480, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLUE
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val headerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Draw background
        canvas.drawColor(android.graphics.Color.WHITE)

        var y = 35f
        // Draw elegant receipt header
        canvas.drawText("ACCRA TECHNICAL UNIVERSITY", 15f, y, titlePaint)
        y += 18f
        canvas.drawText("Official ATU Cafeteria Receipt", 15f, y, headerPaint)
        y += 20f

        // Draw line separator
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 2f
        canvas.drawLine(15f, y, 285f, y, paint)
        y += 20f

        // Receipt details
        textPaint.isFakeBoldText = true
        canvas.drawText("Receipt Code: ATU-TKT-${order.id}", 15f, y, textPaint)
        y += 16f
        textPaint.isFakeBoldText = false
        
        val dateFormated = try {
            java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(order.orderTimestamp))
        } catch(e: Exception) {
            "Date Code: [Live Order]"
        }
        canvas.drawText("Date of Transaction: $dateFormated", 15f, y, textPaint)
        y += 16f
        canvas.drawText("Pickup Status: ${order.status.uppercase()}", 15f, y, textPaint)
        y += 16f
        canvas.drawText("Verification PIN Code: ${order.pickupPin}", 15f, y, textPaint)
        y += 20f

        canvas.drawLine(15f, y, 285f, y, paint)
        y += 20f

        // Meal Item Details Header
        headerPaint.textSize = 11f
        canvas.drawText("MEAL ORDER ITEMS", 15f, y, headerPaint)
        y += 18f

        // Dish details
        canvas.drawText("Dish: ${order.foodName}", 15f, y, textPaint)
        y += 16f
        canvas.drawText("Quantity Bought: ${order.quantity}", 15f, y, textPaint)
        y += 16f
        canvas.drawText("Unit Price: GH₵ ${String.format("%.2f", order.unitPrice)}", 15f, y, textPaint)
        y += 20f

        canvas.drawLine(15f, y, 285f, y, paint)
        y += 20f

        // Summary Price
        val summaryLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("TOTAL AMOUNT PAID:", 15f, y, summaryLabelPaint)
        y += 18f
        val summaryPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#2E7D32")
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("GH₵ ${String.format("%.2f", order.totalPrice)}", 15f, y, summaryPaint)
        y += 35f

        // Footer block
        val footerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 8f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Thank you for choosing ATU culinary service!", 150f, y, footerPaint)
        y += 12f
        canvas.drawText("Accra Technical University Cafeteria Hub", 150f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF
        val fileName = "ATU_Receipt_Order_${order.id}.pdf"
        val file = java.io.File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()

        // Share/View via FileProvider intent
        val authority = "com.example.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "ATU Cafeteria Receipt - Order #${order.id}")
            putExtra(android.content.Intent.EXTRA_TEXT, "Here is your electronic meal audit receipt.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = android.content.Intent.createChooser(intent, "Download / View Receipt PDF")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        
        android.widget.Toast.makeText(context, "Receipt PDF generated and saved successfully!", android.widget.Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error printing receipt: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun generatePdfOrderHistoryReport(context: android.content.Context, studentName: String, orders: List<com.example.data.Order>) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        val calculatedHeight = kotlin.math.max(600, 160 + orders.size * 30 + 100)
        // A4 width is 595. Let's use 600 width for a nice tabular report layout
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(600, calculatedHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = android.graphics.Paint()
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1A237E") // Deep Blue
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subTitlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#455A64")
            textSize = 11f
            isAntiAlias = true
        }
        val tableHeaderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Draw background
        canvas.drawColor(android.graphics.Color.WHITE)

        var y = 40f
        // Draw elegant header
        canvas.drawText("ACCRA TECHNICAL UNIVERSITY - CAFETERIA HUB", 20f, y, titlePaint)
        y += 20f
        canvas.drawText("Student Order History Summary Report", 20f, y, subTitlePaint)
        y += 18f
        
        val dateString = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        canvas.drawText("Student Name: $studentName  |  Generated On: $dateString", 20f, y, textPaint)
        y += 22f

        // Table Header Background
        paint.color = android.graphics.Color.parseColor("#37474F")
        canvas.drawRect(20f, y, 580f, y + 24f, paint)

        // Column Titles
        val cols = listOf("Order ID" to 25f, "Date" to 90f, "Dish/Meal Item" to 190f, "Qty" to 340f, "Unit" to 380f, "Total" to 430f, "Status" to 500f)
        for (col in cols) {
            canvas.drawText(col.first, col.second, y + 16f, tableHeaderPaint)
        }
        y += 24f

        var totalSpent = 0.0
        val cellPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 9f
            isAntiAlias = true
        }
        val borderPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CFD8DC")
            strokeWidth = 1f
            style = android.graphics.Paint.Style.STROKE
        }

        // Draw order history rows
        for (i in orders.indices) {
            val order = orders[i]
            totalSpent += order.totalPrice

            // Alternating backgrounds for rows
            if (i % 2 == 1) {
                paint.color = android.graphics.Color.parseColor("#F5F7F8")
                canvas.drawRect(20f, y, 580f, y + 26f, paint)
            }

            // Draw cell text
            canvas.drawText("#${order.id}", 25f, y + 17f, cellPaint)
            
            val formattedDate = try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.orderTimestamp))
            } catch (e: Exception) {
                "Unknown"
            }
            canvas.drawText(formattedDate, 90f, y + 17f, cellPaint)
            
            // Handle potentially long meal name
            val displayMeal = if (order.foodName.length > 24) order.foodName.substring(0, 22) + ".." else order.foodName
            canvas.drawText(displayMeal, 190f, y + 17f, cellPaint)
            
            canvas.drawText(order.quantity.toString(), 340f, y + 17f, cellPaint)
            canvas.drawText("GH₵${String.format("%.2f", order.unitPrice)}", 380f, y + 17f, cellPaint)
            canvas.drawText("GH₵${String.format("%.2f", order.totalPrice)}", 430f, y + 17f, cellPaint)
            
            val statusPaint = android.graphics.Paint().apply {
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
                color = when (order.status.uppercase()) {
                    "COMPLETED" -> android.graphics.Color.parseColor("#2E7D32")
                    "READY" -> android.graphics.Color.parseColor("#1565C0")
                    "PREPARING" -> android.graphics.Color.parseColor("#EF6C00")
                    "PENDING" -> android.graphics.Color.parseColor("#F57F17")
                    else -> android.graphics.Color.parseColor("#C62828")
                }
            }
            canvas.drawText(order.status.uppercase(), 500f, y + 17f, statusPaint)

            // Horizontal gridline
            canvas.drawLine(20f, y + 26f, 580f, y + 26f, borderPaint)
            y += 26f
        }

        // Draw bottom table border
        canvas.drawLine(20f, y, 580f, y, borderPaint)
        y += 20f

        // Total Aggregate Summary Row
        val sumLabelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val sumValPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#2E7D32")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
        }

        canvas.drawText("Total Number of Orders: ${orders.size}", 20f, y, sumLabelPaint)
        canvas.drawText("Aggregate Expenditure: GH₵ ${String.format("%.2f", totalSpent)}", 340f, y, sumValPaint)
        y += 35f

        // Footer block
        val footerPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 8f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("This is an official transaction record compiled by ATU Cafeteria Hub.", 300f, y, footerPaint)
        y += 12f
        canvas.drawText("Please contact support or individual food vendors if you have any feedback or queries.", 300f, y, footerPaint)

        pdfDocument.finishPage(page)

        // Save PDF
        val fileName = "ATU_Order_History_Report.pdf"
        val file = java.io.File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()

        // Share/View via FileProvider intent
        val authority = "com.example.fileprovider"
        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "ATU Cafeteria - Student Order History")
            putExtra(android.content.Intent.EXTRA_TEXT, "Hello! Please find attached the PDF report detailing my filtered historical orders on ATU Cafeteria Hub.")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = android.content.Intent.createChooser(intent, "Download / View PDF History")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
        
        android.widget.Toast.makeText(context, "History Report PDF generated and saved successfully!", android.widget.Toast.LENGTH_LONG).show()

    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Error generating history report: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}
