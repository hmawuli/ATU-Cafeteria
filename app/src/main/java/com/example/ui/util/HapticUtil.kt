package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.util.Log

/**
 * Custom HapticUtil providing distinct vibrational patterns for payment confirmation,
 * order completion, and error states using Android's HapticFeedbackConstants and Vibrator APIs.
 */
object HapticUtil {
    private const val TAG = "HapticUtil"

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Trigger distinct pattern for Successful Payment.
     */
    fun performPaymentSuccess(context: Context, view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Two strong distinct pulses
                val timings = longArrayOf(0, 50, 60, 100)
                val amplitudes = intArrayOf(0, 200, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 60, 100), -1)
            }
            Log.d(TAG, "HapticUtil: Payment success vibration triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing payment success haptic", e)
        }
    }

    /**
     * Trigger distinct pattern for Order Completion / Ready status.
     */
    fun performOrderCompletion(context: Context, view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Staccato triple pulse
                val timings = longArrayOf(0, 40, 50, 40, 50, 80)
                val amplitudes = intArrayOf(0, 120, 0, 180, 0, 240)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 40, 50, 40, 50, 80), -1)
            }
            Log.d(TAG, "HapticUtil: Order completion vibration triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing order completion haptic", e)
        }
    }

    /**
     * Trigger distinct pattern for Error States or failed payments/orders.
     */
    fun performErrorState(context: Context, view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Heavy buzzing warning pattern
                val timings = longArrayOf(0, 80, 40, 80, 40, 150)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 80, 40, 80, 40, 150), -1)
            }
            Log.d(TAG, "HapticUtil: Error state vibration triggered.")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing error state haptic", e)
        }
    }

    /**
     * Trigger tactile haptic feedback for order button clicks and cart actions.
     */
    fun performOrderButtonHaptic(context: Context, view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing order button haptic", e)
        }
    }

    /**
     * Trigger tactile haptic feedback for barcode and QR code scanner actions.
     */
    fun performScanHaptic(context: Context, view: View? = null) {
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 60)
                val amplitudes = intArrayOf(0, 180, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 60), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing scan action haptic", e)
        }
    }
}
