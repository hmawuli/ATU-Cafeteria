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
