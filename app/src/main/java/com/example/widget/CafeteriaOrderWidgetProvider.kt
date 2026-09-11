package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.Order
import com.example.data.FoodItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Android Home Screen Widget displaying current order status or 'Daily Special' menu item,
 * providing students with glanceable cafeteria updates directly from their home screen.
 */
class CafeteriaOrderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val allOrders: List<Order> = db.orderDao().getAllOrders().firstOrNull() ?: emptyList()
                val activeOrder = allOrders.lastOrNull {
                    it.status.uppercase() != "COMPLETED" && it.status.uppercase() != "CANCELLED"
                } ?: allOrders.lastOrNull()

                val allFoodItems: List<FoodItem> = db.foodItemDao().getAllFoodItems().firstOrNull() ?: emptyList()
                val dailySpecial = allFoodItems.firstOrNull { it.isAvailable }

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.cafeteria_widget)

                    // Set up click intent to launch MainActivity
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        action = "com.example.ACTION_OPEN_CAFETERIA_WIDGET"
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    if (activeOrder != null && activeOrder.status.uppercase() != "COMPLETED") {
                        val statusEmoji = when (activeOrder.status.uppercase()) {
                            "PREPARING" -> "🍳"
                            "READY" -> "🔔"
                            "PLACED", "PENDING" -> "⏳"
                            else -> "📦"
                        }
                        views.setTextViewText(
                            R.id.widget_status_title,
                            "$statusEmoji Order #${activeOrder.id}: ${activeOrder.status}"
                        )
                        views.setTextViewText(
                            R.id.widget_status_detail,
                            "${activeOrder.foodName} (x${activeOrder.quantity}) • GH₵ ${"%.2f".format(activeOrder.totalPrice)}"
                        )
                        views.setTextViewText(R.id.widget_badge, activeOrder.status)
                    } else if (dailySpecial != null) {
                        views.setTextViewText(
                            R.id.widget_status_title,
                            "⭐ Daily Special: ${dailySpecial.name}"
                        )
                        views.setTextViewText(
                            R.id.widget_status_detail,
                            "${dailySpecial.category} • GH₵ ${"%.2f".format(dailySpecial.price)} • Est. 10-15 mins"
                        )
                        views.setTextViewText(R.id.widget_badge, "SPECIAL")
                    } else {
                        views.setTextViewText(R.id.widget_status_title, "🍱 ATU Cafeteria Menu")
                        views.setTextViewText(R.id.widget_status_detail, "Tap to order delicious meals from campus vendors.")
                        views.setTextViewText(R.id.widget_badge, "OPEN")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating Cafeteria Widget")
            }
        }
    }
}
