package com.example.ui.screens
import com.example.ui.util.generatePdfReceipt
import com.example.ui.util.generatePdfOrderHistoryReport
import com.example.ui.util.HapticHelper

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.composed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.example.data.*
import com.example.data.recommendation.*
import com.example.ui.components.FoodRecommendationSection
import com.example.ui.components.DietaryPreferencesDialog
import com.example.ui.components.D3DashboardChart
import com.example.ui.components.D3MonthlySpendingChart
import com.example.ui.components.RechartsDashboardChart
import com.airbnb.lottie.compose.*
import androidx.compose.ui.draw.scale
import com.example.ui.components.RechartsFeedbackDashboardChart
import com.example.ui.components.ChartJsVendorPerformanceChart
import com.example.ui.components.InventoryTrackingHub
import com.example.ui.components.DailyRevenueBarChart
import com.example.ui.components.RadarFeedbackChart
import com.example.ui.components.StudentTrendsLineChart
import com.example.ui.components.VendorPerformanceTrendChart
import com.example.ui.components.WeeklyRevenueTrendLineChart
import com.example.ui.components.LaravelDailyRevenueTrendChart
import com.example.ui.viewmodel.CafeteriaViewModel

// ==========================================
// 1. APP AUTHENTICATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun RatingMetricBadge(label: String, value: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${"%.1f".format(value)}‚òÖ", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9100))
        }
    }
}
 
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

