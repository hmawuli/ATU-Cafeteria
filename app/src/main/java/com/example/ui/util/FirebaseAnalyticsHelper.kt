package com.example.ui.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

/**
 * Utility helper for tracking Firebase Analytics events across cafeteria workflows
 * such as 'Add to Cart', 'Order Completion', and menu views.
 */
object FirebaseAnalyticsHelper {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)
            Timber.d("Firebase Analytics initialized successfully.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Firebase Analytics (Running in fallback mode).")
        }
    }

    /**
     * Log 'Add to Cart' event when student adds item to cafeteria tray.
     */
    fun logAddToCart(itemId: Int, itemName: String, price: Double, quantity: Int) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, itemId.toString())
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            putDouble(FirebaseAnalytics.Param.PRICE, price)
            putInt(FirebaseAnalytics.Param.QUANTITY, quantity)
            putString(FirebaseAnalytics.Param.CURRENCY, "GHS")
        }
        logEvent(FirebaseAnalytics.Event.ADD_TO_CART, bundle)
        Timber.i("Analytics Event: ADD_TO_CART -> $itemName x$quantity (GH₵ $price)")
    }

    /**
     * Log 'Order Completion' (Purchase) event when checkout is successful.
     */
    fun logOrderCompletion(orderId: Long, totalPrice: Double, itemCount: Int, paymentMethod: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.TRANSACTION_ID, orderId.toString())
            putDouble(FirebaseAnalytics.Param.VALUE, totalPrice)
            putInt("item_count", itemCount)
            putString(FirebaseAnalytics.Param.CURRENCY, "GHS")
            putString(FirebaseAnalytics.Param.PAYMENT_TYPE, paymentMethod)
        }
        logEvent(FirebaseAnalytics.Event.PURCHASE, bundle)
        Timber.i("Analytics Event: PURCHASE -> Order #$orderId total: GH₵ $totalPrice via $paymentMethod")
    }

    /**
     * Log menu item views for popular recommendation insights.
     */
    fun logViewMenuItem(itemId: Int, itemName: String, category: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, itemId.toString())
            putString(FirebaseAnalytics.Param.ITEM_NAME, itemName)
            putString(FirebaseAnalytics.Param.ITEM_CATEGORY, category)
        }
        logEvent(FirebaseAnalytics.Event.SELECT_ITEM, bundle)
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

    private fun logEvent(eventName: String, params: Bundle) {
        try {
            firebaseAnalytics?.logEvent(eventName, params)
        } catch (e: Exception) {
            Timber.w("Firebase Analytics logEvent fallback: $eventName - ${e.localizedMessage}")
        }
    }
}
