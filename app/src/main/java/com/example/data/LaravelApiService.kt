package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// ==========================================
// 1. DATA TRANSFER OBJECTS (DTOs)
// ==========================================

@JsonClass(generateAdapter = true)
data class LaravelRegisterRequest(
    val username: String,
    val pin: String, // SHA-256 hashed PIN
    val role: String,
    val fullName: String,
    val info: String
)

@JsonClass(generateAdapter = true)
data class LaravelStudentRegisterRequest(
    val username: String,
    val pin: String,
    val fullName: String,
    val studentId: String
)

@JsonClass(generateAdapter = true)
data class LaravelVendorRegisterRequest(
    val username: String,
    val pin: String,
    val fullName: String,
    val boothDescription: String
)

@JsonClass(generateAdapter = true)
data class LaravelSanctumAuthResponse(
    val success: Boolean,
    val message: String,
    val user: User
)

@JsonClass(generateAdapter = true)
data class LaravelLoginRequest(
    val username: String,
    val pin: String // SHA-256 hashed PIN
)

@JsonClass(generateAdapter = true)
data class LaravelFoodItem(
    val id: Int,
    val vendor_id: Int,
    val name: String,
    val price: Double,
    val category: String,
    val image_url: String?,
    val description: String?,
    val is_available: Boolean,
    val calories: Int? = null,
    val allergens: String? = null
)

@JsonClass(generateAdapter = true)
data class LaravelAddFoodRequest(
    val vendor_id: Int,
    val name: String,
    val price: Double,
    val category: String,
    val description: String,
    val image_url: String
)

@JsonClass(generateAdapter = true)
data class LaravelUpdateFoodRequest(
    val name: String?,
    val price: Double?,
    val category: String?,
    val description: String?,
    val image_url: String?,
    val is_available: Boolean?
)

