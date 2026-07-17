package com.example.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object HapticHelper {
    private const val TAG = "HapticHelper"

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
     * Vibrate the device to simulate Capacitor Haptics impact effects
     */
    fun impact(context: Context, style: String = "MEDIUM") {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = when (style.uppercase()) {
                    "LIGHT" -> longArrayOf(0, 20)
                    "HEAVY" -> longArrayOf(0, 60)
                    else -> longArrayOf(0, 40) // MEDIUM
                }
                val amplitudes = when (style.uppercase()) {
                    "LIGHT" -> intArrayOf(0, 80)
                    "HEAVY" -> intArrayOf(0, 255)
                    else -> intArrayOf(0, 160) // MEDIUM
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                val duration = when (style.uppercase()) {
                    "LIGHT" -> 20L
                    "HEAVY" -> 60L
                    else -> 40L
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
            Log.d(TAG, "Capacitor Haptics: Simulating impact feedback (Style: $style)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform impact haptic feedback", e)
        }
    }

    /**
     * Vibrate the device to simulate Capacitor Haptics notification effects (Success, Warning, Error)
     */
    fun notification(context: Context, type: String = "SUCCESS") {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = when (type.uppercase()) {
                    "SUCCESS" -> longArrayOf(0, 40, 80, 40) // Two quick pulses
                    "WARNING" -> longArrayOf(0, 80, 100, 80) // Long quick pulses
                    "ERROR" -> longArrayOf(0, 60, 60, 60, 60, 120) // Three quick pulsing staccatos
                    else -> longArrayOf(0, 50)
                }
                val amplitudes = when (type.uppercase()) {
                    "SUCCESS" -> intArrayOf(0, 120, 0, 180)
                    "WARNING" -> intArrayOf(0, 180, 0, 180)
                    "ERROR" -> intArrayOf(0, 255, 0, 255, 0, 255)
                    else -> intArrayOf(0, 150)
                }
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                val timings = when (type.uppercase()) {
                    "SUCCESS" -> longArrayOf(0, 40, 80, 40)
                    "WARNING" -> longArrayOf(0, 80, 100, 80)
                    "ERROR" -> longArrayOf(0, 60, 60, 60, 60, 120)
                    else -> longArrayOf(0, 50)
                }
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
            Log.d(TAG, "Capacitor Haptics: Simulating notification feedback (Type: $type)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform notification haptic feedback", e)
        }
    }

    /**
     * Standard device vibration simulation
     */
    fun vibrate(context: Context, durationMs: Long = 200L) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
            Log.d(TAG, "Capacitor Haptics: Simulating vibrate feedback (${durationMs}ms)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform standard haptic vibration", e)
        }
    }
}
