package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    private const val CHANNEL_ORDERS = "atu_orders_channel"
    private const val CHANNEL_PROMO = "atu_promotions_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Order Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about food orders and status changes"
            }

            val promoChannel = NotificationChannel(
                CHANNEL_PROMO,
                "Promotional Offers & Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about campus food court discounts and promotions"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(ordersChannel)
            manager.createNotificationChannel(promoChannel)
        }
    }

    fun sendOrderStatusNotification(context: Context, orderId: Int, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(orderId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendPromotionalNotification(context: Context, notificationId: Int, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_PROMO)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
