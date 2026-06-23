package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// ==========================================
// 1. GEMINI ANALYTICS DATA ARCHITECTURE
// ==========================================

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(val contents: List<GeminiContent>)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val geminiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// ==========================================
// 2. REPOSITORY PATTERN IMPLEMENTATIONS
// ==========================================

class CafeteriaRepository(private val db: AppDatabase) {
    val userDao = db.userDao()
    val foodItemDao = db.foodItemDao()
    val orderDao = db.orderDao()
    val feedbackDao = db.feedbackDao()
    val auditLogDao = db.auditLogDao()
    val walletTransactionDao = db.walletTransactionDao()
    val foodItemFeedbackDao = db.foodItemFeedbackDao()

    // Flow Accessors
    val allVendors: Flow<List<User>> = userDao.getAllVendors()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allFoodItems: Flow<List<FoodItem>> = foodItemDao.getAllFoodItems()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allFeedback: Flow<List<Feedback>> = feedbackDao.getAllFeedback()
    val allFoodFeedback: Flow<List<FoodItemFeedback>> = foodItemFeedbackDao.getAllFoodFeedback()
    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()
    val allWalletTransactions: Flow<List<WalletTransaction>> = walletTransactionDao.getAllWalletTransactions()

    fun getFeedbackForFoodItem(foodItemId: Int): Flow<List<FoodItemFeedback>> =
        foodItemFeedbackDao.getFeedbackForFoodItem(foodItemId)

    fun getWalletTransactionsForUser(userId: Int): Flow<List<WalletTransaction>> =
        walletTransactionDao.getWalletTransactionsForUser(userId)

    fun getFoodItemsForVendor(vendorId: Int): Flow<List<FoodItem>> =
        foodItemDao.getFoodItemsByVendor(vendorId)

    fun getOrdersForCustomer(customerId: Int): Flow<List<Order>> =
        orderDao.getOrdersForCustomer(customerId)

    fun getOrdersForVendor(vendorId: Int): Flow<List<Order>> =
        orderDao.getOrdersForVendor(vendorId)

    fun getFeedbackForVendor(vendorId: Int): Flow<List<Feedback>> =
        feedbackDao.getFeedbackForVendor(vendorId)

