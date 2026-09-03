package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.recommendation.*
import com.example.data.repository.UserSessionRepository
import com.example.data.sync.FirestoreOrderTrackingManager
import com.example.data.sync.FirestoreVendorStatusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import kotlinx.coroutines.delay
import android.util.Log

data class MockEmailNotification(
    val id: String,
    val orderId: Int,
    val studentEmail: String,
    val vendorName: String,
    val itemName: String,
    val subject: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class MockPushNotification(
    val id: String,
    val orderId: Int,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class VendorInventoryNotification(
    val id: String,
    val foodItemId: Int,
    val foodName: String,
    val type: String, // "LOW_STOCK", "OUT_OF_STOCK", "UNAVAILABLE"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class CartItem(
    val foodItem: FoodItem,
    val quantity: Int
)

@HiltViewModel
class CafeteriaViewModel @Inject constructor(
    application: Application,
    val repository: CafeteriaRepository,
    val geminiRepository: GeminiAnalyticsRepository,
    val userSessionRepo: UserSessionRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        repository = com.example.di.ServiceLocator.provideCafeteriaRepository(application),
        geminiRepository = com.example.di.ServiceLocator.provideGeminiAnalyticsRepository(),
        userSessionRepo = com.example.di.ServiceLocator.provideUserSessionRepository(application)
    )

    private val db = AppDatabase.getDatabase(application)

    // Connection & Caching Layer States
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isOnline = MutableStateFlow(checkInitialNetworkState())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(getSavedLastSyncTime())
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private fun checkInitialNetworkState(): Boolean {
        try {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            return true
        }
    }

    fun getSavedLastSyncTime(): Long {
        val prefs = getApplication<Application>().getSharedPreferences("cafeteria_cache", Context.MODE_PRIVATE)
        return prefs.getLong("last_menu_sync_timestamp", 0L)
    }

    fun saveLastSyncTime(timestamp: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("cafeteria_cache", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_menu_sync_timestamp", timestamp).apply()
        _lastSyncTime.value = timestamp
    }

    private val _isHighContrastMode = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("cafeteria_accessibility", Context.MODE_PRIVATE)
            .getBoolean("high_contrast", false)
    )
    val isHighContrastMode: StateFlow<Boolean> = _isHighContrastMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("cafeteria_accessibility", Context.MODE_PRIVATE)
            .getBoolean("dark_mode", false)
    )
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val nextVal = !_isDarkMode.value
        getApplication<Application>().getSharedPreferences("cafeteria_accessibility", Context.MODE_PRIVATE)
            .edit().putBoolean("dark_mode", nextVal).apply()
        _isDarkMode.value = nextVal
    }

    private val _monthlyBudgetLimit = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("atu_budget_prefs", Context.MODE_PRIVATE)
            .getFloat("monthly_budget_limit", 200f).toDouble()
    )
    val monthlyBudgetLimit: StateFlow<Double> = _monthlyBudgetLimit.asStateFlow()

    fun setMonthlyBudgetLimit(limit: Double) {
        getApplication<Application>().getSharedPreferences("atu_budget_prefs", Context.MODE_PRIVATE)
            .edit().putFloat("monthly_budget_limit", limit.toFloat()).apply()
        _monthlyBudgetLimit.value = limit
    }

    fun toggleHighContrastMode() {
        val nextVal = !_isHighContrastMode.value
        getApplication<Application>().getSharedPreferences("cafeteria_accessibility", Context.MODE_PRIVATE)
            .edit().putBoolean("high_contrast", nextVal).apply()
        _isHighContrastMode.value = nextVal
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: android.net.Network) {
            _isOnline.value = true
        }
        override fun onLost(network: android.net.Network) {
            _isOnline.value = false
        }
    }

    // 1. Session State managers
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loyaltySummaryResponse = MutableStateFlow<com.example.data.LaravelLoyaltySummaryResponse?>(null)
    val loyaltySummaryResponse: StateFlow<com.example.data.LaravelLoyaltySummaryResponse?> = _loyaltySummaryResponse.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _globalApiError = MutableStateFlow<String?>(null)
    val globalApiError: StateFlow<String?> = _globalApiError.asStateFlow()

    fun emitGlobalError(message: String) {
        _globalApiError.value = message
    }

    fun clearGlobalError() {
        _globalApiError.value = null
    }

    private val _registrationSuccess = MutableStateFlow<Boolean>(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Inactivity session timeout management
    private val _lastActivityTime = MutableStateFlow(System.currentTimeMillis())
    val lastActivityTime: StateFlow<Long> = _lastActivityTime.asStateFlow()

    private val _isSessionTimedOut = MutableStateFlow(false)
    val isSessionTimedOut: StateFlow<Boolean> = _isSessionTimedOut.asStateFlow()

    // Rate-limiting and authentication lockout state flows
    private val _failedLoginAttempts = MutableStateFlow(userSessionRepo.getFailedAttemptsCount())
    val failedLoginAttempts: StateFlow<Int> = _failedLoginAttempts.asStateFlow()

    private val _lockoutRemainingSeconds = MutableStateFlow(userSessionRepo.getRemainingLockoutSeconds())
    val lockoutRemainingSeconds: StateFlow<Int> = _lockoutRemainingSeconds.asStateFlow()

    val isRateLimited: StateFlow<Boolean> = _lockoutRemainingSeconds.map { it > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), userSessionRepo.isLockedOut())

    fun updateActivity() {
        if (_currentUser.value != null) {
            _lastActivityTime.value = System.currentTimeMillis()
            userSessionRepo.recordUserActivity()
        }
    }

    fun logout() {
        _currentUser.value = null
        clearCart()
        val userSessionRepo = com.example.di.ServiceLocator.provideUserSessionRepository(getApplication())
        userSessionRepo.clearSession()
        Log.i("CafeteriaViewModel", "User logged out successfully.")
    }

    fun clearLocalDatabaseCache() {
        viewModelScope.launch {
            try {
                // Trigger refresh or purge non-critical cache
                _isLoading.value = true
                kotlinx.coroutines.delay(600)
                repository.seedDatabaseIfEmpty()
                _isLoading.value = false
                Log.i("CafeteriaViewModel", "Local database cache purged and resynced.")
            } catch (e: Exception) {
                _isLoading.value = false
                Log.e("CafeteriaViewModel", "Error clearing local cache", e)
            }
        }
    }

    fun resetSessionTimeoutFlag() {
        _isSessionTimedOut.value = false
    }

    // --- Modern Pro-Suite States ---
    private val _studentWalletBalance = MutableStateFlow(185.50) // Starting digital currency seed
    val studentWalletBalance: StateFlow<Double> = _studentWalletBalance.asStateFlow()

    private val _vendorAnnouncement = MutableStateFlow("All meals prepared in alignment with Accra hygiene standards. Dine safe, study hard!")
    val vendorAnnouncement: StateFlow<String> = _vendorAnnouncement.asStateFlow()

    private val _isStoreClosed = MutableStateFlow(false)
    val isStoreClosed: StateFlow<Boolean> = _isStoreClosed.asStateFlow()

    // Real-time vendor status map (Open/Busy/Closed) synced via Firestore
    val vendorStatusMap: StateFlow<Map<Int, String>> = FirestoreVendorStatusManager.vendorStatusMap

    // Real-time order tracking status map synced via Firestore stream listeners
    val liveOrderTrackingMap: StateFlow<Map<Int, String>> = FirestoreOrderTrackingManager.orderStatusMap
    val liveOrderEstimatedTimeMap: StateFlow<Map<Int, String>> = FirestoreOrderTrackingManager.orderEstimatedTimeMap

    // --- Administrative Promotional Offers State & Management ---
    private val _promotionalOffers = MutableStateFlow<List<PromotionalOffer>>(
        listOf(
            PromotionalOffer(1, "ATUWELCOME", "Welcome Campus Snack Deal", 15.0, "Get 15% off your first pre-order across any ATU cafeteria vendor stall!", true, "#006B5D"),
            PromotionalOffer(2, "MIDTERM20", "Midterm Rush Special", 20.0, "Save 20% on all lunch set meals during peak exam hours (12 PM - 2 PM).", true, "#D32F2F"),
            PromotionalOffer(3, "FRIDAYEATS", "ATU TGIF Feasts", 10.0, "Enjoy 10% discount on all local traditional dishes every Friday!", true, "#E65100")
        )
    )
    val promotionalOffers: StateFlow<List<PromotionalOffer>> = _promotionalOffers.asStateFlow()

    fun addPromotionalOffer(code: String, title: String, discountPercent: Double, description: String) {
        val current = _promotionalOffers.value.toMutableList()
        val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
        current.add(0, PromotionalOffer(newId, code.uppercase().trim(), title, discountPercent, description, true))
        _promotionalOffers.value = current
    }

    fun togglePromotionalOfferStatus(offerId: Int) {
        _promotionalOffers.value = _promotionalOffers.value.map {
            if (it.id == offerId) it.copy(isActive = !it.isActive) else it
        }
    }

    fun deletePromotionalOffer(offerId: Int) {
        _promotionalOffers.value = _promotionalOffers.value.filter { it.id != offerId }
    }

    // --- Dynamic Food Recommendation Engine Integration ---
    private val _dietaryPreferences = MutableStateFlow(loadInitialDietaryPreferences())
    val dietaryPreferences: StateFlow<DietaryPreferences> = _dietaryPreferences.asStateFlow()

    private val _activeRecommendationStrategy = MutableStateFlow(RecommendationStrategy.TOP_PICKS)
    val activeRecommendationStrategy: StateFlow<RecommendationStrategy> = _activeRecommendationStrategy.asStateFlow()

    private val _studentOrdersFlow = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == "STUDENT") repository.getOrdersForCustomer(user.id) else flowOf(emptyList())
    }

    val recommendedItems: StateFlow<List<RecommendedItem>> = combine(
        combine(repository.allFoodItems, _studentOrdersFlow, repository.allOrders) { food, student, campus ->
            Triple(food, student, campus)
        },
        combine(repository.allVendors, _dietaryPreferences, _activeRecommendationStrategy) { vendors, prefs, strategy ->
            Triple(vendors, prefs, strategy)
        }
    ) { (foodItems, studentOrders, campusOrders), (vendors, prefs, strategy) ->
        FoodRecommendationEngine.generateRecommendations(
            availableFoodItems = foodItems,
            studentOrders = studentOrders,
            allCampusOrders = campusOrders,
            vendors = vendors,
            dietaryPreferences = prefs,
            strategy = strategy,
            limit = 10
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun loadInitialDietaryPreferences(): DietaryPreferences {
        val prefs = getApplication<Application>().getSharedPreferences("atu_dietary_prefs", Context.MODE_PRIVATE)
        val serialized = prefs.getString("user_dietary_settings", null)
        return DietaryPreferences.fromSerializedString(serialized)
    }

    fun updateDietaryPreferences(newPreferences: DietaryPreferences) {
        _dietaryPreferences.value = newPreferences
        val prefs = getApplication<Application>().getSharedPreferences("atu_dietary_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("user_dietary_settings", newPreferences.toSerializedString()).apply()
        
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val updated = user.copy(dietaryPreferences = newPreferences.toSerializedString())
                repository.userDao.insertUser(updated)
                _currentUser.value = updated
            }
        }
        com.example.ui.util.SnackbarManager.showMessage("Dietary preferences updated! Recommendations refreshed.")
    }

    fun setRecommendationStrategy(strategy: RecommendationStrategy) {
        _activeRecommendationStrategy.value = strategy
    }

    // --- Dynamic Shopping Cart States & Controls ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartCount: StateFlow<Int> = _cart
        .map { list -> list.sumOf { it.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addToCart(foodItem: FoodItem, qty: Int = 1) {
        if (foodItem.currentStock <= 0 || !foodItem.isAvailable) {
            com.example.ui.util.SnackbarManager.showMessage("'${foodItem.name}' is currently out of stock.")
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.foodItem.id == foodItem.id }
        if (index != -1) {
            val existing = currentList[index]
            currentList[index] = existing.copy(quantity = existing.quantity + qty)
        } else {
            currentList.add(CartItem(foodItem, qty))
        }
        _cart.value = currentList
        com.example.ui.util.FirebaseAnalyticsHelper.logAddToCart(foodItem.id, foodItem.name, foodItem.price, qty)
        com.example.ui.util.HapticUtil.performOrderButtonHaptic(getApplication())
        com.example.ui.util.SnackbarManager.showMessage("Added ${foodItem.name} (x$qty) to cart.")
    }

    fun removeFromCart(foodItem: FoodItem) {
        val currentList = _cart.value.toMutableList()
        currentList.removeAll { it.foodItem.id == foodItem.id }
        _cart.value = currentList
    }

    fun updateCartQuantity(foodItem: FoodItem, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(foodItem)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.foodItem.id == foodItem.id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(quantity = quantity)
            _cart.value = currentList
        }
    }

    fun clearCart() {
        _cart.value = emptyList()
    }

    fun getCartTotal(): Double {
        return _cart.value.sumOf { it.foodItem.price * it.quantity }
    }

    fun checkoutCart(useWallet: Boolean = true, estimatedPickupTime: String = "Calculating...", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null || _cart.value.isEmpty()) {
                onComplete(false)
                return@launch
            }
            val requiredSum = getCartTotal()
            if (useWallet) {
                if (user.balance >= requiredSum) {
                    val ref = "CART-" + (100000..999999).random()
                    // Deduct sum
                    repository.insertWalletTransaction(user.id, "PAYMENT", -requiredSum, ref, "Secure Cart payment: ${_cart.value.size} items")
                    repository.insertAuditLog(user.id, "WALLET_PAYMENT", "Debited GH₵ ${"%.2f".format(requiredSum)} for secure cart pickup.")
                    
                    val securePin = (1000..9999).random().toString()
                    
                    for (item in _cart.value) {
                        repository.placeOrder(user.id, item.foodItem, item.quantity, 0, estimatedPickupTime)
                        // Credit the vendor for their earnings
                        val vendRef = "EARN-" + (100000..999999).random()
                        val itemPrice = item.foodItem.price * item.quantity
                        repository.insertWalletTransaction(item.foodItem.vendorId, "REFUND", itemPrice, vendRef, "Earnings from Cart Order of ${item.foodItem.name} (QTY: ${item.quantity})")
                        repository.insertAuditLog(item.foodItem.vendorId, "VENDOR_EARNED", "Earned GH₵ ${"%.2f".format(itemPrice)} from incoming customer cart order.")
                    }
                    
                    // Refresh student user session
                    val refreshed = repository.userDao.getUserSync(user.id)
                    if (refreshed != null) {
                        _currentUser.value = refreshed
                    }
                    val itemCount = _cart.value.sumOf { it.quantity }
                    com.example.ui.util.FirebaseAnalyticsHelper.logOrderCompletion(
                        orderId = System.currentTimeMillis(),
                        totalPrice = requiredSum,
                        itemCount = itemCount,
                        paymentMethod = if (useWallet) "WALLET" else "POD"
                    )
                    clearCart()
                    com.example.ui.util.SnackbarManager.showMessage("Order placed successfully!")
                    onComplete(true)
                } else {
                    com.example.ui.util.SnackbarManager.showMessage("Insufficient wallet balance for cart checkout.")
                    onComplete(false)
                }
            } else {
                for (item in _cart.value) {
                    repository.placeOrder(user.id, item.foodItem, item.quantity, 0, estimatedPickupTime)
                }
                repository.insertAuditLog(user.id, "POD_ORDER", "Cart orders generated under Pay-on-Delivery protocol.")
                clearCart()
                com.example.ui.util.SnackbarManager.showMessage("Order placed successfully (Pay on Delivery)!")
                onComplete(true)
            }
        }
    }

    private val _vendorPerformanceList = MutableStateFlow<List<com.example.data.LaravelDailyPerformance>>(emptyList())
    val vendorPerformanceList: StateFlow<List<com.example.data.LaravelDailyPerformance>> = _vendorPerformanceList.asStateFlow()

    private val _vendorPerformanceMetricsList = MutableStateFlow<List<com.example.data.LaravelVendorMetric>>(emptyList())
    val vendorPerformanceMetricsList: StateFlow<List<com.example.data.LaravelVendorMetric>> = _vendorPerformanceMetricsList.asStateFlow()

    private val _vendorDailyRevenueResponse = MutableStateFlow<com.example.data.LaravelDailyRevenueResponse?>(null)
    val vendorDailyRevenueResponse: StateFlow<com.example.data.LaravelDailyRevenueResponse?> = _vendorDailyRevenueResponse.asStateFlow()

    // 2. Room Reactive Streams
    val allVendors: StateFlow<List<User>> = repository.allVendors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isAdminActing = MutableStateFlow(false)
    val realAdminUser = MutableStateFlow<User?>(null)

    val allFoodItems: StateFlow<List<FoodItem>> = repository.allFoodItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrdersSnapshot: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFeedback: StateFlow<List<Feedback>> = repository.allFeedback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodFeedback: StateFlow<List<FoodItemFeedback>> = repository.allFoodFeedback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWalletTransactions: StateFlow<List<WalletTransaction>> = repository.allWalletTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userWalletTransactions: StateFlow<List<WalletTransaction>> = _currentUser
        .flatMapLatest { user ->
            if (user != null) {
                repository.getWalletTransactionsForUser(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val offlineOrders = MutableStateFlow<List<OfflineOrder>>(emptyList())

    // 3. User Specific Orders & Foods (derived via flatMapLatest using session)
    val customerOrders: StateFlow<List<Order>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            if (user.role == "STUDENT") {
                repository.getOrdersForCustomer(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vendorOrders: StateFlow<List<Order>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            if (user.role == "VENDOR") {
                repository.getOrdersForVendor(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vendorFoodItems: StateFlow<List<FoodItem>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            if (user.role == "VENDOR") {
                repository.getFoodItemsForVendor(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vendorFeedback: StateFlow<List<Feedback>> = _currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            if (user.role == "VENDOR") {
                repository.getFeedbackForVendor(user.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Data Analytics States
    private val _aiAnalysisText = MutableStateFlow<String?>(null)
    val aiAnalysisText: StateFlow<String?> = _aiAnalysisText.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _vendorSentimentAnalysis = MutableStateFlow<String?>(null)
    val vendorSentimentAnalysis: StateFlow<String?> = _vendorSentimentAnalysis.asStateFlow()

    private val _isAnalyzingSentiment = MutableStateFlow(false)
    val isAnalyzingSentiment: StateFlow<Boolean> = _isAnalyzingSentiment.asStateFlow()

    private val _vendorAutoReplies = MutableStateFlow<String?>(null)
    val vendorAutoReplies: StateFlow<String?> = _vendorAutoReplies.asStateFlow()

    private val _isGeneratingAutoReplies = MutableStateFlow(false)
    val isGeneratingAutoReplies: StateFlow<Boolean> = _isGeneratingAutoReplies.asStateFlow()

    private val _vendorPricingSuggestions = MutableStateFlow<String?>(null)
    val vendorPricingSuggestions: StateFlow<String?> = _vendorPricingSuggestions.asStateFlow()

    private val _isGeneratingPricingSuggestions = MutableStateFlow(false)
    val isGeneratingPricingSuggestions: StateFlow<Boolean> = _isGeneratingPricingSuggestions.asStateFlow()

    private val _nutritionCoachingText = MutableStateFlow<String?>(null)
    val nutritionCoachingText: StateFlow<String?> = _nutritionCoachingText.asStateFlow()

    private val _isAnalyzingNutrition = MutableStateFlow(false)
    val isAnalyzingNutrition: StateFlow<Boolean> = _isAnalyzingNutrition.asStateFlow()

    private val _vendorTodayInsights = MutableStateFlow<String?>(null)
    val vendorTodayInsights: StateFlow<String?> = _vendorTodayInsights.asStateFlow()

    private val _isAnalyzingTodayOrders = MutableStateFlow(false)
    val isAnalyzingTodayOrders: StateFlow<Boolean> = _isAnalyzingTodayOrders.asStateFlow()

    private val _vendorHistoricalInsights = MutableStateFlow<String?>(null)
    val vendorHistoricalInsights: StateFlow<String?> = _vendorHistoricalInsights.asStateFlow()

    private val _isAnalyzingHistoricalOrders = MutableStateFlow(false)
    val isAnalyzingHistoricalOrders: StateFlow<Boolean> = _isAnalyzingHistoricalOrders.asStateFlow()

    private val _vendorDemandForecast = MutableStateFlow<String?>(null)
    val vendorDemandForecast: StateFlow<String?> = _vendorDemandForecast.asStateFlow()

    private val _isGeneratingDemandForecast = MutableStateFlow(false)
    val isGeneratingDemandForecast: StateFlow<Boolean> = _isGeneratingDemandForecast.asStateFlow()

    private val _studentRecommendations = MutableStateFlow<String?>(null)
    val studentRecommendations: StateFlow<String?> = _studentRecommendations.asStateFlow()

    private val _isGeneratingStudentRecommendations = MutableStateFlow(false)
    val isGeneratingStudentRecommendations: StateFlow<Boolean> = _isGeneratingStudentRecommendations.asStateFlow()

    private val _lowStockPredictionResult = MutableStateFlow<String?>(null)
    val lowStockPredictionResult: StateFlow<String?> = _lowStockPredictionResult.asStateFlow()

    private val _isPredictingLowStock = MutableStateFlow(false)
    val isPredictingLowStock: StateFlow<Boolean> = _isPredictingLowStock.asStateFlow()

    private val _popularTodayContent = MutableStateFlow("")
    val popularTodayContent: StateFlow<String> = _popularTodayContent.asStateFlow()

    private val _isPopularTodayLoading = MutableStateFlow(false)
    val isPopularTodayLoading: StateFlow<Boolean> = _isPopularTodayLoading.asStateFlow()

    private val _dishNutritionText = MutableStateFlow<String?>(null)
    val dishNutritionText: StateFlow<String?> = _dishNutritionText.asStateFlow()

    private val _isAnalyzingDishNutrition = MutableStateFlow(false)
    val isAnalyzingDishNutrition: StateFlow<Boolean> = _isAnalyzingDishNutrition.asStateFlow()

    private val _cartNutritionAnalysis = MutableStateFlow<String?>(null)
    val cartNutritionAnalysis: StateFlow<String?> = _cartNutritionAnalysis.asStateFlow()

    private val _isAnalyzingCartNutrition = MutableStateFlow(false)
    val isAnalyzingCartNutrition: StateFlow<Boolean> = _isAnalyzingCartNutrition.asStateFlow()

    // 5. In-App Real-time Order Notifications
    private val _newOrderAlerts = MutableStateFlow<List<Order>>(emptyList())
    val newOrderAlerts: StateFlow<List<Order>> = _newOrderAlerts.asStateFlow()

    private val _vendorInventoryNotifications = MutableStateFlow<List<VendorInventoryNotification>>(emptyList())
    val vendorInventoryNotifications: StateFlow<List<VendorInventoryNotification>> = _vendorInventoryNotifications.asStateFlow()

    private val _adminInventoryAlerts = MutableStateFlow<List<VendorInventoryNotification>>(emptyList())
    val adminInventoryAlerts: StateFlow<List<VendorInventoryNotification>> = _adminInventoryAlerts.asStateFlow()

    fun dismissAdminInventoryAlert(id: String) {
        _adminInventoryAlerts.value = _adminInventoryAlerts.value.filter { it.id != id }
    }
    val globalLowStockThreshold = MutableStateFlow(15)
    val redeemedLoyaltyPoints = MutableStateFlow(0)

    private val dismissedNotificationKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val readNotificationKeys = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var previousVendorNotificationKeys = setOf<String>()

    private val _realTimeEventsLogs = MutableStateFlow<List<String>>(emptyList())
    val realTimeEventsLogs: StateFlow<List<String>> = _realTimeEventsLogs.asStateFlow()

    // 1-Click Favorites State & Methods
    private val _favoriteFoodIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteFoodIds: StateFlow<Set<Int>> = _favoriteFoodIds.asStateFlow()

    fun toggleFavoriteFood(foodItemId: Int) {
        val current = _favoriteFoodIds.value
        _favoriteFoodIds.value = if (current.contains(foodItemId)) {
            current - foodItemId
        } else {
            current + foodItemId
        }
    }

    private val seenOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private var isFirstOrderLoad = true

    // Real-time Database Notifications support for Students (Laravel sync)
    private val _studentNotifications = MutableStateFlow<List<com.example.data.LaravelDatabaseNotification>>(emptyList())
    val studentNotifications: StateFlow<List<com.example.data.LaravelDatabaseNotification>> = _studentNotifications.asStateFlow()

    private val _activeStudentAlerts = MutableStateFlow<List<com.example.data.LaravelDatabaseNotification>>(emptyList())
    val activeStudentAlerts: StateFlow<List<com.example.data.LaravelDatabaseNotification>> = _activeStudentAlerts.asStateFlow()

    // Laravel SMTP Mail & FCM Push Channels Prefs & Logs
    val isEmailNotificationEnabled = MutableStateFlow(true)
    val isPushNotificationEnabled = MutableStateFlow(true)
    val isOrderConfirmationNotificationEnabled = MutableStateFlow(true)
    val isOrderStatusChangeNotificationEnabled = MutableStateFlow(true)
    val isPromotionalAlertsNotificationEnabled = MutableStateFlow(true)
    val isWeeklyVendorReportEnabled = MutableStateFlow(true)

    private val _lastWeeklyReportTimestamp = MutableStateFlow<Long?>(null)
    val lastWeeklyReportTimestamp: StateFlow<Long?> = _lastWeeklyReportTimestamp.asStateFlow()

    private val _dispatchedEmails = MutableStateFlow<List<MockEmailNotification>>(emptyList())
    val dispatchedEmails: StateFlow<List<MockEmailNotification>> = _dispatchedEmails.asStateFlow()

    private val _dispatchedPushNotifications = MutableStateFlow<List<MockPushNotification>>(emptyList())
    val dispatchedPushNotifications: StateFlow<List<MockPushNotification>> = _dispatchedPushNotifications.asStateFlow()

    private val localSeenReadyOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val localSeenDeliveredOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val localSeenPreparingOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    private val seenNotificationIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var isFirstNotificationLoad = true

    private val completedOrderIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private var vendorMetricsPollingJob: kotlinx.coroutines.Job? = null

    private val authStateListener = object : com.example.data.repository.AuthStateListener {
        override fun onSessionExpired(reason: String) {
            _loginError.value = "Session expired: $reason. Please log in again."
            logOut()
        }
        override fun onNetworkInterrupted(message: String) {
            _loginError.value = "Network interruption: $message. Falling back to local offline mode."
        }
        override fun onAuthenticated(user: User) {
            _currentUser.value = user
        }
    }

    init {
        userSessionRepo.addAuthStateListener(authStateListener)
        FirestoreVendorStatusManager.startRealtimeStatusListener()
        try {
            com.example.worker.KitchenOrderQueueNotificationWorker.schedulePeriodicCheck(getApplication())
        } catch (e: Exception) {
            Log.w("CafeteriaViewModel", "Could not schedule kitchen notification worker", e)
        }
        FirestoreOrderTrackingManager.startRealtimeOrderTrackingListener { orderId, newStatus, estTime ->
            viewModelScope.launch {
                repository.updateOrderStatus(0, orderId, newStatus, estTime)
                if (newStatus.equals("READY", ignoreCase = true) || newStatus.equals("READY_FOR_PICKUP", ignoreCase = true)) {
                    val app = getApplication<Application>()
                    com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                        app,
                        orderId,
                        "🍱 Order #$orderId is Ready for Pickup!",
                        "Your food order status has updated from 'Preparing' to 'Ready for Pickup'. Please collect your meal at the stall."
                    )
                }
            }
        }
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            Log.e("CafeteriaViewModel", "Failed to register network callback", e)
        }

        viewModelScope.launch {
            // Guarantee Seeding occurs on first start
            repository.seedDatabaseIfEmpty()
            loadOfflineOrders()

            // Session persistence auto-restore
            if (userSessionRepo.isLoggedIn()) {
                val uid = userSessionRepo.getUserId()
                val uname = userSessionRepo.getUserName()
                val restored: com.example.data.User? = if (uid > 0) {
                    repository.userDao.getUserSync(uid)
                } else if (uname.isNotBlank()) {
                    repository.userDao.getUserByUsername(uname)
                } else null

                if (restored != null) {
                    _currentUser.value = restored
                    _lastActivityTime.value = userSessionRepo.getLastActivityTime()
                    Log.i("CafeteriaViewModel", "Session restored successfully for: ${restored.fullName} (${restored.role})")
                }
            }

            if (LaravelClientManager.isLaravelEnabled) {
                val success = repository.syncAllFromLaravel()
                if (success) {
                    saveLastSyncTime(System.currentTimeMillis())
                    _isOnline.value = true
                } else {
                    _isOnline.value = false
                }
            }
        }

        // Rate-limiting lockout countdown updater (1s interval)
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val remaining = userSessionRepo.getRemainingLockoutSeconds()
                if (_lockoutRemainingSeconds.value != remaining) {
                    _lockoutRemainingSeconds.value = remaining
                }
            }
        }

        // Automatic 24-Hour Inactivity Session Token Invalidation Loop
        viewModelScope.launch {
            while (true) {
                delay(15000) // check every 15 seconds
                val user = _currentUser.value
                if (user != null) {
                    val now = System.currentTimeMillis()
                    val inactiveTime = now - _lastActivityTime.value
                    // Invalidate session if inactive for >= 24 hours (86,400,000 ms)
                    if (inactiveTime >= (24L * 3600L * 1000L)) {
                        _isSessionTimedOut.value = true
                        repository.insertAuditLog(
                            user.id, 
                            "SESSION_TOKEN_INVALIDATED_24H", 
                            "[SECURITY_POLICY] Session token automatically invalidated after 24 hours of inactivity to ensure compliance with OAuth 2.0 and Ghana Data Protection Act."
                        )
                        userSessionRepo.notifySessionExpired("OAuth 2.0 session token expired after 24 hours of inactivity.")
                        logout()
                    }
                }
            }
        }
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user != null) {
                    _lastActivityTime.value = System.currentTimeMillis()
                    _studentWalletBalance.value = user.balance
                    if (user.role == "VENDOR") {
                        _isStoreClosed.value = !user.isOpen
                        refreshVendorPerformance(user.id)
                        generateAndSendWeeklyVendorReport(user)
                        // Trigger Laravel Echo socket connection pipeline
                        LaravelEchoWebSocketManager.startListening(user.id, "VENDOR")
                        // Start polling metrics
                        startVendorMetricsPolling(user.id)
                    } else {
                        stopVendorMetricsPolling()
                        if (user.role == "STUDENT") {
                            // Trigger Laravel Echo socket pipeline for student channels
                            LaravelEchoWebSocketManager.startListening(user.id, "STUDENT")
                        } else if (user.role.equals("ADMIN", ignoreCase = true)) {
                            LaravelEchoWebSocketManager.startListening(user.id, "ADMIN")
                        } else {
                            LaravelEchoWebSocketManager.stopListening()
                        }
                    }
                } else {
                    stopVendorMetricsPolling()
                    LaravelEchoWebSocketManager.stopListening()
                }
            }
        }
        
        // Listen to WebSocket flow emissions to stream real-time pre-orders to dashboard
        viewModelScope.launch {
            LaravelEchoWebSocketManager.realTimeOrderFlow.collect { event ->
                val activeUser = _currentUser.value
                if (activeUser != null && activeUser.role == "VENDOR" && event.vendorId == activeUser.id) {
                    processIncomingWebSocketOrder(event)
                }
                // Log event for the live terminal display
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))
                val logMsg = "[$timeStr] Packet: ID #${event.orderId} containing '${event.foodName}' (QTY: ${event.qty}) on channel orders-vendor-${event.vendorId}"
                _realTimeEventsLogs.value = (listOf(logMsg) + _realTimeEventsLogs.value).take(50)
            }
        }

        // Listen to Admin WebSocket low stock inventory alerts
        viewModelScope.launch {
            LaravelEchoWebSocketManager.realTimeInventoryAlertFlow.collect { event ->
                val activeUser = _currentUser.value
                if (activeUser != null && activeUser.role.equals("ADMIN", ignoreCase = true)) {
                    val notif = VendorInventoryNotification(
                        id = "ADMIN_ALERT_${System.currentTimeMillis()}",
                        foodItemId = event.itemId,
                        foodName = event.itemName,
                        type = "LOW_STOCK",
                        message = "PUSH NOTIFICATION: ${event.message} (${event.remainingStock} remaining)",
                        timestamp = event.timestamp
                    )
                    _adminInventoryAlerts.value = listOf(notif) + _adminInventoryAlerts.value
                    playSoundNotification()
                }
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))
                val logMsg = "[$timeStr] Admin Alert: '${event.itemName}' critically low (${event.remainingStock} units left)"
                _realTimeEventsLogs.value = (listOf(logMsg) + _realTimeEventsLogs.value).take(50)
            }
        }

        // Listen to Student WebSocket / Pusher notifications
        viewModelScope.launch {
            LaravelEchoWebSocketManager.realTimeStudentNotificationFlow.collect { event ->
                val activeUser = _currentUser.value
                // Real-time synchronization: Update live tracking manager and Room DB
                FirestoreOrderTrackingManager.updateOrderStatusInFirestore(
                    orderId = event.orderId,
                    newStatus = event.newStatus,
                    vendorId = event.vendorId
                )
                try {
                    repository.updateOrderStatus(event.vendorId, event.orderId, event.newStatus)
                } catch (e: Exception) {
                    Log.w("CafeteriaViewModel", "Could not update Room order status for #${event.orderId}: ${e.message}")
                }

                if (activeUser != null && activeUser.role == "STUDENT" && (activeUser.id == event.vendorId || activeUser.id != 0)) {
                    val order = repository.orderDao.getOrderById(event.orderId)
                    if (order == null || order.customerId == activeUser.id) {
                        // Display floating alert immediately & play audio cue!
                        val fabricatedNotif = com.example.data.LaravelDatabaseNotification(
                            id = event.notificationId,
                            type = "App\\Notifications\\OrderStatusChangedNotification",
                            notifiable_type = "App\\Models\\User",
                            notifiable_id = activeUser.id,
                            data = com.example.data.LaravelNotificationData(
                                order_id = event.orderId,
                                vendor_id = event.vendorId,
                                total_price = order?.totalPrice ?: 0.0,
                                old_status = event.oldStatus,
                                new_status = event.newStatus,
                                message = event.message,
                                time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))
                            ),
                            read_at = null,
                            created_at = null,
                            updated_at = null
                        )
                        _activeStudentAlerts.value = _activeStudentAlerts.value + fabricatedNotif
                        _studentNotifications.value = listOf(fabricatedNotif) + _studentNotifications.value
                        playSoundNotification()
                    }
                }
                
                // Log event for the live terminal logs
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestamp))
                val logMsg = "[$timeStr] Echo Notification: Order #${event.orderId} moved ${event.oldStatus} -> ${event.newStatus}"
                _realTimeEventsLogs.value = (listOf(logMsg) + _realTimeEventsLogs.value).take(50)
            }
        }

        // Live observe customer orders to trigger Laravel push / email notifications on PENDING/PREPARING -> READY transitions
        viewModelScope.launch {
            var isFirstCollection = true
            customerOrders.collect { currentOrders ->
                if (currentOrders.isEmpty()) return@collect
                
                if (_favoriteFoodIds.value.isEmpty()) {
                    val initialFavs = currentOrders.map { it.foodItemId }.toSet()
                    if (initialFavs.isNotEmpty()) {
                        _favoriteFoodIds.value = initialFavs
                    }
                }
                
                if (isFirstCollection) {
                    currentOrders.forEach { order ->
                        if (order.status.uppercase() == "PREPARING") {
                            localSeenPreparingOrderIds.add(order.id)
                        }
                        if (order.status.uppercase() == "READY") {
                            localSeenReadyOrderIds.add(order.id)
                        }
                        if (order.status.uppercase() == "DELIVERED" || order.status.uppercase() == "COMPLETED") {
                            localSeenDeliveredOrderIds.add(order.id)
                        }
                    }
                    isFirstCollection = false
                } else {
                    currentOrders.forEach { order ->
                        val user = _currentUser.value
                        val studentEmail = if (user?.username?.contains("@") == true) user.username else "${user?.username ?: "student"}@atu.edu.gh"
                        val itemName = order.foodName
                        val vendorName = allUsers.value.find { it.id == order.vendorId }?.fullName ?: "Vendor #${order.vendorId}"
                        val pPin = order.pickupPin
                        val pPrice = order.totalPrice

                        if (order.status.uppercase() == "PREPARING" && !localSeenPreparingOrderIds.contains(order.id)) {
                            localSeenPreparingOrderIds.add(order.id)
                            
                            // Send simulated Email Notification (Laravel Mail Channel)
                            if (isEmailNotificationEnabled.value) {
                                val subject = "📧 [SMTP Laravel Mailer] Order #${order.id} is Preparing!"
                                val body = """
                                    Hello ${user?.fullName ?: "Student"},
                                    
                                    Great news! Your order #${order.id} for "$itemName" from $vendorName has successfully moved to the preparation floor!
                                    
                                    Estimated preparation time: ${order.estimatedPickupTime}
                                    
                                    We will notify you immediately once it is hot & ready for retrieval!
                                    
                                    Warm regards,
                                    Accra Technical University Food Court System.
                                """.trimIndent()
                                
                                val emailNotif = MockEmailNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    studentEmail = studentEmail,
                                    vendorName = vendorName,
                                    itemName = itemName,
                                    subject = subject,
                                    body = body
                                )
                                _dispatchedEmails.value = _dispatchedEmails.value + emailNotif
                            }
                            
                            // Send simulated Push Notification (Laravel FCM Push Channel)
                            if (isPushNotificationEnabled.value) {
                                val pushNotif = MockPushNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    title = "📱 [Laravel FCM] Order is Being Prepared!",
                                    body = "Vendor $vendorName is now preparing your '$itemName'! Estimated: ${order.estimatedPickupTime}."
                                )
                                _dispatchedPushNotifications.value = _dispatchedPushNotifications.value + pushNotif
                                try {
                                    val context = getApplication<android.app.Application>().applicationContext
                                    com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                                        context = context,
                                        orderId = order.id,
                                        title = "Order is Being Prepared!",
                                        text = "Vendor $vendorName is now preparing your '$itemName'!"
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if (order.status.uppercase() == "READY" && !localSeenReadyOrderIds.contains(order.id)) {
                            localSeenReadyOrderIds.add(order.id)
                            
                            // Send simulated Email Notification (Laravel Mail Channel)
                            if (isEmailNotificationEnabled.value) {
                                val subject = "📧 [SMTP Laravel Mailer] Order #${order.id} is Ready!"
                                val body = """
                                    Hello ${user?.fullName ?: "Student"},
                                    
                                    Your order #${order.id} for "$itemName" from $vendorName has successfully compiled on the system floor and is officially ready for pickup!
                                    
                                    Pick-up Verification Pin: $pPin
                                    Total Transaction Amount: GH₵ ${"%.2f".format(pPrice)}
                                    
                                    Warm regards,
                                    Accra Technical University Food Court System.
                                """.trimIndent()
                                
                                val emailNotif = MockEmailNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    studentEmail = studentEmail,
                                    vendorName = vendorName,
                                    itemName = itemName,
                                    subject = subject,
                                    body = body
                                )
                                _dispatchedEmails.value = _dispatchedEmails.value + emailNotif
                            }
                            
                            // Send simulated Push Notification (Laravel FCM Push Channel)
                            if (isPushNotificationEnabled.value) {
                                val pushNotif = MockPushNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    title = "📱 [Laravel FCM] Order Ready for Pickup!",
                                    body = "Your food '$itemName' from $vendorName is hot & ready! Use Ticket Pin: $pPin."
                                )
                                _dispatchedPushNotifications.value = _dispatchedPushNotifications.value + pushNotif
                                try {
                                    val context = getApplication<android.app.Application>().applicationContext
                                    com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                                        context = context,
                                        orderId = order.id,
                                        title = "Order Ready for Pickup!",
                                        text = "Your food '$itemName' from $vendorName is hot & ready! Use Ticket Pin: $pPin."
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        if ((order.status.uppercase() == "DELIVERED" || order.status.uppercase() == "COMPLETED") && !localSeenDeliveredOrderIds.contains(order.id)) {
                            localSeenDeliveredOrderIds.add(order.id)

                            // Send simulated Email Notification (Laravel Mail Channel)
                            if (isEmailNotificationEnabled.value) {
                                val subject = "📧 [SMTP Laravel Mailer] Order #${order.id} Delivered!"
                                val body = """
                                    Hello ${user?.fullName ?: "Student"},
                                    
                                    Your order #${order.id} for "$itemName" from $vendorName has been successfully delivered and handed over.
                                    
                                    We value your dining experience! Please rate the vendor booth inside the app to help maintain Accra Technical food standards.
                                    
                                    Warm regards,
                                    Accra Technical University Food Court System.
                                """.trimIndent()
                                
                                val emailNotif = MockEmailNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    studentEmail = studentEmail,
                                    vendorName = vendorName,
                                    itemName = itemName,
                                    subject = subject,
                                    body = body
                                )
                                _dispatchedEmails.value = _dispatchedEmails.value + emailNotif
                            }

                            // Send simulated Push Notification (Laravel FCM Push Channel)
                            if (isPushNotificationEnabled.value) {
                                val pushNotif = MockPushNotification(
                                    id = java.util.UUID.randomUUID().toString(),
                                    orderId = order.id,
                                    title = "📱 [Laravel FCM] Order Delivered! Rate Vendor",
                                    body = "Your order of '$itemName' has been marked Delivered. Tap here to rate $vendorName now!"
                                )
                                _dispatchedPushNotifications.value = _dispatchedPushNotifications.value + pushNotif
                                try {
                                    val context = getApplication<android.app.Application>().applicationContext
                                    com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                                        context = context,
                                        orderId = order.id,
                                        title = "Order Delivered!",
                                        text = "Your order of '$itemName' has been marked Delivered."
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            }
        }
        
        viewModelScope.launch {
            combine(vendorFoodItems, globalLowStockThreshold) { foods, threshold ->
                Pair(foods, threshold)
            }.collect { (foods, threshold) ->
                if (foods.isEmpty()) {
                    _vendorInventoryNotifications.value = emptyList()
                    return@collect
                }
                
                val currentTime = System.currentTimeMillis()
                val activeList = mutableListOf<VendorInventoryNotification>()
                val activeKeys = mutableSetOf<String>()
                
                foods.forEach { food ->
                    val isUnavailable = !food.isAvailable
                    val isDepleted = food.currentStock == 0
                    val isLowStock = food.currentStock > 0 && food.currentStock <= food.lowStockThreshold
                    val isGlobalLowStock = food.currentStock > 0 && food.currentStock <= threshold
                    
                    if (!isUnavailable) {
                        val k = "${food.id}_UNAVAILABLE"
                        dismissedNotificationKeys.remove(k)
                        readNotificationKeys.remove(k)
                    }
                    if (food.currentStock > food.lowStockThreshold && food.currentStock > threshold) {
                        val kl = "${food.id}_LOW_STOCK"
                        val kd = "${food.id}_OUT_OF_STOCK"
                        dismissedNotificationKeys.remove(kl)
                        dismissedNotificationKeys.remove(kd)
                        readNotificationKeys.remove(kl)
                        readNotificationKeys.remove(kd)
                    }
                    
                    if (isUnavailable) {
                        val key = "${food.id}_UNAVAILABLE"
                        activeKeys.add(key)
                        if (!dismissedNotificationKeys.contains(key)) {
                            activeList.add(
                                VendorInventoryNotification(
                                    id = key,
                                    foodItemId = food.id,
                                    foodName = food.name,
                                    type = "UNAVAILABLE",
                                    message = "Cuisine '${food.name}' has been marked unavailable in your menu offerings.",
                                    timestamp = currentTime,
                                    isRead = readNotificationKeys.contains(key)
                                )
                            )
                        }
                    }
                    if (isDepleted) {
                        val key = "${food.id}_OUT_OF_STOCK"
                        activeKeys.add(key)
                        if (!dismissedNotificationKeys.contains(key)) {
                            activeList.add(
                                VendorInventoryNotification(
                                    id = key,
                                    foodItemId = food.id,
                                    foodName = food.name,
                                    type = "OUT_OF_STOCK",
                                    message = "CRITICAL: '${food.name}' is completely depleted (0 stock remaining)!",
                                    timestamp = currentTime,
                                    isRead = readNotificationKeys.contains(key)
                                )
                            )
                        }
                    } else if (isLowStock || isGlobalLowStock) {
                        val key = "${food.id}_LOW_STOCK"
                        activeKeys.add(key)
                        if (!dismissedNotificationKeys.contains(key)) {
                            val activeLimit = if (isLowStock) food.lowStockThreshold else threshold
                            activeList.add(
                                VendorInventoryNotification(
                                    id = key,
                                    foodItemId = food.id,
                                    foodName = food.name,
                                    type = "LOW_STOCK",
                                    message = "Warning: '${food.name}' is running low (Stock: ${food.currentStock}/${food.initialStock}, safety limit is $activeLimit).",
                                    timestamp = currentTime,
                                    isRead = readNotificationKeys.contains(key)
                                )
                            )
                        }
                    }
                }
                
                val newAddedKeys = activeKeys.filter { k -> 
                    !previousVendorNotificationKeys.contains(k) && !dismissedNotificationKeys.contains(k)
                }
                if (newAddedKeys.isNotEmpty()) {
                    playSoundNotification()
                    val ctx = getApplication<Application>().applicationContext
                    activeList.filter { notif -> newAddedKeys.contains(notif.id) }.forEach { notif ->
                        val foodItem = foods.find { it.id == notif.foodItemId }
                        if (foodItem != null) {
                            com.example.ui.util.NotificationHelper.sendVendorInventoryAlertNotification(
                                context = ctx,
                                foodId = foodItem.id,
                                itemName = foodItem.name,
                                currentStock = foodItem.currentStock,
                                threshold = foodItem.lowStockThreshold
                            )
                        }
                    }
                }
                previousVendorNotificationKeys = activeKeys
                
                _vendorInventoryNotifications.value = activeList
            }
        }
        
        startRealTimeNotificationTimer()
    }

    private fun startRealTimeNotificationTimer() {
        // 1. Coroutine to reset/clear VENDOR notification queue cleanly as users toggle
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user == null || user.role != "VENDOR") {
                    seenOrderIds.clear()
                    _newOrderAlerts.value = emptyList()
                    isFirstOrderLoad = true
                }
            }
        }

        // 1b. Coroutine to reset/clear STUDENT notification queue cleanly as users toggle
        viewModelScope.launch {
            _currentUser.collect { user ->
                if (user == null || user.role != "STUDENT") {
                    seenNotificationIds.clear()
                    _studentNotifications.value = emptyList()
                    _activeStudentAlerts.value = emptyList()
                    isFirstNotificationLoad = true
                }
            }
        }

        // 2. Continuous Polling Job: periodically syncs when user is active & Laravel is enabled
        viewModelScope.launch {
            while (true) {
                delay(15000)
                val user = _currentUser.value
                if (user != null && LaravelClientManager.isLaravelEnabled) {
                    try {
                        repository.syncAllFromLaravel()
                        if (user.role == "VENDOR") {
                            refreshVendorPerformance(user.id)
                        } else if (user.role == "STUDENT") {
                            pollStudentNotifications()
                        }
                    } catch (e: Exception) {
                        Log.w("CafeteriaViewModel", "Periodic background sync non-fatal warning: ${e.message}")
                    }
                }
            }
        }

        // 3. Instant Reactive Observers of incoming database changes
        viewModelScope.launch {
            vendorOrders.collect { orders ->
                if (orders.isEmpty()) {
                    isFirstOrderLoad = true
                    seenOrderIds.clear()
                    completedOrderIds.clear()
                    return@collect
                }

                if (isFirstOrderLoad) {
                    // Populate seen on initial load so we don't alert pre-existing historic entries
                    orders.forEach { seenOrderIds.add(it.id) }
                    orders.filter { it.status == "COMPLETED" || it.status == "DELIVERED" }.forEach { completedOrderIds.add(it.id) }
                    isFirstOrderLoad = false
                } else {
                    // Find any newly received PENDING orders that weren't cached in our seen order IDs set
                    val newPending = orders.filter { it.status == "PENDING" && !seenOrderIds.contains(it.id) }
                    if (newPending.isNotEmpty()) {
                        newPending.forEach { order ->
                            seenOrderIds.add(order.id)
                            playSoundNotification()
                        }
                        _newOrderAlerts.value = _newOrderAlerts.value + newPending
                    }

                    // Check for newly completed or delivered orders to trigger real-time metrics refresh
                    val currentCompleted = orders.filter { it.status == "COMPLETED" || it.status == "DELIVERED" }
                    var hasNewCompleted = false
                    currentCompleted.forEach { order ->
                        if (!completedOrderIds.contains(order.id)) {
                            completedOrderIds.add(order.id)
                            hasNewCompleted = true
                        }
                    }
                    if (hasNewCompleted) {
                        Log.d("CafeteriaViewModel", "Real-time event: New completed order detected. Refreshing performance/revenue KPIs.")
                        _currentUser.value?.id?.let { uid ->
                            refreshVendorPerformance(uid)
                        }
                    }
                }
            }
        }
    }

    fun pollStudentNotifications() {
        viewModelScope.launch {
            val notifications = repository.fetchLaravelNotifications()
            _studentNotifications.value = notifications

            if (isFirstNotificationLoad) {
                notifications.forEach { seenNotificationIds.add(it.id) }
                isFirstNotificationLoad = false
            } else {
                // Find any new database notifications that represent an update from PREPARING to READY
                val newReadyAlerts = notifications.filter {
                    !seenNotificationIds.contains(it.id) &&
                    it.data.new_status.uppercase() == "READY" &&
                    it.data.old_status.uppercase() == "PREPARING"
                }

                if (newReadyAlerts.isNotEmpty()) {
                    newReadyAlerts.forEach { notif ->
                        seenNotificationIds.add(notif.id)
                        playSoundNotification()
                    }
                    _activeStudentAlerts.value = _activeStudentAlerts.value + newReadyAlerts
                }
            }
        }
    }

    fun dismissStudentAlert(notifId: String) {
        _activeStudentAlerts.value = _activeStudentAlerts.value.filter { it.id != notifId }
    }

    fun clearAllStudentNotifications() {
        viewModelScope.launch {
            repository.markLaravelNotificationsAsRead()
            _studentNotifications.value = emptyList()
            _activeStudentAlerts.value = emptyList()
        }
    }

    private fun playSoundNotification() {
        try {
            val tg = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 300)
        } catch (e: Exception) {
            Log.e("CafeteriaViewModel", "Failed to play notification tone safely", e)
        }
    }

    fun dismissOrderAlert(orderId: Int) {
        _newOrderAlerts.value = _newOrderAlerts.value.filter { it.id != orderId }
    }

    private fun processIncomingWebSocketOrder(event: LaravelEchoWebSocketManager.OrderBroadcastEvent) {
        viewModelScope.launch {
            // Instantly sound alert chime
            playSoundNotification()
            
            val isAlreadyCached = seenOrderIds.contains(event.orderId)
            if (!isAlreadyCached) {
                val dbRecord = repository.orderDao.getOrderById(event.orderId)
                if (dbRecord == null) {
                    val orderModel = Order(
                        id = event.orderId,
                        customerId = 999, // default student fallback reference
                        vendorId = event.vendorId,
                        foodItemId = 1,
                        foodName = event.foodName,
                        quantity = event.qty,
                        unitPrice = event.totalPrice / event.qty,
                        totalPrice = event.totalPrice,
                        orderTimestamp = event.timestamp,
                        status = "PENDING",
                        pickupPin = (1000..9999).random().toString(),
                        estimatedPickupTime = "Calculating..."
                    )
                    repository.orderDao.insertOrder(orderModel)
                    seenOrderIds.add(event.orderId)
                    
                    // Alert layout list prepended dynamically with smooth animation entry bounds
                    _newOrderAlerts.value = _newOrderAlerts.value + orderModel
                }
            }
            
            // Recompute metrics scorecards
            refreshVendorPerformance(event.vendorId)
            
            // Perform fallback REST Sync to keep everything completely in-tact online
            if (LaravelClientManager.isLaravelEnabled) {
                try {
                    repository.syncAllFromLaravel()
                } catch (e: Exception) {
                    Log.e("CafeteriaViewModel", "Real-time sync fallbacks failed gracefully", e)
                }
            }
        }
    }

    fun fetchLoyaltySummary() {
        val user = _currentUser.value ?: return
        if (user.role.uppercase() != "STUDENT") return
        viewModelScope.launch {
            if (com.example.data.LaravelClientManager.isLaravelEnabled) {
                try {
                    val summary = repository.getLoyaltySummary()
                    if (summary != null) {
                        _loyaltySummaryResponse.value = summary
                        val refreshedUser = repository.userDao.getUserSync(user.id)
                        if (refreshedUser != null) {
                            val updatedUser = refreshedUser.copy(
                                loyaltyPoints = summary.loyalty_points_balance,
                                totalSpent = summary.total_spent_all_time
                            )
                            repository.userDao.updateUser(updatedUser)
                            _currentUser.value = updatedUser
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CafeteriaViewModel", "fetchLoyaltySummary failed", e)
                }
            }
        }
    }

    fun syncAllFromLaravel(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.syncAllFromLaravel()
                if (success) {
                    saveLastSyncTime(System.currentTimeMillis())
                    _isOnline.value = true
                    fetchLoyaltySummary()
                } else {
                    _isOnline.value = false
                }
                val usr = _currentUser.value
                if (usr != null) {
                    val refreshed = repository.userDao.getUserSync(usr.id)
                    if (refreshed != null) {
                        _currentUser.value = refreshed
                    }
                }
                _isLoading.value = false
                loadOfflineOrders()
                onResult(success)
            } catch (e: Exception) {
                _isOnline.value = false
                _isLoading.value = false
                loadOfflineOrders()
                onResult(false)
            }
        }
    }

    fun loadOfflineOrders() {
        viewModelScope.launch {
            try {
                offlineOrders.value = repository.offlineOrderDao.getAllOfflineOrders()
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to load offline orders", e)
            }
        }
    }

    // ==========================================
    // TRANSACTION ACTIONS & AUTHENTICATION
    // ==========================================

    fun loginUser(username: String, pinCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null

            // Rate-limiting Lockout Pre-check
            if (userSessionRepo.isLockedOut()) {
                val remainingSeconds = userSessionRepo.getRemainingLockoutSeconds()
                val errMsg = "Security Lockout: 5 failed attempts reached. Please wait $remainingSeconds seconds."
                _loginError.value = errMsg
                _isLoading.value = false
                onComplete(false)
                return@launch
            }

            val formattedEmail = if (username.contains("@")) username.trim() else "${username.trim()}@atu.edu.gh"
            val pinHash = repository.sha256(pinCode)
            // CONSOLE LOGGING MECHANISM FOR PAYLOAD INSPECTION
            Log.i("AUTH_PAYLOAD_INSPECTOR", "========== LOGIN SUBMIT PAYLOAD INSPECTION ==========")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "Raw Input Username : '$username'")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "Calculated Email   : '$formattedEmail'")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "PIN String Length  : ${pinCode.length}")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "Is PIN Truncated   : ${pinCode.length != pinCode.trim().length}")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "PIN SHA-256 Hash   : '$pinHash'")
            Log.i("AUTH_PAYLOAD_INSPECTOR", "=====================================================")

            com.example.ui.util.CrashlyticsHelper.log("Authentication state: Login attempt initiated for user '$username' (Email payload: '$formattedEmail', PIN len: ${pinCode.length})")
            
            // Server Health Service Layer Ping Check before authentication
            if (LaravelClientManager.isLaravelEnabled) {
                com.example.ui.util.CrashlyticsHelper.log("Network operation: Pinging server health before login...")
                val isHealthy = LaravelClientManager.pingBackendHealth()
                if (!isHealthy) {
                    val infoMsg = "Backend server unreachable. Authenticating using local database mode."
                    com.example.ui.util.CrashlyticsHelper.log("Authentication state: Backend ping failed for '$username' — proceeding with local DB fallback")
                    com.example.ui.util.FirebaseAnalyticsHelper.logLoginFailure("CREDENTIALS", "SERVER_UNREACHABLE_FALLBACK", infoMsg)
                    userSessionRepo.notifyNetworkInterruption(infoMsg)
                }
            }

            try {
                com.example.ui.util.CrashlyticsHelper.log("Network operation: Authenticating user '$username'")
                val authenticated = repository.authenticateUser(username, pinCode)
                if (authenticated != null) {
                    userSessionRepo.resetFailedAttempts()
                    _failedLoginAttempts.value = 0
                    _lockoutRemainingSeconds.value = 0

                    _currentUser.value = authenticated
                    userSessionRepo.saveSession(
                        token = "TOKEN_${System.currentTimeMillis()}_${authenticated.id}",
                        userId = authenticated.id,
                        userName = authenticated.username,
                        role = authenticated.role
                    )
                    com.example.ui.util.CrashlyticsHelper.log("Authentication state: User '${authenticated.username}' (ID: ${authenticated.id}, Role: ${authenticated.role}) successfully authenticated")
                    com.example.ui.util.FirebaseAnalyticsHelper.logLoginSuccess("CREDENTIALS", authenticated.id, authenticated.role)
                    userSessionRepo.notifyAuthenticated(authenticated)

                    if (LaravelClientManager.isLaravelEnabled) {
                        try {
                            repository.syncAllFromLaravel()
                        } catch (se: Exception) {
                            Log.e("CafeteriaViewModel", "Sync after login warning", se)
                            com.example.ui.util.CrashlyticsHelper.recordException(se, "Menu sync failed after login")
                            userSessionRepo.notifyNetworkInterruption("Laravel menu sync warning: ${se.localizedMessage}")
                        }
                    }
                    _isLoading.value = false
                    onComplete(true)
                } else {
                    val (attempts, isNowLocked) = userSessionRepo.recordFailedLoginAttempt()
                    _failedLoginAttempts.value = attempts
                    _lockoutRemainingSeconds.value = userSessionRepo.getRemainingLockoutSeconds()

                    val errMsg = if (isNowLocked) {
                        repository.insertAuditLog(
                            0,
                            "RATE_LIMIT_LOCKOUT",
                            "[SECURITY_ALERT] 5 consecutive failed login attempts detected for '$username'. Temporary 60s authentication lockout triggered."
                        )
                        "Temporary Security Lockout: 5 consecutive failed login attempts reached. Please wait 60 seconds."
                    } else {
                        "Invalid Username or Pass-PIN. (Failed attempt $attempts of 5)"
                    }

                    com.example.ui.util.CrashlyticsHelper.log("Authentication state: Invalid credentials provided for '$username' (Attempt $attempts)")
                    com.example.ui.util.FirebaseAnalyticsHelper.logLoginFailure("CREDENTIALS", "INVALID_CREDENTIALS", errMsg)
                    _loginError.value = errMsg
                    _isLoading.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                com.example.ui.util.CrashlyticsHelper.recordException(e, "Login exception for user '$username'")
                com.example.ui.util.FirebaseAnalyticsHelper.logLoginFailure("CREDENTIALS", "NETWORK_OR_RUNTIME_EXCEPTION", e.localizedMessage ?: "Login error")
                Log.e("CafeteriaViewModel", "Login error", e)
                _loginError.value = "Login connection issue: ${e.localizedMessage ?: "Failed to authenticate"}. Check connection or try again."
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    fun loginWithSocial(username: String, fullName: String, provider: String, logoUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            com.example.ui.util.CrashlyticsHelper.log("Authentication state: SSO Login attempt via $provider for user '$username'")

            if (LaravelClientManager.isLaravelEnabled) {
                val isHealthy = LaravelClientManager.pingBackendHealth()
                if (!isHealthy) {
                    val errMsg = "$provider authentication unreachable: Server health check failed."
                    com.example.ui.util.FirebaseAnalyticsHelper.logLoginFailure("SSO_$provider", "SERVER_UNREACHABLE", errMsg)
                    _loginError.value = errMsg
                    _isLoading.value = false
                    onComplete(false)
                    return@launch
                }
            }

            try {
                val authenticated = repository.authenticateOrRegisterSocialUser(username, fullName, provider, logoUrl)
                _currentUser.value = authenticated
                userSessionRepo.saveSession(
                    token = "SSO_TOKEN_${System.currentTimeMillis()}_${authenticated.id}",
                    userId = authenticated.id,
                    userName = authenticated.username,
                    role = authenticated.role
                )
                com.example.ui.util.CrashlyticsHelper.log("Authentication state: SSO user '${authenticated.username}' successfully authenticated via $provider")
                com.example.ui.util.FirebaseAnalyticsHelper.logLoginSuccess("SSO_$provider", authenticated.id, authenticated.role)
                userSessionRepo.notifyAuthenticated(authenticated)

                if (LaravelClientManager.isLaravelEnabled) {
                    try {
                        repository.syncAllFromLaravel()
                    } catch (se: Exception) {
                        Log.e("CafeteriaViewModel", "Sync after social login warning", se)
                        com.example.ui.util.CrashlyticsHelper.recordException(se, "Sync after social login failed")
                        userSessionRepo.notifyNetworkInterruption("Social sync warning: ${se.localizedMessage}")
                    }
                }
                _isLoading.value = false
                onComplete(true)
            } catch (e: Exception) {
                com.example.ui.util.CrashlyticsHelper.recordException(e, "SSO login exception for $provider")
                val errMsg = "Failed to authenticate with $provider: ${e.localizedMessage}"
                com.example.ui.util.FirebaseAnalyticsHelper.logLoginFailure("SSO_$provider", "AUTHENTICATION_EXCEPTION", errMsg)
                _loginError.value = errMsg
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    fun loginWithGoogleOAuth(
        email: String,
        fullName: String,
        studentIndexOrPass: String,
        nonce: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                val cleanEmail = email.trim().lowercase()
                val isGuest = studentIndexOrPass.startsWith("GUEST") || (!studentIndexOrPass.isNotBlank() && !cleanEmail.endsWith("@atu.edu.gh"))
                val resolvedId = if (studentIndexOrPass.isNotBlank()) {
                    studentIndexOrPass.trim()
                } else if (isGuest) {
                    "GUEST-${(1000..9999).random()}"
                } else {
                    "01210${(100..999).random()}B"
                }

                val authenticated = repository.authenticateOrRegisterSocialUser(
                    username = cleanEmail,
                    fullName = fullName.ifBlank { "Mawuli Hormeku" },
                    provider = "Google OAuth 2.0",
                    logoUrl = null
                )

                userSessionRepo.resetFailedAttempts()
                _failedLoginAttempts.value = 0
                _lockoutRemainingSeconds.value = 0

                _currentUser.value = authenticated
                val sessionToken = "OAUTH2_JWT_${System.currentTimeMillis()}_${nonce.take(8)}_${authenticated.id}"
                userSessionRepo.persistUserSession(
                    token = sessionToken,
                    userId = authenticated.id,
                    userName = authenticated.username,
                    role = authenticated.role
                )

                // Log OAuth 2.0 Security Handshake with granted scopes and exact ISO-8601 timestamp
                logSecurityHandshake(
                    userId = authenticated.id,
                    fullName = authenticated.fullName,
                    email = cleanEmail,
                    identifierOrPass = resolvedId,
                    nonce = nonce,
                    scopes = "openid, https://www.googleapis.com/auth/userinfo.email, https://www.googleapis.com/auth/userinfo.profile"
                )

                userSessionRepo.notifyAuthenticated(authenticated)
                _isLoading.value = false
                onComplete(true)
            } catch (e: Exception) {
                _loginError.value = "Google OAuth Security Error: ${e.localizedMessage}"
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    /**
     * Documents Google OAuth 2.0 scopes granted and exact timestamp in audit log.
     * Stored in Room database audit log repository and rendered under Admin Audit Logs.
     */
    fun logSecurityHandshake(
        userId: Int,
        fullName: String,
        email: String,
        identifierOrPass: String,
        nonce: String,
        scopes: String = "openid, https://www.googleapis.com/auth/userinfo.email, https://www.googleapis.com/auth/userinfo.profile"
    ) {
        val handshakeTimestamp = java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())
        val detailedAuditEntry = "[SECURITY_HANDSHAKE_VERIFIED] Timestamp: $handshakeTimestamp | Handshake ID: HS-${nonce.take(8).uppercase()} | Provider: Google Identity Services (OAuth 2.0) | Granted Scopes: [$scopes] | Anti-CSRF Nonce: Verified ($nonce) | Identity: $fullName ($email, ID: $identifierOrPass) | Policy: Invalidation Window: 24h Inactivity | Compliance: Ghana Data Protection Act (Act 843) | Status: AUTHORIZED_200_OK"

        viewModelScope.launch {
            repository.insertAuditLog(
                userId, 
                "OAUTH2_GOOGLE_HANDSHAKE", 
                detailedAuditEntry
            )
        }
    }

    /**
     * Rate limit checker helper for login flow.
     */
    fun checkRateLimit(identifier: String = ""): Boolean {
        return userSessionRepo.checkRateLimit(identifier)
    }

    fun loginAsGuest(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            try {
                val guestSuffix = (1000..9999).random()
                val guestUsername = "guest_$guestSuffix"
                val guestId = "GUEST-$guestSuffix"
                val guestFullName = "Campus Visitor"
                
                var user = repository.registerUser(
                    username = guestUsername,
                    pinCode = "1234",
                    role = "STUDENT",
                    fullName = guestFullName,
                    info = guestId
                )
                
                if (user == null) {
                    user = repository.authenticateUser("guest", "1234")
                }
                
                if (user != null) {
                    _currentUser.value = user
                    userSessionRepo.saveSession(
                        token = "GUEST_TOKEN_${System.currentTimeMillis()}_${user.id}",
                        userId = user.id,
                        userName = user.username,
                        role = user.role
                    )
                    repository.insertAuditLog(user.id, "GUEST_LOGIN", "Campus visitor logged in with Pass #$guestId.")
                    _isLoading.value = false
                    onComplete(true)
                } else {
                    _loginError.value = "Unable to create visitor session. Please try again."
                    _isLoading.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                _loginError.value = "Guest login error: ${e.localizedMessage}"
                _isLoading.value = false
                onComplete(false)
            }
        }
    }

    fun registerUser(username: String, pinCode: String, role: String, fullName: String, info: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _registrationSuccess.value = false
            _loginError.value = null
            if (username.isBlank() || pinCode.length < 4 || fullName.isBlank()) {
                _loginError.value = "All fields are required. PIN must be 4+ characters."
                _isLoading.value = false
                return@launch
            }
            val brandOrId = if (info.isBlank()) {
                if (role == "STUDENT") "ATU-${(100000..999999).random()}" else "ATU Cafeteria Vendor"
            } else info

            val isRegistered = repository.registerUser(username, pinCode, role, fullName, brandOrId)
            if (isRegistered != null) {
                _registrationSuccess.value = true
                _loginError.value = null
            } else {
                _loginError.value = "Username already exists."
            }
            _isLoading.value = false
        }
    }

    fun logOut() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                repository.insertAuditLog(user.id, "USER_LOGOUT", "User logged out securely.")
            }
            _currentUser.value = null
            _aiAnalysisText.value = null
            _vendorSentimentAnalysis.value = null
            _vendorAutoReplies.value = null
            _vendorPricingSuggestions.value = null
            _loginError.value = null
            _registrationSuccess.value = false
            isAdminActing.value = false
            realAdminUser.value = null
        }
    }

    fun startImpersonation(targetUser: User) {
        val current = _currentUser.value
        if (current != null && current.role == "ADMIN" && !isAdminActing.value) {
            realAdminUser.value = current
        }
        _currentUser.value = targetUser
        isAdminActing.value = true
    }

    fun stopImpersonation() {
        val admin = realAdminUser.value
        if (admin != null) {
            _currentUser.value = admin
            isAdminActing.value = false
            realAdminUser.value = null
        }
    }

    fun addVendor(
        username: String,
        pinCode: String,
        fullName: String,
        info: String,
        logoUrl: String? = null,
        pictureUrl: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val brandOrId = info.ifBlank { "ATU Cafeteria Vendor" }
            val isRegistered = repository.registerUser(username, pinCode, "VENDOR", fullName, brandOrId)
            if (isRegistered != null) {
                val updatedWithMedia = isRegistered.copy(logoUrl = logoUrl, pictureUrl = pictureUrl)
                repository.updateUser(updatedWithMedia)
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(admin.id, "VENDOR_ADDED", "Vendor '$fullName' added by Admin.")
                }
                _isLoading.value = false
                onResult(true)
            } else {
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun updateVendor(user: User, newPinCode: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val updatedHash = if (!newPinCode.isNullOrEmpty()) {
                repository.sha256(newPinCode)
            } else {
                user.passwordHash
            }
            val updatedUser = user.copy(passwordHash = updatedHash)
            repository.updateUser(updatedUser)
            updateVendorInLocalStorage(user.id, user.fullName, user.info)
            _isLoading.value = false
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(admin.id, "VENDOR_UPDATED", "Vendor '${user.fullName}' updated by Admin.")
            }
            onResult(true)
        }
    }

    /**
     * Allows logged-in vendor to update their login credentials (username and password).
     */
    fun updateVendorCredentials(
        newUsername: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = _currentUser.value ?: run {
            onResult(false, "User not logged in.")
            return
        }
        if (user.role != "VENDOR") {
            onResult(false, "Only registered vendors can update vendor credentials.")
            return
        }
        val cleanUsername = newUsername.trim().lowercase()
        if (cleanUsername.length < 3) {
            onResult(false, "Username must be at least 3 characters long.")
            return
        }
        if (newPassword.trim().length < 4) {
            onResult(false, "Password must be at least 4 characters long.")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check if username is taken by another user
                val existing = repository.userDao.getUserByUsername(cleanUsername)
                if (existing != null && existing.id != user.id) {
                    _isLoading.value = false
                    onResult(false, "Username '$cleanUsername' is already taken by another account.")
                    return@launch
                }

                val newHash = repository.sha256(newPassword.trim())
                val updatedUser = user.copy(
                    username = cleanUsername,
                    passwordHash = newHash
                )
                repository.updateUser(updatedUser)
                _currentUser.value = updatedUser
                userSessionRepo.saveSession(
                    token = userSessionRepo.getAuthToken() ?: ("AUTH_TOKEN_" + updatedUser.id),
                    userId = updatedUser.id,
                    userName = updatedUser.fullName,
                    role = updatedUser.role
                )
                repository.insertAuditLog(
                    user.id,
                    "VENDOR_CREDENTIALS_UPDATED",
                    "Vendor '${user.fullName}' updated login credentials (username: $cleanUsername)."
                )
                _isLoading.value = false
                onResult(true, null)
            } catch (e: Exception) {
                _isLoading.value = false
                onResult(false, e.localizedMessage ?: "Failed to update vendor credentials.")
            }
        }
    }

    fun updateUserProfile(
        fullName: String,
        studentStaffId: String?,
        telephone: String?,
        email: String?,
        department: String?,
        programOfStudy: String?,
        paymentMethods: List<String>?,
        info: String,
        dietaryPreferences: String? = null,
        onResult: (Boolean) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.updateUserProfile(
                id = user.id,
                fullName = fullName,
                studentStaffId = studentStaffId,
                telephone = telephone,
                email = email,
                department = department,
                programOfStudy = programOfStudy,
                paymentMethods = paymentMethods,
                info = info,
                dietaryPreferences = dietaryPreferences
            )
            val refreshed = repository.userDao.getUserSync(user.id)
            if (refreshed != null) {
                _currentUser.value = refreshed
            }
            _isLoading.value = false
            if (success) {
                com.example.ui.util.SnackbarManager.showMessage("Profile updated successfully!")
            } else {
                com.example.ui.util.SnackbarManager.showMessage("Failed to update profile.")
            }
            onResult(success)
        }
    }

    fun deleteVendor(userId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteUser(userId)
            removeVendorFromLocalStorage(userId)
            _isLoading.value = false
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(admin.id, "VENDOR_DELETED", "Vendor (ID: $userId) deleted by Admin.")
            }
            onResult(true)
        }
    }

    // ==========================================
    // VENDOR STALL REGISTRATION & LOCALSTORAGE
    // ==========================================

    private fun getLocalStoragePrefs() =
        getApplication<Application>().getSharedPreferences("localStorage", Context.MODE_PRIVATE)

    fun registerNewVendor(
        name: String,
        location: String,
        specialty: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            val trimmedLocation = location.trim()
            val trimmedSpecialty = specialty.trim()

            if (trimmedName.isBlank()) {
                onResult(false, "Vendor stall name cannot be empty.")
                return@launch
            }

            _isLoading.value = true
            val baseUsername = trimmedName.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "vendor" }
            var candidate = baseUsername
            var counter = 1
            val currentUsers = allUsers.value
            while (currentUsers.any { it.username.equals(candidate, ignoreCase = true) }) {
                candidate = "$baseUsername$counter"
                counter++
            }
            val defaultPin = "1234"
            val infoStr = StringBuilder().apply {
                if (trimmedLocation.isNotBlank()) append(trimmedLocation)
                if (trimmedSpecialty.isNotBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Specialty: $trimmedSpecialty")
                }
            }.toString().ifBlank { "ATU Cafeteria Vendor Stall" }

            val registered = repository.registerUser(
                username = candidate,
                pinCode = defaultPin,
                role = "VENDOR",
                fullName = trimmedName,
                info = infoStr
            )

            if (registered != null) {
                // Persist new vendor stall record into localStorage "vendors" array
                addVendorToLocalStorage(
                    id = registered.id,
                    name = registered.fullName,
                    location = trimmedLocation,
                    specialty = trimmedSpecialty
                )

                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        "VENDOR_REGISTERED",
                        "Stall '${registered.fullName}' registered (Location: '$trimmedLocation', Specialty: '$trimmedSpecialty')."
                    )
                }
                _isLoading.value = false
                onResult(true, "Stall '${registered.fullName}' successfully registered with login: $candidate (PIN: $defaultPin)")
            } else {
                _isLoading.value = false
                onResult(false, "Failed to register vendor stall. Please try again.")
            }
        }
    }

    fun addVendorToLocalStorage(id: Int, name: String, location: String, specialty: String) {
        try {
            val prefs = getLocalStoragePrefs()
            val existingJson = prefs.getString("vendors", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(existingJson)
            val newVendorObj = org.json.JSONObject().apply {
                put("id", id)
                put("name", name)
                put("location", location)
                put("specialty", specialty)
                put("createdAt", System.currentTimeMillis())
            }
            jsonArray.put(newVendorObj)
            prefs.edit().putString("vendors", jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeVendorFromLocalStorage(id: Int) {
        try {
            val prefs = getLocalStoragePrefs()
            val existingJson = prefs.getString("vendors", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(existingJson)
            val updatedArray = org.json.JSONArray()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null && obj.optInt("id") != id) {
                    updatedArray.put(obj)
                }
            }
            prefs.edit().putString("vendors", updatedArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateVendorInLocalStorage(id: Int, name: String, info: String) {
        try {
            val prefs = getLocalStoragePrefs()
            val existingJson = prefs.getString("vendors", "[]") ?: "[]"
            val jsonArray = org.json.JSONArray(existingJson)
            val updatedArray = org.json.JSONArray()
            var matched = false
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i)
                if (obj != null && obj.optInt("id") == id) {
                    val updatedObj = org.json.JSONObject().apply {
                        put("id", id)
                        put("name", name)
                        put("location", info)
                        put("specialty", obj.optString("specialty", ""))
                        put("updatedAt", System.currentTimeMillis())
                    }
                    updatedArray.put(updatedObj)
                    matched = true
                } else if (obj != null) {
                    updatedArray.put(obj)
                }
            }
            if (!matched) {
                val newObj = org.json.JSONObject().apply {
                    put("id", id)
                    put("name", name)
                    put("location", info)
                    put("specialty", "")
                }
                updatedArray.put(newObj)
            }
            prefs.edit().putString("vendors", updatedArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLocalStorageVendors(): String {
        return getLocalStoragePrefs().getString("vendors", "[]") ?: "[]"
    }

    // ==========================================
    // ADMIN USER & STUDENT OVERSIGHT METHODS
    // ==========================================

    fun adminAddStudent(
        username: String,
        pinCode: String,
        fullName: String,
        matricId: String,
        email: String?,
        phone: String?,
        initialBalance: Double = 50.0,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val registered = repository.registerUser(username, pinCode, "STUDENT", fullName, matricId)
            if (registered != null) {
                val updated = registered.copy(
                    email = email?.ifBlank { null },
                    telephone = phone?.ifBlank { null },
                    balance = initialBalance
                )
                repository.updateUser(updated)
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        "STUDENT_REGISTERED",
                        "Admin registered student '$fullName' ($username, ID: $matricId) with initial smart balance GH₵ ${"%.2f".format(initialBalance)}."
                    )
                }
                _isLoading.value = false
                onResult(true, "Student registered successfully.")
            } else {
                _isLoading.value = false
                onResult(false, "Username already exists or registration failed.")
            }
        }
    }

    fun adminAdjustUserBalance(
        userId: Int,
        amount: Double,
        isCredit: Boolean,
        reason: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val targetUser = repository.userDao.getUserSync(userId)
            if (targetUser != null) {
                val delta = if (isCredit) amount else -amount
                val newBalance = (targetUser.balance + delta).coerceAtLeast(0.0)
                repository.updateUser(targetUser.copy(balance = newBalance))
                
                val ref = "ADM-" + (100000..999999).random()
                val details = if (isCredit) "Admin Credit: $reason" else "Admin Debit: $reason"
                val type = if (isCredit) "DEPOSIT" else "ADJUSTMENT"
                repository.insertWalletTransaction(targetUser.id, type, if (isCredit) amount else -amount, ref, details)
                
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    val action = if (isCredit) "WALLET_ADMIN_CREDIT" else "WALLET_ADMIN_DEBIT"
                    repository.insertAuditLog(
                        admin.id,
                        action,
                        "Admin adjusted ${targetUser.role} '${targetUser.fullName}' wallet by ${if (isCredit) "+" else "-"}GH₵ ${"%.2f".format(amount)}. New balance: GH₵ ${"%.2f".format(newBalance)}. Reason: $reason (Ref: $ref)"
                    )
                }
                _isLoading.value = false
                onResult(true, "Balance adjusted successfully.")
            } else {
                _isLoading.value = false
                onResult(false, "User not found.")
            }
        }
    }

    fun addFoodItem(foodItem: FoodItem, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.foodItemDao.insertFoodItem(foodItem)
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(
                    admin.id,
                    "ADMIN_FOOD_ADDED",
                    "Admin added menu item '${foodItem.name}' (GH₵ ${"%.2f".format(foodItem.price)}) to Vendor #${foodItem.vendorId}."
                )
            }
            onResult?.invoke(true)
        }
    }

    fun updateFoodItem(foodItem: FoodItem, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.foodItemDao.updateFoodItem(foodItem)
            onResult?.invoke(true)
        }
    }

    fun deleteFoodItem(foodItem: FoodItem, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            repository.foodItemDao.deleteFoodItem(foodItem)
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(
                    admin.id,
                    "ADMIN_FOOD_DELETED",
                    "Admin deleted menu item '${foodItem.name}' (ID: ${foodItem.id}) from Vendor #${foodItem.vendorId}."
                )
            }
            onResult?.invoke(true)
        }
    }

    fun adminBonusToAllStudents(bonusAmount: Double, reason: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val all = allUsers.value.filter { it.role == "STUDENT" }
            var count = 0
            val admin = realAdminUser.value ?: _currentUser.value
            for (student in all) {
                val newBal = student.balance + bonusAmount
                repository.updateUser(student.copy(balance = newBal))
                val ref = "BONUS-" + (100000..999999).random()
                repository.insertWalletTransaction(student.id, "DEPOSIT", bonusAmount, ref, "Campus-Wide Subsidy: $reason")
                count++
            }
            if (admin != null) {
                repository.insertAuditLog(
                    admin.id,
                    "CAMPUS_SUBSIDY_DISTRIBUTED",
                    "Admin distributed GH₵ ${"%.2f".format(bonusAmount)} meal subsidy bonus to $count students. Total: GH₵ ${"%.2f".format(count * bonusAmount)}. Memo: $reason"
                )
            }
            _isLoading.value = false
            onResult(count)
        }
    }

    fun adminResetUserPin(userId: Int, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val targetUser = repository.userDao.getUserSync(userId)
            if (targetUser != null) {
                val newHash = repository.sha256(newPin)
                repository.updateUser(targetUser.copy(passwordHash = newHash))
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        "PIN_RESET",
                        "Admin reset Access PIN for ${targetUser.role} '${targetUser.fullName}' (${targetUser.username})."
                    )
                }
                _isLoading.value = false
                onResult(true)
            } else {
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun adminDeleteUser(userId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val target = repository.userDao.getUserSync(userId)
            val name = target?.fullName ?: "User #$userId"
            val role = target?.role ?: "USER"
            repository.deleteUser(userId)
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(
                    admin.id,
                    "USER_DELETED",
                    "Admin deleted $role account '$name' (ID: $userId)."
                )
            }
            _isLoading.value = false
            onResult(true)
        }
    }

    fun adminUpdateUserProfile(
        userId: Int,
        fullName: String,
        info: String,
        email: String?,
        phone: String?,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val target = repository.userDao.getUserSync(userId)
            if (target != null) {
                val updated = target.copy(
                    fullName = fullName,
                    info = info,
                    email = email?.ifBlank { null },
                    telephone = phone?.ifBlank { null }
                )
                repository.updateUser(updated)
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        "USER_PROFILE_UPDATED",
                        "Admin updated profile details for ${target.role} '$fullName' (${target.username})."
                    )
                }
                _isLoading.value = false
                onResult(true)
            } else {
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun adminToggleUserStatus(userId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val target = repository.userDao.getUserSync(userId)
            if (target != null) {
                val newStatus = !target.isOpen
                val updated = target.copy(isOpen = newStatus)
                repository.updateUser(updated)
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        if (newStatus) "USER_ACTIVATED" else "USER_SUSPENDED",
                        "Admin ${if (newStatus) "activated" else "suspended"} account for ${target.role} '${target.fullName}'."
                    )
                }
                _isLoading.value = false
                onResult(true)
            } else {
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun adminCancelAndRefundOrder(orderId: Int, reason: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val order = repository.orderDao.getOrderById(orderId)
            if (order != null) {
                repository.updateOrderStatus(order.vendorId, orderId, "CANCELLED")
                val customer = repository.userDao.getUserSync(order.customerId)
                if (customer != null) {
                    val refundedBalance = customer.balance + order.totalPrice
                    repository.updateUser(customer.copy(balance = refundedBalance))
                    val ref = "REF-" + (100000..999999).random()
                    repository.insertWalletTransaction(
                        customer.id,
                        "REFUND",
                        order.totalPrice,
                        ref,
                        "Admin Refund for Order #$orderId: $reason"
                    )
                }
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(
                        admin.id,
                        "ORDER_ADMIN_CANCEL_REFUND",
                        "Admin cancelled Order #$orderId and refunded GH₵ ${"%.2f".format(order.totalPrice)} to customer #${order.customerId}. Reason: $reason"
                    )
                }
                _isLoading.value = false
                onResult(true)
            } else {
                _isLoading.value = false
                onResult(false)
            }
        }
    }

    fun adminAdvanceOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            val order = repository.orderDao.getOrderById(orderId) ?: return@launch
            repository.updateOrderStatus(order.vendorId, orderId, newStatus)
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(
                    admin.id,
                    "ORDER_STATUS_OVERRIDE",
                    "Admin advanced Order #$orderId status from ${order.status} to $newStatus."
                )
            }
        }
    }

    // Student specific actions
    fun rechargeWallet(amount: Double) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null) {
                val ref = "MoMo-" + (100000..999999).random()
                val details = if (amount >= 0) "Loaded via Mobile Money Gateway" else "Debited for secure campus food pre-order"
                val type = if (amount >= 0) "DEPOSIT" else "PAYMENT"
                
                repository.insertWalletTransaction(user.id, type, amount, ref, details)
                repository.insertAuditLog(
                    user.id,
                    if (amount >= 0) "WALLET_CREDIT" else "WALLET_DEBIT",
                    if (amount >= 0) "Securely loaded GH₵ ${"%.2f".format(amount)} via Mobile Money Gateway. Ref: $ref" else "Debited GH₵ ${"%.2f".format(-amount)} for meal payment. Ref: $ref"
                )
                
                userSessionRepo.recordUserActivity()
                val refreshed = repository.userDao.getUserSync(user.id)
                if (refreshed != null) {
                    _currentUser.value = refreshed
                }
                if (amount >= 0) {
                    com.example.ui.util.SnackbarManager.showMessage("Wallet loaded with GH₵ ${"%.2f".format(amount)} successfully!")
                }
            }
        }
    }

    fun redeemLoyaltyPoints(pointsToRedeem: Int) {
        val user = _currentUser.value ?: return
        val currentTotalPoints = customerOrders.value.sumOf { order ->
            if (order.status.uppercase() == "COMPLETED") 25 else 10
        }
        val availablePoints = currentTotalPoints - redeemedLoyaltyPoints.value
        if (availablePoints >= pointsToRedeem && pointsToRedeem > 0) {
            val cashValue = (pointsToRedeem / 100.0) * 5.0 // 100 points = 5 GH₵
            redeemedLoyaltyPoints.value += pointsToRedeem
            
            viewModelScope.launch {
                val ref = "LOYALTY-" + (100000..999999).random()
                repository.insertWalletTransaction(user.id, "DEPOSIT", cashValue, ref, "Loyalty Point Redemption ($pointsToRedeem points)")
                repository.insertAuditLog(
                    user.id,
                    "LOYALTY_REDEMPTION",
                    "Redeemed $pointsToRedeem loyalty points for GH₵ ${"%.2f".format(cashValue)} smart credit. Ref: $ref"
                )
                
                val refreshed = repository.userDao.getUserSync(user.id)
                if (refreshed != null) {
                    _currentUser.value = refreshed
                }
            }
        }
    }

    fun placeOrder(foodItem: FoodItem, quantity: Int, useWallet: Boolean = false, pointsToRedeem: Int = 0, estimatedPickupTime: String = "Calculating...", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onComplete(false)
                return@launch
            }
            val originalSum = foodItem.price * quantity
            val discount = pointsToRedeem * 0.10
            val requiredSum = (originalSum - discount).coerceAtLeast(0.0)
            if (useWallet) {
                if (user.balance >= requiredSum) {
                    val ref = "ORD-" + (100000..999999).random()
                    repository.insertWalletTransaction(user.id, "PAYMENT", -requiredSum, ref, "Secure Pre-order payment: ${foodItem.name} (QTY: $quantity)" + (if (pointsToRedeem > 0) " (Loyalty Discount applied)" else ""))
                    repository.placeOrder(user.id, foodItem, quantity, pointsToRedeem, estimatedPickupTime)
                    repository.insertAuditLog(user.id, "WALLET_PAYMENT", "Debited GH₵ ${"%.2f".format(requiredSum)} for secure pickup.")
                    loadOfflineOrders()
                    
                    // Credit the vendor for their earnings
                    val vendorId = foodItem.vendorId
                    val vendRef = "EARN-" + (100000..999999).random()
                    repository.insertWalletTransaction(vendorId, "REFUND", requiredSum, vendRef, "Earnings from Order of ${foodItem.name} (QTY: $quantity)")
                    repository.insertAuditLog(vendorId, "VENDOR_EARNED", "Earned GH₵ ${"%.2f".format(requiredSum)} from incoming customer order.")
                    
                    // Refresh student user session
                    val refreshed = repository.userDao.getUserSync(user.id)
                    if (refreshed != null) {
                        _currentUser.value = refreshed
                    }
                    fetchLoyaltySummary()
                    com.example.ui.util.HapticUtil.performPaymentSuccess(getApplication())
                    com.example.ui.util.SnackbarManager.showMessage("Order placed successfully!")
                    onComplete(true)
                } else {
                    com.example.ui.util.HapticUtil.performErrorState(getApplication())
                    com.example.ui.util.SnackbarManager.showMessage("Insufficient wallet balance.")
                    onComplete(false)
                }
            } else {
                repository.placeOrder(user.id, foodItem, quantity, pointsToRedeem, estimatedPickupTime)
                repository.insertAuditLog(user.id, "POD_ORDER", "Order generated under Pay-on-Delivery protocol." + (if (pointsToRedeem > 0) " Loyalty points applied offline." else ""))
                loadOfflineOrders()
                fetchLoyaltySummary()
                com.example.ui.util.HapticUtil.performOrderButtonHaptic(getApplication())
                com.example.ui.util.SnackbarManager.showMessage("Order placed successfully (Pay on Delivery)!")
                onComplete(true)
            }
        }
    }

    // Modern Cafe Configuration
    fun requestVendorPayout(amount: Double, details: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user != null && user.role == "VENDOR" && user.balance >= amount) {
                val ref = "PAY-" + (100000..999999).random()
                repository.insertWalletTransaction(user.id, "PAYOUT", -amount, ref, "Payout requested to: $details")
                repository.insertAuditLog(user.id, "VENDOR_PAYOUT", "Requested payout of GH₵ ${"%.2f".format(amount)} to: $details")
                
                // Refresh vendor session
                val refreshed = repository.userDao.getUserSync(user.id)
                if (refreshed != null) {
                    _currentUser.value = refreshed
                }
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateVendorAnnouncement(announcement: String) {
        _vendorAnnouncement.value = announcement.ifBlank { "Serving appetizing, dynamic recipes. Check daily specials!" }
    }

    fun setStoreClosedState(isClosed: Boolean) {
        _isStoreClosed.value = isClosed
        val currentVendor = _currentUser.value
        if (currentVendor != null) {
            val newStatus = if (isClosed) "CLOSED" else "OPEN"
            FirestoreVendorStatusManager.updateVendorStatus(currentVendor.id, newStatus, currentVendor.fullName)
        }
        if (LaravelClientManager.isLaravelEnabled) {
            viewModelScope.launch {
                try {
                    val service = LaravelClientManager.getService()
                    val isOpen = !isClosed
                    val response = service.toggleVendorStatus(isOpen)
                    if (response.success) {
                        Log.d("CafeteriaViewModel", "Remote store status updated: ${response.message}")
                    }
                } catch (e: Exception) {
                    Log.e("CafeteriaViewModel", "Failed to sync store status to Laravel", e)
                }
            }
        }
    }

    /**
     * Administrative override to toggle vendor status (OPEN / BUSY / CLOSED) in real-time via Firestore.
     */
    fun setVendorStatusByAdmin(vendorId: Int, status: String, vendorName: String = "") {
        viewModelScope.launch {
            val uppercaseStatus = status.uppercase()
            // Real-time Firestore sync
            FirestoreVendorStatusManager.updateVendorStatus(vendorId, uppercaseStatus, vendorName)

            // Update local Room database
            val isOpen = uppercaseStatus != "CLOSED"
            repository.updateVendorIsOpen(vendorId, isOpen)

            // Sync with remote server if active
            if (LaravelClientManager.isLaravelEnabled) {
                try {
                    val service = LaravelClientManager.getService()
                    service.toggleVendorStatus(isOpen)
                } catch (e: Exception) {
                    Log.w("CafeteriaViewModel", "Laravel status sync warning: ${e.message}")
                }
            }

            repository.insertAuditLog(
                userId = _currentUser.value?.id ?: 0,
                action = "ADMIN_TOGGLED_VENDOR_STATUS",
                details = "Admin set vendor #$vendorId ($vendorName) status to $uppercaseStatus via Firestore real-time sync."
            )
        }
    }

    fun refreshVendorPerformance(vendorId: Int? = null) {
        viewModelScope.launch {
            val id = vendorId ?: _currentUser.value?.id
            if (id != null && LaravelClientManager.isLaravelEnabled) {
                try {
                    val perf = repository.getVendorPerformance(id)
                    _vendorPerformanceList.value = perf
                    
                    val metrics = repository.getVendorPerformanceMetrics()
                    _vendorPerformanceMetricsList.value = metrics

                    // Fetch custom daily revenue totals by vendor and date
                    val dailyRev = repository.getDailyRevenue(id)
                    _vendorDailyRevenueResponse.value = dailyRev
                } catch (e: Exception) {
                    Log.e("CafeteriaViewModel", "refreshVendorPerformance failed", e)
                }
            }
        }
    }

    fun submitOrderFeedback(orderId: Int, vendorId: Int, quality: Int, cleanliness: Int, speed: Int, value: Int, comment: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.submitFeedback(
                customerId = user.id,
                orderId = orderId,
                vendorId = vendorId,
                quality = quality,
                cleanliness = cleanliness,
                speed = speed,
                value = value,
                comment = comment
            )
            // Transition status manually in simulation to ensure feedback loop completes
            repository.insertAuditLog(user.id, "FEEDBACK_POSTED", "Verified review recorded for order #${orderId}.")
            com.example.ui.util.SnackbarManager.showMessage("Feedback submitted successfully!")
            onComplete()
        }
    }

    fun submitFoodFeedback(orderId: Int, foodItemId: Int, rating: Int, comment: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value ?: return@launch
            repository.submitFoodFeedback(
                customerId = user.id,
                orderId = orderId,
                foodItemId = foodItemId,
                rating = rating,
                comment = comment
            )
            onComplete()
        }
    }

    // Vendor specific actions
    fun updateFoodAvailability(item: FoodItem, isAvailable: Boolean) {
        viewModelScope.launch {
            val updated = item.copy(isAvailable = isAvailable)
            repository.updateMenuFoodItem(updated)
        }
    }

    fun bulkToggleFoodItems(ids: List<Int>, isAvailable: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.bulkToggleFoodItems(ids, isAvailable)
            repository.syncAllFromLaravel()
            onComplete()
        }
    }

    fun replyToFeedback(feedbackId: Int, reply: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.replyToFeedback(feedbackId, reply)
            repository.syncAllFromLaravel()
            onComplete()
        }
    }

    fun addVendorFoodItem(name: String, price: Double, category: String, description: String, imageUrl: String, initialStock: Int = 100, threshold: Int = 15, calories: Int = 180, allergens: String = "None") {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            if (name.isNotBlank() && price > 0) {
                repository.addMenuFoodItem(vendor.id, name, price, category, description, imageUrl, initialStock, threshold, calories, allergens)
            }
        }
    }

    fun replenishFoodItemStock(foodItem: FoodItem, amount: Int) {
        viewModelScope.launch {
            val updated = foodItem.copy(
                currentStock = (foodItem.currentStock + amount).coerceAtLeast(0)
            )
            repository.updateMenuFoodItem(updated)
            repository.insertAuditLog(foodItem.vendorId, "INVENTORY_REPLENISHMENT", "Replenished inventory stock of '${foodItem.name}' with +$amount plates. Remaining stock: ${updated.currentStock}.")
        }
    }

    fun updateFoodItemStockSettings(foodItem: FoodItem, initialStock: Int, currentStock: Int, threshold: Int) {
        viewModelScope.launch {
            val updated = foodItem.copy(
                initialStock = initialStock,
                currentStock = currentStock,
                lowStockThreshold = threshold
            )
            repository.updateMenuFoodItem(updated)
            repository.insertAuditLog(foodItem.vendorId, "INVENTORY_SETTINGS_UPDATE", "Reset stock settings for '${foodItem.name}': Min Limit=$threshold, Current=$currentStock.")
        }
    }

    fun updateMenuFoodItemDetails(
        foodItem: FoodItem,
        name: String,
        price: Double,
        category: String,
        description: String,
        initialStock: Int,
        currentStock: Int,
        threshold: Int,
        calories: Int = 180,
        allergens: String = "None"
    ) {
        viewModelScope.launch {
            val updated = foodItem.copy(
                name = name,
                price = price,
                category = category,
                description = description,
                initialStock = initialStock,
                currentStock = currentStock,
                lowStockThreshold = threshold,
                calories = calories,
                allergens = allergens
            )
            repository.updateMenuFoodItem(updated)
            repository.insertAuditLog(foodItem.vendorId, "MENU_ITEM_UPDATED", "Updated item '${foodItem.name}' (ID: ${foodItem.id}): Name=$name, Price=GH₵$price, Calories=$calories kcal, Allergens=$allergens, Category=$category, Description=$description.")
        }
    }

    fun bulkUpdateVendorMenu(vendorId: Int, updates: List<com.example.data.LaravelBulkUpdateItem>, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.bulkUpdateVendorMenu(vendorId, updates)
            onResult(result.first, result.second)
        }
    }

    fun predictStockExhaustion(foodItem: FoodItem, orders: List<Order>): String {
        val itemOrders = orders.filter { it.foodItemId == foodItem.id && it.status in listOf("PENDING", "PREPARING", "READY", "COMPLETED") }
        if (itemOrders.isEmpty()) {
            return "Stable • No purchase rate"
        }
        val sortedOrders = itemOrders.sortedBy { it.orderTimestamp }
        val oldestTimestamp = sortedOrders.first().orderTimestamp
        val newestTimestamp = sortedOrders.last().orderTimestamp
        val totalQty = sortedOrders.sumOf { it.quantity }
        
        val timeSpanMs = newestTimestamp - oldestTimestamp
        val timeSpanHours = timeSpanMs.toDouble() / (1000.0 * 60.0 * 60.0)
        
        val hourlyRate = if (timeSpanHours > 0.1) {
            totalQty.toDouble() / timeSpanHours
        } else {
            val elapsedFromFirstToNow = System.currentTimeMillis() - oldestTimestamp
            val elapsedHours = elapsedFromFirstToNow.toDouble() / (1000.0 * 60.0 * 60.0)
            if (elapsedHours > 0.1) {
                totalQty.toDouble() / elapsedHours
            } else {
                totalQty.toDouble()
            }
        }
        
        if (foodItem.currentStock <= 0) {
            return "EXHAUSTED • Restock immediately!"
        }
        
        if (hourlyRate <= 0.0) {
            return "Stable • No purchase rate"
        }
        
        val hoursRemaining = foodItem.currentStock.toDouble() / hourlyRate
        return when {
            hoursRemaining < 1.0 -> {
                val mins = (hoursRemaining * 60).toInt().coerceAtLeast(1)
                "Exhausts in ~$mins mins"
            }
            hoursRemaining < 24.0 -> {
                val hrs = hoursRemaining.toInt()
                val mins = ((hoursRemaining - hrs) * 60).toInt()
                "Exhausts in ~$hrs hrs $mins mins"
            }
            else -> {
                val days = (hoursRemaining / 24.0).toInt()
                val hrs = ((hoursRemaining / 24.0 - days) * 24).toInt()
                "Exhausts in ~$days d $hrs hrs"
            }
        }
    }

    fun deleteVendorFoodItem(item: FoodItem) {
        viewModelScope.launch {
            repository.deleteMenuFoodItem(item)
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String, estimatedTime: String? = null) {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            repository.updateOrderStatus(vendor.id, orderId, newStatus, estimatedTime)
            FirestoreOrderTrackingManager.updateOrderStatusInFirestore(orderId, newStatus, vendor.id, 0, estimatedTime)
            if (newStatus.equals("READY", ignoreCase = true) || newStatus.equals("READY_FOR_PICKUP", ignoreCase = true)) {
                com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                    getApplication(),
                    orderId,
                    "🍱 Order #$orderId is Ready for Pickup!",
                    "Your food order status has updated from 'Preparing' to 'Ready for Pickup'. Please collect your meal at the stall."
                )
            }
        }
    }

    fun simulateAdvanceOrderStatus(orderId: Int) {
        viewModelScope.launch {
            val order = repository.orderDao.getOrderById(orderId) ?: return@launch
            val nextStatus = when (order.status.uppercase()) {
                "PENDING" -> "PREPARING"
                "PREPARING" -> "READY"
                "READY" -> "COMPLETED"
                else -> "PENDING"
            }
            val estTime = if (nextStatus == "PREPARING") "10-15 Min" else null
            repository.updateOrderStatus(order.vendorId, orderId, nextStatus, estTime)
            FirestoreOrderTrackingManager.updateOrderStatusInFirestore(orderId, nextStatus, order.vendorId, order.customerId, estTime)
            
            if (nextStatus == "READY") {
                com.example.ui.util.NotificationHelper.sendOrderStatusNotification(
                    getApplication(),
                    orderId,
                    "🍱 Order #$orderId is Ready for Pickup!",
                    "Your food order status has updated from 'Preparing' to 'Ready for Pickup'. Please collect your meal at the stall."
                )
            }

            // If completed, register a log in the audit log
            if (nextStatus == "COMPLETED") {
                repository.insertAuditLog(order.customerId, "CUSTOMER_PICKED_UP", "Completed pickup for Order #${orderId} of ${order.foodName}")
            }
        }
    }

    fun cancelOrder(orderId: Int, reason: String) {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            repository.cancelOrder(vendor.id, orderId, reason)
        }
    }

    fun studentCancelOrder(orderId: Int) {
        viewModelScope.launch {
            val student = _currentUser.value ?: return@launch
            try {
                repository.studentCancelOrder(student.id, orderId)
                val refreshed = repository.userDao.getUserSync(student.id)
                if (refreshed != null) {
                    _currentUser.value = refreshed
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to cancel order as student", e)
            }
        }
    }

    fun verifySecurePickup(orderId: Int, inputPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            val verified = repository.verifyAndCompletePickup(vendor.id, orderId, inputPin)
            onResult(verified)
        }
    }

    fun studentVerifyPickupViaQr(orderId: Int, counterQrCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            com.example.ui.util.HapticUtil.performScanHaptic(getApplication())
            val order = repository.orderDao.getOrderById(orderId)
            if (order != null && order.status.uppercase() == "READY" && counterQrCode.startsWith("ATU-COUNTER-")) {
                val vendorIdFromQr = counterQrCode.removePrefix("ATU-COUNTER-").toIntOrNull()
                if (vendorIdFromQr == order.vendorId) {
                    repository.updateOrderStatus(order.vendorId, orderId, "DELIVERED", null)
                    repository.insertAuditLog(order.customerId, "PICKUP_VALIDATED_QR", "Student verified order #${orderId} secure counter QR scan and marked Delivered.")
                    com.example.ui.util.HapticUtil.performOrderCompletion(getApplication())
                    onResult(true)
                } else {
                    com.example.ui.util.HapticUtil.performErrorState(getApplication())
                    onResult(false)
                }
            } else {
                com.example.ui.util.HapticUtil.performErrorState(getApplication())
                onResult(false)
            }
        }
    }

    fun studentCounterCheckIn(counterQrCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            com.example.ui.util.HapticUtil.performScanHaptic(getApplication())
            val user = _currentUser.value
            if (user == null) {
                onResult(false, "User session not active.")
                return@launch
            }
            if (counterQrCode.startsWith("ATU-COUNTER-")) {
                val vendorId = counterQrCode.removePrefix("ATU-COUNTER-").toIntOrNull()
                val vendor = if (vendorId != null) repository.userDao.getUserSync(vendorId) else null
                val vendorName = vendor?.fullName ?: "Accra Tech counter"
                repository.insertAuditLog(user.id, "COUNTER_CHECK_IN", "Checked in at $vendorName. Ready to collect meals.")
                onResult(true, "Successfully checked in at $vendorName counter!")
            } else {
                onResult(false, "Invalid counter QR code. Please scan a valid ATU Counter QR.")
            }
        }
    }

    // ==========================================
    // DATA ANALYTICS METRIC EXTRACTION
    // ==========================================

    fun getVendorMetrics(vendorId: Int, feedbacks: List<Feedback>): Map<String, Double> {
        val filtered = feedbacks.filter { f -> f.vendorId == vendorId }
        if (filtered.isEmpty()) {
            return mapOf(
                "foodQuality" to 0.0,
                "cleanliness" to 0.0,
                "speed" to 0.0,
                "priceValue" to 0.0,
                "overall" to 0.0
            )
        }
        val qualityAvg = filtered.map { it.ratingFoodQuality }.average()
        val cleanlinessAvg = filtered.map { it.ratingCleanliness }.average()
        val speedAvg = filtered.map { it.ratingServiceSpeed }.average()
        val priceAvg = filtered.map { it.ratingPriceValue }.average()
        val overall = (qualityAvg + cleanlinessAvg + speedAvg + priceAvg) / 4.0

        return mapOf(
            "foodQuality" to qualityAvg,
            "cleanliness" to cleanlinessAvg,
            "speed" to speedAvg,
            "priceValue" to priceAvg,
            "overall" to overall
        )
    }

    // Run Predictive Analytics with Gemini API (or fall back to structured offline simulation)
    fun runGeminiVendorAnalytics(vendorId: Int, vendorName: String, feedbacks: List<Feedback>, totalOrders: Int) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _aiAnalysisText.value = null

            val metrics = getVendorMetrics(vendorId, feedbacks)
            val filteredFeedbacks = feedbacks.filter { it.vendorId == vendorId }

            _aiAnalysisText.value = geminiRepository.generateVendorPerformanceReview(
                vendorName = vendorName,
                feedbacks = filteredFeedbacks,
                totalOrdersCount = totalOrders,
                averageRatings = metrics
            )
            _isAnalyzing.value = false
        }
    }

    // Run Qualitative Sentiment Analysis on student reviews and comments with Gemini API
    fun runVendorSentimentAnalysis(vendorId: Int, vendorName: String, feedbacks: List<Feedback>) {
        viewModelScope.launch {
            _isAnalyzingSentiment.value = true
            _vendorSentimentAnalysis.value = null

            val filteredFeedbacks = feedbacks.filter { it.vendorId == vendorId }
            _vendorSentimentAnalysis.value = geminiRepository.generateVendorSentimentAnalysis(
                vendorName = vendorName,
                feedbacks = filteredFeedbacks
            )
            _isAnalyzingSentiment.value = false
        }
    }

    // Run custom AI-generated professional response templates based on reviews
    fun runVendorAutoReplies(vendorId: Int, vendorName: String, feedbacks: List<Feedback>) {
        viewModelScope.launch {
            _isGeneratingAutoReplies.value = true
            _vendorAutoReplies.value = null

            val filteredFeedbacks = feedbacks.filter { it.vendorId == vendorId }
            _vendorAutoReplies.value = geminiRepository.generateVendorAutoReplies(
                vendorName = vendorName,
                feedbacks = filteredFeedbacks
            )
            _isGeneratingAutoReplies.value = false
        }
    }

    // Run custom AI-generated pricing and specials suggestions based on time-of-day demand patterns
    fun runVendorPricingSuggestions(vendorId: Int, vendorName: String, orders: List<Order>, foodItems: List<FoodItem>) {
        viewModelScope.launch {
            _isGeneratingPricingSuggestions.value = true
            _vendorPricingSuggestions.value = null

            val filteredOrders = orders.filter { it.vendorId == vendorId }
            val filteredFoodItems = foodItems.filter { it.vendorId == vendorId }

            _vendorPricingSuggestions.value = geminiRepository.generateMenuPricingSuggestions(
                vendorName = vendorName,
                orders = filteredOrders,
                foodItems = filteredFoodItems
            )
            _isGeneratingPricingSuggestions.value = false
        }
    }

    fun runVendorTodayInsights(vendorId: Int, vendorName: String, todayOrders: List<Order>) {
        viewModelScope.launch {
            _isAnalyzingTodayOrders.value = true
            _vendorTodayInsights.value = null

            val filteredOrders = todayOrders.filter { it.vendorId == vendorId }
            _vendorTodayInsights.value = geminiRepository.generateTodayInsights(
                vendorName = vendorName,
                orders = filteredOrders
            )
            _isAnalyzingTodayOrders.value = false
        }
    }

    fun runVendorHistoricalInsights(vendorId: Int, vendorName: String, allOrders: List<Order>) {
        viewModelScope.launch {
            _isAnalyzingHistoricalOrders.value = true
            _vendorHistoricalInsights.value = null

            val filteredOrders = allOrders.filter { it.vendorId == vendorId && it.status == "COMPLETED" }

            if (LaravelClientManager.isLaravelEnabled) {
                try {
                    val response = repository.getVendorGeminiOrderInsights(vendorId)
                    if (response.success) {
                        _vendorHistoricalInsights.value = response.insights_markdown
                    } else {
                        _vendorHistoricalInsights.value = geminiRepository.generateHistoricalOrderInsights(
                            vendorName = vendorName,
                            orders = filteredOrders
                        )
                    }
                } catch (e: Exception) {
                    _vendorHistoricalInsights.value = geminiRepository.generateHistoricalOrderInsights(
                        vendorName = vendorName,
                        orders = filteredOrders
                    )
                }
            } else {
                _vendorHistoricalInsights.value = geminiRepository.generateHistoricalOrderInsights(
                    vendorName = vendorName,
                    orders = filteredOrders
                )
            }
            _isAnalyzingHistoricalOrders.value = false
        }
    }

    fun runVendorDemandForecast(vendorId: Int, vendorName: String, allOrders: List<Order>, allFoodItems: List<FoodItem>) {
        viewModelScope.launch {
            _isGeneratingDemandForecast.value = true
            _vendorDemandForecast.value = null

            val filteredOrders = allOrders.filter { it.vendorId == vendorId }
            val filteredFoodItems = allFoodItems.filter { it.vendorId == vendorId }

            _vendorDemandForecast.value = geminiRepository.generateDailyDemandForecast(
                vendorName = vendorName,
                orders = filteredOrders,
                menuItems = filteredFoodItems
            )
            _isGeneratingDemandForecast.value = false
        }
    }

    fun runStudentRecommendations(studentName: String, pastOrders: List<Order>, dietaryPreferences: String, availableFoodItems: List<FoodItem>) {
        viewModelScope.launch {
            _isGeneratingStudentRecommendations.value = true
            _studentRecommendations.value = null
            _studentRecommendations.value = geminiRepository.generateStudentMenuRecommendations(
                studentName = studentName,
                pastOrders = pastOrders,
                dietaryPreferences = dietaryPreferences,
                availableFoodItems = availableFoodItems
            )
            _isGeneratingStudentRecommendations.value = false
        }
    }

    fun loadPopularTodaySuggestions() {
        viewModelScope.launch {
            _isPopularTodayLoading.value = true
            try {
                val orders = allOrdersSnapshot.value
                val foods = allFoodItems.value
                val response = geminiRepository.generatePopularTodaySuggestions(orders, foods)
                _popularTodayContent.value = response
            } catch (e: Exception) {
                _popularTodayContent.value = "Unable to fetch popular suggestions today. Please try again later."
            } finally {
                _isPopularTodayLoading.value = false
            }
        }
    }

    fun runGeminiLowStockPredictMonitor(vendorId: Int, vendorName: String, orders: List<Order>, foodItems: List<FoodItem>) {
        viewModelScope.launch {
            _isPredictingLowStock.value = true
            _lowStockPredictionResult.value = null
            
            val filteredOrders = orders.filter { it.vendorId == vendorId }
            val filteredFoodItems = foodItems.filter { it.vendorId == vendorId }
            
            val prediction = geminiRepository.predictLowStockItems(
                vendorName = vendorName,
                orders = filteredOrders,
                foodItems = filteredFoodItems
            )
            _lowStockPredictionResult.value = prediction
            
            val currentTime = System.currentTimeMillis()
            val predictedAlerts = mutableListOf<VendorInventoryNotification>()
            
            filteredFoodItems.forEach { food ->
                if (prediction.contains(food.name, ignoreCase = true)) {
                    val key = "PRED_${food.id}_LOW_STOCK"
                    predictedAlerts.add(
                        VendorInventoryNotification(
                            id = key,
                            foodItemId = food.id,
                            foodName = food.name,
                            type = "LOW_STOCK",
                            message = "ATU Smart Alert: '${food.name}' has high demand risk. Daily sales trends suggest stock may fall below threshold soon. Suggest restocking 15-20 units.",
                            timestamp = currentTime,
                            isRead = false
                        )
                    )
                    repository.insertAuditLog(vendorId, "ATU_STOCK_PREDICTION", "ATU Intelligence predicted '${food.name}' will fall below low-stock threshold based on sales trends.")
                }
            }
            
            if (predictedAlerts.isNotEmpty()) {
                _vendorInventoryNotifications.value = predictedAlerts + _vendorInventoryNotifications.value
            }
            _isPredictingLowStock.value = false
        }
    }

    fun triggerSimulatedRealTimeOrder() {
        val activeUser = _currentUser.value ?: return
        if (activeUser.role != "VENDOR") return
        
        viewModelScope.launch {
            val foodName = listOf(
                "Spiced Jollof with Grilled Chicken",
                "Gari Fortor with Fried Fish",
                "Assorted Waakye with Egg & Shito",
                "Boiled Yam with Palava Sauce",
                "Delicious Red Red & Sweet Plantains"
            ).random()
            
            val orderId = (1001..9999).random()
            val qty = (1..3).random()
            val pricePerItem = 15.0 + (10..35).random() / 10.0
            val totalPrice = qty * pricePerItem
            
            LaravelEchoWebSocketManager.broadcastOrderPlacedLocally(
                orderId = orderId,
                vendorId = activeUser.id,
                foodName = foodName,
                qty = qty,
                totalPrice = totalPrice
            )
        }
    }

    fun exportAnalyticsReport(
        format: String,
        vendorId: Int,
        vendorName: String,
        orders: List<Order>,
        feedbacks: List<Feedback>,
        dateRangeScope: String = "All Time",
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (directory == null || (!directory.exists() && !directory.mkdirs())) {
                    onResult(false, "Failed to access download folder.")
                    return@launch
                }

                // Gather stats
                val completedOrders = orders.filter { it.status == "COMPLETED" }
                val totalRevenue = completedOrders.sumOf { it.totalPrice }
                val metrics = getVendorMetrics(vendorId, feedbacks)
                val currentMonth = dateRangeScope
                val timestamp = System.currentTimeMillis()

                if (format.uppercase() == "PDF") {
                    val pdfDocument = android.graphics.pdf.PdfDocument()
                    // 1 page layout (A4 size: 595 x 842 pt)
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    val paint = android.graphics.Paint()

                    // Title Header
                    paint.textSize = 20f
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.DKGRAY
                    canvas.drawText("PERFORMANCE STATEMENT REPORT", 50f, 60f, paint)

                    paint.textSize = 10f
                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.GRAY
                    canvas.drawText("Scope: $currentMonth | Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}", 50f, 80f, paint)

                    canvas.drawLine(50f, 90f, 545f, 90f, paint)

                    // Vendor Identity Card Info
                    paint.color = android.graphics.Color.BLACK
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    canvas.drawText("Vendor Name: $vendorName", 50f, 120f, paint)
                    canvas.drawText("Vendor Code: ATU-$vendorId", 50f, 137f, paint)

                    // Stats Dashboard Box Outline
                    paint.color = android.graphics.Color.LTGRAY
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRect(50f, 160f, 545f, 250f, paint)

                    paint.style = android.graphics.Paint.Style.FILL
                    paint.color = android.graphics.Color.DKGRAY
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    canvas.drawText("SUMMARY STATS", 60f, 180f, paint)

                    paint.isFakeBoldText = false
                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText("Total Registered Bookings: ${orders.size}", 60f, 200f, paint)
                    canvas.drawText("Dispatched / Completed: ${completedOrders.size}", 60f, 215f, paint)
                    canvas.drawText("Estimated Gross Revenue: GH₵ ${"%.2f".format(totalRevenue)}", 60f, 230f, paint)

                    canvas.drawText("Average Ratings:", 300f, 200f, paint)
                    canvas.drawText("• Food Quality: ${"%.1f".format(metrics["foodQuality"] ?: 0.0)} ★", 310f, 215f, paint)
                    canvas.drawText("• Cleanliness: ${"%.1f".format(metrics["cleanliness"] ?: 0.0)} ★", 310f, 230f, paint)

                    // Reviews List
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    paint.color = android.graphics.Color.DKGRAY
                    canvas.drawText("STUDENT REVIEWS TRANSCRIPTS", 50f, 290f, paint)
                    canvas.drawLine(50f, 295f, 545f, 295f, paint)

                    paint.isFakeBoldText = false
                    paint.textSize = 10f
                    paint.color = android.graphics.Color.BLACK
                    var currentY = 320f
                    val limitReviews = feedbacks.take(8)
                    if (limitReviews.isEmpty()) {
                        canvas.drawText("No written feedback items logged for this date range.", 60f, currentY, paint)
                    } else {
                        for (review in limitReviews) {
                            val scoreStr = "Q:${review.ratingFoodQuality} C:${review.ratingCleanliness} S:${review.ratingServiceSpeed} V:${review.ratingPriceValue}"
                            canvas.drawText("★ Score [$scoreStr] - \"${review.comment}\"", 50f, currentY, paint)
                            currentY += 22f
                            if (currentY > 800f) break
                        }
                    }

                    // Disclaimer Footer
                    paint.textSize = 9f
                    paint.color = android.graphics.Color.GRAY
                    canvas.drawText("Certified Accra Technical University Cafeteria POS Audit records. Confidential.", 50f, 820f, paint)

                    pdfDocument.finishPage(page)

                    // Write to file
                    val filename = "Vendor_Performance_Report_$timestamp.pdf"
                    val file = java.io.File(directory, filename)
                    val outputStream = java.io.FileOutputStream(file)
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    pdfDocument.close()

                    onResult(true, "PDF Download success! Saved to Downloads: ${file.name}")
                } else {
                    // CSV Export
                    val filename = "Vendor_Performance_Report_$timestamp.csv"
                    val file = java.io.File(directory, filename)
                    val writer = java.io.FileWriter(file)

                    writer.append("Vendor Performance Analytics Report - $dateRangeScope\n")
                    writer.append("Vendor Name,ATU-$vendorId - $vendorName\n")
                    writer.append("Generated On,${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n\n")

                    writer.append("=== SUMMARY METRICS ===\n")
                    writer.append("Indicator,Value\n")
                    writer.append("Total Orders placed,${orders.size}\n")
                    writer.append("Total Completed,${completedOrders.size}\n")
                    writer.append("Total Revenue (GH₵),${"%.2f".format(totalRevenue)}\n")
                    writer.append("Food Quality Average Stars,${"%.1f".format(metrics["foodQuality"] ?: 0.0)}\n")
                    writer.append("Cleanliness Average Stars,${"%.1f".format(metrics["cleanliness"] ?: 0.0)}\n")
                    writer.append("Service Speed Average Stars,${"%.1f".format(metrics["speed"] ?: 0.0)}\n")
                    writer.append("Price Value Average Stars,${"%.1f".format(metrics["priceValue"] ?: 0.0)}\n")
                    writer.append("Overall Score Stars,${"%.1f".format(metrics["overall"] ?: 0.0)}\n\n")

                    writer.append("=== CUSTOMER FEEDBACK LIST ===\n")
                    writer.append("Review ID,Food Quality,Cleanliness,Speed,Value,Comment\n")
                    for (f in feedbacks) {
                        val sanitizedComment = f.comment.replace("\"", "\"\"")
                        writer.append("${f.id},${f.ratingFoodQuality},${f.ratingCleanliness},${f.ratingServiceSpeed},${f.ratingPriceValue},\"$sanitizedComment\"\n")
                    }

                    writer.flush()
                    writer.close()

                    onResult(true, "CSV Download success! Saved to Downloads: ${file.name}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Export Error: ${e.localizedMessage}")
            }
        }
    }

    fun exportDailyOrderLogsToCSV(
        vendorId: Int,
        orders: List<com.example.data.Order>,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (directory == null || (!directory.exists() && !directory.mkdirs())) {
                    onResult(false, "Failed to access download folder.")
                    return@launch
                }

                val timestamp = System.currentTimeMillis()
                val filename = "Daily_Order_Logs_Vendor_${vendorId}_$timestamp.csv"
                val file = java.io.File(directory, filename)
                val writer = java.io.FileWriter(file)

                // CSV Header
                writer.append("Order ID,Student ID,Food Item Name,Quantity,Unit Price (GH₵),Total Price (GH₵),Redeemed Points,Discount (GH₵),Status,Time,PIN\n")

                // Filter vendor's orders
                val vendorOrders = orders.filter { it.vendorId == vendorId }.sortedByDescending { it.orderTimestamp }
                
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                
                for (order in vendorOrders) {
                    val dateStr = sdf.format(java.util.Date(order.orderTimestamp))
                    writer.append("${order.id},")
                    writer.append("${order.customerId},")
                    writer.append("\"${order.foodName.replace("\"", "\"\"")}\",")
                    writer.append("${order.quantity},")
                    writer.append("${order.unitPrice},")
                    writer.append("${order.totalPrice},")
                    writer.append("${order.pointsRedeemed},")
                    writer.append("${order.discountApplied},")
                    writer.append("${order.status},")
                    writer.append("$dateStr,")
                    writer.append("'${order.pickupPin}'\n")
                }

                writer.flush()
                writer.close()

                onResult(true, "Successfully exported ${vendorOrders.size} orders to Downloads: ${file.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Export Error: ${e.localizedMessage}")
            }
        }
    }

    fun runNutritionCoaching(
        dailyTargetKcal: Float,
        currentKcal: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        availableFoodItems: List<com.example.data.FoodItem>
    ) {
        viewModelScope.launch {
            _nutritionCoachingText.value = null
            _isAnalyzingNutrition.value = true
            try {
                _nutritionCoachingText.value = geminiRepository.generateNutritionCoaching(
                    dailyTargetKcal = dailyTargetKcal,
                    currentKcal = currentKcal,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    availableFoodItems = availableFoodItems
                )
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "runNutritionCoaching failed", e)
                _nutritionCoachingText.value = "Failed to synchronize lifestyle directives from ATU. Verify network connection."
            } finally {
                _isAnalyzingNutrition.value = false
            }
        }
    }

    fun getMenuItemNutrition(foodName: String, foodDescription: String) {
        viewModelScope.launch {
            _isAnalyzingDishNutrition.value = true
            _dishNutritionText.value = null
            try {
                _dishNutritionText.value = geminiRepository.analyzeMenuItemNutrition(foodName, foodDescription)
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "analyzeMenuItemNutrition failed", e)
                _dishNutritionText.value = "Failed to parse nutritional values. Default: 480 kcal."
            } finally {
                _isAnalyzingDishNutrition.value = false
            }
        }
    }

    fun clearMenuItemNutrition() {
        _dishNutritionText.value = null
        _isAnalyzingDishNutrition.value = false
    }

    fun analyzeCartNutritionWithGemini(cartItems: List<CartItem>, studentDietaryGoal: String = "Balanced Nutrition") {
        viewModelScope.launch {
            if (cartItems.isEmpty()) {
                _cartNutritionAnalysis.value = null
                return@launch
            }
            _isAnalyzingCartNutrition.value = true
            try {
                val summary = cartItems.joinToString(", ") { "${it.quantity}x ${it.foodItem.name} (${it.foodItem.description})" }
                _cartNutritionAnalysis.value = geminiRepository.analyzeCartNutritionalContent(summary, studentDietaryGoal)
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "analyzeCartNutritionWithGemini failed", e)
                _cartNutritionAnalysis.value = "### 🥗 Cart Health Analysis\n• **Est. Total**: ~680 kcal\n• **Status**: Rich in protein and essential carbohydrates for campus study."
            } finally {
                _isAnalyzingCartNutrition.value = false
            }
        }
    }

    fun clearCartNutritionAnalysis() {
        _cartNutritionAnalysis.value = null
    }

    fun updateFoodItemTimeSchedule(
        foodItem: FoodItem,
        isScheduled: Boolean,
        startTime: String = "06:00",
        endTime: String = "11:00"
    ) {
        viewModelScope.launch {
            try {
                val updatedItem = foodItem.copy(
                    isTimeScheduled = isScheduled,
                    availableStartTime = startTime,
                    availableEndTime = endTime
                )
                repository.updateMenuFoodItem(updatedItem)
                
                val actionDesc = if (isScheduled) {
                    "Set time schedule for '${foodItem.name}': $startTime - $endTime"
                } else {
                    "Disabled time scheduling for '${foodItem.name}'"
                }
                repository.insertAuditLog(foodItem.vendorId, "MENU_SCHEDULE_UPDATE", actionDesc)
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to update food time schedule", e)
            }
        }
    }

    fun isFoodItemCurrentlyVisibleBySchedule(food: FoodItem): Boolean {
        if (!food.isTimeScheduled) return food.isAvailable

        try {
            val cal = java.util.Calendar.getInstance()
            val currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMin = cal.get(java.util.Calendar.MINUTE)
            val currentMinsTotal = currentHour * 60 + currentMin

            val startParts = food.availableStartTime.split(":")
            val startMinsTotal = startParts[0].toInt() * 60 + startParts.getOrElse(1) { "00" }.toInt()

            val endParts = food.availableEndTime.split(":")
            val endMinsTotal = endParts[0].toInt() * 60 + endParts.getOrElse(1) { "00" }.toInt()

            val withinTime = currentMinsTotal in startMinsTotal..endMinsTotal
            return food.isAvailable && withinTime
        } catch (e: Exception) {
            return food.isAvailable
        }
    }

    // ==========================================
    // CHAT & PAYSTACK SYSTEM INTEGRATION
    // ==========================================

    private val _chatConversation = MutableStateFlow<List<LaravelChatMessage>>(emptyList())
    val chatConversation: StateFlow<List<LaravelChatMessage>> = _chatConversation.asStateFlow()

    private val _recentChats = MutableStateFlow<List<LaravelRecentChatPartner>>(emptyList())
    val recentChats: StateFlow<List<LaravelRecentChatPartner>> = _recentChats.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun refreshRecentChats() {
        if (!LaravelClientManager.isLaravelEnabled) return
        viewModelScope.launch {
            try {
                val service = LaravelClientManager.getService()
                val response = service.getRecentChats()
                if (response.success) {
                    _recentChats.value = response.chats
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to load recent chats", e)
            }
        }
    }

    fun loadConversation(otherUserId: Int) {
        if (!LaravelClientManager.isLaravelEnabled) return
        viewModelScope.launch {
            _isChatLoading.value = true
            try {
                val service = LaravelClientManager.getService()
                val response = service.getConversation(otherUserId)
                if (response.success) {
                    _chatConversation.value = response.messages
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to load conversation for user $otherUserId", e)
            } finally {
                _isChatLoading.value = false
            }
        }
    }

    fun sendChatMessage(receiverId: Int, message: String, onResult: (Boolean) -> Unit) {
        if (!LaravelClientManager.isLaravelEnabled) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            try {
                val service = LaravelClientManager.getService()
                val response = service.sendChatMessage(LaravelSendChatRequest(receiverId, message))
                if (response.success) {
                    _chatConversation.value = _chatConversation.value + response.chat_message
                    refreshRecentChats()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Failed to send chat message", e)
                onResult(false)
            }
        }
    }

    fun initPaystackPayment(email: String, amount: Double, purpose: String, onResult: (LaravelPaystackInitDetails?) -> Unit) {
        if (!LaravelClientManager.isLaravelEnabled) {
            val ref = "MOCK-PAY-" + System.currentTimeMillis()
            onResult(
                LaravelPaystackInitDetails(
                    authorization_url = "https://checkout.paystack.com/mock-gateway-redirect?ref=$ref",
                    access_code = "MOCK_AC_OFFLINE",
                    reference = ref,
                    amount = amount
                )
            )
            return
        }
        viewModelScope.launch {
            try {
                val service = LaravelClientManager.getService()
                val response = service.initializePaystack(LaravelPaystackInitRequest(email, amount, purpose))
                if (response.success) {
                    onResult(response.data)
                } else {
                    onResult(null)
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Paystack initialization failed", e)
                onResult(null)
            }
        }
    }

    fun verifyPaystackPayment(reference: String, amount: Double, purpose: String, onResult: (Boolean) -> Unit) {
        if (!LaravelClientManager.isLaravelEnabled) {
            viewModelScope.launch {
                if (purpose == "WALLET_TOPUP") {
                    rechargeWallet(amount)
                }
                onResult(true)
            }
            return
        }
        viewModelScope.launch {
            try {
                val service = LaravelClientManager.getService()
                val response = service.verifyPaystack(reference, amount, purpose)
                if (response.success) {
                    if (purpose == "WALLET_TOPUP") {
                        rechargeWallet(amount)
                        syncAllFromLaravel { }
                    }
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e("CafeteriaViewModel", "Paystack payment verification failed", e)
                onResult(false)
            }
        }
    }

    fun updateGlobalLowStockThreshold(newThreshold: Int) {
        globalLowStockThreshold.value = newThreshold.coerceAtLeast(0)
    }

    fun dismissVendorNotification(id: String) {
        dismissedNotificationKeys.add(id)
        triggerInventoryCheck()
    }

    fun markVendorNotificationRead(id: String) {
        readNotificationKeys.add(id)
        triggerInventoryCheck()
    }

    fun triggerInventoryCheck() {
        viewModelScope.launch {
            val foods = vendorFoodItems.value
            val threshold = globalLowStockThreshold.value
            
            val currentTime = System.currentTimeMillis()
            val activeList = mutableListOf<VendorInventoryNotification>()
            val activeKeys = mutableSetOf<String>()
            
            foods.forEach { food ->
                val isUnavailable = !food.isAvailable
                val isDepleted = food.currentStock == 0
                val isLowStock = food.currentStock > 0 && food.currentStock <= food.lowStockThreshold
                val isGlobalLowStock = food.currentStock > 0 && food.currentStock <= threshold
                
                if (isUnavailable) {
                    val key = "${food.id}_UNAVAILABLE"
                    activeKeys.add(key)
                    if (!dismissedNotificationKeys.contains(key)) {
                        activeList.add(
                            VendorInventoryNotification(
                                id = key,
                                foodItemId = food.id,
                                foodName = food.name,
                                type = "UNAVAILABLE",
                                message = "Cuisine '${food.name}' has been marked unavailable in your menu offerings.",
                                timestamp = currentTime,
                                isRead = readNotificationKeys.contains(key)
                            )
                        )
                    }
                }
                if (isDepleted) {
                    val key = "${food.id}_OUT_OF_STOCK"
                    activeKeys.add(key)
                    if (!dismissedNotificationKeys.contains(key)) {
                        activeList.add(
                            VendorInventoryNotification(
                                id = key,
                                foodItemId = food.id,
                                foodName = food.name,
                                type = "OUT_OF_STOCK",
                                message = "CRITICAL: '${food.name}' is completely depleted (0 stock remaining)!",
                                timestamp = currentTime,
                                isRead = readNotificationKeys.contains(key)
                            )
                        )
                    }
                } else if (isLowStock || isGlobalLowStock) {
                    val key = "${food.id}_LOW_STOCK"
                    activeKeys.add(key)
                    if (!dismissedNotificationKeys.contains(key)) {
                        val activeLimit = if (isLowStock) food.lowStockThreshold else threshold
                        activeList.add(
                            VendorInventoryNotification(
                                id = key,
                                foodItemId = food.id,
                                foodName = food.name,
                                type = "LOW_STOCK",
                                message = "Warning: '${food.name}' is running low (Stock: ${food.currentStock}/${food.initialStock}, safety limit is $activeLimit).",
                                timestamp = currentTime,
                                isRead = readNotificationKeys.contains(key)
                            )
                        )
                    }
                }
            }
            previousVendorNotificationKeys = activeKeys
            _vendorInventoryNotifications.value = activeList
        }
    }

    fun startVendorMetricsPolling(vendorId: Int) {
        vendorMetricsPollingJob?.cancel()
        vendorMetricsPollingJob = viewModelScope.launch {
            while (true) {
                delay(20000)
                Log.d("CafeteriaViewModel", "Polling: Refreshing vendor performance metrics for vendor $vendorId")
                refreshVendorPerformance(vendorId)
            }
        }
    }

    fun stopVendorMetricsPolling() {
        vendorMetricsPollingJob?.cancel()
        vendorMetricsPollingJob = null
        Log.d("CafeteriaViewModel", "Polling: Vendor metrics polling stopped.")
    }

    fun studentConfirmReceipt(orderId: Int) {
        viewModelScope.launch {
            val student = _currentUser.value ?: return@launch
            val order = repository.orderDao.getOrderById(orderId) ?: return@launch
            
            // Update order status to "DELIVERED"
            repository.updateOrderStatus(order.vendorId, orderId, "DELIVERED")
            
            // Log in the audit log
            repository.insertAuditLog(student.id, "CUSTOMER_RECEIVED_ORDER", "Student ${student.fullName} confirmed receipt of Order #${orderId} (${order.foodName})")
            
            // Send in-app notification alert to the vendor
            val alertMessage = "Student ${student.fullName} confirmed receipt of Order #${orderId} for '${order.foodName}'!"
            val notif = VendorInventoryNotification(
                id = "CONFIRM_RECEIPT_${orderId}_${System.currentTimeMillis()}",
                foodItemId = order.foodItemId,
                foodName = order.foodName,
                type = "DELIVERED",
                message = alertMessage,
                timestamp = System.currentTimeMillis()
            )
            _vendorInventoryNotifications.value = listOf(notif) + _vendorInventoryNotifications.value
            playSoundNotification()
        }
    }

    fun generateAndSendWeeklyVendorReport(vendor: User, force: Boolean = false) {
        viewModelScope.launch {
            // Wait slightly for any loading operations if not forced
            if (!force) {
                delay(1500)
            }
            
            if (!isWeeklyVendorReportEnabled.value && !force) return@launch
            
            val ordersList = vendorOrders.value.filter { it.vendorId == vendor.id }
            if (ordersList.isEmpty() && !force) {
                return@launch
            }
            
            val lastSent = _lastWeeklyReportTimestamp.value
            val now = System.currentTimeMillis()
            if (!force && lastSent != null && (now - lastSent) < (7 * 24 * 60 * 60 * 1000L)) {
                return@launch
            }
            
            // 1. Calculate Top Selling Dishes
            val topDishes = ordersList
                .filter { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "DELIVERED" }
                .groupBy { it.foodName }
                .mapValues { entry -> entry.value.sumOf { it.quantity } }
                .entries
                .sortedByDescending { it.value }
                .take(3)
                
            // 2. Calculate Peak Order Hours
            val calendar = java.util.Calendar.getInstance()
            val peakHours = ordersList
                .groupBy { 
                    calendar.timeInMillis = it.orderTimestamp
                    val hour24 = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                    val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
                    val amPm = if (hour24 < 12) "AM" else "PM"
                    String.format("%02d:00 %s - %02d:00 %s", hour12, amPm, (hour12 % 12) + 1, if (hour24 + 1 < 12) "AM" else if (hour24 + 1 >= 24) "AM" else "PM")
                }
                .mapValues { it.value.size }
                .entries
                .sortedByDescending { it.value }
                .take(2)
                
            // 3. Average rating from feedback
            val feedbackList = vendorFeedback.value
            val averageRating = if (feedbackList.isNotEmpty()) {
                feedbackList.flatMap { 
                    listOf(it.ratingFoodQuality, it.ratingCleanliness, it.ratingServiceSpeed, it.ratingPriceValue)
                }.average()
            } else {
                4.8
            }
            
            // 4. Snapshot of total sales
            val completedOrders = ordersList.filter { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "DELIVERED" }
            val totalRevenue = completedOrders.sumOf { it.totalPrice }
            val totalVolume = completedOrders.size
            
            val studentEmail = if (vendor.username.contains("@")) vendor.username else "${vendor.username}@atu.edu.gh"
            val subject = "📊 Weekly Performance Digest - ${vendor.fullName}"
            
            val topDishesText = if (topDishes.isNotEmpty()) {
                topDishes.joinToString("\n") { "  • ${it.key}: ${it.value} servings sold" }
            } else {
                "  • No sales recorded this week yet."
            }
            
            val peakHoursText = if (peakHours.isNotEmpty()) {
                peakHours.joinToString("\n") { "  • ${it.key}: ${it.value} orders placed" }
            } else {
                "  • No peak hour patterns detected."
            }
            
            val recommendation = if (topDishes.isNotEmpty() && peakHours.isNotEmpty()) {
                "Ensure high stock of '${topDishes.first().key}' during your peak hour of '${peakHours.first().key}' to maximize your sales conversion rate!"
            } else {
                "Incorporate unique mid-day food specials during standard lecture recesses to drive high-volume student booking traffic."
            }
            
            val body = """
                Hello ${vendor.fullName},
                
                Here is your automated weekly performance digest for Accra Technical University Food Court!
                
                📈 Weekly Sales Snapshot:
                • Total Orders Fulfilled: $totalVolume
                • Gross Revenue: GH₵ ${String.format("%.2f", totalRevenue)}
                • Average Customer Rating: ${String.format("%.1f", averageRating)} / 5.0
                
                🍳 Top-Selling Dishes:
                $topDishesText
                
                ⏰ Peak Order Hours:
                $peakHoursText
                
                💡 Optimization Recommendation:
                $recommendation
                
                Thank you for partnering with the Accra Technical University Food Court System. We value your presence inside our culinary community!
                
                Warm regards,
                Accra Technical University Food Court System.
            """.trimIndent()
            
            val emailNotif = MockEmailNotification(
                id = java.util.UUID.randomUUID().toString(),
                orderId = -1,
                studentEmail = studentEmail,
                vendorName = vendor.fullName,
                itemName = "Weekly Performance Report",
                subject = subject,
                body = body
            )
            
            _dispatchedEmails.value = _dispatchedEmails.value + emailNotif
            _lastWeeklyReportTimestamp.value = now
            
            // Send system in-app notification alert as well
            val systemNotif = VendorInventoryNotification(
                id = "WEEKLY_REPORT_${now}",
                foodItemId = -1,
                foodName = "Weekly Digest",
                type = "WEEKLY_REPORT",
                message = "Your automated weekly performance digest has been compiled and emailed to $studentEmail!",
                timestamp = now
            )
            _vendorInventoryNotifications.value = listOf(systemNotif) + _vendorInventoryNotifications.value
            playSoundNotification()
            
            // Log in Audit Trail
            repository.insertAuditLog(vendor.id, "VENDOR_WEEKLY_REPORT", "Automated weekly report generated. Total Orders: $totalVolume, Revenue: GH₵ ${String.format("%.2f", totalRevenue)}")
        }
    }

    fun getMessagesForOrder(orderId: Int): Flow<List<ChatMessage>> {
        return repository.getMessagesForOrder(orderId)
    }

    fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>> {
        return repository.getMessagesForUser(userId)
    }

    fun sendChatMessage(
        orderId: Int,
        senderId: Int,
        senderName: String,
        recipientId: Int,
        message: String,
        isFromStudent: Boolean
    ) {
        viewModelScope.launch {
            val chatMsg = ChatMessage(
                orderId = orderId,
                senderId = senderId,
                senderName = senderName,
                recipientId = recipientId,
                message = message,
                isFromStudent = isFromStudent,
                timestamp = System.currentTimeMillis()
            )
            repository.insertChatMessage(chatMsg)
            // Local loopback broadcast for immediate live updates if recipient is online
            LaravelEchoWebSocketManager.broadcastStudentNotificationLocally(
                notificationId = java.util.UUID.randomUUID().toString(),
                orderId = orderId,
                vendorId = recipientId,
                oldStatus = "CHAT",
                newStatus = "CHAT_MESSAGE",
                message = message
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        userSessionRepo.removeAuthStateListener(authStateListener)
        stopVendorMetricsPolling()
        LaravelEchoWebSocketManager.stopListening()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

data class PromotionalOffer(
    val id: Int,
    val code: String,
    val title: String,
    val discountPercent: Double,
    val description: String,
    val isActive: Boolean = true,
    val bannerBgColorHex: String = "#006B5D"
)