@JsonClass(generateAdapter = true)
data class LaravelOrder(
    val id: Int,
    val customer_id: Int,
    val vendor_id: Int,
    val food_item_id: Int,
    val food_name: String,
    val quantity: Int,
    val unit_price: Double,
    val total_price: Double,
    val order_timestamp: Long,
    val status: String,
    val pickup_pin: String,
    val estimated_pickup_time: String,
    val points_redeemed: Int? = 0,
    val discount_applied: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class LaravelAddOrderRequest(
    val customer_id: Int,
    val vendor_id: Int,
    val food_item_id: Int,
    val food_name: String,
    val quantity: Int,
    val unit_price: Double,
    val total_price: Double,
    val points_to_redeem: Int? = 0,
    val estimated_pickup_time: String? = null
)

@JsonClass(generateAdapter = true)
data class LaravelUpdateOrderStatusRequest(
    val status: String,
    val estimated_pickup_time: String?
)

@JsonClass(generateAdapter = true)
data class LaravelVerifyPickupRequest(
    val vendor_id: Int,
    val pickup_pin: String
)

@JsonClass(generateAdapter = true)
data class LaravelVerifyPickupResponse(
    val success: Boolean,
    val message: String,
    val order: LaravelOrder?
)

@JsonClass(generateAdapter = true)
data class LaravelFeedback(
    val id: Int,
    val order_id: Int,
    val vendor_id: Int,
    val customer_id: Int,
    val rating_food_quality: Int,
    val rating_cleanliness: Int,
    val rating_service_speed: Int,
    val rating_price_value: Int,
    val comment: String?,
    val vendor_reply: String? = null,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class LaravelAddFeedbackRequest(
    val order_id: Int,
    val vendor_id: Int,
    val customer_id: Int,
    val food_quality: Int,
    val cleanliness: Int,
    val speed: Int,
    val value: Int,
    val comment: String
)

@JsonClass(generateAdapter = true)
data class LaravelFeedbackReplyRequest(
    val vendor_reply: String
)

@JsonClass(generateAdapter = true)
data class LaravelFoodItemFeedback(
    val id: Int,
    val order_id: Int,
    val food_item_id: Int,
    val customer_id: Int,
    val rating: Int,
    val comment: String?,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class LaravelAddFoodFeedbackRequest(
    val order_id: Int,
    val food_item_id: Int,
    val customer_id: Int,
    val rating: Int,
    val comment: String
)

@JsonClass(generateAdapter = true)
data class LaravelAuditLog(
    val id: Int,
    val user_id: Int,
    val timestamp: Long,
    val action: String,
    val details: String
)

@JsonClass(generateAdapter = true)
data class LaravelAddAuditLogRequest(
    val user_id: Int,
    val action: String,
    val details: String
)

@JsonClass(generateAdapter = true)
data class LaravelGeneralResponse(
    val success: Boolean,
    val message: String
)

@JsonClass(generateAdapter = true)
data class LaravelToggleStatusResponse(
    val success: Boolean,
    val is_open: Boolean,
    val message: String
)

@JsonClass(generateAdapter = true)
data class LaravelDailyPerformance(
    val vendor_id: Int,
    val vendor_name: String,
    val order_date: String,
    val total_orders: Int,
    val completed_orders: Int,
    val daily_revenue: Double
)

@JsonClass(generateAdapter = true)
data class LaravelPerformanceResponse(
    val success: Boolean,
    val daily_performance: List<LaravelDailyPerformance>
)

@JsonClass(generateAdapter = true)
data class LaravelVendorMetric(
    val vendor_id: Int,
    val vendor_name: String,
    val contact_info: String,
    val operational_status: String,
    val total_completed_orders: Int,
    val total_orders: Int,
    val total_sales: Double,
    val avg_completion_time_minutes: Double,
    val avg_completion_time_display: String,
    val average_delivery_time: Double,
    val average_delivery_time_display: String,
    val order_fulfillment_rate: Double
)

@JsonClass(generateAdapter = true)
data class LaravelVendorPerformanceMetricsResponse(
    val success: Boolean,
    val message: String,
    val performance: List<LaravelVendorMetric>,
    val generated_at: String
)

@JsonClass(generateAdapter = true)
data class LaravelDailyRevenueItem(
    val date: String,
    val orders_count: Int,
    val revenue: Double
)

@JsonClass(generateAdapter = true)
data class LaravelDailyRevenueResponse(
    val success: Boolean,
    val vendor_id: Int,
    val vendor_name: String,
    val data: List<LaravelDailyRevenueItem>,
    val generated_at: String
)

@JsonClass(generateAdapter = true)
data class LaravelNotificationData(
    val order_id: Int,
    val vendor_id: Int,
    val total_price: Double,
    val old_status: String,
    val new_status: String,
    val message: String,
    val time: String
)

@JsonClass(generateAdapter = true)
data class LaravelDatabaseNotification(
    val id: String,
    val type: String,
    val notifiable_type: String,
    val notifiable_id: Int,
    val data: LaravelNotificationData,
    val read_at: String?,
    val created_at: String?,
    val updated_at: String?
)

@JsonClass(generateAdapter = true)
data class LaravelNotificationsResponse(
    val success: Boolean,
    val notifications: List<LaravelDatabaseNotification>
)

// ==========================================
// CHAT SUBSYSTEM MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class LaravelChatMessage(
    val id: Int,
    val sender_id: Int,
    val receiver_id: Int,
    val message: String,
    val is_read: Boolean,
    val created_at: String?,
    val updated_at: String?
)

@JsonClass(generateAdapter = true)
data class LaravelSendChatRequest(
    val receiver_id: Int,
    val message: String
)

@JsonClass(generateAdapter = true)
data class LaravelRecentChatPartner(
    val id: Int,
    val username: String,
    val fullName: String,
    val role: String,
    val info: String?,
    val last_message: String?,
    val last_message_time: String?,
    val unread_count: Int
)

@JsonClass(generateAdapter = true)
data class LaravelRecentChatsResponse(
    val success: Boolean,
    val chats: List<LaravelRecentChatPartner>
)

@JsonClass(generateAdapter = true)
data class LaravelConversationResponse(
    val success: Boolean,
    val messages: List<LaravelChatMessage>
)

@JsonClass(generateAdapter = true)
data class LaravelSendChatResponse(
    val success: Boolean,
    val message: String,
    val chat_message: LaravelChatMessage
)

// ==========================================
// PAYSTACK SUBSYSTEM MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class LaravelPaystackInitRequest(
    val email: String,
    val amount: Double,
    val purpose: String // "WALLET_TOPUP", "DIRECT_ORDER_PAY"
)

@JsonClass(generateAdapter = true)
data class LaravelPaystackInitDetails(
    val authorization_url: String,
    val access_code: String,
    val reference: String,
    val amount: Double
)

@JsonClass(generateAdapter = true)
data class LaravelPaystackInitResponse(
    val success: Boolean,
    val message: String,
    val data: LaravelPaystackInitDetails
)

@JsonClass(generateAdapter = true)
data class LaravelPaystackVerifyResponse(
    val success: Boolean,
    val message: String,
    val reference: String,
    val amount: Double,
    val purpose: String
)

@JsonClass(generateAdapter = true)
data class LaravelUpdateMenuAvailabilityRequest(
    val item_id: Int,
    val item_type: String,
    val is_available: Boolean,
    val day_of_week: String? = null,
    val start_time: String? = null,
    val end_time: String? = null
)

@JsonClass(generateAdapter = true)
data class LaravelUpdateMenuAvailabilityResponse(
    val success: Boolean,
    val message: String,
    val item_id: Int,
    val item_type: String,
    val is_available: Boolean
)

@JsonClass(generateAdapter = true)
data class LaravelTodayStatusBreakdown(
    val PENDING: Int = 0,
    val PREPARING: Int = 0,
    val READY: Int = 0,
    val COMPLETED: Int = 0,
    val CANCELLED: Int = 0,
    val DECLINED: Int = 0
)

@JsonClass(generateAdapter = true)
data class LaravelOrderSummaryMetrics(
    val today_orders_count: Int,
    val today_revenue: Double,
    val historic_revenue: Double,
    val average_rating: Double,
    val today_status_breakdown: LaravelTodayStatusBreakdown
)

@JsonClass(generateAdapter = true)
data class LaravelSummaryNotifications(
    val unresolved_pending_count: Int,
    val active_preparing_count: Int,
    val ready_pickup_count: Int
)

@JsonClass(generateAdapter = true)
data class LaravelLowStockWarningItem(
    val id: Int,
    val name: String,
    val price: Double,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class LaravelOrderSummaryResponse(
    val success: Boolean,
    val message: String,
    val summary_date: String,
    val cached_summary_id: Int,
    val metrics: LaravelOrderSummaryMetrics,
    val notifications: LaravelSummaryNotifications,
    val low_stock_warnings: List<LaravelLowStockWarningItem>
)

@JsonClass(generateAdapter = true)
data class LaravelBulkUpdateItem(
    val id: Int,
    val price: Double? = null,
    val is_available: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class LaravelBulkUpdateResponse(
    val success: Boolean,
    val message: String,
    val updated_count: Int,
    val errors: List<String>?,
    val updates_log: List<String>?
)

@JsonClass(generateAdapter = true)
data class LaravelBulkToggleRequest(
    val ids: List<Int>,
    val is_available: Boolean
)

@JsonClass(generateAdapter = true)
data class LaravelGeminiInsightResponse(
    val success: Boolean,
    val vendor_id: Int,
    val vendor_name: String,
    val insights_markdown: String,
    val note: String? = null,
    val generated_at: String
)

// ==========================================
// 2. RETROFIT API SERVICE INTERFACE
// ==========================================

interface LaravelApiService {
    @POST("api/register")
    suspend fun register(@Body request: LaravelRegisterRequest): User

    @POST("api/login")
    suspend fun login(@Body request: LaravelLoginRequest): User

    @POST("api/student/register")
    suspend fun registerStudent(@Body request: LaravelStudentRegisterRequest): LaravelSanctumAuthResponse

    @POST("api/student/login")
    suspend fun loginStudent(@Body request: LaravelLoginRequest): LaravelSanctumAuthResponse

    @POST("api/vendor/register")
    suspend fun registerVendor(@Body request: LaravelVendorRegisterRequest): LaravelSanctumAuthResponse

    @POST("api/vendor/login")
    suspend fun loginVendor(@Body request: LaravelLoginRequest): LaravelSanctumAuthResponse

    @GET("api/users")
    suspend fun getAllUsers(): List<User>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): LaravelGeneralResponse

    @GET("api/food-items")
    suspend fun getFoodItems(): List<LaravelFoodItem>

    @GET("api/food-items/vendor/{vendorId}")
    suspend fun getVendorFoodItems(@Path("vendorId") vendorId: Int): List<LaravelFoodItem>

    @POST("api/food-items")
    suspend fun createFoodItem(@Body request: LaravelAddFoodRequest): LaravelFoodItem

    @PUT("api/food-items/{id}")
    suspend fun updateFoodItem(@Path("id") id: Int, @Body request: LaravelUpdateFoodRequest): LaravelFoodItem

    @DELETE("api/food-items/{id}")
    suspend fun deleteFoodItem(@Path("id") id: Int): LaravelGeneralResponse

    @GET("api/orders")
    suspend fun getAllOrders(): List<LaravelOrder>

    @GET("api/orders/customer/{customerId}")
    suspend fun getCustomerOrders(@Path("customerId") customerId: Int): List<LaravelOrder>

    @GET("api/orders/vendor/{vendorId}")
    suspend fun getVendorOrders(@Path("vendorId") vendorId: Int): List<LaravelOrder>

    @POST("api/orders")
    suspend fun createOrder(@Body request: LaravelAddOrderRequest): LaravelOrder

    @POST("api/orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: Int): LaravelOrder

    @PUT("api/orders/{id}/status")
    suspend fun updateOrderStatus(@Path("id") id: Int, @Body request: LaravelUpdateOrderStatusRequest): LaravelOrder

    @POST("api/orders/{id}/verify-pickup")
    suspend fun verifyAndCompletePickup(@Path("id") id: Int, @Body request: LaravelVerifyPickupRequest): LaravelVerifyPickupResponse

    @GET("api/feedback")
    suspend fun getAllFeedback(): List<LaravelFeedback>

    @GET("api/feedback/vendor/{vendorId}")
    suspend fun getVendorFeedback(@Path("vendorId") vendorId: Int): List<LaravelFeedback>

    @POST("api/feedback")
    suspend fun createFeedback(@Body request: LaravelAddFeedbackRequest): LaravelFeedback

    @POST("api/feedback/{id}/reply")
    suspend fun replyToFeedback(@Path("id") feedbackId: Int, @Body request: LaravelFeedbackReplyRequest): LaravelFeedback

    @GET("api/food-items/feedback")
    suspend fun getAllFoodFeedback(): List<LaravelFoodItemFeedback>

    @GET("api/food-items/{foodItemId}/feedback")
    suspend fun getFoodItemFeedback(@Path("foodItemId") foodItemId: Int): List<LaravelFoodItemFeedback>

    @POST("api/food-items/feedback")
    suspend fun createFoodFeedback(@Body request: LaravelAddFoodFeedbackRequest): LaravelFoodItemFeedback

    @GET("api/audit-logs")
    suspend fun getAllAuditLogs(): List<LaravelAuditLog>

    @POST("api/audit-logs")
    suspend fun createAuditLog(@Body request: LaravelAddAuditLogRequest): LaravelAuditLog

    @POST("api/vendor/toggle-status")
    suspend fun toggleVendorStatus(@Query("is_open") isOpen: Boolean? = null): LaravelToggleStatusResponse

    @GET("api/vendor/performance")
    suspend fun getVendorPerformance(
        @Query("vendor_id") vendorId: Int? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): LaravelPerformanceResponse

    @GET("api/vendor/performance-metrics")
    suspend fun getVendorPerformanceMetrics(): LaravelVendorPerformanceMetricsResponse

    @GET("api/vendor/daily-revenue")
    suspend fun getDailyRevenue(
        @Query("vendor_id") vendorId: Int? = null
    ): LaravelDailyRevenueResponse

    @GET("api/vendor/analytics/gemini-order-insights")
    suspend fun getMyGeminiOrderInsights(): LaravelGeminiInsightResponse

    @GET("api/vendor/{vendorId}/gemini-order-insights")
    suspend fun getVendorGeminiOrderInsights(@Path("vendorId") vendorId: Int): LaravelGeminiInsightResponse

    @GET("api/notifications")
    suspend fun getDatabaseNotifications(): LaravelNotificationsResponse

    @POST("api/notifications/mark-read")
    suspend fun markAllNotificationsAsRead(): LaravelGeneralResponse

    // Chat API Methods
    @GET("api/chats/recent")
    suspend fun getRecentChats(): LaravelRecentChatsResponse

    @GET("api/chats/conversation/{otherUserId}")
    suspend fun getConversation(@Path("otherUserId") otherUserId: Int): LaravelConversationResponse

    @POST("api/chats/send")
    suspend fun sendChatMessage(@Body request: LaravelSendChatRequest): LaravelSendChatResponse

    // Paystack API Methods
    @POST("api/paystack/initialize")
    suspend fun initializePaystack(@Body request: LaravelPaystackInitRequest): LaravelPaystackInitResponse

    @GET("api/paystack/verify/{reference}")
    suspend fun verifyPaystack(@Path("reference") reference: String, @Query("amount") amount: Double, @Query("purpose") purpose: String): LaravelPaystackVerifyResponse

    @PUT("api/vendor/menu/availability")
    suspend fun updateMenuAvailability(@Body request: LaravelUpdateMenuAvailabilityRequest): LaravelUpdateMenuAvailabilityResponse

    @GET("api/vendor/orders/summary")
    suspend fun getOrderSummary(): LaravelOrderSummaryResponse

    @POST("api/vendor/menu/bulk-update")
    suspend fun bulkUpdateMenu(@Body request: List<LaravelBulkUpdateItem>): LaravelBulkUpdateResponse

    @POST("api/food-items/bulk-toggle")
    suspend fun bulkToggleFoodItems(@Body request: LaravelBulkToggleRequest): LaravelGeneralResponse

    @POST("api/user/profile")
    suspend fun updateProfile(@Body request: LaravelUpdateProfileRequest): LaravelGeneralResponse

    @GET("api/student/loyalty/summary")
    suspend fun getLoyaltySummary(): LaravelLoyaltySummaryResponse

    @POST("api/student/loyalty/preview-discount")
    suspend fun previewDiscount(@Body request: LaravelPreviewDiscountRequest): LaravelPreviewDiscountResponse
}

@JsonClass(generateAdapter = true)
data class LaravelLoyaltyHistoryItem(
    val order_id: Int,
    val food_name: String,
    val type: String,
    val points: Int,
    val discount_applied: Double,
    val description: String,
    val status: String,
    val date: String,
    val timestamp_ms: Long
)

@JsonClass(generateAdapter = true)
data class LaravelLoyaltySummaryResponse(
    val success: Boolean,
    val loyalty_points_balance: Int,
    val equivalent_cashback_value: Double,
    val tier: String,
    val next_tier: String,
    val points_needed_for_next_tier: Int,
    val total_spent_all_time: Double,
    val conversion_rule: String,
    val earning_rule: String,
    val history: List<LaravelLoyaltyHistoryItem>,
    val generated_at: String? = null
)

@JsonClass(generateAdapter = true)
data class LaravelPreviewDiscountRequest(
    val points_to_redeem: Int
)

@JsonClass(generateAdapter = true)
data class LaravelPreviewDiscountResponse(
    val success: Boolean,
    val points_to_redeem: Int,
    val discount_value: Double,
    val currency: String,
    val remaining_points: Int
)

@JsonClass(generateAdapter = true)
data class LaravelUpdateProfileRequest(
    val fullName: String? = null,
    val student_staff_id: String? = null,
    val telephone: String? = null,
    val phone_number: String? = null,
    val email: String? = null,
    val department: String? = null,
    val program_of_study: String? = null,
    val payment_methods: List<String>? = null,
    val info: String? = null
)

// ==========================================
// 3. RUNTIME DYNAMIC CLIENT MANAGER
// ==========================================

object LaravelClientManager {
    var isLaravelEnabled: Boolean = false
    var baseUrl: String = "http://10.0.2.2:8000/" // Default local emulator loopback IP
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            if (field != formatted) {
                field = formatted
                invalidateService()
            }
        }

    private var cachedService: LaravelApiService? = null

    var authToken: String? = null

    private fun invalidateService() {
        cachedService = null
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = authToken
            if (!token.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            val response: okhttp3.Response
            try {
                response = chain.proceed(requestBuilder.build())
            } catch (e: Exception) {
                // Network or connection timeout exception
                throw e
            }

            val serverToken = response.header("X-Auth-Token")
            if (!serverToken.isNullOrBlank()) {
                authToken = serverToken
            }

            if (!response.isSuccessful) {
                val statusCode = response.code
                val peekBody = try { response.peekBody(1024).string() } catch (_: Exception) { "" }
                android.util.Log.w("LaravelApiService", "HTTP $statusCode error: $peekBody")
            }

            response
        }
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    fun getService(): LaravelApiService {
        val current = cachedService
        if (current != null) return current

        val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LaravelApiService::class.java)

        cachedService = service
        return service
    }

    // Mapper helper utilities
    fun toRoomFoodItem(l: LaravelFoodItem): FoodItem {
        return FoodItem(
            id = l.id,
            vendorId = l.vendor_id,
            name = l.name,
            price = l.price,
            category = l.category,
            imageUrl = l.image_url ?: "",
            description = l.description ?: "",
            isAvailable = l.is_available,
            calories = l.calories ?: 180,
            allergens = l.allergens ?: "None"
        )
    }

    fun toRoomOrder(l: LaravelOrder): Order {
        return Order(
            id = l.id,
            customerId = l.customer_id,
            vendorId = l.vendor_id,
            foodItemId = l.food_item_id,
            foodName = l.food_name,
            quantity = l.quantity,
            unitPrice = l.unit_price,
            totalPrice = l.total_price,
            orderTimestamp = l.order_timestamp,
            status = l.status,
            pickupPin = l.pickup_pin,
            estimatedPickupTime = l.estimated_pickup_time,
            pointsRedeemed = l.points_redeemed ?: 0,
            discountApplied = l.discount_applied ?: 0.0
        )
    }

    fun toRoomFeedback(l: LaravelFeedback): Feedback {
        return Feedback(
            id = l.id,
            orderId = l.order_id,
            vendorId = l.vendor_id,
            customerId = l.customer_id,
            ratingFoodQuality = l.rating_food_quality,
            ratingCleanliness = l.rating_cleanliness,
            ratingServiceSpeed = l.rating_service_speed,
            ratingPriceValue = l.rating_price_value,
            comment = l.comment ?: "",
            vendorReply = l.vendor_reply,
            timestamp = l.timestamp
        )
    }

    fun toRoomFoodItemFeedback(l: LaravelFoodItemFeedback): FoodItemFeedback {
        return FoodItemFeedback(
            id = l.id,
            orderId = l.order_id,
            foodItemId = l.food_item_id,
            customerId = l.customer_id,
            rating = l.rating,
            comment = l.comment ?: "",
            timestamp = l.timestamp
        )
    }

    fun toRoomAuditLog(l: LaravelAuditLog): AuditLog {
        return AuditLog(
            id = l.id,
            timestamp = l.timestamp,
            userId = l.user_id,
            action = l.action,
            details = l.details
        )
    }
}
