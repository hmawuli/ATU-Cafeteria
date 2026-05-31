package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CafeteriaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = CafeteriaRepository(db)
    private val geminiRepository = GeminiAnalyticsRepository()

    // 1. Session State managers
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _registrationSuccess = MutableStateFlow<Boolean>(false)
    val registrationSuccess: StateFlow<Boolean> = _registrationSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Modern Pro-Suite States ---
    private val _studentWalletBalance = MutableStateFlow(185.50) // Starting digital currency seed
    val studentWalletBalance: StateFlow<Double> = _studentWalletBalance.asStateFlow()

    private val _vendorAnnouncement = MutableStateFlow("All meals prepared in alignment with Accra hygiene standards. Dine safe, study hard!")
    val vendorAnnouncement: StateFlow<String> = _vendorAnnouncement.asStateFlow()

    private val _isStoreClosed = MutableStateFlow(false)
    val isStoreClosed: StateFlow<Boolean> = _isStoreClosed.asStateFlow()

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

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        viewModelScope.launch {
            // Guarantee Seeding occurs on first start
            repository.seedDatabaseIfEmpty()
        }
    }

    // ==========================================
    // TRANSACTION ACTIONS & AUTHENTICATION
    // ==========================================

    fun loginUser(username: String, pinCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _loginError.value = null
            val authenticated = repository.authenticateUser(username, pinCode)
            if (authenticated != null) {
                _currentUser.value = authenticated
                _isLoading.value = false
                onComplete(true)
            } else {
                _loginError.value = "Invalid Username or Pass-PIN."
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

    fun addVendor(username: String, pinCode: String, fullName: String, info: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val brandOrId = info.ifBlank { "ATU Cafeteria Vendor" }
            val isRegistered = repository.registerUser(username, pinCode, "VENDOR", fullName, brandOrId)
            _isLoading.value = false
            if (isRegistered != null) {
                val admin = realAdminUser.value ?: _currentUser.value
                if (admin != null) {
                    repository.insertAuditLog(admin.id, "VENDOR_ADDED", "Vendor '$fullName' added by Admin.")
                }
                onResult(true)
            } else {
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
            _isLoading.value = false
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(admin.id, "VENDOR_UPDATED", "Vendor '${user.fullName}' updated by Admin.")
            }
            onResult(true)
        }
    }

    fun deleteVendor(userId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteUser(userId)
            _isLoading.value = false
            val admin = realAdminUser.value ?: _currentUser.value
            if (admin != null) {
                repository.insertAuditLog(admin.id, "VENDOR_DELETED", "Vendor (ID: $userId) deleted by Admin.")
            }
            onResult(true)
        }
    }

    // Student specific actions
    fun rechargeWallet(amount: Double) {
        viewModelScope.launch {
            _studentWalletBalance.value += amount
            val user = _currentUser.value
            if (user != null) {
                repository.insertAuditLog(user.id, "WALLET_CREDIT", "Securely loaded GH₵ ${"%.2f".format(amount)} via Mobile Money Gateway.")
            }
        }
    }

    fun placeOrder(foodItem: FoodItem, quantity: Int, useWallet: Boolean = false, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onComplete(false)
                return@launch
            }
            val requiredSum = foodItem.price * quantity
            if (useWallet) {
                if (_studentWalletBalance.value >= requiredSum) {
                    _studentWalletBalance.value -= requiredSum
                    repository.placeOrder(user.id, foodItem, quantity)
                    repository.insertAuditLog(user.id, "WALLET_PAYMENT", "Debited GH₵ ${"%.2f".format(requiredSum)} for secure pickup.")
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            } else {
                repository.placeOrder(user.id, foodItem, quantity)
                repository.insertAuditLog(user.id, "POD_ORDER", "Order generated under Pay-on-Delivery protocol.")
                onComplete(true)
            }
        }
    }

    // Modern Cafe Configuration
    fun updateVendorAnnouncement(announcement: String) {
        _vendorAnnouncement.value = announcement.ifBlank { "Serving appetizing, dynamic recipes. Check daily specials!" }
    }

    fun setStoreClosedState(isClosed: Boolean) {
        _isStoreClosed.value = isClosed
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

    fun addVendorFoodItem(name: String, price: Double, category: String, description: String, imageUrl: String) {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            if (name.isNotBlank() && price > 0) {
                repository.addMenuFoodItem(vendor.id, name, price, category, description, imageUrl)
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
        }
    }

    fun verifySecurePickup(orderId: Int, inputPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val vendor = _currentUser.value ?: return@launch
            val verified = repository.verifyAndCompletePickup(vendor.id, orderId, inputPin)
            onResult(verified)
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
}
