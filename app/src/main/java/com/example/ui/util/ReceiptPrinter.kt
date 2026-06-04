package com.example.ui.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPrinter {

    /**
     * Triggers the Android Print Spooler to print a receipt for the given order.
     * Uses WebView to render a robust, highly styleable, monochrome thermal-paper-friendly HTML document.
     */
    fun printOrderReceipt(context: Context, order: Order, vendorName: String) {
        try {
            // Must run on the UI (Main) thread
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager == null) {
                        Toast.makeText(context, "System print manager is not available.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val jobName = "ATU_Cafeteria_Receipt_Order_${order.id}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    
                    // Build print attributes (portrait, thermal roll or monochromatic)
                    val attributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)  // Compact size fits thermal receipts well
                        .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, attributes)
                }
            }

            val htmlContent = generateReceiptHtml(order, vendorName)
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to initialize print spooler: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Builds a highly readable, classic thermal receipt print document layout in monochrome HTML.
     */
    private fun generateReceiptHtml(order: Order, vendorName: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentDateStr = dateFormat.format(Date(order.orderTimestamp))
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <title>Order Receipt #${order.id}</title>
                <style>
                    @page {
                        margin: 4mm 6mm;
                    }
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        font-size: 11pt;
                        line-height: 1.3;
                        color: #000000;
                        background-color: #ffffff;
                        margin: 0;
                        padding: 0;
                        width: 100%;
                    }
                    .text-center {
                        text-align: center;
                    }
                    .text-right {
                        text-align: right;
                    }
                    .bold {
                        font-weight: bold;
                    }
                    .header {
                        margin-bottom: 6mm;
                        padding-bottom: 2mm;
                    }
                    .brand-title {
                        font-size: 15pt;
                        font-weight: 900;
                        margin: 0;
                        letter-spacing: 0.5px;
                        text-transform: uppercase;
                    }
                    .sub-brand {
                        font-size: 9pt;
                        margin: 2px 0 0 0;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                    }
                    .dashed-line {
                        border-top: 1px dashed #000000;
                        margin: 4mm 0;
                        height: 0;
                    }
                    .metadata-table {
                        width: 100%;
                        font-size: 10pt;
                        margin-bottom: 4mm;
                    }
                    .metadata-table td {
                        padding: 1.5px 0;
                        vertical-align: top;
                    }
                    .items-table {
                        width: 100%;
                        border-collapse: collapse;
                        font-size: 10.5pt;
                        margin: 4mm 0;
                    }
                    .items-table th {
                        border-bottom: 1px dashed #000000;
                        padding-bottom: 2mm;
                        font-weight: bold;
                        font-size: 10pt;
                    }
                    .items-table td {
                        padding: 3mm 0;
                        vertical-align: top;
                    }
                    .totals-container {
                        width: 100%;
                        font-size: 11pt;
                        margin-top: 2mm;
                    }
                    .totals-container td {
                        padding: 2px 0;
                    }
                    .grand-total {
                        font-size: 13pt;
                        font-weight: bold;
                        border-top: 1px dashed #000000;
                        border-bottom: 1px dashed #000000;
                        padding: 3mm 0 !important;
                    }
                    .pin-section {
                        border: 2px solid #000000;
                        margin: 6mm auto;
                        padding: 4mm 2mm;
                        text-align: center;
                        width: 90%;
                    }
                    .pin-label {
                        font-size: 9pt;
                        font-weight: bold;
                        text-transform: uppercase;
                        margin-bottom: 1.5mm;
                        letter-spacing: 2px;
                    }
                    .pin-value {
                        font-size: 20pt;
                        font-weight: 900;
                        letter-spacing: 8px;
                    }
                    .footer {
                        font-size: 8.5pt;
                        margin-top: 6mm;
                        padding-top: 4mm;
                        border-top: 1px dashed #000000;
                    }
                    .barcode-simulation {
                        font-size: 8pt;
                        letter-spacing: 1.5px;
                        margin: 4mm 0 1mm 0;
                    }
                </style>
            </head>
            <body>
                <!-- RECEIPT HEADER -->
                <div class="header text-center">
                    <p class="brand-title">${vendorName}</p>
                    <p class="sub-brand">Accra Technical University</p>
                    <p style="font-size: 8pt; margin: 2px 0;">MANUAL FULFILLMENT COPY</p>
                </div>

                <div class="dashed-line"></div>

                <!-- METADATA/INFO -->
                <table class="metadata-table">
                    <tr>
                        <td class="bold" style="width: 40%;">ORDER NUMBER:</td>
                        <td class="bold text-right" style="font-size: 12pt;">#${order.id}</td>
                    </tr>
                    <tr>
                        <td>DATE/TIME:</td>
                        <td class="text-right">${currentDateStr}</td>
                    </tr>
                    <tr>
                        <td>CUSTOMER:</td>
                        <td class="text-right">Student ID #${order.customerId}</td>
                    </tr>
                    <tr>
                        <td>EST. PICKUP:</td>
                        <td class="text-right bold">${order.estimatedPickupTime}</td>
                    </tr>
                    <tr>
                        <td>MODE:</td>
                        <td class="text-right">Cashless Wallet</td>
                    </tr>
                </table>

                <div class="dashed-line"></div>

                <!-- ITEMS TABLE -->
                <table class="items-table">
                    <thead>
                        <tr>
                            <th class="bold" style="text-align: left; width: 60%;">ITEM DECRIPTION</th>
                            <th class="bold text-center" style="width: 15%;">QTY</th>
                            <th class="bold text-right" style="width: 25%;">AMOUNT</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="bold">${order.foodName}</td>
                            <td class="text-center">${order.quantity}</td>
                            <td class="text-right">GH₵${"%.2f".format(order.totalPrice)}</td>
                        </tr>
                        <tr>
                            <td colspan="3" style="font-size: 8.5pt; padding-top: 0; color: #444444;">
                                Unit Price: GH₵${"%.2f".format(order.unitPrice)} ea
                            </td>
                        </tr>
                    </tbody>
                </table>

                <!-- TOTALS -->
                <table class="totals-container">
                    <tr>
                        <td style="width: 60%;">SUBTOTAL</td>
                        <td class="text-right">GH₵${"%.2f".format(order.totalPrice)}</td>
                    </tr>
                    <tr>
                        <td>CAMPUS TAX / VAT (0%)</td>
                        <td class="text-right">GH₵0.00</td>
                    </tr>
                    <tr class="grand-total">
                        <td class="bold">TOTAL PAID</td>
                        <td class="bold text-right">GH₵${"%.2f".format(order.totalPrice)}</td>
                    </tr>
                </table>

                <!-- SECURE HANDOFF TICKET PIN -->
                <div class="pin-section">
                    <div class="pin-label">Secure PickUp PIN</div>
                    <div class="pin-value">${order.pickupPin}</div>
                </div>

                <!-- BARCODE AND FOOTER -->
                <div class="text-center footer">
                    <div class="barcode-simulation">
                        ||| | || ||||| | |||| | ||| | |||
                        <br/>
                        *ATU-${order.id}-${order.pickupPin}*
                    </div>
                    <p style="margin: 4mm 0 1mm 0; font-weight: bold;">VERIFICATION REQUIRED FOR HANDOFF</p>
                    <p style="margin: 0; font-size: 8pt;">Campus Dining Smart-Kiosk System</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