    // Password hashing for "Secure Mobile" requirement
    fun sha256(input: String): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }

    // Role-based User operations
    suspend fun registerUser(username: String, pinCode: String, role: String, fullName: String, info: String): User? = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val pHash = sha256(pinCode)
                val user = if (role.uppercase() == "STUDENT") {
                    val resp = service.registerStudent(
                        LaravelStudentRegisterRequest(
                            username = username,
                            pin = pHash,
                            fullName = fullName,
                            studentId = info
                        )
                    )
                    resp.user
                } else if (role.uppercase() == "VENDOR") {
                    val resp = service.registerVendor(
                        LaravelVendorRegisterRequest(
                            username = username,
                            pin = pHash,
                            fullName = fullName,
                            boothDescription = info
                        )
                    )
                    resp.user
                } else {
                    service.register(
                        LaravelRegisterRequest(
                            username = username,
                            pin = pHash,
                            role = role,
                            fullName = fullName,
                            info = info
                        )
                    )
                }
                // Cache locally
                try {
                    userDao.insertUser(user)
                } catch (pe: Exception) {
                    userDao.updateUser(user)
                }
                return@withContext user
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel registration failed - falling back to local database", e)
            }
        }
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            return@withContext null
        }
        val pHash = sha256(pinCode)
        val user = User(
            username = username,
            passwordHash = pHash,
            role = role,
            fullName = fullName,
            info = info
        )
        val id = userDao.insertUser(user)
        val insertedUser = user.copy(id = id.toInt())
        insertAuditLog(insertedUser.id, "USER_REGISTRATION", "Registered ${user.fullName} as ${user.role}")
        return@withContext insertedUser
    }

    suspend fun authenticateUser(username: String, pinCode: String): User? = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val pinHash = sha256(pinCode)
                val user = service.login(
                    LaravelLoginRequest(
                        username = username,
                        pin = pinHash
                    )
                )
                // Cache locally
                val localUser = userDao.getUserSync(user.id)
                val userToSave = if (localUser != null) {
                    user.copy(balance = localUser.balance)
                } else {
                    user
                }
                try {
                    userDao.insertUser(userToSave)
                } catch (pe: Exception) {
                    userDao.updateUser(userToSave)
                }
                return@withContext userToSave
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel login failed - falling back to local database", e)
            }
        }
        val user = userDao.getUserByUsername(username) ?: return@withContext null
        val targetHash = sha256(pinCode)
        if (user.passwordHash == targetHash) {
            insertAuditLog(user.id, "USER_AUTHENTICATION", "Successfully logged in.")
            return@withContext user
        } else {
            insertAuditLog(user.id, "AUTH_FAILURE", "Failed logging attempt with wrong PIN.")
            return@withContext null
        }
    }

    suspend fun authenticateOrRegisterSocialUser(username: String, fullName: String, provider: String, logoUrl: String?): User = withContext(Dispatchers.IO) {
        val existing = userDao.getUserByUsername(username)
        if (existing != null) {
            insertAuditLog(existing.id, "USER_AUTHENTICATION", "Successfully logged in via $provider.")
            return@withContext existing
        }
        val newUser = User(
            username = username,
            passwordHash = sha256("sso-secure-pin"),
            role = "STUDENT",
            fullName = fullName,
            info = "ATU-${(100000..999999).random()}",
            balance = 100.0,
            student_staff_id = "ATU-" + (100000..999999).random(),
            logoUrl = logoUrl
        )
        val id = userDao.insertUser(newUser)
        val insertedUser = newUser.copy(id = id.toInt())
        insertAuditLog(insertedUser.id, "USER_REGISTRATION", "Registered ${newUser.fullName} as STUDENT via $provider")
        return@withContext insertedUser
    }

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun updateUserProfile(
        id: Int,
        fullName: String,
        studentStaffId: String?,
        telephone: String?,
        email: String?,
        department: String?,
        programOfStudy: String?,
        paymentMethods: List<String>?,
        info: String
    ): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserSync(id) ?: return@withContext false
        val updatedUser = user.copy(
            fullName = fullName,
            student_staff_id = studentStaffId,
            telephone = telephone,
            email = email,
            paymentMethods = paymentMethods?.joinToString(","),
            info = info
        )
        userDao.updateUser(updatedUser)
        insertAuditLog(id, "PROFILE_UPDATE", "User updated their email/phone profile settings & payment options.")

        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val response = service.updateProfile(
                    LaravelUpdateProfileRequest(
                        fullName = fullName,
                        student_staff_id = studentStaffId,
                        telephone = telephone,
                        phone_number = telephone,
                        email = email,
                        department = department,
                        program_of_study = programOfStudy,
                        payment_methods = paymentMethods,
                        info = info
                    )
                )
                return@withContext response.success
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
        return@withContext true
    }

    suspend fun deleteUser(userId: Int) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                LaravelClientManager.getService().deleteUser(userId)
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel deleteUser failed", e)
            }
        }
        userDao.deleteUserById(userId)
    }

    // Menu Management
    suspend fun addMenuFoodItem(vendorId: Int, name: String, price: Double, category: String, description: String, imageUrl: String, initialStock: Int = 100, threshold: Int = 15, calories: Int = 180, allergens: String = "None") = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lItem = service.createFoodItem(
                    LaravelAddFoodRequest(
                        vendor_id = vendorId,
                        name = name,
                        price = price,
                        category = category,
                        description = description,
                        image_url = imageUrl
                    )
                )
                val roomItem = LaravelClientManager.toRoomFoodItem(lItem)
                foodItemDao.insertFoodItem(roomItem.copy(initialStock = initialStock, currentStock = initialStock, lowStockThreshold = threshold, calories = calories, allergens = allergens))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel createFoodItem failed - falling back to local", e)
            }
        }
        val item = FoodItem(
            vendorId = vendorId,
            name = name,
            price = price,
            category = category,
            description = description,
            imageUrl = imageUrl,
            isAvailable = true,
            initialStock = initialStock,
            currentStock = initialStock,
            lowStockThreshold = threshold,
            calories = calories,
            allergens = allergens
        )
        foodItemDao.insertFoodItem(item)
        insertAuditLog(vendorId, "MENU_ITEM_CREATED", "Added menu item: ${name} (Calories: ${calories} kcal, Allergens: ${allergens}) to category ${category} with initial stock of ${initialStock}.")
    }

    suspend fun updateMenuFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lItem = service.updateFoodItem(
                    id = item.id,
                    request = LaravelUpdateFoodRequest(
                        name = item.name,
                        price = item.price,
                        category = item.category,
                        description = item.description,
                        image_url = item.imageUrl,
                        is_available = item.isAvailable
                    )
                )
                foodItemDao.insertFoodItem(LaravelClientManager.toRoomFoodItem(lItem))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel updateFoodItem failed - falling back to local", e)
            }
        }
        foodItemDao.updateFoodItem(item)
        insertAuditLog(item.vendorId, "MENU_ITEM_UPDATED", "Updated item details for '${item.name}'")
    }

    suspend fun deleteMenuFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                LaravelClientManager.getService().deleteFoodItem(item.id)
                foodItemDao.deleteFoodItem(item)
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel deleteFoodItem failed - falling back to local", e)
            }
        }
        foodItemDao.deleteFoodItem(item)
        insertAuditLog(item.vendorId, "MENU_ITEM_DELETED", "Deleted item: '${item.name}' from vendor menu.")
    }

    suspend fun bulkUpdateVendorMenu(vendorId: Int, updates: List<LaravelBulkUpdateItem>): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val response = service.bulkUpdateMenu(updates)
                if (response.success) {
                    updates.forEach { update ->
                        val localItem = foodItemDao.getFoodItemById(update.id)
                        if (localItem != null) {
                            var price = localItem.price
                            var isAvailable = localItem.isAvailable
                            if (update.price != null) { price = update.price }
                            if (update.is_available != null) { isAvailable = update.is_available }
                            foodItemDao.updateFoodItem(localItem.copy(price = price, isAvailable = isAvailable))
                        }
                    }
                    insertAuditLog(vendorId, "BULK_MENU_UPLOAD", "Bulk updated ${response.updated_count} menu items via Laravel API in a single request.")
                    return@withContext Pair(true, response.message)
                } else {
                    return@withContext Pair(false, response.message)
                }
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel bulkUpdateVendorMenu failed - falling back to local simulation", e)
            }
        }

        var updatedCount = 0
        try {
            updates.forEach { update ->
                val localItem = foodItemDao.getFoodItemById(update.id)
                if (localItem != null && localItem.vendorId == vendorId) {
                    var price = localItem.price
                    var isAvailable = localItem.isAvailable
                    if (update.price != null) { price = update.price }
                    if (update.is_available != null) { isAvailable = update.is_available }
                    foodItemDao.updateFoodItem(localItem.copy(price = price, isAvailable = isAvailable))
                    updatedCount++
                }
            }
            insertAuditLog(vendorId, "BULK_MENU_UPLOAD", "Bulk updated $updatedCount menu items (Offline/Local State Simulation) via JSON parser.")
            return@withContext Pair(true, "Successfully processed updates. Updated $updatedCount menu item(s) in local cache.")
        } catch (e: Exception) {
            return@withContext Pair(false, "Offline update failed: ${e.message}")
        }
    }

    // Transactions
    suspend fun placeOrder(customerId: Int, foodItem: FoodItem, quantity: Int): Order = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lOrder = service.createOrder(
                    LaravelAddOrderRequest(
                        customer_id = customerId,
                        vendor_id = foodItem.vendorId,
                        food_item_id = foodItem.id,
                        food_name = foodItem.name,
                        quantity = quantity,
                        unit_price = foodItem.price,
                        total_price = foodItem.price * quantity
                    )
                )
                val roomOrder = LaravelClientManager.toRoomOrder(lOrder)
                orderDao.insertOrder(roomOrder)
                
                // Dispatch WebSocket broadcast instantly to subscriber roles in real-time
                com.example.data.LaravelEchoWebSocketManager.broadcastOrderPlacedLocally(
                    orderId = roomOrder.id,
                    vendorId = roomOrder.vendorId,
                    foodName = roomOrder.foodName,
                    qty = roomOrder.quantity,
                    totalPrice = roomOrder.totalPrice
                )
                
                // Deduct inventory stock locally
                try {
                    val existingItem = foodItemDao.getFoodItemById(foodItem.id)
                    if (existingItem != null) {
                        val updatedStock = (existingItem.currentStock - quantity).coerceAtLeast(0)
                        foodItemDao.updateFoodItem(existingItem.copy(currentStock = updatedStock))
                        if (updatedStock <= existingItem.lowStockThreshold) {
                            insertAuditLog(foodItem.vendorId, "LOW_STOCK_ALERT", "Low stock alert: '${foodItem.name}' has fallen below the threshold! Remaining stock: $updatedStock.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CafeteriaRepository", "Failed to update stock logic", e)
                }

                return@withContext roomOrder
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel placeOrder failed - falling back to local", e)
            }
        }
        // Secure pickup PIN is a random 4-digit code
        val securePin = (1000..9999).random().toString()
        val order = Order(
            customerId = customerId,
            vendorId = foodItem.vendorId,
            foodItemId = foodItem.id,
            foodName = foodItem.name,
            quantity = quantity,
            unitPrice = foodItem.price,
            totalPrice = foodItem.price * quantity,
            status = "PENDING",
            pickupPin = securePin,
            estimatedPickupTime = "Calculating..."
        )
        val orderId = orderDao.insertOrder(order)
        insertAuditLog(customerId, "ORDER_CREATED", "Created order #${orderId} for '${foodItem.name}' (QTY: ${quantity}) with secure pick-up code.")
        
        // Dispatch WebSocket broadcast instantly to subscriber roles in real-time
        com.example.data.LaravelEchoWebSocketManager.broadcastOrderPlacedLocally(
            orderId = orderId.toInt(),
            vendorId = order.vendorId,
            foodName = order.foodName,
            qty = order.quantity,
            totalPrice = order.totalPrice
        )
        
        // Deduct inventory stock locally
        try {
            val existingItem = foodItemDao.getFoodItemById(foodItem.id)
            if (existingItem != null) {
                val updatedStock = (existingItem.currentStock - quantity).coerceAtLeast(0)
                foodItemDao.updateFoodItem(existingItem.copy(currentStock = updatedStock))
                if (updatedStock <= existingItem.lowStockThreshold) {
                    insertAuditLog(foodItem.vendorId, "LOW_STOCK_ALERT", "Low stock alert: '${foodItem.name}' has fallen below the threshold! Remaining stock: $updatedStock.")
                }
            }
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Failed to update stock logic", e)
        }

        return@withContext order.copy(id = orderId.toInt())
    }

    suspend fun updateOrderStatus(vendorId: Int, orderId: Int, newStatus: String, estimatedTime: String? = null) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lOrder = service.updateOrderStatus(
                    id = orderId,
                    request = LaravelUpdateOrderStatusRequest(
                        status = newStatus,
                        estimated_pickup_time = estimatedTime
                    )
                )
                orderDao.insertOrder(LaravelClientManager.toRoomOrder(lOrder))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel updateOrderStatus failed - falling back to local", e)
            }
        }
        val o = orderDao.getOrderById(orderId) ?: return@withContext
        val updated = o.copy(
            status = newStatus,
            estimatedPickupTime = estimatedTime ?: o.estimatedPickupTime
        )
        orderDao.updateOrder(updated)
        
        val oldStat = o.status
        val calculatedMsg = when (newStatus.uppercase()) {
            "PREPARING" -> "Chef is preparing your order #${orderId} ('${o.foodName}')! It will be ready soon."
            "READY" -> "Good news! Your order #${orderId} ('${o.foodName}') is READY for pickup. Secure Hand-off PIN: ${o.pickupPin}."
            "COMPLETED" -> "Hurray! Your order #${orderId} has been picked up & marked as completed."
            "CANCELLED" -> "Order #${orderId} ('${o.foodName}') has been cancelled/voided."
            else -> "Your order #${orderId} ('${o.foodName}') status has been updated to $newStatus."
        }
        com.example.data.LaravelEchoWebSocketManager.broadcastStudentNotificationLocally(
            notificationId = java.util.UUID.randomUUID().toString(),
            orderId = orderId,
            vendorId = vendorId,
            oldStatus = oldStat,
            newStatus = newStatus,
            message = calculatedMsg
        )

        insertAuditLog(vendorId, "ORDER_STATUS_CHANGED", "Order #${orderId} transitioned to: ${newStatus} (${estimatedTime ?: "No change to estimate"})")
    }

    suspend fun cancelOrder(vendorId: Int, orderId: Int, reason: String) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lOrder = service.updateOrderStatus(
                    id = orderId,
                    request = LaravelUpdateOrderStatusRequest(
                        status = "CANCELLED",
                        estimated_pickup_time = "Cancelled"
                    )
                )
                orderDao.insertOrder(LaravelClientManager.toRoomOrder(lOrder))
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel cancelOrder failed - falling back to local", e)
            }
        }
        val o = orderDao.getOrderById(orderId) ?: return@withContext
        val updated = o.copy(
            status = "CANCELLED",
            estimatedPickupTime = "Cancelled"
        )
        orderDao.updateOrder(updated)
        insertAuditLog(vendorId, "ORDER_CANCELLED", "Order #${orderId} for '${o.foodName}' (QTY: ${o.quantity}) was cancelled by Vendor. Reason: $reason")

        // Dynamic stock restoration
        try {
            val food = foodItemDao.getFoodItemById(o.foodItemId)
            if (food != null) {
                val restoredStock = food.currentStock + o.quantity
                foodItemDao.updateFoodItem(food.copy(currentStock = restoredStock))
                insertAuditLog(vendorId, "INVENTORY_ADJUSTED", "Restored ${o.quantity} portions of '${food.name}' back to stock because of cancellation. New stock: $restoredStock.")
            }
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Failed to restore inventory stock on cancel order", e)
        }

        // Automatic financial reversal
        try {
            val transactions = walletTransactionDao.getWalletTransactionsForUserSync(o.customerId)
            val hasPaidWithWallet = transactions.any {
                it.type == "PAYMENT" && it.details.contains(o.foodName) && it.amount < 0
            }
            if (hasPaidWithWallet) {
                // Refund student
                val studentRef = "REF-" + (100000..999999).random()
                insertWalletTransaction(
                    userId = o.customerId,
                    type = "REFUND",
                    amount = o.totalPrice,
                    reference = studentRef,
                    details = "Refund for cancelled Order #${o.id} ('${o.foodName}') - Reason: $reason"
                )
                
                // Debit vendor's earnings
                val vendorRef = "REV-" + (100000..999999).random()
                insertWalletTransaction(
                    userId = vendorId,
                    type = "PAYMENT",
                    amount = -o.totalPrice,
                    reference = vendorRef,
                    details = "Earning reversal for cancelled Order #${o.id} ('${o.foodName}') - Reason: $reason"
                )
            }
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Failed to process automatic refund", e)
        }
    }

    suspend fun verifyAndCompletePickup(vendorId: Int, orderId: Int, pin: String): Boolean = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val response = service.verifyAndCompletePickup(
                    id = orderId,
                    request = LaravelVerifyPickupRequest(
                        vendor_id = vendorId,
                        pickup_pin = pin
                    )
                )
                if (response.success && response.order != null) {
                    orderDao.insertOrder(LaravelClientManager.toRoomOrder(response.order))
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel verifyAndCompletePickup failed - falling back to local", e)
            }
        }
        val o = orderDao.getOrderById(orderId) ?: return@withContext false
        if (o.vendorId == vendorId && o.pickupPin == pin) {
            val updated = o.copy(status = "DELIVERED")
            orderDao.updateOrder(updated)
            insertAuditLog(vendorId, "PICKUP_VALIDATED_QR", "Order #${orderId} secure QR/PIN verified successfully and marked Delivered.")
            return@withContext true
        } else {
            insertAuditLog(vendorId, "PICKUP_FAIL", "Order #${orderId} verification failed with bad PIN: '$pin'")
            return@withContext false
        }
    }

    // Feedback
    suspend fun submitFeedback(customerId: Int, orderId: Int, vendorId: Int, quality: Int, cleanliness: Int, speed: Int, value: Int, comment: String) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lFeedback = service.createFeedback(
                    LaravelAddFeedbackRequest(
                        order_id = orderId,
                        vendor_id = vendorId,
                        customer_id = customerId,
                        food_quality = quality,
                        cleanliness = cleanliness,
                        speed = speed,
                        value = value,
                        comment = comment
                    )
                )
                feedbackDao.insertFeedback(LaravelClientManager.toRoomFeedback(lFeedback))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel submitFeedback failed - falling back to local", e)
            }
        }
        val feedback = Feedback(
            orderId = orderId,
            vendorId = vendorId,
            customerId = customerId,
            ratingFoodQuality = quality,
            ratingCleanliness = cleanliness,
            ratingServiceSpeed = speed,
            ratingPriceValue = value,
            comment = comment
        )
        feedbackDao.insertFeedback(feedback)
        insertAuditLog(customerId, "FEEDBACK_SUBMITTED", "Submitted review for order #${orderId} on food quality(${quality}), cleanliness(${cleanliness})")
    }

    suspend fun submitFoodFeedback(customerId: Int, orderId: Int, foodItemId: Int, rating: Int, comment: String) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lFeedback = service.createFoodFeedback(
                    LaravelAddFoodFeedbackRequest(
                        order_id = orderId,
                        food_item_id = foodItemId,
                        customer_id = customerId,
                        rating = rating,
                        comment = comment
                    )
                )
                foodItemFeedbackDao.insertFoodFeedback(LaravelClientManager.toRoomFoodItemFeedback(lFeedback))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel submitFoodFeedback failed - falling back to local", e)
            }
        }
        val feedback = FoodItemFeedback(
            orderId = orderId,
            foodItemId = foodItemId,
            customerId = customerId,
            rating = rating,
            comment = comment
        )
        foodItemFeedbackDao.insertFoodFeedback(feedback)
        insertAuditLog(customerId, "FOOD_FEEDBACK_SUBMITTED", "Submitted review for food item #${foodItemId} on order #${orderId} with rating (${rating})")
    }

    // Logger
    suspend fun insertAuditLog(userId: Int, action: String, details: String) = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val lLog = service.createAuditLog(
                     LaravelAddAuditLogRequest(
                         user_id = userId,
                         action = action,
                         details = details
                     )
                )
                auditLogDao.insertLog(LaravelClientManager.toRoomAuditLog(lLog))
                return@withContext
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel insertAuditLog failed", e)
            }
        }
        auditLogDao.insertLog(AuditLog(userId = userId, action = action, details = details))
    }

    // Performance Analytics
    suspend fun getVendorPerformance(
        vendorId: Int? = null,
        startDate: String? = null,
        endDate: String? = null
    ): List<LaravelDailyPerformance> = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val response = service.getVendorPerformance(vendorId, startDate, endDate)
                if (response.success) {
                    return@withContext response.daily_performance
                }
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel getVendorPerformance failed", e)
            }
        }
        return@withContext emptyList()
    }

    suspend fun getVendorPerformanceMetrics(): List<LaravelVendorMetric> = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                val response = service.getVendorPerformanceMetrics()
                if (response.success) {
                    return@withContext response.performance
                }
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel getVendorPerformanceMetrics failed", e)
            }
        }
        return@withContext emptyList()
    }

    suspend fun getDailyRevenue(vendorId: Int? = null): LaravelDailyRevenueResponse? = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                val service = LaravelClientManager.getService()
                return@withContext service.getDailyRevenue(vendorId)
            } catch (e: Exception) {
                Log.e("CafeteriaRepository", "Laravel getDailyRevenue failed", e)
            }
        }
        return@withContext null
    }

    // Wallet repository interactions
    suspend fun insertWalletTransaction(userId: Int, type: String, amount: Double, reference: String, details: String) = withContext(Dispatchers.IO) {
        val transaction = WalletTransaction(
            userId = userId,
            type = type,
            amount = amount,
            status = "SUCCESS",
            reference = reference,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        walletTransactionDao.insertWalletTransaction(transaction)

        // Adjust local user balance as well
        val userItem = userDao.getUserSync(userId)
        if (userItem != null) {
            val updatedUser = userItem.copy(balance = userItem.balance + amount)
            userDao.updateUser(updatedUser)
        }
    }

    // Multi-table sync from Laravel to Local SQLite/Room DB Cache
    suspend fun syncAllFromLaravel() = withContext(Dispatchers.IO) {
        if (!LaravelClientManager.isLaravelEnabled) return@withContext
        try {
            Log.d("CafeteriaRepository", "Syncing all tables from Laravel API dynamically...")
            val service = LaravelClientManager.getService()

            // 1. Sync Users
            val remoteUsers = service.getAllUsers()
            for (u in remoteUsers) {
                val localUser = userDao.getUserSync(u.id)
                val userToSave = if (localUser != null) {
                    u.copy(balance = localUser.balance)
                } else {
                    u
                }
                try {
                    userDao.insertUser(userToSave)
                } catch (pe: Exception) {
                    userDao.updateUser(userToSave)
                }
            }

            // 2. Sync Foods
            val remoteFoods = service.getFoodItems()
            for (f in remoteFoods) {
                foodItemDao.insertFoodItem(LaravelClientManager.toRoomFoodItem(f))
            }

            // 3. Sync Orders
            val remoteOrders = service.getAllOrders()
            for (o in remoteOrders) {
                orderDao.insertOrder(LaravelClientManager.toRoomOrder(o))
            }

            // 4. Sync Feedbacks
            val remoteFeedback = service.getAllFeedback()
            for (f in remoteFeedback) {
                feedbackDao.insertFeedback(LaravelClientManager.toRoomFeedback(f))
            }

            // 4a. Sync Food Feedbacks
            try {
                val remoteFoodFeedback = service.getAllFoodFeedback()
                for (ff in remoteFoodFeedback) {
                    foodItemFeedbackDao.insertFoodFeedback(LaravelClientManager.toRoomFoodItemFeedback(ff))
                }
            } catch (ffe: Exception) {
                Log.e("CafeteriaRepository", "Syncing food feedbacks failed", ffe)
            }

            // 5. Sync Audit Logs
            val remoteLogs = service.getAllAuditLogs()
            for (l in remoteLogs) {
                auditLogDao.insertLog(LaravelClientManager.toRoomAuditLog(l))
            }

            Log.d("CafeteriaRepository", "Sync completed successfully!")
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Laravel synchronization failed - continuing in local mode offline", e)
        }
    }

    suspend fun fetchLaravelNotifications(): List<LaravelDatabaseNotification> = withContext(Dispatchers.IO) {
        if (!LaravelClientManager.isLaravelEnabled) return@withContext emptyList()
        try {
            val response = LaravelClientManager.getService().getDatabaseNotifications()
            if (response.success) {
                return@withContext response.notifications
            }
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Failed to fetch remote notifications", e)
        }
        emptyList()
    }

    suspend fun markLaravelNotificationsAsRead() = withContext(Dispatchers.IO) {
        if (!LaravelClientManager.isLaravelEnabled) return@withContext
        try {
            LaravelClientManager.getService().markAllNotificationsAsRead()
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Failed to mark notifications as read", e)
        }
    }

    // ==========================================
    // SEED INITIAL SAMPLE DATA
    // ==========================================
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        try {
            val userCount = userDao.getUserCount()
            if (userCount > 0) return@withContext

            Log.d("CafeteriaRepository", "Database is empty! Starting seed procedure...")

            // Create standard roles
            // 1. Student Customer
            val stud1 = User(id = 1, username = "student", passwordHash = sha256("1234"), role = "STUDENT", fullName = "Daniel Mensah", info = "ATU-2024-D45", student_staff_id = "ATU-2024-D45", telephone = "+233 50 123 4567")
            val stud2 = User(id = 2, username = "student2", passwordHash = sha256("1234"), role = "STUDENT", fullName = "Abena Osei", info = "ATU-2025-S12", student_staff_id = "ATU-2025-S12", telephone = "+233 24 987 6543")
            userDao.insertUser(stud1)
            userDao.insertUser(stud2)

            // 2. Vendors
            val v1 = User(
                id = 10,
                username = "maryjoint",
                passwordHash = sha256("1111"),
                role = "VENDOR",
                fullName = "Mary Joint",
                info = "Auntie Mary Special",
                logoUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120&auto=format&fit=crop&q=60",
                pictureUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60"
            )
            val v2 = User(
                id = 11,
                username = "atkitch",
                passwordHash = sha256("2222"),
                role = "VENDOR",
                fullName = "Kofi Local Kitchen",
                info = "ATU Local Hub",
                logoUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=120&auto=format&fit=crop&q=60",
                pictureUrl = "https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=500&auto=format&fit=crop&q=60"
            )
            val v3 = User(
                id = 12,
                username = "snackbag",
                passwordHash = sha256("3333"),
                role = "VENDOR",
                fullName = "Bakery & Treats",
                info = "ATU Snack Corner",
                logoUrl = "https://images.unsplash.com/photo-1517433456452-f9633a875f6f?w=120&auto=format&fit=crop&q=60",
                pictureUrl = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60"
            )
            userDao.insertUser(v1)
            userDao.insertUser(v2)
            userDao.insertUser(v3)

            // 3. Manager/Admin
            val admin = User(id = 99, username = "admin", passwordHash = sha256("admin123"), role = "ADMIN", fullName = "Dr. Emmanuel Kaku", info = "ATU Quality Assurance")
            userDao.insertUser(admin)

            // Seed FoodItems
            val foods = listOf(
                // Mary Joint
                FoodItem(id = 101, vendorId = v1.id, name = "ATU Chicken Jollof Rice", price = 25.0, category = "Lunch Specials", imageUrl = "", description = "Classic aromatic rice stewed with authentic Ghanaian tomato sauce, served with seasoned fried chicken salad & shito.", calories = 650, allergens = "Fish, Soy (shito)"),
                FoodItem(id = 102, vendorId = v1.id, name = "Zesty Ginger Sobolo", price = 10.0, category = "Drinks", imageUrl = "", description = "Refreshing chilled local hibiscus flower drink brewed with fresh ginger, pineapple peels, and sweetener.", calories = 120, allergens = "None"),
                FoodItem(id = 103, vendorId = v1.id, name = "Red-Red Beans Stew", price = 20.0, category = "Lunch Specials", imageUrl = "", description = "Stewed tender cowpea bean hash in palm palm oil, accompanied by fried ripe sugar-plantain dices.", calories = 580, allergens = "None"),

                // Kofi Kitchen
                FoodItem(id = 201, vendorId = v2.id, name = "Waakye Supreme", price = 30.0, category = "Traditional", imageUrl = "", description = "A student favorite! Local black-eyed peas boiled with rice and millet stalks. Accompanying boiled egg, spiced gari, talia, and hot wele shito.", calories = 750, allergens = "Egg, Fish (shito)"),
                FoodItem(id = 202, vendorId = v2.id, name = "Fufu & Goat Light Soup", price = 35.0, category = "Traditional", imageUrl = "", description = "Rich Ghanaian fufu pounded from fresh cassava and green plantains, submerged in aromatic goat meat soup.", calories = 820, allergens = "Goat Meat"),

                // Bakery Corner
                FoodItem(id = 301, vendorId = v3.id, name = "Savoury Meat Pie", price = 15.0, category = "Snacks", imageUrl = "", description = "Crispy, flaky puff pastry loaded with moist, cooked mince beef seasoning.", calories = 380, allergens = "Gluten (Wheat), Dairy"),
                FoodItem(id = 302, vendorId = v3.id, name = "Chilled Coca-Cola", price = 8.0, category = "Drinks", imageUrl = "", description = "330ml Ice-cold Coca-Cola can for dynamic pairing.", calories = 140, allergens = "None")
            )
            for (f in foods) {
                foodItemDao.insertFoodItem(f)
            }

            // Seed historical orders and ratings to give Data Analytics some meat initially!
            val pastOrders = listOf(
                Order(id = 1001, customerId = stud1.id, vendorId = v1.id, foodItemId = 101, foodName = "ATU Chicken Jollof Rice", quantity = 1, unitPrice = 25.0, totalPrice = 25.0, orderTimestamp = System.currentTimeMillis() - 86400000 * 2, status = "COMPLETED", pickupPin = "4444"),
                Order(id = 1002, customerId = stud2.id, vendorId = v1.id, foodItemId = 102, foodName = "Zesty Ginger Sobolo", quantity = 2, unitPrice = 10.0, totalPrice = 20.0, orderTimestamp = System.currentTimeMillis() - 86400000 * 1, status = "COMPLETED", pickupPin = "5555"),
                Order(id = 1003, customerId = stud1.id, vendorId = v2.id, foodItemId = 201, foodName = "Waakye Supreme", quantity = 1, unitPrice = 30.0, totalPrice = 30.0, orderTimestamp = System.currentTimeMillis() - 86400000 * 3, status = "COMPLETED", pickupPin = "6666"),
                Order(id = 1004, customerId = stud2.id, vendorId = v2.id, foodItemId = 202, foodName = "Fufu & Goat Light Soup", quantity = 1, unitPrice = 35.0, totalPrice = 35.0, orderTimestamp = System.currentTimeMillis() - 86400000 * 4, status = "COMPLETED", pickupPin = "7777")
            )

            for (o in pastOrders) {
                orderDao.insertOrder(o)
            }

            // Seed dynamic feedback scores
            val pastReviews = listOf(
                Feedback(id = 1, orderId = 1001, vendorId = v1.id, customerId = stud1.id, ratingFoodQuality = 5, ratingCleanliness = 4, ratingServiceSpeed = 4, ratingPriceValue = 4, comment = "The chicken was very delicious and juicy! A bit crowded but service was quite neat.", timestamp = System.currentTimeMillis() - 86400000 * 2),
                Feedback(id = 2, orderId = 1002, vendorId = v1.id, customerId = stud2.id, ratingFoodQuality = 4, ratingCleanliness = 5, ratingServiceSpeed = 5, ratingPriceValue = 5, comment = "Chilled sobolo was exactly what I needed after lectures. Extremely clean booth!", timestamp = System.currentTimeMillis() - 86400000 * 1),
                Feedback(id = 3, orderId = 1003, vendorId = v2.id, customerId = stud1.id, ratingFoodQuality = 5, ratingCleanliness = 3, ratingServiceSpeed = 3, ratingPriceValue = 5, comment = "The waakye was extremely tasty. However, they need to improve speed of service at peak 12:00 PM hours.", timestamp = System.currentTimeMillis() - 86400000 * 3),
                Feedback(id = 4, orderId = 1004, vendorId = v2.id, customerId = stud2.id, ratingFoodQuality = 4, ratingCleanliness = 2, ratingServiceSpeed = 4, ratingPriceValue = 4, comment = "Pounded fufu was soft and fresh. The chop booth surrounding floor had some napkins; cleanliness can be improved.", timestamp = System.currentTimeMillis() - 86400000 * 4)
            )

            for (r in pastReviews) {
                feedbackDao.insertFeedback(r)
            }

            // Seed food-specific feedback scores
            val pastFoodFeedback = listOf(
                FoodItemFeedback(id = 1, orderId = 1001, foodItemId = 101, customerId = stud1.id, rating = 5, comment = "Excellent spicy seasoning, chicken was crispy and warm!", timestamp = System.currentTimeMillis() - 86400000 * 2),
                FoodItemFeedback(id = 2, orderId = 1002, foodItemId = 102, customerId = stud2.id, rating = 4, comment = "Very thirst-quenching Sobolo so sweet and delicious.", timestamp = System.currentTimeMillis() - 86400000 * 1),
                FoodItemFeedback(id = 3, orderId = 1003, foodItemId = 201, customerId = stud1.id, rating = 5, comment = "Best Waakye supreme packaging on campus! Must try.", timestamp = System.currentTimeMillis() - 86400000 * 3),
                FoodItemFeedback(id = 4, orderId = 1004, foodItemId = 202, customerId = stud2.id, rating = 4, comment = "Rich soft pounded fufu, the meat soup was extremely rich.", timestamp = System.currentTimeMillis() - 86400000 * 4)
            )

            for (ff in pastFoodFeedback) {
                foodItemFeedbackDao.insertFoodFeedback(ff)
            }

            // Seed logs
            insertAuditLog(99, "SYSTEM_INIT", "Database seed initialised with traditional Accra Technical University menus.")
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Warning: Pre-seeding encountered an exception. Skipping seed safely.", e)
        }
    }

    suspend fun getMyGeminiOrderInsights(): LaravelGeminiInsightResponse = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                LaravelClientManager.getService().getMyGeminiOrderInsights()
            } catch (e: Exception) {
                LaravelGeminiInsightResponse(
                    success = false,
                    vendor_id = 0,
                    vendor_name = "Offline Vendor",
                    insights_markdown = "### Error Loading Order Insights\nUnable to reach server. Please check your connectivity.\n\n`Details: ${e.message}`",
                    generated_at = ""
                )
            }
        } else {
            LaravelGeminiInsightResponse(
                success = true,
                vendor_id = 1,
                vendor_name = "Mock Local Diner",
                insights_markdown = "### Offline Mode Active\nConfigure and enable Laravel core server to run integrated mathematical regression models.",
                generated_at = ""
            )
        }
    }

    suspend fun getVendorGeminiOrderInsights(vendorId: Int): LaravelGeminiInsightResponse = withContext(Dispatchers.IO) {
        if (LaravelClientManager.isLaravelEnabled) {
            try {
                LaravelClientManager.getService().getVendorGeminiOrderInsights(vendorId)
            } catch (e: Exception) {
                LaravelGeminiInsightResponse(
                    success = false,
                    vendor_id = vendorId,
                    vendor_name = "Offline Vendor",
                    insights_markdown = "### Error Loading Order Insights\nUnable to reach backend services.\n\n`Details: ${e.message}`",
                    generated_at = ""
                )
            }
        } else {
            LaravelGeminiInsightResponse(
                success = true,
                vendor_id = vendorId,
                vendor_name = "Mock Local Diner",
                insights_markdown = "### Offline Mode Active\nConfigure and enable Laravel core server to run integrated mathematical regression models.",
                generated_at = ""
            )
        }
    }
}

