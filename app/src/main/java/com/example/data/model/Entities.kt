package com.example.data

import androidx.room.*
import com.squareup.moshi.Json

// ==========================================
// ROOM DATABASE ENTITIES
// ==========================================

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val passwordHash: String, // Simulates secure credential storage
    val role: String,         // "STUDENT", "VENDOR", "ADMIN"
    val fullName: String,
    val info: String,         // e.g. Student ID for Students, or Brand Name ("ATU Delight") for Vendors
    val balance: Double = 0.0, // User's virtual wallet balance
    @Json(name = "is_open") @ColumnInfo(defaultValue = "1") val isOpen: Boolean = true,
    val student_staff_id: String? = null,
    val telephone: String? = null,
    val logoUrl: String? = null,
    val pictureUrl: String? = null,
    val email: String? = null,
    val paymentMethods: String? = null,
    @Json(name = "loyalty_points") val loyaltyPoints: Int = 0,
    @Json(name = "total_spent") val totalSpent: Double = 0.0,
    val dietaryPreferences: String? = null
)

@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["vendorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vendorId"])]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int, // Refers to User.id of role VENDOR
    val name: String,
    val price: Double,
    val category: String, // "Breakfast", "Local Dish", "Fast Food", "Drinks", "Snacks"
    val imageUrl: String, // Base64 or local visual reference
    val description: String,
    val isAvailable: Boolean = true,
    val initialStock: Int = 100,
    val currentStock: Int = 100,
    val lowStockThreshold: Int = 15,
    val calories: Int = 180,
    val allergens: String = "None",
    val availableStartTime: String = "06:00",
    val availableEndTime: String = "23:59",
    val isTimeScheduled: Boolean = false
)

@Entity(
    tableName = "orders",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["vendorId"])]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val vendorId: Int,
    val foodItemId: Int,
    val foodName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val orderTimestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "PREPARING", "READY", "COMPLETED", "DECLINED"
    val pickupPin: String, // Secure 4-digit PIN generated to substantiate custody hand-off
    val estimatedPickupTime: String = "Calculating...",
    @Json(name = "points_redeemed") val pointsRedeemed: Int = 0,
    @Json(name = "discount_applied") val discountApplied: Double = 0.0
)

@Entity(
    tableName = "feedback",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["vendorId"]), Index(value = ["customerId"])]
)
data class Feedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val vendorId: Int,
    val customerId: Int,
    val ratingFoodQuality: Int, // 1 to 5
    val ratingCleanliness: Int, // 1 to 5
    val ratingServiceSpeed: Int, // 1 to 5
    val ratingPriceValue: Int, // 1 to 5
    val comment: String,
    val vendorReply: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: Int,
    val action: String, // e.g., "USER_AUTHENTICATION", "ORDER_CREATED", "PICKUP_VALIDATED"
    val details: String
)

@Entity(
    tableName = "wallet_transactions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class WalletTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val type: String, // "DEPOSIT", "PAYMENT", "PAYOUT", "REFUND"
    val amount: Double,
    val status: String, // "SUCCESS", "PENDING", "FAILED"
    val reference: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "food_item_feedback",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"]), Index(value = ["foodItemId"]), Index(value = ["customerId"])]
)
data class FoodItemFeedback(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val foodItemId: Int,
    val customerId: Int,
    val rating: Int, // 1 to 5
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "vendor_menu_availabilities",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["vendorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vendorId"])]
)
data class VendorMenuAvailability(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val menuItemId: Int,
    val dayOfWeek: String, // monday, tuesday, etc.
    val startTime: String?,
    val endTime: String?,
    val isActive: Boolean = true
)

@Entity(
    tableName = "vendor_order_summaries",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["vendorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["vendorId"]), Index(value = ["vendorId", "summaryDate"], unique = true)]
)
data class VendorOrderSummary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vendorId: Int,
    val summaryDate: String,
    val totalOrders: Int = 0,
    val completedOrders: Int = 0,
    val pendingOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val averageRating: Double = 5.0
)

@Entity(tableName = "offline_orders")
data class OfflineOrder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val vendorId: Int,
    val foodItemId: Int,
    val foodName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val pointsToRedeem: Int = 0,
    val estimatedPickupTime: String = "Calculating...",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val senderId: Int,
    val senderName: String,
    val recipientId: Int,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromStudent: Boolean
)


