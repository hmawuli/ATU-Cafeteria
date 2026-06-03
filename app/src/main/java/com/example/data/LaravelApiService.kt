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
    val is_available: Boolean
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
    val estimated_pickup_time: String
)

@JsonClass(generateAdapter = true)
data class LaravelAddOrderRequest(
    val customer_id: Int,
    val vendor_id: Int,
    val food_item_id: Int,
    val food_name: String,
    val quantity: Int,
    val unit_price: Double,
    val total_price: Double
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

// ==========================================
// 2. RETROFIT API SERVICE INTERFACE
// ==========================================

interface LaravelApiService {
    @POST("api/register")
    suspend fun register(@Body request: LaravelRegisterRequest): User

    @POST("api/login")
    suspend fun login(@Body request: LaravelLoginRequest): User

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
}

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
            val response = chain.proceed(requestBuilder.build())
            val serverToken = response.header("X-Auth-Token")
            if (!serverToken.isNullOrBlank()) {
                authToken = serverToken
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
            isAvailable = l.is_available
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
            estimatedPickupTime = l.estimated_pickup_time
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
