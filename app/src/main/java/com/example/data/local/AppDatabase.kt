package com.example.data

import android.content.Context
import androidx.room.*

// ==========================================
// APPDATABASE HOLDER
// ==========================================

@Database(
    entities = [
        User::class,
        FoodItem::class,
        Order::class,
        Feedback::class,
        AuditLog::class,
        WalletTransaction::class,
        FoodItemFeedback::class,
        VendorMenuAvailability::class,
        VendorOrderSummary::class,
        OfflineOrder::class,
        ChatMessage::class
    ],
    version = 14,
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
    abstract fun vendorMenuAvailabilityDao(): VendorMenuAvailabilityDao
    abstract fun vendorOrderSummaryDao(): VendorOrderSummaryDao
    abstract fun offlineOrderDao(): OfflineOrderDao
    abstract fun chatMessageDao(): ChatMessageDao

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
                .fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
