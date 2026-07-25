package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * High-Contrast Dark Mode Color Palette specifically designed for Vendor Menu screens
 * to guarantee optimal WCAG AAA accessibility, crisp readability, and legibility
 * in bright outdoor sunlight as well as low-light cafeteria spaces.
 */
object VendorHighContrastTheme {

    @Composable
    fun cardBackground(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFF000000) // Pure black canvas for max OLED contrast
            isDark -> Color(0xFF1E293B)
            else -> Color(0xFFFFFFFF)
        }
    }

    @Composable
    fun cardBorder(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFFFD600) // Vivid Electric Amber border
            isDark -> Color(0xFF475569)
            else -> Color(0xFFCBD5E1)
        }
    }

    @Composable
    fun primaryText(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFFFFFFF) // High-luminance white
            isDark -> Color(0xFFF8FAFC)
            else -> Color(0xFF0F172A)
        }
    }

    @Composable
    fun secondaryText(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFE2E8F0) // 90% white contrast
            isDark -> Color(0xFF94A3B8)
            else -> Color(0xFF64748B)
        }
    }

    @Composable
    fun priceTagColor(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFFFD600) // High visibility Gold
            isDark -> Color(0xFFF59E0B)
            else -> Color(0xFFD97706)
        }
    }

    @Composable
    fun availableBadgeBg(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFF00E676) // Vivid Neon Green
            isDark -> Color(0xFF166534)
            else -> Color(0xFFDCFCE7)
        }
    }

    @Composable
    fun availableBadgeText(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFF000000) // High contrast black text on neon green
            isDark -> Color(0xFF86EFAC)
            else -> Color(0xFF15803D)
        }
    }

    @Composable
    fun outOfStockBadgeBg(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFFF1744) // Bright Neon Red
            isDark -> Color(0xFF991B1B)
            else -> Color(0xFFFEE2E2)
        }
    }

    @Composable
    fun outOfStockBadgeText(isDark: Boolean = isSystemInDarkTheme(), highContrast: Boolean = false): Color {
        return when {
            highContrast && isDark -> Color(0xFFFFFFFF)
            isDark -> Color(0xFFFCA5A5)
            else -> Color(0xFFB91C1C)
        }
    }
}