fun Modifier.pressScaleEffect(interactionSource: MutableInteractionSource) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ==========================================
// 3. STUDENT FOOD ORDERING & RATING PORTAL
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val allFoodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()
    val studentOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val allVendors by viewModel.allVendors.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    val allFoodFeedback by viewModel.allFoodFeedback.collectAsStateWithLifecycle()
    val studentWalletBalance by viewModel.studentWalletBalance.collectAsStateWithLifecycle()
    val userWalletTransactions by viewModel.userWalletTransactions.collectAsStateWithLifecycle()
    val vendorAnnouncement by viewModel.vendorAnnouncement.collectAsStateWithLifecycle()
    val isAdminActing by viewModel.isAdminActing.collectAsStateWithLifecycle()
    val redeemedPoints by viewModel.redeemedLoyaltyPoints.collectAsStateWithLifecycle()
    val loyaltySummary by viewModel.loyaltySummaryResponse.collectAsStateWithLifecycle()
    val allOrdersSnapshot by viewModel.allOrdersSnapshot.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val popularTodayContent by viewModel.popularTodayContent.collectAsStateWithLifecycle()
    val isPopularTodayLoading by viewModel.isPopularTodayLoading.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // Recommendation Engine Reactive States
    val recommendedItems by viewModel.recommendedItems.collectAsStateWithLifecycle()
    val activeRecommendationStrategy by viewModel.activeRecommendationStrategy.collectAsStateWithLifecycle()
    val dietaryPreferences by viewModel.dietaryPreferences.collectAsStateWithLifecycle()
    var showDietaryPreferencesDialog by remember { mutableStateOf(false) }
    val currentTotalPoints = remember(studentOrders) {
        studentOrders.sumOf { order ->
            if (order.status.uppercase() == "COMPLETED") 25 else 10
        }
    }
    val availablePoints = currentUser?.loyaltyPoints ?: (currentTotalPoints - redeemedPoints)

    val acknowledgedOrders = remember { mutableStateListOf<Int>() }
    var showOrderPlacedConfetti by remember { mutableStateOf(false) }
    var showQrForOrder by remember { mutableStateOf<Order?>(null) }
    var showScannerForOrder by remember { mutableStateOf<Order?>(null) }
    var showGeneralCheckInScanner by remember { mutableStateOf(false) }
    var activeChatOrder by remember { mutableStateOf<Order?>(null) }
    var isTtsEnabled by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val tts = remember {
        var textToSpeech: android.speech.tts.TextToSpeech? = null
        try {
            textToSpeech = android.speech.tts.TextToSpeech(context) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    try {
                        textToSpeech?.setLanguage(java.util.Locale.US)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentDashboardScreen", "TextToSpeech engine initialization failed", e)
        }
        textToSpeech
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                tts?.shutdown()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val activeOrdersList = remember(studentOrders) {
        studentOrders.filter { it.status == "PENDING" || it.status == "PREPARING" || it.status == "READY" }
    }
    val activeOrderStatusStr = remember(activeOrdersList) {
        activeOrdersList.joinToString { "Order for ${it.foodName} is ${it.status}" }
    }

    LaunchedEffect(activeOrderStatusStr) {
        if (isTtsEnabled && activeOrderStatusStr.isNotEmpty()) {
            tts?.speak(
                "Active order update: $activeOrderStatusStr",
                android.speech.tts.TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var previousOnlineState by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(isOnline) {
        if (previousOnlineState != null && previousOnlineState != isOnline) {
            if (!isOnline) {
                snackbarHostState.showSnackbar(
                    message = "‚ö†Ô∏è Connectivity lost: You are browsing the menu in offline mode with cached items.",
                    duration = SnackbarDuration.Short
                )
            } else {
                snackbarHostState.showSnackbar(
                    message = "‚ö° Internet reconnected! Live menu refreshed.",
                    duration = SnackbarDuration.Short
                )
            }
        }
        previousOnlineState = isOnline
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Browse Food, 1: Orders Hub, 2: Nutrition, 3: Prep Reserves, 4: Smart Wallet & ID
    var ordersSubTab by remember { mutableIntStateOf(0) } // 0: Live Tracker, 1: Dining History, 2: Favorites

    val browseScrollState = rememberLazyListState()
    val activeOrdersScrollState = rememberLazyListState()
    val pastOrdersScrollState = rememberLazyListState()
    val nutritionScrollState = rememberLazyListState()
    val prepScrollState = rememberLazyListState()
    val walletScrollState = rememberLazyListState()

    val favoritesScrollState = rememberLazyListState()

    val currentActiveScrollState = remember(activeTab, ordersSubTab) {
        when (activeTab) {
            0 -> browseScrollState
            1 -> {
                when (ordersSubTab) {
                    0 -> activeOrdersScrollState
                    1 -> pastOrdersScrollState
                    else -> favoritesScrollState
                }
            }
            2 -> nutritionScrollState
            3 -> prepScrollState
            4 -> walletScrollState
            else -> browseScrollState
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var selectedFoodForOrder by remember { mutableStateOf<FoodItem?>(null) }
    var orderQuantity by remember { mutableIntStateOf(1) }
    var payViaWallet by remember { mutableStateOf(false) }
    var orderPlacementError by remember { mutableStateOf<String?>(null) }
    var redeemLoyaltyPointsChecked by remember { mutableStateOf(false) }

    var showSubmissionConfirmation by remember { mutableStateOf(false) }
    var submissionConfirmFood by remember { mutableStateOf<FoodItem?>(null) }
    var submissionConfirmQuantity by remember { mutableIntStateOf(1) }
    var submissionConfirmPaymentMode by remember { mutableStateOf("WALLET") }
    var submissionConfirmCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var submissionIsProcessing by remember { mutableStateOf(false) }

    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }
    var topUpPhone by remember { mutableStateOf("") }
    var isTopUpProcessing by remember { mutableStateOf(false) }
    var topUpSuccess by remember { mutableStateOf(false) }
    var selectedVendorIdFilter by remember { mutableStateOf<Int?>(null) }
    var browseSelectionTab by remember { mutableStateOf("Dishes") }
    var vendorSearchQuery by remember { mutableStateOf("") }
    var showQualityRatingsLeaderboard by remember { mutableStateOf(false) }
    var showNotificationCenter by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showRewardsCatalogDialog by remember { mutableStateOf(false) }
    var showIndoorMapDialog by remember { mutableStateOf(false) }
    var showDailyHealthSummaryDialog by remember { mutableStateOf(false) }
    val monthlyBudgetLimit by viewModel.monthlyBudgetLimit.collectAsStateWithLifecycle()
    val studentAlerts by viewModel.activeStudentAlerts.collectAsStateWithLifecycle()
    val studentNotifications by viewModel.studentNotifications.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Advanced features state managers
    val scheduledReservations = remember { mutableStateListOf<ScheduledMeal>() }
    var dailyCalorieTarget by remember { mutableFloatStateOf(2000f) }
    var selectedGoalFilter by remember { mutableStateOf("All Goals") }
    val loggedNutritionMeals = remember { mutableStateListOf<Pair<FoodItem, Long>>() }
    var successNutritionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(allFoodItems) {
        if (loggedNutritionMeals.isEmpty() && allFoodItems.isNotEmpty()) {
            val jollof = allFoodItems.firstOrNull { it.name.lowercase().contains("jollof") || it.name.lowercase().contains("rice") }
                ?: allFoodItems.firstOrNull()
            jollof?.let {
                loggedNutritionMeals.add(it to System.currentTimeMillis() - 4 * 3600000)
            }
            val drink = allFoodItems.firstOrNull { it.category == "Drinks" }
            drink?.let {
                loggedNutritionMeals.add(it to System.currentTimeMillis() - 2 * 3600000)
            }
        }
        if (scheduledReservations.isEmpty() && allFoodItems.isNotEmpty()) {
            val localDish = allFoodItems.firstOrNull { it.category == "Local Dish" } ?: allFoodItems.firstOrNull()
            localDish?.let {
                scheduledReservations.add(
                    ScheduledMeal(
                        id = 1,
                        foodItem = it,
                        quantity = 1,
                        targetTime = "12:45 PM",
                        dateLabel = "Today",
                        specs = "Extra hot chili spice, bio degradable box.",
                        isPaid = true,
                        barcodeSeed = "REC-ATU-7731"
                    )
                )
            }
        }
    }

    var feedbackTargetOrder by remember { mutableStateOf<Order?>(null) }
    var foodQualityRating by remember { mutableIntStateOf(5) }
    var cleanlinessRating by remember { mutableIntStateOf(5) }
    var speedRating by remember { mutableIntStateOf(5) }
    var priceRating by remember { mutableIntStateOf(5) }
    var feedbackComment by remember { mutableStateOf("") }
    var foodItemRating by remember { mutableIntStateOf(5) }
    var foodItemComment by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }

    // Dynamic Record Audio Permission Launcher
    val audioPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "Microphone permission granted! Tap Mic again to speak.", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission required for voice ordering.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Voice-controlled search & Voice Ordering Speech Recognition Launcher
    val speechRecognizerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                val lowerSpoken = spokenText.lowercase()

                // Smart voice matching against menu items
                val matchedItem = allFoodItems.find { food ->
                    val foodLower = food.name.lowercase()
                    lowerSpoken.contains(foodLower) || foodLower.contains(lowerSpoken.replace("add", "").replace("cart", "").replace("order", "").replace("i want", "").trim())
                }

                if (matchedItem != null && (lowerSpoken.contains("add") || lowerSpoken.contains("cart") || lowerSpoken.contains("order") || lowerSpoken.contains("want") || lowerSpoken.contains("buy"))) {
                    viewModel.addToCart(matchedItem, 1)
                    searchQuery = matchedItem.name
                    android.widget.Toast.makeText(context, "üé§ Voice Command Recognized: Added '${matchedItem.name}' to cart!", android.widget.Toast.LENGTH_LONG).show()
                    tts?.speak("Added ${matchedItem.name} to your shopping cart", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                } else if (matchedItem != null) {
                    searchQuery = matchedItem.name
                    android.widget.Toast.makeText(context, "üé§ Voice Search: Found '${matchedItem.name}'", android.widget.Toast.LENGTH_SHORT).show()
                    tts?.speak("Found ${matchedItem.name} on the menu", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    searchQuery = spokenText
                    android.widget.Toast.makeText(context, "üé§ Voice Search: '$spokenText'", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Breakfast", "Lunch", "Vegan", "Local Dish", "Fast Food", "Drinks", "Snacks")
    var selectedDietaryFilter by remember { mutableStateOf("All") }
    var showCartCheckoutDialog by remember { mutableStateOf(false) }
    var showCartOrderConfirmationDialog by remember { mutableStateOf(false) }

    // Real-time synchronization initialization
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.loadPopularTodaySuggestions()
            viewModel.fetchLoyaltySummary()
        }
    }

    val trackingContext = androidx.compose.ui.platform.LocalContext.current
    var previousStatuses by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    LaunchedEffect(studentOrders) {
        if (previousStatuses.isNotEmpty()) {
            studentOrders.forEach { order ->
                val prevStatus = previousStatuses[order.id]
                if (prevStatus != null && prevStatus.uppercase() != "READY" && order.status.uppercase() == "READY") {
                    android.widget.Toast.makeText(
                        trackingContext,
                        "Your order of ${order.foodName} is ready for pickup!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        previousStatuses = studentOrders.associate { it.id to it.status }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isAdminActing) {
                        IconButton(onClick = {
                            viewModel.stopImpersonation()
                            navController.navigate("admin_home") { popUpTo(0) }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Return to Admin Panel", tint = Color.White)
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(if (isAdminActing) "SIMULATION: STUDENT" else "ATU Student Panel", fontWeight = FontWeight.Bold)
                                
                                // Dynamic Sync/Offline Status Badge in Top Bar
                                Surface(
                                    color = if (!LaravelClientManager.isLaravelEnabled) Color(0xFFFFB300) else Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.semantics { contentDescription = "Network status indicator" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (!LaravelClientManager.isLaravelEnabled) Icons.Default.CloudOff else Icons.Default.Wifi,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = if (!LaravelClientManager.isLaravelEnabled) "OFFLINE" else "ONLINE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                "ID: ${currentUser?.info ?: ""} ‚Ä¢ Welcome, ${currentUser?.fullName ?: ""}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val tierEmojiHeader = when {
                                    availablePoints < 100 -> "ü•â"
                                    availablePoints < 300 -> "ü•à"
                                    else -> "ü•á"
                                }
                                Text("$tierEmojiHeader ‚≠ê", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$availablePoints pts",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    val cartCountVal by viewModel.cartCount.collectAsStateWithLifecycle()

                    IconButton(
                        onClick = {
                            viewModel.toggleDarkMode()
                            val text = if (!isDarkMode) "Dark theme enabled" else "Light theme enabled"
                            tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                        },
                        modifier = Modifier.testTag("student_top_theme_toggle_button")
                    ) {
                        Text(
                            text = if (isDarkMode) "‚òÄÔ∏è" else "üåô",
                            fontSize = 18.sp
                        )
                    }

                    IconButton(
                        onClick = { showCartCheckoutDialog = true },
                        modifier = Modifier.testTag("student_top_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (cartCountVal > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    ) {
                                        Text(cartCountVal.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "View Shopping Cart",
                                tint = Color.White
                            )
                        }
                    }

                    if (LaravelClientManager.isLaravelEnabled) {
                        val unreadCount = studentNotifications.count { it.read_at == null }
                        IconButton(onClick = { showNotificationCenter = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = Color.White
                                        ) {
                                            Text(unreadCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notification Center",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.impact(context)
                            showGeneralCheckInScanner = true
                        },
                        modifier = Modifier.testTag("open_qr_scanner_topbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Track & QR Code",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.impact(context)
                            showIndoorMapDialog = true
                        },
                        modifier = Modifier.testTag("open_indoor_map_topbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Cafeteria Indoor Floor Map",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticHelper.impact(context)
                            showDailyHealthSummaryDialog = true
                        },
                        modifier = Modifier.testTag("open_health_summary_topbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Daily Health & Nutrition Summary",
                            tint = Color.White
                        )
                    }
                    if (isAdminActing) {
                        Button(
                            onClick = {
                                viewModel.stopImpersonation()
                                navController.navigate("admin_home") { popUpTo(0) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("EXIT")
                        }
                    } else {
                        IconButton(onClick = {
                            viewModel.logOut()
                            navController.navigate("login") { popUpTo(0) }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isAdminActing) Color(0xFFFF8F00) else MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Browse", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Active Track") },
                    label = { Text("Orders Hub", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Calorie & Nutrition") },
                    label = { Text("Nutrition", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Reserve Meal") },
                    label = { Text("Reserve", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Wallet & ID", fontSize = 10.sp) }
                )
            }
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp, bottom = 12.dp)
            ) {
                // Scroll Up Button
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            currentActiveScrollState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp).testTag("scroll_up_fab"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Scroll to Top",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Scroll Down Button
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val lastIdx = currentActiveScrollState.layoutInfo.totalItemsCount - 1
                            if (lastIdx >= 0) {
                                currentActiveScrollState.animateScrollToItem(lastIdx)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(44.dp).testTag("scroll_down_fab"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Scroll to Bottom",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(225)) togetherWith fadeOut(animationSpec = tween(225))
                },
                label = "student_tabs"
            ) { targetTab ->
                when (targetTab) {
                0 -> {
                    // Browse Menu tab
                    var isRefreshingHome by remember { mutableStateOf(false) }
                    PullToRefreshBox(
                        isRefreshing = isRefreshingHome,
                        onRefresh = {
                            isRefreshingHome = true
                            viewModel.syncAllFromLaravel { success ->
                                isRefreshingHome = false
                                HapticHelper.notification(context, if (success) "SUCCESS" else "ERROR")
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("home_pull_to_refresh")
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        // Sub-Tab Segment Switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(24.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Dishes Menu", "Vendor Directory", "Campus Map").forEach { tabName ->
                                val selected = (tabName == "Dishes Menu" && browseSelectionTab == "Dishes") || 
                                               (tabName == "Vendor Directory" && browseSelectionTab == "Vendors") ||
                                               (tabName == "Campus Map" && browseSelectionTab == "Map")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .bounceClickable { 
                                            browseSelectionTab = when (tabName) {
                                                "Dishes Menu" -> "Dishes"
                                                "Vendor Directory" -> "Vendors"
                                                else -> "Map"
                                            }
                                        }
                                        .testTag("browse_selection_tab_${tabName.replace(" ", "_").lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabName,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Offline / Caching Layer Status Card
                        if (!isOnline) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("offline_cache_banner"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = "Offline Cache",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Intermittent Campus Internet Detected",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (lastSyncTime > 0L) {
                                            val dateStr = remember(lastSyncTime) {
                                                val sdf = java.text.SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", java.util.Locale.getDefault())
                                                sdf.format(java.util.Date(lastSyncTime))
                                            }
                                            Text(
                                                text = "Viewing cached menu from local storage ($dateStr)",
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            Text(
                                                text = "Viewing cached menu from offline database",
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    // Dynamic refresh/retry button
                                    var isRefreshingLocal by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = {
                                            isRefreshingLocal = true
                                            viewModel.syncAllFromLaravel { success ->
                                                isRefreshingLocal = false
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).testTag("offline_cache_retry_button")
                                    ) {
                                        if (isRefreshingLocal) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Retry Connection",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (lastSyncTime > 0L) {
                            // Subtle Online/Cached Sync Confirmation
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("online_sync_banner"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), shape = CircleShape)
                                    )
                                    Text(
                                        text = "Connected to ATU Campus Network ‚Ä¢ Menu synced with local cache",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Offline Storage Queue Indicator
                        val offlineOrders by viewModel.offlineOrders.collectAsStateWithLifecycle()
                        if (offlineOrders.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("offline_orders_queue_banner"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = "Offline Pending Orders",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Offline Queue: ${offlineOrders.size} Pending Orders",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        var isSyncingNow by remember { mutableStateOf(false) }
                                        Button(
                                            onClick = {
                                                isSyncingNow = true
                                                viewModel.syncAllFromLaravel { success ->
                                                    isSyncingNow = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("sync_offline_orders_btn")
                                        ) {
                                            if (isSyncingNow) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onError
                                                )
                                            } else {
                                                Text("Sync Now", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "Your pre-orders are stored safely on this device. They will automatically sync to the campus server once you are back online or press Sync Now.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
                                    offlineOrders.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "‚Ä¢ ${item.foodName} (x${item.quantity})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "GH‚Çµ ${"%.2f".format(item.totalPrice)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (browseSelectionTab == "Dishes") {
                            val currentActiveOrder = studentOrders.filter { it.status == "PENDING" || it.status == "PREPARING" || it.status == "READY" }.maxByOrNull { it.id }

                            LazyColumn(
                                state = browseScrollState,
                                modifier = Modifier.fillMaxSize().weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (currentActiveOrder != null) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("dashboard_active_order_banner"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (currentActiveOrder.status.uppercase()) {
                                                    "READY" -> MaterialTheme.colorScheme.tertiaryContainer
                                                    "PREPARING" -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                                }
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = when (currentActiveOrder.status.uppercase()) {
                                                    "READY" -> MaterialTheme.colorScheme.tertiary
                                                    "PREPARING" -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                                }
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                // Header Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                                        val alphaScale by infiniteTransition.animateFloat(
                                                            initialValue = 0.3f,
                                                            targetValue = 1f,
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(1000, easing = LinearEasing),
                                                                repeatMode = RepeatMode.Reverse
                                                            ),
                                                            label = "alpha"
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    when (currentActiveOrder.status.uppercase()) {
                                                                        "READY" -> Color(0xFF2E7D32).copy(alpha = alphaScale)
                                                                        "PREPARING" -> Color(0xFF1976D2).copy(alpha = alphaScale)
                                                                        else -> Color(0xFFF9A825).copy(alpha = alphaScale)
                                                                    }
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "LIVE ORDER TRACKER",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp,
                                                            color = when (currentActiveOrder.status.uppercase()) {
                                                                "READY" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                                "PREPARING" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                                                            }
                                                        )
                                                    }

                                                    Text(
                                                        text = "TICKET #${currentActiveOrder.id}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = when (currentActiveOrder.status.uppercase()) {
                                                            "READY" -> MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                                            "PREPARING" -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                            else -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                        }
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Food item & quantity details
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = currentActiveOrder.foodName,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        val vendorName = allVendors.find { it.id == currentActiveOrder.vendorId }?.fullName ?: "Campus Kitchen"
                                                        Text(
                                                            text = "Boiled/Prepared by: $vendorName",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = "QTY: ${currentActiveOrder.quantity}x",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Clean Real-Time Status Notification & Text
                                                val statusLabel = when (currentActiveOrder.status.uppercase()) {
                                                    "PENDING" -> "Cooking Protocol Pending Approval"
                                                    "PREPARING" -> "Culinary Preparation underway..."
                                                    "READY" -> "Hot & Fresh! Ready for Pickup!"
                                                    else -> currentActiveOrder.status
                                                }
                                                val statusColor = when (currentActiveOrder.status.uppercase()) {
                                                    "READY" -> Color(0xFF2E7D32)
                                                    "PREPARING" -> Color(0xFF1976D2)
                                                    else -> Color(0xFFF9A825)
                                                }
                                                val statusIcon = when (currentActiveOrder.status.uppercase()) {
                                                    "READY" -> Icons.Default.CheckCircle
                                                    "PREPARING" -> Icons.Default.Restaurant
                                                    else -> Icons.Default.Schedule
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(statusColor.copy(alpha = 0.08f))
                                                        .padding(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = statusIcon,
                                                        contentDescription = null,
                                                        tint = statusColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = statusLabel,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = statusColor
                                                    )
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(
                                                        text = if (currentActiveOrder.status.uppercase() == "READY") "PIN: [ ${currentActiveOrder.pickupPin} ]" else currentActiveOrder.estimatedPickupTime,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.sp,
                                                        color = statusColor
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Real-Time Smooth Progress Bar Slider
                                                val progress = when (currentActiveOrder.status.uppercase()) {
                                                    "PENDING" -> 0.25f
                                                    "PREPARING" -> 0.65f
                                                    "READY" -> 1.0f
                                                    else -> 0.1f
                                                }
                                                LinearProgressIndicator(
                                                    progress = progress,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = statusColor,
                                                    trackColor = statusColor.copy(alpha = 0.15f)
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Lottie Animated Status Feedback Component
                                                LottieOrderStatusUpdateView(
                                                    status = currentActiveOrder.status,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                Spacer(modifier = Modifier.height(10.dp))

                                                // Visual timeline row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    val steps = listOf("Queued", "Preparing", "Ready")
                                                    val currentStepIdx = when (currentActiveOrder.status.uppercase()) {
                                                        "PENDING" -> 0
                                                        "PREPARING" -> 1
                                                        "READY" -> 2
                                                        else -> 0
                                                    }
                                                    steps.forEachIndexed { i, label ->
                                                        val isPassedOrCurrent = i <= currentStepIdx
                                                        val isCurrent = i == currentStepIdx
                                                        Text(
                                                            text = label,
                                                            fontSize = 9.sp,
                                                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                                            color = if (isPassedOrCurrent) statusColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(14.dp))

                                                // Interactive Acceleration / Sim buttons right on the dashboard!
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            if (currentActiveOrder.status.uppercase() == "READY") {
                                                                showScannerForOrder = currentActiveOrder
                                                            } else {
                                                                viewModel.simulateAdvanceOrderStatus(currentActiveOrder.id)
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f).height(36.dp).testTag("dashboard_simulate_advance"),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (currentActiveOrder.status.uppercase() == "READY") Color(0xFF2E7D32) else statusColor,
                                                            contentColor = Color.White
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                                        shape = RoundedCornerShape(18.dp)
                                                    ) {
                                                        Icon(
                                                            if (currentActiveOrder.status.uppercase() == "READY") Icons.Default.QrCodeScanner else Icons.Default.AutoAwesome,
                                                            contentDescription = "Simulate Tick",
                                                            modifier = Modifier.size(13.dp),
                                                            tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = when (currentActiveOrder.status.uppercase()) {
                                                                "PENDING" -> "Simulate: Start Prep"
                                                                "PREPARING" -> "Simulate: Ready"
                                                                "READY" -> "Scan Counter QR"
                                                                else -> "Advance Live Status"
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    if (currentActiveOrder.status.uppercase() == "READY") {
                                                        Button(
                                                            onClick = { showQrForOrder = currentActiveOrder },
                                                            modifier = Modifier.height(36.dp).testTag("dashboard_view_ticket"),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                                                contentColor = MaterialTheme.colorScheme.tertiaryContainer
                                                            ),
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            shape = RoundedCornerShape(18.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.QrCodeScanner,
                                                                contentDescription = "Show Pass",
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Speed Pass", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        OutlinedButton(
                                                            onClick = { activeTab = 1; ordersSubTab = 0 },
                                                            modifier = Modifier.height(36.dp),
                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                contentColor = statusColor
                                                            ),
                                                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                                            shape = RoundedCornerShape(18.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.OpenInNew,
                                                                contentDescription = "Details",
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Hub Details", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    val completedOrdersForPoints = studentOrders.filter { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "DELIVERED" }
                                    
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("loyalty_points_dashboard_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                    Text("‚≠ê", fontSize = 22.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "Loyalty Rewards Club",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                                        )
                                                        Text(
                                                            text = "Calculated from completed orders",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.tertiary)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "${currentUser?.loyaltyPoints ?: 0} PTS",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onTertiary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), thickness = 1.dp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Recent Points Ledger:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )

                                            if (completedOrdersForPoints.isEmpty()) {
                                                Text(
                                                    text = "No recent completed orders yet. Complete orders to earn 25 PTS each!",
                                                    fontSize = 10.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                                                    completedOrdersForPoints.take(3).forEach { o ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "‚Ä¢ ${o.foodName} (Order #${o.id})",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = "+25 PTS",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "Secure Ordering Protocol Guarded",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "Every order generates a unique Digital Verification Ticket for secure pickup validation. Avoid line intercepts.",
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "CAMPUS BULLET BROADCAST",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            vendorAnnouncement,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Smart Food Recommendation Engine Section
                            FoodRecommendationSection(
                                recommendedItems = recommendedItems,
                                activeStrategy = activeRecommendationStrategy,
                                dietaryPreferences = dietaryPreferences,
                                onStrategySelected = { strategy ->
                                    viewModel.setRecommendationStrategy(strategy)
                                },
                                onOpenDietaryPreferences = {
                                    showDietaryPreferencesDialog = true
                                },
                                onAddToCart = { foodItem: FoodItem ->
                                    viewModel.addToCart(foodItem, 1)
                                    android.widget.Toast.makeText(context, "Added '${foodItem.name}' to cart!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onItemClick = { foodItem: FoodItem ->
                                    selectedFoodForOrder = foodItem
                                    orderQuantity = 1
                                }
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("popular_today_gemini_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("üî•", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Popular Today",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "AI-Powered Trending Analytics",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { viewModel.loadPopularTodaySuggestions() },
                                            modifier = Modifier.size(28.dp).testTag("refresh_popular_today_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isPopularTodayLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Gemini is calculating real-time cafeteria demand...",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = popularTodayContent.ifEmpty { "No order trends logged yet today." },
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Real-time preparation hours/minutes alerts from vendor
                        val preparingOrders = studentOrders.filter { it.status == "PREPARING" }
                        if (preparingOrders.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Active Prep Alerts",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "LIVE KITCHEN PREPARATION ALERTS",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.8.sp,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        preparingOrders.forEach { order ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = order.foodName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                                    )
                                                    Text(
                                                        text = "Order #${order.id} ‚Ä¢ Qty: ${order.quantity}",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.tertiary)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = order.estimatedPickupTime,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onTertiary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Professional Dynamic Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                HapticHelper.impact(context, "LIGHT")
                                                val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                                                    context,
                                                    android.Manifest.permission.RECORD_AUDIO
                                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                                if (!hasMicPerm) {
                                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                } else {
                                                    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to search or add items to cart (e.g. 'Add Jollof Rice')...")
                                                    }
                                                    try {
                                                        speechRecognizerLauncher.launch(intent)
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Voice Speech Recognition not supported on this device.", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.testTag("voice_search_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = "Voice Search",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                                            }
                                        }
                                    }
                                },
                                placeholder = { Text("Search delicious local dishes or vendor name...") },
                                label = { Text("Search Campus Menus") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        item {
                            // Dynamic Scrollable Horizontal Circular/Rounded Vendors Row
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(
                                    text = "Browse by Cafeteria Booths",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    // Circular/Rounded button "All Booths"
                                    item {
                                        val isSelected = selectedVendorIdFilter == null
                                        Card(
                                            modifier = Modifier
                                                .width(130.dp)
                                                .bounceClickable { selectedVendorIdFilter = null },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.RestaurantMenu,
                                                    contentDescription = "All Dishes",
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "All Booths",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Custom actual list of vendors under the new vendors table sync
                                    items(allVendors) { v ->
                                        val isSelected = selectedVendorIdFilter == v.id
                                        val overallRating = viewModel.getVendorMetrics(v.id, allFeedback)["overall"] ?: 0.0
                                        val vHoursInfo = com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(v.id, v.isOpen)
                                        Card(
                                            modifier = Modifier
                                                .width(165.dp)
                                                .bounceClickable { selectedVendorIdFilter = v.id },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                            ),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Card(
                                                        shape = androidx.compose.foundation.shape.CircleShape,
                                                        modifier = Modifier.size(24.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                                    ) {
                                                        v.logoUrl?.let { logo ->
                                                            coil.compose.AsyncImage(
                                                                model = logo,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                            )
                                                        } ?: Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = v.fullName.take(1).uppercase(),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    Text(
                                                        text = v.fullName,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    if (overallRating > 0.0) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(10.dp))
                                                            Text("${"%.1f".format(overallRating)}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Booth: ${v.info.ifBlank { "Main Cafeteria" }}",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                        maxLines = 1,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (vHoursInfo.isCurrentlyOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "‚óè ${vHoursInfo.statusLabel}",
                                                            color = if (vHoursInfo.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "üïí ${vHoursInfo.scheduleText}",
                                                    fontSize = 8.5.sp,
                                                    color = if (vHoursInfo.isCurrentlyOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1
                                                )

                                                if (vHoursInfo.isCurrentlyOpen) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val activeOrdersForVendor = allOrdersSnapshot.filter { it.vendorId == v.id && com.example.ui.util.WaitTimeService.isOrderActiveInQueue(it.status) }
                                                    val activeCount = activeOrdersForVendor.size
                                                    val (busyLabel, busyColor) = when {
                                                        activeCount <= 1 -> Pair("Quiet", Color(0xFF2E7D32))
                                                        activeCount <= 4 -> Pair("Moderate", Color(0xFFF57C00))
                                                        else -> Pair("Busy", Color(0xFFD32F2F))
                                                    }
                                                    val waitMinutes = com.example.ui.util.WaitTimeService.calculateEstimatedWaitTime(activeOrdersForVendor)

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .background(busyColor, androidx.compose.foundation.shape.CircleShape)
                                                        )
                                                        Text(
                                                            text = "$busyLabel ‚Ä¢ ${waitMinutes}m wait",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = busyColor
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        text = vHoursInfo.statusDetail,
                                                        fontSize = 8.sp,
                                                        color = Color(0xFFC62828),
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Dynamic Scrollable Horizontal Chips
                            Text(
                                text = "Filter by Hot Categories",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(categories) { category ->
                                    val isSelected = selectedCategory == category
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedCategory = category },
                                        label = { Text(category, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            // Dietary Preference Horizontal Chips
                            Text(
                                text = "Filter by Dietary Preferences",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                val dietaryOptions = listOf("All", "Vegan", "Vegetarian", "Gluten-Free")
                                items(dietaryOptions) { dietary ->
                                    val isSelected = selectedDietaryFilter == dietary
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDietaryFilter = dietary },
                                        label = { Text(dietary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        modifier = Modifier.testTag("dietary_chip_$dietary")
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Campus Diner Menus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Button(
                                    onClick = { showQualityRatingsLeaderboard = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFFB300),
                                        contentColor = Color(0xFF2E2E2E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E2E2E))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("üèÜ Booth Standings", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        val selectedVendor = allVendors.find { it.id == selectedVendorIdFilter }
                        if (selectedVendor != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                        ) {
                                            selectedVendor.pictureUrl?.let { coverUrl ->
                                                coil.compose.AsyncImage(
                                                    model = coverUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } ?: Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.linearGradient(
                                                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                                        )
                                                    )
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                                                            startY = 100f
                                                        )
                                                    )
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Card(
                                                    shape = androidx.compose.foundation.shape.CircleShape,
                                                    modifier = Modifier.size(44.dp).border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                                ) {
                                                    selectedVendor.logoUrl?.let { logo ->
                                                        coil.compose.AsyncImage(
                                                            model = logo,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } ?: Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = selectedVendor.fullName.take(1).uppercase(),
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                Column {
                                                    Text(
                                                        text = selectedVendor.fullName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 16.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = selectedVendor.info.ifBlank { "Campus Vendor Booth" },
                                                        fontSize = 11.sp,
                                                        color = Color.White.copy(alpha = 0.85f)
                                                    )
                                                    val mBasic = viewModel.getVendorMetrics(selectedVendor.id, allFeedback)
                                                    val overallBasic = mBasic["overall"] ?: 0.0
                                                    val countBasic = allFeedback.count { it.vendorId == selectedVendor.id }
                                                    if (overallBasic > 0.0) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                                                            Text(
                                                                text = "${"%.1f".format(overallBasic)}/5‚òÖ ($countBasic ${if (countBasic == 1) "review" else "reviews"})",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { selectedVendorIdFilter = null },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                                    .size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear Vendor Filter",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        // OPERATING HOURS INDICATOR BANNER
                                        val selVendorHours = remember(selectedVendor.id, selectedVendor.isOpen) {
                                            com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(selectedVendor.id, selectedVendor.isOpen)
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                .testTag("vendor_operating_hours_card"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (selVendorHours.isCurrentlyOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (selVendorHours.isCurrentlyOpen) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Schedule,
                                                        contentDescription = "Operating Hours",
                                                        tint = if (selVendorHours.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "Operating Hours: ${selVendorHours.scheduleText}",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selVendorHours.isCurrentlyOpen) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                                        )
                                                        Text(
                                                            text = selVendorHours.statusDetail,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 11.sp,
                                                            color = if (selVendorHours.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (selVendorHours.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                            shape = RoundedCornerShape(6.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = selVendorHours.statusLabel,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }
                                            }
                                        }

                                        // LIVE QUEUE & WAIT-TIME TRACKING BANNER
                                        val vendorActiveOrders = remember(allOrdersSnapshot, selectedVendor.id) {
                                            allOrdersSnapshot.filter { it.vendorId == selectedVendor.id && com.example.ui.util.WaitTimeService.isOrderActiveInQueue(it.status) }
                                        }
                                        val activeQueueSize = vendorActiveOrders.size
                                        val estWaitMinutes = com.example.ui.util.WaitTimeService.calculateEstimatedWaitTime(vendorActiveOrders)

                                        val (busyLevelText, busyLevelColor, busyLevelIcon) = when {
                                            activeQueueSize <= 1 -> Triple("Quiet (Instant Service)", Color(0xFF2E7D32), Icons.Default.CheckCircle)
                                            activeQueueSize <= 4 -> Triple("Moderate (Normal Prep)", Color(0xFFF57C00), Icons.Default.AccessTime)
                                            else -> Triple("High Demand (Expect Delays)", Color(0xFFD32F2F), Icons.Default.Warning)
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp)
                                                .testTag("vendor_queue_status_card"),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, busyLevelColor.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = busyLevelIcon,
                                                            contentDescription = "Busy level",
                                                            tint = busyLevelColor,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = busyLevelText,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = busyLevelColor
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Active orders in queue: $activeQueueSize",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "Est. Wait Time",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "$estWaitMinutes mins",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = busyLevelColor
                                                    )
                                                }
                                            }
                                        }

                                        // Detailed rating breakdown & Written reviews
                                        val m = viewModel.getVendorMetrics(selectedVendor.id, allFeedback)
                                        val count = allFeedback.count { it.vendorId == selectedVendor.id }
                                        if (count > 0) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Vendor Rating Breakdown",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    RatingMetricBadge(label = "üçî Taste", value = m["foodQuality"] ?: 0.0)
                                                    RatingMetricBadge(label = "ü´ß Hygiene", value = m["cleanliness"] ?: 0.0)
                                                    RatingMetricBadge(label = "‚ö° Speed", value = m["speed"] ?: 0.0)
                                                    RatingMetricBadge(label = "üí∞ Value", value = m["priceValue"] ?: 0.0)
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                var showReviewsList by remember(selectedVendor.id) { mutableStateOf(false) }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { showReviewsList = !showReviewsList }
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "üí¨ Read Customer Reviews ($count)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                    Icon(
                                                        imageVector = if (showReviewsList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                if (showReviewsList) {
                                                    val feedbacksForThisVendor = allFeedback.filter { it.vendorId == selectedVendor.id }
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.padding(top = 6.dp)
                                                    ) {
                                                        feedbacksForThisVendor.sortedByDescending { it.timestamp }.take(4).forEach { f ->
                                                            Card(
                                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Column(modifier = Modifier.padding(8.dp)) {
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        modifier = Modifier.fillMaxWidth()
                                                                    ) {
                                                                        val avgF = (f.ratingFoodQuality + f.ratingCleanliness + f.ratingServiceSpeed + f.ratingPriceValue) / 4.0
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                                        ) {
                                                                            repeat(5) { starIdx ->
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Star,
                                                                                    contentDescription = null,
                                                                                    tint = if (starIdx < avgF.toInt()) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                                                                    modifier = Modifier.size(10.dp)
                                                                                )
                                                                            }
                                                                            Spacer(modifier = Modifier.width(4.dp))
                                                                            Text("${"%.1f".format(avgF)}‚òÖ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                        val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US).format(java.util.Date(f.timestamp))
                                                                        Text(dateStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                                                    }
                                                                    if (f.comment.isNotBlank()) {
                                                                        Spacer(modifier = Modifier.height(4.dp))
                                                                        Text(
                                                                            text = f.comment,
                                                                            fontSize = 11.sp,
                                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val filteredFoods = allFoodItems.filter { food ->
                            val vendor = allVendors.find { it.id == food.vendorId }
                            val vendorIsOpen = com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(food.vendorId, vendor?.isOpen ?: true).isCurrentlyOpen
                            val vendorName = vendor?.fullName ?: ""
                            val vendorInfo = vendor?.info ?: ""
                            val matchesDietary = when (selectedDietaryFilter) {
                                "Vegan" -> food.description.contains("Vegan", ignoreCase = true) || food.name.contains("Vegan", ignoreCase = true) || food.allergens.contains("Vegan", ignoreCase = true)
                                "Vegetarian" -> food.description.contains("Vegetarian", ignoreCase = true) || food.name.contains("Vegetarian", ignoreCase = true) || food.description.contains("Vegan", ignoreCase = true) || food.allergens.contains("Vegetarian", ignoreCase = true)
                                "Gluten-Free" -> food.description.contains("Gluten-Free", ignoreCase = true) || food.name.contains("Gluten-Free", ignoreCase = true) || food.allergens.contains("Gluten-Free", ignoreCase = true) || !food.allergens.contains("Wheat", ignoreCase = true)
                                else -> true
                            }
                            val isScheduledVisible = viewModel.isFoodItemCurrentlyVisibleBySchedule(food)
                            vendorIsOpen &&
                            isScheduledVisible &&
                            matchesDietary &&
                            (selectedVendorIdFilter == null || food.vendorId == selectedVendorIdFilter) &&
                            (selectedCategory == "All" || food.category.equals(selectedCategory, ignoreCase = true)) &&
                            (searchQuery.isBlank() || 
                             food.name.contains(searchQuery, ignoreCase = true) || 
                             food.description.contains(searchQuery, ignoreCase = true) ||
                             food.category.contains(searchQuery, ignoreCase = true) ||
                             food.allergens.contains(searchQuery, ignoreCase = true) ||
                             vendorName.contains(searchQuery, ignoreCase = true) ||
                             vendorInfo.contains(searchQuery, ignoreCase = true))
                        }
                        if (isLoading) {
                            items(4) {
                                MenuSkeletonItem()
                            }
                        } else if (filteredFoods.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No food items match your query. Check other categories!")
                                }
                            }
                        } else {
                            items(filteredFoods) { food ->
                                val vendor = allVendors.find { it.id == food.vendorId }
                                val vendorIsOpen = com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(food.vendorId, vendor?.isOpen ?: true).isCurrentlyOpen
                                val isSoldOut = !food.isAvailable
                                val isClickable = vendorIsOpen && !isSoldOut
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bounceClickable(enabled = isClickable) {
                                            orderQuantity = 1
                                            selectedFoodForOrder = food
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSoldOut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = food.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = food.category,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                    
                                                    if (!vendorIsOpen) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.errorContainer)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                 text = "CLOSED",
                                                                 fontSize = 8.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 color = MaterialTheme.colorScheme.onErrorContainer
                                                            )
                                                        }
                                                    }
                                                    if (isSoldOut) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.error)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "SOLD OUT",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Seller: ${vendor?.info ?: vendor?.fullName ?: "Traditional Kitchen"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "GH‚Çµ ${"%.2f".format(food.price)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = food.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        ) {
                                            // Calories Chip
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .testTag("calories_indicator_${food.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Whatshot,
                                                    contentDescription = "Calories",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "${food.calories} kcal",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            // Allergens Chip
                                            val hasAllergens = !food.allergens.equals("None", ignoreCase = true) && food.allergens.isNotBlank()
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (hasAllergens)
                                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                                        else
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    .testTag("allergens_indicator_${food.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (hasAllergens) Icons.Default.Warning else Icons.Default.Info,
                                                    contentDescription = "Allergens",
                                                    tint = if (hasAllergens)
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (hasAllergens) "Allergens: ${food.allergens}" else "No Allergens",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasAllergens)
                                                        MaterialTheme.colorScheme.onErrorContainer
                                                    else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val foodReviews = allFoodFeedback.filter { it.foodItemId == food.id }
                                        val avgRating = if (foodReviews.isNotEmpty()) foodReviews.map { it.rating }.average() else 0.0
                                        var showReviews by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (avgRating > 0.0) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Rating Star",
                                                        tint = Color(0xFFFFB300),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "${"%.1f".format(avgRating)} / 5.0 (${foodReviews.size} reviews)",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "No reviews yet",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }

                                            if (foodReviews.isNotEmpty()) {
                                                TextButton(
                                                    onClick = { showReviews = !showReviews },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = if (showReviews) "Hide Reviews" else "Read Reviews (${foodReviews.size})",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Icon(
                                                            imageVector = if (showReviews) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (showReviews && foodReviews.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                foodReviews.forEach { rev ->
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                                repeat(rev.rating) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Star,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFFB300),
                                                                        modifier = Modifier.size(11.dp)
                                                                    )
                                                                }
                                                            }
                                                            val reviewer = allUsers.find { it.id == rev.customerId }
                                                            Text(
                                                                text = reviewer?.fullName ?: "Campus Diner",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        if (rev.comment.isNotBlank()) {
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = rev.comment,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val vendorIsOpen = com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(food.vendorId, vendor?.isOpen ?: true).isCurrentlyOpen
                                        Button(
                                            onClick = {
                                                orderQuantity = 1
                                                selectedFoodForOrder = food
                                            },
                                            enabled = vendorIsOpen,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (vendorIsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (vendorIsOpen) Icons.Default.ShoppingCart else Icons.Default.Block,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (vendorIsOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (vendorIsOpen) "Place Order" else "Store Closed",
                                                fontSize = 12.sp,
                                                color = if (vendorIsOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    } else if (browseSelectionTab == "Vendors") {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Search bar for vendors
                                item {
                                    OutlinedTextField(
                                        value = vendorSearchQuery,
                                        onValueChange = { vendorSearchQuery = it },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Vendors", tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            if (vendorSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = { vendorSearchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                                }
                                            }
                                        },
                                        placeholder = { Text("Search booths, Auntie Mary, Kofi Kitchen...") },
                                        label = { Text("Search Cafeteria Vendors") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("vendor_search_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }

                                // list of vendors
                                val filteredVendors = allVendors.filter {
                                    it.fullName.contains(vendorSearchQuery, ignoreCase = true) ||
                                    it.info.contains(vendorSearchQuery, ignoreCase = true)
                                }

                                if (filteredVendors.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = "No Vendors Found",
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = "No matching cafeteria booths found.",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Try adjusting your search terms.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredVendors) { v ->
                                        VendorDirectoryCard(
                                            vendor = v,
                                            viewModel = viewModel,
                                            allFeedback = allFeedback,
                                            onViewMenu = {
                                                selectedVendorIdFilter = v.id
                                                browseSelectionTab = "Dishes"
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            CampusMapScreen(currentUser = currentUser, modifier = Modifier.weight(1f))
                        }
                    }
                    }
                }
                1 -> {
                    // Merged tracking and history hub
                    // ordersSubTab is hoisted to the top of StudentDashboardScreen
                    var pastOrdersSearchQuery by remember { mutableStateOf("") }
                    var pastOrdersSelectedStatus by remember { mutableStateOf("All") }
                    var pastOrdersSelectedVendorId by remember { mutableStateOf<Int?>(null) }
                    var pastOrdersSelectedPeriod by remember { mutableStateOf("All Time") }
                    var pastOrdersSortBy by remember { mutableStateOf("Newest First") }
                    var pastOrdersShowFilters by remember { mutableStateOf(false) }
                    val expandedOrderIds = remember { mutableStateMapOf<Int, Boolean>() }
                    var isRefreshingOrders by remember { mutableStateOf(false) }
                    
                    PullToRefreshBox(
                        isRefreshing = isRefreshingOrders,
                        onRefresh = {
                            isRefreshingOrders = true
                            viewModel.syncAllFromLaravel { success ->
                                isRefreshingOrders = false
                                HapticHelper.notification(context, if (success) "SUCCESS" else "ERROR")
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("orders_pull_to_refresh")
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        TabRow(selectedTabIndex = ordersSubTab) {
                            Tab(
                                selected = ordersSubTab == 0,
                                onClick = { ordersSubTab = 0 },
                                text = { Text("Live Tracker", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = ordersSubTab == 1,
                                onClick = { ordersSubTab = 1 },
                                text = { Text("Historical Dishes", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                            Tab(
                                selected = ordersSubTab == 2,
                                onClick = { ordersSubTab = 2 },
                                text = { Text("1-Click Favorites", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                        
                        if (ordersSubTab == 0) {
                            val liveTrackingMap by viewModel.liveOrderTrackingMap.collectAsStateWithLifecycle()
                            val liveEstTimeMap by viewModel.liveOrderEstimatedTimeMap.collectAsStateWithLifecycle()
                            val activeOrders = studentOrders.filter { it.status == "PENDING" || it.status == "PREPARING" || it.status == "READY" || it.status == "COMPLETED" || it.status == "DELIVERED" }.sortedByDescending { it.id }
                            
                            LazyColumn(
                                state = activeOrdersScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                        item {
                            Column {
                                Text(
                                    text = "Real-Time Meal Tracker",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Monitor live preparation speed under Accra Technical culinary protocols.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (activeOrders.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No active order pipeline detected",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            "Satisfy your cravings with nutritious dishes. Go to the Browse tab and request a dining ticket!",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(activeOrders) { order ->
                                val effectiveStatus = liveTrackingMap[order.id] ?: order.status
                                val effectiveEstTime = liveEstTimeMap[order.id] ?: order.estimatedPickupTime
                                val vendor = allVendors.find { it.id == order.vendorId }
                                val orderFeedback = allFeedback.find { it.orderId == order.id }
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("order_tracker_card_${order.id}"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "TICKET ORDER #${order.id}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = order.foodName,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "Seller: ${vendor?.fullName ?: "Cafeteria Vendor"} (${vendor?.info ?: "Culinary booth"})",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            // Status tag with smooth animated color transition
                                            val animatedBadgeColor by animateColorAsState(
                                                targetValue = when (effectiveStatus.uppercase()) {
                                                    "PENDING", "ORDER_PLACED", "RECEIVED" -> Color(0xFFF9A825) // Amber / Received
                                                    "PREPARING" -> Color(0xFF1976D2) // Blue / Preparing
                                                    "READY", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY" -> Color(0xFF2E7D32) // Green / Out for Delivery
                                                    "COMPLETED", "DELIVERED" -> Color(0xFF388E3C) // Emerald Green / Delivered
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                animationSpec = tween(durationMillis = 400),
                                                label = "BadgeColorAnimation"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(animatedBadgeColor)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                AnimatedContent(
                                                    targetState = effectiveStatus.uppercase(),
                                                    transitionSpec = {
                                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                                    },
                                                    label = "StatusTextTransition"
                                                ) { targetStatus ->
                                                    Text(
                                                        text = when (targetStatus) {
                                                            "PENDING", "ORDER_PLACED", "RECEIVED" -> "Received"
                                                            "PREPARING" -> "Preparing"
                                                            "READY", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY" -> "Out for Delivery"
                                                            "COMPLETED", "DELIVERED" -> "Delivered"
                                                            else -> targetStatus
                                                        },
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Stepper progress timeline helper
                                        val steps = listOf("Received", "Preparing", "Out for Delivery", "Delivered")
                                        val activeStep = when (effectiveStatus.uppercase()) {
                                            "PENDING", "ORDER_PLACED", "RECEIVED" -> 0
                                            "PREPARING" -> 1
                                            "READY", "READY_FOR_PICKUP", "OUT_FOR_DELIVERY" -> 2
                                            "COMPLETED", "DELIVERED" -> 3
                                            else -> 0
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            steps.forEachIndexed { index, stepTitle ->
                                                val isCompleted = index < activeStep
                                                val isCurrent = index == activeStep

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isCurrent) MaterialTheme.colorScheme.primary
                                                                else if (isCompleted) MaterialTheme.colorScheme.primaryContainer
                                                                else MaterialTheme.colorScheme.surfaceVariant
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isCompleted) {
                                                            Icon(
                                                                Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        } else if (isCurrent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.onPrimary)
                                                            )
                                                        } else {
                                                            Text(
                                                                text = (index + 1).toString(),
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = stepTitle,
                                                        fontSize = 9.sp,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (index < steps.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(3.dp)
                                                            .weight(0.4f)
                                                            .background(
                                                                if (index < activeStep) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Estimated pickup banner
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                                .padding(12.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    if (order.status == "PENDING") {
                                                        Text(
                                                            text = "ESTIMATED PICKUP TIME",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                        )
                                                        Text(
                                                            text = "Awaiting acceptance from vendor...",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                        Spacer(modifier = Modifier.height(10.dp))
                                                        Button(
                                                            onClick = { viewModel.studentCancelOrder(order.id) },
                                                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("student_cancel_order_${order.id}"),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.error,
                                                                contentColor = MaterialTheme.colorScheme.onError
                                                            ),
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Cancel,
                                                                contentDescription = "Cancel Order",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Cancel Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else if (order.status == "PREPARING") {
                                                        Text(
                                                            text = "ESTIMATED PICKUP TIME",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                        )
                                                        Text(
                                                            text = order.estimatedPickupTime,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                        
                                                        val avgPrepMin = remember(order.vendorId, auditLogs) {
                                                            val vendorReadyLogs = auditLogs.filter { 
                                                                it.action == "ORDER_STATUS_CHANGED" && 
                                                                it.details.contains("transitioned to: READY", ignoreCase = true) 
                                                            }
                                                            val durations = mutableListOf<Long>()
                                                            for (rLog in vendorReadyLogs) {
                                                                try {
                                                                    val orderIdPart = rLog.details.substringAfter("Order #").substringBefore(" ")
                                                                    val orderId = orderIdPart.toIntOrNull()
                                                                    if (orderId != null) {
                                                                        val pLog = auditLogs.find { 
                                                                            it.action == "ORDER_STATUS_CHANGED" && 
                                                                            it.details.contains("Order #$orderId ") && 
                                                                            it.details.contains("transitioned to: PREPARING", ignoreCase = true) 
                                                                        }
                                                                        if (pLog != null && rLog.timestamp > pLog.timestamp) {
                                                                            durations.add(rLog.timestamp - pLog.timestamp)
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    // Ignore parsing issues
                                                                }
                                                            }
                                                            if (durations.isNotEmpty()) {
                                                                (durations.average() / 60000.0)
                                                            } else {
                                                                10.0
                                                             }
                                                        }
                                                        
                                                        val prepDurationMs = (avgPrepMin * 60 * 1000).toLong()
                                                        val targetTimeMs = order.orderTimestamp + prepDurationMs
                                                        
                                                        var timeLeftSeconds by remember(order.id, targetTimeMs) { 
                                                            mutableLongStateOf(((targetTimeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)) 
                                                        }
                                                        
                                                        LaunchedEffect(order.id, targetTimeMs) {
                                                            while (timeLeftSeconds > 0) {
                                                                delay(1000)
                                                                timeLeftSeconds = ((targetTimeMs - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
                                                            }
                                                        }
                                                        
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Schedule,
                                                                contentDescription = "Countdown Timer",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            val minutesLeft = timeLeftSeconds / 60
                                                            val secondsLeft = timeLeftSeconds % 60
                                                            Text(
                                                                text = if (timeLeftSeconds > 0) {
                                                                    "Fulfillment Countdown: ${minutesLeft}m ${secondsLeft}s"
                                                                } else {
                                                                    "Wrapping up preparation..."
                                                                },
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    } else if (order.status.uppercase() == "READY") {
                                                        Text(
                                                            text = "HOT & READY FOR RETRIEVAL",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                        Text(
                                                            text = "Bring token to physical stand immediately!",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 12.sp,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Button(
                                                             onClick = { viewModel.studentConfirmReceipt(order.id) },
                                                             modifier = Modifier.fillMaxWidth().height(36.dp).testTag("student_confirm_receipt_${order.id}"),
                                                             colors = ButtonDefaults.buttonColors(
                                                                 containerColor = MaterialTheme.colorScheme.primary,
                                                                 contentColor = MaterialTheme.colorScheme.onPrimary
                                                             ),
                                                             shape = RoundedCornerShape(8.dp),
                                                             contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                             Icon(
                                                                 imageVector = Icons.Default.CheckCircle,
                                                                 contentDescription = "Received Food",
                                                                 modifier = Modifier.size(16.dp)
                                                             )
                                                             Spacer(modifier = Modifier.width(6.dp))
                                                             Text("I have received my food", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                     } else if (order.status.uppercase() == "COMPLETED" || order.status.uppercase() == "DELIVERED") {
                                                         Text(
                                                             text = "ORDER SAFELY FULFILLED",
                                                             fontSize = 10.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = MaterialTheme.colorScheme.primary
                                                         )
                                                         Text(
                                                             text = "Successfully retrieved custody of your meal!",
                                                             fontWeight = FontWeight.ExtraBold,
                                                             fontSize = 12.sp,
                                                             color = MaterialTheme.colorScheme.primary
                                                         )
                                                         Spacer(modifier = Modifier.height(4.dp))
                                                         Row(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                             verticalAlignment = Alignment.CenterVertically
                                                         ) {
                                                             val context = androidx.compose.ui.platform.LocalContext.current
                                                             OutlinedButton(
                                                                 onClick = { generatePdfReceipt(context, order) },
                                                                 modifier = Modifier.padding(top = 4.dp).testTag("download_receipt_live_${order.id}").height(34.dp),
                                                                 colors = ButtonDefaults.outlinedButtonColors(
                                                                     contentColor = MaterialTheme.colorScheme.primary
                                                                 ),
                                                                 shape = RoundedCornerShape(8.dp),
                                                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                                 contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                             ) {
                                                                 Icon(
                                                                     imageVector = Icons.Default.Print,
                                                                     contentDescription = "PDF Receipt",
                                                                     modifier = Modifier.size(12.dp)
                                                                 )
                                                                 Spacer(modifier = Modifier.width(4.dp))
                                                                 Text("Download Receipt", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                             }

                                                             if (orderFeedback == null) {
                                                                 Button(
                                                                     onClick = { feedbackTargetOrder = order },
                                                                     modifier = Modifier.padding(top = 4.dp).testTag("feedback_button_live_${order.id}").height(34.dp),
                                                                     colors = ButtonDefaults.buttonColors(
                                                                         containerColor = MaterialTheme.colorScheme.secondary,
                                                                         contentColor = MaterialTheme.colorScheme.onSecondary
                                                                     ),
                                                                     shape = RoundedCornerShape(8.dp),
                                                                     contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                 ) {
                                                                     Icon(
                                                                         imageVector = Icons.Default.Star,
                                                                         contentDescription = null,
                                                                         modifier = Modifier.size(12.dp)
                                                                     )
                                                                     Spacer(modifier = Modifier.width(4.dp))
                                                                     Text("Rate Vendor", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                                 }
                                                             } else {
                                                                 val avgRating = (orderFeedback.ratingFoodQuality + orderFeedback.ratingCleanliness + orderFeedback.ratingServiceSpeed + orderFeedback.ratingPriceValue) / 4.0
                                                                 Row(
                                                                     verticalAlignment = Alignment.CenterVertically,
                                                                     modifier = Modifier.padding(top = 4.dp)
                                                                 ) {
                                                                     Icon(
                                                                         imageVector = Icons.Default.Star,
                                                                         contentDescription = null,
                                                                         tint = Color(0xFFF9A825),
                                                                         modifier = Modifier.size(14.dp)
                                                                     )
                                                                     Spacer(modifier = Modifier.width(4.dp))
                                                                     Text(
                                                                         text = "%.1f ‚òÖ".format(avgRating),
                                                                         fontSize = 11.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                     )
                                                                 }
                                                             }
                                                         }
                                                     }
                                                }
                                            }
                                        }

                                         // Handshake Token (PIN)
                                         Spacer(modifier = Modifier.height(10.dp))
                                         Row(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clip(RoundedCornerShape(8.dp))
                                                 .background(if (order.status.uppercase() == "COMPLETED") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                                 .padding(12.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Icon(
                                                 if (order.status.uppercase() == "COMPLETED") Icons.Default.CheckCircle else Icons.Default.Lock,
                                                 contentDescription = null,
                                                 tint = if (order.status.uppercase() == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                 modifier = Modifier.size(18.dp)
                                             )
                                             Spacer(modifier = Modifier.width(10.dp))
                                             Column {
                                                 Text(
                                                     text = if (order.status.uppercase() == "COMPLETED") "VERIFIED SECURE HANDSHAKE" else "SECURE PICKUP TOKEN PIN: [ ${order.pickupPin} ]",
                                                     fontWeight = FontWeight.Bold,
                                                     fontSize = 12.sp,
                                                     color = if (order.status.uppercase() == "COMPLETED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onErrorContainer
                                                 )
                                                 Text(
                                                     text = if (order.status.uppercase() == "COMPLETED") "Custody of this order was successfully handed off." else "Quote this secret verification code to the server booth.",
                                                     fontSize = 9.sp,
                                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                                 )
                                             }
                                         }

                                         Spacer(modifier = Modifier.height(12.dp))
                                         Button(
                                             onClick = {
                                                 HapticHelper.impact(context)
                                                 showQrForOrder = order
                                             },
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .height(38.dp)
                                                 .testTag("track_and_qr_btn_${order.id}"),
                                             colors = ButtonDefaults.buttonColors(
                                                 containerColor = MaterialTheme.colorScheme.primary,
                                                 contentColor = MaterialTheme.colorScheme.onPrimary
                                             ),
                                             shape = RoundedCornerShape(10.dp)
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.QrCode,
                                                 contentDescription = "Track Order & QR Claim Pass",
                                                 modifier = Modifier.size(16.dp)
                                             )
                                             Spacer(modifier = Modifier.width(8.dp))
                                             Text(
                                                 text = "Track Order & View Claim QR",
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }

                                         Spacer(modifier = Modifier.height(8.dp))
                                         Button(
                                             onClick = { activeChatOrder = order },
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .height(38.dp)
                                                 .testTag("chat_with_vendor_btn_${order.id}"),
                                             colors = ButtonDefaults.buttonColors(
                                                 containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                 contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                             ),
                                             shape = RoundedCornerShape(10.dp)
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.Chat,
                                                 contentDescription = "Chat with Vendor",
                                                 modifier = Modifier.size(16.dp)
                                             )
                                             Spacer(modifier = Modifier.width(8.dp))
                                             Text(
                                                 text = "Chat with Vendor (Urgent Inquiry)",
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }

                                         // Real-time Simulation / Status Progression button
                                         Spacer(modifier = Modifier.height(12.dp))
                                         Button(
                                             onClick = {
                                                 if (order.status.uppercase() == "READY") {
                                                     showScannerForOrder = order
                                                 } else {
                                                     viewModel.simulateAdvanceOrderStatus(order.id)
                                                 }
                                             },
                                             modifier = Modifier.fillMaxWidth().testTag("simulate_status_btn_${order.id}"),
                                             colors = ButtonDefaults.buttonColors(
                                                 containerColor = if (order.status.uppercase() == "READY") Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary,
                                                 contentColor = if (order.status.uppercase() == "READY") Color.White else MaterialTheme.colorScheme.onSecondary
                                             ),
                                             shape = RoundedCornerShape(10.dp)
                                         ) {
                                             Icon(
                                                 imageVector = if (order.status.uppercase() == "READY") Icons.Default.QrCodeScanner else Icons.Default.AutoAwesome,
                                                 contentDescription = "Simulate Progression",
                                                 modifier = Modifier.size(16.dp)
                                             )
                                             Spacer(modifier = Modifier.width(8.dp))
                                             Text(
                                                 text = when (order.status.uppercase()) {
                                                     "PENDING" -> "Simulate: Start Preparing"
                                                     "PREPARING" -> "Simulate: Set Ready for Pickup"
                                                     "READY" -> "Scan Counter QR"
                                                     else -> "Simulate: Reset Order to Pending"
                                                 },
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }

                                         // ==========================================
                                         // ‚ö° DIGITAL EXPRESS DISPENSE & WARMING LOCKER ROUTER
                                         // ==========================================
                                         if (order.status == "READY") {
                                             var showDigitalPass by remember { mutableStateOf(true) }
                                             var selectedLocker by remember { mutableStateOf<String?>(null) }
                                             var isLockerAssigned by remember { mutableStateOf(false) }

                                             Spacer(modifier = Modifier.height(12.dp))
                                             Button(
                                                 onClick = { showDigitalPass = !showDigitalPass },
                                                 modifier = Modifier.fillMaxWidth().testTag("toggle_speed_pass_btn_${order.id}"),
                                                 colors = ButtonDefaults.buttonColors(
                                                     containerColor = MaterialTheme.colorScheme.tertiary,
                                                     contentColor = MaterialTheme.colorScheme.onTertiary
                                                 ),
                                                 shape = RoundedCornerShape(10.dp)
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.AutoAwesome,
                                                     contentDescription = null,
                                                     modifier = Modifier.size(16.dp)
                                                 )
                                                 Spacer(modifier = Modifier.width(8.dp))
                                                 Text(
                                                     text = if (showDigitalPass) "HIDE LIVE RETRIEVAL TICKET" else "‚ö° VIEW EXPRESS RETRIEVAL PASS",
                                                     fontSize = 10.sp,
                                                     fontWeight = FontWeight.Bold,
                                                     letterSpacing = 0.5.sp
                                                 )
                                             }

                                             AnimatedVisibility(
                                                 visible = showDigitalPass,
                                                 enter = expandVertically() + fadeIn(),
                                                 exit = shrinkVertically() + fadeOut()
                                             ) {
                                                 Column(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .padding(vertical = 8.dp)
                                                 ) {
                                                     Box(
                                                         modifier = Modifier
                                                             .fillMaxWidth()
                                                             .background(
                                                                 color = Color(0xFF1E1E24),
                                                                 shape = RoundedCornerShape(14.dp)
                                                             )
                                                             .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(14.dp))
                                                             .padding(vertical = 16.dp, horizontal = 20.dp)
                                                     ) {
                                                         Column(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             horizontalAlignment = Alignment.CenterHorizontally
                                                         ) {
                                                             Text(
                                                                 text = "ACCRA TECH SPEED-LINE EXPRESS PASS",
                                                                 color = Color(0xFF00E676),
                                                                 fontWeight = FontWeight.ExtraBold,
                                                                 fontSize = 10.sp,
                                                                 letterSpacing = 1.sp
                                                             )
                                                             Text(
                                                                 text = "OFFICIAL DIGITAL SECURITY TOKEN ‚Ä¢ ORDER #${order.id}",
                                                                 color = Color.White.copy(alpha = 0.6f),
                                                                 fontWeight = FontWeight.Bold,
                                                                 fontSize = 8.sp,
                                                                 letterSpacing = 0.5.sp
                                                             )

                                                             Spacer(modifier = Modifier.height(12.dp))

                                                             Row(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 horizontalArrangement = Arrangement.SpaceBetween
                                                             ) {
                                                                 Column {
                                                                     Text("MEAL TYPE", color = Color.White.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                                     Text(order.foodName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                                                 }
                                                                 Column(horizontalAlignment = Alignment.End) {
                                                                     Text("DISPENSE STATUS", color = Color.White.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                                     Text("READY & HEATED", color = Color(0xFFFF9100), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                                                 }
                                                             }

                                                             Spacer(modifier = Modifier.height(10.dp))

                                                             Row(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 horizontalArrangement = Arrangement.SpaceBetween
                                                             ) {
                                                                 Column {
                                                                     Text("CULINARY OUTLET", color = Color.White.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                                     Text(vendor?.fullName ?: "Cafeteria Booth", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                 }
                                                                 Column(horizontalAlignment = Alignment.End) {
                                                                     Text("SECRET PASS PIN", color = Color.White.copy(alpha = 0.5f), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                                     Text(order.pickupPin, color = Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.height(10.dp))
                                                            Button(
                                                                onClick = { showQrForOrder = order },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                                                                    contentColor = Color(0xFF00E676)
                                                                ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("student_generate_qr_btn_${order.id}")
                                                              ) {
                                                                  Icon(
                                                                      imageVector = Icons.Default.QrCode,
                                                                      contentDescription = "Show Claim QR",
                                                                      modifier = Modifier.size(16.dp)
                                                                  )
                                                                  Spacer(modifier = Modifier.width(6.dp))
                                                                  Text("TAP FOR QR CLAIM CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                              }
                                                              Column(horizontalAlignment = Alignment.End) {
                                                                  Text("", modifier = Modifier.size(0.dp))
                                                                 }
                                                             }

                                                             Spacer(modifier = Modifier.height(14.dp))

                                                             Row(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                             ) {
                                                                 repeat(30) {
                                                                     Box(
                                                                         modifier = Modifier
                                                                             .height(2.dp)
                                                                             .weight(1f)
                                                                             .background(Color.White.copy(alpha = 0.15f))
                                                                     )
                                                                 }
                                                             }

                                                             Spacer(modifier = Modifier.height(14.dp))

                                                             Box(
                                                                 modifier = Modifier
                                                                     .fillMaxWidth()
                                                                     .height(75.dp)
                                                                     .clip(RoundedCornerShape(6.dp))
                                                                     .background(Color.White)
                                                                     .padding(horizontal = 14.dp, vertical = 6.dp)
                                                             ) {
                                                                 val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
                                                                 val laserYOffset by infiniteTransition.animateFloat(
                                                                     initialValue = 0f,
                                                                     targetValue = 1f,
                                                                     animationSpec = infiniteRepeatable(
                                                                         animation = tween(1500, easing = LinearEasing),
                                                                         repeatMode = RepeatMode.Reverse
                                                                     ),
                                                                     label = "laser_y"
                                                                 )

                                                                 androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                                     val lineCount = 38
                                                                     val spaceWidth = size.width / lineCount
                                                                     for (i in 0 until lineCount) {
                                                                         if (i % 4 != 0) {
                                                                             val strokeW = if (i % 3 == 0) 3.5.dp.toPx() else if (i % 2 == 0) 1.5.dp.toPx() else 2.5.dp.toPx()
                                                                             drawRect(
                                                                                 color = Color(0xFF15151A),
                                                                                 topLeft = androidx.compose.ui.geometry.Offset(i * spaceWidth, 0f),
                                                                                 size = androidx.compose.ui.geometry.Size(strokeW, size.height)
                                                                             )
                                                                         }
                                                                     }

                                                                     val currentHeight = size.height * laserYOffset
                                                                     drawLine(
                                                                         color = Color(0xFF00E676),
                                                                         start = androidx.compose.ui.geometry.Offset(0f, currentHeight),
                                                                         end = androidx.compose.ui.geometry.Offset(size.width, currentHeight),
                                                                         strokeWidth = 3.2.dp.toPx()
                                                                     )
                                                                 }
                                                             }

                                                             Spacer(modifier = Modifier.height(8.dp))

                                                             Row(
                                                                 verticalAlignment = Alignment.CenterVertically,
                                                                 horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                             ) {
                                                                 Box(
                                                                     modifier = Modifier
                                                                         .size(6.dp)
                                                                         .clip(CircleShape)
                                                                         .background(Color(0xFF00E676))
                                                                 )
                                                                 Text(
                                                                     text = "DYNAMIC SECURITY CODE: ACTU-SEC-${order.id * 123}",
                                                                     color = Color(0xFF00E676),
                                                                     fontFamily = FontFamily.Monospace,
                                                                     fontSize = 9.sp,
                                                                     fontWeight = FontWeight.Medium
                                                                 )
                                                             }

                                                             Spacer(modifier = Modifier.height(14.dp))

                                                             Card(
                                                                 modifier = Modifier.fillMaxWidth(),
                                                                 colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                                                 shape = RoundedCornerShape(10.dp)
                                                             ) {
                                                                 Column(modifier = Modifier.padding(10.dp)) {
                                                                     Row(
                                                                         verticalAlignment = Alignment.CenterVertically,
                                                                         horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                     ) {
                                                                         Icon(
                                                                             imageVector = Icons.Default.Lock,
                                                                             contentDescription = null,
                                                                             tint = Color(0xFFFFAB40),
                                                                             modifier = Modifier.size(16.dp)
                                                                         )
                                                                         Text(
                                                                             text = "AI Smart Warming Locker System",
                                                                             fontWeight = FontWeight.ExtraBold,
                                                                             fontSize = 10.sp,
                                                                             color = Color(0xFFFFAB40)
                                                                         )
                                                                     }
                                                                     Text(
                                                                         text = "Optionally direct the vendor to deposit this meal inside a heated campus smart locker. Retrieve it using your PIN anytime.",
                                                                         fontSize = 8.5.sp,
                                                                         color = Color.White.copy(alpha = 0.7f),
                                                                         lineHeight = 11.sp,
                                                                         modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                                                     )

                                                                     if (!isLockerAssigned) {
                                                                         val lockers = listOf("Smart Cabinet A-3 (Heated)", "Smart Cabinet B-1 (Heated)", "Smart Locker C-2 (Standard)", "Drop Rack Tier 2")
                                                                         LazyRow(
                                                                             horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                             modifier = Modifier.fillMaxWidth()
                                                                         ) {
                                                                             items(lockers) { locker ->
                                                                                 val isLocSelected = selectedLocker == locker
                                                                                 Box(
                                                                                     modifier = Modifier
                                                                                         .clip(RoundedCornerShape(6.dp))
                                                                                         .background(if (isLocSelected) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.1f))
                                                                                         .border(
                                                                                             width = 1.dp,
                                                                                             color = if (isLocSelected) Color(0xFFFFAB40) else Color.White.copy(alpha = 0.2f),
                                                                                             shape = RoundedCornerShape(6.dp)
                                                                                         )
                                                                                         .clickable { selectedLocker = locker }
                                                                                         .padding(horizontal = 8.dp, vertical = 6.dp)
                                                                                 ) {
                                                                                     Text(
                                                                                         text = locker,
                                                                                         fontSize = 8.sp,
                                                                                         fontWeight = FontWeight.Bold,
                                                                                         color = if (isLocSelected) Color(0xFF1E1E24) else Color.White
                                                                                     )
                                                                                 }
                                                                             }
                                                                         }

                                                                         if (selectedLocker != null) {
                                                                             Spacer(modifier = Modifier.height(8.dp))
                                                                             Button(
                                                                                 onClick = { isLockerAssigned = true },
                                                                                 colors = ButtonDefaults.buttonColors(
                                                                                     containerColor = Color(0xFFFFAB40),
                                                                                     contentColor = Color(0xFF1E1E24)
                                                                                 ),
                                                                                 modifier = Modifier.fillMaxWidth().height(30.dp),
                                                                                 shape = RoundedCornerShape(6.dp),
                                                                                 contentPadding = PaddingValues(0.dp)
                                                                             ) {
                                                                                 Text(
                                                                                     text = "ROUT TO LOCKER [ $selectedLocker ]",
                                                                                     fontSize = 9.sp,
                                                                                     fontWeight = FontWeight.ExtraBold
                                                                                 )
                                                                             }
                                                                         }
                                                                     } else {
                                                                         Box(
                                                                             modifier = Modifier
                                                                                 .fillMaxWidth()
                                                                                 .background(Color(0xFF00E676).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                                                 .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                                                                                 .padding(10.dp)
                                                                         ) {
                                                                             Row(
                                                                                 verticalAlignment = Alignment.CenterVertically,
                                                                                 horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                             ) {
                                                                                 Icon(
                                                                                     imageVector = Icons.Default.CheckCircle,
                                                                                     contentDescription = null,
                                                                                     tint = Color(0xFF00E676),
                                                                                     modifier = Modifier.size(16.dp)
                                                                                 )
                                                                                 Column {
                                                                                     Text(
                                                                                         text = "ROUTED SUCCESSFULLY",
                                                                                         fontSize = 9.sp,
                                                                                         fontWeight = FontWeight.ExtraBold,
                                                                                         color = Color(0xFF00E676)
                                                                                     )
                                                                                     Text(
                                                                                         text = "Vendor notified. Deposit hot meal inside $selectedLocker. Pin to pop door: ${order.pickupPin}",
                                                                                         fontSize = 8.sp,
                                                                                         color = Color.White
                                                                                     )
                                                                                 }
                                                                             }
                                                                         }
                                                                     }
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                      }
                                  }
                              }
                          }
                        } else if (ordersSubTab == 1) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val completedOrCanceledOrders = studentOrders
                            val totalSpend = completedOrCanceledOrders.filter { it.status == "COMPLETED" || it.status == "DELIVERED" }.sumOf { it.totalPrice }
                            val orderCount = completedOrCanceledOrders.filter { it.status == "COMPLETED" || it.status == "DELIVERED" }.size
                            
                            val pastVendors = remember(completedOrCanceledOrders, allVendors) {
                                val vendorIds = completedOrCanceledOrders.map { it.vendorId }.distinct()
                                allVendors.filter { vendorIds.contains(it.id) }
                            }

                            val filteredPastOrders = remember(completedOrCanceledOrders, pastOrdersSearchQuery, pastOrdersSelectedStatus, pastOrdersSelectedVendorId, pastOrdersSelectedPeriod, pastOrdersSortBy) {
                                var list = completedOrCanceledOrders
                                
                                if (pastOrdersSearchQuery.isNotBlank()) {
                                    list = list.filter {
                                        it.foodName.contains(pastOrdersSearchQuery, ignoreCase = true) ||
                                        allVendors.find { v -> v.id == it.vendorId }?.fullName?.contains(pastOrdersSearchQuery, ignoreCase = true) == true
                                    }
                                }
                                
                                if (pastOrdersSelectedStatus != "All") {
                                    list = list.filter { it.status.equals(pastOrdersSelectedStatus, ignoreCase = true) }
                                }
                                
                                if (pastOrdersSelectedVendorId != null) {
                                    list = list.filter { it.vendorId == pastOrdersSelectedVendorId }
                                }
                                
                                val now = System.currentTimeMillis()
                                when (pastOrdersSelectedPeriod) {
                                    "Last 7 Days" -> {
                                        val weekAgo = now - (7L * 24 * 60 * 60 * 1000)
                                        list = list.filter { it.orderTimestamp >= weekAgo }
                                    }
                                    "Last 30 Days" -> {
                                        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                                        list = list.filter { it.orderTimestamp >= thirtyDaysAgo }
                                    }
                                }
                                
                                list = when (pastOrdersSortBy) {
                                    "Newest First" -> list.sortedByDescending { it.orderTimestamp }
                                    "Oldest First" -> list.sortedBy { it.orderTimestamp }
                                    "Price: High to Low" -> list.sortedByDescending { it.totalPrice }
                                    "Price: Low to High" -> list.sortedBy { it.totalPrice }
                                    else -> list.sortedByDescending { it.orderTimestamp }
                                }
                                
                                list
                            }

                            LazyColumn(
                                state = pastOrdersScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Student Meal Dashboard",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Examine your cumulative dining footprints and vendor performance archives.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        var isSyncingHistory by remember { mutableStateOf(false) }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    generatePdfOrderHistoryReport(
                                                        context = context,
                                                        studentName = currentUser?.fullName ?: "Student",
                                                        orders = filteredPastOrders
                                                    )
                                                },
                                                modifier = Modifier.testTag("download_pdf_history_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = "Download PDF Summary",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            
                                            IconButton(
                                                onClick = {
                                                    isSyncingHistory = true
                                                    viewModel.syncAllFromLaravel {
                                                        isSyncingHistory = false
                                                    }
                                                },
                                                modifier = Modifier.testTag("sync_order_history_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Refresh,
                                                    contentDescription = "Sync from API",
                                                    tint = if (isSyncingHistory) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Historical summary cards stats
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Total Invested", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    "GH‚Çµ ${"%.2f".format(totalSpend)}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text("Virtual Smart Funds", fontSize = 8.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("Plates Claimed", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                                Text(
                                                    "$orderCount Dishes",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text("Full Meal Deliveries", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }

                                item {
                                    StudentTrendsLineChart(
                                        orders = completedOrCanceledOrders.filter { it.status == "COMPLETED" },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                item {
                                    D3MonthlySpendingChart(
                                        orders = completedOrCanceledOrders,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }

                                // Interactive Filter Controller items
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = pastOrdersSearchQuery,
                                            onValueChange = { pastOrdersSearchQuery = it },
                                            placeholder = { Text("Search past meals or food booths...", fontSize = 13.sp) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                                            trailingIcon = {
                                                if (pastOrdersSearchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { pastOrdersSearchQuery = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("student_order_search_input"),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                            )
                                        )
                                        
                                        FilledTonalIconButton(
                                            onClick = { pastOrdersShowFilters = !pastOrdersShowFilters },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = "Toggle advanced filters",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Interactive advanced filter container
                                item {
                                    AnimatedVisibility(
                                        visible = pastOrdersShowFilters,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // Time frame filters
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Time Frame:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("All Time", "Last 7 Days", "Last 30 Days").forEach { period ->
                                                            val isSel = pastOrdersSelectedPeriod == period
                                                            SuggestionChip(
                                                                onClick = { pastOrdersSelectedPeriod = period },
                                                                label = { Text(period, fontSize = 10.sp) },
                                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                                    labelColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // Sort option selection
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Sort By:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf("Newest First", "Price: High to Low", "Price: Low to High").forEach { criteria ->
                                                            val isSel = pastOrdersSortBy == criteria
                                                            val label = when(criteria) {
                                                                "Price: High to Low" -> "Price $$$"
                                                                "Price: Low to High" -> "Price $"
                                                                else -> "Newest"
                                                            }
                                                            SuggestionChip(
                                                                onClick = { pastOrdersSortBy = criteria },
                                                                label = { Text(label, fontSize = 10.sp) },
                                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                                    containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                                    labelColor = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            )
                                                        }
                                                    }
                                                }

                                                // Filter by Vendor booth
                                                if (pastVendors.isNotEmpty()) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Filter by Booth:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        LazyRow(
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            contentPadding = PaddingValues(vertical = 2.dp)
                                                        ) {
                                                            item {
                                                                val isAll = pastOrdersSelectedVendorId == null
                                                                FilterChip(
                                                                    selected = isAll,
                                                                    onClick = { pastOrdersSelectedVendorId = null },
                                                                    label = { Text("All Booths", fontSize = 10.sp) }
                                                                )
                                                            }
                                                            items(pastVendors) { vendor ->
                                                                val isSel = pastOrdersSelectedVendorId == vendor.id
                                                                FilterChip(
                                                                    selected = isSel,
                                                                    onClick = { pastOrdersSelectedVendorId = vendor.id },
                                                                    label = { Text(vendor.fullName, fontSize = 10.sp) }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Quick filter tags
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        listOf("All", "PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED").forEach { status ->
                                            val isSel = pastOrdersSelectedStatus == status
                                            val label = when(status) {
                                                "PENDING" -> "Pending"
                                                "PREPARING" -> "Preparing"
                                                "READY" -> "Ready"
                                                "COMPLETED" -> "Completed"
                                                "CANCELLED" -> "Cancelled"
                                                else -> "All Statuses"
                                            }
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { pastOrdersSelectedStatus = status },
                                                label = { Text(label, fontSize = 11.sp) },
                                                modifier = Modifier.testTag("status_filter_chip_${status.lowercase()}"),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }
                                }

                                // Info headers and counter
                                item {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Official Receipt Journal (${filteredPastOrders.size} dishes found)", 
                                                fontWeight = FontWeight.Bold, 
                                                style = MaterialTheme.typography.titleSmall, 
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (filteredPastOrders.isNotEmpty()) {
                                                Button(
                                                    onClick = {
                                                        HapticHelper.notification(context, "SUCCESS")
                                                        generatePdfOrderHistoryReport(
                                                            context,
                                                            currentUser?.fullName ?: "Accra Tech Student",
                                                            filteredPastOrders
                                                        )
                                                    },
                                                    modifier = Modifier.height(34.dp).testTag("export_monthly_pdf_summary_btn"),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Print,
                                                        contentDescription = "Export Monthly Summary PDF",
                                                        modifier = Modifier.size(15.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Export Monthly Summary PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                                }
                                            }

                                            if (pastOrdersSearchQuery.isNotEmpty() || pastOrdersSelectedStatus != "All" || pastOrdersSelectedVendorId != null || pastOrdersSelectedPeriod != "All Time") {
                                                TextButton(
                                                    onClick = {
                                                        pastOrdersSearchQuery = ""
                                                        pastOrdersSelectedStatus = "All"
                                                        pastOrdersSelectedVendorId = null
                                                        pastOrdersSelectedPeriod = "All Time"
                                                        pastOrdersSortBy = "Newest First"
                                                    },
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                                 ) {
                                                     Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                                 }
                                             }
                                         }
                                     }
                                 }

                                if (isLoading) {
                                    items(3) {
                                        OrderHistorySkeletonItem()
                                    }
                                } else if (filteredPastOrders.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No matching past orders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                Text("Modify search filters to discover historical dishes.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredPastOrders) { order ->
                                        val orderFeedback = allFeedback.find { it.orderId == order.id }
                                        val isExpanded = expandedOrderIds[order.id] ?: false
                                        val vendorInfo = allVendors.find { it.id == order.vendorId }

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { expandedOrderIds[order.id] = !isExpanded }
                                                .testTag("order_history_item_card_${order.id}"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                // Primary summary header
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                            Text("Booth: ${vendorInfo?.fullName ?: "Cafeteria Vendor"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                        
                                                        val favoriteFoodIds by viewModel.favoriteFoodIds.collectAsStateWithLifecycle()
                                                        val isFav = favoriteFoodIds.contains(order.foodItemId)
                                                        IconButton(
                                                            onClick = { viewModel.toggleFavoriteFood(order.foodItemId) },
                                                            modifier = Modifier.size(36.dp).testTag("favorite_toggle_${order.foodItemId}")
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                                contentDescription = if (isFav) "Remove Favorite" else "Add Favorite",
                                                                tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        // One-click Quick Re-order Button directly on Card
                                                        IconButton(
                                                            onClick = {
                                                                HapticHelper.impact(context, "LIGHT")
                                                                val matchedFood = allFoodItems.find { it.id == order.foodItemId } ?: FoodItem(
                                                                    id = order.foodItemId,
                                                                    vendorId = order.vendorId,
                                                                    name = order.foodName,
                                                                    price = order.unitPrice,
                                                                    category = "Reordered",
                                                                    imageUrl = "",
                                                                    description = "Archived culinary selection from past transactions"
                                                                )
                                                                viewModel.addToCart(matchedFood, order.quantity)
                                                                android.widget.Toast.makeText(
                                                                    context,
                                                                    "${order.foodName} added to shopping cart!",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                ).show()
                                                            },
                                                            modifier = Modifier.size(36.dp).testTag("quick_reorder_${order.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Refresh,
                                                                contentDescription = "Quick Reorder",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        val statusColors = when (order.status) {
                                                            "COMPLETED", "DELIVERED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                                                            "DECLINED" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                                                            "CANCELLED" -> Color(0xFFECEFF1) to Color(0xFF37474F)
                                                            else -> Color(0xFFFFF8E1) to Color(0xFFF9A825)
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(statusColors.first)
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = order.status,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = statusColors.second
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            "GH‚Çµ ${"%.2f".format(order.totalPrice)}",
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Date summary and expand layout
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DateRange,
                                                            contentDescription = "Order creation timestamp",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        val dateStr = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.US).format(java.util.Date(order.orderTimestamp))
                                                        Text(dateStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isExpanded) "Hide details" else "Show details",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Icon(
                                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                            contentDescription = if (isExpanded) "Collapse detailed information" else "Expand detailed information",
                                                            modifier = Modifier.size(16.dp).testTag("expand_order_details_${order.id}"),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                // Expandable detailed breakdown slot
                                                AnimatedVisibility(
                                                    visible = isExpanded,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .padding(top = 10.dp)
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                                            .padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Receipt reference code:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("ATU-TKT-${order.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Quantity bought:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("${order.quantity} units", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Price per item unit:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text("GH‚Çµ ${"%.2f".format(order.unitPrice)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                        }
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("Handshake Pickup PIN:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(order.pickupPin, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                        }

                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                        Spacer(modifier = Modifier.height(6.dp))

                                                        if (order.status != "COMPLETED") {
                                                            Text(
                                                                text = "Notes: ${if (order.status == "CANCELLED") "Voided on demand before preparation loop." else "Refunded due to booth culinary stock limits."}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.padding(bottom = 6.dp)
                                                            )
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    HapticHelper.impact(context, "LIGHT")
                                                                    val matchedFood = allFoodItems.find { it.id == order.foodItemId } ?: FoodItem(
                                                                        id = order.foodItemId,
                                                                        vendorId = order.vendorId,
                                                                        name = order.foodName,
                                                                        price = order.unitPrice,
                                                                        category = "Reordered",
                                                                        imageUrl = "",
                                                                        description = "Archived culinary selection from past transactions"
                                                                    )
                                                                    viewModel.addToCart(matchedFood, order.quantity)
                                                                    android.widget.Toast.makeText(
                                                                        context,
                                                                        "${order.foodName} added to shopping cart!",
                                                                        android.widget.Toast.LENGTH_SHORT
                                                                    ).show()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                                ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier
                                                                    .height(34.dp)
                                                                    .testTag("reorder_button_${order.id}"),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = "Reorder", modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("Reorder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }

                                                            if (order.status == "COMPLETED" || order.status == "DELIVERED") {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    val context = androidx.compose.ui.platform.LocalContext.current
                                                                    OutlinedButton(
                                                                        onClick = { generatePdfReceipt(context, order) },
                                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f)),
                                                                        modifier = Modifier.height(34.dp).testTag("download_receipt_past_${order.id}"),
                                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Print, contentDescription = "PDF Receipt", modifier = Modifier.size(13.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("PDF Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                    }

                                                                    if (orderFeedback == null) {
                                                                        Button(
                                                                            onClick = { feedbackTargetOrder = order },
                                                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                                            shape = RoundedCornerShape(8.dp),
                                                                            modifier = Modifier
                                                                                .height(34.dp)
                                                                                .testTag("feedback_button_${order.id}"),
                                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                                        ) {
                                                                            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Text("Rate Booth", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                        }
                                                                    } else {
                                                                        val avgRating = (orderFeedback.ratingFoodQuality + orderFeedback.ratingCleanliness + orderFeedback.ratingServiceSpeed + orderFeedback.ratingPriceValue) / 4.0
                                                                        Row(
                                                                            modifier = Modifier
                                                                                .clip(RoundedCornerShape(8.dp))
                                                                                .background(Color(0xFFE8F5E9))
                                                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.Center
                                                                        ) {
                                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                                                            Spacer(modifier = Modifier.width(6.dp))
                                                                            Text(
                                                                                text = "Rated ${"%.1f".format(avgRating)}/5‚òÖ",
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = Color(0xFF2E7D32)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (ordersSubTab == 2) {
                            val favoriteFoodIds by viewModel.favoriteFoodIds.collectAsStateWithLifecycle()
                            val favoritedFoods = remember(favoriteFoodIds, allFoodItems) {
                                allFoodItems.filter { favoriteFoodIds.contains(it.id) }
                            }
                            
                            var reorderProcessingId by remember { mutableStateOf<Int?>(null) }
                            var reorderFeedbackMessage by remember { mutableStateOf<String?>(null) }
                            var isErrorFeedback by remember { mutableStateOf(false) }

                            LazyColumn(
                                state = favoritesScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    Column {
                                        Text(
                                            text = "Your Quick Favorites",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Re-order your favorite cafeteria picks with a single click using your Smart Wallet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (reorderFeedbackMessage != null) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("reorder_feedback_card"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isErrorFeedback) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isErrorFeedback) Icons.Default.Error else Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = if (isErrorFeedback) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                                    )
                                                    Text(
                                                        text = reorderFeedbackMessage ?: "",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { reorderFeedbackMessage = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Dismiss feedback",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = if (isErrorFeedback) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (favoritedFoods.isEmpty()) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FavoriteBorder,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Text(
                                                    text = "No saved favorites yet",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "To start placing instant 1-click orders, go to your 'Historical Dishes' page and favorite your premium dishes!",
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = { ordersSubTab = 1 },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Text("Go to Historical Dishes", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(favoritedFoods) { food ->
                                        val vendorInfo = allVendors.find { it.id == food.vendorId }
                                        val isProcessing = reorderProcessingId == food.id
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("fav_item_card_${food.id}"),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(food.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                        Text("Booth: ${vendorInfo?.fullName ?: "ATU Vendor"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    
                                                    IconButton(
                                                        onClick = { viewModel.toggleFavoriteFood(food.id) },
                                                        modifier = Modifier.size(36.dp).testTag("delete_favorite_${food.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Favorite,
                                                            contentDescription = "Remove Favorite",
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        SuggestionChip(
                                                            onClick = {},
                                                            label = { Text(food.category, fontSize = 9.sp) }
                                                        )
                                                        Text(
                                                            text = "‚Ä¢ ${food.calories} kcal",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = "GH‚Çµ ${"%.2f".format(food.price)}",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Button(
                                                    onClick = {
                                                        if (!food.isAvailable) {
                                                            reorderFeedbackMessage = "This food item is currently out of stock."
                                                            isErrorFeedback = true
                                                            return@Button
                                                        }
                                                        if (studentWalletBalance < food.price) {
                                                            reorderFeedbackMessage = "Insufficient balance! Please top up your Smart Wallet."
                                                            isErrorFeedback = true
                                                            return@Button
                                                        }
                                                        submissionConfirmFood = food
                                                         submissionConfirmQuantity = 1
                                                         submissionConfirmPaymentMode = "WALLET"
                                                         submissionConfirmCallback = {
                                                             reorderProcessingId = food.id
                                                             viewModel.placeOrder(foodItem = food, quantity = 1, useWallet = true) { success ->
                                                                 reorderProcessingId = null
                                                                 submissionIsProcessing = false
                                                                 showSubmissionConfirmation = false
                                                                 if (success) {
                                                                     HapticHelper.notification(context, "SUCCESS")
                                                                     reorderFeedbackMessage = "Successfully pre-ordered ${food.name} with 1-Click!"
                                                                     isErrorFeedback = false
                                                                     ordersSubTab = 0
                                                                 } else {
                                                                     HapticHelper.notification(context, "ERROR")
                                                                     reorderFeedbackMessage = "Failed to place order. Connection busy."
                                                                     isErrorFeedback = true
                                                                 }
                                                             }
                                                         }
                                                         submissionIsProcessing = false
                                                         showSubmissionConfirmation = true
                                                        if (false) { val success = false
                                                            reorderProcessingId = null
                                                            if (success) {
                                                                reorderFeedbackMessage = "Successfully pre-ordered ${food.name} with 1-Click!"
                                                                isErrorFeedback = false
                                                                ordersSubTab = 0
                                                            } else {
                                                                reorderFeedbackMessage = "Failed to place order. Connection busy."
                                                                isErrorFeedback = true
                                                            }
                                                        }
                                                    },
                                                    enabled = !isProcessing,
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    shape = RoundedCornerShape(12.dp),
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("one_click_reorder_button_${food.id}")
                                                ) {
                                                    if (isProcessing) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("1-Click Smart Re-order", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
                2 -> {
                    // ATU CALORIE & HEALTH PLANNING CENTER (ADVANCED PAGE)
                    val nutritionCoachingText by viewModel.nutritionCoachingText.collectAsStateWithLifecycle()
                    val isAnalyzingNutrition by viewModel.isAnalyzingNutrition.collectAsStateWithLifecycle()

                    val calorieLoggedSum = loggedNutritionMeals.sumOf { food ->
                        val macro = getNutritionalProfile(food.first)
                        val kcal = macro.first * 4f + macro.second * 4f + macro.third * 9f
                        kcal.toDouble()
                    }.toFloat()

                    val proteinLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).first.toDouble() }.toFloat()
                    val carbsLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).second.toDouble() }.toFloat()
                    val fatLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).third.toDouble() }.toFloat()

                    LazyColumn(
                        state = nutritionScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    text = "ATU Campus Health & Nutrition Portal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Align your physical wellness goals with your cafeteria dining habits dynamically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Success notification banner if any
                        successNutritionMessage?.let { msg ->
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }

                        // Circular rings and calorie indicators
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Circular dial
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(110.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { (calorieLoggedSum / dailyCalorieTarget).coerceIn(0f, 1f) },
                                            modifier = Modifier.size(110.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 10.dp,
                                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${calorieLoggedSum.toInt()}",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "/ ${dailyCalorieTarget.toInt()} kcal",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Controls & Goal setting
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Daily Calorie Budget",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Maintain active study/sports metabolism levels.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Slider(
                                            value = dailyCalorieTarget,
                                            onValueChange = { dailyCalorieTarget = it.toInt().toFloat() },
                                            valueRange = 1500f..3500f,
                                            steps = 19
                                        )

                                        Text(
                                            text = "Adjusted Target: ${dailyCalorieTarget.toInt()} kcal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Macro Breakdown linear bars
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Macro-Nutrient Performance Trackers", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    // Protein tracker
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("üçó Protein Intake", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${proteinLogged.toInt()}g / 130g target", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (proteinLogged / 130f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0xFF4CAF50),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    // Carbs tracker
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("üåæ Carb Fuel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${carbsLogged.toInt()}g / 280g limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (carbsLogged / 280f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0xFFFF9800),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    // Fat tracker
                                    Column {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("ü•ë Core Fats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${fatLogged.toInt()}g / 80g limit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { (fatLogged / 80f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = Color(0xFFE91E63),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                "üí° Gemini Sport-Nutrition Coach",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB300),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Click to evaluate your current logged macros (Protein, Carbs, Fats, and daily Kcal target) against active campus cafeteria menus using Google Gemini AI.",
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isAnalyzingNutrition) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                                            Text(
                                                "Formulating athletic calorie directives...",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.runNutritionCoaching(
                                                    dailyTargetKcal = dailyCalorieTarget,
                                                    currentKcal = calorieLoggedSum,
                                                    protein = proteinLogged,
                                                    carbs = carbsLogged,
                                                    fat = fatLogged,
                                                    availableFoodItems = allFoodItems
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("Ask Gemini Advisor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    nutritionCoachingText?.let { coachMsg ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = coachMsg,
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Interactive AI-driven Recommendation Engine
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ai_recommendations_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "AI Recommendations",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                        Text(
                                            text = "Gemini AI Student Recommendation Engine",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Get personalized culinary recommendations curated by Gemini AI based on your past ATU cafeteria order history and your current dietary goals.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Text(
                                        text = "Select Current Dietary Goal:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    
                                    val prefs = listOf("High Protein / Gains", "Vegan / Plant-Based", "Low Calorie / Fitness", "Brain Focus / Studying", "Budget Friendly")
                                    var selectedPref by remember { mutableStateOf("High Protein / Gains") }
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        items(prefs) { pref ->
                                            FilterChip(
                                                selected = selectedPref == pref,
                                                onClick = { selectedPref = pref },
                                                label = { Text(pref, fontSize = 10.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                                                ),
                                                modifier = Modifier.testTag("dietary_chip_${pref.replace(" ", "_").lowercase()}")
                                            )
                                        }
                                    }
                                    
                                    val isGeneratingRecs by viewModel.isGeneratingStudentRecommendations.collectAsStateWithLifecycle()
                                    val recsText by viewModel.studentRecommendations.collectAsStateWithLifecycle()
                                    
                                    if (isGeneratingRecs) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.5.dp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                "Analyzing historical trends & formulating culinary recommendations...",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.runStudentRecommendations(
                                                    studentName = currentUser?.fullName ?: "Student",
                                                    pastOrders = studentOrders,
                                                    dietaryPreferences = selectedPref,
                                                    availableFoodItems = allFoodItems
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary,
                                                contentColor = MaterialTheme.colorScheme.onTertiary
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("generate_ai_recs_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generate AI Recommendations", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    recsText?.let { content ->
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = content,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.fillMaxWidth().testTag("ai_recs_content")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Logging logs & suggestions header
                        item {
                            Text(
                                text = "ATU Smart Nutri-Scan meal suggestor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        item {
                            // Filter goals
                            val goals = listOf("All Goals", "High Protein gains", "Low Calorie snack", "Budget Healthy")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(goals) { goal ->
                                    FilterChip(
                                        selected = selectedGoalFilter == goal,
                                        onClick = { selectedGoalFilter = goal },
                                        label = { Text(goal, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        // Filtered suggestions
                        val filteredSuggestList = allFoodItems.filter { food ->
                            val vendor = allVendors.find { it.id == food.vendorId }
                            val vendorIsOpen = vendor?.isOpen ?: true
                            vendorIsOpen && when (selectedGoalFilter) {
                                "High Protein gains" -> {
                                    val triple = getNutritionalProfile(food)
                                    triple.first >= 15f
                                }
                                "Low Calorie snack" -> {
                                    val triple = getNutritionalProfile(food)
                                    val kc = triple.first * 4 + triple.second * 4 + triple.third * 9
                                    kc < 450f
                                }
                                "Budget Healthy" -> {
                                    food.price < 35.0
                                }
                                else -> true
                            }
                        }

                        if (filteredSuggestList.isEmpty()) {
                            item {
                                Text("No matching dietary items found inside active campus stands.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(filteredSuggestList) { food ->
                                val triple = getNutritionalProfile(food)
                                val kcalValue = triple.first * 4f + triple.second * 4f + triple.third * 9f
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = food.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${kcalValue.toInt()} kcal",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Protein: ${triple.first.toInt()}g ‚Ä¢ Carbs: ${triple.second.toInt()}g ‚Ä¢ Fat: ${triple.third.toInt()}g",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "GH‚Çµ ${"%.2f".format(food.price)} ‚Ä¢ ${food.category}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = {
                                                    loggedNutritionMeals.add(food to System.currentTimeMillis())
                                                    successNutritionMessage = "Healthy choice logged! Added ${kcalValue.toInt()} kcal to your board."
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                            ) {
                                                Text("Log Food", fontSize = 10.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    selectedFoodForOrder = food
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                                            ) {
                                                Text("Order Now", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Log history footer
                        item {
                            Text("Today's Nutritional Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        if (loggedNutritionMeals.isEmpty()) {
                            item {
                                Text("No foods logged in active health system today.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(loggedNutritionMeals) { pair ->
                                val fl = pair.first
                                val triple = getNutritionalProfile(fl)
                                val kcalValue = triple.first * 4f + triple.second * 4f + triple.third * 9f
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(fl.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Macros: P:${triple.first.toInt()}g C:${triple.second.toInt()}g F:${triple.third.toInt()}g", fontSize = 10.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "+${kcalValue.toInt()} kcal",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // CAMPUS PRE-ORDER RESERVATION SCHEDULER (ADVANCED PAGE)
                    var reserveFoodSelection by remember { mutableStateOf<FoodItem?>(allFoodItems.firstOrNull()) }
                    var reserveQuantity by remember { mutableIntStateOf(1) }
                    var reserveTimeSlot by remember { mutableStateOf("Mid-day Class break: 11:45 AM") }
                    var reserveSpecs by remember { mutableStateOf("") }
                    var reservePayWithWallet by remember { mutableStateOf(true) }
                    var reserveMessage by remember { mutableStateOf<String?>(null) }
                    var reserveErrorMsg by remember { mutableStateOf<String?>(null) }

                    LazyColumn(
                        state = prepScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column {
                                Text(
                                    text = "ATU Line-Bypass Scheduling Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Schedule meal preparation up to 48 hours early. Arrive and flash your verification barcode to grab your hot plate.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Notifications feedback
                        reserveMessage?.let { msg ->
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }

                        reserveErrorMsg?.let { msg ->
                            item {
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(msg, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                    }
                                }
                            }
                        }

                        // Scheduler setup Card form
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "Setup Future Meal Reservation",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Pick food item
                                    Text("1. SELECT DISH SPECIFICATION:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    if (allFoodItems.isEmpty()) {
                                        Text("No campus food menu items seeded yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        var showDropMenu by remember { mutableStateOf(false) }
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { showDropMenu = true },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text(reserveFoodSelection?.name ?: "Select Food item...", fontWeight = FontWeight.Bold)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = showDropMenu,
                                                onDismissRequest = { showDropMenu = false }
                                            ) {
                                                allFoodItems.forEach { item ->
                                                    DropdownMenuItem(
                                                        text = { Text("${item.name} ‚Ä¢ GH‚Çµ ${"%.2f".format(item.price)}") },
                                                        onClick = {
                                                            reserveFoodSelection = item
                                                            showDropMenu = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Picking time-slot
                                    Text("2. DEFINE PICKS TARGET SLOT WINDOW:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    val slots = listOf(
                                        "Breakfast slot: 8:30 AM",
                                        "Mid-day Class break: 11:45 AM",
                                        "Afternoon recess: 2:30 PM",
                                        "Evening prep sessions: 5:45 PM",
                                        "Late night snack: 8:15 PM"
                                    )
                                    var showSlotsDropMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(
                                            onClick = { showSlotsDropMenu = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(reserveTimeSlot, fontWeight = FontWeight.Bold)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = showSlotsDropMenu,
                                            onDismissRequest = { showSlotsDropMenu = false }
                                        ) {
                                            slots.forEach { slot ->
                                                DropdownMenuItem(
                                                    text = { Text(slot) },
                                                    onClick = {
                                                        reserveTimeSlot = slot
                                                        showSlotsDropMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Pick Quantity
                                    Text("3. CULINARY QUANTITY:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(onClick = { if (reserveQuantity > 1) reserveQuantity-- }) {
                                            Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("$reserveQuantity", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 14.dp))
                                        TextButton(onClick = { if (reserveQuantity < 5) reserveQuantity++ }) {
                                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Custom specs
                                    Text("4. SPECIAL INSTRUCTIONS / INTENT:", fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                    OutlinedTextField(
                                        value = reserveSpecs,
                                        onValueChange = { reserveSpecs = it },
                                        placeholder = { Text("e.g. Extra spicy, eco-packing, serving temperature", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Payment toggle option
                                    val currentCost = (reserveFoodSelection?.price ?: 0.0) * reserveQuantity
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = reservePayWithWallet, onCheckedChange = { reservePayWithWallet = it })
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text("Settle Pay securely via Virtual Student Wallet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Price: GH‚Çµ ${"%.2f".format(currentCost)} ‚Ä¢ Smart Wallet Balance: GH‚Çµ ${"%.2f".format(studentWalletBalance)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Large Action lock reservation button
                                    Button(
                                        onClick = {
                                            reserveMessage = null
                                            reserveErrorMsg = null
                                            val foodSelectedVal = reserveFoodSelection
                                            if (foodSelectedVal == null) {
                                                reserveErrorMsg = "Please pick a valid campus food item."
                                            } else {
                                                val cost = foodSelectedVal.price * reserveQuantity
                                                if (reservePayWithWallet) {
                                                    if (studentWalletBalance >= cost) {
                                                        // Deduct using viewModel
                                                        viewModel.rechargeWallet(-cost)
                                                        scheduledReservations.add(
                                                            ScheduledMeal(
                                                                id = (1000..9999).random(),
                                                                foodItem = foodSelectedVal,
                                                                quantity = reserveQuantity,
                                                                targetTime = reserveTimeSlot.substringAfter(": "),
                                                                dateLabel = "Today",
                                                                specs = reserveSpecs.ifBlank { "No special dietary options defined." },
                                                                isPaid = true,
                                                                barcodeSeed = "REC-ATU-${(1000..9999).random()}"
                                                            )
                                                        )
                                                        reserveMessage = "Campus Pre-Order Booking Lock Saved! Wallet debited."
                                                        reserveSpecs = ""
                                                        reserveQuantity = 1
                                                    } else {
                                                        reserveErrorMsg = "Your available ATU Virtual Balance is insufficient. Please reload first!"
                                                    }
                                                } else {
                                                    // POD
                                                    scheduledReservations.add(
                                                        ScheduledMeal(
                                                            id = (1000..9999).random(),
                                                            foodItem = foodSelectedVal,
                                                            quantity = reserveQuantity,
                                                            targetTime = reserveTimeSlot.substringAfter(": "),
                                                            dateLabel = "Today",
                                                            specs = reserveSpecs.ifBlank { "No special dietary options defined." },
                                                            isPaid = false,
                                                            barcodeSeed = "REC-ATU-${(1000..9999).random()}"
                                                        )
                                                    )
                                                    reserveMessage = "Campus pre-order scheduled securely (Pay-on-delivery)!"
                                                    reserveSpecs = ""
                                                    reserveQuantity = 1
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFAF9F6),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Book Future Meal Pre-Order", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // List of scheduled bookings
                        item {
                            Text("My Scheduled College Bookings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        if (scheduledReservations.isEmpty()) {
                            item {
                                Text("No campus meal reservations recorded currently.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(scheduledReservations) { booking ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(booking.foodItem.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text("Qty: ${booking.quantity} ‚Ä¢ Paid Price: GH‚Çµ ${"%.2f".format(booking.foodItem.price * booking.quantity)}", fontSize = 11.sp)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (booking.isPaid) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (booking.isPaid) "PRE-PAID" else "PAY-ON-DELIVERY",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = "‚è∞ Pick target: ${booking.dateLabel} at [ ${booking.targetTime} ]",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Instructions: ${booking.specs}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // QR / Barcode aesthetic Canvas Representation
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            // Canvas Barcode
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier
                                                    .size(width = 110.dp, height = 30.dp)
                                                    .background(Color.White)
                                            ) {
                                                val spaceWidth = size.width / 18f
                                                for (i in 0 until 18) {
                                                    if (i % 2 == 0) {
                                                        drawRect(
                                                            color = Color.Black,
                                                            topLeft = androidx.compose.ui.geometry.Offset(i * spaceWidth, 0f),
                                                            size = androidx.compose.ui.geometry.Size(
                                                                width = if (i % 3 == 0) spaceWidth * 1.5f else spaceWidth * 0.7f,
                                                                height = size.height
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = booking.barcodeSeed,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Text(
                                                    text = "Flash at vendor scanner for express bypass validation.",
                                                    fontSize = 8.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Cancel reservation
                                        TextButton(
                                            onClick = {
                                                scheduledReservations.remove(booking)
                                                if (booking.isPaid) {
                                                    viewModel.rechargeWallet(booking.foodItem.price * booking.quantity)
                                                }
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Cancel Book & Refund", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Smart Wallet & ID Tab
                    val scope = rememberCoroutineScope()


                    var selectedGatewayPayMethod by remember { mutableStateOf("MOMO") } // "MOMO", "CARD", "BANK", "PAYPAL"
                    var selectedMomoOperator by remember { mutableStateOf("MTN MoMo") }
                    var gatewayMomoPin by remember { mutableStateOf("") }
                    var gatewayCardNumber by remember { mutableStateOf("") }
                    var gatewayCardName by remember { mutableStateOf("") }
                    var gatewayCardExpiry by remember { mutableStateOf("") }
                    var gatewayCardCvv by remember { mutableStateOf("") }
                    var selectedBankName by remember { mutableStateOf("Ecobank Ghana") }
                    var gatewayBankAccount by remember { mutableStateOf("") }
                    var gatewayBankPin by remember { mutableStateOf("") }
                    var gatewayPaypalEmail by remember { mutableStateOf("") }
                    var gatewayPaypalPassword by remember { mutableStateOf("") }
                    var gatewayErrorMessage by remember { mutableStateOf<String?>(null) }
                    var gatewayTransactionStep by remember { mutableStateOf("") }
                    var helpSubject by remember { mutableStateOf("") }
                    var helpMessage by remember { mutableStateOf("") }
                    var helpSubmitted by remember { mutableStateOf(false) }

                    LazyColumn(
                        state = walletScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Text(
                                text = "ATU Smart Portal & ID Hub",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // 1. Sleek Digital Student ID Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "ACCRA TECHNICAL UNIVERSITY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                "OFFICIAL DIGITAL STUDENT ID",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Mock Photo Frame
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column {
                                            Text(
                                                text = currentUser?.fullName ?: "Traditional Student",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "ID: ${currentUser?.info ?: "ATU-492049"}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = "Role: Student Registry Member",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Dynamic Barcode Visual representation
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(30.dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                            val lineCount = 38
                                            val spaceBetween = size.width / lineCount
                                            for (i in 0 until lineCount) {
                                                val strokeWidth = if (i % 3 == 0) 3.dp.toPx() else if (i % 2 == 0) 1.dp.toPx() else 1.5.dp.toPx()
                                                val color = if (i % 5 == 0) Color.Transparent else Color.Black
                                                drawLine(
                                                    color = color,
                                                    start = androidx.compose.ui.geometry.Offset(i * spaceBetween, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(i * spaceBetween, size.height),
                                                    strokeWidth = strokeWidth
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 1a. ATU Counter Check-In Card (Capacitor Barcode Scanner Plugin)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showGeneralCheckInScanner = true }
                                    .testTag("counter_checkin_scanner_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Scan Counter QR",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Self Check-In at Counter",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Verify presence or register check-in using the Capacitor Barcode Scanner plugin.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Open Scanner",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // 1b. Loyalty Point Club Card
                        item {
                            val liveAvailablePoints = loyaltySummary?.loyalty_points_balance ?: availablePoints

                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("loyalty_club_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("‚≠ê", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ATU Student Loyalty Club",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        androidx.compose.material3.Badge(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ) {
                                            Text("Loyal Student Level", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    val tierLabel: String
                                    val tierBadgeIcon: String
                                    val tierColorVal: Color
                                    val nextPointsNeeded: Int
                                    val progressPercentage: Float

                                    val remoteTier = loyaltySummary?.tier
                                    if (remoteTier != null) {
                                        tierLabel = remoteTier
                                        if (remoteTier.contains("Platinum", ignoreCase = true)) {
                                            tierBadgeIcon = "üèÜ"
                                            tierColorVal = Color(0xFF3F51B5)
                                            nextPointsNeeded = loyaltySummary?.points_needed_for_next_tier ?: 0
                                            progressPercentage = 1.0f
                                        } else if (remoteTier.contains("Gold", ignoreCase = true)) {
                                            tierBadgeIcon = "ü•á"
                                            tierColorVal = Color(0xFFFFB300)
                                            nextPointsNeeded = loyaltySummary?.points_needed_for_next_tier ?: 0
                                            val earnedInTier = liveAvailablePoints - 250
                                            progressPercentage = (earnedInTier / 250f).coerceIn(0f, 1f)
                                        } else if (remoteTier.contains("Silver", ignoreCase = true)) {
                                            tierBadgeIcon = "ü•à"
                                            tierColorVal = Color(0xFF9E9E9E)
                                            nextPointsNeeded = loyaltySummary?.points_needed_for_next_tier ?: 0
                                            val earnedInTier = liveAvailablePoints - 100
                                            progressPercentage = (earnedInTier / 150f).coerceIn(0f, 1f)
                                        } else {
                                            tierBadgeIcon = "ü•â"
                                            tierColorVal = Color(0xFFCD7F32)
                                            nextPointsNeeded = loyaltySummary?.points_needed_for_next_tier ?: 0
                                            progressPercentage = (liveAvailablePoints / 100f).coerceIn(0f, 1f)
                                        }
                                    } else {
                                        if (liveAvailablePoints < 100) {
                                            tierLabel = "Bronze Tier"
                                            tierBadgeIcon = "ü•â"
                                            tierColorVal = Color(0xFFCD7F32)
                                            nextPointsNeeded = 100
                                            progressPercentage = liveAvailablePoints / 100f
                                        } else if (liveAvailablePoints < 300) {
                                            tierLabel = "Silver Tier"
                                            tierBadgeIcon = "ü•à"
                                            tierColorVal = Color(0xFF9E9E9E)
                                            nextPointsNeeded = 300
                                            progressPercentage = (liveAvailablePoints - 100) / 200f
                                        } else {
                                            tierLabel = "Gold Tier"
                                            tierBadgeIcon = "ü•á"
                                            tierColorVal = Color(0xFFFFB300)
                                            nextPointsNeeded = 300
                                            progressPercentage = 1.0f
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(tierColorVal.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(tierBadgeIcon, fontSize = 32.sp)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = tierLabel,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = tierColorVal
                                                )
                                                Text(
                                                    text = if (remoteTier != null) {
                                                        if (nextPointsNeeded == 0) "Max Level" else "${(progressPercentage * 100).toInt()}%"
                                                    } else if (liveAvailablePoints < 300) "${(progressPercentage * 100).toInt()}%" else "Max Level",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tierColorVal
                                                )
                                            }
                                            val progressLabelText = if (remoteTier != null) {
                                                if (nextPointsNeeded == 0) "Ultimate tier achieved!" else "$liveAvailablePoints / ${liveAvailablePoints + nextPointsNeeded} pts to next tier"
                                            } else if (liveAvailablePoints < 300) {
                                                "$liveAvailablePoints / $nextPointsNeeded pts to next tier"
                                            } else {
                                                "Ultimate Gold status achieved!"
                                            }
                                            Text(
                                                text = progressLabelText,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = progressPercentage,
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = tierColorVal,
                                                trackColor = tierColorVal.copy(alpha = 0.2f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Earn points automatically with every meal ordered! Convert points back into virtual coins anytime.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Net Redeemable Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                            Text(
                                                text = "$liveAvailablePoints Points",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    HapticHelper.impact(context)
                                                    showRewardsCatalogDialog = true
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("open_rewards_catalog_wallet_btn")
                                            ) {
                                                Text("Catalog üéÅ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { 
                                                    HapticHelper.notification(context, "SUCCESS")
                                                    viewModel.redeemLoyaltyPoints(100) 
                                                },
                                                enabled = liveAvailablePoints >= 100,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.testTag("redeem_loyalty_points_btn")
                                            ) {
                                                Text("Redeem 100 pts\n(for GH‚Çµ 5.00)", fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val totalEarnedText = loyaltySummary?.total_spent_all_time?.let { "Total spent: GH‚Çµ ${"%.2f".format(it)}" } ?: "Total points earned historically: $currentTotalPoints pts"
                                        Text(totalEarnedText, fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                        Text("Spent/Redeemed: ${loyaltySummary?.history?.filter { it.type == "REDEEM" }?.sumOf { it.points } ?: redeemedPoints} pts", fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }

                        // 1c. Detailed Loyalty Points Earning History Breakdown
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("loyalty_points_history_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Loyalty Points Earning History",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Breakdown",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    val historyItems = loyaltySummary?.history
                                    if (historyItems != null && historyItems.isNotEmpty()) {
                                        historyItems.forEach { item ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(item.description, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(
                                                        text = item.date,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    val sign = if (item.type.uppercase() == "REDEEM") "-" else "+"
                                                    val color = if (item.type.uppercase() == "REDEEM") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                                    Text("$sign${item.points} PTS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = color)
                                                    Text(item.status, fontSize = 9.sp, color = if (item.status.uppercase() == "COMPLETED") androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    } else {
                                        val completedOrders = studentOrders.filter { it.status.uppercase() == "COMPLETED" }
                                        if (completedOrders.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    "No completed orders yet. Complete an order to earn 25 pts!",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            completedOrders.forEach { order ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text("Order #${order.id} ‚Ä¢ ${order.foodName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        Text(
                                                            text = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.US).format(java.util.Date(order.orderTimestamp)),
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("+25 PTS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                                                        Text("Completed", fontSize = 9.sp, color = androidx.compose.ui.graphics.Color(0xFF2E7D32))
                                                    }
                                                }
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Interactive Digital Smart Card Wallet
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("ATU Smart Wallet Card", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        }
                                        Text(
                                            "Secure OTP System",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    val isLowBalance = studentWalletBalance < 15.0

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Current virtual smart-coin balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "GH‚Çµ ${"%.2f".format(studentWalletBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = if (isLowBalance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )

                                    if (isLowBalance) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("wallet_low_balance_warning"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Warning,
                                                        contentDescription = "Warning",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "Low Balance Alert",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                        Text(
                                                            text = "Your funds have fallen below GH‚Çµ 15.00. Top up to ensure uninterrupted checkouts.",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        topUpSuccess = false
                                                        topUpAmount = ""
                                                        topUpPhone = ""
                                                        showTopUpDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                                    modifier = Modifier.height(32.dp).testTag("low_balance_action_top_up")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Top Up", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                topUpSuccess = false
                                                topUpAmount = ""
                                                topUpPhone = ""
                                                showTopUpDialog = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Top-Up Wallet", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.rechargeWallet(0.0) // generates log entry
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Sync Account", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // MONTHLY BUDGET & SPENDING TRACKER DASHBOARD
                        item {
                            val monthlyBudgetLimit by viewModel.monthlyBudgetLimit.collectAsStateWithLifecycle()
                            val spentThisMonth = remember(studentOrders) {
                                val cal = java.util.Calendar.getInstance()
                                val currentMonth = cal.get(java.util.Calendar.MONTH)
                                val currentYear = cal.get(java.util.Calendar.YEAR)

                                studentOrders.filter { o ->
                                    if (o.status.uppercase() == "COMPLETED") {
                                        val oCal = java.util.Calendar.getInstance()
                                        oCal.timeInMillis = o.orderTimestamp
                                        val oMonth = oCal.get(java.util.Calendar.MONTH)
                                        val oYear = oCal.get(java.util.Calendar.YEAR)
                                        currentMonth == oMonth && currentYear == oYear
                                    } else {
                                        false
                                    }
                                }.sumOf { it.totalPrice }
                            }

                            val budgetPercentage = if (monthlyBudgetLimit > 0.0) spentThisMonth / monthlyBudgetLimit else 0.0
                            val isLimitExceeded = budgetPercentage >= 1.0
                            val isNearLimit = budgetPercentage >= 0.8 && budgetPercentage < 1.0

                            val statusLabel = when {
                                isLimitExceeded -> "Budget Exceeded üö®"
                                isNearLimit -> "Near Budget Limit ‚ö†Ô∏è"
                                else -> "Within Budget üü¢"
                            }

                            val statusColor = when {
                                isLimitExceeded -> MaterialTheme.colorScheme.error
                                isNearLimit -> Color(0xFFF57C00) // Orange
                                else -> Color(0xFF2E7D32) // Green
                            }

                            val adviceText = when {
                                isLimitExceeded -> "üí° Smart Tip: You've exceeded your budget limit of GH‚Çµ ${"%.2f".format(monthlyBudgetLimit)}! Avoid purchasing extra sides and focus on essential low-cost meal combos."
                                isNearLimit -> "üí° Smart Tip: You've spent 80%+ of your budget. Consider swapping premium protein selections for nutritious budget-friendly alternatives like beans or local eggs."
                                else -> "üí° Smart Tip: You are doing great! Keep planning your meals ahead of time and tracking your cafeteria spending."
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("monthly_budget_tracker_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Monthly Spending & Budget",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                showEditBudgetDialog = true
                                            },
                                            modifier = Modifier.size(24.dp).testTag("edit_budget_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Budget",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Budget Figures
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Spent this month", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "GH‚Çµ ${"%.2f".format(spentThisMonth)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = statusColor
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Monthly Budget Limit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                "GH‚Çµ ${"%.2f".format(monthlyBudgetLimit)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Progress Bar
                                    LinearProgressIndicator(
                                        progress = { budgetPercentage.toFloat().coerceIn(0.0f, 1.0f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = statusColor,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Status: $statusLabel",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                        val percentageText = (budgetPercentage * 100).toInt()
                                        Text(
                                            text = "$percentageText% used",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Advice Alert Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = statusColor.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = adviceText,
                                            fontSize = 10.sp,
                                            lineHeight = 13.sp,
                                            color = statusColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Category Spending Breakdown Section
                                    val monthlyCompletedOrders = remember(studentOrders) {
                                        studentOrders.filter { o ->
                                            if (o.status.uppercase() == "COMPLETED") {
                                                val oCal = java.util.Calendar.getInstance()
                                                oCal.timeInMillis = o.orderTimestamp
                                                val oMonth = oCal.get(java.util.Calendar.MONTH)
                                                val oYear = oCal.get(java.util.Calendar.YEAR)
                                                val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
                                                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                                currentMonth == oMonth && currentYear == oYear
                                            } else {
                                                false
                                            }
                                        }
                                    }

                                    val categorySpending = remember(monthlyCompletedOrders, allFoodItems) {
                                        val map = mutableMapOf<String, Double>()
                                        monthlyCompletedOrders.forEach { order ->
                                            val foodItem = allFoodItems.find { it.id == order.foodItemId }
                                            val cat = foodItem?.category?.ifBlank { "Uncategorized" } ?: "Main Course"
                                            map[cat] = (map[cat] ?: 0.0) + order.totalPrice
                                        }
                                        map.toList().sortedByDescending { it.second }
                                    }

                                    if (categorySpending.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Spending by Category",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        categorySpending.forEach { (cat, amt) ->
                                            val catPercentage = if (spentThisMonth > 0) amt / spentThisMonth else 0.0
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    val emoji = when (cat.uppercase()) {
                                                        "BREAKFAST" -> "üç≥"
                                                        "DRINKS" -> "ü•§"
                                                        "SNACKS" -> "üç™"
                                                        "DESSERT" -> "üç∞"
                                                        else -> "üçõ"
                                                    }
                                                    Text(emoji, fontSize = 12.sp)
                                                    Text(cat, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "GH‚Çµ ${"%.2f".format(amt)}",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "${(catPercentage * 100).toInt()}%",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2.2 Wallet Transaction History Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("wallet_transactions_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "Wallet Transaction History",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Deposits, meal spending & statements",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                com.example.ui.util.generatePdfWalletTransactionReport(
                                                    context = context,
                                                    studentName = currentUser?.fullName ?: "Student",
                                                    studentIndex = currentUser?.username ?: "ATU-STUDENT",
                                                    currentBalance = currentUser?.balance ?: 0.0,
                                                    transactions = userWalletTransactions
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("export_wallet_pdf_button")
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = "Export PDF",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (userWalletTransactions.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No wallet transaction logs recorded yet.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        userWalletTransactions.forEach { tx ->
                                            val isPositive = tx.amount >= 0
                                            val color = if (isPositive) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)
                                            val prefix = if (isPositive) "+" else ""
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tx.details, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                                    Text("Ref: ${tx.reference} ‚Ä¢ ${java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.US).format(java.util.Date(tx.timestamp))}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(
                                                    "${prefix}GH‚Çµ ${"%.2f".format(tx.amount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = color
                                                )
                                            }
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }

                        // 2.3 Laravel Notification Channels & Dispatcher History
                        item {
                            val emailEnabled by viewModel.isEmailNotificationEnabled.collectAsStateWithLifecycle()
                            val pushEnabled by viewModel.isPushNotificationEnabled.collectAsStateWithLifecycle()
                            val dispatchedEmails by viewModel.dispatchedEmails.collectAsStateWithLifecycle()
                            val dispatchedPushes by viewModel.dispatchedPushNotifications.collectAsStateWithLifecycle()
                            var isLogHistoryExpanded by remember { mutableStateOf(false) }

                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("laravel_notifications_card"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Laravel Notification Channels",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (LaravelClientManager.isLaravelEnabled) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.secondaryContainer,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (LaravelClientManager.isLaravelEnabled) "Laravel Active" else "Offline Simulation",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (LaravelClientManager.isLaravelEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Manage SMTP Email and FMC Push alert dispatches triggered by Laravel in response to order status changes from pending to ready.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Channel 1: SMTP Email Channel
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("üìß", fontSize = 18.sp)
                                            Column {
                                                Text("Laravel SMTP Mail Notification", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("Dispatches high-fidelity emails to user inbox when chef completes meal preparation.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = emailEnabled,
                                            onCheckedChange = { viewModel.isEmailNotificationEnabled.value = it }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Channel 2: FCM Push Channel
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("üì±", fontSize = 18.sp)
                                            Column {
                                                Text("Laravel FCM Push Notification", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("Broadcasts immediate high-priority push events to registered client devices.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = pushEnabled,
                                            onCheckedChange = { viewModel.isPushNotificationEnabled.value = it }
                                        )
                                    }

                                    val totalLogsCount = dispatchedEmails.size + dispatchedPushes.size
                                    if (totalLogsCount > 0) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isLogHistoryExpanded = !isLogHistoryExpanded }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "üìú Dispatched Channel Outbox ($totalLogsCount)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Icon(
                                                imageVector = if (isLogHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        if (isLogHistoryExpanded) {
                                            Column(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Combine logs and sort chronologically latest first
                                                val emailLogs = dispatchedEmails.map { it to "EMAIL" }
                                                val pushLogs = dispatchedPushes.map { it to "PUSH" }
                                                val combined = (emailLogs + pushLogs).sortedByDescending {
                                                    if (it.second == "EMAIL") (it.first as com.example.ui.viewmodel.MockEmailNotification).timestamp
                                                    else (it.first as com.example.ui.viewmodel.MockPushNotification).timestamp
                                                }

                                                combined.forEach { record ->
                                                    if (record.second == "EMAIL") {
                                                        val email = record.first as com.example.ui.viewmodel.MockEmailNotification
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4).copy(alpha = 0.4f)),
                                                            border = BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.7f)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(10.dp)) {
                                                                Row(
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Text("üìß SMTP EMAIL DISPATCHED", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFFF57F17))
                                                                    Text(
                                                                        java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.US).format(java.util.Date(email.timestamp)),
                                                                        fontSize = 8.sp,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text("To: ${email.studentEmail}", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                                Text("Subject: ${email.subject}", fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text(
                                                                    email.body,
                                                                    fontSize = 9.sp,
                                                                    color = Color.DarkGray,
                                                                    lineHeight = 11.sp
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        val push = record.first as com.example.ui.viewmodel.MockPushNotification
                                                        Card(
                                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD).copy(alpha = 0.4f)),
                                                            border = BorderStroke(1.dp, Color(0xFF1E88E5).copy(alpha = 0.7f)),
                                                            shape = RoundedCornerShape(8.dp),
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Column(modifier = Modifier.padding(10.dp)) {
                                                                Row(
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Text("üì± FCM PUSH SENT", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF1565C0))
                                                                    Text(
                                                                        java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.US).format(java.util.Date(push.timestamp)),
                                                                        fontSize = 8.sp,
                                                                        color = Color.Gray
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Text("Title: ${push.title}", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                                Text(
                                                                    push.body,
                                                                    fontSize = 9.sp,
                                                                    color = Color.DarkGray
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2b. Profile Settings & Saved Payment Methods Hub
                        item {
                            var isEditingProfile by remember { mutableStateOf(false) }
                            var editFullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
                            var editStudentId by remember { mutableStateOf(currentUser?.student_staff_id ?: "") }
                            var editTelephone by remember { mutableStateOf(currentUser?.telephone ?: "") }
                            var editEmail by remember { mutableStateOf(currentUser?.email ?: "") }
                            var editDietaryPrefs by remember { mutableStateOf(currentUser?.dietaryPreferences ?: "") }
                            val presetDiets = remember { listOf("Vegetarian", "Vegan", "Gluten-Free", "Halal", "Kosher", "Lactose-Free", "Nut-Free") }
                            
                            // Saved payment details (simulated using split token strings or custom state)
                            val loadedPaymentMethods = currentUser?.paymentMethods?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                            var selectedMomoOp by remember { mutableStateOf(if (loadedPaymentMethods.any { it.startsWith("MTN") }) "MTN MoMo" else if (loadedPaymentMethods.any { it.startsWith("Telecel") }) "Telecel Cash" else "AT Money") }
                            var momoPhoneNumber by remember { mutableStateOf(loadedPaymentMethods.firstOrNull { it.matches(Regex("^[0-9]+$")) } ?: editTelephone) }
                            var creditCardNumber by remember { mutableStateOf(loadedPaymentMethods.firstOrNull { it.contains("****") } ?: "4111 **** **** 9823") }
                            var cardExpiry by remember { mutableStateOf("12/28") }
                            
                            var profileSaveSuccess by remember { mutableStateOf<String?>(null) }
                            var profileSaveError by remember { mutableStateOf<String?>(null) }

                            // Synchronize views when user reference changes
                            LaunchedEffect(currentUser) {
                                currentUser?.let {
                                    editFullName = it.fullName
                                    editStudentId = it.student_staff_id ?: ""
                                    editTelephone = it.telephone ?: ""
                                    editEmail = it.email ?: ""
                                    editDietaryPrefs = it.dietaryPreferences ?: ""
                                }
                            }

                            // ‚ôø ATU ACCESSIBILITY & INCLUSIVITY CENTER CARD
                            val highContrastEnabled by viewModel.isHighContrastMode.collectAsStateWithLifecycle()
                            val darkModeEnabled by viewModel.isDarkMode.collectAsStateWithLifecycle()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .testTag("atu_accessibility_center_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("‚ôø", fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "ATU Accessibility & Inclusivity Hub",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Accra Technical University Inclusive Learning Initiative",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // High Contrast Mode Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "High-Contrast Display",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Boosts legibility with optimal light and dark elements for students with visual challenges.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = highContrastEnabled,
                                            onCheckedChange = { 
                                                viewModel.toggleHighContrastMode()
                                                val text = if (!highContrastEnabled) "High Contrast Display Enabled" else "High Contrast Display Disabled"
                                                tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            modifier = Modifier.testTag("toggle_high_contrast_btn")
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Dark Mode Theme Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "Dark Mode Theme",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Toggles dark and light mode settings to reduce eye fatigue under variable lighting conditions.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = darkModeEnabled,
                                            onCheckedChange = { 
                                                viewModel.toggleDarkMode()
                                                val text = if (!darkModeEnabled) "Dark Mode Enabled" else "Dark Mode Disabled"
                                                tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            modifier = Modifier.testTag("toggle_dark_mode_btn")
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Screen Reader Assist Mode Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "Vocal Screen Reader Assist",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Vocally announces active meal preparation and pickup status updates in real-time.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isTtsEnabled,
                                            onCheckedChange = { 
                                                isTtsEnabled = it
                                                val text = if (it) "Vocal Screen Reader Assist Enabled" else "Vocal Screen Reader Assist Disabled"
                                                tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            modifier = Modifier.testTag("toggle_tts_btn")
                                        )
                                    }
                                    
                                    if (isTtsEnabled) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Button(
                                            onClick = {
                                                val textToSpeak = if (activeOrdersList.isEmpty()) {
                                                    "You currently have no active orders in preparation at Accra Technical University Cafeteria."
                                                } else {
                                                    "You have ${activeOrdersList.size} active order updates: $activeOrderStatusStr"
                                                }
                                                tts?.speak(textToSpeak, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("announce_status_btn")
                                        ) {
                                            Icon(Icons.Default.VolumeUp, contentDescription = "Listen to Statuses", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Vocalize Current Active Order Statuses", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("üí°", fontSize = 14.sp)
                                            Text(
                                                "Tip: Screen Reader Assist will vocalize when your order status updates to PREPARING, READY, or completed.",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // üîê BIOMETRIC LOGIN TOGGLE IN DASHBOARD
                                    val bioPrefs = remember { context.getSharedPreferences("cafeteria_cache", android.content.Context.MODE_PRIVATE) }
                                    var biometricEnabled by remember { mutableStateOf(bioPrefs.getBoolean("biometric_enabled", false)) }
                                    var showPinConfirmDialog by remember { mutableStateOf(false) }
                                    var pinConfirmationInput by remember { mutableStateOf("") }
                                    var pinConfirmationError by remember { mutableStateOf<String?>(null) }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(
                                                "Biometric Account Access",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Enable rapid fingerprint or face scan login to secure and access your student account instantly.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = biometricEnabled,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    showPinConfirmDialog = true
                                                } else {
                                                    biometricEnabled = false
                                                    bioPrefs.edit()
                                                        .remove("biometric_username")
                                                        .remove("biometric_pin")
                                                        .putBoolean("biometric_enabled", false)
                                                        .apply()
                                                }
                                            },
                                            modifier = Modifier.testTag("student_dashboard_biometric_switch")
                                        )
                                    }

                                    if (showPinConfirmDialog) {
                                        AlertDialog(
                                            onDismissRequest = { 
                                                showPinConfirmDialog = false 
                                                pinConfirmationInput = ""
                                                pinConfirmationError = null
                                            },
                                            title = { Text("Confirm PIN for Biometrics", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                                            text = {
                                                Column {
                                                    Text(
                                                        "Please confirm your current Access PIN to securely register your biometric credentials.",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(bottom = 12.dp)
                                                    )
                                                    OutlinedTextField(
                                                        value = pinConfirmationInput,
                                                        onValueChange = { pinConfirmationInput = it },
                                                        label = { Text("Access PIN") },
                                                        visualTransformation = PasswordVisualTransformation(),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                    pinConfirmationError?.let {
                                                        Text(
                                                            text = it,
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifxúÏ}€r€H≤‡˚˘äjÆ«AMÀ–≈ñ€Ì∑á")ã€∫∞EZΩ˝§(E#@†$û>~ÿyﬂ›ÿç˚⁄/g~`#&bøg~`Á∂≤
Ç HV
≤,+∫e,‘%3+ÔÈêΩC«æÌZ∂m«ª™«~@?~eŸ¡∆ø M–˚Â'Â_©˝‚”¶“„ﬂ:·x«æG∑‰7ÂÈÒü÷µ6√˜öÆ3∏÷zØ gàÍÅ„5˘BpÏ¯^«&±ÂD'~ºÔbÔ∫æ±Q‚ 7ÿE#çàMÁ˙|É≠à&°O≠cE¯ä¥ú+≈÷â;^co@Íµﬁa„≈ÓﬁÎö>é	∞l6|ÒBcìF‚)]i˘w}◊Æ◊jtœPáõ»â—ã‡OÙ-™˝a{˜ÆFÅ‘ùxC°≥ K7ÚcƒËî˛/ÜïpBå˙Õ;‰M\=.^Dπ@›˙°}HOΩ{óúlY∏t¸nHÜëEl'Æó? ãûq/Åm’Ë¯cBˇ\LËB<<&µÕtY‚£*_Kêæ±ÕΩuﬂ˜]ÇΩÏkâá/]b”W«·ƒ‘q∏SáîN≥ÕgIfYz‹h‰ﬂv”ùn9ÿıØËÿCÏFÂ/:B:x≠fz‰v˙@€@Ñ•∆˛Ñ]∏"]2¡Z«£‹»±Q∑sb°.EA˙∂8ú"|Öœ*∑1˙\RÔóÍø“e}r◊k…Üj\uïä7∂çù(*!ﬁ¿Úâ8HVq˝QÕ≥ ”§ÔXrõ[=K?-7ìOˇ"ıÿ÷˙ÁÔˇ˛ÔËÙ¨’>CΩ~£ˇ±áNN˚ùÉN≥—Ôúû†Êa„‰§}Ñöß'ùœÿßRcﬂ‡y~Ï{˛ƒ≥≈Ex9E!ìÒ%ï√~C„I˜bì”a}‡è-rá«ÅK¨âcMb«µN`g¿∞Áê∏ïjú(;$˝ï”≥⁄ê<”âù;ó!«J£ìÀ;7A©ˆ< a}úË®YuuDú´Q\ﬂŸEU˝–˘W:Ï∂ú«¶|óQ·1]YHYGDWm±O{ˆw:cNÈg¡¥é›`ÑÈ„€÷ŒﬁPÚ•ÚKêÆÈªì±W8‹–q›c|˜≥c«#5µéQ∏ô÷8µÑt‡≈$BY@Õˆ<‚¢âc*&G5µªiHOÈg∂;tÅÈ?¨}™~©è‘s˛ï–qË>GÅ™	@?s- çCiN¢ÿ√jΩÏa`óÑ1äÄUD{6∫Ñâ”ïy›ßêí<v_ƒŒò ülƒvÿt˘ë•qÜbÁ∑+ÿyﬂÎM¬!%ÆsL‚
é`=ÌæQ ]Ä3ˇVÌL◊∫⁄æéRFÔäÓ§Dñ˘ó≈ñΩO‚[B<µ—o(¢Q¨sÆsÂâë≈ﬂVì˛áÑÁ…3Æ—(J.\bôcHFÏ∫§Ãh9w 9ó≤ú;¿v‘üﬁ≠FÍb1EÌ¡5Sì$µì †Ú5∏Òarvb-@/~–íÂòwbH≠Ò$eâàƒEíŒ¶x∑ÜT™æùE§H˘b‹«WıZÏ_]π‰ÇÌœcµ√EâY˛iI
‡âÎ|&Æsà˙Vî
¬_Á…ÀÓá˚,(+˜≈Åñ©3ï•Rﬁ‚DÊ%™”IÏ:±uQe|líx„ƒKî!°ÕFråƒÙp¡å=µSTƒ∞hÑ ’3∏¡(È˙°G¬|»Fm¥ı◊»ua£.¢iìqÇøæmó±ßÄ∫™\ªCO£ˇâ¨‚âåó=)/nëh:cÈÔP≠«f9ØŒ›¬UGî„◊w^À€*¨ ò[∂ÖcfÃ“Wìê§ ∫Có¡óñYç¸}•vW…ô®÷?µ˙â5¨©âC{=Àê@c<!D˙(£ó~°OC.<±‰b4πº–◊◊$hjUrúZ?”ú#:,9AÙ»Ç	4Ÿ7å)a H√ÊZÕ;‚z∑ƒ[/π!·⁄gÙ‚–ø¶s∂¿‚µ©nu{µŒË&C¸+,h‚Bï‰$J“µI…∫:©∫âZÖ/√é™MAOVˇÁÔˇÛ?ÊY‹.X™sË7Íö„®¶KNœWÎ¿é‚AÏ‹‘ÂÃ
=G]ŒÆtπÃtDÒ‘%l*û˛UàÉ—‘äùÿ%Ω1≈Ωò∑˝
P◊J+=84	– $6=G∫nNÆ!4&Ò»∑À´æçW@Ö∂^äB≥a)@˙AêEK*3»â⁄∂Ù*˘˙f·3E’@FÒBÑx›á‹Æåî`Ø»9ƒ· .ø-h^h∫~DxxÕ¸3u|/÷(`,¡t5HëN?6Œ©ÃãÚü√_
GºÄ˙*∏∂µÖŒ∂_úzÓµp4∫Ù©–ÃúûëÙ Z˜˘ë≈™ç©c˘àNå"Mélå%pâÁå\—Q	Ω–	èêT∑Gnj0ÏSW4	?Ñs[XH&ˆΩ5ú∏.¨Ωãj'[ç‹™^Æ’ZïV§µóûHnÜá.	#ﬂ[bvÄH°MiR_eÜ‡©Œ2R-R‡ÊLì\¢E≤ˇ[}™êDÜ#S4!¨R’∂ïÛ‘û÷âÒπI2A}e%6U¯Ñ ˙Ëd¬b|Íœ"‚“[ëÿ«˛ÿ?6càÈ¢ÇëÔë˜ñ3dÈÙ1–‹0~Ÿ5ÙâÛç˘OãX»a∞†GŒ#û∫§.ˆˆ;.jÿvH"ΩXáG»ñî#bƒ>Dâg$ú˘Ï·0;ü'û¡SÓÚ#eˆ˘/ñhkÁNÑ—30691¨$ë/⁄w¡[˙1˝Ä˛ÂÑ”LùÕtÓO$:É'•»›rH$
iåÙ¶ı‰Kº^µL¥êã@ˆ&öÀD}oŸ|O2[¢5>ÀTfŸ°NtB	Ï44êù,≤<Ç¢ÄÄÙ ˆZ≥)ùüß¡ó_a¶7{Z•ÍëáÆWj< £%
“Ag;”ÑæÎ÷EÊˇ'Àz)ì;^6wöìE∏EÊÕ⁄Ü5∆ò¯c+¶WLù2p «,S«IR˚€„ ûÚØ¸∞ç#˙å£xóÖ}ˇÆ*
®EXóxp}BºD}Ì›óÂfQÑÖFÄÕ ô	$û·],≤â2—#fê€Dﬁ> „§Ä@õã.?ñ’nÓ /º\ÂÖáüë\ô¯{F¢OËù?âø3P{˙ÛÀ ≈€æ,H≠¬’mm°’ßt)6:Úßÿç©¸ÏSÏâ=ç¡5ù!ËZ{(¸“"‘D-bF*í/¶Æ†\óﬁ«ÛZ˚X<t˘Ó_l˜/bæ˚“1àyXì®)?ÆçQ‘å.ëlåaÁ≥Qû¥^Ó’èCc'ñ≈MÆéö”_òµ™Ú¨#6»ÑUæR	´ÃC)»tBS™Mn BEâNY(#1V∂ôá¢‡˝áÀ˜Á/†Z*∆0ŒPﬂæ;†∞ˇr{{…˝…ƒñ›ÌÚ˙@!£y–!Õ∑¢4˙Q/û@azèüë[ õ#‘t'óP>„o®æáZxJ	æN‰"›†}GÔÆ ,dËK©˝<´.Ø3Ö∂qË°ow˜P∑ﬂcYÒÑ¢ÙRuóÄ√8Ú7µ"MHk‚yn˝Fï[Á·æµ }ñFÂ◊π!âL¯%bJo2˝·Ωï[.±Öh¿ﬂóÿ!˚>Âº¸Á⁄≥(m”®ƒ‹5pù†æ,ÀÄÂK F"Ñ
Ô+¥|Ïlõ5}îΩR8xñ¡I Vp≤}óÓbY.¶ø‹˚28 h˝H"û≥ƒç™ÔÃà£sÜm˜Œo‡Ì1]>ãX|ánG§Ã-ù¨„áwho{jë÷∫.¶bÕdLëîn4)Q±,zwè˝Å^ÿËò«MªìÃ∏Á∏7‹ILi£Ã–Ã…#ÓáæGi±ÃTıH◊£§O∏è√+Wpƒtﬂ∂ÕùÍûë¡í≥§cñ>ΩÕ	ÈüX˙W,u‚=Ø:_ñ˚Æè„˙⁄ ÍÏ´ —I8 Øæ=‹D;ˆ1ãéFÙµ‡
…∆˛JçmÀ'√g·I_~Ë˙25 ´ﬁ¢g‚JPNπæw}ICD*:∆àÃºEˇÒ◊ø£gø’˛`Ì”2ÿ)Ro|*±≤ôÇæ€˛ÆırWSZP(TëÖı"Ãk≈¢Y8¢ßÅ√n¬Ç:≥ñ”’Í3Âd(Ê‚ôcmÆ¶Ût¨lÈêr#-Sπ∏ÂR˚§ìIı_¡˙“…˚∫âî öànæˆK "0—oÄó|5P«tÕqZ˜¥‘¿PË8±∏5qeé”b«•J™õe
´ÎªÑ|˛æÄ4_î•äñ§.‡w	R◊—%˚ßzÅã‘Ê°?•uÖtÓ]ÿP3ïs/ø—J6Y®¢™MS.W(gvjéuËüøˇ˜ˇZaeõ,T&qîaï[[ËòR]Cd‰Gœı◊Pø6(Ù'—+HnZﬂx@‚Ö\Ñ`µQú˘yÿG˛ZŒJn∂∫XÑå≈bâ36ìçı“∑=»
MyèÕﬁ∞‰ÃWπ˚W≥‡≠‰≥/‚î#6Ù“˛ô
è≥ˆœﬂˇˆË $µB«ªNx‰r≤ùX¯ææ∑V¸Õ*'Y»\ﬂk<…h∂2NVÖcH†ñü¿r¢\–È◊aıVjJ˜Ìh¯bYÁŒˆ£‚ùl9_5Ûd;@π„ˇ˙{b≠€ŸFß√aˇÃ?Q˚˛}@,4áÕO<t=|∫˚∏‰œ›Ø^ ›M$–ˇˆ∏Ñ˘ü©Ë±–‹ı›'!Ùaq–›ØF
≠º±≤Ù”í•‚T”e∑∂+°w‡ácƒöıU\õMTùFs‡W#˜‰&qıÉ˜Ù ©`¶”eÅyW≥=2„Œ«Z…b¯í∏ô‘wêWå”JÿRqü TuÉñΩCÿ_≠ `\˛Uâ±*¿Û[ïAcYø›—jÊQÑfÈÄ∆,fæE©Óq&J†NÎ˛–/‚≥pÏ'¸3Ö}QéÕ˛•√øŸàºrÕ˝·{ÎÆô¬5^dÀûÒäj¶pL‘^úØ”voà∆J§}àf+ì¸C•Qı4…ö(õˇÅÍ∑xäéµÀ¨õUÕ µC®¶,˛Ω 1ˆ0•jõá?@	|™Ã°§ÕPÑûÛhÔC§{\•TÛäã·Vºáj«˛±èNB%‚UmﬂSF+í°äàÈä„t¨lÆ≈ß√zÌ∏¬ˆ∫∂âò–4†W\G#¯w£OøÚ»¥∂ë)?‰ã3—,AÚN‘K ”MòØXåﬁΩK_°?X&–x¯ mrci¶§„dR¸J[—x∑q´¨⁄˘∫JjÊÌÚˆ>£Aw û3≠ó?fïøˇ≥\b‚úg5ÄH`â£|+œffå¨\ç©•’x Ñ¡≤HŒUŸ.˜7∆D5±e˙’√‰Òu±öÿZã~æ‘Ênc¯¸ÇõA}~LIèUΩÁ6Z}nHc∫}Æè¬Ω[ë`]_í)ÈQ·iæÏµ	D](•mÃ
≈Ê≈◊DêáêçvΩq¯
≈Ω>¶~πˆ•ÇR‘Ë9:$ÿçGàÓì•i‹ÀÒpA≈Ù PÏ£Äıu¬.l«‘üÑts|Öd‡è©lfÛ“ƒª¢à¸’õú™I}ÎÔm
îeCÈ¿"ç£ú∞5+dÈº˘1¥+6«˛Ò$Âf©ò∫¢ònTë°´∫Ñ%ÉòzÒLùsBvÚQ∆⁄eÒçY∫Ê◊J£:ºDOYyX∆Æ
Î\}ë∞'3ïÄ'3’
»ö©åÔ¥ô∫Ò9ŒEØˇÜî‡[Y(›Sc≈D±mõöeÈÚÇ≈‚ÖﬂÒ˙~è
ﬁ8JΩ”êYsMë?˝ ÷ÈõReNÍb0rÇãgø¡ﬂñÎﬂípÄ#*dPú\»®Ω ◊‘Em„ìÊ˛î∂ÇñZmãÀ+«ÃÃ¥lœéVM‡¯Á∞¸jO¯°öåïy-œMEwÂò´!Û⁄$ä˝1v0…†:Ë˚¯ED®PÓ—Œ∆X‰◊hVj/@∑ñPŸB†˘Mp#	 K,I‘Ö6™qw˜¬ΩÕ9)J$$±qπı¯Ì≥úO•Ã∞3{˜€ÖfízÂ<5À2:‰ñbq-~,–Û.iS¢√YrFπ‹I®:ãñg±¯%É˝ƒôÍíÒ€HI["DWÄl4N@,q†âáÑ]ıÙ wº¯«äÊ·ÒÈ6q
√7Ò8òD¢†lâë9Y'·†t‰Er◊‹ÒÜæπ”_ÏÏh‰‡{g$Ç0L∏3£…` K∂ßY1 Ñõ\û=|Cz…)*à6Eÿ≥≈q2/$ã`7B—‘åBﬂ£2∂-$?˝Fø∞q¡å⁄a»‰c®‘U^˚è }éﬁÚbqÔ–êÆ•\ñ£1Ω`’µ›e∫ø…Ws˚óà&!AÆ⁄&ïƒB|E,£˚?√à|⁄ø÷˚•˙%[±¯—É*[d–h%?∂ƒ
¿9ƒ5ó|◊·]√≈ÎåßÛ IﬂãƒıﬁrI—:—ï ’`æÂ ;:ãY@Â˛°Ôÿw4ÜÒ#bMã ¡»D÷BRπj‡õ‹Ã‘˜ï±O±´$T
∆ÆhWÈ,t˙aWµ£ÂûX˛Ìä≥⁄⁄B/-‘ÉÀâßÙŒ∫ä–7∞ItM•∫_'NHo5™YíÂ&*'&„5ÃEÆG¢âDÜUÕwÂÇ\◊6T®Á…ÙÓñaÕ2MÓ^´4πµ‘CÅ¢¡Ëå¸:°◊Í≥„^’xÜ»FÒî	ÛãèßÅœ∏—‘äùò“˛C=WÈà9jIJ0˘ãÀ[¨Kf]sl∂&.ç∏ˆÑÌ±„Q'd/]j‡:T≠∏Ú·°ê\—Ô®™as-—“n±ïÁïZåaS®"@¬Ω…Âÿâ]j Óy.˘≤%SKª⁄W–/°ƒnØjö¶]…ò_Wc√Rñ!…≥`2÷_U(}+ÄA1íßÄ™∫xH‹©ÖŒÈ!°áµuZoQ£ˇÒ≈≥ﬂ†ÇÀ˙û¡ÜR ◊7>¡çwEÏ<ΩÔ»”˚zòíÜÂ%a„Œÿ‰ƒù1+£9û"îDÙIÔO‚QI÷èGt∆qˆ¬…ÇÈÌR∂îÙ!	ˇQ≤u°◊í≠-∫ç2CiyåÚﬁ"1VùXW⁄áÚkÇ5o¶&&¢ªÜ«»t^™æ/H¡	c>‘ SÕ_)¡ãq( Ò˛óêﬂNô†!8ì«,À˙¸®3∆wÄ7 ‘ø˙<X£Ï‘ÒñÒfƒF`<6xﬂ≈ﬁu}=ûEöπÔtÕ›˘ÎDªì»<G¨ÈôZÁIBc≈¯àÚ‰ÄAÊ™œ$Ø∂g´ÚSSÊE=†ÂDé£y}3kYTl%∑_‚±5è¨¯zïQlkΩ≤•N∏Pqhﬂb™¥¬∏xäû£ﬁ $Ù‚=#’Ûô¸zL‚–D®ãΩUm1$å/º≈üÔù´	WÄÈÅ¢ô˝–J∫åÆÀ&^ˆ¢dîˇß‹Ô◊è±≠`læYœs3∑ÊæïÏêÒ‡•£âØŒº{GﬂUüõl¶©„≈Z…ˇ7ËóomŒE~EŸ∑àExç4ÙËÈµÊ^≥˛˜å“s?N?+;9':¢
^4‡‹m˛¿˝(0!Ç‘ñn%:≠0°gpz÷iüÙ˝ŒÈ…≈Q„§’k6∫Ì≤3ºu®zÀ¢ÈbÊ ìÌ‘:OBØìN∂Mü5¸BıcˇÃbÁπ›ê∏∆Ú„Ωy≈«;&∂3£˙’Z!–{ãÚP˛éeN;‚∂Ô˜™ß«≤≈áëö‰:WÉúiYÕw«ˆ‰"Lô1KïÚ‡0?Ø∑?À›uÍVÍ%Eæ_…îÇëYÿ™bﬂkÀÀïHêEå€“”Ÿåj7ˆ¥≠¢â≠í¨®ºûD´◊˛˘˚ˇ˛€ˇ˚øˇ£@∞jLl'V§2mÆ‚7QπÇ¥e\=≥œH:_˘<ªµ€ïgà;˙m
ÓÕ|çüHñæﬂUõê≤@3%%”!#·m†Z*ñâ›”≥˛Y£”◊âñ,ﬂ‡¿pbJ5µT–«ún∞ﬁT¶RÀBóƒÌpF∞˚"v∆çÑ.ŒÖ>d”©>‚{P’¬Âu:Öæ;NTr∞mN¿jeO=<Ê∑ü•Çl%2°™™ò kôîØEÚ($ß4•GÅ+*qƒD˝7gôRèŸkw≠b±π‘∆XÊ„A_ä,5sË+§⁄√Ù)Yπ@Ô&¨u;ˇ•}ÑŒ⁄Ω”£è`gò∑ÑæQ®U'«†∏·fπı¸∫üÕ€ÀÓ–≥y€VŒ"¸jÂF¥Ô‚ktKÊS	ú;*:mºÿZûÿˇ!ƒ*;®í˘ƒTﬂ¸ÿ¯¿yß˝3àÆË∏›?Î4{_#hy>–
Ù¯ÄÒò√‹¨Ì†v!˘ò1QM…)≠Ã°R˚ë—‚J‘•^g x5uU]Â|ÊgÊ}@l5à˘ﬁÍ¿ï©_À¶ì˜ÖÕƒPÇâ u¸Ñ8YHb≈∏äú¯{s÷}N‘©=˚-Îà¸4Du;pÛ‹mŒÎ	ë"±&'Ï Q)u®\x˜ô±)uA∫CüWµh$˘ÿÁ1Õ®ÙK´"•≤ “‡ÃWÍ÷Ùr	ô;Q~‘«6ö ™Í1Hı´◊äÍï±%ÓqiÒC„&ÛÖ§À!	ibÔGÖtó†4pçHIX“8!:jDG±n·Z√
Ü≈∆·Ù¢6ê˙kπ"w˚/^µÖñÖ0©øq¿Cº1›Ω≠ŸTΩÊKˇÓÁ∑à-@Î«áZ?V{öï´»úÕŸ}”äËÖUg7¸èh€˙n{®5–!àç∑ïE µ°ÙK@$òQLô•¸ÃórH—_J’Îí!Özzx/ÿ‘Å0wó√≈~êåñl ÓPw8HÊLƒØ¬‘w‚É_nj1&Ûﬁ‰Ø´r◊Ïﬂû—yïêfô˙ÌF´up†Q@w˙àü› ù9#◊·ò7·7:Øä¯	à√®TÚ∏˜≥» _‘1ËåêÑ≠,#{¸ÄXIêﬁm"ÉÄ¡ê^œ›ª∫™≠∑rÄC8ê¯y©≈8T¡
Ù-„ª[ªCm¸ û≠ˇB˛œCΩ•öIu«⁄õ·I•hÚ8>ª˚Ù0ÓÌ‡º˜s¸COˆë……aaÎE*#p¯˙∂tÙ1w » π,wc2OπÂ~z√¸Ô’„yk≤U∞’E¯≈¨uÂÙt>G'÷Çµ≤èJwÀñ¶*˜ƒ™Ç?≈ü~ºµÖ˙ß]Ù±ãZù∆—Èá¬áò∫8Úo˚~1h9ÿıØVÌ+ÇCÀâ∆Nâ¬2ê1C}„Dl§nËC≠-«££ÂÜG“ı¥KIùíéQ≤®øÊé§mU˛»˙@aÖBBR+)c”MwaWÕ$kâ][Ÿ¿gÌpJeê§'WÎÒjC=«.w£|†∫≈SÖO3¬ri<çK~T≥—∆
≈ú‰è ÍÌ~Ù mîØEœ—>•SÓFÜò
%ìALJÒTÊº<˜L+$WnÛ$ú>ØãG≈¿Ü{Í≈lüjG≠´ıäRu°œ[(*πÂ‘£¸tåﬂùÅN…~ÒÀ»Jr<≠ÊàÆıp$Ÿk^â%Hr¸°<∞ﬁp±√6J∑ı∆_™¯º∫/ÃÆ§ÂÄ(âC’ohrÎ˚õµ’ı™çÈNÔ√·?˛˙wÙÏ∑⁄¨›a∫éqÃcLŸMLU¯ñ?πt…ixB±®æÅﬁøE€÷6´ôÜÌ\}qt„`:öhÓêàDÙrÊ’Ê?ô,∞ñ7—ºjR5·¨ÈˆÓc{"JÏ#«ãbzÌ≥,°EŒ LWHêó!dGÃîêSZ]∂ﬁ‹Rç∞ö⁄9ï]E	C=bﬁÜ†j!°⁄>ßo*'y≥°fS∂Üô2efﬁçX—3ÂÆ8[cï¨gµ¢îkó,∂÷ââ˚•5:7◊(–GÔ&Ó@a/`rÃºHí(X—Ê
	cΩ¥ ‚ÏU'xM¶ó>ÌSˆ>(âÒ„¸'uÒD Ã&?f˛iÒ¶=™ï‚g—B%%Är
°\'◊9‚ó9‚Ò|yŸ=â˘‰ç…®.z:ê≤G‹‚Äœ≤¢ír^áqiPeü_!ÅﬂQ≤µ™CK+∫TäÀõË{◊¨gÄ]˘)(õF}•¸Z5#°Í¨€≈¨v|z|ZÉ·µcˇÿ◊((Pk6ŒZ|∞TÎå∞ﬂ8˘ëè∞OAgÑn„ón„àè—¡‹U+p∏ëÈ/^¯6Ÿ‰◊“ÜN€§Ö&„À‘(–/”z√Bı#ÕÜŒ∫Eìˆ4í)YeñÍSNêVﬁwT˘⁄ﬂM6{/ï‚*∆»X
Á±ÀhZ´x[¶≈∂˙¶ga9y1Í*7∏!ô8˙≠£äoo4rÉ0iø†[-ó…PåÁ™∂üô…K)k—hñUôç‚3À∏gƒÉít1Ø‹FcNéív,YÇ0¶E≠
Z÷,π◊^7óV-≠!∏*#£D}¡Ç{º:±T¶·ÆR!¬∞ä¢ıe‹_ó%Òã ˆ§Ø%ÖJè}èL9†·Ãõ^]÷]*s–˛!eO¢q2Uó}g@ö#'à¥«SœDÕÅÒƒ‘()åöeÛ(€§5Q(k«˝ƒ5CTÉ&≈‚Rç‡ﬂç˛é›µ¨˛‰ã#-ŸqÄ´RßAÅ2ïmªäîx≠ëwr&<Ø¶Ãfa@∑öÁ…•Á¨≠É‰¡§Ní: ¬òùÖ8ˇ<r‚‚:<ä.ûıØfHí\øåπóógµßbíò¢æ¬±⁄D#pbT°ê‰¡D˜j v«ã-ûªÕøWPY¥YãV˝ù%P? tBÆP®QÓGìáí.¡<Ãπª#zmñ£ôEè·ld§”Ù(yøHàªú‘=qy(Á¯bã,…s™ñıÙ˝by(Å√Êõ}Â°"*IÃnp£uïíÓ∞H)Û£WB-Ø^¥ú+:ÏœÿÖFi‚nÁ§<ı‹8—ÑﬁTiàxdwV%GÅMÃb÷´ã£Ëñä)Áø.ãÌü¡ëùá/â†u~•wœ&Ó9Êå^å!PÕF0⁄¢Ç
¥{‹C6j<P∆€vÇ«∆eÄ‹ï∞6|DOèn;º£<;˚í(˜Î∆X∆ß´ƒYˆÇJ∞vÁur#3éı0‰ÿØÓﬁ{¿‘Û∏å«zY ⁄Hs"Än‘æ›¸å,¨‰H¸%F8¿B∞+æ~|ºıÀ/ÆEPÑà3”k˘ÒÕ1Ä$p?®÷ºπ©œË*C≤Ê˘πÃzxz'¿∏Éæ4≤”˝•û™©ΩÚ–P⁄kÀÅíRLQc0`Y≥$˜G•π>.IÊa∏¡€Ç‘— º1¯æ?4˜èVFµ∆~Ø1Áøº6Á¸ﬁø.p~√õπ~ˇéæ¨2ó˜Ï›O.o-xryWÏÚû›ì£;F›ó◊%]‹¨‰…≈˝Ö∫∏µh3µ*2fﬁPâÕpNZ|2J√Wb2¨û|™Û}'£WG6"‡˘ú≤˙!ΩAô≠‡…˚˝D“˜Í˝â°,<Ø4Ωû|ﬁk!«–∫,øª=∆éf—&KôZÊï0∂Œ»ï±T%gÔ˙‹2õƒWƒ?ær
7Y•D$^R	Ò"£¨∫%–ìx◊cæ$∫“˘ï˙Ω¸ Rmí∫ﬂ[¶˘"°r‚íº¢Æ2*£:ôr0Rù b4âyw[ë≈r˚—B=m≠DTed≠Ç|´ÏÃçÙÏ~M'L\“æ
Èw<T6ø¯$Y!Àó‹ZYjTa®UdŒBR8?· ÏÆ¿∏(z1Q(©ú˝¬Ã˘QäHΩG∆éZ·Ì,HüÆfV∫õÒW]“¨∏’èjW¶® ˇJ—lù¢ì”>jùˆ⁄®ÿÈ°ŒIø}v–h∂k⁄÷v]£π¬=´”xNÀ']1Û]Í{n{vÂ7S@T™&h©∫*µ∆•™t¯†.›≠†÷bKõeÎT[êŸamÒ|‰x®≤™ä∞ˆ Â∞·yq!Ùoˇ∆ﬁˆßwhª¨#Ø∏ÑQ≠ÎL1ã©@√˙õj†¸–s√|}je‘ÚíxzÊGwœG ⁄?‹⁄BI#QÚ∂áiÑ˙ı)™)X"¿D·ióñiIU#Ô*°?°ÔM9ïó‡$Ø˝*êÒ˚;€»fA˚sµT∏¡º$n
0É£ G ¿÷œÁ'Œ∂ˇ’Ωlˇ‘üÑË’ãÇçœf1>¬Ì/˜kôv2H Rπ,'⁄w±w≠—f{	¨æ"hDÑrπ^è–Ú€Õ˘KJv;Ø+ﬁÚfHÏ˘Ã$4ûD1∫$à‹Qı“ù“9pN}G¿Û"ÓëÛÒ<å‡ºÿz«C,-qc◊±ÒÕõõŸÆøº◊Î˛ÂãW…mü–î˜CÕÛsV∆Ûn…˚∆@lºÄdb™fà¶Í;'Ùoõp—Éc∑∆â≥›˚J$æ$*Á˛¯^•ì™7 €!'¢ªÙÎƒ°t¯7Ω$›â)¿⁄Ö.Ü/XI=”®^˚s≠jâØëê\‚éDGX≈„FÄ˝Õ˚æ?ã‚ïBƒÕx3ã%'Õ@LÏÒƒg	±Æ†∏w4bπx‚A&P©-Ypzö»˚+v\rÂûæe¿;ó†[á‚~£›{±ª˜ızGà~nG#|M,´$⁄ƒ≈”˙õÌÌrë˝À”ÙÈÏjã}UõxHò]&ùT#ﬁP°‚¡Ø•Ay ÒbCKß=°Ûf˝Ë^∂ÑëÚx‚∆Œã≠UÊY∆√_ãû≤Éö}âN˚]ƒÔ–ì¸ú*·óÄ|–ëc;û·Ñ“˝G⁄RôQãíˆá<•––¢^ó]‹Ölë;<\bQô[G8ƒ7ƒm∫ù¸1ùÌ%'J>n{êìU⁄|Õ ú,Äﬂ"Ñìn_Hﬂ˘ë~ÙﬁÇ/<»ızˇ’¢xb”o˛å„âEÏâu52p'¬Îô‘”˜È+ì‹•t>yôl6QÊ≠=˚-˝‰ìŸyôXYàœâ[ÑÆ¿çﬁ¢eáL≈è(∆ÉÎŒÏ·˜eÔƒŸ,r{Luy◊ÇπàW%m∏Í≥ùﬂﬂ◊&™˝‹8:j˜/˙ß›è›E0äÂlF&≤kd∂Ñ.2Ω¸–Ú·¿n±7ì‰¶ÌÚCﬁé¿í_ü[s‚m|˛<Û≤?°ó•}é)pæ¥Sñ/•êNÛ€oƒ9ôa:väÉô”)?4∞/1Ù7¸®ç, ËM&∑'Èß˛Ì˚>Ωˇ<s\ÉΩ-ÂÏù”<ÔHViÖdHËç1XŒBí∂æ∆Xõ›‚>@≤>ˇÀÃk•6&B3åâ…ôS·N3©=sL 8ó2À¶å>Ç¢›˝Ü+ïÊˆrââ#QUÑ$üù
R⁄%6¢ KÏå·èIl°ƒáSÑØ®ÿe»ËQ¨W≥(+C„'fnx ßøµÖZìÒx ›∫L	°z1æå¸˘ÆÕ) ]∫˛‡⁄«lc{jØPdäLS*2r\%á–ä–,ÑŸµí¡zàÒz÷™VÚ ≠±f!O≥ºî±Ìæ dïMåED4CÆ<HT%°«ÃS´üX˛mÒ7ãüŒ"iéÜ√˛«V˚§è~:CÕ£FÁ5O[mÙùûµ⁄g®’Ó7:G=‘Í4éN?†˙QÁºçNO[®÷h˛ÿ9˘0R9¸Sx‡áßPïG§3Òfr≤.h7ƒ≥Y–8•Èsˆwdœ¶øsbÀa˝iŸ ≤c¨^Ù¢ÂyEA¸Åãc°∞é¸võ¸Y+1Ì,å«üÎæ◊r¢±Eg‰◊	â‚4:z∂∆D¡XΩ∫m°j›ß\Ë˘ÍgE˘†Y∏9dwÏ≤∫IôíB;Î´õY1]}_’Ö…Î‚◊b‡bg|a≥≠™≠àÇ_—öp˜’ÍrFt\\Ò6‚íë≠π¬∂¯í™yÏ„vÊÈÂïnWqFô|0ù¬`È¡Û4∑ıœ+ {^ Oo˙Æ[ÈˆBhˇgÑª˙∫˛ïÍ˘g´«Sí(Ê∑≥∫˙∫ªå2ÕCÇÌ5Á ïkb"∑D&óÑÂCÏì¯ñâR.ÈVÆ8òÛ‰ô5iÅ2r¡äˆú≥¬u≤lª⁄¸75*A™à; ¢Íü»J¯âuFô)û–¡„MQÑ≠E¢AË	;Åeì™°lÎ≥ﬁ
ÒàÂGÓ(v’TOILÆ‹øˇ”≥ﬂ¯]ÌÿüP?ƒÉk Üjäùl!”na›Ò4ØBå¶VÏƒ.9&∂3´çº,y≠}áX==Q:-QzTπÉíI’é29F.bΩ∑ÜOÑó´â«¡$¢Ê€`WçG
™üV∫~w¡|îm¢¨6´"D‡H%5Ã˙£íê¡Al[»$⁄øœœ·Tƒø≥&A@¬éà|ÿ}≠yz‹=j˜€-®€jÉ‡~÷ÊôÏMıÌªÉÉˆõÉΩˆ˜rXW;k7Zø‰xy∞{–í†{÷Ó6Œ®ŒêÑ¬õˆé‰ Õ∆I≥}t$÷’<Íú,,ãŒkø›ñê)Ê˝≤Ωw∞'sæÎûòo_4ÖøÁﬁm◊zπ[‚Äwˆ^Ô5%-]ÀxÔªÉùÔpÛıÓõ›7∫¸›˛ŒACbS$X∫˛oô˙æPs6®/kÏ.πgkˆŒqÖ!
U 7yçrµÄ-@’µn≤eÙ•wum+]]+ì‰ï/¿1v;≠˛vù
ƒ§Åé7Ùô¶ºÚÈ’&U˝DÓjä_–/Éiª¡É≥–z9\ßû¨∞[Ï¨±[XØ‡U©>	bÂŸÓõ*:Ú+5ßÅ)©µ†ÀíÂû≤‘ˇÑSCÙ
†Tø¬d›
Û6mTîs}}˜ß	’8úx˙	ï˜◊‰ìOlÕ§m(*æÂJ®ÎKÜ’R ˝˝¸p¯èø˛ùÓe/Ü†Uãg;÷ˇÇo∞5âóõÌâı±G•¬?XªC*Úmè}äñ›–êçO%6¸•ÚÜ/ΩÂ]*f=&K∏—˘^ì(¶”àâ›u◊ì†Ô∞å?nèÉx™ñÆè*m1ò`¡Ï `ÑöÃó˛:±^--$Y‚I´C±◊|a§^ﬁ›πNº<rnHj@D”¨—òV»AÛ◊^±ÛJYDí∂'fÄ+“≠ËÂ”mü¥ò¶˚®∂c°32 tÕ6hΩ=æ›µ–èN< ≥A7$<ë®ÍÌK¯-∂ßê éö‡íÉf÷ˆ–+bb—Èp(·]g˛TÓÖΩÔ–üﬁô3]Ã€	vT≠ª•$/ï,ÎÉ%,“≤õ)π≠ôÕ∞º&ã2ú|Ds+ÜéÑEÿwõ¸KïPZﬁ”™Â≥rµt(pï«v≈¡ö¸Á…xÔJåßS$‘¥35´}sÚóãbîêVw-Sù¥∏mwWªÛ7€AQSó0´ÅÊ0´ØBàk†Z4'!hT14›?J'à,ª#z!h˛ŸÊàP	^¬5;◊˘lπˆ•^±¡Rî<≤ÓŸo¿éæE;üääy™Àë'.@¢Œ≥f…kÂe&∫	ªä¨àP‘—®uõ9†7Z%xÁTRN…≈¥ë∑Cs‹ |x∑FOå˘kÈÎ wj‘ˆÓ
(Q◊W˝ec|ıÔAƒóh®T@7ÛT)›l&D¥®ñ÷%∏»\”Õº1~o∏Æ;’!<’∑©JHU{Éd £j»EïhÄÚ<•Fı3g@w5'QÏSµVZkP!iŒ–≥$ñw[ô4ãØßÖ]Ìç¸[»ÏâÿCÒ»â–%°†¬1˝7A¨ÿΩB®≤Ãs˚P¿åHÍ˛Ω%Ôï®*∆• äÊMÓ«+Œe´Ô‰B\”_-Û•´VÑ^“ We —∆vèπœK∞œ¬∆∑ f±7ÚfMh ≤r;ËŸøÜ˚N<∆PﬂBƒ◊Eº°p∫ÖûÖãY:[öÄRÛA¸¬ÙÅ–=Ü∂·≥+˛/ÚSÿSŸèœOM~LvëUYÈ|¡2/^d)”ø”•Õ>ö-N’‘‰”£ﬁﬁéúA¿¥[!=Pçƒ∆6"‡gÿY®ù¬•¿,1'Uõ|Å≤XÀ‘eIE∞”.FÀ*ûﬁ
+!‹Cıç≈åçLe»ﬁp`ã.≤3P0‰»°Å™˛Z†äsí[©ã/◊ª∑∑πQN[gÓπá‚Ê‡x–ÅÛa_ùızˇÅ4Û6¢"…Ω\ÕÓÉ	£)cüWøcw‘ÃäO Æ–©∫ók"Këe5Œõ¿ﬁ(Ù≥©¬@ñ§æ]¯πvÆª´g ¬T¶jÚóT¥€ûRiÆ§Acªy÷Ó£nß˘„«.Î⁄¸pAÊÃ‹+3ß‘∫¨˙;Îß	t±ip°ÜóÌ\}m=®T∏‘x≥<eSÄ∆äV´*çÅtb*Ùâ ã”Eƒı$·YcA¯ªÅf´±H]fË+áWr◊√jO°0À'm˚‰\ø+ƒámeUµÀ/`:^ÚÎ?-∆ÇÃ≈≥ÀŸd9\°«	ØŒ4‚\Z€K°ìg—0oƒ≥ˆsó÷∂Å÷üa_˝	[A◊≤pü #±UH≤¯'R\˜ä§ÿù5yπîª≠î†U%¥∏˙€ï_œ"H£∆ X&	Óí9µ™	™{ÙP{:[‚√¯=ô*È|=©ö~…˛πLQ_HÏì§8â†H—KTÅé◊*ˆUí)7%'•IÆØí´¬XâvÆ®f‚v˙È<OMﬂ:·u◊ìÒ^⁄•àXñÚÚùDjÂ<£—<“÷∏±y˝üãÀÿ[cïˆ◊öÆëƒÙ7´ë∞FìÕ£¬Í…T\OjænTØŸ89i”ˇwé?5˙ßgIπ®πüUâÚP!r"ˆ+.ß©◊á˛j<â°‰7´6s:u˛äJCq∑/+πrå?Ò‹ì˜?‘π+b˘X¢:õ˛å∆ÿõ`˜ß∞√üÆßV+*^Ö¨seÉ>£1HsÓãÎ®ÈzY¨*†UàÅ¨ÕÏ∞WQSÒÕ8ã,r?‡b]∫’∏"æ#è©˜êÉêƒ˛†‰È_	á˘JÉŸSAÆb¯rr…≈’XˇïDﬁzû»X(·#ﬁW≥eâ ≈Zô…\çW&∂ºÎÉÁuÄ«$ƒ"“*©˜8òIæ,ã~Dx#Fèu ôè∏Áúä»éG5HzëBUÿ-VÇêtQImdIú£zl\•q§Üs˘Jú)ÍŒùh°á¸¿ŒÈ~£-î `ßî¨]ºØ§¬øt›®™¨ë˝&ëÛUÇ≈„ªM…P+ˆ≈´pÇ‘ì(7íÖ!!ﬁ∫ È¥˙€Ú…†_π.≠áGÜÀHvY`yUTˆúò∞0'—ÍH{êˆa?¨3ì∏¸:?Æª¯íÄ±Ê‚à2}πÉâ∞Á9}qJ…˚…ÅÎcÖÃÖû+3z2#›P>(Ü"ÿq˙”=ïﬂ¶ª’»@b;≈ÇœH@0SL‘‚™“·$ﬁ≈‚(Únoo"Çìä⁄Î~a˜8l≥«≥òB∂&®-.Òû≥Ùa˙'eÉ
eæ¶5è¢ø»µêCd©áîrÓ Ê⁄È0ÛÙ∑Ÿµ_b‡pıüÎ˚guä„¯√aD‚˙î.üüñ˙Tnô3"a«0xÊê£8qqÿ©,KØ°égCS?î«ÑA÷BùdƒïA#n°}π^ a¸‰Í¬ï´Ä§ëQ¢\+V	ÅXÁ9a=ﬂ°Ç4Fûj"ñï¨ì]SåVù¿d¬M˘hWÜ Ø÷GqdA˛…ı©Ö:ïukI,”y˚¨s–aï	Ê	áÔçÇ)xw≠)XÄD`êJºÓHÛ.#C·±¯]óî°∆.Û¡µØáä¬Ãr"ë+«(¿$ë(x≈ ˇH„∏}÷@çføsﬁ^ ë•õe‹ÂP6ú`Ì–âPCxXıΩuë‡  ©≠ÒmI:=™<ƒÛjËÛ)P'„‰gI@∆ÜƒëjŒEïÁFTÒÃˆ˝8ˆ«:[w9˚ÂΩÃQ}/≈Ô$Á∑·W~ô˙·ÑÎèÑÖéø<»gz&-:Æ©LÛ}ÿi∑’¶{Æ˙∂.<Ã®‡ùı¬{^Â‚4˝7Û &nC˙øqyúÉ°πaÔ‹M ∏◊£•Ji⁄Zì-nMåÖâäÔÎ{≈·kôp	≈÷q≥	≈ñπÛQÔ8©Xµ&È©∏ß⁄Të5£°›‰6<ñO”<˝x“œd‘àæaü‘zwŒ⁄&^ˆsﬁÙó9œ¸S6˜8ùD…ÓæsÁß◊Kê8‘åyòãvˆ¸8mí:ãWÆı>6õÌ^O£U@6ä¶DèCVZëq0[=T3&9i£–ùBó_Îá¸ˇ   ˇˇÏ}ÎnIñÊ´Ñ	OÅÍíhY∂k\Fπ‹4%€BÈ÷¢l£v±RdPL8ô… LJfπÃÙb±ãÃbÅˆœ˛ÿÃÛ‘Ã<¬∆âKﬁòó∏%%ïuÄÓíIfDd\NúÎw*hÇÒÃég‘â öÃŒ¢V”ÍE#ç≠d∂ËﬁÈÈÒ©¡Ï∞ÛaµNpQÛZ÷•Õ]Ÿ%Ço∏x•πD©¿ô¬ù- ˚ú;˘il“àzA÷ÕqEÇ‘êa~÷ù≤LÜiÃóˇê∂!t£5Y=Bo˜éˆN˚I-UÙ‚≤º€¸¥µî◊ñÑ‘“ãû‹êƒ£¸hﬂÁ∑EŸæ˘«”fZÂﬁµÇjÔ|dÓJÉìÖè¶§è[æ©î⁄ùk¯Ôƒ÷®äÀ∞—P—ö?—#øüU∏jaC“–ç@xVáæ<≥Ävâí›ºÒ!é»KD∆~ßVéJ{˚„7a0„* ≈®ÆÊôt|ò„¢è.aKô|¬a‹K?Ò¿â0óJ7  ∫û√0Ê–yÑµ>˜\¬ø∂$ò-˙e∂Ë«óË…kË?Ó¸ß^Ï˚D8;"'ßª¡•Z©Øö[ïæÒŸOg∫ÔÀ⁄[\DtÁÙ'Ñ=g⁄‹Høzç…6≈t"¯Æ…øïÓª;Ív¯Íø¥˘&Iõ%oÚM«˙+ƒ·hıxãm?ø99vwı€ Ü∫zÂÆÆHGJr	3ß^=
N)àJ¶‘”VTœÙPì ¥,âdeO≈VT•KnJ¢·ºÅHa©“˜<(±ë˘D‘l˙Ì:≈c7ƒ£J∂Ù¬Ì˜g3Ú π<Ω%—,(n~î,–bNƒRnªò,<r#åÇKüFŸ’øSï$eDa∞gŒj¬n•ª"..ÿèÎ¨‰æ•SX˚ø1´æñ“öaÆ'<±äl.¥ƒ1r}Z|Ç®!&KÄ~°8`ÂB◊‰^†öÏ˛n„U7^ÃfKÒJÙøÕ÷L8S≈#%õ
®3x%x˙Ô#»#ÌΩzÅÀî≤f¶?XYô–`º„ô Ôiï\ÜÙ6˙D¥=^,˜ì©¯nÈKÀ¿ºâ:RrcXê„AÒˇ»œwûı∂%¬Ÿ»@ÈGX÷6ºfZ∫£˘©|ˆ|Á±ƒ3tæ†rÈrh|√eDVDà∑Õ°Îë}‹˘uóòo0≥[êGD¸èså“ `ÁÒ„Ûâ{˛[2®ˇM9óˇUV ‚è’⁄%s=¢∫-”√≠t≤∂ˆÀ;K¥©¢Ï.¸•Bc≠TÙhq5£Ωe¸õh]691äŒ	ªéßCÈÅ%”“|y8∏„`G÷q`v¸Ä,a≤ÜzõF˘¨æi¬ïMuM/§∞´mºsŒŸﬁÁl˜W"Np–ÿ:Ò∞√˘u>E.√NƒèÀÔÅ&˚$– mÁq◊n€Ø.ùõDy9ø…}N˜}N˜}Nw91UΩ–7Ç˝›ÁrØêç©ŒÂrógkÑs»”Œh!◊]‡Û SÌpÔ∏\L‘DÚï#√ÔSπÕRπy˜õÏ˜Y€˜Y€Y∫ÀY€ÁDH∫œ‹æœ‹æÌô€tüJ=|üΩ≠Ù‰}ˆvûÓ≥∑õÈf´ZŒﬁ¶Ê˚n≥‰T–qéw˜–Èﬁ‡¯Ì—˛∞ëƒ-≠tüƒ}üƒ-E7~Nx∑_˝™íπ9åK∞ Q¯Ì¬%ÇÍ:%ˇ$¸1•Ω¶1qqøÃú2É(ö:n≥ Võ!Ã™÷◊d€%›T©r˚,ßﬁ2èDœ‘ê"À¯éB&˘w6r°´ﬂî&Bﬂ‰{≤åÍ6ﬂ2óï~cØöœpoˇ}ova≥ŸÚVﬁıq˘ªrï&≠[HÂ/-ÀƒE:–€óƒ/WΩ»F÷7êMœ›"Ò˚‚Ï?ï™Ñd)µ© çVÔD£/aØ,Éåk-KeÎö¯I
O ŸvmeIµºúä´ZˇNMUƒΩdˇBêFˆi3<Am‚bíóãu0!È@“NÏd!'=üTE¥.µY…’§7È OXÆ†ÆP:]PUéjê€-ô˚äÜÌIZmQq[ï$õû‚Ià£iE¢iáh>·≤Sók™PÔJêÕZÇòˆJáõÇ√[/ƒ#H2A€Po5ˆ
ˆVH_^âÏπ—Q')íup§^ùﬂk´}™¡;)€-øF(ß¶êV¯ïÛC†JGÉ©;ØOeá}ƒ‚ºY¬’Å≈ €ÜÀõÍÅä«òÑûõ	|‘ß/E;‡ÈS—ÂÍ{÷ã 8ø.•ö)ad)Tå2,ﬁ.HÂérc<ãVˆ€´5•
À”è"Ú0lq5ô»Dn“ás í±‘:Œw&‡8´iÌ≈zÍêΩ»ˇJ≤ëƒi‘oö1ê7j¬!ê+¯¬yO6Y^uŸüIVÊÜzıO≠qawêÈË*k4jbèp∂¨6«∑OÚj∫T©fîÑN28Dı§⁄GÖ6	õ‚çã=	”⁄è: )c)y>WL·&†K]‘Ê‹XfÕ=rL…v£´-∂Ûâ≈¥˙OºúcÜ5—≈ΩÀJOÚ„-H\›÷œ¿m*ULíL©á!⁄àsQâ@—–qΩÃˆñ⁄- ‰‰ÊFY¬›r;ÅÓbvu]∂Æ
vE(É≠^πÖQkPpÀÇ&MdÆ‡˙M^;·∏Jù..f‰‘±pw·á∂$< ø±w¥R¶5…èak’]X≈Û	0>P±∆ Âµ-Dbﬂ£‹®àr[ü fÏV‰W›ÉﬁﬁÉﬁR∫†∑“iæÇ(îçF	vÁäaÖ€Ä¿SFÃ&m[Ã¶åî≥¥Æ£e>»©jôœı56m®—ä∑)¯èU«a˚∂›Y‰ê	îÁ=ÑÁ=ÑßÑgíñ˘á√ÔºÎü°è˚gÔ–áΩ£›Ú∫˜å7Â9ñ≠yÔ	<a¬ÿ=Ô˝ª†,Å6ØÄ°ú1_IÙ∆ÆQ
Bô‡ñÉE4πƒÒ°xÄ£¶d~YtI^L<IVŸ·‡yx˜#ö≠ &Óèñ#wITtd∏ª∫%`9C<˜ñº'ÿ\∫»ú ÅEïô0óàœ…ÖπÚ‡ΩﬁÒxoBÜw≥/MèY’	ß.ŸÀ⁄íQä5ñı} <’jˇh=.?.eõzÂ£:êá¬˛E¢
≈-Ñ\‡ø}«.∂Ìﬁﬂìõ≠˛â±ö··ú√Ãö°-<né¥Å∂P⁄Ÿç`"% 6.q 	í—tÅﬁ*%õﬁ¢Ø1Màî®„~ ö ∂¸7^Äô5’óï‘LiÅñ{Ú;¿J‡ ª⁄r.Ï¬6≤of∆AêÇê•º y¥9ÍB˙˝˛7z¯Ö	Øz¬‡)¨·´P˙Ä.íp°†∆e∫∆À·ålhõ”´Ñ≠ »Ü˘#c´W∂ÄU›Æ˙j]˘ïF¡˛ò˙÷¶∫2Än™l÷ÙKÔ*1#∑ZıªÎ^π„
ùF\>ÇìÔxM◊⁄∆AXî|é∏†ôàsZÊp©’»oπﬂ´ﬁ}
5íaê|õú∞u çÒø®Ne◊•9˜M—¢ ç+∫Ê ÆD¡à§îçdöëdíïOüOp≠p√ »ò$K™FGı†y q+…=hFã∆Ì˙ø,‹pâXÄõ∑dÿE¢‘è8QYG≠A©ç+Xê˙U\Ãñ˘{I%Ë¸ñ*ˆbmñÂ,`Wë¿NÕŒâ9º(aM <dw∏R#NÊÏ1üCºë9Öê∫F_.˝àÊÓ)urq9(™©i_ç"pÚlXRÑ	CgüP%)2<…¡˛â‚»>_†ÙU≥ßÄle∆	*œ¢J∂PkºZ>0KÍ∂ì‚>Uú†[èr[@ôhÍQoÖÂ†¥ëO‹MèmíÌ˝m√∆Ÿ”¶∑ìñi' -+ﬁøâå“CìÃ?ZQ"4#cıÑA\òÅd¶í&QF‚öÁLTø!)YÑm¥˙–ã›mÜΩPÄ,íµUåà∏âC0∆Ëœ∆Øˆ\9≥ÿêùƒÃ,ˆY	»öv≈·º√&ëµŸﬂ’¨ÂñÉÊöÅ™TbÊ‚∂Èõs?i—*wÓVC∏W‹©≤Q‹@Âë‹g¿Õ’g~˚@MÈ‰ç(m˙òù@”ã™Ìç>Sn®Õ≈µ6õjõµç÷cÔàlW‹LÃÕ˝⁄s¸O›ó…o“)À‘.!w‚ U∂5í≤Xï$Ç(\ç€ã]Ã≈ T4∆+IÂõæıFC<rÁ.i)Û¶"\DΩµYR¸de±YiGı&s&ùµ+æÑ„)†X«kèeUpÿ`Ì{¿ÑZô∞@æ≥‡ˆQ˚F=¢ãFß¢ìÉ˛`ÔpÔË¨,†+¬”Ñ«êÒ+¬§DT∂ØÍät*è›hz¥ 'ÊQÑ=•¨pÂÎ⁄ê´Ú.‹®Ô;ﬁÚWr˚ÓfõÀ˜Tı´ÜWz,DR¡ÑU÷Œ¶˘à}J:¶èˆ|`çt⁄{„t√≠æhi–S4"ìçKΩTu-ÚÓN∏:bÉ-àÄ£e»ñ9qñ òRÏÎ˙ ∏è˝ÉÉΩ3ê¿`oãn¢Œ…1†ñvﬁˆœˆ>ˆ^ÂÇ¥¢6ﬂÁoIc◊ŒíÙzà„)Ÿﬂı}'=“êé˝S⁄·Î˛—O¥ˇ˛œ'˝É˙~ÉYp<«! 7ıyvD8‡aPY”˚íΩ4y¥†çhœ4u5Z©kÃöV∆Gq¬ºôΩœsÉô74∏∫≤6_bèº&¶ƒ{ÓçÇÚKÙñË?N”X°Õ˛àV!1|kh©Ö]BN‚‹ÒˆféÎêµt‚D—5‡"ÿß	û Œ˝⁄∂iÆW√Xi’ó	¸aåÁ2É-a˘’qÆºnŸ »7J/¸∆0X∫K·¢[xò<t ∂œ=ûM†7¢’yà'ò®„ö%Q%‚°Ôºx≤çNKWËîJÀXùöƒ‡∆ ‹“'o∂LòÄ≥\ŸØ fl’’¬D≈Ë$ƒ[lø∏!YïIﬂLl7∆s≈µö=	¢“PÛ ‰Ñ⁄∫d)ß  P◊>Ì5#?K6œ‚G4‚Af¢yBü7ªfà¯ôHﬁÑ•Ï˚ìÄä¸û≥§ºÆÉ∂îÊ¨Uk‰πÛn‚†zm¨∆úÇä»¿πïÎL∞=…X@µÙıf‘U3Ü4Bñm∑ÉÚÀ]jÈ·ˇ˛Oˇıüê*O•ÎP∞€R—∂≥GˆÌÂ2ﬂÎ˜ûHáâ®ı˝esdâ»Ã„Ë7Ùâ¸©Ì®4“ıU%¯∫∑ÚÔˇ„˝€ø˛m›õπÔyº]—çÏg∫ô1Ñ6∂±ÜlÀŒkiÅ†I"xãgÆÔ¢˛~*†◊!v>çÉkøY,êãZ≥ô·Wò,ﬁ«èüIœÎ°≈ÛŒã}Ó»ıi¡…±V	´Dï/πì˙]∆K"æª£s_l∞sX˘◊áaÍ^íFπ£z¶⁄∆Ω§<*’¥2ÿ~Á˜ˇ˘¥/ê§%ád'ÂFªl”îq%≈LÉF≈ç¬I™¶HŸã˛≤õ*	dÎñíÆá%’}-˜ù©lR•√
‘÷ÅRõ≤éﬂ_¬≠ÂπêrƒzeïÕÏ0¶¿uKGN~£^ÒT/öT∞óπR<ç¡hÊå¬ Ç¿¨ªûU§‘Öı"U@)pµ'2¡Ä≈Hê◊]uC“u¬/ŸsÆOX!"ˇ	◊&O—≈kÒ^‚·«≈¡Zæá4∑ä˙6Qkb3ﬂâ[é`’xi…®Z5&p+î∞ò®üÅõÀ¥¡c}gMÉòGfà®3ñª“0˛ò∫¥‹8Ö;z˘2ﬂ42f=¸ŸôÕ=öOπà]Ø˜—qcêqxÂé†PmùΩ¡æˇó^‡ÆyóNºàö–¶ÎøÖ	"Z ÖÎ∫‰¸‡‹Ù¨Nﬁ&À˚ÀÇlS7ñ*™*Ûídãç(h€ûç¯Öƒ'`û8%âíô}˚‚ÃXŸº;tµ?k/F#∑Ép˜Çg6U∏ye^∞3‰œé…v—√
ﬂn=∑óæä:ßÿ√MÇÆÏS"*D®˚0∑|˝Æ√h#grY´° QıE@÷˘/¿FŒØ…Œ<'”äπ∆/Èù¨≥I»≤9©Ÿ6ªf{”ì&üìU_O≥}EN ∏qCª\]uUü—˚/+…ÛñÕÏj&ï‰∆CÙjGp[ `gä⁄é=cGvÆvî≈…õ2ï iL>ÉœΩ ˚Ö]@ıA£‚es;å™ïWLPmuö_;Ÿ·Åü`ve:j{¯ç#K1·.I%Ω˝âî1wBá⁄È’•jX©.”≤
w∑%˘x†∫V√›≈YE¢˛?BL$DBbD«4TßXlÃéÎO™^ÑCÈ}®ME®M£ÿìt¶jsI¥YPL+≤ú‚K4Tà0Iz‚Á±û[∂@Kê∑¬ˆkÚVhK38"aS†◊Rµ∏¿®v,VQÂ]“8mr’Eq0„˙:ä©ÏWR≠≠ —<√k7M%µ?PB)D}ÖB6C|¿ ‰àW≈Ø´§ä'*.{ßT…UPp3Øy6]Ã.§ı\9W±çdK>SÁd∑üÉ1*<èË˚JÂp◊}€ k⁄ü¨D⁄©◊: VHıös0–à‘Â∫[S÷zÄFﬁ¸ ‰^`)Ä™Js[CnKˇ2Ôºÿ¶y.ÏÔ'…ﬂôè”O3?~í˛˘îˇ)1¿ï1 |©Vùe©π Ùy˛y„™‹üó/ì~î:Q(≤R
îíµ bÒŸií¿lÑläÅ◊G;™“+¥‘{
ú—'∏#`{VÌ®t„™Wπ•Ωî¢•=/Ç•©m‰:XΩ}ôÒ}ÍŒœä—Àñ+ íM[ ,8}±8ø|z[’Ã¬ìm≈.`òê⁄’é}ÇÏ)Ë©bt√’¿n‹∞’¶≈ÁÎ´÷¡§B·öGgêŒø®)§≈å‘r‚∫⁄ª®XHT ßÄrU÷V∞ÂE∂∞0à\`˙=ﬁ»«:lmIû+ıh‚≠º¯æÛ‘í¯.7^ûî{YïÚk;ÃD¢ÜÅ*iO∫Ω‰Ù¨∞Aæ˝∂µÚmßl∆◊±A÷‰€∏kQUÈ∑0ä#Ü!÷ÖqÏX
„∞ê¬£	"ﬁ∞ïPêõM§±â‡~ìI9“A™Ÿ7„·ƒìÇ˛Ô.ÿ·æSJ
≤Èå§Ïîd¨VvNÒ„:ñéG.Õì¿ı„H#ó »æ~hm#H∆7tVd¬-Îä@Ê´˜⁄Ò†*¸Ù–πr\ƒ*∂Çhn¥ä93ı˙Ê‹¥∞–Õ™Ï@*Ó>A©€/§íüG∂ò‹ïß∂e¿Í∂ïúÄÇR9æ∏˚~;-¨m$‡”ô±‰qL⁄SÚ<
…E√Í»˚	ù—'PÕ"iû‘íX_Ÿ9˜ÿ&ë˜{&c±®¯J˝à!VWnl≤	Kwß™«jÊ|fèa“lzÜŸ0›F kö‰ﬂ§á^Ï˚±BÕ6Í£ùú¸b~	°ñ«ìn·6ã„QÎdÏF±?ü{.sEÂª˝DèoK∑)œmy®)'“∂Xr…ûp¥ﬁzyÔiRÈ√¬6òÎäÉÊBDï09ƒ3WO†lIVcæk\•-Ùˆ›Ôˇ¯/Ë·óŒﬂıv&ùﬁ$gN‹-úÁïÚæÇ⁄[≤;∫\Jô√pÔôØôfÊ¯Tç9j'w‡RANå<ÏD1»rå@ŸÃàc7.·¡ÊﬂØ!÷‹D•∏&Z∏Æì§»ÒIQ:®ó~dv_€¬ç˘Ö≤zÅÑŒKåÿ]ëo*ßPB“ÅÜ'ÆOn¯4z‚,i¥E˝$mUåeÉÏHép?>Ä≥’›Óm7X◊Wk=›π|H`AwﬁÎ´d‹„Pttü!æ—Í<ƒÚû$Z-ª¸t¸àîÆ)˝[Â8t/·Ω®c™èì≤DsW+`™vÒ(‡˘\Rµ•”ﬂ˜†‹÷Ÿ4ó”[vÕ±›]±ˆ+÷‹VÍìjík√•Ã^d0†Ñ/‡lT#Œwç˘[í®ä‚}f¨>√ÀÍoá[§ N*√˜x\≤‘)EY@PÓàZX◊Sjù∂ ≈\Ë„'ö¢.˘ßl.mR Ü>æKK›√j6£m¿)‹sFSÙ¡¬õàÀm»Ü9≥ÊLhsYuõó/a»Ö4SaÆº&˘⁄"Ée‹Ób¨º‹©‚0€çdî˙ëRÃ∏i¨∏NeE˙\&∂<ø+6+C∞{H¢®ªı·Iê†Á(ûz‘€¢r—	îÏÑ¶ˆ ’±ƒƒñ<_@ï•dÂ•[aÿ ¢*Âde9Ér◊rr≤oÖ9¿R–Óı˚DX›ñR0G‚í€˙Qr-JÊ¶ãlÛÎœÿt≈Hô4≤/h”gÌÖ¯ó∆√≈¨—îQ¥/óŸ0d˚u£É‡˙8‹˜£≈d‚é\∆
x}K&*Ò∞0†>Îm£ø˛µÍÎÃ;Hı/®(»¶´»NTa⁄Z©d¥2±u≤≈ÉØΩx:ŸhíI*ù∂üZ7±™Î·ÄÇî=ç@∂Ωç@ˆ-bE≤h!+íNÚT{ÈE¢ÄZOäßS$	^tê3+∆πÙ´—(¥õ‰(∫¨Cí=ËèØàY´]Ωßc>øSW81Së¿–µO—ï∆(/</€€<*…‘‰§>ı¨– }£wÙTìHä§SÏæå‚`˛~>\å@›#ç—˙çÊrãæRçÚ ∆N¶ÅèÕ€ä¶¡ı¥«Íh"çíÌYRÃR"±MêàGÙü:“j“ÈîπÈøãËçÖÊæüÍÔ⁄ùdÆÛ1û0ôàåíˇı¡Ò8ZMÛ6Ë™FYeU4K0X«‘zyNˆ˚bÆ}ò%]NTÇ’èkA°™Ø´mV7Äﬂ∑tWÙ>N›X≤†X5ﬁH
)|eƒn%¬–˚yﬁ•Ò‹"–Sñ‘/ µ'‘~-Üß“®YÏ◊œ¡"1B0®r§ñ©À762‘P[3Î∂∞öJ£Ùkãbìıx1A∆â)«™—d≥Åx©?î3ÂD_
8O›àŸ§tÿ~íyL~ÈxDˇ{Äﬁ,¸qrõá.∞Gæèß!&≤é7F¡Ñü0+mõÁ«®∞eiJ£·8)∑›V>P+ò+T!ﬁö;Km¶ÿõGËb9wàDOr#à™Ä⁄¡uÑ"V{H∫4ßWW í›ﬁoCß¡Ø>)6,òÛX⁄,ãÕ°
{£˜é€7uïH∑PP[Û4›ˇuØ¶=§-;KbcŸ
¸≠ÉôáV¥jZπ5¸∞øà©J«!ƒë\oR.8ñß˘tQœÓ‚N»ôõ≥ b¿„)ÊïÇµhíßs≤3(2Îxâ¶§ô≠`2πÀ|3âúëÁù’, œUË£GË)‚!(`®É“ﬂâT(≈¬—ÇTπúJÃVñ:á«á«,~Í084DƒŒ†öpÖcù^˜è~b-ºv¸O:-úÙ>È∞6€;q<5Sa.l§ñ•ïÿ∞àˇ)bkúÂ!5|˘çdƒä=ã	0«∆·bO4≤’MB«Ü≤√6Ü¡S~@à»)ên∞YÆµ¿3?Ì≠’ ¥,UAzÕ∑°ñ%=–R† “≤∫j∂]0Q ]CuEt€˜Ì∑e©-ÛkC \ñ,¡óeIM%≤-E≠O5¯-≈¨öj1Q&∂í„2ÅˆYFã–X∞à°‚´©) Hπ†t[& ÆY≈¥u˘v•Uû2b⁄Âap·zD	|ºDØ…ÊªfK»˜ed]¢H⁄jèe©ç@≤,µ®ª…ƒ/$Íû!¶§°ŒÏ!»ôÅ˜œﬁ≤›ÿ…™2¡áP„]Gã)”jéÁ%zÕa0éEWDß›ZÈì1Õºæêé¬BNLûáèY[(íM’†H6TÖï63™C∆ÁﬂÜﬁ∞⁄5›$¸∫ÃÏòµtÆ¢ƒîôÙ†ÍÀ˝+√≤´©Ω†H¶åSΩ≥≈+wl˙¨G«\Ef˚Cˇi%•HJÀ∂¢“R§c∆¬«¥¥πã=ÉÇ,@WF∆y…N0ú’£TÅ7;:ÅOcÕ2¯Ñ+ X¬"Qô∂/Ñ’√ ±ˆ;¶ç€¬2/£OxyΩ/-ëÙS˛ìÆ¯≈Ÿí™e?e˛Ÿ£Q¢Ü‹≠m©Ê»√ Z`}jpZÏ$Œ◊Q˚ÁÒƒ’HÛ»RÌa$≠∑rünÌ∫ó§Ÿ!Ñ`@¶Ÿ…˛—Ì>íWn¥ —â"j◊ “·˙ÛE‹;q¢ËöHaJû6=BfLÇ±¡ØàKË<•'&pG†Î
Ÿ6zDƒ´à»W`D&}m+∑îK¬º9∫¥U≤I—|+|á F2Ì–«Ìfëwâ|›ß°M>Ì†ïÒ¯;.:Pv¯«Êø∫{˙üÃ?ñ-_ø\)ê©I 2ó*·t{üÁÆI&≤†Zn«:±¬ÌÄäè7ﬂ=<|ÙÛœÊº®M~T_√«º}{»Ä% ≠g+ÆÆZﬁ«§á÷6Ò‡√áª±soüûtd†ªv¨uü‘”˚5≠,Ë◊Çµ bÜ—«±«Ó˝Åáù0É"qo,(°ßÖ9‰¯--ÈGôZQê8˙'›¢ü{5IäÓ’§;rB€sz÷[9ôß¡"Ü∏Ñ/ﬂ{=‘ÈûM(–ZΩ"ı»Ç$√2óÓùT‡édÊÊé∑7s\|8†JôÈ°.…◊˛_∫‡ı"åçˆuª˘§W¢/¯1•Ø¸täÎ±’*:iÛåÄÜËÍv—€' ‹•#´Û‘ÌˆZû@ChhIr‰´ûácr™p(ïô`-ÀEÊâ√:ë ÌEI0"π∆Ù0»å‡ΩåäYÇËgÒ’eQÛç√ÁπhRÛdã˝dúé51i-;	‹hYÁ‚¿°∂_H&È2$≥¥Ôè›ƒ∂óÆE®ŸQH≥öUFœø¢È=‚å‡∆xÆ¢ﬁ$UpUJ`¡)¶Rw·•e≈ËO˘∫5ø\Sd°ˇÕÎ)Ì\oaﬂV:À˜|	ñ°P·ècg ãìDò7A@˛ÛÃXHöGø)ó<È¸e·∆í∞©r∑b#4”cƒQU¨Át¢ÿìV≥,¥\}$€Õ:™ê≤]H©*â†6* ãvo¢Jâ £"9ER/öS$YPM}≥[9pGßù≥á "x1GK ßÿı˚ªº‰›}ÄÚ«ã–ˇ3;Ôk¥_FããôK-Ä˜'n8ÜL&6ûùì˚2øÅÌ¥ûØ0&6ùù∂dQ!©ô`Ï_π¯FÁıÊ∞—ËEGèıf~:6©Æ∫Y`#õ£ÛÃùajú	¬›öH ˚3ª”wﬁèRôŸ® p˚√‚î
˚ÉyÛî;∞I∞ë'˙Œô«ÓËˆÊ‰Ü˜Éò\ı#fÓ†ôÆü„M‘æˆÜCMxˆ,¡‹'ÃgìÉ„ÿµ˛AõØÆå€˘˝
ü9 ¥µ&-#7ëÃ⁄ÌùûüZXπä+cèT±!;€ı∏«º8(ZF1<◊ˇ‘3´^aíªªæßZ‡'µ|DÎ∏®øö"Œpë˛`ó,L∆ùºaÈ&ºøbeÈ˛ä≠i˛˛ä]Û˚Ü^¨ Ò
&<Äﬂ"|à˝¯˛jΩ√W´:uë=Bœe zÚ¯—Yjp»Ë0„ºêûá˝Àxä~@ﬂ€‡–'oèb†_¡£òà·©2∑z2∆)LÓâÎß3˚¥ıô}∫5¶πùÃàƒ
tptà?»Î?iU (≥¬"uΩÁFØ=«ˇ‘›ÄäÀ+yºÈx¸]·,7-≥E
ﬂÆÆ“/ü¥∏ˆ}v*-ç¿ºO$]rÙ–âáùÍÑÓdâ&”›o&I'@ôÕî…kHó¸yf?–Íı±X5AtA˙aåc"<›Øøù`] ÿVCC{´8Ív˛‹…≤ï|t⁄z7[ÛÇK◊GŒ"ûì`“˚WΩ#41Ì,£ΩG£`é{û≥≤U{
J"gLU›Ú®ÿYøäÙ£ODbÒ¡0DãÍ¿D$¢P,»⁄3ÿ_cÏ9ÀÓÛmE¶‹Ù3ß ¯#cósg,bzD HÇùê[˚g¡'Ïªø¬¯	[¡±®ÏÊ-oÔò3iö#H¬„h∏ÙG◊T˙µ0ˆß&cßú}Ãz¯≥3õ{∏G¥Jßw‡ÑŒˆ∏Ñﬂπ$ª€ç¯«{,~◊òïÉˇ·Pd‚ßÔ…GØzÖÄKØ^†wãˇŸâ=<^Ù.ßÜå∫∆–ÌY@∫„·"…XÚ◊⁄Ffê¨8Â√/…'øŸìÈÖ»ı›xó…>/P’¢íÎ1äù—ß˝Ù«Ø¨XÚRª4åCt√mÈ›t∂7≥·õ¬á}~v|Ú˛§Üh.æŸ@ÒŒN	yIﬁ≤Y≥Üò≈∞P◊éxn∞ZÃÃ
]O¡4ÿÕΩ'è+˚ÊõLGD1€∂0ÃòŒc#¶ì^Ô€ootMÃô…8Ÿcôï0kXíhˆ[R+êÍ…\(2ó«Ù:»%Â€·¥ßÑ#0Ωº»Ñû‚	&ú‘Ã,˘®“ÆŒy{ﬁãy‡Õ-2sfCõcßtÊ œ3;å»&Û≤∆ÄÄW€öÛ™Ÿ£Ñ…1º‹h
: OπãFS«˜±«¥q^-®Õ
õπ”ñW ôVyc7âùLô&Q“¶Nxâôùºõ5§˛£÷√: [µ€QﬁE!¿C∏‰,ˆ`%“É“Ì®l3‹£ıxèñX“›é#πÙXw,%≥∆â<Ñ$¢RÑˇ/ÊD}«‰∏”ÛœÎ{_πé∞ ô^Œ&7ûﬁ£öèµ¡jÏáÑPZW†L©‘ÍŸhtO# *¨%•5%a´}≥˙i˛ìÇo‰—#4|w|r≤Ù˙ßghno”Ò˚3¥ªﬂ?8~õ˚1e˜dú0X6ªÆ„•πº¥ñ5˘Â~åg∫XfÓw¯2;Å˜£aLN˝G"Ü∏<Zé<\íC∆˙È˛Æ¡>%2"ÓY aÈòƒq©Œ:˚Ä„ª‰`x¸,t//	«!C	„¢êﬂ_–l¶É<ûtiìì@≈r<ﬁõêâªÔ}7Æ€c%}◊E©.Wõ¨ uv™=ƒt”'M∫i˝≤Ôk†>;¿Y∏ÆGÑ√Í\Ã+¯çái pÒç´3li“0XJ<wå˜˝ºÊö∑§ÜHÚF«ìIÑ„üÈFtcÙÌ¿Ü˚ñº$¸û/òÊxÀ{ç±OÁÆ&≠vc—ÁÒ"ŒtÉö◊ıI~_◊iiüuﬁ\GXßûa!√π˘˜%EÔX¢n}rtMù_Ü]P˘tÌúH¿hÕäxÀπ¬§=1√QH∏iW∞-ˆO ≤∫5ôÃMÁ\*Ö›F˙∫LÍ:Õ€~çÈVnnQLLÑCz¨jõìë‰H:?CÇÈpÃÁ;ë†é9à\aa!^ŒÉÀ–ôOó=¢]{¯è›≈Ã<
«lhl≠y≥Ô=E%ﬂ>JPz”rÈ@ÓMÀ∂yLê3Á≤€yAÑœA™9øà}	…PVÚÑwÓ¬ˇE=^@Ω7Äæ6EôŒ]çBwŒ≈¯˝R¢]ô»R≠zÍé‚aœçˆfsr{K7DÖëñ˘U/©‰πíÀÍâƒe%HÅFÆ›Ñ)6”`KhwKœ9´ó¯g)o…o¡9·(À√âïû∞Ù»7ªt∂dÀMÀ7\	ÌÛù|ur»Èq±õ*7úU‰F"v≈["'∂'y{5b I∑‘xì]„• EdÄ*tsã‘è	øàa¿†óπ#7XDhÜ…›Ü&a0CÒì˙Ü]BQ!à∆Ù@q›‘qÀÄöÂ©Ü¬uîœÕÕ/ó%§®õ 9S∏
‰Í*…ﬁÈï?	¬=Bcâ
ãg*Óô?œc™Z}∆wŸìﬁﬁÁ9˘ﬁ√Òƒﬁ{“üª/^å<'ä‰Øré'™NïÌµ¥€Dˇ∫vÁ¯,‡¶™◊¡g¶ä)ô0GÃB[EÂÉ°Î∏¥h¨M∂çóµo¥Ú&Ù!¿;Ü¿I–_ˇ™e6Ì˛, C—ı=e–≥‡
ø!¸‰ä.l»xaßÍπC≤AâÑ-b◊Î1Ã{¯ìlO¿]•ñd¶‘∞/Ö3FØSΩÃNìûC¢-\T†‰5Úï≠≠∆#"Œí≤Jc<˜ó!«LD÷r„SS?øÍY¬mvD=˛Ê˛#≤∑Óy5·Sz–√&\	rë`c,˙ZA%Õ=∂§ì2·áä¡Ó(Í†œçÿü; ∏∑|XÅî˜± eΩHB,¡Uì0if‰πÛnâ˘X*µ¥›Ù‰uÈæ0hJòRªHÍﬂ…äƒ’Â:ÎÉíhëtœΩöı†ÏÈ‘~∞ã=¨ $≥Tnw£Ã 4,&HPâVA¡*R≥≠¡ﬂ3ÁÍ;NÒﬁVõÄë¡§]…∂é£j.\i'√™∑@îSõ…WÛe◊ÒÊSÇü{O&õeN/SÆ%XÕs}Ê¢Êö—ÎC’°¥6V∆u˝˙¬ì&±t‘|î”_zê\V__(ÿΩCt˝-¬eﬁæ˚˝ˇ=¸“˘ªﬁŒ§”c5&
„¢Ë∑D˝pF”BI§mäÆn=2ˆpqd◊º@“/Ä˛Dπ}Ôù∫ÒõRy'¸Û9v¬Â:Î`	_«YR*.oV/í…¡Tu_VQ∆≠i‘≠Ãê›ºËGÙÿV
Kj]aqù`Y!‹˘É¥ô?Ahã¬xˆíLPªñ¢‹†çZ–⁄tÕ˜‹*pæÁ-∂”W6÷á_r{N0Ìu‹êwÉ¸Äû›<'¯÷
'∏”áÍ€¬°zz+U{ZñÈöi´çÏa=üCè⁄sŒŸF"∫„ﬂÚ%LwHIx∑oTÿ)NÈÎê’ha`e≠™ÉDÂ'›°ﬂÂº ŸË©ü…áT…¥ñÌ∫W.$—Ÿ*%ÿ%R 2}·a$Íà5äÜ< ˘®ùê∫ãE+oÇ¿°¶GVUQ?Nø™ßöü‚ÒI.ï∞æßŒ„ùO∂—…!‘ãîõ^Ÿ{.QêñËb˝ÁÎÖr°`Ö*Z∂ˆÑL8AJ∫§©iŒƒg”Ío…úßÿ©¶°Æ-ÖΩÉú ı	[OÒÂJ/?}+]…e-Œ{≈uMõØÍ§,^ıe≠±mÎ§i¥…¨üB∑Œ	ˆ◊Ω
:´zômŸòwMìÖ—h≈¡ågöìïô·1\⁄Æ4ºv„—T1Fâ’òD≥Í{ZM:'
 k4ÛT'ËîÉ¶7Ïù˘iåÿEÍ
üÜ≥Èbv1∞ñë%EWK}≤§)D|:œ…Q:˜`¨Áùm∆™,“*÷YÖŸZ-s
§l∑]	ªM3∫íÈ\.TêÍôcµ |´¿j|Ù¿˘u©ÂﬂPö¥˙<√2“-⁄ô"†ÖÎ%Ú‹(¶Z c–R˙ádJAcŸ¶˚˚IÚwÊ„Ù”Ãèü§>Â™ﬂ®`Aâ∫+c¨¯>“kÅIp#–¶Ü›ÉﬂMUö›ÀóIüZjá:Ÿ>  jv,¬π©‹hæ‘òœEWÀy:—)»Rï“f‘∞Ÿ”†Mé>¡=∆Öô ›ò 3£uyHÃ√& $•©s¶ïé*ƒ
çœGSw~˛PºR«`q%“÷Ó–UK‘)£8øÊf{E1nÕ,Ÿ∏äÑn§∆_ˇDé√+}€¸7PÀ˝RR:Ù˝˛∑ˇ˜oˇ˙7täoãr®èé≥øˆ¢ò„MP¥‘}Gd¬xÄ…|É‹+å>ÄÊè—Æ;r79fiÇDt@⁄À#»$_}gMU8ô™.ôÇè√™~Ûﬂ[ÍîÜQß(≈LS§¢≈ÏxB§Ró®¨EõÅÔ|¿˛8˜«πf'nHd¬h·y›çWâ3ÎUÔJ¸˙’¥-∑3†#¿Öç¿˜∞ a.OSº7ÛÀπY2’*‹∏,øÊ#«áWÓà«-@πe(≈x,~†∆∂ò&‰Â^CM◊JŒE“⁄Í,®™ÜπÎ≠÷àèØaç2xîŸ-y3Åf‹ô,Ÿ‘∏Û!5A˘'—HƒhK[Üí®Ó¢|ºÛl¢†|+dS◊Ä‰lÀÁ†+Z„ç-*B∆ecº£Ö	“r:®kË5 ¿?k≤™®<2µÙh$KH˚Eî[ÆÙ£H¬2eiù-âµ√;ry˜–O`∆>"
Í‹	ôHg≤F@˙Ë≈VÏ*$ˆ∞ãä§Èd“zã¢Ï2#Z4_ØÎÎµ˜9ù6-Q oı≤uB63òéá_VVÚó^`Xâﬂ˝ì‚FD•JÂH‰êõå0k4''Eså«Ê«Ù{ÌUWw|∂ºJ7§/KÑRm´ÖR˝˚?˝ÛGoÒÃı]¶ÉÉÖç∆R°£E∫¿™…vËìˇ[F.líw‰WÒî∞Ô–'R“£SLT&"{åSE]IìLz]à»®<¨Î œ,È»nD¸ïC?%›‰GPı´ÜAHç¢Ä¥ö(∂*"YÙÏ(àpœrsíöBË„‹˚¬≤ÕíÁÕÖØPèKÚ¥æMÆ&°1Øü©np-ø¨m«;P€9l≠Ê™Í88¥3˛nKfØÅÁËf`
ÍtÓ˛"˙◊8
LúDÂ8)˙˚m¬$QøèÜÿF=Ôü÷Sf~øNπ«ÖµoRaı«WnÑÎbGÛ-Ÿw*d<Øi}’›wÃçY.ujÉ∏!ËÜ·I\Ü8äˆ˝1‘j	B´°&¸¯<’Gr ä‚0¯ÑÈÂJ⁄Ü¸êØw?(÷∑õ[!H®Â˙öAyM}æ«„™Gïè-Xı√Ïz 2;¡@ig€Ü›ÙwõV¬)˛,Yo2}.é6—ÃÖÅKO˚c4e7^ò3KDP%¸élÑ{ñ<sF§3p+3“^j¿∏ƒÒ@|ß`ö∏>sQØ—Yå%OrN∆4È‰aMÙû\u2ÂsŸÈG(
à<ËDhD¥ÜçdW_qÚ©i¶°∂’¿~)AKöR/ÄF)Íœ†íãÃyFWáïeÕ0“^ΩU®í[ú“Õ&h0¬;xMWhP#C‚ñß'©ÿô¬µıÈ◊¨tzG6◊˚Vså*ï3ÃsAî1øÄnŸ∆ã1åöjVî‘:'«ª¨ôÅMQó¸sCæ§ÁF06ﬁDûsÅâ*£ò+¬ºI≈¸êlù‰ó∞Ój˛cÜÄ\0€¿⁄“'T|
bÃ†ö©w=—ÿZπ5∂rj2âﬁzœÁ}≥√6.ÁjvIÆÊ7leÕlí≤:Fö¿ü∂8¥îgîÂlÀ›ô…y˘£hq*Bµ∏õEAOVÿπÓv˛aHt~ˇÚ’è]fubo˜ˇ  ˇˇÏ}ko«ñ‡˜¸ä2·â©å‹í(À±çõd)ääπë%]ív&XÑYîz‘ds∫õíy=fX`Óá;ªwqÅ‹/ôY‡b,∞¿›˝C˘˘	[ß›’Ô™~PN‚ ¶»ÆÍzú:uﬁG˜=R	dµú/JoRbåÙá¸Ñ:Ü±+Âƒπ;so5õYã!π«.Lv_ä:◊øä»¬Í¶V-K:@›Ê–z-Í¨«‘;3±îy∑Q4ï~JPa—-≈oﬂxÀ›'ö¥ø97^+{ßåï†≤bóU
„Ö¡–%√Ú¨∫i'‰∫ JøÚÆIE‹SƒØ˙_÷=dÆI9”V5„©¿∑˛¯/‡≤°µﬁÏ®˘®≤Œø∂<Ê3©SÏ,µ¯EäéÓ√ÜJQÙµó’˚§‚≈œeµaõ}/´∆(ÿkª_S˚‰–2e/&8Âm£R>4;ÃÎ≠'^[Ù.nCöﬁ{:¨èJi7D}~d¸CY6·πÆÙ¨…:zÊUŒ£ΩvUÎU§âáÂr´áY¯I∏ÿyq«‹ó°∏ÚT'7—}yFŸ±ü¶∑a…t? e¯ÇÚ~sy>s‹ÔΩR§òØ'‚¬•æ$S°]‹ß‹k≤ÉÿÙ"Ë∑(ÃP£(v,ºY≈ÊUe	ÎÁ‰ÍÛ÷Sqb∏ó˙Q ¿ôÃ≤!+ˆA$ci˝˜ˇ=c™Ä$ Ÿ·†rR˝}≠ˆæ®”Rßå”íÄ_F6ÄZ≥*ÚÛ	•ÏWÜç}ÙûW«<FOi£b#Ê	‚õJœwÀ(4¬IΩ≥ñXîÈΩ[Wnò¿Áµ„‹^ËQcØUÁ´Îd$Ãóå¶]⁄¶<„ƒ!úgè=kp)V˘=eJDT)sR†„Gü~™¢°/{1%m%‰’ŸJ+çù%Z-—‘©∑ñÎØ»∂é∏µ˝Å∫≠\@•Hı(	‚¿qrH_aﬂ;¶ÁsÛWø¡9WªÛÑn1∆Îø@ﬂÖãA(µKk≠§Æ≈IˇÙÎÒÀãì≥”ØÀﬂ∂ÜwM⁄MñâK√u∞˘ïÍFûù4)ËÃ,óÂ;"WësvÿRïŒıòÉÍ9ó£
ñXÆƒ	_≠ou9∑|(/£õ"1G©¥âLΩÁ6π£—+"m[èÈV°69%aˆ2ö∫Äf{›“P›hDQÏÏ†WØO∆É«ÉqˇÍuáct6<ÍQÔÏÙx0|’ŒN——†{rˆ5jü˚o˙ß„ÍˆzÉ#Ú©{ÇŒ_{/ª£˛H}|@T;òõk§I/íátj∏î‚ìrn8≠◊≤¡∂a‘¥à˘ˇÌä†(‹¥tñ≤ò°‚ÛIMÒË}ôõB_≠,‡É,Ê(´%[â«Œ€#!““_LÈ⁄óÕ≤öCJ:Bƒ
5˛:aıÃï¨‰H∞çoMÆ…K¶/~lOŸ◊}ÈÈr¶+óFî⁄ìJ.ë¸*—W’X|í;GO.KNÂﬂh‰íπï?„UxÍJI»Í∆ &ŸTO1M;S™°œü∏P5ê…mL…Y≈„Me¢®•`ÂµŒ3™åÆùÂíá9WO’û]Ù®r◊
FñÛ8∂T~i∂~Á>3–k2Ö2Tó…[ú;‰RpÆàrçEgœ_€8±¡˛zÈ\πÊÚzm~ÿ∆'¶{UCZıf°7ë NÄfÏ∏¸’UU2’s%T≠
Qõîá™Ù∫t¢Úú"’®GB≤¸Cu≤◊@`b‘Që∫û[RÙﬁçdä∞˙SpÈ™æ® ﬁè 
w‰(q:ÎyuAY· Ô®ﬁCa˘√äıèTøb¥∫ìâ¶r∂œWÓ‰⁄ÑXQ◊Ò	+GÛ	—4ˆÔ]ı9+ƒ{MªlGxnUøp(]º	ƒøgú,_Ë~˘ø!ÜDÃ§E}±OPLìÄ–hiÆi¯-:Ä.ÒÃq1ÚÓ0ﬁhjπçÌ5ÑèNÃ¶;ánx∫jœ7g≥≤âC≤îóŒt=öõU…Ø~äëjÔõΩé3,ª‘êK'oå≈£ï%ŸÚ8óL °∞`+|UÇZ™•_æ≥É®∆]∫ÿºô:w3]∫ø™ <ÄFî35i∑É˛òO∆`—ûõoi>ÖrNÜâ~o9ö∏ém∑Ö´>˚ì∆˝µ∑*≤∞˙Ω˚§>A$º¯Æl¡DjG ö»¬öÕÂZ…Éä; uà< 9!ÑÂköÁA}ú≠ ÅÏ°taV-õáÊÙ=qP‚7^·©•"\’)≤Ä˙w∑ıkM›lƒ.≥ˆ{Ù6#ñ4â‘*^Ÿ1'ÕH;6ês.Í¡ájµJ‘ãQ9Ÿ§R0}ÜhW+mÜ¨hêî˙^⁄\ù4®é±’∞ı^‰˚ñâD∆1öKÊSƒÚ
Aæt«ï˘Õ¶˘ÃF3ÛÀPïÌ‚Óx"/ˆØùi<3-ÂT‚Æ©v‡Í!ÍÖ©ZZo‚ﬁ“-Ê˙kUùÍ’S+ﬁcsW√&¨å ˜bü˛HA‘CAXvœs¶éçê'5F˜d°2ægÒ}QÔ⁄Zxªx˛–ÊHA≈¢y2lÜ[ªjPO¯Vî	ÈzVoHWT
Ô–òB›?Ω≤∫ùÍW5Ë¬Œ>-˜ñR<ú∫#~sf˚Py(«∑,&ÿæHz™≥ óF' n=:ï5≠√S√ÿ™+CBñ˙˛Ù‘å%É≤≤@*+…ﬂTØ qÂa&-ê¡Á
ı*¸0Ø*Äìö^œ†>]–ã’∂´këôA©OΩZs5äWÎ;iƒ[π:$^öKﬂöºƒˆíê”Ö„∫:a¡("¢µFØ{Ω˛hT}ä,(]LÕ7Ö%≈æo’}úÂ◊ı$tip€jaè“@e√˙√·Ÿ∞ÅÌJç)_ê-3û…¸Ωf¶e„©Å∆‡kseZC?Ä<Í—˙◊◊”xH&+q9úßëÿ9î˜„rÑ#˛ß ﬂgŸ”íRrc	>≤‡˛‘Ûõiuﬂ)2´=ë˝k˙/…oc˛ŒŸΩ>|5ç `[9z˚Ëı˘…†Güãpã ÌH¬ç>X‚“NñÚÿq¶"èX≥ò&ZF!å⁄ÜG£b¢”_s„bjOÖÒÿaw/èù>SµPÏ‚êÎ2Nh∫Nf∫°“!%¶>¢·Íî	åÆ Ω˘@Á¶öãÆUü∆≤~ãeéµïMM◊(¨r˚‚˘õ‡mM†B‹bäh‹[aCÂà·*Œß•"Ä7È[GDØ*´¶{T$nEçF÷÷A[¸îÇçA›0÷¥ÇNlkÌ1¨’-…uZã\N4V˘‹∆œ‰≤pí)&òe{Ë€Œùà!D√µSò˜?6‚_c0¢*Rª?US±M∆x(Ï„F¸ì@Rà]⁄Œ‰¶∞ô≤}´.V3^È5{ù´^Xµ{çÎ[˘©@•ÔË]ü[ç(ëj†	◊Õö´Z„Â⁄ô2m¶õv(k˚]4Ê—⁄†”µjNZïßî“€ øåkå£ª?5∫¡‘ÎÓRkYÂBR£J¯©â_—:§€π≈M4™”î)†Ò≥∫r5Os¨t}G∑e≠/Äø!˙¨ÜàpDıv_∑Z‰“\É<O˙ºÉ ˆv‚Rï<tÌ@AvÎ«_∆î›…
∑Û,◊ÃıAØkZ¬9—/¨ß’5Œ&∫ÍπÂ¿’CΩ∏ù∫çD?ıﬂ']ﬁ¢ñº-IRÙ:+T€Ë∫}÷í ü•Ry¢wEËsÆ<Ø}˝G@>n≠MËŒCÙª«®::[†#l[Ñ≠ıªß´uDU Ëk≤Òw¶fGö&ı∆òı˙dπü €Ø¿BÓÏ†CÀôcü»ThÑ'+ƒØO°Ã—-‹TG´•Õ]RD{th.T≤ÿ¸∏”∏q •"pΩ,Î%U6∂˜h~5Ì2√ù∫áp–uÔâ◊≠œrLfå]≤ƒ:Èr“Àº%èñ[≠zœ÷aQœ±¢ˆT·E≠QrGSä	◊ú˙≤Ω@“~r.v»\˘T‹√B«ø‰X?≠ÖÂ[dÈ=Ç+sr—êèhÈ‚•È“∆:)¢öHµYÒ(nÜ€úäCπxì M◊•Óå≈≤≈©Ë◊HQ%úeK‚¡#E^CÍâ48%M3‹î™Q<)ÃÑ£–aïp«‰¡'óte›0µ-≠Ø¬QMn˚Wåj´∞õZ\0h ¿K¯ÿ∆/1ô¯≠9_⁄‘X∏Ú-€2wsûYãió7nŒzŒû‡¡ºÙà° ó €-/¯¶{kZ6†Z0ä2Bõ K·|ﬂ<û/KÜüHK->ñ-Î”;©’%ó !/p'btg˘◊Ú}Y2¥ïú⁄†öÀvç.É[ô&Ö)@˛EÓPdG@"pß‰Ä@c·*U¢ó“âQ’ÜÑ°GêNæ2¨≈≠sÉÀﬂ*ªT"@ªÊc»ç{LÊÇ`2hbcs¡‹'<ﬂÑÑ®”êõ¢%ÑåÃu
 ∆·'∂;⁄≠ÙﬁS:b•ﬁe¨{˘4úÿÔÖﬂ|wŸ˝8“2Bﬂóãx⁄jèà:iä„aÒoÈÆÁz∏öHØç§Ÿ⁄ÒgÂ“⁄{æKPõJ §Oö,ø[°,ñiı≠Q=Ø	°jUÌJéy£hSÙ»F)ÿÛﬁWãÜ“*NXœSÕGŸDˇä¸	W|øtÿÌ}√√k"?œ0ûµÉK¥q0L„í¬]‰ÖÆ§Ù«&;*%?"EW˜¨£oÆör8OìõÌ
üùìÌÉï(¢∆j:π÷ÑäOœ‹˛/¶éäKs5#MΩûDı∏˝‰#
#%t±=|GOÕ*yjŒÒ˚GËñÆ’Jîp1KÅèﬂ…“¬ãI"ΩƒVÊø∞X´∂˜TA≠FNˇûÅ ∫˝ze⁄ ‘¨vô	A•‹¶0PC÷‚–T∏Ω]˙(è„ØeÕã7”Y∞'{◊†Võ2öÔÜ|k˘*¸£m^b(~”˙Ò˚ﬂ˝Å-Coe[(Ê=* ;ëÜ\æs44°’£,%ª⁄1–»\X>S·mpOôîHxcB˜+Ïi¢õ2{˙ÔˇÜá∞p/◊W‰ú!ÎÖ˝˛DwvüÏ,vo!¶t¥$∑È&7◊ÉVŸV©˝˝·èˇä$ÅñŒ˛'∫âO2≤Öè}ÁÒπ„“C˙∆¥WxìªIΩ∞´Ï¶‘Aô„˘?˛O∆"‘ø©UÔ“é¬∂
3«∂
Wﬂ“-ˇ"‡ä{Œ4h*ªAämFÑ±Ê]hÔ«;Œ√º¡Ó•i#ﬁçá⁄gT™3Ì≠÷ñJu‘ÊÊ€ —§è˝Õll^/…Ä¶<Ó[*©ßÑ5ä|¯èﬂˇÈüÄ‚M¿¯Õ8ZÍÉ1Ê?;é<®YO·~ûX<öø¡Ç≈Kc’≈c‡⁄“[ÆÇ‰ÜHŸÎsÊŒ™lyÿá˛qdy◊ÚúınÖ˝N„∑Ç∏Îª¯Vπ¢]îæ^:wËŒÙËAöcYi–ZÚ€‘{_›˜ï—i¯ Pë´ï¸OÍò∑äﬂIëÔ?Ø¢∏4‡	≤€Hu^Z◊ÌVèzj(h_ãı©Ö∫bU/Jå2ÓaZKfµ°ÎwÃóTœ
BÔ±^v£Yöæ–LC%u æ–ÎÊoπ©înGI˛/ßTêÅJù∫‚´Ç"kILëÈÙ7∆ß÷Ï–67‰,µ∆¶ÁÉ!›µ<ÚºçÆ‡ÍÁ¥’3Z∂S][\ÅÈ(áà Uë@‹\R7·W˙›ïÊl‚ ÌdÙnïwB5&ñ≥ÚÄ>π(…∂˙Zbw∆
jm&@ø¶l‚¨›aHáÿ√>pX>÷ãÌ¢CâÛÇ•ªYôñ~$LRJ.”IäæY>iN˝^¢
5˝ˆQé^˚ÕÁ#‘c)®?´œ•á¿àUô≈hﬁ {~v˛˙˚›ì«„¡´>˙f0ÓΩÏü¢ÓI8°ˆÒÏz”?=:¢˛à<“˜è}Ùı˘˘#:≈¿√3bDVÉÉo-ˇ˙‘ÈR¶≈ÛW‡’Gø˜Íz‰Û)qÔà»`¿±_y4ˆ˘∞ﬁNønÅ√&˘Õö¢÷Çº·f·‹ŸxJ‰?÷MlûπC(4*wmÏÚ¡ÈR™πyÁ3tÍ<vñ@}gé;¡Ú0)¯l'KDêŸ(W»ÂÎE“‰_FZÊ’≤˙©E‚‰˘à@¬rÖT_È17Ä"‹∂Aw]A/å¥Òaïk–'èÑÖó∂zésdáNFuõS“ï\á
óqÄÿR:3Ë‰J‹≠œ∑AÏÕ’Ê2¢APé©!ùlﬂR∞oók9iÛÄ}†§í™ß≤k>n'm®C·Pò€TÈÇ÷p†'W=t©z.M/õ=%/zp3i2dr≥üùÈ§Ûêt÷œµjQ’[]¨ﬁW¢∆öà1+ú0 †∑H|î÷\˘Í®F»5„U‹Q«⁄g†r@©–¨Ó6R*∂;¡3€å)„¸ÒxÂC©ıfÁ(•¬ÕΩ§ˇ*_¢Ô√?:YS ùúÅèÓßÄ|ÛÜ»PóP_$∞û¶l˚GT‚≤`√[
ëèπ=Ç˝H'¯∫|¡ıR˚O…lV_™·ÚøÍ±¥ﬂ‚GdØh5ö5,:4A•pÙ‚¬¸j9•É¿ŒZ˛#èHò,pr≈i´∞∑˙õSs÷ïöd>√,>πBãäQH6ë%ı"a!€B3^h!≠; I\Â’<n"ú˙÷¥|Áæ•VUz\8UUZ¥á§*mHàÕcä™Õlá)ôäI.’Ñ.i>–	EXT≈MÉûÿTî˜À;u¸˛|ÈØ€©B@^aùàÖD—–Cál˚ú©é≤óO5»!gBÌú‰ß4{ç‰ îF•!Ωßk•≤–-∫‡do˙Ê‰|ë’Z,Wbı# 52‡sµXq◊öy=ö-"R∫§äJvœç‘ú1îèÈ±Âák-ßUï,Î¢ƒlƒ2£™µ—Uı‘©Ω®;GääÚe3≈MtRëmG í®øCÏ∏ZÒãÕ'›:ï.TØj⁄-f¨1FUÓYYï.@ΩDIY∏‘´’·ËTEè©ü±_´8xùE¿õ…ËPs¶;ÄfÎÔ«Ôˇ^+o0bbùä¥&CçöÄFtñ ıfmP'∫@· T‡ñÂø–µj Ï”#âØmSfVEåC4@◊±H?m2@ﬁ≈—≥O≥Z@˙≈¡} "wFâîCµ¶[(Q)+Æàz™®à∞˘D ˜Ø´dGjj˙¶1«ûgÍîbŸd¢G›‰«˜òÊ†xc5r”V›X_€àP `Sf…„«ˆ‡ﬁı«uπ•IqEQûú1Jû∏tﬁÚ¸I⁄µs'7„:¨î˚'/ÅHz/®∞¥Ì=&a«c@¯pÛ-8YÓ‰b.Ãí›t≥ïgõ®:[1˘	èŒiH7”HxéûËR£»¢¬˙)}u+ã jty‘ô3¿á©©π:kçÈ«’’êç§?1]Û€ëkI%¡áÄÕ÷.˚–Àñ=!â≈Ò“eÆÊ¥Ó€i2bÜÃG¨¡≠øÿËY¯
°ü	ÕéÃ Ó!oΩ t
Y‰Ü&I◊ô£Ód‚ö§˚…ıÇ€^/† ãg˘ÎGxLÔ%‘ºıVó¡‹Uú(ÍhÙ-˜)æÂ¬Å≈ı~I≠üH‰V»øﬁÚ7¥zù⁄Nø‡ùí9B©ÀÀ±%«a36çPA¨ﬁ¶åÜwì%◊Û¨Úi 1 9√1Iï±®l7Omûæ&/èÕ,féÆb∑ñ h≈ö5vhkRØ>QﬂÄFkû:7≠±Ø„YPóv
‡Á[††ƒ}Á¨¿Ô
Écπ˚!jƒÁ,◊ÿÂŒw&‘πôîª%|uë\⁄îgf⁄)≥ßÂ<&‰˙ÒÒÒMo[äŒª˙πÕˆq™˚WŒı´ö∞˘ƒ¸ÕZÁ2ªgN@Ω]J-çﬁµﬂ©™˜)§ìÚRY;“#“:u b◊Ú^/à® Èh{˛∏0}¬’ä·Wãì°N„=@=1c—cjL‡¨≈äï´ÁPæÜ‡û™ä_8Ë5è”À%•˚∫ÊCÕª?[i]"?´¢ﬂW"˘¿◊#öú_iˆJ•Ò◊Æ·®Ω–åËkùiÌZ
öÛéN@£ZéA2‘Mg4Á0$CŒC2îŸ	Ä“ª–Ñ?¥4˚zúöe◊ Jy ˜†¨±…É*<\0πVO¨ÕÌO…Cç™íæ∞iP≠µæló≠◊ß Ø±<-%ù…P,?±U¨¸¢ö}≥^!˘`‹”^ó.™%CM»Úr0üΩÓ…FP•vQ;Í+Œû’˚ÄC•ZñkU’∏5äâè%¸¶‚P⁄è*’±µå»†ﬂJ„ä=ÂJ´(èIL–vçî°	7I@_ˇRêëΩÉ“∏¶Åg˙¸πVãÕÁ ´dÌÆ)è˘Âg•°˙S·˜slπ˘°¡i∞±bœA0§ütªPƒ,9Ö≠F‘ÇR ª∂Q/∑îÖR#W]Å£·õTõf-?L€7ÌúiéHo¸Õ>ÔEJ`Ï6b∫Nf ¯îÏé{·˛”î¢€πåÈfl; ¢@…ƒ‚ÖVj[XB’ÅLµ#í^ÇX,tn.∞› ⁄‹´√æpπèd*ˆN»¡Ó•c∫©J≠"œ˚ÃŒ~R¯?:‡ˇÑ5µÚ%∏·
ÆπP°'∑4w–¢¯ﬁﬁ}{L‡pww+ßBtß±P≤Vw¸:(u*—Øèæ‚)–∞ØxÖÀ$Ì-˜·2û˜kÓèäeﬂN ÷éÊÜ%"\Ó5ØdjÆ¶ñÔ!gñ„?é&ÊS<@óP’£ô›,íÉÁ9ó^Yæ7ÍÔ2ﬁGœÚ|∫]~ﬂ¡≥á·<eµ¥ÅUU≈…tl˛Ì6"üEïC65ÊÊ“∞í”äNG0¨9ˆ]k
µWÿg›æb?∂Y∑P—%2Bµó∞!˘jÛw˝ßñCpït‘˙kÙ’≤çª
G“◊á´Œ4¶'É)†Eõ;≥¿QGˆnsn˚ídı¡;Ÿ3ŸÍ‘	jl4ıMÜ≤¢4µ$¢&–5!lÄ‘’ï<|uÖ›ê‹=àâd*íÈ”ä d^’h¯≥Ù»ÃÁh‚:∂›ƒä˝	ö0Aˇ⁄›3kñ˜£«ñÁ·Ó-π	ÒÄO€hiZ ìŒáPMN‘æ†çY·≠ˆú ä”Í°q∫†¸‡/¬€î∫Ì∑)÷îu⁄%àV¡ÁÙII˚'•dπoÆ◊ã@≥“Ym~ß*y%„8ûí;»u¨È[∏c",Ù£Í„>‹PÎBπÑﬁEC%õ∆uªÀG«Ÿ¯≈;Œ6™iS“åÊªÌß‘ R=@ÈBÎ«Ôˇ{˝Zà2ÏÒ~˛Ù’˙Èà~~[≠A§HWø˚øÂª*Áπ@%!∫aQAÊia˘<`$©FŒŒV∂≈oÚÕ]—)ÏWöB0Ç÷!(¶^†áÔÉ∫ò9r%’WÑ˚!ß	õ-Ù˛}L‹mLÃÇM∫˛È3“2Ë€°åAw–¶Ï~ú˘´ªÏr:–	ëÌK™∞Ç"R≠ø0ˆf-ê+Á¶/~ﬂzè~¯Áˇ⁄bd®u∫”≠ËÊõPÖTÓ≠AóÔI´:}¯Ø¢è±rxÔ≥xıEiPÆ•˛¡◊o°› ÉÀù´b¡gÂ‹\*E15≈‘ùƒ>™PªZ”~gªø	kı‘[Qœt◊pèGIa†Áójf∑˛zÎ=∆ÌÑ?y^ZYPkˆ“,8!˜©ÈûªŒïã=o∞òÇõ!ï:XÚ.©È¥ù±™¬ÇÇv–˘«ùc(y”ﬁ"Ãv'x∞hÔŒ∂Ÿz›\”iP|∆"˘~Îãp	oî'ΩÓÒ¡n]ìJ¬+˙„îUi»∞i`£T·ﬂˇΩd¶Í¢ >»–yﬁG¢†YD!≤™ø8¢–Ÿ{˛Ùxˇ#QPÑÖ˛¯Ø¨™{E‡˜è¥@≤h_œ_8>~˛l˜#k–L+Ω^‰[ﬁØµµ™·%"À9πÈ
øE"˘ﬁq
∑E2Ägh.n`÷≠*Dæ·êä˛‘ÚWd¥>èìH4á]døë°√Ò&…µb-4à∑…€}ka˘‰!0Cûõì’∫∂◊¨ıâ5∑¸¥éÃ[Lﬂf√Y/°7âá˝Wâé€¥q˙~‰Õ¢`…∑“£XÜ¯éúØg˙–Î1—¿Df[∑¯‹áı¥ùµi˚Î—jŒ_¸Ôã•c-|Ô‚“¥©◊+π'+◊≈Ïê˛œÈØâÆ”Üê~∆Õ[”≤ÕK≥û` lHÈ'œYÒ„9{›6¿û„˘€»•_è¡©|M-oB•ﬂù√ˇÛúï¬ÌsiÔ'lÓlHÌŸ'Í•πÙ≠…Kl/		ëçR„9¡€®5z›ÎıG£WjÓHa‹YÄ∆ÿ1=ﬂòõ7ò“Ä∞'∂xäJ~ÄËxëÿ)ñM{j*ë⁄ÔIˇÙÎÒÀã—À≥·xÀ ƒ…puÀ §…√ò∂Î·ÅLtí[èX 3a¸«}e.≥Òx‚Ã¸÷oIce1'î‘%6z¬©;÷K:*&'kVu.G’◊/1A≠k~Ã L*´óåI—2⁄@aô;+´ùæ£¡Y|Ö+Î±Ìc«ô acFAcqpdè:Ut…öHÂe∂∞O:;wÒB5¡^ˆ2g=õæòúÚIOì±N]d- ‹%—∂Ô–ﬂ¡Wûybg:9Ë∂Ë¶:m·ùÉòDX°Äå< 4ù ·s«.è–⁄£Æ7Ja≤∂Ke¢üﬁ"˛˚‰?Ù®˜‡ˇ'≥’"ÉÉ	πñË–q@ƒˆjoÅ+≈ÎÖ‡(d∆„:rV‰¢‡i¡æ⁄}"pp˙A¯Œ-≤Í˛ ]0ƒø5]ÑÉQÀ°˛Î FÇ¨Ÿ|Â√|®ÎÒŸ¨-Ñ∂#"˘.Æ¿#üü§Ïê÷‡+˘%ÕÓ*û⁄Eﬁ’±»‰Ô>“ÿºj∑`ÓótÚS:ÙVåa◊åNm""5xAú¸dy¿Gå&Oã~Ú˝(#ôB≥› D·éˆÁuCADπ›€}ıX@˝∏øÚ1~)ŸZ*$z…`Qè-lgx´‹r—$v®≥®#=«Ωk∞•R
ß_ "±d–Vrœ”[˚ùp.í6µø~˘√˘[≠L◊“&KqMñù.ÉËWÍÏÓªªŸM=Ç«6ÌiÈª´å¥J
ä±Äp@Ö¨¶~1ÉEéSÉ`•í:‹‡5U0ú-Ÿ˝óÔ˜áƒ¯&⁄$[À!˙ØÈ¿êvH'tOß+†ı©='ig9¨~öÅ’ôéeı*u8d©TTÛâÑä¶‡ŒÀVÂ€“7%GÂ√éQƒ7;CLÃHF•>´|Ö”	 HP!¬00.ÂÃ=]Ÿ6arò¶<∑?‡_X_~°Í@√ò¢<ùâÄVŸ=©äG!∏ñw)cKäwî*ëÿ›òµ´˘ﬂ§±≠îI%óÿ‰Üs≈=g1#õÏ{mI$!Ï(cÛ∂·c|*ÀΩ cÒ¸_≈DLê2ÊT ËÒø‹‚O≤æîxR˘ÜÂQT`${ä0ÑzBSŒﬂZMƒ@˜∆¯Rx‚l&∫óûeïSô`ë∞<r˛mã,ÈvkãGÔZ3…%N.€π√ÓƒÙ0¡Õ˜@p| ì‰<c|	ÈˇfÑ;hCA(6¨èº°0öÖ9«Ù2≈sc∆bæîﬂi1≈ﬁ$—b∆’g6$¨vØ0Ω^¢≠É_R€Ëåñd•`F“2∆ëî˙ƒ'ÒZúÀı⁄≠[|Koô˜A6IGtÊko˙‰^∫s‹)S‚¡Œí´„[ìkB}>.˜˛ùYﬁ5¸-ÈøÏåëOûπÇ|ãÒ_öó·“ú∞	~ë€>ŸÊ¸2„<ä #2Ã≈Ñmìr|vÑ∫†ø˚;∫W—/sS)hlûº›zÙ]/ﬁ?qpŸÜ+óPRP›@NI47}≤ã+VΩÊM∞¿tÈôPkî§ÈXºwÊÇ¿vµ›√WWÏIå=∫ssÀ¶Èƒ.|∫vl„÷Œ’ •≠&.6ÁÂ7ökg±0≠ÃÄ
—	~»»–\Ÿ+/œ\åã∞Å=öÇÑd∞ΩûŸd¥t_\ñCÆEò≤´kÏ˚¸± ≥∑È.Mœ7·Îìm•@áKÀõ¨,ÜÊ}~‚87VﬁFF«¶πëuHE„k” u›–›h÷Á6!≥÷‰Z∫˙Ó,ˇömˆ◊lGé…é4≥ÂDDÛâÏ†¥ÁS”r◊)[.qx¨√„ú]˛¢Ë…Üsô≥ëë÷¥è¥œ÷V÷œbòµÓ3}gﬁ6ü]hnü+_iè…s);Læ•'õ¸a,ÆÒ|2Ìπ≥†«˚Œ¥·◊ÏÌî˙Æi3È˚≤~‰£≠u'Iá^ﬁFûíennØM€¥ãvÓﬁî-WrpK^ãsÈNŸ>Núk«¶è`vvÔà$ú≥ùÚãÓÒ∂\8ã«/aa‰{≥∆Eøq™ª5Ø∫wMÑë%#êå{!/±m¡/—M˘I¨¸7tuÍX˙§Ù
ˇÁ¢b04iwv–ô øéæÈüÙ«gßË‰¨{48˝ùw«„˛tT–4a›	UÎ+€√]`©πûàº‚'æûÄæk.<ãgDˆï¡b<~l≠jãˆ⁄íZö‚r-ıdPÆ‘«ÃΩ0X¢òó
§ëuDTÄt%¡oœ•ﬂL1á—S·ìoà	π§‚vT=<zXàPoÔÌÓÓn#lz,¡,Û¿Ï”?c
;óˆ	6IP£CäBø©Y∞4t)XàÒñå˛µÂ‘`MºsMew˙•H©¡˛M5÷Å≈ztÉmL§¿Ë∂8QUë}*œ6ï¿îpnâ5öMz§‰à§›†Y4JóÍÓÃ∏üL`™_%À_4DÚ˜<q…Lq™òöYL5¡/ˇîùo…òÿ÷≤ùeW»h£T*ÃéoœÓ≥Y¨„Ë_ë?rî˝wt—˜R<∑#ËX-ÛR\k‰∑JöÅ”Æ∫Ãîq:Ÿ√ì«ì\◊Õ(¢“z@Ÿœf°ƒì|Áÿäh±◊â£@Úõzó-?Y|ŸûÂU¶πßu€}™∂n˜b˚*Œ'S1_LôõèQ;yƒZûˇ(£JOã3‹ïE⁄∂â£ê˛m3ãYú,Ç/fŒı<Y‚⁄6q˛ íﬂ*ÿ»‚|"u|ID8«]‰7«/™‰{íñÛ)ïŒñ°±’Èk⁄öFWµnê¢j!u£≠°0TfAï–…mm¶0OÂQòB∏LVYE˛0x<∆®T‚´rë—ˆM]f Ÿø4ª‹jn„ÀΩˇXÔÏ˚@UﬂP/[A%›}¶"k˘ˆ>4^!Ò'JEæ≥˝≈¥> ‘Ô◊Q‡ËÔ˜yﬁè¬f74°À∞INZY,9¯Ÿà%Yú4Ä™)B´£Øœè¨[ã0ömçLÑ	$9àœ´‹†~!lÊF<BâXÿá≠⁄©u≈:öD∂bœK.Xö∫6“∞"	)^∞T©;◊óf÷˚Ò˚?˝ı˙'˝√aw|6¸ùè˙C‘;;=Óè«‘=ºÍégßËÏMx“˝NÛq5 ∏ Çª“9l∂πf€myo,œ"è$"ªé≠Öënrh∏Røè∆iƒ√¥l¥4·¿⁄XÆaÛ2$}naé,≥úÆcÔÛ'Oà∞…‡±GãÑ…_ıªâ§·œªª˝ßü?ç¥ _Gæ::ÿ›=~.E0ü®kÔÁ…ê&∂¨Á|∂…c5Åäœw∑JöÄÍeëÒœí§pMû~L)>zåCoÏ´¥∫•/y‹90åŒA¸˘‘k:*Ú¸•Á=ñfápz∆ﬁì∞AjÆzq—±›ûçíÌƒ
F€,∂ ¸M≤B©o ¢3Õ–{µ°;œ™°=2¿0≥Lö:¿Tæ"¬Mˇ91WBß˝ŸO¸6úv”ßH∆˛Sπ @H#¯ﬁÚ=Lã‰π≥‹Ò@„‘®K—k8≤áK∏˝qjRº˚Û‡ÈË∫Ø<|nõ>‰î‚JE — G]Ë)◊Ÿ‚ê‹&Á<wR2t+xåÜöú≠|œöä Øëó3q›ÍëB∆´¥RÈﬁ£ÙÃ†ÁÅPaúñ”Ó◊…`'zŸJÈzã”∫º‡Øûπ∏5S¬Ωr îâdÂÏò–ﬁ≈~1b\DŒ√/E‘$}ö±&IÅ8∏§D’»Æ¡øÀÃ|ÔXπﬂ=lãáç5˙À†•A»Îg!È˘=Ÿùm°ø@{†)& #Ã{≈_¡+ÇNﬂFﬁ6˙ÜyC2]◊gÚ •æ	nq:kAËùa»CÀ”5N]ÛÆgπ©7û‚2Nø»œ1‰öSkÂ…çË˙Œ˘€¢ÙDQÚ:çL\aRü≠ç≥ŸÃ√>Ã˚Ø∂ÈVVWG)fñÆuKÓDNåâ&∂Iˆ-ù{Ä›˚—Ài;¯fù¯Ê6˘–mÚ)Xª©Î±"Tﬁ8ZÜè“Ωy¡ÿ°O∂J0µ?¸ÔˇéÜ˝◊£Ó·IuiÓó|çª√«Ñœ˜µ¡È˘Î1·s_ùüùˆO«Ÿ⁄Ï7˙ÅR≥,9÷°…ìÊ±Çï/‡)¡ÿ≤'X1p∑‰ßXÓA≤$∑∏êxÒGı"oÛhœ‰◊ˆ5\¬£¢Â&èwûåΩûE¿⁄WàU≈xÏ§A|îœ&ÂƒiGıE⁄üπAtZÙßG∏ãËˆáåWh?dÀ∂ÛPL—ˇo•Dπg≈ßè‹JèQ/ŒSØ°(…‡)ä5$1—;<≥	Õàñô(%kaSUû˝03Å}|ﬂià`'ê˘=√õuﬂZﬁ€Ñƒêoä∂˚’¸0%ûóJ¯¶“J9ç]Ï¯—!eÜÕkÈ®Ú^ú?rQ©m¬òW“ÊÇMÌ‚·;zæí„ø2”±ˇÌoÊãçnÅ7qÎ˝≈Chüìﬂ ‹\¯PÅ/?8ΩÒ.˙‰¨|(ËJè˛∂ñè[Wv=õît1#£Uì3˚!BÃ~CpÑû’î¢ﬂô-”g &…f∆∂9Ù«ú∫ºv8–øc∑íµJXëçBÎ~¬¨êW68Ì`F∞∞Füéùœ>˚Ñ0s'·„KÇòStky+rTgºX£$Wô≥(r∏ÿ¥˚÷≥ƒXÄ6>·ÆXí#œÄ~â»:jÇR´Kü0æ\:ûJ≥«¬gÕÔeÓ‹íÁi°ËGD>"úŸ±GP®ı—õ”5}ı99˜´Â#x«N‚Êfì9cN0§◊tDo,Ã©/hÃ]|7◊&#al_~å’rÑJG´ﬂ=˙Æ%=L#ªÕ˘∞ﬁ.¶Í+l:è◊nPvK∫ò˘† ‘µÔ/Ω;;ÑÌ√æw`∞fêo
¬|Ê;‰˙∏!' €±gù›ãøπtÔˆØ'ø1˛∆#'@Í0d≤”Áùö{ÛNgÈ«:
aE{ÎÙ∂ÚÊ≥∑O/óRoíáÒ=Lç#%hbõﬁl'æ5äAñ≥,Ï÷Vl±œ%•«’D7]èUü¶)èË˘Øà[¿KäT'¢«Tˆ=c@ƒ«éˇ≤Ú$™µäTM≈πÍÕÁG˚ù≠¨˝‹{˛˘”#ÈA±g©{ﬁ}÷9àhb"jÜ2Bwr˝∫tîÒ≤""∂*≈∆OÉπgq¨ÇÊ˚‡@£:™¿Á*…ÑÃ CØ:ÜF.∫zqÈºm)ƒT·ˆÙ“¡ÏÊ2}T¥óê˛.“Æ{ÜÎ°„^ÍÕ•pÄdà$èù[uéè]©O2ﬂDoŒ¨B‹îº'»ä⁄e>r)∏)ﬁ5uzëÕƒ@èí/ P6ÀêàÄzˆ,E	”CÉ…;#≥•¶Z:µm†¢˛<¢°>6=ˇlÂèl»ö™®ñA5rJÜåÓ¢¡ftÕìúnró≤9Ÿ(õë!$x8$·Q~∑)iò6,ßqÑ¥G;†˙˚ïõó^P¸ßJ˚U∆k3NY:ˇ}&3áí&¿Ö>úÛ‡1R]óD∑üÔôbxÄÌäm∑ë?K◊∑(L®éJ≠øˇ«ﬂ2/mDyRt|6DÁÉﬁ7ØœdÀqqV«Ôˇø~¯˚ﬂ˝¯˝Ô˛åæ±¸	º?|‚√»Ó.`¯oÉ¡l›‚iz£¨\wJ)Gsrı’ùÒ0ÜÇ
t°Œ]dW(z√§≤c.ïΩÄ=‡ßıÁÉSd˙àÊË∆Æ∆Óéò`∆_“g¶˘D”}rI€∞_$cß¬ÆˇzÖWDﬁ°ÌÜ#tñ—¥Ïﬁ_:”u¡÷’‡^f	K˝äÆÖ4!˙Iñ∏è0-¬BñÜg˙F<’7bŸWÈäÒ<”Hø,õ:π*	;:Êy¬v˙∞H—ÓÅéÒÚ∆cÚqäyÄΩìøë∆lÑ‚Í4*SéVÚk)	WÚ«0”ª§%ß≠"©‰£9|ôïÕ«WTıãwiM‡E©Íl‹“◊Ñ∑oÿ‹úÄß3EƒµŸÈˆcÈı£ )ì˛‘éO^N¶&ëK_âò— 5≤§ê X¬ıS=æp˘yã[]€ìl	£,t„ÚD¯oú¡v`]ù‹ûx™πØæl3Ê_J´ÕfÏÅwK∫üNökN/[{†•9T—
í–#>J¯ë¨÷Õ≤iÓ4õúÄâÎ4Ôé_£K®ÙÂ=ˆÄ”<0vi^n~T»◊1ÜÀ!‹¨Àh À∫ùñc<d †;0.ö·d‰\íÊËà,Mq—≥MkéˆdôAìk"ì”|E÷ë¨ÕµEÛ8yºåπ≥ßÉ¶›k™íƒ»O¬¶–◊D‹éˆ;«ùcÖÔáÀº∑ã∆`)Ö5 IŒ«%ô _≤»cŒlÜ˛£c€Œ≠	ﬁFﬂöÊÕÙÅËx5[¡ûÖ»^ıêKï«æˇÏYøß0ˆ'·jõ∑‰6\W‚£sãÊQbGS,9Õ≥éf6'ÇîZ‰aoAÓSÎ>åı \Ï¸ê-)êÌ#‚#vy/Yª:e¨“†7ÉsÇ”Û%Ÿs`§–9!Œ¿ÄÈÜP† Õkµ”ô¬î<ﬁ9=ûÌOõÍŒ•!~∏w‹UAÊß¡Ú&:òh(cÙ˛˛Ó‹P˙2|Çºg 8|fØ≥˜Ï‡PR∫D4Q“Ö¡ºxDJô ≈p/9äÿvKã*Y
L<◊uH”⁄q7°&4h|£dX¢ñQUúÙC$ÁDÍ+—ƒıM…7*ëù∑¥kTÅ7TRÕ≈Y™™…ÔwçÁùî%„é∞=&ò2Áìº4˘í›3-=|N¨i'≈:[ÃPÚ(“h3»nﬂ∆∑BSF˝LeúFâTÆ7ò{é£yû
∑r8ÊO>†?∑fx¡§™+PPö7Äf¬ôöU1Ëêöòæã+Ωê¡Ñy6·É·.~^åT‰mWf˚¢Ñ‹T[òv)gòËÉÎã´¬Û€ŒVlw
‚?≤”}3ÇR0ñã;.∫˛¯˝?˛ÁÇBz€Zµ+Ú˜_ÇÚw\VNìåóúJ®Jà¸∫+ö˛≠‰⁄íñ#?u[[“ƒ&Wg.&lÌ/.˝Ç¥´)ÿ◊Éû≤–è˛®ë•.ÒïB(Ÿìåj;;à◊∞ÿ KC‚±d—e/ö:SD¯@÷Y.y÷$NéûfR£º§ÈAP•.ß‚ %<Îπ°*ﬁ“yã†Aﬁ∫o∫ÉÍqzrˆ]˜d¸:ÏûtO{˝-Ÿ-¨≠Fi∆qÑQ!„Uœ«£Ë¯:ù‹Òıﬂ˙Æ©:HÅ”e.ñTF?y,ˆnqŸfı(±≈á<‹µ“l/S˛€‡‚>≠Rﬁ	´î«7¯3∏ªıQcmÍΩñèãy«><«≥¡[BíäJqJ∑ i*»ÁÄrÒ Íz/(∆y∂l*mú ÛˆüÙü6ï›`˛B ^/â~“Q‰ùÉ‹k
†.9
†&Y™hN iLWh”œbΩF´…{©ú∞Q•9Ä“ÊÈ80⁄≈œ+
jÈÚ	Äñr˝ ƒÇﬂä	˜˝ÄñK˙˜√É~gW!ëV
5y O
H¨r˛ãÍ®˙]ÜiŒ*åGòÊ@8π∂ñ…R“'Êo÷ô'MˇTî≠6ßÃºÇÀ9aπ˝.y¥Ωyı]è©é¥G¶_¿gHa)ÍŸâôQ)PÄ|êÏÄé2ßn@¨j º1Åõô!rEIDºxH˛óM–¨òóÉ±BÖ—| |Õ“ofÃ>sÜ™∑3ÙÄPGÇﬂ˜ü‰∞ù©å2‚FÚQ?ftÿ¢∫<œ-pÃUª3¬=v«π«/y1¨–Dù-*eä◊äœz/‡Â<gæ5}üUWR@°@^x$$vÍæ*Vh´X;üëõqV˛ëﬂ¡^QJµÇ•—»|ôy˘*cKD7Krüàœ‰IÒR‰Áπ‹œM‹V¿áÚé π™ééñ™C@ù,)@;eP·ﬂ
u˛ Ë˛pÄZû«†Qï‹iëé$i©ë™
®Ï ï"≈$ü2§˙;’ùbC|∫yRÉÇêêQ3UÜ⁄Ñ  …SSó"ˆÅÅ“|â‰“÷¥Ω@Ìò‚¬Ÿ¥¯^ÛÔ%Õ`Å0[ãcT-STpW’‚ª2∞∞1%–Íé_?ˆøÌè?|G´¥∆saÍ°ú◊‰`døøvl¡∑#Gí°Wƒ„OùÜ•)∆ ¥ı]¯Ü˜‡⁄iëgê8≈/–Cò≤⁄\
Ñx|‰îä7ú7P•ïúâc;∞q¨pñe%Åkj/+Wë<9dJbœã@∫„ü®Œ’ã©I;q5iäÔHHp∞Ô‹÷&æÁ\€˝¬Ë_îÁ„v’cÏZßòl|Ç¶†«q˘‚=Z˙q[k±}DÖåî˚U]ì˛ó‰π˝ˇ  ˇˇ ^ß¸L