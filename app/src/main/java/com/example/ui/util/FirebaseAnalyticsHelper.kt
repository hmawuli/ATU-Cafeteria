package com.example.ui.util

import android.content.Context
import android.os.Bundle
import timber.log.Timber

/**
 * Utility helper for tracking analytics and telemetry events across cafeteria workflows
 * such as 'Add to Cart', 'Order Completion', and menu views.
 */
object FirebaseAnalyticsHelper {

    fun init(context: Context) {
        Timber.d("Telemetry and Analytics helper initialized.")
    }

    /**
     * Log 'Add to Cart' event when student adds item to cafeteria tray.
     */
    fun logAddToCart(itemId: Int, itemName: String, price: Double, quantity: Int) {
        val bundle = Bundle().apply {
            putString("item_id", itemId.toString())
            putString("item_name", itemName)
            putDouble("price", price)
            putInt("quantity", quantity)
            putString("currency", "GHS")
        }
        logEvent("add_to_cart", bundle)
        Timber.i("Analytics Event: ADD_TO_CART -> $itemName x$quantity (GH₵ $price)")
    }

    /**
     * Log 'Order Completion' (Purchase) event when checkout is successful.
     */
    fun logOrderCompletion(orderId: Long, totalPrice: Double, itemCount: Int, paymentMethod: String) {
        val bundle = Bundle().apply {
            putString("transaction_id", orderId.toString())
            putDouble("value", totalPrice)
            putInt("item_count", itemCount)
            putString("currency", "GHS")
            putString("payment_type", paymentMethod)
        }
        logEvent("purchase", bundle)
        Timber.i("Analytics Event: PURCHASE -> Order #$orderId total: GH₵ $totalPrice via $paymentMethod")
    }

    /**
     * Log menu item views for popular recommendation insights.
     */
    fun logViewMenuItem(itemId: Int, itemName: String, category: String) {
        val bundle = Bundle().apply {
            putString("item_id", itemId.toString())
            putString("item_name", itemName)
            putString("item_category", category)
        }
        logEvent("select_item", bundle)
    }

    /**
     * Log user language switching event in settings.
     */
    fun logLanguageChanged(languageCode: String) {
        val bundle = Bundle().apply {
            putString("language_code", languageCode)
        }
        logEvent("change_language", bundle)
    }

    /**
     * Log login success event for telemetry.
     */
    fun logLoginSuccess(method: String, userId: Int, role: String) {
        val bundle = Bundle().apply {
            putString("method", method)
            putString("user_id", userId.toString())
            putString("role", role)
        }
        logEvent("login", bundle)
        Timber.i("Analytics Event: LOGIN SUCCESS ($method) -> User #$userId [$role]")
    }

    /**
     * Log login failure event segmented by error type for crash & error diagnosis.
     */
    fun logLoginFailure(method: String, errorType: String, errorMessage: String) {
        val bundle = Bundle().apply {
            putString("method", method)
            putString("error_type", errorType)
            putString("error_message", errorMessage.take(100))
        }
        logEvent("login_failure", bundle)
        Timber.w("Analytics Event: LOGIN FAILURE ($method) -> $errorType: $errorMessage")
    }

    private fun logEvent(eventName: String, params: Bundle) {
        Timber.d("Telemetry logged: $eventName with params: $params")
    }
}