// ==========================================
// 3. GEMINI PREDICTIVE PERFORMANCE ANALYTICS
// ==========================================
class GeminiAnalyticsRepository {

    suspend fun generateVendorPerformanceReview(
        vendorName: String,
        feedbacks: List<Feedback>,
        totalOrdersCount: Int,
        averageRatings: Map<String, Double>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "To generate real-time predictive analytics:\n" +
                    "1. Set up GEMINI_API_KEY in the Secrets/Env configuration panel.\n\n" +
                    "Offline Local Simulated Prediction: \n" +
                    "Feedback Analysis for '$vendorName':\n" +
                    "• Price Value rating is high (${"%.1f".format(averageRatings["priceValue"] ?: 4.0)}/5), indicating good portion-size ratios on campus.\n" +
                    "• Cleanliness is rated at ${"%.1f".format(averageRatings["cleanliness"] ?: 4.0)}/5. Recommend enforcing strict wastebin intervals.\n" +
                    "• Speed of delivery is ${"%.1f".format(averageRatings["speed"] ?: 4.0)}/5. Peak 12:30 PM bottleneck detected on campus. Recommend mobile pre-cooking."
        }

        // Aggregate comments from the reviews
        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are the Chief Academic Evaluator and Data Analytics Officer for the Accra Technical University (ATU) Cafeteria Board.
            Analyze the following student performance metrics and reviews for the Vendor: '$vendorName'.
            
