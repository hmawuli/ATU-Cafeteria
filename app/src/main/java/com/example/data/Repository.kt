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

    // Flow Accessors
    val allVendors: Flow<List<User>> = userDao.getAllVendors()
    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allFoodItems: Flow<List<FoodItem>> = foodItemDao.getAllFoodItems()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()
    val allFeedback: Flow<List<Feedback>> = feedbackDao.getAllFeedback()
    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getAllLogs()

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

    suspend fun updateUser(user: User) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Int) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(userId)
    }

    // Menu Management
    suspend fun addMenuFoodItem(vendorId: Int, name: String, price: Double, category: String, description: String, imageUrl: String) = withContext(Dispatchers.IO) {
        val item = FoodItem(
            vendorId = vendorId,
            name = name,
            price = price,
            category = category,
            description = description,
            imageUrl = imageUrl,
            isAvailable = true
        )
        foodItemDao.insertFoodItem(item)
        insertAuditLog(vendorId, "MENU_ITEM_CREATED", "Added menu item: ${name} to category ${category}")
    }

    suspend fun updateMenuFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        foodItemDao.updateFoodItem(item)
        insertAuditLog(item.vendorId, "MENU_ITEM_UPDATED", "Updated item details for '${item.name}'")
    }

    suspend fun deleteMenuFoodItem(item: FoodItem) = withContext(Dispatchers.IO) {
        foodItemDao.deleteFoodItem(item)
        insertAuditLog(item.vendorId, "MENU_ITEM_DELETED", "Deleted item: '${item.name}' from vendor menu.")
    }

    // Transactions
    suspend fun placeOrder(customerId: Int, foodItem: FoodItem, quantity: Int): Order = withContext(Dispatchers.IO) {
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
        return@withContext order.copy(id = orderId.toInt())
    }

    suspend fun updateOrderStatus(vendorId: Int, orderId: Int, newStatus: String, estimatedTime: String? = null) = withContext(Dispatchers.IO) {
        val o = orderDao.getOrderById(orderId) ?: return@withContext
        val updated = o.copy(
            status = newStatus,
            estimatedPickupTime = estimatedTime ?: o.estimatedPickupTime
        )
        orderDao.updateOrder(updated)
        insertAuditLog(vendorId, "ORDER_STATUS_CHANGED", "Order #${orderId} transitioned to: ${newStatus} (${estimatedTime ?: "No change to estimate"})")
    }

    suspend fun verifyAndCompletePickup(vendorId: Int, orderId: Int, pin: String): Boolean = withContext(Dispatchers.IO) {
        val o = orderDao.getOrderById(orderId) ?: return@withContext false
        if (o.vendorId == vendorId && o.pickupPin == pin) {
            val updated = o.copy(status = "COMPLETED")
            orderDao.updateOrder(updated)
            insertAuditLog(vendorId, "PICKUP_VALIDATED", "Order #${orderId} secure pin verified successfully and marked Completed.")
            return@withContext true
        } else {
            insertAuditLog(vendorId, "PICKUP_FAIL", "Order #${orderId} verification failed with bad PIN: '$pin'")
            return@withContext false
        }
    }

    // Feedback
    suspend fun submitFeedback(customerId: Int, orderId: Int, vendorId: Int, quality: Int, cleanliness: Int, speed: Int, value: Int, comment: String) = withContext(Dispatchers.IO) {
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

    // Logger
    suspend fun insertAuditLog(userId: Int, action: String, details: String) = withContext(Dispatchers.IO) {
        auditLogDao.insertLog(AuditLog(userId = userId, action = action, details = details))
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
            val stud1 = User(id = 1, username = "student", passwordHash = sha256("1234"), role = "STUDENT", fullName = "Daniel Mensah", info = "ATU-2024-D45")
            val stud2 = User(id = 2, username = "student2", passwordHash = sha256("1234"), role = "STUDENT", fullName = "Abena Osei", info = "ATU-2025-S12")
            userDao.insertUser(stud1)
            userDao.insertUser(stud2)

            // 2. Vendors
            val v1 = User(id = 10, username = "maryjoint", passwordHash = sha256("1111"), role = "VENDOR", fullName = "Mary Joint", info = "Auntie Mary Special")
            val v2 = User(id = 11, username = "atkitch", passwordHash = sha256("2222"), role = "VENDOR", fullName = "Kofi Local Kitchen", info = "ATU Local Hub")
            val v3 = User(id = 12, username = "snackbag", passwordHash = sha256("3333"), role = "VENDOR", fullName = "Bakery & Treats", info = "ATU Snack Corner")
            userDao.insertUser(v1)
            userDao.insertUser(v2)
            userDao.insertUser(v3)

            // 3. Manager/Admin
            val admin = User(id = 99, username = "admin", passwordHash = sha256("admin123"), role = "ADMIN", fullName = "Dr. Emmanuel Kaku", info = "ATU Quality Assurance")
            userDao.insertUser(admin)

            // Seed FoodItems
            val foods = listOf(
                // Mary Joint
                FoodItem(id = 101, vendorId = v1.id, name = "ATU Chicken Jollof Rice", price = 25.0, category = "Lunch Specials", imageUrl = "", description = "Classic aromatic rice stewed with authentic Ghanaian tomato sauce, served with seasoned fried chicken salad & shito."),
                FoodItem(id = 102, vendorId = v1.id, name = "Zesty Ginger Sobolo", price = 10.0, category = "Drinks", imageUrl = "", description = "Refreshing chilled local hibiscus flower drink brewed with fresh ginger, pineapple peels, and sweetener."),
                FoodItem(id = 103, vendorId = v1.id, name = "Red-Red Beans Stew", price = 20.0, category = "Lunch Specials", imageUrl = "", description = "Stewed tender cowpea bean hash in palm palm oil, accompanied by fried ripe sugar-plantain dices."),

                // Kofi Kitchen
                FoodItem(id = 201, vendorId = v2.id, name = "Waakye Supreme", price = 30.0, category = "Traditional", imageUrl = "", description = "A student favorite! Local black-eyed peas boiled with rice and millet stalks. Accompanying boiled egg, spiced gari, talia, and hot wele shito."),
                FoodItem(id = 202, vendorId = v2.id, name = "Fufu & Goat Light Soup", price = 35.0, category = "Traditional", imageUrl = "", description = "Rich Ghanaian fufu pounded from fresh cassava and green plantains, submerged in aromatic goat meat soup."),

                // Bakery Corner
                FoodItem(id = 301, vendorId = v3.id, name = "Savoury Meat Pie", price = 15.0, category = "Snacks", imageUrl = "", description = "Crispy, flaky puff pastry loaded with moist, cooked mince beef seasoning."),
                FoodItem(id = 302, vendorId = v3.id, name = "Chilled Coca-Cola", price = 8.0, category = "Drinks", imageUrl = "", description = "330ml Ice-cold Coca-Cola can for dynamic pairing.")
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

            // Seed logs
            insertAuditLog(99, "SYSTEM_INIT", "Database seed initialised with traditional Accra Technical University menus.")
        } catch (e: Exception) {
            Log.e("CafeteriaRepository", "Warning: Pre-seeding encountered an exception. Skipping seed safely.", e)
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
}
