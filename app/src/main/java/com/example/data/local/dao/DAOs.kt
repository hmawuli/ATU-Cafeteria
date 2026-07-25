package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Int): Flow<User?>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserSync(id: Int): User?

    @Query("SELECT * FROM users WHERE role = 'VENDOR'")
    fun getAllVendors(): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Int)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
}

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_items")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE vendorId = :vendorId")
    fun getFoodItemsByVendor(vendorId: Int): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodItemById(id: Int): FoodItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(item: FoodItem): Long

    @Update
    suspend fun updateFoodItem(item: FoodItem)

    @Query("UPDATE food_items SET isAvailable = :isAvailable WHERE id IN (:ids)")
    suspend fun bulkToggleAvailability(ids: List<Int>, isAvailable: Boolean)

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY orderTimestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersSync(): List<Order>

    @Query("SELECT * FROM orders WHERE customerId = :customerId ORDER BY orderTimestamp DESC")
    fun getOrdersForCustomer(customerId: Int): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE vendorId = :vendorId ORDER BY orderTimestamp DESC")
    fun getOrdersForVendor(vendorId: Int): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Int): Order?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)
}

@Dao
interface FeedbackDao {
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<Feedback>>

    @Query("SELECT * FROM feedback WHERE vendorId = :vendorId ORDER BY timestamp DESC")
    fun getFeedbackForVendor(vendorId: Int): Flow<List<Feedback>>

    @Query("SELECT * FROM feedback WHERE orderId = :orderId LIMIT 1")
    suspend fun getFeedbackByOrderId(orderId: Int): Feedback?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: Feedback): Long

    @Update
    suspend fun updateFeedback(feedback: Feedback)

    @Query("UPDATE feedback SET vendorReply = :vendorReply WHERE id = :feedbackId")
    suspend fun updateFeedbackReply(feedbackId: Int, vendorReply: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog): Long
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllWalletTransactions(): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWalletTransactionsForUser(userId: Int): Flow<List<WalletTransaction>>

    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getWalletTransactionsForUserSync(userId: Int): List<WalletTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalletTransaction(transaction: WalletTransaction): Long
}

@Dao
interface FoodItemFeedbackDao {
    @Query("SELECT * FROM food_item_feedback ORDER BY timestamp DESC")
    fun getAllFoodFeedback(): Flow<List<FoodItemFeedback>>

    @Query("SELECT * FROM food_item_feedback WHERE foodItemId = :foodItemId ORDER BY timestamp DESC")
    fun getFeedbackForFoodItem(foodItemId: Int): Flow<List<FoodItemFeedback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodFeedback(feedback: FoodItemFeedback): Long
}

@Dao
interface VendorMenuAvailabilityDao {
    @Query("SELECT * FROM vendor_menu_availabilities WHERE vendorId = :vendorId")
    fun getAvailabilitiesForVendor(vendorId: Int): Flow<List<VendorMenuAvailability>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: VendorMenuAvailability): Long

    @Query("DELETE FROM vendor_menu_availabilities WHERE id = :id")
    suspend fun deleteAvailability(id: Int)
}

@Dao
interface VendorOrderSummaryDao {
    @Query("SELECT * FROM vendor_order_summaries WHERE vendorId = :vendorId ORDER BY summaryDate DESC")
    fun getSummariesForVendor(vendorId: Int): Flow<List<VendorOrderSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: VendorOrderSummary): Long
}

@Dao
interface OfflineOrderDao {
    @Query("SELECT * FROM offline_orders ORDER BY timestamp ASC")
    suspend fun getAllOfflineOrders(): List<OfflineOrder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineOrder(order: OfflineOrder): Long

    @Delete
    suspend fun deleteOfflineOrder(order: OfflineOrder)

    @Query("DELETE FROM offline_orders")
    suspend fun clearOfflineOrders()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY timestamp ASC")
    fun getMessagesForOrder(orderId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE recipientId = :userId OR senderId = :userId ORDER BY timestamp DESC")
    fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long
}


