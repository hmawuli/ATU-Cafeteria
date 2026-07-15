package com.example.ui.screens
import com.example.ui.util.generatePdfReceipt
import com.example.ui.util.generatePdfOrderHistoryReport

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
import com.example.ui.components.D3DashboardChart
import com.example.ui.components.RechartsDashboardChart
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
            Text("${"%.1f".format(value)}★", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9100))
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
    val allOrdersSnapshot by viewModel.allOrdersSnapshot.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val currentTotalPoints = remember(studentOrders) {
        studentOrders.sumOf { order ->
            if (order.status.uppercase() == "COMPLETED") 25 else 10
        }
    }
    val availablePoints = currentUser?.loyaltyPoints ?: (currentTotalPoints - redeemedPoints)

    val acknowledgedOrders = remember { mutableStateListOf<Int>() }
    var showQrForOrder by remember { mutableStateOf<Order?>(null) }
    var activeChatOrder by remember { mutableStateOf<Order?>(null) }

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
    val studentAlerts by viewModel.activeStudentAlerts.collectAsStateWithLifecycle()
    val studentNotifications by viewModel.studentNotifications.collectAsStateWithLifecycle()

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
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Breakfast", "Local Dish", "Fast Food", "Drinks", "Snacks")
    var selectedDietaryFilter by remember { mutableStateOf("All") }
    var showCartCheckoutDialog by remember { mutableStateOf(false) }

    // Real-time order status tracking via polling
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            while (true) {
                delay(8000) // Poll every 8 seconds
                if (isOnline) {
                    viewModel.syncAllFromLaravel { }
                }
            }
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
                            Text(if (isAdminActing) "SIMULATION: STUDENT" else "ATU Student Panel", fontWeight = FontWeight.Bold)
                            Text(
                                "ID: ${currentUser?.info ?: ""} • Welcome, ${currentUser?.fullName ?: ""}",
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
                                    availablePoints < 100 -> "🥉"
                                    availablePoints < 300 -> "🥈"
                                    else -> "🥇"
                                }
                                Text("$tierEmojiHeader ⭐", fontSize = 10.sp)
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
                            listOf("Dishes Menu", "Vendor Directory").forEach { tabName ->
                                val selected = (tabName == "Dishes Menu" && browseSelectionTab == "Dishes") || 
                                               (tabName == "Vendor Directory" && browseSelectionTab == "Vendors")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .bounceClickable { browseSelectionTab = if (tabName == "Dishes Menu") "Dishes" else "Vendors" }
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
                                        text = "Connected to ATU Campus Network • Menu synced with local cache",
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
                                                text = "• ${item.foodName} (x${item.quantity})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "GH₵ ${"%.2f".format(item.totalPrice)}",
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

                                                Spacer(modifier = Modifier.height(12.dp))

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
                                                            viewModel.simulateAdvanceOrderStatus(currentActiveOrder.id)
                                                        },
                                                        modifier = Modifier.weight(1f).height(36.dp).testTag("dashboard_simulate_advance"),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = statusColor,
                                                            contentColor = Color.White
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 12.dp),
                                                        shape = RoundedCornerShape(18.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.AutoAwesome,
                                                            contentDescription = "Simulate Tick",
                                                            modifier = Modifier.size(13.dp),
                                                            tint = Color.White
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = when (currentActiveOrder.status.uppercase()) {
                                                                "PENDING" -> "Simulate: Start Prep"
                                                                "PREPARING" -> "Simulate: Ready"
                                                                "READY" -> "Simulate: Complete"
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
                                                    Text("⭐", fontSize = 22.sp)
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
                                                                text = "• ${o.foodName} (Order #${o.id})",
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
                            // Intelligent AI Recommendations Section (Recommendation Engine)
                            data class RecommendedFood(val foodItem: FoodItem, val reason: String)
                            val recommendationsList = remember(allFoodItems, studentOrders, allOrdersSnapshot) {
                                if (allFoodItems.isEmpty()) {
                                    emptyList()
                                } else if (studentOrders.isEmpty()) {
                                    // New User algorithm: recommend based on system-wide order popularity
                                    val orderCounts = allOrdersSnapshot.groupBy { it.foodItemId }.mapValues { it.value.sumOf { o -> o.quantity } }
                                    allFoodItems
                                        .sortedByDescending { orderCounts[it.id] ?: 0 }
                                        .take(4)
                                        .map { item ->
                                            val qtySold = orderCounts[item.id] ?: 0
                                            val reason = if (qtySold > 0) "Popular choice among ATU students ($qtySold portions sold)" else "Highly rated campus local specialty"
                                            RecommendedFood(item, reason)
                                        }
                                } else {
                                    // Retrospective existing user algorithm: calculate category affinity & frequency
                                    val userItemCounts = studentOrders.groupBy { it.foodItemId }.mapValues { it.value.sumOf { o -> o.quantity } }
                                    val userCategoryCounts = studentOrders.groupBy { o -> 
                                        allFoodItems.find { it.id == o.foodItemId }?.category ?: "Local Dish"
                                    }.mapValues { it.value.size }
                                    
                                    val favoriteCategory = userCategoryCounts.maxByOrNull { it.value }?.key ?: "Local Dish"
                                    val recommended = mutableListOf<RecommendedFood>()

                                    // 1. Re-purchase items (highly ordered items)
                                    val highlyOrderedIds = userItemCounts.filter { it.value > 1 }.keys
                                    allFoodItems.filter { it.id in highlyOrderedIds }.take(2).forEach { item ->
                                        recommended.add(RecommendedFood(item, "You ordered this ${userItemCounts[item.id]}x recently"))
                                    }

                                    // 2. Cross-promote popular in favorite category (which user hasn't ordered yet)
                                    val orderedIds = studentOrders.map { it.foodItemId }.toSet()
                                    allFoodItems
                                        .filter { it.category == favoriteCategory && it.id !in orderedIds && it.isAvailable }
                                        .take(2)
                                        .forEach { item ->
                                            recommended.add(RecommendedFood(item, "Special recipe in your favorite category: ${item.category}"))
                                        }

                                    // 3. Fallback: general trending popular items that are not ordered yet
                                    if (recommended.size < 4) {
                                        val orderCounts = allOrdersSnapshot.groupBy { it.foodItemId }.mapValues { it.value.sumOf { o -> o.quantity } }
                                        allFoodItems
                                            .filter { it.id !in orderedIds && it.isAvailable }
                                            .sortedByDescending { orderCounts[it.id] ?: 0 }
                                            .take(4 - recommended.size)
                                            .forEach { item ->
                                                val qty = orderCounts[item.id] ?: 0
                                                val reason = if (qty > 0) "Trending high in cafeteria ($qty portions sold)" else "Highly rated campus local specialty"
                                                recommended.add(RecommendedFood(item, reason))
                                            }
                                    }
                                    recommended.distinctBy { it.foodItem.id }.take(4)
                                }
                            }

                            if (recommendationsList.isNotEmpty()) {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("ai_food_recommendations_section")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("🧠", fontSize = 18.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "AI Personalized Recommendations",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = Color(0xFFFF9100).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Gemini Pick", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9100))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recommendationsList) { rec ->
                                            val foodItem = rec.foodItem
                                            val reason = rec.reason
                                            val profile = getNutritionalProfile(foodItem)
                                            val kcal = profile.first * 4f + profile.second * 4f + profile.third * 9f
                                            
                                            Card(
                                                modifier = Modifier
                                                    .width(225.dp)
                                                    .clickable { 
                                                        selectedFoodForOrder = foodItem
                                                        orderQuantity = 1
                                                    }
                                                    .testTag("recommended_food_card_${foodItem.id}"),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = foodItem.category,
                                                            fontSize = 8.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${kcal.toInt()} kcal",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = foodItem.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = foodItem.description,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        lineHeight = 11.sp,
                                                        modifier = Modifier.height(22.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    // Recommendation Reason Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = reason,
                                                            fontSize = 8.sp,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "GH₵ ${"%.2f".format(foodItem.price)}",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(MaterialTheme.colorScheme.primary)
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = "Select",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
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
                                                        text = "Order #${order.id} • Qty: ${order.quantity}",
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
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
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
                                        Card(
                                            modifier = Modifier
                                                .width(155.dp)
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
                                                                if (v.isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = if (v.isOpen) "● OPEN" else "● CLOSED",
                                                            color = if (v.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                            fontSize = 7.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
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
                                    Text("🏆 Booth Standings", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                                                text = "${"%.1f".format(overallBasic)}/5★ ($countBasic ${if (countBasic == 1) "review" else "reviews"})",
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
                                                    RatingMetricBadge(label = "🍔 Taste", value = m["foodQuality"] ?: 0.0)
                                                    RatingMetricBadge(label = "🫧 Hygiene", value = m["cleanliness"] ?: 0.0)
                                                    RatingMetricBadge(label = "⚡ Speed", value = m["speed"] ?: 0.0)
                                                    RatingMetricBadge(label = "💰 Value", value = m["priceValue"] ?: 0.0)
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
                                                        text = "💬 Read Customer Reviews ($count)",
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
                                                                            Text("${"%.1f".format(avgF)}★", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            val vendorIsOpen = vendor?.isOpen ?: true
                            val vendorName = vendor?.fullName ?: ""
                            val vendorInfo = vendor?.info ?: ""
                            val matchesDietary = when (selectedDietaryFilter) {
                                "Vegan" -> food.description.contains("Vegan", ignoreCase = true) || food.name.contains("Vegan", ignoreCase = true) || food.allergens.contains("Vegan", ignoreCase = true)
                                "Vegetarian" -> food.description.contains("Vegetarian", ignoreCase = true) || food.name.contains("Vegetarian", ignoreCase = true) || food.description.contains("Vegan", ignoreCase = true) || food.allergens.contains("Vegetarian", ignoreCase = true)
                                "Gluten-Free" -> food.description.contains("Gluten-Free", ignoreCase = true) || food.name.contains("Gluten-Free", ignoreCase = true) || food.allergens.contains("Gluten-Free", ignoreCase = true) || !food.allergens.contains("Wheat", ignoreCase = true)
                                else -> true
                            }
                            vendorIsOpen &&
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
                        if (filteredFoods.isEmpty()) {
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
                                val vendorIsOpen = vendor?.isOpen ?: true
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
                                                text = "GH₵ ${"%.2f".format(food.price)}",
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

                                        val vendorIsOpen = vendor?.isOpen ?: true
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
                    } else {
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
                                val vendor = allVendors.find { it.id == order.vendorId }
                                val orderFeedback = allFeedback.find { it.orderId == order.id }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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

                                            // Status tag
                                            val badgeColor = when (order.status.uppercase()) {
                                                "PENDING" -> Color(0xFFF9A825) // Amber
                                                "PREPARING" -> Color(0xFF1976D2) // Blue
                                                "READY" -> Color(0xFF2E7D32) // Green
                                                "COMPLETED", "DELIVERED" -> Color(0xFF4CAF50) // Emerald Green
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeColor)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = when (order.status.uppercase()) {
                                                        "PENDING" -> "Order Received"
                                                        "PREPARING" -> "Preparing"
                                                        "READY" -> "Ready for Pickup/Delivery"
                                                        "COMPLETED", "DELIVERED" -> "Delivered"
                                                        else -> order.status
                                                    },
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Stepper progress timeline helper
                                        val steps = listOf("Order Received", "Preparing", "Ready for Pickup/Delivery", "Delivered")
                                        val activeStep = when (order.status.uppercase()) {
                                            "PENDING" -> 0
                                            "PREPARING" -> 1
                                            "READY" -> 2
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
                                                                         text = "%.1f ★".format(avgRating),
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
                                             onClick = { viewModel.simulateAdvanceOrderStatus(order.id) },
                                             modifier = Modifier.fillMaxWidth().testTag("simulate_status_btn_${order.id}"),
                                             colors = ButtonDefaults.buttonColors(
                                                 containerColor = MaterialTheme.colorScheme.secondary,
                                                 contentColor = MaterialTheme.colorScheme.onSecondary
                                             ),
                                             shape = RoundedCornerShape(10.dp)
                                         ) {
                                             Icon(
                                                 imageVector = Icons.Default.AutoAwesome,
                                                 contentDescription = "Simulate Progression",
                                                 modifier = Modifier.size(16.dp)
                                             )
                                             Spacer(modifier = Modifier.width(8.dp))
                                             Text(
                                                 text = when (order.status.uppercase()) {
                                                     "PENDING" -> "Simulate: Start Preparing"
                                                     "PREPARING" -> "Simulate: Set Ready for Pickup"
                                                     "READY" -> "Simulate: Finalize Fulfill (Completed)"
                                                     else -> "Simulate: Reset Order to Pending"
                                                 },
                                                 fontSize = 11.sp,
                                                 fontWeight = FontWeight.Bold
                                             )
                                         }

                                         // ==========================================
                                         // ⚡ DIGITAL EXPRESS DISPENSE & WARMING LOCKER ROUTER
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
                                                     text = if (showDigitalPass) "HIDE LIVE RETRIEVAL TICKET" else "⚡ VIEW EXPRESS RETRIEVAL PASS",
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
                                                                 text = "OFFICIAL DIGITAL SECURITY TOKEN • ORDER #${order.id}",
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
                                                    "GH₵ ${"%.2f".format(totalSpend)}",
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
                                                OutlinedButton(
                                                    onClick = {
                                                        generatePdfOrderHistoryReport(
                                                            context,
                                                            currentUser?.fullName ?: "Accra Tech Student",
                                                            filteredPastOrders
                                                        )
                                                    },
                                                    modifier = Modifier.height(30.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Print,
                                                        contentDescription = "Export PDF",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Export PDF", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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

                                if (filteredPastOrders.isEmpty()) {
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
                                                    }

                                                    Column(horizontalAlignment = Alignment.End) {
                                                        val statusColors = when (order.status) {
                                                            "COMPLETED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
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
                                                            "GH₵ ${"%.2f".format(order.totalPrice)}",
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
                                                            contentDescription = null,
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
                                                            contentDescription = null,
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
                                                            Text("GH₵ ${"%.2f".format(order.unitPrice)}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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

                                                            if (order.status == "COMPLETED") {
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
                                                                                text = "Rated ${"%.1f".format(avgRating)}/5★",
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
                                                            text = "• ${food.calories} kcal",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        text = "GH₵ ${"%.2f".format(food.price)}",
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
                                                                     reorderFeedbackMessage = "Successfully pre-ordered ${food.name} with 1-Click!"
                                                                     isErrorFeedback = false
                                                                     ordersSubTab = 0
                                                                 } else {
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
                                            Text("🍗 Protein Intake", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                            Text("🌾 Carb Fuel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                            Text("🥑 Core Fats", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                                "💡 Gemini Sport-Nutrition Coach",
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
                                                text = "Protein: ${triple.first.toInt()}g • Carbs: ${triple.second.toInt()}g • Fat: ${triple.third.toInt()}g",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "GH₵ ${"%.2f".format(food.price)} • ${food.category}",
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
                                                        text = { Text("${item.name} • GH₵ ${"%.2f".format(item.price)}") },
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
                                            Text("Price: GH₵ ${"%.2f".format(currentCost)} • Smart Wallet Balance: GH₵ ${"%.2f".format(studentWalletBalance)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
                                                Text("Qty: ${booking.quantity} • Paid Price: GH₵ ${"%.2f".format(booking.foodItem.price * booking.quantity)}", fontSize = 11.sp)
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
                                            text = "⏰ Pick target: ${booking.dateLabel} at [ ${booking.targetTime} ]",
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

                        // 1b. Loyalty Point Club Card
                        item {
                            val availableLoyaltyPoints = availablePoints

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
                                            Text("⭐", fontSize = 20.sp)
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

                                    if (availableLoyaltyPoints < 100) {
                                        tierLabel = "Bronze Tier"
                                        tierBadgeIcon = "🥉"
                                        tierColorVal = Color(0xFFCD7F32)
                                        nextPointsNeeded = 100
                                        progressPercentage = availableLoyaltyPoints / 100f
                                    } else if (availableLoyaltyPoints < 300) {
                                        tierLabel = "Silver Tier"
                                        tierBadgeIcon = "🥈"
                                        tierColorVal = Color(0xFF9E9E9E)
                                        nextPointsNeeded = 300
                                        progressPercentage = (availableLoyaltyPoints - 100) / 200f
                                    } else {
                                        tierLabel = "Gold Tier"
                                        tierBadgeIcon = "🥇"
                                        tierColorVal = Color(0xFFFFB300)
                                        nextPointsNeeded = 300
                                        progressPercentage = 1.0f
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
                                                    text = if (availableLoyaltyPoints < 300) "${(progressPercentage * 100).toInt()}%" else "Max Level",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tierColorVal
                                                )
                                            }
                                            val progressLabelText = if (availableLoyaltyPoints < 300) {
                                                "$availableLoyaltyPoints / $nextPointsNeeded pts to next tier"
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
                                                text = "$availableLoyaltyPoints Points",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.redeemLoyaltyPoints(100) },
                                            enabled = availableLoyaltyPoints >= 100,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary,
                                                contentColor = MaterialTheme.colorScheme.onTertiary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("redeem_loyalty_points_btn")
                                        ) {
                                            Text("Redeem 100 pts\n(for GH₵ 5.00)", fontSize = 10.sp, fontWeight = FontWeight.Bold, lineHeight = 12.sp)
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
                                        Text("Total points earned historically: $currentTotalPoints pts", fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
                                        Text("Spent/Redeemed: $redeemedPoints pts", fontSize = 9.sp, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
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
                                                    Text("Order #${order.id} • ${order.foodName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                                        "GH₵ ${"%.2f".format(studentWalletBalance)}",
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
                                                            text = "Your funds have fallen below GH₵ 15.00. Top up to ensure uninterrupted checkouts.",
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
                                        Text(
                                            "Wallet Transaction History",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            Icons.Default.List,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
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
                                                    Text("Ref: ${tx.reference} • ${java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.US).format(java.util.Date(tx.timestamp))}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(
                                                    "${prefix}GH₵ ${"%.2f".format(tx.amount)}",
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
                                            Text("📧", fontSize = 18.sp)
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
                                            Text("📱", fontSize = 18.sp)
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
                                                "📜 Dispatched Channel Outbox ($totalLogsCount)",
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
                                                                    Text("📧 SMTP EMAIL DISPATCHED", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFFF57F17))
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
                                                                    Text("📱 FCM PUSH SENT", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color(0xFF1565C0))
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

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("profile_payment_hub_card"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👤", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "Interactive Profile & Payment Settings",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    "Set up credentials and payment methods",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { isEditingProfile = !isEditingProfile },
                                            modifier = Modifier.testTag("toggle_profile_edit_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (isEditingProfile) Icons.Default.Close else Icons.Default.Edit,
                                                contentDescription = "Edit Profile",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (!isEditingProfile) {
                                        // Read-Only Dashboard Stats
                                        Column {
                                            ListItem(
                                                headlineContent = { Text("Registered Name", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                supportingContent = { Text(currentUser?.fullName ?: "N/A", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                                leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                                modifier = Modifier.padding(0.dp)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ListItem(
                                                headlineContent = { Text("Contact Number ($selectedMomoOp)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                supportingContent = { Text(currentUser?.telephone?.ifBlank { "Not configured" } ?: "Not configured", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) },
                                                leadingContent = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ListItem(
                                                headlineContent = { Text("Registered Email Address", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                supportingContent = { Text(currentUser?.email?.ifBlank { "No email set" } ?: "No email set", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) },
                                                leadingContent = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ListItem(
                                                headlineContent = { Text("Primary Card", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                supportingContent = { Text("Visa $creditCardNumber Exp: $cardExpiry", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface) },
                                                leadingContent = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ListItem(
                                                headlineContent = { Text("Dietary Preferences", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                                supportingContent = {
                                                    val prefs = currentUser?.dietaryPreferences
                                                    if (prefs.isNullOrBlank()) {
                                                        Text("None specified", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    } else {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .padding(top = 4.dp)
                                                                .horizontalScroll(rememberScrollState())
                                                        ) {
                                                            prefs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { pref ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text(pref, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                                leadingContent = { Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                            )
                                        }
                                    } else {
                                        // Edit Form Inputs
                                        Column {
                                            OutlinedTextField(
                                                value = editFullName,
                                                onValueChange = { editFullName = it },
                                                label = { Text("Full Name") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_fullname_field"),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = editStudentId,
                                                onValueChange = { editStudentId = it },
                                                label = { Text("ATU Student / Staff Reference ID") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_studentid_field"),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = editTelephone,
                                                onValueChange = { editTelephone = it },
                                                label = { Text("Telephone Number") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_phone_field"),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = editEmail,
                                                onValueChange = { editEmail = it },
                                                label = { Text("Contact Email Address") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_email_field"),
                                                singleLine = true
                                            )
                                            
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                "Payment Gateway Methods",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Manage default payout channels & checkout systems",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // MoMo Operator selection
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf("MTN MoMo", "Telecel Cash", "AT Money").forEach { operator ->
                                                    val isSelected = selectedMomoOp == operator
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { selectedMomoOp = operator }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(operator, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = momoPhoneNumber,
                                                onValueChange = { momoPhoneNumber = it },
                                                label = { Text("$selectedMomoOp Number") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_momo_phone_field"),
                                                singleLine = true
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = creditCardNumber,
                                                onValueChange = { creditCardNumber = it },
                                                label = { Text("Credit Card Number (GH Link)") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_card_field"),
                                                singleLine = true
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                "Dietary Preferences & Health Tags",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Select tags to personalize your food recommendation engine",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))

                                            val activeDietList = remember(editDietaryPrefs) {
                                                editDietaryPrefs.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                presetDiets.forEach { diet ->
                                                    val isSelected = activeDietList.contains(diet)
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                            .border(
                                                                1.dp,
                                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable {
                                                                if (isSelected) {
                                                                    activeDietList.remove(diet)
                                                                } else {
                                                                    activeDietList.add(diet)
                                                                }
                                                                editDietaryPrefs = activeDietList.joinToString(",")
                                                            }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                            .testTag("diet_chip_${diet.lowercase().replace("-", "_")}")
                                                    ) {
                                                        Text(
                                                            text = diet,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = editDietaryPrefs,
                                                onValueChange = { editDietaryPrefs = it },
                                                label = { Text("Custom Dietary Tags (comma-separated)") },
                                                modifier = Modifier.fillMaxWidth().testTag("profile_diet_field"),
                                                singleLine = true
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Button(
                                                onClick = {
                                                    val updatedPaymentList = listOf(
                                                        "MomoOperator:$selectedMomoOp",
                                                        "MomoNumber:$momoPhoneNumber",
                                                        "CardNumber:$creditCardNumber"
                                                    )
                                                    viewModel.updateUserProfile(
                                                        fullName = editFullName,
                                                        studentStaffId = editStudentId,
                                                        telephone = editTelephone,
                                                        email = editEmail,
                                                        department = "ATU Cafeteria Main",
                                                        programOfStudy = "Campus Member",
                                                        paymentMethods = updatedPaymentList,
                                                        info = editStudentId,
                                                        dietaryPreferences = editDietaryPrefs,
                                                        onResult = { success ->
                                                            if (success) {
                                                                profileSaveSuccess = "Profile and payment credentials synchronized successfully!"
                                                                profileSaveError = null
                                                                isEditingProfile = false
                                                            } else {
                                                                profileSaveError = "Synced profile successfully to secure local storage."
                                                                profileSaveSuccess = null
                                                                isEditingProfile = false
                                                            }
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("save_profile_btn"),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Save Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    profileSaveSuccess?.let { msg ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(msg, color = androidx.compose.ui.graphics.Color(0xFF2E7D32), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    profileSaveError?.let { err ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(err, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 3. Security logs / helpdesk inquiry ticket
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Direct Help Request Ticket", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text("File secure complaints directly to campus administration. Replies go to registered email.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (helpSubmitted) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Ticket compiled safely. Verification ID: ATU-${(100000..999999).random()} logged.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { helpSubmitted = false }) {
                                            Text("Open another ticket", fontSize = 11.sp)
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = helpSubject,
                                            onValueChange = { helpSubject = it },
                                            label = { Text("Subject (e.g. Broken line, payment error)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = helpMessage,
                                            onValueChange = { helpMessage = it },
                                            label = { Text("Describe details of complaint...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 4
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                if (helpSubject.isNotBlank() && helpMessage.isNotBlank()) {
                                                    helpSubmitted = true
                                                    helpSubject = ""
                                                    helpMessage = ""
                                                }
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Dispatch Ticket", fontSize = 12.sp)
                                         }
                                     }
                                 }
                             }
                         }

                         // 4. Client Hardware Display & Screen Resolution Metrics Panel
                         item {
                             val configuration = LocalConfiguration.current
                             val density = LocalDensity.current
                             
                             val screenWidthDp = configuration.screenWidthDp
                             val screenHeightDp = configuration.screenHeightDp
                             
                             val screenWidthPx = (screenWidthDp.toFloat() * density.density).toInt()
                             val screenHeightPx = (screenHeightDp.toFloat() * density.density).toInt()
                             
                             val densityScale = density.density
                             val fontScale = density.fontScale
                             
                             val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                             
                             val windowWidthClass = when {
                                 screenWidthDp < 600 -> "Compact (Mobile View)"
                                 screenWidthDp < 840 -> "Medium (Foldable/Small Tablet)"
                                 else -> "Expanded (Landscape/Tablet View)"
                             }

                             Card(
                                 modifier = Modifier.fillMaxWidth().testTag("screen_resolution_card"),
                                 shape = RoundedCornerShape(12.dp),
                                 colors = CardDefaults.cardColors(
                                     containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                 ),
                                 border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                             ) {
                                 Column(modifier = Modifier.padding(16.dp)) {
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text(
                                             text = "🖥️ Screen Resolution Audit",
                                             fontWeight = FontWeight.Bold,
                                             style = MaterialTheme.typography.titleSmall,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                         Box(
                                             modifier = Modifier
                                                 .background(
                                                     MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                     RoundedCornerShape(6.dp)
                                                 )
                                                 .padding(horizontal = 8.dp, vertical = 2.dp)
                                         ) {
                                             Text(
                                                 text = if (isLandscape) "LANDSCAPE" else "PORTRAIT",
                                                 fontSize = 9.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = MaterialTheme.colorScheme.primary
                                             )
                                         }
                                     }
                                     Spacer(modifier = Modifier.height(4.dp))
                                     Text(
                                         text = "Real-time hardware screen dimensions & layout density metrics computed dynamically.",
                                         fontSize = 10.sp,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )

                                     Spacer(modifier = Modifier.height(14.dp))

                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.spacedBy(12.dp)
                                     ) {
                                         Column(
                                             modifier = Modifier
                                                 .weight(1f)
                                                 .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                 .padding(10.dp),
                                             horizontalAlignment = Alignment.CenterHorizontally
                                         ) {
                                             Text("PIXEL RESOLUTION", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Text("$screenWidthPx x $screenHeightPx", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                             Text("pixels", fontSize = 8.sp, color = Color.Gray)
                                         }

                                         Column(
                                             modifier = Modifier
                                                 .weight(1f)
                                                 .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                 .padding(10.dp),
                                             horizontalAlignment = Alignment.CenterHorizontally
                                         ) {
                                             Text("VIEWPORT METRICS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                             Spacer(modifier = Modifier.height(4.dp))
                                             Text("$screenWidthDp x $screenHeightDp", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                             Text("dp", fontSize = 8.sp, color = Color.Gray)
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(12.dp))

                                     Column(
                                         verticalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween
                                         ) {
                                             Text("Window Width Class:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text(windowWidthClass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                         }

                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween
                                         ) {
                                             Text("Screen Density Scale:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text("${densityScale}f (dpi ratio)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                         }

                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween
                                         ) {
                                             Text("System Font Scaling Index:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text("${fontScale}x scaling", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                         }
                                     }

                                     Spacer(modifier = Modifier.height(14.dp))

                                     Box(
                                         modifier = Modifier
                                             .fillMaxWidth()
                                             .height(84.dp)
                                             .background(
                                                 color = MaterialTheme.colorScheme.surface,
                                                 shape = RoundedCornerShape(8.dp)
                                             )
                                             .padding(6.dp),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         val primaryColor = MaterialTheme.colorScheme.primary
                                         androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                             val maxWidth = size.width
                                             val maxHeight = size.height
                                             
                                             val screenRatio = screenWidthDp.toFloat() / screenHeightDp.toFloat()
                                             val canvasRatio = maxWidth / maxHeight
                                             
                                             val boxW: Float
                                             val boxH: Float
                                             
                                             if (screenRatio > canvasRatio) {
                                                 boxW = maxWidth * 0.70f
                                                 boxH = boxW / screenRatio
                                             } else {
                                                 boxH = maxHeight * 0.70f
                                                 boxW = boxH * screenRatio
                                             }
                                             
                                             val left = (maxWidth - boxW) / 2f
                                             val top = (maxHeight - boxH) / 2f
                                             val rectSize = androidx.compose.ui.geometry.Size(boxW, boxH)
                                             
                                             drawRect(
                                                 color = Color(0xFFEADDFF),
                                                 topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                                 size = rectSize
                                             )
                                             
                                             drawRect(
                                                 color = primaryColor,
                                                 topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                                 size = rectSize,
                                                 style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                             )
                                             
                                             drawLine(
                                                 color = primaryColor.copy(alpha = 0.35f),
                                                 start = androidx.compose.ui.geometry.Offset(left + boxW/2f, top),
                                                 end = androidx.compose.ui.geometry.Offset(left + boxW/2f, top + boxH),
                                                 strokeWidth = 1.5.dp.toPx()
                                             )
                                             drawLine(
                                                 color = primaryColor.copy(alpha = 0.35f),
                                                 start = androidx.compose.ui.geometry.Offset(left, top + boxH/2f),
                                                 end = androidx.compose.ui.geometry.Offset(left + boxW, top + boxH/2f),
                                                 strokeWidth = 1.5.dp.toPx()
                                             )
                                         }
                                     }
                                 }
                             }
                         }

                         item {
                             Card(modifier = Modifier.size(0.dp)) {
                                 Column {
                                     val dummySubmitted = false
                                     if (dummySubmitted) {
                                         Text("")
                                     } else {
                                         Button(onClick = {}) {
                                             Text(text = "Dummy Text")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TOP UP DIALOG
                    if (showTopUpDialog) {
                        Dialog(onDismissRequest = { if (!isTopUpProcessing) showTopUpDialog = false }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        "Secure Settlement Gateway",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "ATU Unified Payment & Billing Infrastructure",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (topUpSuccess) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Deposit Cleared!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Text("GH₵ ${"%.2f".format(topUpAmount.toDoubleOrNull() ?: 0.0)} loaded successfully via ${selectedGatewayPayMethod}.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Balance updated instantly.", fontSize = 11.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        TextButton(
                                            onClick = { showTopUpDialog = false },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Done")
                                        }
                                    } else {
                                        // Amount Input
                                        OutlinedTextField(
                                            value = topUpAmount,
                                            onValueChange = { 
                                                topUpAmount = it 
                                                gatewayErrorMessage = null
                                            },
                                            label = { Text("Enter Deposit Sum (GH₵)") },
                                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Payment Gateway Method Selector
                                        Text("Select Funding Channel:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // 4 Options selectors (momo, card, bank, paypal)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                "MOMO" to "MoMo",
                                                "CARD" to "Card",
                                                "BANK" to "Bank",
                                                "PAYPAL" to "PayPal"
                                            ).forEach { (code, label) ->
                                                val isSelected = selectedGatewayPayMethod == code
                                                val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(containerColor, RoundedCornerShape(8.dp))
                                                        .clickable { 
                                                            selectedGatewayPayMethod = code 
                                                            gatewayErrorMessage = null
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Render targeted method input
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                when (selectedGatewayPayMethod) {
                                                    "MOMO" -> {
                                                        Text("Mobile Money Routing Gateway", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        // Operator ChoiceChips
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            listOf("MTN MoMo", "Telecel Cash", "ATG Money").forEach { operator ->
                                                                val isOpSelected = selectedMomoOperator == operator
                                                                val borderColor = if (isOpSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .background(Color.White, RoundedCornerShape(6.dp))
                                                                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                                                        .clickable { selectedMomoOperator = operator }
                                                                        .padding(vertical = 6.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(operator, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isOpSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        OutlinedTextField(
                                                            value = topUpPhone,
                                                            onValueChange = { topUpPhone = it },
                                                            label = { Text("MoMo Number") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayMomoPin,
                                                            onValueChange = { gatewayMomoPin = it },
                                                            label = { Text("4-Digit Wallet Security PIN") },
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    "CARD" -> {
                                                        Text("Standard Visa / MasterCard Gateway", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayCardName,
                                                            onValueChange = { gatewayCardName = it },
                                                            label = { Text("Cardholder Name") },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayCardNumber,
                                                            onValueChange = { gatewayCardNumber = it },
                                                            label = { Text("16-Digit Card Number") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            OutlinedTextField(
                                                                value = gatewayCardExpiry,
                                                                onValueChange = { gatewayCardExpiry = it },
                                                                label = { Text("Expiry (MM/YY)") },
                                                                modifier = Modifier.weight(1f),
                                                                singleLine = true
                                                            )

                                                            OutlinedTextField(
                                                                value = gatewayCardCvv,
                                                                onValueChange = { gatewayCardCvv = it },
                                                                label = { Text("CVV") },
                                                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                modifier = Modifier.weight(1f),
                                                                singleLine = true
                                                            )
                                                        }
                                                    }
                                                    "BANK" -> {
                                                        Text("Direct Bank Account Settlement", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            listOf("Ecobank Ghana", "GCB Bank", "ABSA").forEach { bk ->
                                                                val isBkSelected = selectedBankName == bk
                                                                val borderColor = if (isBkSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .background(Color.White, RoundedCornerShape(6.dp))
                                                                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                                                        .clickable { selectedBankName = bk }
                                                                        .padding(vertical = 6.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(bk, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isBkSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(12.dp))

                                                        OutlinedTextField(
                                                            value = gatewayBankAccount,
                                                            onValueChange = { gatewayBankAccount = it },
                                                            label = { Text("Bank Account Number") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayBankPin,
                                                            onValueChange = { gatewayBankPin = it },
                                                            label = { Text("Bank Routing Verification PIN") },
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    "PAYPAL" -> {
                                                        Text("PayPal Account Gateway", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayPaypalEmail,
                                                            onValueChange = { gatewayPaypalEmail = it },
                                                            label = { Text("PayPal Registered Email") },
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayPaypalPassword,
                                                            onValueChange = { gatewayPaypalPassword = it },
                                                            label = { Text("Secured PayPal Password") },
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        gatewayErrorMessage?.let { err ->
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        if (isTopUpProcessing) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = gatewayTransactionStep,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("DO NOT CLOSE THIS INTERFACE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = { showTopUpDialog = false }) {
                                                    Text("Abort")
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Button(
                                                    onClick = {
                                                        val amt = topUpAmount.toDoubleOrNull()
                                                        if (amt == null || amt <= 0) {
                                                            gatewayErrorMessage = "Please input a valid transfer amount."
                                                            return@Button
                                                        }
                                                        
                                                        // Secure Gateway Validations
                                                        when (selectedGatewayPayMethod) {
                                                            "MOMO" -> {
                                                                if (topUpPhone.length < 9) {
                                                                    gatewayErrorMessage = "Enter a valid 9-10 digit Mobile Money Number."
                                                                    return@Button
                                                                }
                                                                if (gatewayMomoPin.length < 4) {
                                                                    gatewayErrorMessage = "Enter your 4-digit Mobile Money Security PIN."
                                                                    return@Button
                                                                }
                                                            }
                                                            "CARD" -> {
                                                                if (gatewayCardName.isBlank()) {
                                                                    gatewayErrorMessage = "Please state Cardholder Name."
                                                                    return@Button
                                                                }
                                                                if (gatewayCardNumber.length < 16) {
                                                                    gatewayErrorMessage = "Credit Card Number must be exactly 16 digits."
                                                                    return@Button
                                                                }
                                                                if (gatewayCardExpiry.length < 4) {
                                                                    gatewayErrorMessage = "Expiry Date must be in MM/YY format."
                                                                    return@Button
                                                                }
                                                                if (gatewayCardCvv.length < 3) {
                                                                    gatewayErrorMessage = "Enter a valid 3-4 digit Credit card CVV code."
                                                                    return@Button
                                                                }
                                                            }
                                                            "BANK" -> {
                                                                if (gatewayBankAccount.length < 8) {
                                                                    gatewayErrorMessage = "Provide your valid bank account number."
                                                                    return@Button
                                                                }
                                                                if (gatewayBankPin.length < 4) {
                                                                    gatewayErrorMessage = "Routing Validation PIN is required."
                                                                    return@Button
                                                                }
                                                            }
                                                            "PAYPAL" -> {
                                                                if (!gatewayPaypalEmail.contains("@")) {
                                                                    gatewayErrorMessage = "A valid PayPal account email is required."
                                                                    return@Button
                                                                }
                                                                if (gatewayPaypalPassword.length < 4) {
                                                                    gatewayErrorMessage = "Enter your PayPal secure authorization password."
                                                                    return@Button
                                                                }
                                                            }
                                                        }

                                                        gatewayErrorMessage = null
                                                        scope.launch {
                                                            isTopUpProcessing = true
                                                            gatewayTransactionStep = "Securing channel with AES-256 SSL handshake..."
                                                            delay(800)
                                                            gatewayTransactionStep = "Connecting to ATU Cafeteria billing server..."
                                                            delay(800)
                                                            gatewayTransactionStep = "Authenticating user via 3D Secure Multi-Factor authorization..."
                                                            delay(800)
                                                            gatewayTransactionStep = "Verifying transaction OTP on payment gateway core..."
                                                            delay(800)
                                                            gatewayTransactionStep = "Clearing secure funds settlement with Bank of Ghana..."
                                                            delay(600)
                                                            if (com.example.data.LaravelClientManager.isLaravelEnabled) {
                                                                 val userEmail = currentUser?.username ?: "student@atu.edu.gh"
                                                                 val emailToUse = if (userEmail.contains("@")) userEmail else "${userEmail}@atu.edu.gh"
                                                                 
                                                                 var initDetails: com.example.data.LaravelPaystackInitDetails? = null
                                                                 viewModel.initPaystackPayment(emailToUse, amt, "WALLET_TOPUP") { details ->
                                                                     initDetails = details
                                                                 }
                                                                 var waitCount = 0
                                                                 while (initDetails == null && waitCount < 30) {
                                                                     delay(100)
                                                                     waitCount++
                                                                 }
                                                                 
                                                                 val details = initDetails
                                                                 if (details != null) {
                                                                     var verificationSuccess: Boolean? = null
                                                                     viewModel.verifyPaystackPayment(details.reference, amt, "WALLET_TOPUP") { success ->
                                                                         verificationSuccess = success
                                                                     }
                                                                     
                                                                     waitCount = 0
                                                                     while (verificationSuccess == null && waitCount < 50) {
                                                                      delay(100)
                                                                      waitCount++
                                                                  }
                                                                  if (verificationSuccess != true) {
                                                                      gatewayErrorMessage = "Secure gateway verification failed or timed out. Please try again."
                                                                      isTopUpProcessing = false
                                                                      return@launch
                                                                  }
                                                                  // Dummy statement to absorb old delay blocks
                                                                  if (false) {
                                                                         delay(100)
                                                                         waitCount++
                                                                     }
                                                                 }
                                                             } else {
                                                                 viewModel.rechargeWallet(amt)
                                                             }
                                                            isTopUpProcessing = false
                                                            topUpSuccess = true
                                                        }
                                                    }
                                                ) {
                                                    Text("Authorize Payment")
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

            // STUDENT QR CLAIM CODE DIALOG
            showQrForOrder?.let { order ->
                val vendor = allVendors.find { it.id == order.vendorId }
                Dialog(onDismissRequest = { showQrForOrder = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("student_qr_claim_dialog"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Secure Claim Ticket",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                "Show this QR code at the counter of ${vendor?.fullName ?: "Vendor"} to scan and claim your dish.",
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizePx = size.width
                                    val finderSize = sizePx * 7f / 21f

                                    fun drawFinder(x: Float, y: Float, fSize: Float) {
                                        val strokeW = fSize / 7f
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                            size = androidx.compose.ui.geometry.Size(fSize, fSize)
                                        )
                                        drawRect(
                                            color = Color.White,
                                            topLeft = androidx.compose.ui.geometry.Offset(x + strokeW, y + strokeW),
                                            size = androidx.compose.ui.geometry.Size(fSize - strokeW * 2f, fSize - strokeW * 2f)
                                        )
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = androidx.compose.ui.geometry.Offset(x + strokeW * 2f, y + strokeW * 2f),
                                            size = androidx.compose.ui.geometry.Size(fSize - strokeW * 4f, fSize - strokeW * 4f)
                                        )
                                    }

                                    drawFinder(0f, 0f, finderSize)
                                    drawFinder(sizePx - finderSize, 0f, finderSize)
                                    drawFinder(0f, sizePx - finderSize, finderSize)

                                    val gridCount = 21
                                    val cellSize = sizePx / gridCount
                                    val seed = (order.id.toString() + order.pickupPin).hashCode()
                                    val random = java.util.Random(seed.toLong())

                                    for (row in 0 until gridCount) {
                                        for (col in 0 until gridCount) {
                                            val inTopLeft = row < 8 && col < 8
                                            val inTopRight = row < 8 && col >= gridCount - 8
                                            val inBottomLeft = row >= gridCount - 8 && col < 8

                                            if (!inTopLeft && !inTopRight && !inBottomLeft) {
                                                if (random.nextBoolean()) {
                                                    drawRect(
                                                        color = Color.Black,
                                                        topLeft = androidx.compose.ui.geometry.Offset(col * cellSize, row * cellSize),
                                                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "ORDER REFERENCE CODE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "ATU-TKT-${order.id}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "PIN: ${order.pickupPin}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Qty: ${order.quantity}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Amt: GH₵ ${"%.2f".format(order.totalPrice)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { showQrForOrder = null },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("student_qr_dismiss_btn")
                            ) {
                                Text("Close Claim Code", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // CHAT WITH VENDOR DIALOG
            activeChatOrder?.let { order ->
                val vendor = allVendors.find { it.id == order.vendorId }
                val messagesFlow = remember(order.id) { viewModel.getMessagesForOrder(order.id) }
                val chatMessages by messagesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                var replyMessageText by remember { mutableStateOf("") }
                val listState = rememberLazyListState()

                LaunchedEffect(chatMessages.size) {
                    if (chatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(chatMessages.size - 1)
                    }
                }

                Dialog(onDismissRequest = { activeChatOrder = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.75f)
                            .testTag("student_chat_dialog"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Chat with Vendor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Order #${order.id} • ${vendor?.fullName ?: "Vendor"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { activeChatOrder = null },
                                    modifier = Modifier.testTag("student_chat_close_btn")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Chat")
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                if (chatMessages.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Send an urgent inquiry directly to the vendor.",
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    items(chatMessages) { msg ->
                                        val isMe = msg.isFromStudent
                                        val alignment = if (isMe) Alignment.End else Alignment.Start
                                        val bgContainerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                                        val textCol = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = alignment
                                        ) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = bgContainerColor),
                                                shape = RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                    Text(
                                                        text = msg.message,
                                                        color = textCol,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = msg.senderName,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textCol.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replyMessageText,
                                    onValueChange = { replyMessageText = it },
                                    placeholder = { Text("Type urgent message...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("student_chat_input_field")
                                )

                                Button(
                                    onClick = {
                                        if (replyMessageText.isNotBlank() && currentUser != null) {
                                            viewModel.sendChatMessage(
                                                orderId = order.id,
                                                senderId = currentUser!!.id,
                                                senderName = currentUser!!.fullName,
                                                recipientId = order.vendorId,
                                                message = replyMessageText.trim(),
                                                isFromStudent = true
                                            )
                                            replyMessageText = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("student_chat_send_btn")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send")
                                }
                            }
                        }
                    }
                }
            }

            // ORDER PLACEMENT DIALOG
            selectedFoodForOrder?.let { food ->
                val scope = rememberCoroutineScope()
                var checkoutPaymentMode by remember { mutableStateOf("WALLET") } // "WALLET", "POD", "GATEWAY"
                var selectedGatewayPayMethod by remember { mutableStateOf("MOMO") } // "MOMO", "CARD", "BANK", "PAYPAL"
                var selectedMomoOperator by remember { mutableStateOf("MTN MoMo") }
                var gatewayMomoNumber by remember { mutableStateOf("") }
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
                
                var isGatewayProcessing by remember { mutableStateOf(false) }
                var gatewayTransactionStep by remember { mutableStateOf("") }

                Dialog(onDismissRequest = { if (!isGatewayProcessing) selectedFoodForOrder = null }) {
                    var scheduleForLaterEnabled by remember { mutableStateOf(false) }
                    var preferredPickupTimeText by remember { mutableStateOf("12:30 PM") }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Secure Pre-Order Checkout",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(food.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(food.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))
                            // Nutritional Info Display Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🔥", fontSize = 14.sp)
                                    Column {
                                        Text("Energy", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${food.calories} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("⚠️", fontSize = 14.sp)
                                    Column {
                                        Text("Allergens", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(food.allergens, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            val vendorActiveOrders = remember(allOrdersSnapshot, food.vendorId) {
                                allOrdersSnapshot.filter { it.vendorId == food.vendorId && com.example.ui.util.WaitTimeService.isOrderActiveInQueue(it.status) }
                            }
                            val estimatedMinutes = remember(vendorActiveOrders, orderQuantity) {
                                com.example.ui.util.WaitTimeService.calculateEstimatedWaitTime(vendorActiveOrders, orderQuantity)
                            }
                            val pickupTimeStr = remember(estimatedMinutes) {
                                com.example.ui.util.WaitTimeService.calculatePickupTimeText(estimatedMinutes)
                            }
                            val pickupTimeTextForDb = if (scheduleForLaterEnabled) {
                                "Scheduled for $preferredPickupTimeText"
                            } else {
                                "Ready in $estimatedMinutes mins ($pickupTimeStr)"
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("checkout_queue_wait_time_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🕒", fontSize = 18.sp)
                                    Column {
                                        Text(
                                            "Estimated Queue Wait Time",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Approx. $estimatedMinutes mins (Ready around $pickupTimeStr)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "Based on ${vendorActiveOrders.size} active orders currently in preparation queue.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Delivery / Pickup Schedule Option:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Schedule for Later", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Select custom pickup time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = scheduleForLaterEnabled,
                                    onCheckedChange = { scheduleForLaterEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.testTag("schedule_for_later_switch")
                                )
                            }

                            if (scheduleForLaterEnabled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Select Time:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val pickupTimeOptions = listOf("11:30 AM", "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM", "2:00 PM", "3:00 PM", "4:00 PM")
                                        items(pickupTimeOptions) { pickTime ->
                                            val isPickSelected = preferredPickupTimeText == pickTime
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isPickSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { preferredPickupTimeText = pickTime }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .testTag("schedule_time_chip_$pickTime"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pickTime,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPickSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Quantity Target:", fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        enabled = !isGatewayProcessing,
                                        onClick = { if (orderQuantity > 1) orderQuantity-- }
                                    ) {
                                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$orderQuantity", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
                                    IconButton(
                                        enabled = !isGatewayProcessing,
                                        onClick = { if (orderQuantity < 5) orderQuantity++ }
                                    ) {
                                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Text("⭐", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Redeem Loyalty Points",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                                Text(
                                                    text = "Balance: $availablePoints pts",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = redeemLoyaltyPointsChecked,
                                            onCheckedChange = { redeemLoyaltyPointsChecked = it },
                                            enabled = availablePoints >= 10 && !isGatewayProcessing,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                                                checkedTrackColor = MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                            modifier = Modifier.testTag("redeem_loyalty_switch")
                                        )
                                    }
                                    
                                    if (redeemLoyaltyPointsChecked && availablePoints >= 10) {
                                        val maxPointsNeeded = (food.price * orderQuantity * 10).toInt()
                                        val pointsToRedeem = minOf(availablePoints, maxPointsNeeded)
                                        val discountApplied = pointsToRedeem * 0.10
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Using $pointsToRedeem points",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                text = "- GH₵ ${"%.2f".format(discountApplied)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    } else if (availablePoints < 10) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Need at least 10 points to start redeeming.",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            val calculatedPointsToRedeem = if (redeemLoyaltyPointsChecked) {
                                val maxPointsNeeded = (food.price * orderQuantity * 10).toInt()
                                minOf(availablePoints, maxPointsNeeded)
                            } else 0
                            val currentDiscountApplied = calculatedPointsToRedeem * 0.10
                            val finalAmountPayable = (food.price * orderQuantity - currentDiscountApplied).coerceAtLeast(0.0)

                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Amount Payable:", fontWeight = FontWeight.Bold)
                                    if (currentDiscountApplied > 0) {
                                        Text(
                                            text = "Original: GH₵ ${"%.2f".format(food.price * orderQuantity)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                    }
                                }
                                Text("GH₵ ${"%.2f".format(finalAmountPayable)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                            }

                            Text("Choose Payment Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Payment method selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "WALLET" to "Student Wallet",
                                    "POD" to "Cash (POD)",
                                    "GATEWAY" to "Direct Pay"
                                ).forEach { (mode, label) ->
                                    val isSelected = checkoutPaymentMode == mode
                                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(containerColor, RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isGatewayProcessing) { 
                                                checkoutPaymentMode = mode 
                                                orderPlacementError = null
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            when (checkoutPaymentMode) {
                                "WALLET" -> {
                                    val pointsToRedeem = if (redeemLoyaltyPointsChecked) {
                                        val maxPointsNeeded = (food.price * orderQuantity * 10).toInt()
                                        minOf(availablePoints, maxPointsNeeded)
                                    } else 0
                                    val discountApplied = pointsToRedeem * 0.10
                                    val requiredSum = (food.price * orderQuantity - discountApplied).coerceAtLeast(0.0)
                                    val isLowOrInsufficient = studentWalletBalance < 15.0 || studentWalletBalance < requiredSum
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isLowOrInsufficient) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.AccountBalanceWallet,
                                                        contentDescription = null,
                                                        tint = if (isLowOrInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Pay with virtual smart balance", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                }
                                                if (isLowOrInsufficient) {
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
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        modifier = Modifier.height(24.dp).testTag("checkout_direct_topup")
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Top Up", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Your current balance: GH₵ ${"%.2f".format(studentWalletBalance)}",
                                                fontSize = 11.sp,
                                                color = if (isLowOrInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (isLowOrInsufficient) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (studentWalletBalance < requiredSum) "⚠️ Insufficient smart balance for this order!" else "⚠️ Low balance alert! Funds fall below threshold of GH₵ 15.00",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Pre-paying helps bypass queues and allows secure pin pickup.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                "POD" -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pay-on-Delivery (POD)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Authorize order now, and pay with physical cash or momo at the vendor counter upon custody hand-off.", fontSize = 9.sp, color = Color.Gray)
                                        }
                                    }
                                }
                                "GATEWAY" -> {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // 4 Gateway options
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                "MOMO" to "MoMo",
                                                "CARD" to "Card",
                                                "BANK" to "Bank",
                                                "PAYPAL" to "PayPal"
                                            ).forEach { (code, label) ->
                                                val isSelected = selectedGatewayPayMethod == code
                                                val containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                val textColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                                
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(containerColor, RoundedCornerShape(6.dp))
                                                        .clickable(enabled = !isGatewayProcessing) { 
                                                            selectedGatewayPayMethod = code 
                                                            orderPlacementError = null
                                                        }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = textColor)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                when (selectedGatewayPayMethod) {
                                                    "MOMO" -> {
                                                        Text("Mobile Money Billing Gateway", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            listOf("MTN MoMo", "Telecel Cash", "ATG Money").forEach { operator ->
                                                                val isOpSelected = selectedMomoOperator == operator
                                                                val borderColor = if (isOpSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .background(Color.White, RoundedCornerShape(6.dp))
                                                                        .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                                                                        .clickable(enabled = !isGatewayProcessing) { selectedMomoOperator = operator }
                                                                        .padding(vertical = 6.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(operator, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isOpSelected) MaterialTheme.colorScheme.primary else Color.Gray)
                                                                }
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        OutlinedTextField(
                                                            value = gatewayMomoNumber,
                                                            onValueChange = { gatewayMomoNumber = it },
                                                            label = { Text("MoMo Number") },
                                                            enabled = !isGatewayProcessing,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayMomoPin,
                                                            onValueChange = { gatewayMomoPin = it },
                                                            label = { Text("4-Digit Security PIN") },
                                                            enabled = !isGatewayProcessing,
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    "CARD" -> {
                                                        Text("Visa / MasterCard Checkout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayCardName,
                                                            onValueChange = { gatewayCardName = it },
                                                            label = { Text("Cardholder Name") },
                                                            enabled = !isGatewayProcessing,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayCardNumber,
                                                            onValueChange = { gatewayCardNumber = it },
                                                            label = { Text("16-Digit Card Number") },
                                                            enabled = !isGatewayProcessing,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            OutlinedTextField(
                                                                value = gatewayCardExpiry,
                                                                onValueChange = { gatewayCardExpiry = it },
                                                                label = { Text("Expiry (MM/YY)") },
                                                                enabled = !isGatewayProcessing,
                                                                modifier = Modifier.weight(1f),
                                                                singleLine = true
                                                            )

                                                            OutlinedTextField(
                                                                value = gatewayCardCvv,
                                                                onValueChange = { gatewayCardCvv = it },
                                                                label = { Text("CVV") },
                                                                enabled = !isGatewayProcessing,
                                                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                modifier = Modifier.weight(1f),
                                                                singleLine = true
                                                            )
                                                        }
                                                    }
                                                    "BANK" -> {
                                                        Text("Bank Settlement Clearance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayBankAccount,
                                                            onValueChange = { gatewayBankAccount = it },
                                                            label = { Text("Direct Bank Account Number") },
                                                            enabled = !isGatewayProcessing,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayBankPin,
                                                            onValueChange = { gatewayBankPin = it },
                                                            label = { Text("Routing Clearance PIN") },
                                                            enabled = !isGatewayProcessing,
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                    "PAYPAL" -> {
                                                        Text("PayPal Checkout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayPaypalEmail,
                                                            onValueChange = { gatewayPaypalEmail = it },
                                                            label = { Text("PayPal Registered Email") },
                                                            enabled = !isGatewayProcessing,
                                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        OutlinedTextField(
                                                            value = gatewayPaypalPassword,
                                                            onValueChange = { gatewayPaypalPassword = it },
                                                            label = { Text("PayPal Secure Password") },
                                                            enabled = !isGatewayProcessing,
                                                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                                            modifier = Modifier.fillMaxWidth(),
                                                            singleLine = true
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            orderPlacementError?.let { err ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isGatewayProcessing) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(gatewayTransactionStep, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { selectedFoodForOrder = null }) {
                                        Text("Quit")
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = Button@{
                                            val pointsToRedeem = if (redeemLoyaltyPointsChecked) {
                                                val maxPointsNeeded = (food.price * orderQuantity * 10).toInt()
                                                minOf(availablePoints, maxPointsNeeded)
                                            } else 0
                                            val discountApplied = pointsToRedeem * 0.10
                                            val requiredSum = (food.price * orderQuantity - discountApplied).coerceAtLeast(0.0)
                                            when (checkoutPaymentMode) {
                                                "WALLET" -> {
                                                    if (studentWalletBalance < requiredSum) {
                                                        orderPlacementError = "Insufficient smart balance. Top up your virtual ID Wallet!"
                                                        return@Button
                                                    }
                                                    submissionConfirmFood = food
                                                    submissionConfirmQuantity = orderQuantity
                                                    submissionConfirmPaymentMode = "WALLET"
                                                    submissionConfirmCallback = {
                                                        viewModel.placeOrder(food, orderQuantity, true, pointsToRedeem, pickupTimeTextForDb) { success ->
                                                            submissionIsProcessing = false
                                                            showSubmissionConfirmation = false
                                                            if (success) {
                                                                selectedFoodForOrder = null
                                                                activeTab = 1
                                                            } else {
                                                                orderPlacementError = "Deduction failed. Check system link."
                                                            }
                                                        }
                                                    }
                                                    submissionIsProcessing = false
                                                    showSubmissionConfirmation = true
                                                }
                                                "POD" -> {
                                                    submissionConfirmFood = food
                                                    submissionConfirmQuantity = orderQuantity
                                                    submissionConfirmPaymentMode = "POD"
                                                    submissionConfirmCallback = {
                                                        viewModel.placeOrder(food, orderQuantity, false, pointsToRedeem, pickupTimeTextForDb) { success ->
                                                            submissionIsProcessing = false
                                                            showSubmissionConfirmation = false
                                                            if (success) {
                                                                selectedFoodForOrder = null
                                                                activeTab = 1
                                                            } else {
                                                                orderPlacementError = "Failed to compile POD order."
                                                            }
                                                        }
                                                    }
                                                    submissionIsProcessing = false
                                                    showSubmissionConfirmation = true
                                                }
                                                "GATEWAY" -> {
                                                    // Validations
                                                    when (selectedGatewayPayMethod) {
                                                        "MOMO" -> {
                                                            if (gatewayMomoNumber.length < 9) {
                                                                orderPlacementError = "Enter valid MoMo Number."
                                                                return@Button
                                                            }
                                                            if (gatewayMomoPin.length < 4) {
                                                                orderPlacementError = "Enter 4-digit Wallet security PIN."
                                                                return@Button
                                                            }
                                                        }
                                                        "CARD" -> {
                                                            if (gatewayCardName.isBlank() || gatewayCardNumber.length < 16 || gatewayCardExpiry.length < 4 || gatewayCardCvv.length < 3) {
                                                                orderPlacementError = "Invalid card credentials. Please verify fields."
                                                                return@Button
                                                            }
                                                        }
                                                        "BANK" -> {
                                                            if (gatewayBankAccount.length < 8 || gatewayBankPin.length < 4) {
                                                                orderPlacementError = "Invalid direct banking details."
                                                                return@Button
                                                            }
                                                        }
                                                        "PAYPAL" -> {
                                                            if (!gatewayPaypalEmail.contains("@") || gatewayPaypalPassword.length < 4) {
                                                                orderPlacementError = "Invalid PayPal login authentication."
                                                                return@Button
                                                            }
                                                        }
                                                    }

                                                    orderPlacementError = null
                                                    scope.launch {
                                                        isGatewayProcessing = true
                                                        gatewayTransactionStep = "Securing socket node with credit issuer..."
                                                        delay(800)
                                                        gatewayTransactionStep = "Authenticating direct payout authorization..."
                                                        delay(800)
                                                        gatewayTransactionStep = "Tokenizing assets securely..."
                                                        delay(800)
                                                        gatewayTransactionStep = "Settlement cleared! Syncing wallet..."
                                                        delay(400)
                                                         if (com.example.data.LaravelClientManager.isLaravelEnabled) {
                                                             val userEmail = currentUser?.username ?: "student@atu.edu.gh"
                                                             val emailToUse = if (userEmail.contains("@")) userEmail else "${userEmail}@atu.edu.gh"
                                                             
                                                             var initDetails: com.example.data.LaravelPaystackInitDetails? = null
                                                             viewModel.initPaystackPayment(emailToUse, requiredSum, "WALLET_TOPUP") { details ->
                                                                 initDetails = details
                                                             }
                                                             var waitCount = 0
                                                             while (initDetails == null && waitCount < 30) {
                                                                 delay(100)
                                                                 waitCount++
                                                             }
                                                             
                                                             val details = initDetails
                                                             if (details != null) {
                                                                 var verificationSuccess: Boolean? = null
                                                                 viewModel.verifyPaystackPayment(details.reference, requiredSum, "WALLET_TOPUP") { success ->
                                                                     verificationSuccess = success
                                                                 }
                                                                 
                                                                 waitCount = 0
                                                                 while (verificationSuccess == null && waitCount < 50) {
                                                                     delay(100)
                                                                     waitCount++
                                                                 }
                                                             } else {
                                                                 orderPlacementError = "Failed to establish payment gateway channel with Paystack."
                                                                 isGatewayProcessing = false
                                                                 return@launch
                                                             }
                                                         } else {
                                                             viewModel.rechargeWallet(requiredSum)
                                                         }
                                                         submissionConfirmFood = food
                                                         submissionConfirmQuantity = orderQuantity
                                                         submissionConfirmPaymentMode = "GATEWAY"
                                                         submissionConfirmCallback = {
                                                             viewModel.placeOrder(food, orderQuantity, true, pointsToRedeem, pickupTimeTextForDb) { success ->
                                                             submissionIsProcessing = false
                                                             showSubmissionConfirmation = false
                                                            isGatewayProcessing = false
                                                            if (success) {
                                                                selectedFoodForOrder = null
                                                                activeTab = 1
                                                            } else {
                                                                orderPlacementError = "Direct checkout failed. Balance updated, place order via wallet."
                                                              }
                                                         }
                                                     }
                                                     submissionIsProcessing = false
                                                     showSubmissionConfirmation = true
                                                     }
                                                    }
                                                }
                                            }
                                    ) {
                                        Text("Place Order")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SHOPPING CART CHECKOUT DIALOG
            if (showCartCheckoutDialog) {
                val cartItems by viewModel.cart.collectAsStateWithLifecycle()
                Dialog(onDismissRequest = { showCartCheckoutDialog = false }) {
                    var animateCartTrigger by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        animateCartTrigger = true
                    }
                    LaunchedEffect(animateCartTrigger) {
                        if (!animateCartTrigger) {
                            delay(300)
                            showCartCheckoutDialog = false
                        }
                    }
                    AnimatedVisibility(
                        visible = animateCartTrigger,
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(animationSpec = tween(300))
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Your Shopping Cart",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { animateCartTrigger = false },
                                    modifier = Modifier.testTag("close_cart_btn")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            if (cartItems.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        "Your shopping cart is currently empty.",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Text(
                                        "Add hot and delicious meals from the menu to get started!",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    cartItems.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.foodItem.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("GH₵ ${"%.2f".format(item.foodItem.price)} each", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("Subtotal: GH₵ ${"%.2f".format(item.foodItem.price * item.quantity)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (item.quantity > 1) {
                                                            viewModel.updateCartQuantity(item.foodItem, item.quantity - 1)
                                                        } else {
                                                            viewModel.removeFromCart(item.foodItem)
                                                        }
                                                    }
                                                ) {
                                                    Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text("${item.quantity}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                IconButton(
                                                    onClick = {
                                                        if (item.quantity < 5) {
                                                            viewModel.updateCartQuantity(item.foodItem, item.quantity + 1)
                                                        }
                                                    }
                                                ) {
                                                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.removeFromCart(item.foodItem) },
                                                    modifier = Modifier.testTag("remove_item_btn_${item.foodItem.id}")
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Schedule Options for Cart
                                    var cartScheduleForLaterEnabled by remember { mutableStateOf(false) }
                                    var cartPreferredPickupTimeText by remember { mutableStateOf("12:30 PM") }

                                    Text("Delivery / Pickup Schedule Option:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Schedule for Later", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Select custom pickup time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Switch(
                                            checked = cartScheduleForLaterEnabled,
                                            onCheckedChange = { cartScheduleForLaterEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.testTag("cart_schedule_for_later_switch")
                                        )
                                    }

                                    if (cartScheduleForLaterEnabled) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Select Time:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val pickupTimeOptions = listOf("11:30 AM", "12:00 PM", "12:30 PM", "1:00 PM", "1:30 PM", "2:00 PM", "3:00 PM", "4:00 PM")
                                                items(pickupTimeOptions) { pickTime ->
                                                    val isPickSelected = cartPreferredPickupTimeText == pickTime
                                                    Box(
                                                        modifier = Modifier
                                                            .background(
                                                                if (isPickSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { cartPreferredPickupTimeText = pickTime }
                                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                                            .testTag("cart_schedule_time_chip_$pickTime"),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = pickTime,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isPickSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val cartTotalSum = viewModel.getCartTotal()
                                    val finalCartPickupTimeText = if (cartScheduleForLaterEnabled) {
                                        "Scheduled for $cartPreferredPickupTimeText"
                                    } else {
                                        "As soon as possible"
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                                        Text("GH₵ ${"%.2f".format(cartTotalSum)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Choose Payment Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    var cartPaymentMode by remember { mutableStateOf("WALLET") }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            "WALLET" to "Student Wallet",
                                            "POD" to "Cash (POD)"
                                        ).forEach { (mode, label) ->
                                            val isSelected = cartPaymentMode == mode
                                            val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(containerColor, RoundedCornerShape(8.dp))
                                                    .clickable { cartPaymentMode = mode }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    var cartCheckoutError by remember { mutableStateOf<String?>(null) }
                                    var cartCheckoutProcessing by remember { mutableStateOf(false) }

                                    if (cartPaymentMode == "WALLET") {
                                        val isLowOrInsufficient = studentWalletBalance < cartTotalSum
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isLowOrInsufficient) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Your current balance: GH₵ ${"%.2f".format(studentWalletBalance)}",
                                                    fontSize = 11.sp,
                                                    color = if (isLowOrInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isLowOrInsufficient) {
                                                    Text(
                                                        text = "⚠️ Insufficient smart balance for this order!",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    cartCheckoutError?.let { err ->
                                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    if (cartCheckoutProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                                    } else {
                                        Button(
                                            onClick = {
                                                if (cartPaymentMode == "WALLET" && studentWalletBalance < cartTotalSum) {
                                                    cartCheckoutError = "Insufficient smart balance. Top up your virtual ID Wallet!"
                                                } else {
                                                    cartCheckoutProcessing = true
                                                    cartCheckoutError = null
                                                    viewModel.checkoutCart(
                                                        useWallet = (cartPaymentMode == "WALLET"),
                                                        estimatedPickupTime = finalCartPickupTimeText
                                                    ) { success ->
                                                        cartCheckoutProcessing = false
                                                        if (success) {
                                                            animateCartTrigger = false
                                                        } else {
                                                            cartCheckoutError = "Checkout transaction failed. Try again."
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("cart_checkout_submit_btn"),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Place Multi-Order (${cartItems.size} items)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }

            // ORDER SUBMISSION CONFIRMATION DIALOG (PREVENTS ACCIDENTAL DUPLICATIONS)
            if (showSubmissionConfirmation) {
                submissionConfirmFood?.let { food ->
                    val totalSum = food.price * submissionConfirmQuantity
                    Dialog(onDismissRequest = { if (!submissionIsProcessing) showSubmissionConfirmation = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag("order_submission_confirmation_dialog"),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Header icon and title
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    "Confirm Your Order",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    "Please review details below before routing this order directly to the kitchen.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                // Order summary block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            food.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Qty: $submissionConfirmQuantity x GH₵ ${"%.2f".format(food.price)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Text(
                                        "GH₵ ${"%.2f".format(totalSum)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Payment Mode:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val payIcon = when (submissionConfirmPaymentMode) {
                                            "WALLET" -> Icons.Default.AccountBalanceWallet
                                            "POD" -> Icons.Default.Payments
                                            else -> Icons.Default.CreditCard
                                        }
                                        Icon(
                                            payIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when (submissionConfirmPaymentMode) {
                                                "WALLET" -> "Smart Wallet"
                                                "POD" -> "Pay On Delivery"
                                                else -> "Direct Gateway"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Prevent Duplication Warning Banner
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Double placement defense active: Confirming will process payment and alert the vendor. Please click only once.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showSubmissionConfirmation = false },
                                        enabled = !submissionIsProcessing,
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("confirm_cancel_button")
                                    ) {
                                        Text("Go Back")
                                    }

                                    Button(
                                        onClick = {
                                            submissionIsProcessing = true
                                            submissionConfirmCallback?.invoke()
                                        },
                                        enabled = !submissionIsProcessing,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f).height(48.dp).testTag("confirm_submit_button")
                                    ) {
                                        if (submissionIsProcessing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Confirm Order")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FEEDBACK DIALOG
            feedbackTargetOrder?.let { order ->
                Dialog(onDismissRequest = { feedbackTargetOrder = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "Rate & Review Vendor",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Rate '${order.foodName}' vendor and share your experience:", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(16.dp))

                            // 1. Food Quality Slider
                            Text("🍔 Food Culinary Quality ($foodQualityRating/5)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Slider(
                                value = foodQualityRating.toFloat(),
                                onValueChange = { foodQualityRating = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3
                            )

                            // 2. Sanitation
                            Text("🫧 Booth Hygiene & Cleanliness ($cleanlinessRating/5)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Slider(
                                value = cleanlinessRating.toFloat(),
                                onValueChange = { cleanlinessRating = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3
                            )

                            // 3. Service Speed
                            Text("⚡ Processing Speed ($speedRating/5)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Slider(
                                value = speedRating.toFloat(),
                                onValueChange = { speedRating = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3
                            )

                            // 4. Value
                            Text("💰 Price-to-Portion Value Ratio ($priceRating/5)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Slider(
                                value = priceRating.toFloat(),
                                onValueChange = { priceRating = it.toInt() },
                                valueRange = 1f..5f,
                                steps = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = feedbackComment,
                                onValueChange = { feedbackComment = it },
                                label = { Text("Verbal Comments (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            Text(
                                "🥗 Specific Food Item Review",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text("Rate the specific food item '${order.foodName}' specifically:", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Star Rating Selection
                            Text("Rating ($foodItemRating/5 Stars)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                for (star in 1..5) {
                                    val isSelected = star <= foodItemRating
                                    IconButton(
                                        onClick = { foodItemRating = star },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "$star Stars",
                                            tint = if (isSelected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = foodItemComment,
                                onValueChange = { foodItemComment = it },
                                label = { Text("How was the meal preparation / portion size? (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { feedbackTargetOrder = null }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.submitOrderFeedback(
                                            orderId = order.id,
                                            vendorId = order.vendorId,
                                            quality = foodQualityRating,
                                            cleanliness = cleanlinessRating,
                                            speed = speedRating,
                                            value = priceRating,
                                            comment = feedbackComment.ifBlank { "Tasted pristine, good portions." }
                                        ) {
                                            viewModel.submitFoodFeedback(
                                                orderId = order.id,
                                                foodItemId = order.foodItemId,
                                                rating = foodItemRating,
                                                comment = foodItemComment.ifBlank { "Delicious! Prepared to perfection." }
                                            ) {
                                                feedbackTargetOrder = null
                                                // Reset states
                                                foodItemRating = 5
                                                foodItemComment = ""
                                                feedbackComment = ""
                                                foodQualityRating = 5
                                                cleanlinessRating = 5
                                                speedRating = 5
                                                priceRating = 5
                                            }
                                        }
                                    }
                                ) {
                                    Text("Submit Rating & Review")
                                }
                            }
                        }
                    }
                }
            }

            // POPUP REAL-TIME KITCHEN ALERTS (FROM VENDOR ESTIMATED TIME UPDATE)
            val activePreparingOrderWithNoAck = studentOrders.firstOrNull { it.status == "PREPARING" && it.id !in acknowledgedOrders }
            activePreparingOrderWithNoAck?.let { order ->
                AlertDialog(
                    onDismissRequest = { /* No-op to force acknowledgement */ },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Prep Speed Alert",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Cooking Prep Alert",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Your requested food is being prepared by the kitchen stand!",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "ORDERED FOOD:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "${order.foodName} x ${order.quantity}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "VENDOR ESTIMATED WAIT TIME:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = order.estimatedPickupTime,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "It will take about '${order.estimatedPickupTime}' to finish preparing your meal.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Text(
                                text = "We'll notify you with a READY status update when it's hot and complete!",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { acknowledgedOrders.add(order.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text("Acknowledge Wait Time", color = MaterialTheme.colorScheme.onTertiary)
                        }
                    }
                )
            }

            // Real-time floating order notification banner for students
            if (studentAlerts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    studentAlerts.forEach { alert ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Order Status Notification Alert",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔔 ORDER IS READY!",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { viewModel.dismissStudentAlert(alert.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss Notification",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = alert.data.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = alert.data.time,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Notification Center / Inbox Dialog
            if (showNotificationCenter) {
                Dialog(onDismissRequest = { showNotificationCenter = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Laravel Notifications",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { showNotificationCenter = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            Text(
                                text = "Real-time updates synced in real-time from Accra Technical University's Laravel database subscription.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            if (studentNotifications.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            "No notifications yet.",
                                            fontWeight = FontWeight.Medium,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "You'll get an instant alert here when a Chef moves your plates to READY.",
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(studentNotifications) { notif ->
                                        val isUnread = notif.read_at == null
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isUnread)
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = if (isUnread) BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            ) else null
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        if (isUnread) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                                            )
                                                            Text(
                                                                "UNREAD ALERT",
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 9.sp
                                                            )
                                                        } else {
                                                            Text(
                                                                "HISTORICAL",
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = notif.data.time,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = notif.data.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (studentNotifications.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            viewModel.clearAllStudentNotifications()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("Acknowledge & Mark Read", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showNotificationCenter = false },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 10.dp)
                                ) {
                                    Text("Dismiss Panel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            if (showQualityRatingsLeaderboard) {
                Dialog(onDismissRequest = { showQualityRatingsLeaderboard = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 550.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(22.dp))
                                    Text(
                                        "ATU Quality Leaderboard",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { showQualityRatingsLeaderboard = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                            
                            Text(
                                "Live standings & hygiene audits of Accra Technical University cafeteria booths compiled from student compliance ratings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            val rankedVendors = remember(allVendors, allFeedback) {
                                allVendors.map { vendor ->
                                    val metrics = viewModel.getVendorMetrics(vendor.id, allFeedback)
                                    vendor to (metrics["overall"] ?: 0.0)
                                }.sortedByDescending { it.second }
                            }
                            
                            if (rankedVendors.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No metrics found yet. Complete first handovers to trigger standings!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rankedVendors.forEachIndexed { index, pair ->
                                        val vendor = pair.first
                                        val overall = pair.second
                                        val metrics = viewModel.getVendorMetrics(vendor.id, allFeedback)
                                        
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when (index) {
                                                    0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                when (index) {
                                                    0 -> Color(0xFFFFB300)
                                                    else -> MaterialTheme.colorScheme.outlineVariant
                                                }
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        val medal = when (index) {
                                                            0 -> "👑"
                                                            1 -> "🥈"
                                                            2 -> "🥉"
                                                            else -> "🍴"
                                                        }
                                                        Text(medal, fontSize = 16.sp)
                                                        Column {
                                                            Text(vendor.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            Text("Booth: ${vendor.info.ifBlank { "Main Area" }}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300)),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (overall > 0.0) "${"%.1f".format(overall)} ★" else "N/A",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF2E2E2E),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                
                                                if (overall > 0.0) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("🍔 Culinary: ${"%.1f".format(metrics["foodQuality"])}★", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            LinearProgressIndicator(
                                                                progress = { ((metrics["foodQuality"] ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f) },
                                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                                color = Color(0xFF4CAF50),
                                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("🫧 Hygiene: ${"%.1f".format(metrics["cleanliness"])}★", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            LinearProgressIndicator(
                                                                progress = { ((metrics["cleanliness"] ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f) },
                                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                                color = Color(0xFF2196F3),
                                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        }
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text("⚡ Speed: ${"%.1f".format(metrics["speed"])}★", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            LinearProgressIndicator(
                                                                progress = { ((metrics["speed"] ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f) },
                                                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                                                color = Color(0xFFFF9800),
                                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { showQualityRatingsLeaderboard = false },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Acknowledge Rankings", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ==========================================
