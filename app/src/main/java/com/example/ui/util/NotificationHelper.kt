package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ORDERS = "atu_orders_channel"
    const val CHANNEL_PROMO = "atu_promotions_channel"
    const val CHANNEL_INVENTORY = "atu_vendor_inventory_channel"

    private const val PREFS_NAME = "atu_notification_prefs"
    private const val KEY_SOUND_ENABLED = "key_notif_sound_enabled"
    private const val KEY_VIBRATION_ENABLED = "key_notif_vibration_enabled"
    private const val KEY_ORDER_STATUS_ALERTS = "key_order_status_alerts"
    private const val KEY_PROMO_ALERTS = "key_promo_alerts"
    private const val KEY_VENDOR_INVENTORY_PREFIX = "key_vendor_inventory_notif_"

    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        createNotificationChannels(context)
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_VIBRATION_ENABLED, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        createNotificationChannels(context)
    }

    fun isOrderStatusAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ORDER_STATUS_ALERTS, true)
    }

    fun setOrderStatusAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ORDER_STATUS_ALERTS, enabled).apply()
    }

    fun isPromotionalAlertsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_PROMO_ALERTS, true)
    }

    fun setPromotionalAlertsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_PROMO_ALERTS, enabled).apply()
    }

    fun isVendorInventoryAlertsEnabled(context: Context, vendorId: Int): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_VENDOR_INVENTORY_PREFIX + vendorId, true)
    }

    fun setVendorInventoryAlertsEnabled(context: Context, vendorId: Int, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_VENDOR_INVENTORY_PREFIX + vendorId, enabled).apply()
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundOn = isSoundEnabled(context)
            val vibrationOn = isVibrationEnabled(context)

            val soundUri = if (soundOn) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Order Status Updates",
                if (soundOn) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Real-time notifications for meal status, kitchen updates, and ready pickups"
                enableVibration(vibrationOn)
                if (vibrationOn) {
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                }
                if (soundUri != null) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            val promoChannel = NotificationChannel(
                CHANNEL_PROMO,
                "Promotional Offers & Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about campus food court discounts and vendor special menus"
                enableVibration(vibrationOn)
                if (vibrationOn) {
                    vibrationPattern = longArrayOf(0, 150, 100, 150)
                }
                if (soundUri != null) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            val inventoryChannel = NotificationChannel(
                CHANNEL_INVENTORY,
                "Vendor Inventory Low Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time push alerts sent when cafeteria menu item stock falls below safety thresholds"
                enableVibration(vibrationOn)
                if (vibrationOn) {
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                }
                if (soundUri != null) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(ordersChannel)
            manager.createNotificationChannel(promoChannel)
            manager.createNotificationChannel(inventoryChannel)
        }
    }

    fun openChannelSettings(context: Context, channelId: String = CHANNEL_ORDERS) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    fun sendOrderStatusNotification(context: Context, orderId: Int, title: String, text: String) {
        if (!isOrderStatusAlertsEnabled(context)) return
        val soundOn = isSoundEnabled(context)
        val vibrationOn = isVibrationEnabled(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (soundOn) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSound(null)
        }

        if (vibrationOn) {
            builder.setVibrate(longArrayOf(0, 300, 150, 300))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(orderId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendPromotionalNotification(context: Context, notificationId: Int, title: String, text: String) {
        if (!isPromotionalAlertsEnabled(context)) return
        val soundOn = isSoundEnabled(context)
        val vibrationOn = isVibrationEnabled(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_PROMO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        if (soundOn) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSound(null)
        }

        if (vibrationOn) {
            builder.setVibrate(longArrayOf(0, 150, 100, 150))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendVendorInventoryAlertNotification(
        context: Context,
        foodId: Int,
        itemName: String,
        currentStock: Int,
        threshold: Int,
        vendorId: Int = 1
    ) {
        if (!isVendorInventoryAlertsEnabled(context, vendorId)) return
        val soundOn = isSoundEnabled(context)
        val vibrationOn = isVibrationEnabled(context)

        val title = "⚠️ LOW STOCK ALERT: $itemName"
        val text = "Current inventory ($currentStock units) is at or below threshold ($threshold units). Restock recommended!"

        val builder = NotificationCompat.Builder(context, CHANNEL_INVENTORY)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (soundOn) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSound(null)
        }

        if (vibrationOn) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(90000 + foodId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

