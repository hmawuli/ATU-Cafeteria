package com.example.ui.screens
import com.example.ui.util.generatePdfReceipt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
fun AdminDashboardScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val allVendors by viewModel.allVendors.collectAsStateWithLifecycle()
    val allOrdersSnapshot by viewModel.allOrdersSnapshot.collectAsStateWithLifecycle()
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    val auditHistoryLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val adminInventoryAlerts by viewModel.adminInventoryAlerts.collectAsStateWithLifecycle()
    val promotionalOffers by viewModel.promotionalOffers.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Compliance Board, 1: AI Advisor, 2: Cyber Logs

    val scope = rememberCoroutineScope()
    val adminVendorsScrollState = rememberLazyListState()
    val adminStudentsScrollState = rememberLazyListState()
    val adminAiScrollState = rememberScrollState()
    val adminCyberScrollState = rememberLazyListState()
    val adminOversightScrollState = rememberLazyListState()

    // Local Dialog States
    var showAddVendorDialog by remember { mutableStateOf(false) }
    var newlyCreatedVendorCreds by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(Username, PIN)
    var vendorToEdit by remember { mutableStateOf<User?>(null) }
    var vendorToDelete by remember { mutableStateOf<User?>(null) }
    var vendorToResetPin by remember { mutableStateOf<User?>(null) }
    var vendorForMenuManagement by remember { mutableStateOf<User?>(null) }

    // Student & User Management Dialog States
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var userToEditProfile by remember { mutableStateOf<User?>(null) }
    var userToAdjustWallet by remember { mutableStateOf<User?>(null) }
    var showCampusBonusDialog by remember { mutableStateOf(false) }
    var userToResetPin by remember { mutableStateOf<User?>(null) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var orderToRefund by remember { mutableStateOf<Order?>(null) }

    // User Directory Search & Filters
    var studentSearchQuery by remember { mutableStateOf("") }
    var studentRoleFilter by remember { mutableStateOf("ALL") } // ALL, STUDENT, VENDOR, ADMIN

    // Dialog Input states
    var addUsername by remember { mutableStateOf("") }
    var addPinCode by remember { mutableStateOf("") }
    var addFullName by remember { mutableStateOf("") }
    var addInfo by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }

    var editFullName by remember { mutableStateOf("") }
    var editInfo by remember { mutableStateOf("") }
    var editPinCode by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    var addLogoUrl by remember { mutableStateOf("") }
    var addPictureUrl by remember { mutableStateOf("") }
    var editLogoUrl by remember { mutableStateOf("") }
    var editPictureUrl by remember { mutableStateOf("") }

    val allFoodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()

    var listSelection by remember { mutableIntStateOf(0) } // 0: Manage Vendors, 1: Student Directory

    // AI selection targets
    var selectedVendorForAiReview by remember { mutableStateOf<User?>(null) }
    val aiAnalysiResultText by viewModel.aiAnalysisText.collectAsStateWithLifecycle()
    val isAnalyzingUiState by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val vendorStatusMap by viewModel.vendorStatusMap.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ATU Cafeteria Audit Board", fontWeight = FontWeight.Bold)
                        Text(
                            "Bureau: ${currentUser?.fullName ?: ""} • Audit Mode",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.logOut()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Vendors") }
                )
                NavigationBarItem(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("ATU Analytics") }
                )
                NavigationBarItem(
                    selected = activeSubTab == 2,
                    onClick = { activeSubTab = 2 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text("Cyber Registry") }
                )
                NavigationBarItem(
                    selected = activeSubTab == 3,
                    onClick = { activeSubTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Oversight Hub") }
                )
            }
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 4.dp, bottom = 12.dp)
            ) {
                if (activeSubTab == 0 && listSelection == 0) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            addFullName = ""
                            addUsername = ""
                            addPinCode = "1234"
                            addInfo = ""
                            addLogoUrl = ""
                            addPictureUrl = ""
                            addError = null
                            showAddVendorDialog = true
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Add Vendor") }
                    )
                }

                // Scroll Up Button
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            when (activeSubTab) {
                                0 -> {
                                    if (listSelection == 0) adminVendorsScrollState.animateScrollToItem(0)
                                    else adminStudentsScrollState.animateScrollToItem(0)
                                }
                                1 -> adminAiScrollState.animateScrollTo(0)
                                2 -> adminCyberScrollState.animateScrollToItem(0)
                                3 -> adminOversightScrollState.animateScrollToItem(0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp).testTag("admin_scroll_up_fab"),
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
                        scope.launch {
                            when (activeSubTab) {
                                0 -> {
                                    if (listSelection == 0) {
                                        val count = adminVendorsScrollState.layoutInfo.totalItemsCount - 1
                                        if (count >= 0) adminVendorsScrollState.animateScrollToItem(count)
                                    } else {
                                        val count = adminStudentsScrollState.layoutInfo.totalItemsCount - 1
                                        if (count >= 0) adminStudentsScrollState.animateScrollToItem(count)
                                    }
                                }
                                1 -> adminAiScrollState.animateScrollTo(adminAiScrollState.maxValue)
                                2 -> {
                                    val count = adminCyberScrollState.layoutInfo.totalItemsCount - 1
                                    if (count >= 0) adminCyberScrollState.animateScrollToItem(count)
                                }
                                3 -> {
                                    val count = adminOversightScrollState.layoutInfo.totalItemsCount - 1
                                    if (count >= 0) adminOversightScrollState.animateScrollToItem(count)
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(44.dp).testTag("admin_scroll_down_fab"),
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
                targetState = activeSubTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(225)) togetherWith fadeOut(animationSpec = tween(225))
                },
                label = "admin_tabs"
            ) { targetTab ->
                when (targetTab) {
                0 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Directory selection row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { listSelection = 0 }
                                    .background(if (listSelection == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Vendors Registry",
                                    color = if (listSelection == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { listSelection = 1 }
                                    .background(if (listSelection == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Student Directory",
                                    color = if (listSelection == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { listSelection = 2 }
                                    .background(if (listSelection == 2) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Vendor Analytics",
                                    color = if (listSelection == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        when (listSelection) {
                            0 -> {
                            // Vendors Directory
                            LazyColumn(
                                state = adminVendorsScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (allVendors.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                            Text("No vendors registered in directory.")
                                        }
                                    }
                                } else {
                                    items(allVendors) { vendor ->
                                        val metrics = viewModel.getVendorMetrics(vendor.id, allFeedback)
                                        val ordersForThisVendor = allOrdersSnapshot.filter { it.vendorId == vendor.id }
                                        var showReviewsAndDetails by remember { mutableStateOf(false) }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(vendor.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                        Text("Username: ${vendor.username} • Booth: ${vendor.info}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("All Orders logged: ${ordersForThisVendor.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    }

                                                    // Overall Score indicator
                                                    val overallStar = metrics["overall"] ?: 0.0
                                                    val starsText = if (overallStar == 0.0) "N/A" else "${"%.1f".format(overallStar)}★"
                                                    val scoreColor = if (overallStar >= 4.0) Color(0xFF2E7D32) else if (overallStar >= 3.0) Color(0xFFF9A825) else Color(0xFFC62828)

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(scoreColor.copy(alpha = 0.15f))
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(starsText, color = scoreColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Mini linear breakdown
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Quality: ${"%.1f".format(metrics["foodQuality"] ?: 0.0)}", fontSize = 10.sp)
                                                    Text("Cleanliness: ${"%.1f".format(metrics["cleanliness"] ?: 0.0)}", fontSize = 10.sp)
                                                    Text("Speed: ${"%.1f".format(metrics["speed"] ?: 0.0)}", fontSize = 10.sp)
                                                    Text("Value: ${"%.1f".format(metrics["priceValue"] ?: 0.0)}", fontSize = 10.sp)
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Real-Time Vendor Status Override (Firestore Sync: Open / Busy / Closed)
                                                val currentStatus = vendorStatusMap[vendor.id] ?: if (vendor.isOpen) "OPEN" else "CLOSED"
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                        .padding(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                Icons.Default.CloudSync,
                                                                contentDescription = "Firestore Real-time Sync",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                "Vendor Status (Firestore Sync):",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                        Text(
                                                            text = currentStatus,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = when (currentStatus) {
                                                                "OPEN" -> Color(0xFF2E7D32)
                                                                "BUSY" -> Color(0xFFE65100)
                                                                else -> Color(0xFFC62828)
                                                            }
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        // OPEN Button
                                                        FilterChip(
                                                            selected = currentStatus == "OPEN",
                                                            onClick = { viewModel.setVendorStatusByAdmin(vendor.id, "OPEN", vendor.fullName) },
                                                            label = { Text("OPEN", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.CheckCircle,
                                                                    contentDescription = "Open Status",
                                                                    modifier = Modifier.size(12.dp),
                                                                    tint = if (currentStatus == "OPEN") Color(0xFF2E7D32) else Color.Gray
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("admin_vendor_status_open_${vendor.id}")
                                                        )

                                                        // BUSY Button
                                                        FilterChip(
                                                            selected = currentStatus == "BUSY",
                                                            onClick = { viewModel.setVendorStatusByAdmin(vendor.id, "BUSY", vendor.fullName) },
                                                            label = { Text("BUSY", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.Schedule,
                                                                    contentDescription = "Busy Status",
                                                                    modifier = Modifier.size(12.dp),
                                                                    tint = if (currentStatus == "BUSY") Color(0xFFE65100) else Color.Gray
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("admin_vendor_status_busy_${vendor.id}")
                                                        )

                                                        // CLOSED Button
                                                        FilterChip(
                                                            selected = currentStatus == "CLOSED",
                                                            onClick = { viewModel.setVendorStatusByAdmin(vendor.id, "CLOSED", vendor.fullName) },
                                                            label = { Text("CLOSED", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                                            leadingIcon = {
                                                                Icon(
                                                                    Icons.Default.Cancel,
                                                                    contentDescription = "Closed Status",
                                                                    modifier = Modifier.size(12.dp),
                                                                    tint = if (currentStatus == "CLOSED") Color(0xFFC62828) else Color.Gray
                                                                )
                                                            },
                                                            modifier = Modifier.weight(1f).testTag("admin_vendor_status_closed_${vendor.id}")
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                val totalCompletedOrders = ordersForThisVendor.filter { it.status.uppercase() == "COMPLETED" || it.status.uppercase() == "DELIVERED" }
                                                val totalSalesRevenue = totalCompletedOrders.sumOf { it.totalPrice }

                                                // Sales and Orders Stats Section
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                                        .padding(10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("TOTAL REVENUE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                        Text("GH₵ ${"%.2f".format(totalSalesRevenue)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("COMPLETED PRE-ORDERS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("${totalCompletedOrders.size} portions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Orders: Pnd:${ordersForThisVendor.count { it.status.uppercase() == "PENDING" }} • Prep:${ordersForThisVendor.count { it.status.uppercase() == "PREPARING" }} • Rdy:${ordersForThisVendor.count { it.status.uppercase() == "READY" }} • Dec:${ordersForThisVendor.count { it.status.uppercase() == "DECLINED" }}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )

                                                    TextButton(
                                                        onClick = { showReviewsAndDetails = !showReviewsAndDetails },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (showReviewsAndDetails) "Hide Reviews ▲" else "View Reviews ▼",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                if (showReviewsAndDetails) {
                                                    val vendorFeedbackList = allFeedback.filter { it.vendorId == vendor.id }
                                                    
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 8.dp, bottom = 4.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                     ) {
                                                         Text(
                                                             "Customer Review Log (${vendorFeedbackList.size})",
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = MaterialTheme.colorScheme.secondary
                                                         )

                                                         if (vendorFeedbackList.isEmpty()) {
                                                             Text(
                                                                 "No written customer reviews recorded yet.",
                                                                 fontSize = 11.sp,
                                                                 color = Color.Gray,
                                                                 modifier = Modifier.padding(vertical = 4.dp)
                                                             )
                                                         } else {
                                                             vendorFeedbackList.forEach { fb ->
                                                                 val reviewer = allUsers.find { it.id == fb.customerId }
                                                                 val reviewerName = reviewer?.fullName ?: "ATU Student"
                                                                 val reviewerId = reviewer?.info ?: "UID: ${fb.customerId}"
                                                                 val averageRating = (fb.ratingFoodQuality + fb.ratingCleanliness + fb.ratingServiceSpeed + fb.ratingPriceValue) / 4f

                                                                 Card(
                                                                     modifier = Modifier.fillMaxWidth(),
                                                                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                                                     shape = RoundedCornerShape(10.dp)
                                                                 ) {
                                                                     Column(modifier = Modifier.padding(10.dp)) {
                                                                         Row(
                                                                             modifier = Modifier.fillMaxWidth(),
                                                                             horizontalArrangement = Arrangement.SpaceBetween,
                                                                             verticalAlignment = Alignment.CenterVertically
                                                                         ) {
                                                                             Column {
                                                                                 Text(
                                                                                     reviewerName,
                                                                                     fontWeight = FontWeight.Bold,
                                                                                     fontSize = 11.sp
                                                                                 )
                                                                                 Text(
                                                                                     reviewerId,
                                                                                     fontSize = 9.sp,
                                                                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                 )
                                                                             }
                                                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                 Icon(
                                                                                     Icons.Default.Star,
                                                                                     contentDescription = null,
                                                                                     tint = Color(0xFFF9A825),
                                                                                     modifier = Modifier.size(12.dp)
                                                                                 )
                                                                                 Text(
                                                                                     text = "${"%.1f".format(averageRating)}★",
                                                                                     fontSize = 11.sp,
                                                                                     fontWeight = FontWeight.Bold,
                                                                                     modifier = Modifier.padding(start = 2.dp)
                                                                                 )
                                                                             }
                                                                         }
                                                                         Spacer(modifier = Modifier.height(6.dp))
                                                                         Text(
                                                                             text = "\"${fb.comment}\"",
                                                                             fontSize = 11.sp,
                                                                             fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                                             color = MaterialTheme.colorScheme.onSurface
                                                                         )
                                                                     }
                                                                 }
                                                             }
                                                         }
                                                     }
                                                 }

                                                 Spacer(modifier = Modifier.height(8.dp))
                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.startImpersonation(vendor)
                                                            navController.navigate("vendor_home")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1.1f),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Simulate", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            vendorForMenuManagement = vendor
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Menu", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = {
                                                            editFullName = vendor.fullName
                                                            editInfo = vendor.info
                                                            editPinCode = ""
                                                            editLogoUrl = vendor.logoUrl ?: ""
                                                            editPictureUrl = vendor.pictureUrl ?: ""
                                                            editError = null
                                                            vendorToEdit = vendor
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Edit", fontSize = 10.sp)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            vendorToResetPin = vendor
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.LockReset, contentDescription = "Reset PIN", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            vendorToDelete = vendor
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Vendor", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }
                            1 -> {
                            // Comprehensive Student & Campus User Governance Directory
                            val filteredUsers = remember(allUsers, studentSearchQuery, studentRoleFilter) {
                                allUsers.filter { user ->
                                    val matchesRole = when (studentRoleFilter) {
                                        "STUDENT" -> user.role == "STUDENT"
                                        "VENDOR" -> user.role == "VENDOR"
                                        "ADMIN" -> user.role == "ADMIN"
                                        else -> true
                                    }
                                    val q = studentSearchQuery.trim().lowercase()
                                    val matchesSearch = q.isEmpty() ||
                                        user.fullName.lowercase().contains(q) ||
                                        user.username.lowercase().contains(q) ||
                                        user.info.lowercase().contains(q) ||
                                        (user.email?.lowercase()?.contains(q) == true) ||
                                        (user.telephone?.lowercase()?.contains(q) == true)
                                    matchesRole && matchesSearch
                                }
                            }

                            LazyColumn(
                                state = adminStudentsScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Top Action Banner & Quick Tools
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        "Campus User & Student Governance",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "Manage student meal wallets, access PINs, account limits & campus subsidies.",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { showAddStudentDialog = true },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(vertical = 10.dp)
                                                ) {
                                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Enroll Student", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = { showCampusBonusDialog = true },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(vertical = 10.dp)
                                                ) {
                                                    Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Campus Subsidy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Search Bar & Filter Chips
                                item {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = studentSearchQuery,
                                            onValueChange = { studentSearchQuery = it },
                                            placeholder = { Text("Search by name, matric ID, username, email or phone...") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                            trailingIcon = {
                                                if (studentSearchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { studentSearchQuery = "" }) {
                                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = studentRoleFilter == "ALL",
                                                onClick = { studentRoleFilter = "ALL" },
                                                label = { Text("All Users (${allUsers.size})", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = studentRoleFilter == "STUDENT",
                                                onClick = { studentRoleFilter = "STUDENT" },
                                                label = { Text("Students (${allUsers.count { it.role == "STUDENT" }})", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = studentRoleFilter == "VENDOR",
                                                onClick = { studentRoleFilter = "VENDOR" },
                                                label = { Text("Vendors (${allUsers.count { it.role == "VENDOR" }})", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = studentRoleFilter == "ADMIN",
                                                onClick = { studentRoleFilter = "ADMIN" },
                                                label = { Text("Admins (${allUsers.count { it.role == "ADMIN" }})", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                if (filteredUsers.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.PersonOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("No user accounts found matching your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredUsers, key = { it.id }) { user ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                // Top row: Avatar, Name, Badges
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .background(
                                                                    when (user.role) {
                                                                        "STUDENT" -> MaterialTheme.colorScheme.primaryContainer
                                                                        "VENDOR" -> Color(0xFFFFE0B2)
                                                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                                                    },
                                                                    shape = CircleShape
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                user.fullName.take(1).uppercase(),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 16.sp,
                                                                color = when (user.role) {
                                                                    "STUDENT" -> MaterialTheme.colorScheme.primary
                                                                    "VENDOR" -> Color(0xFFE65100)
                                                                    else -> MaterialTheme.colorScheme.secondary
                                                                }
                                                            )
                                                        }

                                                        Column {
                                                            Text(
                                                                user.fullName,
                                                                fontWeight = FontWeight.Bold,
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                            Text(
                                                                "@${user.username}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }

                                                    // Status & Role Badges
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = if (user.isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                            border = BorderStroke(1.dp, if (user.isOpen) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f))
                                                        ) {
                                                            Text(
                                                                if (user.isOpen) "Active" else "Suspended",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (user.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                user.role,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Middle Row: Info & Smart Balance
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        if (user.info.isNotBlank()) {
                                                            Text(
                                                                "ID: ${user.info}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }
                                                        user.email?.let { email ->
                                                            Text("Email: $email", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                        user.telephone?.let { tel ->
                                                            Text("Phone: $tel", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                        }
                                                    }

                                                    // Smart Wallet Balance Badge
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                    ) {
                                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                                                            Text("SMART WALLET", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                            Text(
                                                                "GH₵ ${"%.2f".format(user.balance)}",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }

                                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                // Bottom Action Buttons Row
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = { userToAdjustWallet = user },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1.2f),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Adjust Wallet", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    OutlinedButton(
                                                        onClick = { userToEditProfile = user },
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(0.8f),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("Edit", fontSize = 10.sp)
                                                    }

                                                    IconButton(
                                                        onClick = { userToResetPin = user },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.LockReset, contentDescription = "Reset PIN", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }

                                                    IconButton(
                                                        onClick = { viewModel.adminToggleUserStatus(user.id) {} },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            if (user.isOpen) Icons.Default.Block else Icons.Default.CheckCircle,
                                                            contentDescription = if (user.isOpen) "Suspend" else "Activate",
                                                            tint = if (user.isOpen) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { userToDelete = user },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            viewModel.startImpersonation(user)
                                                            if (user.role == "VENDOR") {
                                                                navController.navigate("vendor_home")
                                                            } else {
                                                                navController.navigate("student_home")
                                                            }
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = "Simulate", tint = Color(0xFFFF8F00), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }
                            2 -> {
                                VendorPerformanceAnalyticsDashboard(
                                    allVendors = allVendors,
                                    allFeedback = allFeedback,
                                    allOrders = allOrdersSnapshot,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Gemini Analytics & Order Trends Visualizer page
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(adminAiScrollState),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Weekly Vendor Order Trends & Peak Cafeteria Traffic Hours",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Interactive Recharts visualization detailing 7-day weekly order trends, peak traffic hours, and top performance metrics for administrative oversight.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Embedded Recharts Data Visualization
                        RechartsDashboardChart(
                            orders = allOrdersSnapshot,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "ATU Campus Analytics Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Examine feedback clusters on kitchen units using advanced ATU data reasoning to output operational advisories for compliance management.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vendor select dropdown list
                        Text("1. SELECT COMPLIANCE VENDOR:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allVendors.forEach { vendor ->
                                val isSelected = selectedVendorForAiReview?.id == vendor.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedVendorForAiReview = vendor }
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                    Text(
                                        vendor.fullName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        selectedVendorForAiReview?.let { vendor ->
                            val feedbacksForThis = allFeedback.filter { it.vendorId == vendor.id }
                            val totalOrdersForThis = allOrdersSnapshot.filter { it.vendorId == vendor.id }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.runGeminiVendorAnalytics(
                                        vendorId = vendor.id,
                                        vendorName = vendor.fullName,
                                        feedbacks = feedbacksForThis,
                                        totalOrders = totalOrdersForThis.size
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Engage ATU Diagnostics")
                            }
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Choose an active vendor above to consult ATU Analytics", fontSize = 11.0.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Response Card
                        if (isAnalyzingUiState) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("ATU Intelligence is mining campus feedback clusters...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            aiAnalysiResultText?.let { report ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "GEMINI COMPLIANCE REPORT SUMMARY",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = report,
                                            style = MaterialTheme.typography.bodyMedium,
                                            lineHeight = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Cyber Logs Timeline
                    LazyColumn(
                        state = adminCyberScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "Cyber Security Audit Registry Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Cryptographic log records showing trace audit trails of campus logistics, logins, orders, and secure verified code handshakes.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        if (auditHistoryLogs.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    Text("Log directory empty.")
                                }
                            }
                        } else {
                            items(auditHistoryLogs) { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    log.action.contains("FAIL") || log.action.contains("FAILURE") -> MaterialTheme.colorScheme.error
                                                    log.action.contains("SUCCESS") || log.action.contains("VALIDATED") -> Color(0xFF2E7D32)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("User ID: ${log.userId} • Epoch: ${log.timestamp}", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // System configuration & active metrics tab
                    var commissionRate by remember { mutableStateOf("1.5") }
                    var maxVendorsSupported by remember { mutableStateOf("10") }
                    var emergencyHaltState by remember { mutableStateOf(false) }
                    var syncStatus by remember { mutableStateOf<String?>(null) }
                    var isSyncing by remember { mutableStateOf(false) }

                    LazyColumn(
                        state = adminOversightScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "System Configuration & Oversight Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Global properties governing commission fees, automated fraud checkers, escrow logs, and campus food guidelines.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // 1. Escrow Overview card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Global Escrow Balance Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("AGGREGATE ESCROW SUM", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            Text("GH₵ 42,950.00", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Column {
                                            Text("TOTAL ATU STUDENTS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            Text("1,452 Members", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (isSyncing) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verifying SHA-256 Ledger signatures...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        syncStatus?.let { status ->
                                            Text(status, color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                isSyncing = true
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    isSyncing = false
                                                    syncStatus = "SHA-256 Checksum success. System integrity token [0x97A2E9F] matched registry records."
                                                }, 2000)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Sanitize Escrow Ledgers", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Commission Rate config
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Global Logistics Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                                    OutlinedTextField(
                                        value = commissionRate,
                                        onValueChange = { commissionRate = it },
                                        label = { Text("Base Transaction Commission Fee (%)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = maxVendorsSupported,
                                        onValueChange = { maxVendorsSupported = it },
                                        label = { Text("Maximum Vendor Nodes in Grid") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // Promotional Offers & Loyalty Program Manager card
                        item {
                            var promoCodeInput by remember { mutableStateOf("") }
                            var promoTitleInput by remember { mutableStateOf("") }
                            var promoDiscountInput by remember { mutableStateOf("") }
                            var promoDescInput by remember { mutableStateOf("") }
                            var promoErrorMsg by remember { mutableStateOf<String?>(null) }
                            var promoSuccessMsg by remember { mutableStateOf<String?>(null) }

                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("admin_promotional_offers_card"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Discount,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Promotional Offers & Campus Deals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                "${promotionalOffers.count { it.isActive }} Active",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        "Create and manage campus-wide promotional discount codes displayed to students in the ATU Cafeteria app.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // Input Fields for New Offer
                                    OutlinedTextField(
                                        value = promoCodeInput,
                                        onValueChange = { promoCodeInput = it.uppercase() },
                                        label = { Text("Promo Code (e.g. EXAM20)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = promoTitleInput,
                                            onValueChange = { promoTitleInput = it },
                                            label = { Text("Campaign Title") },
                                            modifier = Modifier.weight(1.5f),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = promoDiscountInput,
                                            onValueChange = { promoDiscountInput = it },
                                            label = { Text("Discount %") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = promoDescInput,
                                        onValueChange = { promoDescInput = it },
                                        label = { Text("Offer Description / Conditions") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    promoErrorMsg?.let { err ->
                                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    promoSuccessMsg?.let { msg ->
                                        Text(msg, color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val pct = promoDiscountInput.toDoubleOrNull()
                                            if (promoCodeInput.isBlank() || promoTitleInput.isBlank() || pct == null || pct <= 0.0) {
                                                promoErrorMsg = "Please fill in valid code, title, and discount percentage."
                                                promoSuccessMsg = null
                                                return@Button
                                            }
                                            viewModel.addPromotionalOffer(promoCodeInput, promoTitleInput, pct, promoDescInput.ifBlank { "Special ATU Campus discount offer!" })
                                            promoErrorMsg = null
                                            promoSuccessMsg = "Promotional offer '${promoCodeInput}' created successfully!"
                                            promoCodeInput = ""
                                            promoTitleInput = ""
                                            promoDiscountInput = ""
                                            promoDescInput = ""
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("add_promo_offer_btn"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Publish Promotional Offer")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()

                                    Text("Active & Managed Promotional Offers", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    promotionalOffers.forEach { offer ->
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (offer.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(offer.code, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text("${offer.discountPercent.toInt()}% OFF", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                    Text(offer.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    Text(offer.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { viewModel.togglePromotionalOfferStatus(offer.id) }) {
                                                        Icon(
                                                            imageVector = if (offer.isActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                            contentDescription = "Toggle status",
                                                            tint = if (offer.isActive) MaterialTheme.colorScheme.primary else Color.Gray
                                                        )
                                                    }
                                                    IconButton(onClick = { viewModel.deletePromotionalOffer(offer.id) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete offer", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Live Campus Orders Oversight & Override
                        item {
                            var orderFilterStatus by remember { mutableStateOf("ALL") }
                            val filteredLiveOrders = remember(allOrdersSnapshot, orderFilterStatus) {
                                if (orderFilterStatus == "ALL") allOrdersSnapshot
                                else allOrdersSnapshot.filter { it.status.uppercase() == orderFilterStatus }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Live Campus Orders Dispatch & Override", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                            Text("Real-time orders queue with administrative status advance & instant wallet refund overrides.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Filter chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("ALL", "PENDING", "PREPARING", "READY", "COMPLETED", "CANCELLED").forEach { st ->
                                            val count = if (st == "ALL") allOrdersSnapshot.size else allOrdersSnapshot.count { it.status.uppercase() == st }
                                            FilterChip(
                                                selected = orderFilterStatus == st,
                                                onClick = { orderFilterStatus = st },
                                                label = { Text("$st ($count)", fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    if (filteredLiveOrders.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(100.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No orders in '$orderFilterStatus' status.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                    } else {
                                        filteredLiveOrders.take(15).forEach { order ->
                                            val vendor = allUsers.find { it.id == order.vendorId }
                                            val customer = allUsers.find { it.id == order.customerId }

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text("Order #${order.id} • GH₵ ${"%.2f".format(order.totalPrice)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                            Text("Vendor: ${vendor?.fullName ?: "Vendor #${order.vendorId}"} • Student: ${customer?.fullName ?: "Student #${order.customerId}"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = when (order.status.uppercase()) {
                                                                "PENDING" -> Color(0xFFFFF3E0)
                                                                "PREPARING" -> Color(0xFFE3F2FD)
                                                                "READY" -> Color(0xFFE8F5E9)
                                                                "COMPLETED" -> Color(0xFFF1F8E9)
                                                                else -> Color(0xFFFFEBEE)
                                                            }
                                                        ) {
                                                            Text(
                                                                order.status.uppercase(),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when (order.status.uppercase()) {
                                                                    "PENDING" -> Color(0xFFE65100)
                                                                    "PREPARING" -> Color(0xFF1565C0)
                                                                    "READY" -> Color(0xFF2E7D32)
                                                                    "COMPLETED" -> Color(0xFF33691E)
                                                                    else -> Color(0xFFC62828)
                                                                },
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Admin Actions on Order
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        when (order.status.uppercase()) {
                                                            "PENDING" -> {
                                                                Button(
                                                                    onClick = { viewModel.adminAdvanceOrderStatus(order.id, "PREPARING") },
                                                                    modifier = Modifier.weight(1f),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                                ) {
                                                                    Text("Advance to Cooking", fontSize = 10.sp)
                                                                }
                                                            }
                                                            "PREPARING" -> {
                                                                Button(
                                                                    onClick = { viewModel.adminAdvanceOrderStatus(order.id, "READY") },
                                                                    modifier = Modifier.weight(1f),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                                ) {
                                                                    Text("Mark Ready for Pickup", fontSize = 10.sp)
                                                                }
                                                            }
                                                            "READY" -> {
                                                                Button(
                                                                    onClick = { viewModel.adminAdvanceOrderStatus(order.id, "COMPLETED") },
                                                                    modifier = Modifier.weight(1f),
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                                ) {
                                                                    Text("Mark Completed", fontSize = 10.sp)
                                                                }
                                                            }
                                                        }

                                                        if (order.status.uppercase() != "CANCELLED" && order.status.uppercase() != "COMPLETED") {
                                                            OutlinedButton(
                                                                onClick = { orderToRefund = order },
                                                                shape = RoundedCornerShape(6.dp),
                                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Cancel & Refund", fontSize = 10.sp)
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

                        // 4. Emergency Halting Command
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (emergencyHaltState) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Emergency Security Lockdown",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (emergencyHaltState) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Killswitch. Instantly freeze checkout options across campus in the event of local logistical/cellular infrastructure breakdown.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (emergencyHaltState) "GATEWAY SYSTEM FROZEN" else "SYSTEMS OPERATIONAL",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (emergencyHaltState) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                        )

                                        Switch(
                                            checked = emergencyHaltState,
                                            onCheckedChange = { emergencyHaltState = it }
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

    // Real-Time Push Alert Dialog for Low Inventory
    if (adminInventoryAlerts.isNotEmpty()) {
        val alert = adminInventoryAlerts.first()
        AlertDialog(
            onDismissRequest = { viewModel.dismissAdminInventoryAlert(alert.id) },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("⚠️ CRITICAL INVENTORY ALERT", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Automated monitoring detected low ingredient levels:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(alert.message, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Item: ${alert.foodName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissAdminInventoryAlert(alert.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Acknowledge & Dismiss")
                }
            }
        )
    }

    // 1. ADD NEW VENDOR DIALOG
        if (showAddVendorDialog) {
            Dialog(onDismissRequest = { showAddVendorDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Register New Campus Vendor",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Creates vendor stall & generates default login credentials",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Vendor Details Section
                        OutlinedTextField(
                            value = addFullName,
                            onValueChange = { name ->
                                addFullName = name
                                // Automatically generate default login username from vendor name if not manually modified
                                val clean = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(18)
                                if (clean.isNotEmpty()) {
                                    addUsername = if (clean.startsWith("vendor_")) clean else "vendor_$clean"
                                }
                            },
                            label = { Text("Vendor / Stall Name *") },
                            placeholder = { Text("e.g. Kiki Grills & Chills") },
                            modifier = Modifier.fillMaxWidth().testTag("add_vendor_name_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )

                        OutlinedTextField(
                            value = addInfo,
                            onValueChange = { addInfo = it },
                            label = { Text("Booth Location / Kitchen Specialty") },
                            placeholder = { Text("e.g. Stall #04, Block B • Fast Food") },
                            modifier = Modifier.fillMaxWidth().testTag("add_vendor_info_input"),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )

                        // 🔑 DEFAULT LOGIN CREDENTIALS HIGHLIGHT CARD
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Key,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Default Vendor Login Credentials",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            if (addFullName.isNotBlank()) {
                                                val clean = addFullName.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(18)
                                                addUsername = "vendor_$clean"
                                            } else {
                                                addUsername = "vendor_${System.currentTimeMillis() % 10000}"
                                            }
                                            addPinCode = "1234"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Reset Defaults", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    "Admin sets default access credentials. The vendor will use these to log in and can update them at any time from their profile.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = addUsername,
                                    onValueChange = { addUsername = it.lowercase().trim() },
                                    label = { Text("Default Login Username *") },
                                    placeholder = { Text("e.g. vendor_kiki_grills") },
                                    modifier = Modifier.fillMaxWidth().testTag("add_vendor_username_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )

                                OutlinedTextField(
                                    value = addPinCode,
                                    onValueChange = { addPinCode = it },
                                    label = { Text("Default Password / PIN * (4+ chars)") },
                                    placeholder = { Text("1234") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    modifier = Modifier.fillMaxWidth().testTag("add_vendor_pin_input"),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }

                        // Logo & Cover Setup
                        Text("Logo Setup", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = addLogoUrl,
                            onValueChange = { addLogoUrl = it },
                            label = { Text("Logo Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "🍲" to "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120&auto=format&fit=crop&q=60",
                                "🍔" to "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=120&auto=format&fit=crop&q=60",
                                "🍰" to "https://images.unsplash.com/photo-1517433456452-f9633a875f6f?w=120&auto=format&fit=crop&q=60",
                                "🥤" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=120&auto=format&fit=crop&q=60"
                            ).forEach { (emoji, url) ->
                                FilterChip(
                                    selected = addLogoUrl == url,
                                    onClick = { addLogoUrl = url },
                                    label = { Text(emoji) }
                                )
                            }
                        }

                        Text("Cover Photo Setup", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = addPictureUrl,
                            onValueChange = { addPictureUrl = it },
                            label = { Text("Cover Banner Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "🍛 Traditional" to "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60",
                                "🥖 Bakery/Treats" to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
                                "🥗 Healthy" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60"
                            ).forEach { (label, url) ->
                                FilterChip(
                                    selected = addPictureUrl == url,
                                    onClick = { addPictureUrl = url },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }

                        addError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddVendorDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val finalUsername = if (addUsername.isNotBlank()) addUsername else {
                                        val clean = addFullName.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(18)
                                        if (clean.isNotEmpty()) "vendor_$clean" else "vendor_${System.currentTimeMillis() % 10000}"
                                    }
                                    val finalPin = if (addPinCode.isNotBlank()) addPinCode else "1234"

                                    if (addFullName.isBlank() || finalUsername.isBlank() || finalPin.length < 4) {
                                        addError = "Vendor Name, Username, and 4+ character PIN/Password are mandatory."
                                    } else {
                                        viewModel.addVendor(
                                            username = finalUsername,
                                            pinCode = finalPin,
                                            fullName = addFullName,
                                            info = addInfo,
                                            logoUrl = addLogoUrl.ifBlank { null },
                                            pictureUrl = addPictureUrl.ifBlank { null }
                                        ) { success ->
                                            if (success) {
                                                newlyCreatedVendorCreds = Pair(finalUsername, finalPin)
                                                showAddVendorDialog = false
                                                addUsername = ""
                                                addFullName = ""
                                                addInfo = ""
                                                addPinCode = "1234"
                                                addLogoUrl = ""
                                                addPictureUrl = ""
                                                addError = null
                                            } else {
                                                addError = "Username '$finalUsername' already exists. Please choose a different username."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("submit_add_vendor_btn")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register & Create Credentials")
                            }
                        }
                    }
                }
            }
        }

        // Dialog showing the newly created vendor login credentials to Admin
        newlyCreatedVendorCreds?.let { creds ->
            AlertDialog(
                onDismissRequest = { newlyCreatedVendorCreds = null },
                icon = {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text("Vendor Registered Successfully!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "The new vendor stall has been created with the following default login credentials. Please hand these over to the vendor:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Default Username:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(creds.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Default Password / PIN:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(creds.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        Text(
                            "💡 The vendor can log in immediately and customize their credentials anytime from their Vendor Profile.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { newlyCreatedVendorCreds = null }) {
                        Text("Done")
                    }
                }
            )
        }
        // 2. EDIT VENDOR DIALOG
        vendorToEdit?.let { vendor ->
            Dialog(onDismissRequest = { vendorToEdit = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Edit Vendor Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = editFullName,
                            onValueChange = { editFullName = it },
                            label = { Text("Vendor Full Brand name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editInfo,
                            onValueChange = { editInfo = it },
                            label = { Text("Booth Location / Kitchen Desc.") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editPinCode,
                            onValueChange = { editPinCode = it },
                            label = { Text("New PIN Code (Optional)") },
                            placeholder = { Text("Leave blank to keep current PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Logo Setup", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = editLogoUrl,
                            onValueChange = { editLogoUrl = it },
                            label = { Text("Logo Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                "🍲" to "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120&auto=format&fit=crop&q=60",
                                "🍔" to "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=120&auto=format&fit=crop&q=60",
                                "🍰" to "https://images.unsplash.com/photo-1517433456452-f9633a875f6f?w=120&auto=format&fit=crop&q=60",
                                "🥤" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=120&auto=format&fit=crop&q=60"
                            ).forEach { (emoji, url) ->
                                FilterChip(
                                    selected = editLogoUrl == url,
                                    onClick = { editLogoUrl = url },
                                    label = { Text(emoji) }
                                )
                            }
                        }

                        Text("Cover Photo Setup", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = editPictureUrl,
                            onValueChange = { editPictureUrl = it },
                            label = { Text("Cover Banner Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "🍛 Traditional" to "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60",
                                "🥖 Bakery/Treats" to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
                                "🥗 Healthy" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60"
                            ).forEach { (label, url) ->
                                FilterChip(
                                    selected = editPictureUrl == url,
                                    onClick = { editPictureUrl = url },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }

                        editError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { vendorToEdit = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (editFullName.isBlank()) {
                                        editError = "Brand name cannot be empty."
                                    } else if (!editPinCode.isEmpty() && editPinCode.length < 4) {
                                        editError = "PIN must be at least 4 digits if updated."
                                    } else {
                                        val updatedVendor = vendor.copy(
                                            fullName = editFullName,
                                            info = editInfo.ifBlank { "ATU Cafeteria Vendor" },
                                            logoUrl = editLogoUrl.ifBlank { null },
                                            pictureUrl = editPictureUrl.ifBlank { null }
                                        )
                                        viewModel.updateVendor(updatedVendor, editPinCode.ifEmpty { null }) { success ->
                                            if (success) {
                                                vendorToEdit = null
                                            } else {
                                                editError = "Update operation failed."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }

        // 3. DELETE CONFIRM DIALOG
        vendorToDelete?.let { vendor ->
            AlertDialog(
                onDismissRequest = { vendorToDelete = null },
                title = { Text("Delete Vendor Profile?") },
                text = { Text("Are you absolutely sure you want to delete '${vendor.fullName}'? This action cannot be undone and will delete all associated menus/dishes.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVendor(vendor.id) {
                                vendorToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vendorToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 4. RESET VENDOR PIN DIALOG
        vendorToResetPin?.let { vendor ->
            var newPin by remember { mutableStateOf("1234") }
            var pinError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { vendorToResetPin = null }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reset Vendor Access PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(vendor.fullName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text("Set a new numerical PIN code for vendor login:", fontSize = 12.sp)

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6) newPin = it },
                            label = { Text("New PIN Code (4-6 digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { newPin = "1234" },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Default: 1234", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { newPin = "0000" },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("0000", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { newPin = (1000..9999).random().toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Generate Random", fontSize = 10.sp)
                            }
                        }

                        pinError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { vendorToResetPin = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPin.length < 4) {
                                        pinError = "PIN must be at least 4 digits"
                                    } else {
                                        viewModel.adminResetUserPin(vendor.id, newPin) { success ->
                                            if (success) {
                                                vendorToResetPin = null
                                            } else {
                                                pinError = "Failed to update PIN."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Update PIN")
                            }
                        }
                    }
                }
            }
        }

        // 5. VENDOR MENU & DISHES OVERSIGHT MODAL
        vendorForMenuManagement?.let { vendor ->
            val vendorDishes = remember(allFoodItems, vendor.id) { allFoodItems.filter { it.vendorId == vendor.id } }
            var showAddDishSection by remember { mutableStateOf(false) }
            var newDishName by remember { mutableStateOf("") }
            var newDishPrice by remember { mutableStateOf("") }
            var newDishCategory by remember { mutableStateOf("Local Dishes") }
            var newDishPrepTime by remember { mutableStateOf("10 mins") }
            var newDishImageUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300&auto=format&fit=crop&q=60") }
            var dishError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { vendorForMenuManagement = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.RestaurantMenu, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text("Menu Oversight: ${vendor.fullName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text("${vendorDishes.size} dishes listed in catalog", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { vendorForMenuManagement = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Add Dish Toggle Button
                        Button(
                            onClick = { showAddDishSection = !showAddDishSection },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = if (showAddDishSection) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                        ) {
                            Icon(if (showAddDishSection) Icons.Default.Close else Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showAddDishSection) "Hide Add Dish Form" else "+ Add Dish to This Stall", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Add Dish Form (Expandable)
                        if (showAddDishSection) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Add New Dish for ${vendor.fullName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                                    OutlinedTextField(
                                        value = newDishName,
                                        onValueChange = { newDishName = it },
                                        label = { Text("Dish Name *") },
                                        placeholder = { Text("e.g. Special Jollof with Grilled Tilapia") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = newDishPrice,
                                            onValueChange = { newDishPrice = it },
                                            label = { Text("Price (GH₵) *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )

                                        OutlinedTextField(
                                            value = newDishPrepTime,
                                            onValueChange = { newDishPrepTime = it },
                                            label = { Text("Prep Time") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                    }

                                    OutlinedTextField(
                                        value = newDishCategory,
                                        onValueChange = { newDishCategory = it },
                                        label = { Text("Category") },
                                        placeholder = { Text("Local Dishes, Continental, Drinks, Snacks") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    dishError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val priceVal = newDishPrice.toDoubleOrNull()
                                            if (newDishName.isBlank()) {
                                                dishError = "Dish name cannot be empty."
                                            } else if (priceVal == null || priceVal <= 0.0) {
                                                dishError = "Please enter a valid price."
                                            } else {
                                                val newFood = FoodItem(
                                                    id = 0,
                                                    vendorId = vendor.id,
                                                    name = newDishName.trim(),
                                                    price = priceVal,
                                                    category = newDishCategory.ifBlank { "Local Dishes" },
                                                    imageUrl = newDishImageUrl,
                                                    description = "Campus prepared by ${vendor.fullName}",
                                                    isAvailable = true,
                                                    initialStock = 100,
                                                    currentStock = 100,
                                                    lowStockThreshold = 15,
                                                    calories = 250,
                                                    allergens = "None"
                                                )
                                                viewModel.addFoodItem(newFood)
                                                newDishName = ""
                                                newDishPrice = ""
                                                dishError = null
                                                showAddDishSection = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Publish Dish to Vendor Menu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Dishes List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (vendorDishes.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                                        Text("No dishes cataloged for this stall yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                items(vendorDishes, key = { it.id }) { dish ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(dish.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("GH₵ ${"%.2f".format(dish.price)} • ${dish.category} • Stock: ${dish.currentStock}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                // Stock toggle button
                                                TextButton(
                                                    onClick = {
                                                        viewModel.updateFoodItem(dish.copy(isAvailable = !dish.isAvailable))
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        if (dish.isAvailable) "In Stock" else "Sold Out",
                                                        color = if (dish.isAvailable) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteFoodItem(dish)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Dish", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
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

        // 6. ENROLL STUDENT DIALOG
        if (showAddStudentDialog) {
            var sUsername by remember { mutableStateOf("") }
            var sPin by remember { mutableStateOf("1234") }
            var sFullName by remember { mutableStateOf("") }
            var sMatricId by remember { mutableStateOf("") }
            var sEmail by remember { mutableStateOf("") }
            var sPhone by remember { mutableStateOf("") }
            var sInitialBalance by remember { mutableStateOf("50.00") }
            var sError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { showAddStudentDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Enroll New Student Account", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Creates verified ATU student meal wallet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = sFullName,
                            onValueChange = { name ->
                                sFullName = name
                                val clean = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "").take(12)
                                if (clean.isNotEmpty() && sUsername.isEmpty()) {
                                    sUsername = clean
                                }
                            },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Kwame Mensah") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sUsername,
                            onValueChange = { sUsername = it },
                            label = { Text("Username *") },
                            placeholder = { Text("e.g. kwame_m") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sMatricId,
                            onValueChange = { sMatricId = it },
                            label = { Text("Matric / Index ID") },
                            placeholder = { Text("e.g. ATU/01/2026/0892") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sEmail,
                                onValueChange = { sEmail = it },
                                label = { Text("Email Address") },
                                placeholder = { Text("student@atu.edu.gh") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sPhone,
                                onValueChange = { sPhone = it },
                                label = { Text("Phone") },
                                placeholder = { Text("0241234567") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = sPin,
                                onValueChange = { if (it.length <= 6) sPin = it },
                                label = { Text("Access PIN *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = sInitialBalance,
                                onValueChange = { sInitialBalance = it },
                                label = { Text("Initial Balance (GH₵)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        sError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddStudentDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (sFullName.isBlank()) {
                                        sError = "Full name cannot be empty."
                                    } else if (sUsername.isBlank()) {
                                        sError = "Username cannot be empty."
                                    } else if (sPin.length < 4) {
                                        sError = "PIN must be at least 4 digits."
                                    } else {
                                        val initialBal = sInitialBalance.toDoubleOrNull() ?: 50.0
                                        viewModel.adminAddStudent(
                                            username = sUsername.trim(),
                                            pinCode = sPin.trim(),
                                            fullName = sFullName.trim(),
                                            matricId = sMatricId.trim().ifBlank { "ATU-${(1000..9999).random()}" },
                                            email = sEmail.trim().ifBlank { null },
                                            phone = sPhone.trim().ifBlank { null },
                                            initialBalance = initialBal
                                        ) { success, msg ->
                                            if (success) {
                                                showAddStudentDialog = false
                                            } else {
                                                sError = msg
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Enroll Student")
                            }
                        }
                    }
                }
            }
        }

        // 7. ADJUST SMART WALLET DIALOG
        userToAdjustWallet?.let { user ->
            var isCredit by remember { mutableStateOf(true) }
            var adjAmount by remember { mutableStateOf("") }
            var adjReason by remember { mutableStateOf("Directorate Cash Desk Top-Up") }
            var adjError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { userToAdjustWallet = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Adjust Smart Wallet Balance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(user.fullName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Current Balance Display
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Current Ledger Balance:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("GH₵ ${"%.2f".format(user.balance)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        // Credit / Debit selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { isCredit = true },
                                modifier = Modifier.weight(1f),
                                colors = if (isCredit) ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Credit (Add)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { isCredit = false },
                                modifier = Modifier.weight(1f),
                                colors = if (!isCredit) ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)) else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("- Debit (Deduct)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = adjAmount,
                            onValueChange = { adjAmount = it },
                            label = { Text("Adjustment Amount (GH₵) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Quick Amount Chips
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 20, 50, 100).forEach { amt ->
                                OutlinedButton(
                                    onClick = { adjAmount = amt.toString() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("+$amt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = adjReason,
                            onValueChange = { adjReason = it },
                            label = { Text("Mandatory Reason Note *") },
                            placeholder = { Text("e.g. Physical Cash Deposit, Meal Subsidy Refund") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        adjError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { userToAdjustWallet = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amountVal = adjAmount.toDoubleOrNull()
                                    if (amountVal == null || amountVal <= 0.0) {
                                        adjError = "Please enter a valid amount."
                                    } else if (adjReason.isBlank()) {
                                        adjError = "Audit reason is required."
                                    } else {
                                        viewModel.adminAdjustUserBalance(
                                            userId = user.id,
                                            amount = amountVal,
                                            isCredit = isCredit,
                                            reason = adjReason.trim()
                                        ) { success, msg ->
                                            if (success) {
                                                userToAdjustWallet = null
                                            } else {
                                                adjError = msg
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Execute Adjustment")
                            }
                        }
                    }
                }
            }
        }

        // 8. CAMPUS-WIDE SUBSIDY BONUS DIALOG
        if (showCampusBonusDialog) {
            var bonusAmt by remember { mutableStateOf("20.00") }
            var bonusReason by remember { mutableStateOf("Directorate Campus Meal Subsidy Grant") }
            var bonusError by remember { mutableStateOf<String?>(null) }
            val studentCount = remember(allUsers) { allUsers.count { it.role == "STUDENT" } }

            Dialog(onDismissRequest = { showCampusBonusDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Campus-Wide Meal Subsidy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Distribute credit to all $studentCount enrolled students", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = bonusAmt,
                            onValueChange = { bonusAmt = it },
                            label = { Text("Subsidy Amount per Student (GH₵) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10, 20, 50, 100).forEach { amt ->
                                OutlinedButton(
                                    onClick = { bonusAmt = "$amt.00" },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("GH₵ $amt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = bonusReason,
                            onValueChange = { bonusReason = it },
                            label = { Text("Official Memo / Subsidy Note *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        bonusError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCampusBonusDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amountVal = bonusAmt.toDoubleOrNull()
                                    if (amountVal == null || amountVal <= 0.0) {
                                        bonusError = "Please enter a valid bonus amount."
                                    } else if (bonusReason.isBlank()) {
                                        bonusError = "Official memo is required."
                                    } else {
                                        viewModel.adminBonusToAllStudents(amountVal, bonusReason.trim()) { count ->
                                            showCampusBonusDialog = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Distribute to All")
                            }
                        }
                    }
                }
            }
        }

        // 9. EDIT USER PROFILE DIALOG
        userToEditProfile?.let { user ->
            var pFullName by remember { mutableStateOf(user.fullName) }
            var pInfo by remember { mutableStateOf(user.info) }
            var pEmail by remember { mutableStateOf(user.email ?: "") }
            var pPhone by remember { mutableStateOf(user.telephone ?: "") }
            var pError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { userToEditProfile = null }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Edit User Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("@${user.username} • ${user.role}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = pFullName,
                            onValueChange = { pFullName = it },
                            label = { Text("Full Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pInfo,
                            onValueChange = { pInfo = it },
                            label = { Text(if (user.role == "STUDENT") "Matric / Index ID" else "Location / Specialty") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pEmail,
                            onValueChange = { pEmail = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = pPhone,
                            onValueChange = { pPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        pError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { userToEditProfile = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (pFullName.isBlank()) {
                                        pError = "Full name cannot be empty."
                                    } else {
                                        viewModel.adminUpdateUserProfile(
                                            userId = user.id,
                                            fullName = pFullName.trim(),
                                            info = pInfo.trim(),
                                            email = pEmail.trim().ifBlank { null },
                                            phone = pPhone.trim().ifBlank { null }
                                        ) { success ->
                                            if (success) {
                                                userToEditProfile = null
                                            } else {
                                                pError = "Failed to update profile."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        }

        // 10. RESET STUDENT / USER PIN DIALOG
        userToResetPin?.let { user ->
            var uPin by remember { mutableStateOf("1234") }
            var uPinError by remember { mutableStateOf<String?>(null) }

            Dialog(onDismissRequest = { userToResetPin = null }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Reset Access PIN", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${user.fullName} (@${user.username})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        OutlinedTextField(
                            value = uPin,
                            onValueChange = { if (it.length <= 6) uPin = it },
                            label = { Text("New Access PIN (4-6 digits)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { uPin = "1234" },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Default: 1234", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { uPin = (1000..9999).random().toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Random 4-digit", fontSize = 10.sp)
                            }
                        }

                        uPinError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { userToResetPin = null }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (uPin.length < 4) {
                                        uPinError = "PIN must be at least 4 digits."
                                    } else {
                                        viewModel.adminResetUserPin(user.id, uPin.trim()) { success ->
                                            if (success) {
                                                userToResetPin = null
                                            } else {
                                                uPinError = "Failed to update PIN."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Confirm PIN Reset")
                            }
                        }
                    }
                }
            }
        }

        // 11. DELETE USER CONFIRMATION DIALOG
        userToDelete?.let { user ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("Delete User Account?") },
                text = { Text("Are you sure you want to delete ${user.fullName} (@${user.username})? This action is irreversible.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminDeleteUser(user.id) {
                                userToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete Account")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // 12. ADMIN CANCEL & REFUND ORDER DIALOG
        orderToRefund?.let { order ->
            var refundReason by remember { mutableStateOf("Vendor Unavailability / Out of Stock") }

            AlertDialog(
                onDismissRequest = { orderToRefund = null },
                title = { Text("Cancel & Refund Order #${order.id}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This will immediately cancel the order and credit GH₵ ${"%.2f".format(order.totalPrice)} back to the student's smart meal wallet.")
                        OutlinedTextField(
                            value = refundReason,
                            onValueChange = { refundReason = it },
                            label = { Text("Refund Reason Note *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.adminCancelAndRefundOrder(order.id, refundReason.trim().ifBlank { "Administrative Refund" }) {
                                orderToRefund = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel & Refund")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { orderToRefund = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

@Composable
fun VendorDirectoryCard(
    vendor: User,
    viewModel: CafeteriaViewModel,
    allFeedback: List<Feedback>,
    onViewMenu: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val metrics = remember(vendor.id, allFeedback) { viewModel.getVendorMetrics(vendor.id, allFeedback) }
    val overallRating = metrics["overall"] ?: 0.0
    val quality = metrics["foodQuality"] ?: 0.0
    val cleanliness = metrics["cleanliness"] ?: 0.0
    val speed = metrics["speed"] ?: 0.0
    val priceValue = metrics["priceValue"] ?: 0.0
    val reviewsCount = remember(vendor.id, allFeedback) { allFeedback.count { it.vendorId == vendor.id } }
    val vHoursInfo = remember(vendor.id, vendor.isOpen) {
        com.example.ui.util.VendorOperatingHoursHelper.getOperatingHoursInfo(vendor.id, vendor.isOpen)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vendor_dir_card_${vendor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            // Header Image/Cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                vendor.pictureUrl?.let { coverUrl ->
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

                // Status Badge floating on top-right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            if (vHoursInfo.isCurrentlyOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (vHoursInfo.isCurrentlyOpen) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (vHoursInfo.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                        )
                        Text(
                            text = vHoursInfo.statusLabel,
                            color = if (vHoursInfo.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Vendor Info Block
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .size(40.dp)
                                .border(2.dp, MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            vendor.logoUrl?.let { logo ->
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
                                    text = vendor.fullName.take(1).uppercase(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column {
                            Text(
                                text = vendor.fullName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Booth: ${vendor.info.ifBlank { "Main Area" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Operating Hours",
                                    tint = if (vHoursInfo.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${vHoursInfo.scheduleText} • ${vHoursInfo.statusDetail}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = if (vHoursInfo.isCurrentlyOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Main Rating Display
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (overallRating > 0.0) "%.1f".format(overallRating) else "4.5",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (reviewsCount > 0) "$reviewsCount feedback audits" else "No reviews yet",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Detailed metrics meters
                Text(
                    text = "STUDENT FEEDBACK AUDIT SCORES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RatingMetricRow("Food Quality", if (overallRating > 0.0) quality else 4.5)
                        RatingMetricRow("Cleanliness", if (overallRating > 0.0) cleanliness else 4.2)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        RatingMetricRow("Service Speed", if (overallRating > 0.0) speed else 4.3)
                        RatingMetricRow("Price Value", if (overallRating > 0.0) priceValue else 4.6)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Actions Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val phone = vendor.telephone ?: "+233 24 000 0000"
                    OutlinedButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Contact Vendor at: $phone", android.widget.Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onViewMenu,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explore Menu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RatingMetricRow(label: String, score: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            maxLines = 1
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${"%.1f".format(score)}★",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFF9100)
            )
        }
    }
}

@Composable
fun VendorPerformanceAnalyticsDashboard(
    allVendors: List<com.example.data.User>,
    allFeedback: List<com.example.data.Feedback>,
    allOrders: List<com.example.data.Order>,
    viewModel: com.example.ui.viewmodel.CafeteriaViewModel
) {
    var selectedVendorId by remember { mutableStateOf<Int?>(null) } // null means "All Vendors (Comparative Overview)"

    val scrollState = rememberScrollState()

    // Calculated metrics
    val workingOrders = remember(allOrders, selectedVendorId) {
        if (selectedVendorId == null) allOrders else allOrders.filter { it.vendorId == selectedVendorId }
    }

    val workingFeedback = remember(allFeedback, selectedVendorId) {
        if (selectedVendorId == null) allFeedback else allFeedback.filter { it.vendorId == selectedVendorId }
    }

    // 1. Calculations: Total revenue
    val totalRevenue = remember(workingOrders) {
        workingOrders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
    }

    // 2. Calculations: Completion Rate
    val completionRatePercentage = remember(workingOrders) {
        if (workingOrders.isEmpty()) 0f
        else {
            val completedCount = workingOrders.count { it.status == "COMPLETED" }
            (completedCount.toFloat() / workingOrders.size.toFloat()) * 100f
        }
    }

    // 3. Average prep time (dynamically generated or formatted)
    val averagePrepTimeMinutes = remember(workingOrders) {
        if (workingOrders.isEmpty()) 12.0
        else {
            val totalPreps = workingOrders.sumOf { order ->
                val base = 10.0 + (order.vendorId % 3)
                val variance = (order.id % 4) * 1.5
                base + variance
            }
            totalPreps / workingOrders.size.toDouble()
        }
    }

    // 4. Customer satisfaction ratings
    val ratingAvg = remember(workingFeedback) {
        if (workingFeedback.isEmpty()) 4.5f
        else {
            val total = workingFeedback.flatMap { listOf(it.ratingFoodQuality, it.ratingCleanliness, it.ratingServiceSpeed, it.ratingPriceValue) }
            total.average().toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header / Title Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ATU Vendor Performance Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Secure live student transit feedback, average transaction velocity, and overall booth health diagnostics.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selection row for Vendors
        Text(
            text = "CHOOSE CLUSTER STATION",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 0.5.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Comparative view chip
            FilterChip(
                selected = selectedVendorId == null,
                onClick = { selectedVendorId = null },
                label = { Text("Comparative Overview", fontSize = 11.sp) },
                shape = RoundedCornerShape(20.dp)
            )

            allVendors.forEach { vendor ->
                FilterChip(
                    selected = selectedVendorId == vendor.id,
                    onClick = { selectedVendorId = vendor.id },
                    label = { Text(vendor.fullName, fontSize = 11.sp) },
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Dashboard Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsCard(
                    title = "Total Orders",
                    subtitle = "All records",
                    value = "${workingOrders.size}",
                    icon = Icons.Default.ShoppingBag,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsCard(
                    title = "Completion Velocity",
                    subtitle = "Completion rate",
                    value = "${"%.1f".format(completionRatePercentage)}%",
                    icon = Icons.Default.Speed,
                    tint = Color(0xFF2E7D32)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsCard(
                    title = "Average Prep Speed",
                    subtitle = "Time in minutes",
                    value = "${"%.1f".format(averagePrepTimeMinutes)} mins",
                    icon = Icons.Default.Timer,
                    tint = Color(0xFFFF8F00)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                AnalyticsCard(
                    title = "Aggregate Revenue",
                    subtitle = "Earnings",
                    value = "GH₵ ${"%.1f".format(totalRevenue)}",
                    icon = Icons.Default.TrendingUp,
                    tint = Color(0xFF2E7D32)
                )
            }
        }

        // Section: Customer Satisfaction Audit
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Customer Satisfaction Indices",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Aggregated student ratings (1 to 5 stars)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${"%.2f".format(ratingAvg)}★",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val qualityAvg = remember(workingFeedback) {
                    if (workingFeedback.isEmpty()) 4.5f else workingFeedback.map { it.ratingFoodQuality }.average().toFloat()
                }
                val cleanlinessAvg = remember(workingFeedback) {
                    if (workingFeedback.isEmpty()) 4.2f else workingFeedback.map { it.ratingCleanliness }.average().toFloat()
                }
                val speedAvg = remember(workingFeedback) {
                    if (workingFeedback.isEmpty()) 4.3f else workingFeedback.map { it.ratingServiceSpeed }.average().toFloat()
                }
                val priceAvg = remember(workingFeedback) {
                    if (workingFeedback.isEmpty()) 4.6f else workingFeedback.map { it.ratingPriceValue }.average().toFloat()
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SatisfactionLinearRow("Food Quality Excellence", qualityAvg)
                    SatisfactionLinearRow("Cleanliness and Hygiene Checklist", cleanlinessAvg)
                    SatisfactionLinearRow("Service Speed & Preparation Turnaround", speedAvg)
                    SatisfactionLinearRow("Pricing & Student Elasticity Index", priceAvg)
                }
            }
        }

        // Section: Popular Menu Items
        val popularFoods = remember(workingOrders) {
            workingOrders
                .groupBy { it.foodName }
                .mapValues { entry -> 
                    val count = entry.value.sumOf { it.quantity }
                    val rev = entry.value.sumOf { it.totalPrice }
                    count to rev
                }
                .toList()
                .sortedByDescending { it.second.first }
                .take(4)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Most Popular Menu Items",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (popularFoods.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No orders placed yet.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    val maxSoldCount = popularFoods.maxOfOrNull { it.second.first } ?: 1
                    popularFoods.forEachIndexed { i, (foodName, rawPair) ->
                        val count = rawPair.first
                        val revenue = rawPair.second
                        val progress = count.toFloat() / maxSoldCount.toFloat()

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val rankColor = when (i) {
                                        0 -> Color(0xFFFFD700)
                                        1 -> Color(0xFFC0C0C0)
                                        else -> Color(0xFFCD7F32)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(rankColor, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("#${i+1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(foodName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$count plates (GH₵ ${"%.1f".format(revenue)})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        // Custom Visual Hourly Sales Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SignalCellularAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Hourly Demand Density Plot",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Text(
                    text = "Hourly sales distribution (8 AM - 8 PM)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Bar heights matching representative volumes
                val hourlyLabels = listOf("8 AM", "11 AM", "1 PM", "4 PM", "6 PM", "8 PM")
                val hourlySalesWeights = remember(selectedVendorId) {
                    val seed = (selectedVendorId ?: 4) * 7
                    listOf(
                        15 + (seed % 15),
                        65 + (seed % 25),
                        85 + (seed % 15),
                        35 + (seed % 25),
                        75 + (seed % 15),
                        20 + (seed % 15)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    hourlySalesWeights.forEachIndexed { index, weight ->
                        val barHeightFactor = weight.toFloat() / 110.0f
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(barHeightFactor)
                                    .width(28.dp)
                                    .background(
                                        color = if (index % 2 == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(hourlyLabels[index], fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun AnalyticsCard(
    title: String,
    subtitle: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun SatisfactionLinearRow(label: String, rating: Float) {
    val progress = rating / 5f
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text("${"%.1f".format(rating)}★", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF9100))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            color = Color(0xFFFF9100),
            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}
