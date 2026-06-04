package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. DATABASE ENTITIES
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
    @com.squareup.moshi.Json(name = "is_open") @ColumnInfo(defaultValue = "1") val isOpen: Boolean = true
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
    val lowStockThreshold: Int = 15
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
    val estimatedPickupTime: String = "Calculating..."
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

// ==========================================
// 2. DATA ACCESS OBJECTS (DAOs)
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

    @Delete
    suspend fun deleteFoodItem(item: FoodItem)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY orderTimestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

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

// ==========================================
// 3. APPDATABASE HOLDER
// ==========================================

@Database(
    entities = [
        User::class,
        FoodItem::class,
        Order::class,
        Feedback::class,
        AuditLog::class,
        WalletTransaction::class,
        FoodItemFeedback::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun foodItemDao(): FoodItemDao
    abstract fun orderDao(): OrderDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun walletTransactionDao(): WalletTransactionDao
    abstract fun foodItemFeedbackDao(): FoodItemFeedbackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atu_cafeteria_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
