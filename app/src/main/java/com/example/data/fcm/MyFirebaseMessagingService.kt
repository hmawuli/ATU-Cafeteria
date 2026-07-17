package com.example.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.ui.util.NotificationHelper

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Incoming FCM Message from: ${remoteMessage.from}")

        // Handle Notification Payloads
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification title: ${it.title}, body: ${it.body}")
            val title = it.title ?: "ATU Cafeteria Hub"
            val body = it.body ?: ""
            NotificationHelper.sendOrderStatusNotification(applicationContext, 8888, title, body)
        }

        // Handle Custom Data Payloads
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "ATU Cafeteria Alert"
            val body = remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: "An update is available for your account."
            val type = remoteMessage.data["type"] ?: "ORDER_STATUS"
            val orderIdStr = remoteMessage.data["order_id"] ?: "0"
            val orderId = orderIdStr.toIntOrNull() ?: 0

            if (type.contains("LOYALTY", ignoreCase = true)) {
                NotificationHelper.sendPromotionalNotification(applicationContext, orderId, title, body)
            } else {
                NotificationHelper.sendOrderStatusNotification(applicationContext, orderId, title, body)
            }
        }
    }
}
