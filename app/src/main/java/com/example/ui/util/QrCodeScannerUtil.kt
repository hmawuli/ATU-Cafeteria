package com.example.ui.util

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/**
 * Utility class leveraging ZXing library to scan and decode QR code payloads 
 * for rapid food collection verification at vendor stalls.
 */
object QrCodeScannerUtil {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
            put(DecodeHintType.TRY_HARDER, true)
        }
        setHints(hints)
    }

    /**
     * Decodes a QR code string payload from an Android Bitmap using ZXing's MultiFormatReader.
     *
     * @param bitmap The Bitmap image containing a QR code to decode.
     * @return Decoded String content if successful, null otherwise.
     */
    fun decodeQrBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val luminanceSource = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
            val result = reader.decodeWithState(binaryBitmap)
            reader.reset()
            result.text
        } catch (e: Exception) {
            reader.reset()
            null
        }
    }

    /**
     * Helper to validate if scanned payload matches valid ATU Cafeteria pickup token or counter format.
     */
    fun isValidPickupToken(scannedText: String): Boolean {
        return scannedText.startsWith("ATU-ORDER-") ||
               scannedText.startsWith("ATU-COUNTER-") ||
               scannedText.startsWith("ORDER-") ||
               scannedText.contains("ATU_CAFETERIA_")
    }
}
