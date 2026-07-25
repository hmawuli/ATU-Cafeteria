package com.example.ui.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.EnumMap

/**
 * Utility for generating high-resolution QR code Bitmaps using ZXing library,
 * facilitating instant order verification and payment validation for vendors.
 */
object QrCodeGeneratorUtil {

    /**
     * Generate an Android Bitmap representing a QR code encoding the provided text payload.
     *
     * @param content The string payload to encode in the QR Code.
     * @param widthPx Width in pixels of the output bitmap.
     * @param heightPx Height in pixels of the output bitmap.
     * @param qrColor ARGB Color of the QR dark modules (default Black).
     * @param bgColor ARGB Color of the QR background (default White).
     */
    fun generateQrBitmap(
        content: String,
        widthPx: Int = 512,
        heightPx: Int = 512,
        qrColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }

            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, widthPx, heightPx, hints)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height

            val pixels = IntArray(matrixWidth * matrixHeight)
            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) qrColor else bgColor
                }
            }

            Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate a Jetpack Compose ImageBitmap directly for immediate rendering in Composable Image components.
     */
    fun generateQrImageBitmap(
        content: String,
        sizePx: Int = 512
    ): ImageBitmap? {
        return generateQrBitmap(content, sizePx, sizePx)?.asImageBitmap()
    }
}