            HISTOGRAM PERFORMANCE SUMMARY:
            - Total Verified Customer Orders: $totalOrdersCount
            - Mean Metric Ratings (out of 5.0 stars):
              * Food Quality: ${averageRatings["foodQuality"] ?: 0.0}
              * Booth Cleanliness: ${averageRatings["cleanliness"] ?: 0.0}
              * Service Speed: ${averageRatings["speed"] ?: 0.0}
              * Price-to-Value Ratio: ${averageRatings["priceValue"] ?: 0.0}
              
            STUDENT REVIEWS & TRANSCRIPTS:
            $reviewSummary
            
            Based on the data above, provide a structured management consulting report containing exactly the following three sections (use neat, professional Markdown text, formatted beautifully for a mobile dashboard):
            
            1. **🏆 Performance Diagnostics Scorecard**: Interpret the mean score combinations. Highlight their specific competitive strengths and operational bottlenecks in the ATU campus environment.
            
            2. **📈 Predictive Demand & Bottleneck Warnings**: Predict what periods or meal types are likely to cause issues based on the student comments, and mention expected crowding patterns during Accra campus peak hours (e.g., 11:30 AM to 1:30 PM).
            
            3. **💡 Strategic University Directives**: Deliver exactly 3 highly specific, localized action items (e.g. food prep instructions, waste control, digitised queuing) that the vendor must implement to comply with ATU hygiene and efficiency standards. Keep the tone insightful, academic, encouraging, and highly professional.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight received from ATU Analytics, try again later."
        } catch (e: Exception) {
            Log.e("GeminiAnalytics", "Error communicating with Gemini", e)
            "Offline Simulation Mode (Network/API Limit reached): \n\n" +
                    "### 🏆 Performance Diagnostics Scorecard\n" +
                    "• **Strengths**: Excelling on Price-to-Value ratios for ATU student budgets, keeping food local and appetizing.\n" +
                    "• **Bottlenecks**: Pounded food preparation and Jollof peak crowding delays service speed during lunch hour transitions.\n\n" +
                    "### 📈 Predictive Demand & Bottleneck Warnings\n" +
                    "• High queue lengths are simulated on Tuesdays/Thursdays between 11:45 AM and 1:15 PM following morning lectures. Demand for Sobolo spikes alongside temperature peaks. \n\n" +
                    "### 💡 Strategic University Directives\n" +
                    "1. **Pre-portion Waakye Shito Sides**: Pre-packaging standard student packages before 11:30 AM will cut serving times by 40%.\n" +
                    "2. **Implement Dual-Line Service**: Have separate channels for cash/PIN-verification pickups and queue orders.\n" +
                    "3. **Campus Hygiene Protocol**: Arrange structured cleaning sweeps at 11:00 AM and 2:00 PM."
        }
    }

    suspend fun generateVendorSentimentAnalysis(
        vendorName: String,
        feedbacks: List<Feedback>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "To generate real-time sentiment analytics:\n" +
                    "1. Set up GEMINI_API_KEY in the Secrets/Env configuration panel.\n\n" +
                    "Offline Local Simulated Sentiment Analysis:\n\n" +
                    "### 📊 Overall Sentiment Balance\n" +
                    "🟢 **Positive**: 78% | 🟡 **Neutral**: 14% | 🔴 **Negative**: 8%\n\n" +
                    "### 🏆 Key Praise & Strengths\n" +
                    "• **Value for Money**: Students consistently highlight generous portions of Waakye relative to prices.\n" +
                    "• **Taste & Spiciness**: Shito and chicken seasoning received high praise across multiple comments.\n\n" +
                    "### ⚠️ Key Friction Points & Complaints\n" +
                    "• **Queue Waiting Bottlenecks**: Peak lunch transit congestion at 12:15 PM remains student friction point.\n" +
                    "• **Order status signaling**: Students noted that sometimes orders are marked 'Ready' but are still being boxed."
        }

        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are an advanced Customer Sentiment and Linguistic Specialist for the Accra Technical University (ATU) Cafeteria Board.
            Perform a thorough qualitative sentiment analysis on student reviews and order comments for Vendor: '$vendorName'.
            
            STUDENT REVIEWS & TRANSCRIPTS:
            $reviewSummary
            
            Based on this raw conversational text, generate a beautiful, concise, polished sentiment report. Your output must contain:
            
            1. **📊 Sentiment Balance Breakdown**: Provide estimated percentages for Positive, Neutral, and Negative sentiments based on comments and rating distributions.
            2. **👍 Praise Highlights**: Summarize the leading aspects that students are happy about (e.g., taste, hygiene, hospitality, portion sizes).
            3. **👎 Critical Actionable Friction Points**: Identify specific student complaints, pain points, or constructive criticism in their descriptions.
            4. **💡 Executive Recommendation**: Give a 2-sentence summary recommendation to improve student experiences.
            
            Keep the content highly structured, engaging, and professional for a mobile dashboard. Use bold markdown headers and formatting.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No sentiment insight received, try again later."
        } catch (e: Exception) {
            Log.e("GeminiSentiment", "Error communicating with Gemini", e)
            "Offline Simulation Mode (Network/API Limit reached): \n\n" +
                    "### 📊 Overall Sentiment Balance\n" +
                    "🟢 **Positive**: 78% | 🟡 **Neutral**: 14% | 🔴 **Negative**: 8%\n\n" +
                    "### 🏆 Key Praise & Strengths\n" +
                    "• **Value for Money**: Students consistently highlight generous portions of Waakye relative to prices.\n" +
                    "• **Taste & Spiciness**: Shito and chicken seasoning received high praise across multiple comments.\n\n" +
                    "### ⚠️ Key Friction Points & Complaints\n" +
                    "• **Queue Waiting Bottlenecks**: Peak lunch transit congestion at 12:15 PM remains student friction point.\n" +
                    "• **Order status signaling**: Students noted that sometimes orders are marked 'Ready' but are still being boxed."
        }
    }

    suspend fun generateVendorAutoReplies(
        vendorName: String,
        feedbacks: List<Feedback>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "Offline Local Simulated Response Templates:\n\n" +
                    "### 📝 Template 1: For Service Speed/Waiting Complaints\n" +
                    "\"Dear Student, thank you for your valuable feedback. We are sincerely sorry you experienced a delay during peak hours. ATU Cafeteria values your time, and we are implementing pre-packaging and dual lines next week to speed up order collection. We hope to serve you better next time! - $vendorName\"\n\n" +
                    "### 📝 Template 2: For Food Quality/Portion Complaints\n" +
                    "\"Hello! Thank you for sharing your experience. We take food quality seriously. We want to ensure you get the best value for your money. Please show this message to our manager on your next visit so we can make this right. - $vendorName\"\n\n" +
                    "### 📝 Template 3: For Booth Hygiene/Cleanliness Complaints\n" +
                    "\"Thank you for bringing this to our attention. We are committed to strict hygienic protocols on campus. We have augmented our clean-up sweeps to address this immediately. Thank you for helping us keep ATU clean! - $vendorName\""
        }

        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are a Professional Communications and PR specialist for the Accra Technical University (ATU) Cafeteria Board.
            Your task is to generate professional, polite, and constructive custom auto-reply response templates for Vendor: '$vendorName' to use when responding to critical or negative student reviews and comments.
            
            STUDENT REVIEWS & COMMENTS:
            $reviewSummary

            Generate 3 customized, highly professional, polite response templates tailored to the specific friction points, complaints, or negative aspects found in the reviews (such as slow service, cleanliness issues, taste, or price-value mismatch). Each template should be actionable and present a solution.
            
            Format your response beautifully with:
            - Clear Markdown headers (e.g., ### 📝 Template 1: [Topic Title])
            - Brief explanation of when the vendor should use each template.
            - The actual ready-to-copy placeholder response text enclosed in professional quotes.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No template suggestions received, please try again."
        } catch (e: Exception) {
            Log.e("GeminiAutoReply", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 📝 Template 1: For Service Speed/Waiting Complaints\n" +
                    "\"Dear Student, thank you for your valuable feedback. We are sincerely sorry you experienced a delay during peak hours. ATU Cafeteria values your time, and we are implementing pre-packaging and dual lines next week to speed up order collection. We hope to serve you better next time! - $vendorName\"\n\n" +
                    "### 📝 Template 2: For Food Quality/Portion Complaints\n" +
                    "\"Hello! Thank you for sharing your experience. We take food quality seriously. We want to ensure you get the best value for your money. Please show this message to our manager on your next visit so we can make this right. - $vendorName\"\n\n" +
                    "### 📝 Template 3: For Booth Hygiene/Cleanliness Complaints\n" +
                    "\"Thank you for bringing this to our attention. We are committed to strict hygienic protocols on campus. We have augmented our clean-up sweeps to address this immediately. Thank you for helping us keep ATU clean! - $vendorName\""
        }
    }

    suspend fun generateMenuPricingSuggestions(
        vendorName: String,
        orders: List<Order>,
        foodItems: List<FoodItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "Offline Local Simulated Suggestions:\n\n" +
                    "### ☀️ Breakfast Peak Hour Suggestions (8:00 AM - 10:30 AM)\n" +
                    "• **Special**: 'Rise & Shine Porridge Combo' (Koko + Egg + Bread) reduced from GH₵ 18.00 to **GH₵ 15.00**.\n" +
                    "• **Pricing Strategy**: Maintain current prices for single pastries as they are highly price-elastic for students first thing in the morning.\n\n" +
                    "### 🍚 Lunch Rush hour Suggestions (11:30 AM - 2:00 PM)\n" +
                    "• **Dynamic Pricing**: Waakye premium packages with fish & egg can support a **5% peak price increase** (GH₵ 30 to GH₵ 31.50) due to high demand.\n" +
                    "• **Special Combo**: 'ATU Lunch Champion' (Waakye + Sobolo) bundled for GH₵ 35.00 (saves 12% compared to separate purchases).\n\n" +
                    "### 🍹 Afternoon Slack Hour Suggestions (2:30 PM - 5:00 PM)\n" +
                    "• **Specials**: 'Happy Hour Drinks': Discount Sobolo and fresh juices by **20%** to generate traffic during lecture intervals."
        }

        val cal = java.util.Calendar.getInstance()
        var breakfastCount = 0
        var lunchCount = 0
        var dinnerCount = 0
        
        val breakfastItems = java.util.HashMap<String, Int>()
        val lunchItems = java.util.HashMap<String, Int>()
        val dinnerItems = java.util.HashMap<String, Int>()

        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val name = order.foodName
            
            when (hour) {
                in 6..10 -> {
                    breakfastCount += qty
                    breakfastItems[name] = (breakfastItems[name] ?: 0) + qty
                }
                in 11..14 -> {
                    lunchCount += qty
                    lunchItems[name] = (lunchItems[name] ?: 0) + qty
                }
                else -> {
                    dinnerCount += qty
                    dinnerItems[name] = (dinnerItems[name] ?: 0) + qty
                }
            }
        }

        val menuListStr = foodItems.joinToString("\n") { "• ${it.name} (${it.category}) - GH₵ ${"%.2f".format(it.price)}" }

        val bSorted = breakfastItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }
        val lSorted = lunchItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }
        val dSorted = dinnerItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }

        val prompt = xmlDocClean("""
            You are an expert hospitality consultant and algorithmic pricing strategist for the Accra Technical University (ATU) Cafeteria Board.
            Analyze the following menu and actual purchase demand logs for the Vendor '$vendorName' to suggest optimal menu pricing strategies and daily specials.
            
            CURRENT MENU OVERVIEW:
            $menuListStr
            
            REAL-TIME PURCHASE PATTERNS (By Time of Day):
            
            1. **Breakfast Period (6:00 AM - 10:59 AM)**:
               - Total items ordered: $breakfastCount units
               - Top selling dishes: ${if (bSorted.isEmpty()) "No data yet" else bSorted}
               
            2. **Lunch Period (11:00 AM - 2:59 PM)**:
               - Total items ordered: $lunchCount units
               - Top selling dishes: ${if (lSorted.isEmpty()) "No data yet" else lSorted}
               
            3. **Late Afternoon & Evening (3:00 PM onwards)**:
               - Total items ordered: $dinnerCount units
               - Top selling dishes: ${if (dSorted.isEmpty()) "No data yet" else dSorted}
               
            Based on these realistic demand trends, provide a structured intelligence report containing exactly:
            
            - **☀️ Breakfast Hour pricing & specials**: Analyze if early lecture slots justify breakfast combo bundles (e.g. porridge & pastry combos) and recommend pricing.
            - **🍚 Lunch Rush Peak strategies**: Since lunch is the most crowded period at ATU, should the vendor use dynamic pricing (slight premium on waakye/jollof peak demand) or meal/drink combos (e.g., adding Sobolo) to speed up lines? Recommend exact price adjustments.
            - **🍹 Off-Peak Happy Hour promotions**: Propose discounts or clearance pricing to encourage sales during off-peak windows (2:30 PM - 4:30 PM) to clear stock of perishables.
            
            Keep the report beautifully styled with bullet points, bold percentages, and bold pricing figures (GH₵) so vendors can read them instantly on their phone dashboard.
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No price recommendations generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiPricing", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### ☀️ Breakfast Peak Hour Suggestions (8:00 AM - 10:30 AM)\n" +
                    "• **Special**: 'Rise & Shine Porridge Combo' (Koko + Egg + Bread) reduced from GH₵ 18.00 to **GH₵ 15.00**.\n" +
                    "• **Pricing Strategy**: Maintain current prices for single pastries as they are highly price-elastic for students first thing in the morning.\n\n" +
                    "### 🍚 Lunch Rush hour Suggestions (11:30 AM - 2:00 PM)\n" +
                    "• **Dynamic Pricing**: Waakye premium packages with fish & egg can support a **5% peak price increase** (GH₵ 30 to GH₵ 31.50) due to high demand.\n" +
                    "• **Special Combo**: 'ATU Lunch Champion' (Waakye + Sobolo) bundled for GH₵ 35.00 (saves 12% compared to separate purchases).\n\n" +
                    "### 🍹 Afternoon Slack Hour Suggestions (2:30 PM - 5:00 PM)\n" +
                    "• **Specials**: 'Happy Hour Drinks': Discount Sobolo and fresh juices by **20%** to generate traffic during lecture intervals."
        }
    }

    suspend fun generateTodayInsights(
        vendorName: String,
        orders: List<Order>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        val cal = java.util.Calendar.getInstance()
        val hourlyCount = IntArray(24)
        val hourlyRevenue = DoubleArray(24)
        val itemQuantities = java.util.HashMap<String, Int>()
        val itemRevenue = java.util.HashMap<String, Double>()
        var totalRev = 0.0
        var totalQty = 0
        
        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val price = order.totalPrice
            val name = order.foodName
            
            hourlyCount[hour] += qty
            hourlyRevenue[hour] += price
            itemQuantities[name] = (itemQuantities[name] ?: 0) + qty
            itemRevenue[name] = (itemRevenue[name] ?: 0.0) + price
            totalRev += price
            totalQty += qty
        }
        
        // Find busiest hour range
        var busiestHourIndex = -1
        var maxHourlyQty = 0
        for (h in 0..23) {
            if (hourlyCount[h] > maxHourlyQty) {
                maxHourlyQty = hourlyCount[h]
                busiestHourIndex = h
            }
        }
        
        val busiestHourStr = if (busiestHourIndex != -1) {
            val startHour = busiestHourIndex
            val endHour = (busiestHourIndex + 1) % 24
            val startAmPm = if (startHour >= 12) "PM" else "AM"
            val displayStart = if (startHour % 12 == 0) 12 else startHour % 12
            val endAmPm = if (endHour >= 12) "PM" else "AM"
            val displayEnd = if (endHour % 12 == 0) 12 else endHour % 12
            "$displayStart $startAmPm - $displayEnd $endAmPm"
        } else {
            "No orders logged today yet."
        }
        
        // Find most ordered item
        val topItem = itemQuantities.entries.maxByOrNull { it.value }
        val topItemStr = if (topItem != null) {
            "${topItem.key} (${topItem.value} units - GH₵ ${"%.2f".format(itemRevenue[topItem.key] ?: 0.0)})"
        } else {
            "No orders logged today yet."
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            if (orders.isEmpty()) {
                return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                        "Offline Local Simulated Today's Insights:\n\n" +
                        "• **Busiest Hour**: Lunch Rush (12:00 PM - 1:30 PM) is usually the peak. Jollof and Waakye sales spike by over **35%**.\n" +
                        "• **Most Demanded Item**: Waakye Premium Combo (with fish, egg, and extra shito) is expected to be the most ordered item.\n" +
                        "• **Vendor Pro-Tip**: Prepare Sobolo packaging and dynamic combo bundles before 11:30 AM to minimize queue bottlenecks."
            } else {
                return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                        "Offline Local Simulated Today's Insights:\n\n" +
                        "### 📊 Today's Live Sales Analytics\n" +
                        "• **Busiest Hour**: **$busiestHourStr**\n" +
                        "• **Most Ordered Item**: **$topItemStr**\n" +
                        "• **Total Revenue**: **GH₵ ${"%.2f".format(totalRev)}** across **$totalQty** items ordered.\n\n" +
                        "### 💡 Smart Recommendations for $vendorName\n" +
                        "1. **Peak Demand Action**: Your busiest window was around **$busiestHourStr**. Consider preparing pre-packaged portions 15 minutes before this peak to serve students instantaneously!\n" +
                        "2. **Menu Focus**: **$topItemStr** is leading your sales today. Ensure ingredients are adequately stocked to avoid missing out on late-afternoon orders.\n" +
                        "3. **Dynamic Bundle**: Bundle your top seller with Sobolo for an elegant multi-item savings deal (e.g. GH₵ 2.00 off combo) to increase average ticket size."
            }
        }

        val prompt = xmlDocClean("""
            You are an expert AI business intelligence analyst for the Accra Technical University (ATU) Cafeteria Board.
            Analyze today's live sales transactions for the vendor '$vendorName' to generate critical operational insights and actionable advice.
            
            TODAY'S RAW SALES METRICS:
            - **Total Sales Revenue Today**: GH₵ ${"%.2f".format(totalRev)}
            - **Total Items Sold**: $totalQty units
            - **Busiest Registered Hour**: $busiestHourStr
            - **Most Demanded Menu Item**: $topItemStr
            
            FEEDBACK & SALES TRANSACTION LOGS DETAIL:
            ${if (orders.isEmpty()) "No orders recorded yet." else orders.joinToString("\n") { "• Order #${it.id}: ${it.foodName} (QTY: ${it.quantity}), Revenue: GH₵ ${"%.2f".format(it.totalPrice)}, Status: ${it.status}, Customer ID: ${it.customerId}" }}
            
            Based on these realistic intraday sales trends, generate a structured intelligence update containing:
            
            - **🔥 Peak Intensity analysis**: Report on the 'Busiest Hour' ($busiestHourStr) with tips on how $vendorName can handle this rush (e.g. queue management or pre-prep).
            - **✨ Star Product performance**: Analyze the 'Most Ordered Item' ($topItemStr) and dynamic suggestions to cross-promote it or manage inventory around it.
            - **💡 Immediate Operational Pro-Tip**: A bold, highly practical tip tailored specifically to today's transaction size and items to optimize profitability or food waste.
            
            Keep the report beautifully styled with concise bullets, bold keys, and clear pricing symbols (GH₵) so vendors can read and digest them in seconds. Keep it compact!
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No today's insights generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiTodayInsights", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 📊 Today's Live Sales Analytics\n" +
                    "• **Busiest Hour**: **$busiestHourStr**\n" +
                    "• **Most Ordered Item**: **$topItemStr**\n" +
                    "• **Total Revenue**: **GH₵ ${"%.2f".format(totalRev)}** across **$totalQty** items ordered.\n\n" +
                    "### 💡 Smart Recommendations for $vendorName\n" +
                    "1. **Peak Demand Action**: Your busiest window was around **$busiestHourStr**. Consider preparing pre-packaged portions 15 minutes before this peak to serve students instantaneously!"
        }
    }

    suspend fun generateHistoricalOrderInsights(
        vendorName: String,
        orders: List<Order>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Compute historical metrics locally
        var totalRev = 0.0
        var totalQty = 0
        val hourlyCount = IntArray(24)
        val itemQuantities = java.util.HashMap<String, Int>()
        val itemRevenue = java.util.HashMap<String, Double>()
        
        val cal = java.util.Calendar.getInstance()
        
        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val price = order.totalPrice
            val name = order.foodName
            
            hourlyCount[hour] += qty
            itemQuantities[name] = (itemQuantities[name] ?: 0) + qty
            itemRevenue[name] = (itemRevenue[name] ?: 0.0) + price
            totalRev += price
            totalQty += qty
        }
        
        // Find busiest hour
        var busiestHourIndex = -1
        var maxHourlyQty = 0
        for (h in 0..23) {
            if (hourlyCount[h] > maxHourlyQty) {
                maxHourlyQty = hourlyCount[h]
                busiestHourIndex = h
            }
        }
        
        val busiestHourStr = if (busiestHourIndex != -1) {
            val startHour = busiestHourIndex
            val endHour = (busiestHourIndex + 1) % 24
            val startAmPm = if (startHour >= 12) "PM" else "AM"
            val displayStart = if (startHour % 12 == 0) 12 else startHour % 12
            val endAmPm = if (endHour >= 12) "PM" else "AM"
            val displayEnd = if (endHour % 12 == 0) 12 else endHour % 12
            "$displayStart $startAmPm - $displayEnd $endAmPm"
        } else {
            "No historical orders recorded yet."
        }
        
        // Find top selling food items
        val sortedPopularItems = itemQuantities.entries.sortedByDescending { it.value }.take(3)
        val popularItemsStr = sortedPopularItems.joinToString("\n") { 
            "• **${it.key}**: Sold **${it.value} units**, yielding total sales of **GH₵ ${"%.2f".format(itemRevenue[it.key] ?: 0.0)}**."
        }
        
        val topItem = sortedPopularItems.firstOrNull()?.key ?: "signature meals"
        val topItemQty = sortedPopularItems.firstOrNull()?.value ?: 0
        val topItemRev = itemRevenue[topItem] ?: 0.0
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "### 📈 Gemini Intelligence: Historical Order Analytics Summary for **$vendorName**\n" +
                    "*(Local Smart Fallback Report — Active Data Aggregation Running Live)*\n\n" +
                    "An analysis of **${orders.size} completed transactions** shows heavy student demand and high-contrast purchase peaks sync'd to Accra Technical University's lecture calendar.\n\n" +
                    "---\n\n" +
                    "### 1. 🔥 Peak Traffic Density & Order Velocity\n" +
                    "• **Absolute Peak Hour**: **$busiestHourStr** represents the absolute highest ordering concentration, accounting for major delivery lines.\n" +
                    "• **Operational Strategy**: Prepare portion prep **20 minutes before $busiestHourStr**. Setup a dual-line checkout (split for digital pre-orders vs walk-in ordering) to optimize fulfillment.\n\n" +
                    "---\n\n" +
                    "### 2. 🍔 Core Menu Popularity Index (Top Dish Assessment)\n" +
                    "If details are available:\n" +
                    (if (sortedPopularItems.isEmpty()) "• No menu trends database recorded yet." else popularItemsStr) + "\n\n" +
                    "• **Strategic Recommendation**: Introduce a **'Star Combo Promo'** combining **$topItem** with a popular refreshing drink to boost overall transaction size.\n\n" +
                    "---\n\n" +
                    "### 3. 🔋 Kitchen Resource & Supply Chain Guidance\n" +
                    "• **Inventory Buffer**: Maintain a **20% stock surplus of ingredients** for **$topItem** on heavy lecture days to avoid missing late-stage demand.\n" +
                    "• **Fulfillment Prep**: Standardize prep timing to ensure standard hand-offs during peak rush intervals do not exceed **4-6 minutes per student**."
        }

        val prompt = xmlDocClean("""
            You are a lead institutional restaurant analyst and predictive supply chain strategist at Accra Technical University (ATU).
            Please review this aggregated historical dataset from the 'orders' table for vendor '$vendorName':
            
            HISTORICAL ORDERS SUMMARY:
            - Total Completed Transactions: ${orders.size}
            - Total Cumulative Revenue: GH₵ ${"%.2f".format(totalRev)}
            - Absolute Single Busiest Peak Hour: $busiestHourStr
            
            STAR FOOD ITEMS & POPULARITY:
            ${if (sortedPopularItems.isEmpty()) "No data logged." else sortedPopularItems.joinToString("\n") { "• ${it.key}: Sold ${it.value} units, Revenue of GH₵ ${"%.2f".format(itemRevenue[it.key] ?: 0.0)}" }}
            
            Please construct a comprehensive, action-oriented predictive demand and culinary intelligence report in clean Markdown format with the following pillars:
            
            1. **🔥 Peak Hour Traffic Density & Bottlenecks**: Analyze their busiest windows specifically around $busiestHourStr. Suggest how to adjust service velocity or introduce digital pre-orders to navigate these peak university lecture breaks.
            2. **🍔 Core Menu Popularity Index**: Analyze the top selling items. Suggest how they can bundle slower-moving products with popular items to drive larger orders.
            3. **🔋 Kitchen Resource Planning Guidance**: Give tailored guidance on preparing ingredients beforehand to prevent running out of food, minimizing local wait times, and preventing daily surplus waste.
            
            Keep the report beautifully styled, concise, encouraging, and highly professional. Limit to 350-400 words.
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No historical order insights generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiHistoricalOrderInsights", "Error communicating with Gemini", e)
            "### 📈 Gemini Intelligence: Historical Order Analytics Summary for **$vendorName**\n" +
                    "*(Local Smart Fallback Report — Active Data Aggregation Running Live)*\n\n" +
                    "• **Busiest Hour**: $busiestHourStr\n" +
                    "• **Top Food Performance**:\n" +
                    popularItemsStr + "\n" +
                    "• **Operational Tip**: Prep portion lines 20 minutes before peak sessions."
        }
    }

    suspend fun generateNutritionCoaching(
        dailyTargetKcal: Float,
        currentKcal: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        availableFoodItems: List<FoodItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key Configuration Error: Gemini API key has not been entered into the Secrets tab.\n\n" +
                    "Offline Local Simulated Diet Coaching:\n\n" +
                    "### 🛡️ Daily Macro Analysis\n" +
                    "• **Calories**: ${currentKcal.toInt()} / ${dailyTargetKcal.toInt()} kcal (${"%.1f".format(if (dailyTargetKcal > 0) (currentKcal / dailyTargetKcal) * 100 else 0f)}% met)\n" +
                    "• **Protein**: ${protein.toInt()}g (Target: 130g) - " + (if (protein < 50) "Critical deficit! Increase lean meat or egg intake." else "A healthy foundation!") + "\n" +
                    "• **Carbs**: ${carbs.toInt()}g - Primary energy source for active lectures.\n" +
                    "• **Fats**: ${fat.toInt()}g - Balanced dietary lipids.\n\n" +
                    "### 🍏 Smart Meal Recommendations\n" +
                    "1. **High-Protein Option**: Choose **Waakye with Egg & Fish** or **Pounded Yam with Goat Soup** from the cafeteria to fulfill your protein targets while respecting your ${dailyTargetKcal.toInt()} kcal budget.\n" +
                    "2. **Hydration Boost**: Pair with local **Sobolo** or a light beverage instead of soda to minimize blood sugar spikes durings lecturings.\n\n" +
                    "### 💡 Lifestyle Tips\n" +
                    "• Try to allocate 40% of your calorie consumption for breakfast and lunch. Avoid heavy carbohydrate menus after 6:00 PM to improve sleep quality."
        }

        val foodMenuStr = availableFoodItems.joinToString("\n") {
            "• ${it.name} (Price: GH₵ ${it.price}, Category: ${it.category}, Description: ${it.description})"
        }

        val prompt = """
            You are an expert Sports Nutritionist and Academic Health Advisor representing the Accra Technical University (ATU) Cafeteria Wellness Board.
            Analyze the following student's modern nutrition state and available campus diner menus:
            
            DAILY GOALS & CURRENT TARGETS:
            - Target Calorie Budget: ${dailyTargetKcal.toInt()} kcal
            - Calories Logged Today: ${currentKcal.toInt()} kcal
            - Macronutrients Logged Today:
              * Protein: ${protein.toInt()}g
              * Carbohydrates: ${carbs.toInt()}g
              * Fats: ${fat.toInt()}g
              
            AVAILABLE DISHES AT ATU CAFETERIA:
            $foodMenuStr
            
            Generate a personalized health advisor report in beautiful Markdown containing exactly these sections:
            1. **🎯 Personalized Calorie & Macro Scorecard**: Assess whether their current macros are balanced, noting deficiencies or excess (e.g. low protein, too much carbs).
            2. **🍽️ Customized Meal Recommendations**: Recommend exactly 2 matching dishes from the provided ATU cafeteria menu list that will optimize their macros and stay within budget. Specify the price and vendor name if applicable.
            3. **⚡ Nutrition Advice for ATU Studies**: Give 2 practical wellness tips to stay focused during lectures, avoid "food coma" drowsiness during Accra afternoon humidity, or build lean muscle.
            
            Make sure your response has a bright, encouraging, supportive tone. Limit the response to 400 words.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No nutrition insight generated."
        } catch (e: Exception) {
            Log.e("GeminiNutrition", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 🛡️ Daily Macro Analysis\n" +
                    "• **Calories**: ${currentKcal.toInt()} / ${dailyTargetKcal.toInt()} kcal\n" +
                    "• **Protein**: ${protein.toInt()}g logged vs 130g goal\n\n" +
                    "### 🍏 Smart Meal Recommendations\n" +
                    "• **High Protein**: Select double eggs with Waakye from the local stands to lift your macro density!"
        }
    }

    private fun xmlDocClean(input: String): String {
        return input.replace("<", "&lt;").replace(">", "&gt;")
    }
}
