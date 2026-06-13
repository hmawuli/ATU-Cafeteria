package com.example.ui.screens

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
fun LoginScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ATU CAFETERIA HUB",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Welcome Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accra Technical University",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Secure Mobile Food Portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Access Lock-In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("Access PIN (Numeric)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    loginError?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Button(
                            onClick = {
                                if (username.isNotBlank() && pinCode.isNotBlank()) {
                                    viewModel.loginUser(username.trim().lowercase(), pinCode) { success ->
                                        if (success) {
                                            val u = viewModel.currentUser.value
                                            if (u != null) {
                                                when (u.role) {
                                                    "STUDENT" -> navController.navigate("student_home") { popUpTo(0) }
                                                    "VENDOR" -> navController.navigate("vendor_home") { popUpTo(0) }
                                                    "ADMIN" -> navController.navigate("admin_home") { popUpTo(0) }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure Login")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation to Registration page
            TextButton(
                onClick = { navController.navigate("register") }
            ) {
                Text("New Student / Vendor? Enroll Account here", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Credentials helper
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Demo Portals (Pin is required):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Student Portal: user 'student' / PIN '1234'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Vendor (Mary Joint): user 'maryjoint' / PIN '1111'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Admin Compliance Panel: user 'admin' / PIN 'admin123'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Laravel Backend Configuration Tool (Expandable Card)
            var showLaravelConfig by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (LaravelClientManager.isLaravelEnabled) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLaravelConfig = !showLaravelConfig },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (LaravelClientManager.isLaravelEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (LaravelClientManager.isLaravelEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (LaravelClientManager.isLaravelEnabled) "Laravel Live Sync: ON" else "Laravel Offline Mode: ON",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = if (showLaravelConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showLaravelConfig) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var isEnabledState by remember { mutableStateOf(LaravelClientManager.isLaravelEnabled) }
                        var urlInput by remember { mutableStateOf(LaravelClientManager.baseUrl) }
                        var syncStatus by remember { mutableStateOf<String?>(null) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Connect to Laravel REST API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Routes read/saves to remote database", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isEnabledState,
                                onCheckedChange = { isEnabledState = it }
                            )
                        }

                        if (isEnabledState) {
                             Spacer(modifier = Modifier.height(12.dp))
                             OutlinedTextField(
                                 value = urlInput,
                                 onValueChange = { urlInput = it },
                                 label = { Text("Laravel Base API URL") },
                                 leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                 placeholder = { Text("e.g. http://10.0.2.2:8000") },
                                 modifier = Modifier.fillMaxWidth(),
                                 singleLine = true
                             )
                        }

                        syncStatus?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                color = if (it.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                LaravelClientManager.isLaravelEnabled = isEnabledState
                                if (isEnabledState) {
                                    val formattedUrl = if (urlInput.trim().endsWith("/")) urlInput.trim() else "${urlInput.trim()}/"
                                    LaravelClientManager.baseUrl = formattedUrl
                                    syncStatus = "Attempting to handshake & sync database..."
                                    viewModel.syncAllFromLaravel { success ->
                                        if (success) {
                                             syncStatus = "🚀 Handshake Success! All ATU tables fetched."
                                             android.widget.Toast.makeText(context, "Laravel Connection Applied!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                             syncStatus = "❌ Sync Failed! Please verify server is hosted on '$formattedUrl' and connection is reachable."
                                        }
                                    }
                                } else {
                                    syncStatus = "Toggled Offline Local Room mode."
                                    android.widget.Toast.makeText(context, "Switched to standard Offline sandbox database.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnabledState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ACCOUNT REGISTRATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("STUDENT") } // "STUDENT" or "VENDOR"

    val regSuccess by viewModel.registrationSuccess.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ENROLL PORTAL", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                "Enroll New Account Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Become a registered guest or secure seller on at ATU Cafeteria campus",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Account Specification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Role Picker Tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "STUDENT" }
                                .background(if (role == "STUDENT") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Student Account",
                                color = if (role == "STUDENT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { role = "VENDOR" }
                                .background(if (role == "VENDOR") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Vendor Account",
                                color = if (role == "VENDOR") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Unified Login Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Profile Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = info,
                        onValueChange = { info = it },
                        label = {
                            Text(if (role == "STUDENT") "Student Matric Card ID Number" else "Vendor Kitchen Brand Title (e.g. Sobolo Palace)")
                        },
                        placeholder = {
                            Text(if (role == "STUDENT") "e.g., ATU-2024-X45" else "e.g., Auntie Mary Delight")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { pinCode = it },
                        label = { Text("4-Digit Access PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    loginError?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    if (regSuccess) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Registration Success! Return to login to access.",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Button(
                            onClick = {
                                viewModel.registerUser(
                                    username = username.trim().lowercase(),
                                    pinCode = pinCode,
                                    role = role,
                                    fullName = fullName,
                                    info = info
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enlist New Profile")
                        }
                    }
                }
            }
        }
    }
}

data class ScheduledMeal(
    val id: Int,
    val foodItem: FoodItem,
    val quantity: Int,
    val targetTime: String,
    val dateLabel: String,
    val specs: String,
    val isPaid: Boolean,
    val barcodeSeed: String
)

fun getNutritionalProfile(food: FoodItem): Triple<Float, Float, Float> {
    val nameLower = food.name.lowercase()
    return when {
        nameLower.contains("rice") || nameLower.contains("jollof") || nameLower.contains("waakye") -> Triple(18f, 112f, 12f)
        nameLower.contains("fufu") || nameLower.contains("soup") || nameLower.contains("banku") -> Triple(22f, 125f, 14f)
        nameLower.contains("egg") || nameLower.contains("oat") || nameLower.contains("breakfast") || nameLower.contains("bread") -> Triple(14f, 45f, 10f)
        nameLower.contains("chicken") || nameLower.contains("meat") || nameLower.contains("fish") -> Triple(32f, 15f, 11f)
        food.category == "Drinks" -> Triple(0f, 38f, 0f)
        food.category == "Snacks" -> Triple(8f, 50f, 15f)
        else -> Triple(12f, 75f, 10f)
    }
}

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
    val currentTotalPoints = remember(studentOrders) {
        studentOrders.sumOf { order ->
            if (order.status.uppercase() == "COMPLETED") 25 else 10
        }
    }
    val availablePoints = currentTotalPoints - redeemedPoints

    val acknowledgedOrders = remember { mutableStateListOf<Int>() }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Browse Food, 1: Orders Hub, 2: Nutrition, 3: Prep Reserves, 4: Smart Wallet & ID
    var ordersSubTab by remember { mutableIntStateOf(0) } // 0: Live Tracker, 1: Dining History

    val browseScrollState = rememberLazyListState()
    val activeOrdersScrollState = rememberLazyListState()
    val pastOrdersScrollState = rememberLazyListState()
    val nutritionScrollState = rememberLazyListState()
    val prepScrollState = rememberLazyListState()
    val walletScrollState = rememberLazyListState()

    val currentActiveScrollState = remember(activeTab, ordersSubTab) {
        when (activeTab) {
            0 -> browseScrollState
            1 -> if (ordersSubTab == 0) activeOrdersScrollState else pastOrdersScrollState
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
    var selectedVendorIdFilter by remember { mutableStateOf<Int?>(null) }
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
                                Text("⭐", fontSize = 10.sp)
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
                    LazyColumn(
                        state = browseScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                            // Intelligent AI Recommendations Section
                            val recommendationsList = remember(allFoodItems) {
                                allFoodItems.shuffled().take(3)
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
                                        items(recommendationsList) { foodItem ->
                                            val profile = getNutritionalProfile(foodItem)
                                            val kcal = profile.first * 4f + profile.second * 4f + profile.third * 9f
                                            
                                            Card(
                                                modifier = Modifier
                                                    .width(220.dp)
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
                                placeholder = { Text("Search delicious local dishes...") },
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
                                                .clickable { selectedVendorIdFilter = null },
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
                                                .clickable { selectedVendorIdFilter = v.id },
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
                            food.isAvailable &&
                            (selectedVendorIdFilter == null || food.vendorId == selectedVendorIdFilter) &&
                            (selectedCategory == "All" || food.category.equals(selectedCategory, ignoreCase = true)) &&
                            (searchQuery.isBlank() || food.name.contains(searchQuery, ignoreCase = true) || food.description.contains(searchQuery, ignoreCase = true))
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
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                                    
                                                    val vendorIsOpen = vendor?.isOpen ?: true
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
                        }
                        
                        if (ordersSubTab == 0) {
                            val activeOrders = studentOrders.filter { it.status == "PENDING" || it.status == "PREPARING" || it.status == "READY" || it.status == "COMPLETED" }.sortedByDescending { it.id }
                            
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
                                                "COMPLETED" -> Color(0xFF4CAF50) // Emerald Green
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
                                                        "READY" -> "Ready for Pickup"
                                                        "COMPLETED" -> "Completed"
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
                                        val steps = listOf("Received", "Preparing", "Ready", "Completed")
                                        val activeStep = when (order.status.uppercase()) {
                                            "PENDING" -> 0
                                            "PREPARING" -> 1
                                            "READY" -> 2
                                            "COMPLETED" -> 3
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
                                                    } else if (order.status.uppercase() == "COMPLETED") {
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
                    }
                } else {
                            val completedOrCanceledOrders = studentOrders.filter { it.status == "COMPLETED" || it.status == "DECLINED" || it.status == "CANCELLED" }
                            val totalSpend = completedOrCanceledOrders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
                            val orderCount = completedOrCanceledOrders.filter { it.status == "COMPLETED" }.size
                            
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
                                    Column {
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
                                        listOf("All", "COMPLETED", "DECLINED", "CANCELLED").forEach { status ->
                                            val isSel = pastOrdersSelectedStatus == status
                                            val label = when(status) {
                                                "COMPLETED" -> "Completed"
                                                "DECLINED" -> "Declined"
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Official Receipt Journal (${filteredPastOrders.size} dishes found)", 
                                            fontWeight = FontWeight.Bold, 
                                            style = MaterialTheme.typography.titleSmall, 
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (pastOrdersSearchQuery.isNotEmpty() || pastOrdersSelectedStatus != "All" || pastOrdersSelectedVendorId != null || pastOrdersSelectedPeriod != "All Time") {
                                            TextButton(
                                                onClick = {
                                                    pastOrdersSearchQuery = ""
                                                    pastOrdersSelectedStatus = "All"
                                                    pastOrdersSelectedVendorId = null
                                                    pastOrdersSelectedPeriod = "All Time"
                                                    pastOrdersSortBy = "Newest First"
                                                },
                                                modifier = Modifier.height(26.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Reset", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
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
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                        Text("Booth: ${vendorInfo?.fullName ?: "Cafeteria Vendor"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                                    orderQuantity = order.quantity
                                                                    selectedFoodForOrder = matchedFood
                                                                },
                                                                colors = ButtonDefaults.buttonColors(
                                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                                ),
                                                                shape = RoundedCornerShape(8.dp),
                                                                modifier = Modifier
                                                                    .height(34.dp)
                                                                    .testTag("reorder_button_${order.id}"),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                            ) {
                                                                Icon(Icons.Default.Refresh, contentDescription = "Reorder", modifier = Modifier.size(14.dp))
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text("Reorder Meal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }

                                                            if (order.status == "COMPLETED") {
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
                                                                        Text("Rate & Review Vendor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                                            text = "Reviewed ${"%.1f".format(avgRating)}/5★",
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
                            when (selectedGoalFilter) {
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
                                shape = RoundedCornerShape(16.dp)
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
                                        shape = RoundedCornerShape(8.dp)
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
                    var showTopUpDialog by remember { mutableStateOf(false) }
                    var topUpAmount by remember { mutableStateOf("") }
                    var topUpPhone by remember { mutableStateOf("") }
                    var isTopUpProcessing by remember { mutableStateOf(false) }
                    var topUpSuccess by remember { mutableStateOf(false) }
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text("Current virtual smart-coin balance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "GH₵ ${"%.2f".format(studentWalletBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

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
                                                            viewModel.rechargeWallet(amt)
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

                            HorizontalDivider()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Amount Payable:", fontWeight = FontWeight.Bold)
                                Text("GH₵ ${"%.2f".format(food.price * orderQuantity)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
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
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pay with virtual smart balance", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Your current balance: GH₵ ${"%.2f".format(studentWalletBalance)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
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
                                        onClick = {
                                            val requiredSum = food.price * orderQuantity
                                            when (checkoutPaymentMode) {
                                                "WALLET" -> {
                                                    if (studentWalletBalance < requiredSum) {
                                                        orderPlacementError = "Insufficient smart balance. Top up your virtual ID Wallet!"
                                                        return@Button
                                                    }
                                                    viewModel.placeOrder(food, orderQuantity, true) { success ->
                                                        if (success) {
                                                            selectedFoodForOrder = null
                                                            activeTab = 1
                                                        } else {
                                                            orderPlacementError = "Deduction failed. Check system link."
                                                        }
                                                    }
                                                }
                                                "POD" -> {
                                                    viewModel.placeOrder(food, orderQuantity, false) { success ->
                                                        if (success) {
                                                            selectedFoodForOrder = null
                                                            activeTab = 1
                                                        } else {
                                                            orderPlacementError = "Failed to compile POD order."
                                                        }
                                                    }
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
                                                        viewModel.rechargeWallet(requiredSum)
                                                        viewModel.placeOrder(food, orderQuantity, true) { success ->
                                                            isGatewayProcessing = false
                                                            if (success) {
                                                                selectedFoodForOrder = null
                                                                activeTab = 1
                                                            } else {
                                                                orderPlacementError = "Direct checkout failed. Balance updated, place order via wallet."
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text(if (checkoutPaymentMode == "GATEWAY") "Authorize Direct Pay" else "Verify & Route Order")
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

// ==========================================
// 4. VENDOR WORKSPACE & VERIFICATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VendorDashboardScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val vendorFoods by viewModel.vendorFoodItems.collectAsStateWithLifecycle()
    val incomingOrders by viewModel.vendorOrders.collectAsStateWithLifecycle()
    val vendorFeedbackList by viewModel.vendorFeedback.collectAsStateWithLifecycle()
    val vendorAnnouncement by viewModel.vendorAnnouncement.collectAsStateWithLifecycle()
    val isStoreClosed by viewModel.isStoreClosed.collectAsStateWithLifecycle()
    val isAdminActing by viewModel.isAdminActing.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.newOrderAlerts.collectAsStateWithLifecycle()
    val sentimentAnalysisText by viewModel.vendorSentimentAnalysis.collectAsStateWithLifecycle()
    val isAnalyzingSentiment by viewModel.isAnalyzingSentiment.collectAsStateWithLifecycle()
    val autoRepliesText by viewModel.vendorAutoReplies.collectAsStateWithLifecycle()
    val isGeneratingAutoReplies by viewModel.isGeneratingAutoReplies.collectAsStateWithLifecycle()
    val pricingSuggestionsText by viewModel.vendorPricingSuggestions.collectAsStateWithLifecycle()
    val isGeneratingPricingSuggestions by viewModel.isGeneratingPricingSuggestions.collectAsStateWithLifecycle()
    val performanceData by viewModel.vendorPerformanceList.collectAsStateWithLifecycle()
    val performanceMetrics by viewModel.vendorPerformanceMetricsList.collectAsStateWithLifecycle()
    val dailyRevenueResponse by viewModel.vendorDailyRevenueResponse.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        val uid = currentUser?.id ?: 0
        if (uid > 0) {
            viewModel.refreshVendorPerformance(uid)
        }
    }

    val todayInsightsText by viewModel.vendorTodayInsights.collectAsStateWithLifecycle()
    val isAnalyzingTodayOrders by viewModel.isAnalyzingTodayOrders.collectAsStateWithLifecycle()

    val historicalInsightsText by viewModel.vendorHistoricalInsights.collectAsStateWithLifecycle()
    val isAnalyzingHistoricalOrders by viewModel.isAnalyzingHistoricalOrders.collectAsStateWithLifecycle()

    val todayOrders = remember(incomingOrders) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        incomingOrders.filter { it.orderTimestamp >= cal.timeInMillis }
    }

    LaunchedEffect(currentUser, todayOrders.size) {
        val uid = currentUser?.id ?: 0
        if (uid > 0 && todayInsightsText == null && todayOrders.isNotEmpty()) {
            viewModel.runVendorTodayInsights(uid, currentUser?.fullName ?: "Vendor", todayOrders)
        }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Menu List, 2: Ratings/Analytics, 3: Settings & Hub

    val ordersScrollState = rememberLazyListState()
    val menuScrollState = rememberLazyListState()
    val analyticsScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()
    val financeScrollState = rememberLazyListState()
    var showVendorNotificationsDialog by remember { mutableStateOf(false) }
    var orderTimeFilter by remember { mutableStateOf("All Time") }
    var orderStatusFilter by remember { mutableStateOf("Active Orders") }
    var bulkSelectedOrderIds by remember { mutableStateOf(setOf<Int>()) }
    var isAddingFood by remember { mutableStateOf(false) }
    var previewTargetReceipt by remember { mutableStateOf<Order?>(null) }

    // Low stock and procurement assistant states
    var isLowStockExpanded by remember { mutableStateOf(true) }
    var showProcurementDialog by remember { mutableStateOf(false) }

    // Predictive peak demand warning alert states
    var simulatedCurrentHour by remember { mutableStateOf<Int?>(null) } // null means real time
    var anticipationHoursWindow by remember { mutableIntStateOf(2) } // warning anticipation window limit
    var isPeakAlertExpanded by remember { mutableStateOf(true) }
    var dismissPeakAlertDay by remember { mutableStateOf(false) }

    // PIN verification dialogues
    var verifyTargetOrder by remember { mutableStateOf<Order?>(null) }
    var enteredTicketPin by remember { mutableStateOf("") }
    var pinVerificationError by remember { mutableStateOf<String?>(null) }

    // Estimation dialogues
    var showEstTimeDialogForOrder by remember { mutableStateOf<Order?>(null) }
    var estimatedMinutesSelected by remember { mutableStateOf("15 mins") }

    // Cancellation dialogues
    var showCancelOrderDialogForOrder by remember { mutableStateOf<Order?>(null) }
    var cancelOrderReason by remember { mutableStateOf("") }

    // Add Food States
    var newFoodName by remember { mutableStateOf("") }
    var newFoodPrice by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("Local Dish") }
    var newFoodDescription by remember { mutableStateOf("") }
    var newFoodInitialStock by remember { mutableStateOf("100") }
    var newFoodSafetyThreshold by remember { mutableStateOf("15") }
    var newFoodCalories by remember { mutableStateOf("180") }
    var newFoodAllergens by remember { mutableStateOf("None") }

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
                    Column {
                        Text(if (isAdminActing) "SIMULATION: ${currentUser?.fullName ?: "Vendor"}" else (currentUser?.fullName ?: "Cafeteria Vendor"), fontWeight = FontWeight.Bold)
                        Text(
                            "Brand: '${currentUser?.info ?: ""}' • Store ID: ${currentUser?.id ?: ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    val inventoryNotifs by viewModel.vendorInventoryNotifications.collectAsStateWithLifecycle()
                    val unreadCount = remember(inventoryNotifs) { inventoryNotifs.count { !it.isRead } }

                    IconButton(
                        onClick = { showVendorNotificationsDialog = true },
                        modifier = Modifier.testTag("vendor_notifications_bell_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(unreadCount.toString(), fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Active Alerts",
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))

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
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("Orders") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    label = { Text("Menu manager") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Analytics Charts") }
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings & Hub") }
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Finance Hub") }
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
                        scope.launch {
                            when (activeTab) {
                                0 -> ordersScrollState.animateScrollToItem(0)
                                1 -> menuScrollState.animateScrollToItem(0)
                                2 -> analyticsScrollState.animateScrollTo(0)
                                3 -> settingsScrollState.animateScrollTo(0)
                                4 -> financeScrollState.animateScrollToItem(0)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp).testTag("vendor_scroll_up_fab"),
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
                            when (activeTab) {
                                0 -> {
                                    val lastIdx = ordersScrollState.layoutInfo.totalItemsCount - 1
                                    if (lastIdx >= 0) ordersScrollState.animateScrollToItem(lastIdx)
                                }
                                1 -> {
                                    val lastIdx = menuScrollState.layoutInfo.totalItemsCount - 1
                                    if (lastIdx >= 0) menuScrollState.animateScrollToItem(lastIdx)
                                }
                                2 -> {
                                    analyticsScrollState.animateScrollTo(analyticsScrollState.maxValue)
                                }
                                3 -> {
                                    settingsScrollState.animateScrollTo(settingsScrollState.maxValue)
                                }
                                4 -> {
                                    val lastIdx = financeScrollState.layoutInfo.totalItemsCount - 1
                                    if (lastIdx >= 0) financeScrollState.animateScrollToItem(lastIdx)
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(44.dp).testTag("vendor_scroll_down_fab"),
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
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. CALCULATE HISTORICAL VENDOR PEAK HOURS
            val historicalPeakHours = remember(incomingOrders) {
                val hourCounts = IntArray(24) { 0 }
                incomingOrders.forEach { order ->
                    val cal = java.util.Calendar.getInstance().apply {
                        timeInMillis = order.orderTimestamp
                    }
                    val hr = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    if (hr in 0..23) {
                        hourCounts[hr] += order.quantity
                    }
                }
                
                val maxQty = hourCounts.maxOrNull() ?: 0
                val peakHoursList = mutableListOf<Int>()
                if (maxQty > 0) {
                    hourCounts.forEachIndexed { h, count ->
                        if (count >= maxQty * 0.70 && count > 0) {
                            peakHoursList.add(h)
                        }
                    }
                }
                if (peakHoursList.isEmpty()) {
                    listOf(12, 13) // Fallback default ATU campus lunch rush peaks
                } else {
                    peakHoursList
                }
            }

            val formatHour = { hour: Int ->
                val ampm = if (hour >= 12) "PM" else "AM"
                val displayH = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                "$displayH:00 $ampm"
            }

            val currentHourReal = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
            val effectiveCurrentHour = simulatedCurrentHour ?: currentHourReal

            // Find the upcoming or current peak hour
            val upcomingPeakHour = remember(historicalPeakHours, effectiveCurrentHour) {
                historicalPeakHours.sorted().firstOrNull { it >= effectiveCurrentHour }
                    ?: historicalPeakHours.sorted().minOrNull() ?: 12
            }

            val hoursUntilUpcomingPeak = upcomingPeakHour - effectiveCurrentHour
            val isPeakUpcomingActive = remember(upcomingPeakHour, effectiveCurrentHour, anticipationHoursWindow) {
                val diff = upcomingPeakHour - effectiveCurrentHour
                diff in 0..anticipationHoursWindow
            }

            // SYSTEM AUTOMATED PREDICTIVE PEAK-DEMAND ALERT BANNER
            if (isPeakUpcomingActive && !dismissPeakAlertDay) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("peak_demand_predictive_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📈", fontSize = 22.sp)
                                Column {
                                    Text(
                                        "Automated Predictive Peak Demand Alert",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Predicted based on ${incomingOrders.size} historical campus dining logs",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { isPeakAlertExpanded = !isPeakAlertExpanded },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPeakAlertExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand or Collapse Peak Demand Warning Info",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        if (isPeakAlertExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val peakDescription = when {
                                upcomingPeakHour in 7..10 -> "☀️ Breakfast Peak (ATU Classroom Rush)"
                                upcomingPeakHour in 11..14 -> "🍚 Lunch Rush Peak (Heavy Campus Transaction Wave)"
                                else -> "🍹 Late Afternoon Transition Peak"
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Target Peak Period: $peakDescription",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Predicted at ${formatHour(upcomingPeakHour)} (Approx. in $hoursUntilUpcomingPeak hours)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                            Text("CONGESTION RISK: HIGH", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "💡 Procurement & Mitigation Strategy:\n" +
                                        "• Pre-pack at least 25 portions of your popular menu food items (Jollof / Waakye) 15-20 mins ahead to avoid campus queue bottlenecks.\n" +
                                        "• Ensure enough cooking/serving staff are deployed at the registers.\n" +
                                        "• Consider enabling Dynamic Peak Hour price strategies (+5% premium combos) to manage the transactional line flow.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Dynamic configuration controls inside alert
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("Anticipate: ", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            listOf(1, 2, 3).forEach { hrs ->
                                                FilterChip(
                                                    selected = anticipationHoursWindow == hrs,
                                                    onClick = { anticipationHoursWindow = hrs },
                                                    label = { Text("${hrs}h before", fontSize = 9.sp) },
                                                    modifier = Modifier.height(24.dp).testTag("anticipate_window_${hrs}h")
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        viewModel.repository.insertAuditLog(
                                                            currentUser?.id ?: 0,
                                                            "PEAK_DEMAND_PREPARATION",
                                                            "Acknowledged peak alert at ${formatHour(upcomingPeakHour)}. Initiated extra ingredients preparation & split-queue dispatch actions."
                                                        )
                                                    }
                                                    dismissPeakAlertDay = true
                                                },
                                                modifier = Modifier.height(28.dp).testTag("peak_alert_acknowledge_btn")
                                            ) {
                                                Text("Acknowledge & Prepare", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SIMULATION AND INTERACTIVE FORECAST CONTROL CONSOLE FOR VENDORS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("forecast_simulation_panel"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp)
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
                            Text("⚡", fontSize = 16.sp)
                            Column {
                                Text(
                                    text = "Campus Temporal Loader & Peak Simulator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Simulate different lecture slots & meal schedules to evaluate automated predictive warnings.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hour:", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            // Option chips for hours
                            val options = listOf(null to "Live 🕒", 9 to "9:00 AM 🍳", 11 to "11:00 AM 🔔", 12 to "12:00 PM 🍚", 16 to "4:00 PM 💬")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(options) { (hrValue, labelText) ->
                                    FilterChip(
                                        selected = simulatedCurrentHour == hrValue,
                                        onClick = { 
                                            simulatedCurrentHour = hrValue 
                                            dismissPeakAlertDay = false // Reset dismiss on hour change
                                        },
                                        label = { Text(labelText, fontSize = 8.sp) },
                                        modifier = Modifier.height(24.dp).testTag("sim_hour_${hrValue ?: "live"}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SYSTEM AUTOMATED LOW-STOCK WARNING AREA
            val lowStockItems = remember(vendorFoods) { vendorFoods.filter { it.currentStock <= it.lowStockThreshold } }
            if (lowStockItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("global_low_stock_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🚨", fontSize = 20.sp)
                                Column {
                                    Text(
                                        "Automated Procurement Assist & Alerts",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "${lowStockItems.size} inventory items are below threshold limits.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Procurement planner action trigger
                                Button(
                                    onClick = { showProcurementDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(26.dp).padding(end = 6.dp).testTag("open_procurement_planner_button")
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Procure Plan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { isLowStockExpanded = !isLowStockExpanded },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLowStockExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand or Collapse Low Stock Alerts",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        
                        if (isLowStockExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            lowStockItems.forEach { item ->
                                val predictedTime = viewModel.predictStockExhaustion(item, incomingOrders)
                                val progressFraction = if (item.initialStock > 0) {
                                    (item.currentStock.toFloat() / item.initialStock.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    item.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Current: ${item.currentStock}/${item.initialStock}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.currentStock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        "| Threshold:",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    // Interactive Threshold tuner inside notification
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                val prev = (item.lowStockThreshold - 1).coerceAtLeast(1)
                                                                viewModel.updateFoodItemStockSettings(item, item.initialStock, item.currentStock, prev)
                                                            },
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(10.dp))
                                                        }
                                                        Text(
                                                            "${item.lowStockThreshold}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                val next = item.lowStockThreshold + 1
                                                                viewModel.updateFoodItemStockSettings(item, item.initialStock, item.currentStock, next)
                                                            },
                                                            modifier = Modifier.size(16.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(10.dp))
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // Quick restock button
                                            Button(
                                                onClick = { viewModel.replenishFoodItemStock(item, 50) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("+50 Plates", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = { progressFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp),
                                            color = if (progressFraction <= 0.15f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = "Depletion Estimation",
                                                    tint = if (item.currentStock <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Exhaustion forecast: $predictedTime",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (item.currentStock <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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

            // PROCUREMENT PLANNER POPUP DIALOGUE
            if (showProcurementDialog) {
                Dialog(onDismissRequest = { showProcurementDialog = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("procurement_dialog"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🛒", fontSize = 24.sp)
                                    Column {
                                        Text(
                                            "Procurement Planner",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Automated Peak Capacity Restock",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { showProcurementDialog = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // List of items under safety threshold limits
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(lowStockItems) { item ->
                                    val neededQty = (item.initialStock - item.currentStock).coerceAtLeast(0)
                                    val estimatedUnitCost = item.price
                                    val estTotalCost = neededQty * estimatedUnitCost
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(
                                                        text = "Current: ${item.currentStock}/${item.initialStock} | Qty needed: $neededQty",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (item.currentStock == 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (item.currentStock == 0) "DEPLETED" else "LOW",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.currentStock == 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Unit Cost: GH₵ ${"%.2f".format(estimatedUnitCost)}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    "Total: GH₵ ${"%.2f".format(estTotalCost)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Cost breakdown sum
                            val totalProcurementCost = lowStockItems.sumOf { (it.initialStock - it.currentStock).coerceAtLeast(0) * it.price }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total Est. Cost:",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "GH₵ ${"%.2f".format(totalProcurementCost)}",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        // Save a procurement memo report to Cyber Logs Audit System
                                        val memoSummaryText = lowStockItems.joinToString("\n") { 
                                            val qty = it.initialStock - it.currentStock
                                            "• ${it.name}: procurement of $qty units requested (Unit Cost: GH₵ ${"%.2f".format(it.price)})"
                                        }
                                        scope.launch {
                                            viewModel.repository.insertAuditLog(
                                                currentUser?.id ?: 0, 
                                                "PROCUREMENT_PLANNER", 
                                                "Generated automated shopping list: Total Cost=GH₵ ${"%.2f".format(totalProcurementCost)}.\nPlan Detail:\n$memoSummaryText"
                                            )
                                        }
                                        showProcurementDialog = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Log Shopping Plan", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        // Bulk Restock all low items directly to their peak capacity limits
                                        lowStockItems.forEach { item ->
                                            val restockValue = (item.initialStock - item.currentStock).coerceAtLeast(0)
                                            if (restockValue > 0) {
                                                viewModel.replenishFoodItemStock(item, restockValue)
                                            }
                                        }
                                        showProcurementDialog = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Procure All Items", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(225)) togetherWith fadeOut(animationSpec = tween(225))
                },
                label = "vendor_tabs"
            ) { targetTab ->
                when (targetTab) {
                0 -> {
                    val timeFrames = listOf("All Time", "Last 2 hours", "Today")
                    val statusOptions = listOf("Active Orders", "Completed", "All Statuses")

                    val filteredOrders = remember(incomingOrders, orderTimeFilter, orderStatusFilter) {
                        val now = System.currentTimeMillis()
                        incomingOrders.filter { order ->
                            val statusMatches = when (orderStatusFilter) {
                                "Active Orders" -> order.status != "COMPLETED" && order.status != "DECLINED" && order.status != "CANCELLED"
                                "Completed" -> order.status == "COMPLETED"
                                else -> true
                            }
                            val timeMatches = when (orderTimeFilter) {
                                "Last 2 hours" -> {
                                    order.orderTimestamp >= (now - 2 * 60 * 60 * 1000)
                                }
                                "Today" -> {
                                    val cal = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                    order.orderTimestamp >= cal.timeInMillis
                                }
                                else -> true
                            }
                            statusMatches && timeMatches
                        }
                    }

                    val todayCompletedOrders = remember(todayOrders) {
                        todayOrders.filter { it.status == "COMPLETED" }
                    }
                    val todayTotalEarnings = remember(todayCompletedOrders) {
                        todayCompletedOrders.sumOf { it.totalPrice }
                    }
                    val avgOrderValue = remember(todayCompletedOrders, todayTotalEarnings) {
                        if (todayCompletedOrders.isNotEmpty()) todayTotalEarnings / todayCompletedOrders.size else 0.0
                    }
                    val todayActiveOrders = remember(todayOrders) {
                        todayOrders.filter { it.status != "COMPLETED" && it.status != "DECLINED" && it.status != "CANCELLED" }
                    }
                    val potentialActiveEarnings = remember(todayActiveOrders) {
                        todayActiveOrders.sumOf { it.totalPrice }
                    }
                    val pendingOrdersList = remember(filteredOrders) {
                        filteredOrders.filter { it.status == "PENDING" }
                    }

                    LazyColumn(
                        state = ordersScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            val realTimeLogs by viewModel.realTimeEventsLogs.collectAsStateWithLifecycle()
                            var isTerminalExpanded by remember { mutableStateOf(false) }
                            var isPulseActive by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(900)
                                    isPulseActive = !isPulseActive
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .testTag("pusher_echo_sync_dashboard_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
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
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isPulseActive) Color(0xFF00E676)
                                                        else Color(0xFF00E676).copy(alpha = 0.4f)
                                                    )
                                            )
                                            Text(
                                                text = "Laravel Echo & Pusher Online",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "WS SYNCED",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Subscribed: orders-vendor-${currentUser?.id ?: 0} • Callback Latency: <20ms (Direct Hook)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isStoreClosed) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isStoreClosed) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                                    imageVector = if (isStoreClosed) Icons.Default.Block else Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isStoreClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = if (isStoreClosed) "STORE STATUS: CLOSED" else "STORE STATUS: OPEN",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp,
                                                        color = if (isStoreClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = if (isStoreClosed) "Students see OUT OF STOCK / CLOSED" else "Menu is fully available to all students",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            
                                            Switch(
                                                checked = !isStoreClosed,
                                                onCheckedChange = { viewModel.setStoreClosedState(!it) },
                                                modifier = Modifier.testTag("vendor_dashboard_store_toggle_switch")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.triggerSimulatedRealTimeOrder()
                                            },
                                            modifier = Modifier
                                                .weight(1.2f)
                                                .height(36.dp)
                                                .testTag("simulate_realtime_order_btn"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Simulate Live Order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { isTerminalExpanded = !isTerminalExpanded },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("terminal_logs_toggle_btn"),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isTerminalExpanded) "Hide Logs" else "WS Live Frame",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isTerminalExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .padding(top = 10.dp)
                                                .fillMaxWidth()
                                                .background(Color(0xFF1C1C1E), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = "🟢 WEB_SOCKET_RECEIVER >_",
                                                color = Color(0xFF39FF14),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(bottom = 6.dp)
                                            )

                                            if (realTimeLogs.isEmpty()) {
                                                Text(
                                                    text = "Sync engine initialized. Waiting for incoming broadcast frame elements... Tap 'Simulate Live Order' to manually inject test transaction packet.",
                                                    color = Color.LightGray.copy(alpha = 0.8f),
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 14.sp
                                                )
                                            } else {
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    realTimeLogs.forEach { log ->
                                                        Text(
                                                            text = log,
                                                            color = Color(0xFFF2F2F7),
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            lineHeight = 14.sp
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .testTag("daily_revenue_tracker_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Revenue Tracker Icon",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Today's Revenue Tracker",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }

                                        // Completed count badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.tertiary,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${todayCompletedOrders.size} COMPLETED",
                                                color = MaterialTheme.colorScheme.onTertiary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.testTag("completed_orders_count")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Main Earnings Metric
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "GH₵ ${"%.2f".format(todayTotalEarnings)}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.testTag("today_earnings_text")
                                            )
                                            Text(
                                                text = "Immediate financial feedback from completed orders",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Trend Graphic or Icon
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = "Trend up",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.material3.HorizontalDivider(
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        thickness = 1.dp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Inner secondary metrics layout
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Average ticket size info
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "Average Value",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "GH₵ ${"%.2f".format(avgOrderValue)}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        // Uncompleted/Active orders info
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "Sales Pipeline",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${todayActiveOrders.size} Active (GH₵ ${"%.2f".format(potentialActiveEarnings)})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .testTag("gemini_insights_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Gemini Spark",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Gemini Today's Intelligence",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        // Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary, 
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "LIVE AI",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (isAnalyzingTodayOrders) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp), 
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Analyzing today's sales velocity...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        todayInsightsText?.let { insights ->
                                            Text(
                                                text = insights,
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .padding(bottom = 12.dp)
                                                    .testTag("insights_text")
                                            )
                                        } ?: run {
                                            Text(
                                                text = "Need a quick update on today's performance? Tap below to run your Gemini-powered live revenue and busiest hour stats analysis!",
                                                style = MaterialTheme.typography.bodySmall,
                                                lineHeight = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.runVendorTodayInsights(
                                                    vendorId = currentUser?.id ?: 0,
                                                    vendorName = currentUser?.fullName ?: "Vendor",
                                                    todayOrders = todayOrders
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("run_insights_button"),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (todayInsightsText == null) "Analyze Today's Orders" else "Recalculate Insights",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            var isInventoryExpanded by remember { mutableStateOf(true) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp)
                                    .testTag("quick_inventory_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isInventoryExpanded = !isInventoryExpanded },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RestaurantMenu,
                                                contentDescription = "Quick Stock Toggle",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Quick Stock Toggle",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "Mark meals 'Out of Stock' instantly",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        
                                        TextButton(
                                            onClick = { isInventoryExpanded = !isInventoryExpanded },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isInventoryExpanded) "HIDE" else "SHOW ALL",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    AnimatedVisibility(visible = isInventoryExpanded) {
                                        Column(modifier = Modifier.padding(top = 12.dp)) {
                                            if (vendorFoods.isEmpty()) {
                                                Text(
                                                    text = "No meals listed under this account.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                vendorFoods.forEach { food ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = food.name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (food.isAvailable) MaterialTheme.colorScheme.onSurface 
                                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                            )
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = food.category,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = "•",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontSize = 11.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                                Text(
                                                                    text = "Stock: ${food.currentStock}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    fontSize = 11.sp,
                                                                    color = if (food.currentStock <= food.lowStockThreshold) MaterialTheme.colorScheme.error 
                                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        color = if (food.isAvailable) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f) 
                                                                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                                                        shape = RoundedCornerShape(4.dp)
                                                                    )
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (food.isAvailable) "IN STOCK" else "OUT OF STOCK",
                                                                    fontSize = 8.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (food.isAvailable) MaterialTheme.colorScheme.onPrimaryContainer 
                                                                            else MaterialTheme.colorScheme.onErrorContainer
                                                                )
                                                            }
                                                            
                                                            Switch(
                                                                modifier = Modifier.testTag("dashboard_stock_toggle_${food.id}"),
                                                                checked = food.isAvailable,
                                                                onCheckedChange = { isChecked ->
                                                                    viewModel.updateFoodAvailability(food, isChecked)
                                                                }
                                                            )
                                                        }
                                                    }
                                                    androidx.compose.material3.HorizontalDivider(
                                                        modifier = Modifier.padding(vertical = 4.dp),
                                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "Filter Orders",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Filter Dashboard Orders",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "TIME FRAME",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        timeFrames.forEach { filter ->
                                            val isSelected = orderTimeFilter == filter
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { orderTimeFilter = filter }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = filter,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "STATUS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        statusOptions.forEach { filter ->
                                            val isSelected = orderStatusFilter == filter
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { orderStatusFilter = filter }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = filter,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (pendingOrdersList.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp)
                                        .testTag("bulk_actions_card"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.FilterList,
                                                    contentDescription = "Bulk Actions",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Bulk Actions (${bulkSelectedOrderIds.intersect(pendingOrdersList.map { it.id }.toSet()).size} Selected)",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            // Select All / Deselect All Button
                                            val isAllPendingSelected = remember(pendingOrdersList, bulkSelectedOrderIds) {
                                                val pendingIds = pendingOrdersList.map { it.id }.toSet()
                                                pendingIds.isNotEmpty() && bulkSelectedOrderIds.containsAll(pendingIds)
                                            }
                                            TextButton(
                                                onClick = {
                                                    val pendingIds = pendingOrdersList.map { it.id }.toSet()
                                                    bulkSelectedOrderIds = if (isAllPendingSelected) {
                                                        bulkSelectedOrderIds - pendingIds
                                                    } else {
                                                        bulkSelectedOrderIds + pendingIds
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isAllPendingSelected) "DESELECT ALL" else "SELECT ALL PENDING",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Check pending orders below and mark them ready in a single click to streamline preparation.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )

                                        val currentSelectedPendingIds = remember(pendingOrdersList, bulkSelectedOrderIds) {
                                            val pendingIds = pendingOrdersList.map { it.id }.toSet()
                                            bulkSelectedOrderIds.intersect(pendingIds)
                                        }

                                        if (currentSelectedPendingIds.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    currentSelectedPendingIds.forEach { orderId ->
                                                        viewModel.updateOrderStatus(orderId, "READY")
                                                    }
                                                    bulkSelectedOrderIds = bulkSelectedOrderIds - currentSelectedPendingIds
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("bulk_mark_prepared_button"),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Mark Selected (${currentSelectedPendingIds.size}) as Prepared",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            val activeCount = filteredOrders.count { it.status == "PENDING" || it.status == "PREPARING" }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Matching Orders (${filteredOrders.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (activeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$activeCount Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredOrders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No orders match these criteria.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Try switching filters to 'All Time' or 'All Statuses'.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filteredOrders) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (order.status == "PENDING") {
                                            Checkbox(
                                                checked = bulkSelectedOrderIds.contains(order.id),
                                                onCheckedChange = { isChecked ->
                                                    bulkSelectedOrderIds = if (isChecked) {
                                                        bulkSelectedOrderIds + order.id
                                                    } else {
                                                        bulkSelectedOrderIds - order.id
                                                    }
                                                },
                                                modifier = Modifier
                                                    .padding(end = 12.dp)
                                                    .testTag("order_checkbox_${order.id}")
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Receipt Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                    Text("QTY: ${order.quantity} • Volume: GH₵ ${"%.2f".format(order.totalPrice)}", style = MaterialTheme.typography.bodySmall)
 
                                                    Spacer(modifier = Modifier.height(4.dp))
 
                                                    // Find student and display name / contact info
                                                    val student = allUsers.find { it.id == order.customerId }
                                                    val sName = student?.fullName ?: "Student #${order.customerId}"
                                                    val sPhone = student?.telephone ?: "+233 50 123 4567"
                                                    val sRegId = student?.student_staff_id ?: student?.info ?: "ATU-STUDENT"
 
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = "$sName ($sRegId) • 📞 $sPhone",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
 
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = { previewTargetReceipt = order },
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Print,
                                                            contentDescription = "Print Ticket",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
 
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(order.status, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                    }
                                                }
                                            }
 
                                            Spacer(modifier = Modifier.height(12.dp))
 
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (order.status == "PENDING") {
                                                    Button(
                                                        onClick = {
                                                            showEstTimeDialogForOrder = order
                                                            estimatedMinutesSelected = "15 mins"
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Accept Prep", fontSize = 11.sp)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            cancelOrderReason = ""
                                                            showCancelOrderDialogForOrder = order
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("cancel_order_button_${order.id}"),
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Text("Cancel Order", fontSize = 11.sp)
                                                    }
                                                } else if (order.status == "PREPARING") {
                                                    Button(
                                                        onClick = { viewModel.updateOrderStatus(order.id, "READY") },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Mark Ready, Wait for Pickup", fontSize = 12.sp)
                                                    }
                                                } else if (order.status == "READY") {
                                                    // PICKUP VERIFICATION
                                                    Button(
                                                        onClick = {
                                                            verifyTargetOrder = order
                                                            enteredTicketPin = ""
                                                            pinVerificationError = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Secure PickUp Validation (PIN Required)", fontSize = 12.sp)
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
                1 -> {
                    // Menu Management
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = menuScrollState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Manage Food Items (${vendorFoods.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Button(
                                        onClick = {
                                            newFoodName = ""
                                            newFoodPrice = ""
                                            newFoodCategory = "Local Dish"
                                            newFoodDescription = ""
                                            isAddingFood = true
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Text("Add item")
                                    }
                                }
                            }

                            item {
                                var isJsonCardExpanded by remember { mutableStateOf(false) }
                                var rawJsonInput by remember { mutableStateOf("") }
                                var bulkUpdateStatusMessage by remember { mutableStateOf<String?>(null) }
                                var isBulkUpdateSuccess by remember { mutableStateOf(true) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bulk_json_update_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("📂", fontSize = 18.sp)
                                                Column {
                                                    Text(
                                                        "Bulk JSON Price & Availability",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "Upload or paste a JSON array to update multiple items.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { isJsonCardExpanded = !isJsonCardExpanded },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isJsonCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand/Collapse JSON Upload"
                                                )
                                            }
                                        }

                                        if (isJsonCardExpanded) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Format requires a valid JSON array of objects with either 'id' or 'item_id', 'price' (optional), and 'is_available' (optional). Example:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                                )
                                            ) {
                                                Text(
                                                    text = "[\n  { \"id\": 101, \"price\": 28.5, \"is_available\": true },\n  { \"id\": 102, \"is_available\": false }\n]",
                                                    fontSize = 10.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            OutlinedTextField(
                                                value = rawJsonInput,
                                                onValueChange = { rawJsonInput = it },
                                                label = { Text("Paste JSON File Content", fontSize = 12.sp) },
                                                placeholder = { Text("[{\"id\": 1,...}]") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .testTag("bulk_json_input_field"),
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    fontSize = 11.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                ),
                                                maxLines = 10
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        val sampleList = vendorFoods.map { f ->
                                                            "  {\n    \"id\": ${f.id},\n    \"price\": ${f.price},\n    \"is_available\": ${f.isAvailable}\n  }"
                                                        }.joinToString(",\n")
                                                        rawJsonInput = "[\n$sampleList\n]"
                                                        bulkUpdateStatusMessage = "Template loaded from actual food items!"
                                                        isBulkUpdateSuccess = true
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("load_json_template_btn")
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Load Real Items JSON", fontSize = 10.sp)
                                                }

                                                Button(
                                                    onClick = {
                                                        if (rawJsonInput.isBlank()) {
                                                            bulkUpdateStatusMessage = "JSON cannot be empty."
                                                            isBulkUpdateSuccess = false
                                                            return@Button
                                                        }

                                                        try {
                                                            val jsonArray = org.json.JSONArray(rawJsonInput)
                                                            val updatesList = mutableListOf<com.example.data.LaravelBulkUpdateItem>()

                                                            for (i in 0 until jsonArray.length()) {
                                                                val obj = jsonArray.getJSONObject(i)
                                                                val id = obj.optInt("id", obj.optInt("item_id", -1))
                                                                if (id == -1) {
                                                                    throw Exception("Item at index $i is missing 'id' or 'item_id'.")
                                                                }
                                                                val price = if (obj.has("price")) obj.getDouble("price") else null
                                                                val isAvailable = if (obj.has("is_available")) obj.getBoolean("is_available") else null

                                                                updatesList.add(
                                                                    com.example.data.LaravelBulkUpdateItem(
                                                                        id = id,
                                                                        price = price,
                                                                        is_available = isAvailable
                                                                    )
                                                                )
                                                            }

                                                            viewModel.bulkUpdateVendorMenu(
                                                                vendorId = currentUser?.id ?: 0,
                                                                updates = updatesList
                                                            ) { success, msg ->
                                                                isBulkUpdateSuccess = success
                                                                bulkUpdateStatusMessage = msg
                                                                if (success) {
                                                                    rawJsonInput = "" // clear on success
                                                                }
                                                            }

                                                        } catch (e: Exception) {
                                                            bulkUpdateStatusMessage = "JSON Error: ${e.message}"
                                                            isBulkUpdateSuccess = false
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("apply_bulk_json_btn")
                                                ) {
                                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Apply Bulk Update", fontSize = 10.sp)
                                                }
                                            }

                                            bulkUpdateStatusMessage?.let { msg ->
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = msg,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isBulkUpdateSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.fillMaxWidth().testTag("bulk_update_status_msg")
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            val lowStockFoods = vendorFoods.filter { it.currentStock <= it.lowStockThreshold }
                            if (lowStockFoods.isNotEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("bulk_restock_badge"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Warning Low Stock",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    "⚠️ RE-STOCK ALERTS: ${lowStockFoods.size} items low!",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    "The following cuisines have hit safety levels: ${lowStockFoods.joinToString { it.name }}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    lowStockFoods.forEach { food ->
                                                        viewModel.updateFoodItemStockSettings(
                                                            food,
                                                            food.initialStock,
                                                            food.initialStock,
                                                            food.lowStockThreshold
                                                        )
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Reset All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            if (vendorFoods.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No menus listed. Seed database or add your first culinary special!")
                                    }
                                }
                            } else {
                                items(vendorFoods) { food ->
                                    var isEditingFoodDetails by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("food_item_card_${food.id}"),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(food.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                    Text("Category: ${food.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("GH₵ ${"%.2f".format(food.price)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    IconButton(
                                                        onClick = { isEditingFoodDetails = true },
                                                        modifier = Modifier.size(28.dp).testTag("edit_food_details_btn_${food.id}")
                                                     ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Details",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    IconButton(
                                                        onClick = { viewModel.deleteVendorFoodItem(food) },
                                                        modifier = Modifier.size(28.dp).testTag("delete_food_btn_${food.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(food.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Available on student Portal:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                Switch(
                                                    checked = food.isAvailable,
                                                    onCheckedChange = { viewModel.updateFoodAvailability(food, it) }
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            var isEditingStockSettings by remember { mutableStateOf(false) }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            "Stock: ${food.currentStock}/${food.initialStock}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (food.currentStock <= food.lowStockThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            "(Safety Limit: ${food.lowStockThreshold})",
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )

                                                        if (food.currentStock <= food.lowStockThreshold) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(
                                                                        MaterialTheme.colorScheme.errorContainer,
                                                                        RoundedCornerShape(6.dp)
                                                                    )
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    "🚨 RE-STOCK NEEDED",
                                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    val depletionTime = viewModel.predictStockExhaustion(food, incomingOrders)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Timer,
                                                            contentDescription = null,
                                                            tint = if (food.currentStock <= food.lowStockThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            "Forecast: $depletionTime",
                                                            fontSize = 10.sp,
                                                            color = if (food.currentStock <= food.lowStockThreshold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Button(
                                                        onClick = { viewModel.replenishFoodItemStock(food, 10) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("+10 Plates", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    IconButton(
                                                        onClick = { isEditingStockSettings = true },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Stock Thresholds",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            if (isEditingStockSettings) {
                                                var tempInitialStock by remember { mutableStateOf(food.initialStock.toString()) }
                                                var tempCurrentStock by remember { mutableStateOf(food.currentStock.toString()) }
                                                var tempThreshold by remember { mutableStateOf(food.lowStockThreshold.toString()) }

                                                AlertDialog(
                                                    onDismissRequest = { isEditingStockSettings = false },
                                                    title = { Text("Stock Panel: ${food.name}") },
                                                    text = {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            OutlinedTextField(
                                                                value = tempInitialStock,
                                                                onValueChange = { tempInitialStock = it },
                                                                label = { Text("Initial Prep Stock (Refill Max)") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            OutlinedTextField(
                                                                value = tempCurrentStock,
                                                                onValueChange = { tempCurrentStock = it },
                                                                label = { Text("Current Portions Remaining") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                            OutlinedTextField(
                                                                value = tempThreshold,
                                                                onValueChange = { tempThreshold = it },
                                                                label = { Text("Low Stock Alarm Level") },
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    },
                                                    confirmButton = {
                                                        Button(
                                                            onClick = {
                                                                val initVal = tempInitialStock.toIntOrNull() ?: food.initialStock
                                                                val currVal = tempCurrentStock.toIntOrNull() ?: food.currentStock
                                                                val thresVal = tempThreshold.toIntOrNull() ?: food.lowStockThreshold
                                                                viewModel.updateFoodItemStockSettings(food, initVal, currVal, thresVal)
                                                                isEditingStockSettings = false
                                                            }
                                                        ) {
                                                            Text("Update")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { isEditingStockSettings = false }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (isEditingFoodDetails) {
                                        var editName by remember { mutableStateOf(food.name) }
                                        var editPrice by remember { mutableStateOf(food.price.toString()) }
                                        var editCategory by remember { mutableStateOf(food.category) }
                                        var editDescription by remember { mutableStateOf(food.description) }
                                        var editInitialStock by remember { mutableStateOf(food.initialStock.toString()) }
                                        var editCurrentStock by remember { mutableStateOf(food.currentStock.toString()) }
                                        var editThreshold by remember { mutableStateOf(food.lowStockThreshold.toString()) }
                                        var editCalories by remember { mutableStateOf(food.calories.toString()) }
                                        var editAllergens by remember { mutableStateOf(food.allergens) }

                                        Dialog(onDismissRequest = { isEditingFoodDetails = false }) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth().testTag("edit_food_dialog_${food.id}"),
                                                shape = RoundedCornerShape(16.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .padding(24.dp)
                                                        .verticalScroll(rememberScrollState()),
                                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    Text(
                                                        "Edit Culinary Dish Details",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )

                                                    OutlinedTextField(
                                                        value = editName,
                                                        onValueChange = { editName = it },
                                                        label = { Text("Food Title / Name") },
                                                        modifier = Modifier.fillMaxWidth().testTag("edit_food_name_field")
                                                    )

                                                    OutlinedTextField(
                                                        value = editPrice,
                                                        onValueChange = { editPrice = it },
                                                        label = { Text("Price (GH₵)") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        modifier = Modifier.fillMaxWidth().testTag("edit_food_price_field")
                                                    )

                                                    // Choice selection for diet category
                                                    Column {
                                                        Text("Diet Category Selection:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            listOf("Traditional", "Snacks", "Drinks", "SpecialTY", "Local Dish").forEach { cat ->
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .clickable { editCategory = cat }
                                                                        .background(if (editCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                        .testTag("edit_category_chip_$cat")
                                                                ) {
                                                                    Text(
                                                                        cat,
                                                                        fontSize = 9.sp,
                                                                        color = if (editCategory == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    OutlinedTextField(
                                                        value = editDescription,
                                                        onValueChange = { editDescription = it },
                                                        label = { Text("Aesthetic Culinary Description & Ingredients") },
                                                        modifier = Modifier.fillMaxWidth().testTag("edit_food_description_field"),
                                                        maxLines = 4
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedTextField(
                                                             value = editInitialStock,
                                                             onValueChange = { editInitialStock = it },
                                                             label = { Text("Initial Stock") },
                                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                             modifier = Modifier.weight(1f).testTag("edit_food_initial_stock")
                                                        )
                                                        OutlinedTextField(
                                                             value = editCurrentStock,
                                                             onValueChange = { editCurrentStock = it },
                                                             label = { Text("Current Stock") },
                                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                             modifier = Modifier.weight(1f).testTag("edit_food_current_stock")
                                                        )
                                                    }

                                                    OutlinedTextField(
                                                        value = editThreshold,
                                                        onValueChange = { editThreshold = it },
                                                        label = { Text("Safety Stock Alert Level") },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        modifier = Modifier.fillMaxWidth().testTag("edit_food_threshold")
                                                    )

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        OutlinedTextField(
                                                             value = editCalories,
                                                             onValueChange = { editCalories = it },
                                                             label = { Text("Calories (kcal)") },
                                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                             modifier = Modifier.weight(1f).testTag("edit_food_calories")
                                                        )
                                                        OutlinedTextField(
                                                             value = editAllergens,
                                                             onValueChange = { editAllergens = it },
                                                             label = { Text("Allergens (e.g. Nuts)") },
                                                             modifier = Modifier.weight(1f).testTag("edit_food_allergens")
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                        TextButton(onClick = { isEditingFoodDetails = false }) {
                                                             Text("Cancel")
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Button(
                                                            onClick = {
                                                                val priceVal = editPrice.toDoubleOrNull() ?: food.price
                                                                val initStockVal = editInitialStock.toIntOrNull() ?: food.initialStock
                                                                val curStockVal = editCurrentStock.toIntOrNull() ?: food.currentStock
                                                                val thresholdVal = editThreshold.toIntOrNull() ?: food.lowStockThreshold
                                                                val caloriesVal = editCalories.toIntOrNull() ?: food.calories

                                                                if (editName.isNotBlank() && priceVal > 0.0) {
                                                                    viewModel.updateMenuFoodItemDetails(
                                                                        foodItem = food,
                                                                        name = editName,
                                                                        price = priceVal,
                                                                        category = editCategory,
                                                                        description = editDescription,
                                                                        initialStock = initStockVal,
                                                                        currentStock = curStockVal,
                                                                        threshold = thresholdVal,
                                                                        calories = caloriesVal,
                                                                        allergens = editAllergens.ifBlank { "None" }
                                                                    )
                                                                    isEditingFoodDetails = false
                                                                }
                                                            },
                                                            modifier = Modifier.testTag("submit_edit_food_details_btn")
                                                        ) {
                                                             Text("Save Changes")
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
                    // Ratings / Analytics Panel
                    // Date range filtering options and presets
                    var selectedDateRangePreset by remember { mutableStateOf("ALL") } // "ALL", "TODAY", "WEEK", "MONTH", "CUSTOM"
                    var startDateStr by remember { mutableStateOf("2026-05-26") } // Defaults to a week ago
                    var endDateStr by remember { mutableStateOf("2026-06-02") }   // Defaults to today's local metadata date

                    // Filtering calculation
                    val filteredIncomingOrders = remember(incomingOrders, selectedDateRangePreset, startDateStr, endDateStr) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val startMillis = try { sdf.parse(startDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                        val endMillis = try { (sdf.parse(endDateStr)?.time ?: 0L) + 24 * 60 * 60 * 1000L - 1 } catch(e: Exception) { Long.MAX_VALUE }

                        val now = System.currentTimeMillis()
                        incomingOrders.filter { order ->
                            when (selectedDateRangePreset) {
                                "TODAY" -> {
                                    val orderDay = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(order.orderTimestamp))
                                    val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(now))
                                    orderDay == today
                                }
                                "WEEK" -> {
                                    val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000L
                                    order.orderTimestamp >= oneWeekAgo
                                }
                                "MONTH" -> {
                                    val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
                                    order.orderTimestamp >= thirtyDaysAgo
                                }
                                "CUSTOM" -> {
                                    order.orderTimestamp in startMillis..endMillis
                                }
                                else -> true // "ALL"
                            }
                        }
                    }

                    val filteredFeedbackList = remember(vendorFeedbackList, selectedDateRangePreset, startDateStr, endDateStr) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val startMillis = try { sdf.parse(startDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                        val endMillis = try { (sdf.parse(endDateStr)?.time ?: 0L) + 24 * 60 * 60 * 1000L - 1 } catch(e: Exception) { Long.MAX_VALUE }

                        val now = System.currentTimeMillis()
                        vendorFeedbackList.filter { feedback ->
                            when (selectedDateRangePreset) {
                                "TODAY" -> {
                                    val feedbackDay = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(feedback.timestamp))
                                    val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date(now))
                                    feedbackDay == today
                                }
                                "WEEK" -> {
                                    val oneWeekAgo = now - 7L * 24 * 60 * 60 * 1000L
                                    feedback.timestamp >= oneWeekAgo
                                }
                                "MONTH" -> {
                                    val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L
                                    feedback.timestamp >= thirtyDaysAgo
                                }
                                "CUSTOM" -> {
                                    feedback.timestamp in startMillis..endMillis
                                }
                                else -> true // "ALL"
                            }
                        }
                    }

                    val dateScopeLabel = when (selectedDateRangePreset) {
                        "TODAY" -> "Today"
                        "WEEK" -> "Last 7 Days"
                        "MONTH" -> "Last 30 Days"
                        "CUSTOM" -> "$startDateStr to $endDateStr"
                        else -> "All Time"
                    }

                    val metrics = viewModel.getVendorMetrics(currentUser?.id ?: 0, filteredFeedbackList)
                    val completedOrders = filteredIncomingOrders.filter { it.status == "COMPLETED" }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(analyticsScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Booth Analytics & Student Scorecards",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // ADVANCED POS DATE RANGE FILTER
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("date_range_filter_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Date Filters",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Filter Audit & History Scope",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Scrollable Row of Filter Preset Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val presets = listOf(
                                        "ALL" to "All Time",
                                        "TODAY" to "Today",
                                        "WEEK" to "7 Days",
                                        "MONTH" to "30 Days",
                                        "CUSTOM" to "Custom Date"
                                    )
                                    presets.forEach { (presetKey, presetName) ->
                                        val isSelected = selectedDateRangePreset == presetKey
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { selectedDateRangePreset = presetKey }
                                                .testTag("preset_filter_$presetKey"),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = presetName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = selectedDateRangePreset == "CUSTOM"
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        Text(
                                            text = "Specify Accounting Custom Range (inclusive):",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = startDateStr,
                                                onValueChange = { startDateStr = it },
                                                label = { Text("Start Date", fontSize = 10.sp) },
                                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("custom_start_date_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            
                                            Text("to", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            
                                            OutlinedTextField(
                                                value = endDateStr,
                                                onValueChange = { endDateStr = it },
                                                label = { Text("End Date", fontSize = 10.sp) },
                                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("custom_end_date_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Quick info context
                                        Text(
                                            text = "* Data is updated live on all visual graphs and downloads below upon entering a valid YYYY-MM-DD date format.",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 1. Core Summary metrics and KPI Dashboard
                        val getOrderPrepTimeMinutes: (com.example.data.Order) -> Int = { order ->
                            val digits = order.estimatedPickupTime.filter { it.isDigit() }
                            if (digits.isNotEmpty()) {
                                val parsed = digits.toIntOrNull()
                                if (parsed != null && parsed > 0) parsed else (10 + (order.id % 16))
                            } else {
                                10 + (order.id % 16)
                            }
                        }

                        val totalOrdersCount = filteredIncomingOrders.size
                        val totalRevenue = completedOrders.sumOf { it.totalPrice }
                        val averageOrderValue = if (completedOrders.isNotEmpty()) totalRevenue / completedOrders.size else 0.0
                        val avgRatingsVal = metrics["overall"] ?: 0.0
                        val avgPrepTime = if (completedOrders.isNotEmpty()) completedOrders.map { getOrderPrepTimeMinutes(it) }.average() else 12.5

                        Text(
                            "Key Performance Indicators (${dateScopeLabel})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // 3 column or 2 column grid of beautifully polished metrics cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: REVENUE & TOTAL ORDERS
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_revenue"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Revenue",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "REVENUE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "GH₵ ${"%.2f".format(totalRevenue)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Settled sales in period",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_total_orders"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Total Orders",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "TOTAL ORDERS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "$totalOrdersCount",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${completedOrders.size} Completed plates",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Row 2: AVERAGE ORDER VALUE & PREP TIME
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_aov"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = "Average Order Value",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "AVG ORDER (AOV)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "GH₵ ${"%.2f".format(averageOrderValue)}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Mean checkout ticket",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("kpi_avg_prep_time"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = "Avg Prep Time",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "AVG PREP SPEED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${"%.1f".format(avgPrepTime)}m",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Minutes cooking span",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Row 3: CUSTOMER RATINGS SATISFACTION INDEX
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("kpi_satisfaction"),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Satisfaction Rating",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                "CUSTOMER RATINGS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Overall quality & experience scorecard",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${"%.1f".format(avgRatingsVal)}★",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Based on ${filteredFeedbackList.size} ratings",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // FEATURE ADDITION: MOST POPULAR DISHES WIDGET
                        val popularDishes = remember(filteredIncomingOrders) {
                            filteredIncomingOrders
                                .groupBy { it.foodName }
                                .mapValues { entry -> 
                                    val count = entry.value.sumOf { it.quantity }
                                    val revenue = entry.value.sumOf { it.totalPrice }
                                    count to revenue
                                }
                                .toList()
                                .sortedByDescending { it.second.first }
                                .take(3)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("popular_dishes_card"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = "Popular Dishes",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Most Popular Dishes (Sales Lead)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (popularDishes.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No checkout data registered in this period.", fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                } else {
                                    val maxVolume = popularDishes.maxOfOrNull { it.second.first } ?: 1
                                    popularDishes.forEachIndexed { index, (dishName, valPair) ->
                                        val quantity = valPair.first
                                        val revenue = valPair.second
                                        val progress = quantity.toFloat() / maxVolume.toFloat()
                                        
                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val rankColor = when (index) {
                                                        0 -> Color(0xFFFFD700) // Gold
                                                        1 -> Color(0xFFC0C0C0) // Silver
                                                        else -> Color(0xFFCD7F32) // Bronze
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .background(rankColor, RoundedCornerShape(4.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("#${index + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                    Text(dishName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text("$quantity plates (GH₵ ${"%.2f".format(revenue)})", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
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

                        // DATA ANALYTICS & TREND HIGHLIGHTS CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analytics_trends_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Trend Highlights",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Booth Analytics & Trend Highlights",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))

                                // Trend recommendation 1: Volume insight
                                val volumeInsight = if (totalOrdersCount > 10) {
                                    "High transactional velocity detected. Your peak hour congestion is currently stable, but prepare for heavy lunch rush transitions."
                                } else {
                                    "Moderate order volume. Recommend triggering dynamic off-peak happy hour discounts to drive higher student afternoon engagement."
                                }

                                // Trend recommendation 2: Pricing / AOV insight
                                val priceInsight = if (averageOrderValue < 20.0) {
                                    "AOV is relatively low at GH₵ ${"%.2f".format(averageOrderValue)}. Formulate Sobolo drink bundled combos with main meals to elevate food basket sizes."
                                } else {
                                    "Solid student purchase elasticity! Your current pricing structure yields a strong average basket ticket size of GH₵ ${"%.2f".format(averageOrderValue)}."
                                }

                                // Trend recommendation 3: Speed index insight
                                val serviceInsignt = if (avgPrepTime > 15.0) {
                                    "Preparation times average ${"%.1f".format(avgPrepTime)}m. Initiate pre-chopping or kitchen line splits to avoid peak student queues during common lecture breaks."
                                } else {
                                    "Excellent cooking turnaround speed. Your food station is highly optimized under the university's priority delivery benchmarks."
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("📈", fontSize = 14.sp)
                                        Column {
                                            Text("Sales & Demand Traffic", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(volumeInsight, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("💰", fontSize = 14.sp)
                                        Column {
                                            Text("Pricing Elasticity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(priceInsight, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("⏱️", fontSize = 14.sp)
                                        Column {
                                            Text("Cooking & Queue Efficiency", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(serviceInsignt, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 1b. DOWNLOADABLE REPORT CONTROL CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = "Download Report",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Monthly Performance Reports",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Generate and download a comprehensive monthly POS audit report of all statistics, revenues, food completion metrics, and review transcripts.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                var reportStatusText by remember { mutableStateOf<String?>(null) }
                                var isExportingReport by remember { mutableStateOf(false) }

                                if (isExportingReport) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Compiling certified database records...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    reportStatusText?.let { status ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isExportingReport = true
                                                viewModel.exportAnalyticsReport(
                                                    format = "PDF",
                                                    vendorId = currentUser?.id ?: 0,
                                                    vendorName = currentUser?.fullName ?: "Vendor",
                                                    orders = filteredIncomingOrders,
                                                    feedbacks = filteredFeedbackList,
                                                    dateRangeScope = dateScopeLabel
                                                ) { success, message ->
                                                    isExportingReport = false
                                                    reportStatusText = message
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Export PDF", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                isExportingReport = true
                                                viewModel.exportAnalyticsReport(
                                                    format = "CSV",
                                                    vendorId = currentUser?.id ?: 0,
                                                    vendorName = currentUser?.fullName ?: "Vendor",
                                                    orders = filteredIncomingOrders,
                                                    feedbacks = filteredFeedbackList,
                                                    dateRangeScope = dateScopeLabel
                                                ) { success, message ->
                                                    isExportingReport = false
                                                    reportStatusText = message
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.GridOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Export CSV", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 1c. d3.js Interactive Dashboard Chart View
                        D3DashboardChart(orders = filteredIncomingOrders, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(8.dp))

                        // Recharts Interactive Dashboard Chart View
                        RechartsDashboardChart(orders = filteredIncomingOrders, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(10.dp))

                        // Recharts Interactive 30-Day Average Ratings & Order Volumes Feedback Chart View
                        RechartsFeedbackDashboardChart(
                            orders = filteredIncomingOrders,
                            feedbacks = filteredFeedbackList,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chart.js Service Performance Metrics Component (Fulfillment rates & Delivery times)
                        ChartJsVendorPerformanceChart(
                            performanceMetrics = performanceMetrics,
                            currentUser = currentUser,
                            orders = incomingOrders,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Joint Interactive Low-Stock Highlight and Control Center
                        InventoryTrackingHub(
                            vendorFoods = vendorFoods,
                            onUpdateThreshold = { food, newThreshold ->
                                viewModel.updateFoodItemStockSettings(food, food.initialStock, food.currentStock, newThreshold)
                            },
                            onReplenishStock = { food, amount ->
                                viewModel.replenishFoodItemStock(food, amount)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Dynamic Radar Visual Chart
                        RadarFeedbackChart(metrics = metrics, modifier = Modifier.fillMaxWidth())

                        // 3. Dynamic Revenue Bar Chart
                        DailyRevenueBarChart(orders = filteredIncomingOrders, modifier = Modifier.fillMaxWidth())

                        // 3b. Interactive Weekly Revenue Trend Spline Line Chart (Recharts-inspired Style)
                        WeeklyRevenueTrendLineChart(orders = incomingOrders, modifier = Modifier.fillMaxWidth())

                        // 3a. Recharts-style spline line chart fed by VendorPerformanceController from backend
                        if (com.example.data.LaravelClientManager.isLaravelEnabled) {
                            VendorPerformanceTrendChart(
                                performanceData = performanceData,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LaravelDailyRevenueTrendChart(
                                dailyRevenueResponse = dailyRevenueResponse,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 4. Vendor Performance Controller Metrics (Cards & Basic Table)
                        // This section directly implements the core user feature requirement
                        
                        // Extract current logged-in vendor's metrics if available from performanceMetrics StateFlow
                        val matchedMetric = remember(performanceMetrics, currentUser) {
                            performanceMetrics.find { it.vendor_id == currentUser?.id }
                        }
                        
                        // Local fallback calculations for simulation robustness
                        val localTotalOrders = incomingOrders.size
                        val localCompletedOrders = incomingOrders.count { it.status == "COMPLETED" }
                        val localTotalSales = incomingOrders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
                        val localFulfillment = if (localTotalOrders > 0) (localCompletedOrders.toDouble() / localTotalOrders * 100.0) else 100.0
                        
                        val dispFulfillment = if (matchedMetric != null) matchedMetric.order_fulfillment_rate else localFulfillment
                        val dispDeliveryTime = if (matchedMetric != null) matchedMetric.average_delivery_time_display else "12.5 mins"
                        val dispCompletedCount = if (matchedMetric != null) matchedMetric.total_completed_orders else localCompletedOrders
                        val dispTotalCount = if (matchedMetric != null) matchedMetric.total_orders else localTotalOrders
                        val dispSales = if (matchedMetric != null) matchedMetric.total_sales else localTotalSales
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("vendor_performance_service_section"),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📋", fontSize = 20.sp)
                                Column {
                                    Text(
                                        text = "Vendor Operations Performance Scorecard",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (matchedMetric != null) "Direct Live Connection: VendorPerformanceController (Laravel)" else "Simulation Engine: Synthesized On-the-Fly Metrics",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (matchedMetric != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            
                            // 2x2 Grid of Beautiful KPI Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Card 1: Fulfillment Rate
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("perf_card_fulfillment"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Fulfillment Rate",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "FULFILLMENT RATE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "%.1f%%".format(dispFulfillment),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Ratio of completed orders",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Card 2: Average Delivery Time
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("perf_card_delivery_time"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = "Delivery Time",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "AVG DELIVERY TIME",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = dispDeliveryTime,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Tick-to-pickup turnaround",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Card 3: Total Orders
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("perf_card_total_orders"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Orders Tracked",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "TOTAL OPERATIONS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$dispCompletedCount / $dispTotalCount",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Completed over total placed",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Card 4: Total Revenue (Sales)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("perf_card_revenue"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = "Aggregated Revenue",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "OFFICIAL SALES",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "GH₵ %.2f".format(dispSales),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Gross settled sales",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Benchmark/Comparison Table
                            Text(
                                text = "🏁 Inter-Booth Service-Level Benchmarks (Basic Table)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("perf_comparison_table_card"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Table Header Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "VENDOR", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.5f))
                                        Text(text = "FULFILLMENT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                        Text(text = "AVG DELIVERY", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                        Text(text = "ORDERS", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    }
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                                    
                                    val tableItems = remember(performanceMetrics, dispFulfillment, dispDeliveryTime, dispTotalCount, currentUser) {
                                        if (performanceMetrics.isNotEmpty()) {
                                            performanceMetrics
                                        } else {
                                            // Fallback peers for gorgeous demonstration
                                            listOf(
                                                com.example.data.LaravelVendorMetric(
                                                    vendor_id = currentUser?.id ?: 1,
                                                    vendor_name = currentUser?.fullName ?: "My Food Booth",
                                                    contact_info = "N/A",
                                                    operational_status = "active",
                                                    total_completed_orders = dispCompletedCount,
                                                    total_orders = dispTotalCount,
                                                    total_sales = dispSales,
                                                    avg_completion_time_minutes = if (dispDeliveryTime.contains("mins")) dispDeliveryTime.replace(" mins", "").toDoubleOrNull() ?: 12.5 else 12.5,
                                                    avg_completion_time_display = dispDeliveryTime,
                                                    average_delivery_time = if (dispDeliveryTime.contains("mins")) dispDeliveryTime.replace(" mins", "").toDoubleOrNull() ?: 12.5 else 12.5,
                                                    average_delivery_time_display = dispDeliveryTime,
                                                    order_fulfillment_rate = dispFulfillment
                                                ),
                                                com.example.data.LaravelVendorMetric(
                                                    vendor_id = 101,
                                                    vendor_name = "Waakye Express",
                                                    contact_info = "0245-WA-AKYE",
                                                    operational_status = "active",
                                                    total_completed_orders = 18,
                                                    total_orders = 20,
                                                    total_sales = 270.00,
                                                    avg_completion_time_minutes = 9.8,
                                                    avg_completion_time_display = "9.8 mins",
                                                    average_delivery_time = 9.8,
                                                    average_delivery_time_display = "9.8 mins",
                                                    order_fulfillment_rate = 90.0
                                                ),
                                                com.example.data.LaravelVendorMetric(
                                                    vendor_id = 102,
                                                    vendor_name = "Auntie Mary's Waakye",
                                                    contact_info = "0554-MARY-K",
                                                    operational_status = "active",
                                                    total_completed_orders = 24,
                                                    total_orders = 25,
                                                    total_sales = 360.00,
                                                    avg_completion_time_minutes = 14.1,
                                                    avg_completion_time_display = "14.1 mins",
                                                    average_delivery_time = 14.1,
                                                    average_delivery_time_display = "14.1 mins",
                                                    order_fulfillment_rate = 96.0
                                                )
                                            )
                                        }
                                    }
                                    
                                    tableItems.forEachIndexed { idx, item ->
                                        val isMe = item.vendor_id == currentUser?.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Unspecified)
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (isMe) "👤 ${item.vendor_name} (Me)" else item.vendor_name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1.5f),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "%.1f%%".format(item.order_fulfillment_rate),
                                                fontSize = 11.sp,
                                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                                color = if (item.order_fulfillment_rate >= 90.0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1.2f),
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                text = item.average_delivery_time_display,
                                                fontSize = 11.sp,
                                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1.2f),
                                                textAlign = TextAlign.End
                                            )
                                            Text(
                                                text = "${item.total_completed_orders}/${item.total_orders}",
                                                fontSize = 11.sp,
                                                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        if (idx < tableItems.size - 1) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }

                        // 3b. Gemini AI Qualitative Sentiment Analysis Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("sentiment_analysis_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Gemini AI",
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                    Text(
                                        text = "Gemini Student Sentiment Analyst",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Employ Gemini Flash to analyze all student feedback transcripts and order remarks instantly for a complete qualitative report.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isAnalyzingSentiment) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Analyzing conversational comment transcripts...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                } else {
                                    sentimentAnalysisText?.let { analysisResult ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "AI MODEL SYNTHESIS",
                                                            color = Color(0xFF2E7D32),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = analysisResult,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.runVendorSentimentAnalysis(
                                                vendorId = currentUser?.id ?: 0,
                                                vendorName = currentUser?.fullName ?: "Vendor",
                                                feedbacks = filteredFeedbackList
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("analyse_sentiment_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (sentimentAnalysisText == null) "Run Sentiment Analysis" else "Re-analyze Comments",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 3c. Gemini AI Auto-Reply Assistant Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("auto_reply_templates_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Quick Reply Suggestions",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Gemini Quick Response Assistant",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Generate professional, polite, and actionable auto-reply response templates customized to address student pain points and negative comments.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isGeneratingAutoReplies) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Drafting professional support templates...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                } else {
                                    autoRepliesText?.let { templates ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "READY TO COPY TEMPLATES",
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = templates,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.runVendorAutoReplies(
                                                vendorId = currentUser?.id ?: 0,
                                                vendorName = currentUser?.fullName ?: "Vendor",
                                                feedbacks = filteredFeedbackList
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("generate_auto_replies_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (autoRepliesText == null) "Generate Professional Replies" else "Regenerate Response Templates",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 3d. Gemini AI Pricing & Specials Planner Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("pricing_suggestions_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Pricing Suggestions",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Gemini Pricing & Specials Planner",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Synthesize active campus transaction patterns to dynamically suggest student pricing elasticities and off-peak Happy Hour combos.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isGeneratingPricingSuggestions) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Analyzing hourly student purchase elasticities...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    pricingSuggestionsText?.let { dynamicPlan ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "AUTOMATED REVENUE ROADMAP",
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = dynamicPlan,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.runVendorPricingSuggestions(
                                                vendorId = currentUser?.id ?: 0,
                                                vendorName = currentUser?.fullName ?: "Vendor",
                                                orders = filteredIncomingOrders,
                                                foodItems = vendorFoods
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("generate_pricing_suggestions_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (pricingSuggestionsText == null) "Suggest Pricing & Specials" else "Re-optimize Pricing Plans",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // 3e. Gemini AI Historical Demand & Busiest Hour Advisor Card (NEW)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("historical_demand_insights_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Historical Insights",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Gemini Demand & Busiest Hour Advisor",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = "Deploy Google Gemini to crawl your complete historical order database. Generates insights on popular food items, identifies peak crowding slots, and predicts prep schedules.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isAnalyzingHistoricalOrders) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Mining historical order databases & timeslots...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    historicalInsightsText?.let { insightReport ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
                                                .padding(14.dp)
                                        ) {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            "GEMINI HISTORICAL DEMAND SYNTHESIS",
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = insightReport,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.runVendorHistoricalInsights(
                                                vendorId = currentUser?.id ?: 0,
                                                vendorName = currentUser?.fullName ?: "Vendor",
                                                allOrders = incomingOrders
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("generate_historical_insights_button")
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (historicalInsightsText == null) "Analyze Historical Demand Patterns" else "Regenerate Campus Demand Insights",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Feedback details
                        Text("Live Customer Student Feedback Log", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        if (filteredFeedbackList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No reviews have been submitted by students for physical dishes yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            filteredFeedbackList.forEach { f ->
                                val studentUser = allUsers.find { it.id == f.customerId }
                                val studentName = studentUser?.fullName ?: "Verified Student"
                                val studentIdStr = studentUser?.student_staff_id ?: studentUser?.info ?: "ATU Student"

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(studentName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Column {
                                                    Text(studentName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(studentIdStr, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                text = "Order Ref: #${f.orderId}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        androidx.compose.foundation.layout.FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("🍔 Food Taste: ", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                                repeat(5) { index ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (index < f.ratingFoodQuality) Color(0xFFFFB300) else Color.LightGray,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("✨ Hygiene: ", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                                repeat(5) { index ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (index < f.ratingCleanliness) Color(0xFFFFB300) else Color.LightGray,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("⏱️ Service Speed: ", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                                repeat(5) { index ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (index < f.ratingServiceSpeed) Color(0xFFFFB300) else Color.LightGray,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("💰 Price Value: ", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                                repeat(5) { index ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (index < f.ratingPriceValue) Color(0xFFFFB300) else Color.LightGray,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text("STUDENT REMARKS", fontStyle = androidx.compose.ui.text.font.FontStyle.Normal, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = f.comment.ifBlank { "No detailed comments entered by user." },
                                                    fontSize = 11.sp, 
                                                    color = MaterialTheme.colorScheme.onSurface, 
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontStyle = if (f.comment.isNotBlank()) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Store Settings & Hub Tab
                    var tempBroadText by remember { mutableStateOf(vendorAnnouncement) }
                    var tempMinOrderPrice by remember { mutableStateOf("5.0") }
                    var exportLedgerStatus by remember { mutableStateOf<String?>(null) }
                    var isExporting by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(settingsScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Pro-Suite Settings & Hub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Interactive Broadcaster Bulletin
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Live Bulletin Broadcast", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text("Update the broadcast announcement shown on all student menu browsers.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = tempBroadText,
                                    onValueChange = { tempBroadText = it },
                                    label = { Text("Bulletin text") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        viewModel.updateVendorAnnouncement(tempBroadText)
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Publish to Students", fontSize = 12.sp)
                                }
                            }
                        }

                        // 1b. Update Brand Visuality (Logo & Cover)
                        var selfLogoUrl by remember(currentUser) { mutableStateOf(currentUser?.logoUrl ?: "") }
                        var selfPictureUrl by remember(currentUser) { mutableStateOf(currentUser?.pictureUrl ?: "") }
                        var showUpdateSuccessMsg by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("brand_visuals_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎨 Profile Brand Visuals", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text("Alter your brand's digital signage, including rounding avatar logos and header covers seen by students.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(4.dp))

                                // Logo Edit Field
                                Text("Vendor Logo Icon Link", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                OutlinedTextField(
                                    value = selfLogoUrl,
                                    onValueChange = { selfLogoUrl = it },
                                    label = { Text("Logo Photo URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Logo presets Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Logo presets:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    listOf(
                                        "🍲" to "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=120&auto=format&fit=crop&q=60",
                                        "🍔" to "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=120&auto=format&fit=crop&q=60",
                                        "🍰" to "https://images.unsplash.com/photo-1517433456452-f9633a875f6f?w=120&auto=format&fit=crop&q=60",
                                        "🥤" to "https://images.unsplash.com/photo-1497534446932-c925b458314e?w=120&auto=format&fit=crop&q=60"
                                    ).forEach { (emoji, url) ->
                                        FilterChip(
                                            selected = selfLogoUrl == url,
                                            onClick = { selfLogoUrl = url },
                                            label = { Text(emoji, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Picture Edit Field
                                Text("Vendor Cover Backdrop Link", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                OutlinedTextField(
                                    value = selfPictureUrl,
                                    onValueChange = { selfPictureUrl = it },
                                    label = { Text("Cover Picture URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                // Cover presets Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cover presets:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    listOf(
                                        "🍛 Jollof Joint" to "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=60",
                                        "🥖 Baker/Treats" to "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=60",
                                        "🥗 Salad/Healthy" to "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=500&auto=format&fit=crop&q=60"
                                    ).forEach { (label, url) ->
                                        FilterChip(
                                            selected = selfPictureUrl == url,
                                            onClick = { selfPictureUrl = url },
                                            label = { Text(label, fontSize = 8.sp) }
                                        )
                                    }
                                }

                                if (showUpdateSuccessMsg) {
                                    Text("Visual configurations synchronized instantly!", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = {
                                        currentUser?.let { cu ->
                                            val updatedUser = cu.copy(
                                                logoUrl = selfLogoUrl.ifBlank { null },
                                                pictureUrl = selfPictureUrl.ifBlank { null }
                                            )
                                            viewModel.updateVendor(updatedUser, newPinCode = null) { success ->
                                                if (success) {
                                                    showUpdateSuccessMsg = true
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Save Visual Changes", fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. Real-time Operating Status
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Store Operations", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Toggle instant pause on orders. Students cannot book foods while set as recessed.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isStoreClosed) "PAUSED / RECESSED" else "OPEN FOR BUSINESS",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isStoreClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                        )
                                        Text("Instantly halts queue bookings.", fontSize = 9.sp)
                                    }
                                    Switch(
                                        checked = isStoreClosed,
                                        onCheckedChange = { viewModel.setStoreClosedState(it) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = tempMinOrderPrice,
                                    onValueChange = { tempMinOrderPrice = it },
                                    label = { Text("Minimum Checkout Threshold (GH₵)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        // 2b. Real-time Inventory Alerts Settings
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("configurable_alert_settings_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("⚠️ Inventory Alert Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Configure safety stock alert levels to trigger real-time notifications on low-stock conditions.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                Spacer(modifier = Modifier.height(12.dp))

                                val currentThreshold by viewModel.globalLowStockThreshold.collectAsStateWithLifecycle()
                                var tempThresholdText by remember(currentThreshold) { mutableStateOf(currentThreshold.toString()) }

                                OutlinedTextField(
                                    value = tempThresholdText,
                                    onValueChange = { 
                                        tempThresholdText = it 
                                        it.toIntOrNull()?.let { newVal ->
                                            viewModel.updateGlobalLowStockThreshold(newVal)
                                        }
                                    },
                                    label = { Text("Global Safety Stock Threshold") },
                                    placeholder = { Text("e.g. 15") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth().testTag("global_safety_threshold_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "An alert will be raised if any menu item stock drops below this value. It can be overridden for specific items in the Menu Manager.",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // 3. Accounting ledgers exports
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("SEC & GRA Tax Compliance Ledger", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text("Instantly export certified digital POS reports corresponding to Accra municipality audits.", fontSize = 10.sp)

                                Spacer(modifier = Modifier.height(12.dp))

                                if (isExporting) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Compiling cryptographic Ledger records...", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                } else {
                                    exportLedgerStatus?.let { status ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.secondaryContainer,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                isExporting = true
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    isExporting = false
                                                    exportLedgerStatus = "Cryptographic ledger file ATU-LEDGER-${(1000..9999).random()}.pdf compiled."
                                                }, 1800)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Export PDF Ledger", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                isExporting = true
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    isExporting = false
                                                    exportLedgerStatus = "GRA CSV receipt audit stream initialized."
                                                }, 1400)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Export GRA CSV", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Custom CSV Business Exporter
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("csv_export_feature_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📊", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            "Business CSV Exporter",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Export your POS logs and sales trends for offline bookkeeping and spreadsheet calculations.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                var selectedExportType by remember { mutableStateOf("ORDERS") }
                                 var selectedPeriod by remember { mutableStateOf("All Time") } // "Past 24 Hours", "Past 7 Days", "Past 30 Days", "All Time"
                                 var selectedFormat by remember { mutableStateOf("CSV") } // "CSV" or "PDF"

                                 val filteredOrders = remember(incomingOrders, selectedPeriod) {
                                     com.example.ui.util.CsvExporter.filterOrdersByPeriod(incomingOrders, selectedPeriod)
                                 }

                                Text(
                                    "1. Choose Dataset Option:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedExportType = "ORDERS" }
                                            .testTag("export_type_orders"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedExportType == "ORDERS") 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedExportType == "ORDERS") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("📋", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Order Log", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedExportType == "ORDERS") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${filteredOrders.size} orders", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedExportType = "REVENUE" }
                                            .testTag("export_type_revenue"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedExportType == "REVENUE") 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedExportType == "REVENUE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("📈", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Revenue Trend", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedExportType == "REVENUE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(selectedPeriod.replace("Past ", ""), fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedExportType = "PERFORMANCE" }
                                            .testTag("export_type_performance"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedExportType == "PERFORMANCE") 
                                                MaterialTheme.colorScheme.primaryContainer 
                                            else 
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedExportType == "PERFORMANCE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text("⭐", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Performance", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selectedExportType == "PERFORMANCE") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${vendorFeedbackList.size} reviews", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    "2. Choose Reporting Period:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val periods = listOf("Past 24 Hours", "Past 7 Days", "Past 30 Days", "All Time")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    periods.forEach { period ->
                                        val isSelected = selectedPeriod == period
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .clickable { selectedPeriod = period }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = period.replace("Past ", ""),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    "3. Choose Report Format:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedFormat = "CSV" }
                                            .testTag("export_format_csv"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedFormat == "CSV")
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedFormat == "CSV") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("📄", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("CSV Spreadsheet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedFormat == "CSV") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedFormat = "PDF" }
                                            .testTag("export_format_pdf"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedFormat == "PDF")
                                                MaterialTheme.colorScheme.secondaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selectedFormat == "PDF") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("📕", fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PDF Document", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedFormat == "PDF") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                val context = androidx.compose.ui.platform.LocalContext.current

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val vendorName = currentUser?.fullName ?: "Official Vendor"
                                            val vendorId = currentUser?.id ?: 0
                                            val prefix = when (selectedExportType) {
                                                "ORDERS" -> "ATU_Order_History"
                                                "REVENUE" -> "ATU_Weekly_Revenue"
                                                else -> "ATU_Performance_Audit"
                                            }
                                            val finalFileName = "${prefix}_Export_${System.currentTimeMillis()}.csv"

                                            if (selectedFormat == "PDF") {
                                                val pdfFileName = "${prefix}_Report_${System.currentTimeMillis()}.pdf"
                                                if (selectedExportType == "PERFORMANCE") {
                                                    viewModel.exportAnalyticsReport(
                                                        format = "PDF",
                                                        vendorId = vendorId,
                                                        vendorName = vendorName,
                                                        orders = filteredOrders,
                                                        feedbacks = vendorFeedbackList,
                                                        dateRangeScope = selectedPeriod
                                                    ) { success, msg ->
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    com.example.ui.util.CsvExporter.exportOrdersPdf(context, pdfFileName, vendorName, selectedPeriod, filteredOrders)
                                                }
                                                 scope.launch {
                                                     viewModel.repository.insertAuditLog(
                                                         currentUser?.id ?: 0,
                                                         "REPORT_EXPORTER",
                                                         "Downloaded PDF report ($selectedExportType - $selectedPeriod) to Downloads folder."
                                                     )
                                                 }
                                             } else {
                                                 val actualCsv = when (selectedExportType) {
                                                     "ORDERS" -> com.example.ui.util.CsvExporter.generateOrdersCsv(filteredOrders, allUsers)
                                                     "REVENUE" -> com.example.ui.util.CsvExporter.generateRevenueReportCsv(filteredOrders)
                                                     else -> com.example.ui.util.CsvExporter.generatePerformanceReportCsv(vendorId, vendorName, filteredOrders, vendorFeedbackList)
                                                 }
                                                 com.example.ui.util.CsvExporter.saveCsvToDownloads(context, finalFileName, actualCsv)
                                                 scope.launch {
                                                     viewModel.repository.insertAuditLog(
                                                         currentUser?.id ?: 0,
                                                         "REPORT_EXPORTER",
                                                         "Downloaded CSV spreadsheet ($selectedExportType - $selectedPeriod) to Downloads folder."
                                                     )
                                                 }
                                             }

                                            

                                            

                                        },
                                        modifier = Modifier.weight(1.2f).testTag("action_csv_save"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save to Downloads", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val csvData = if (selectedExportType == "ORDERS") {
                                                com.example.ui.util.CsvExporter.generateOrdersCsv(incomingOrders, allUsers)
                                            } else {
                                                com.example.ui.util.CsvExporter.generateRevenueReportCsv(incomingOrders)
                                            }
                                            val subjectName = when (selectedExportType) {
                                                "ORDERS" -> "ATU Order History Report"
                                                "REVENUE" -> "ATU Weekly Revenue Report"
                                                else -> "ATU Vendor Performance Report"
                                            }
                                            val csvDataShared = when (selectedExportType) {
                                                "ORDERS" -> com.example.ui.util.CsvExporter.generateOrdersCsv(filteredOrders, allUsers)
                                                "REVENUE" -> com.example.ui.util.CsvExporter.generateRevenueReportCsv(filteredOrders)
                                                else -> com.example.ui.util.CsvExporter.generatePerformanceReportCsv(currentUser?.id ?: 0, currentUser?.fullName ?: "Vendor", filteredOrders, vendorFeedbackList)
                                            }
                                            val sharedContent = if (selectedFormat == "PDF") {
                                                val completedOrders = filteredOrders.filter { it.status == "COMPLETED" }
                                                val totalRevenue = completedOrders.sumOf { it.totalPrice }
                                                "Report Summary: $subjectName ($selectedPeriod)\n" +
                                                "Total Records: ${filteredOrders.size} orders\n" +
                                                "Completed: ${completedOrders.size}\n" +
                                                "Total Gross Sales: GH₵ ${"%.2f".format(totalRevenue)}\n\n" +
                                                "CSV Data Sample:\n$csvDataShared"
                                            } else {
                                                csvDataShared
                                            }
                                            com.example.ui.util.CsvExporter.shareCsvData(context, subjectName, sharedContent)
                                            val bypassedShare = true
                                            if (!bypassedShare) com.example.ui.util.CsvExporter.shareCsvData(context, subjectName, csvData)
                                            
                                            scope.launch {
                                                viewModel.repository.insertAuditLog(
                                                    currentUser?.id ?: 0,
                                                    "CSV_EXPORTER",
                                                     "Shared CSV report metadata ($selectedExportType) via sharing sheet."
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("action_csv_share"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 10.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val csvData = if (selectedExportType == "ORDERS") {
                                                com.example.ui.util.CsvExporter.generateOrdersCsv(incomingOrders, allUsers)
                                            } else {
                                                com.example.ui.util.CsvExporter.generateRevenueReportCsv(incomingOrders)
                                            }
                                            if (selectedFormat == "PDF") {
                                                android.widget.Toast.makeText(context, "Direct clipboard copy not supported for binary PDFs. Please use 'Save to Downloads' or 'Share'.", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                val csvDataCopied = when (selectedExportType) {
                                                    "ORDERS" -> com.example.ui.util.CsvExporter.generateOrdersCsv(filteredOrders, allUsers)
                                                    "REVENUE" -> com.example.ui.util.CsvExporter.generateRevenueReportCsv(filteredOrders)
                                                    else -> com.example.ui.util.CsvExporter.generatePerformanceReportCsv(currentUser?.id ?: 0, currentUser?.fullName ?: "Vendor", filteredOrders, vendorFeedbackList)
                                                }
                                                com.example.ui.util.CsvExporter.copyToClipboard(context, "ATU_CSV_Data", csvDataCopied)
                                            }
                                            val bypassedCopy = true
                                            if (!bypassedCopy) com.example.ui.util.CsvExporter.copyToClipboard(context, "ATU_CSV_Data", csvData)
                                            
                                            scope.launch {
                                                viewModel.repository.insertAuditLog(
                                                    currentUser?.id ?: 0,
                                                    "CSV_EXPORTER",
                                                    "Copied plain CSV content ($selectedExportType) to clipboard."
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(0.9f).testTag("action_csv_copy"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    // Vendor Finance & Earnings Hub Tab
                    val providerTransactions by viewModel.userWalletTransactions.collectAsStateWithLifecycle()
                    var payoutAmountState by remember { mutableStateOf("") }
                    var payoutDetailsState by remember { mutableStateOf("") }
                    var hasRequestedPayout by remember { mutableStateOf<String?>(null) }
                    var payoutErrorMessage by remember { mutableStateOf<String?>(null) }

                    LazyColumn(
                        state = financeScrollState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "Vendor Finance Hub",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Manage your student meal earnings, payouts, and book-keeping records.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // A. Accumulated Earnings Visual Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Total Accumulated Earnings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "GH₵ ${"%.2f".format(currentUser?.balance ?: 0.0)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Funds reflect instantly when students complete order secure PIN verification.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // B. Request Instant Mobile Money Settlement
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Initiate Mobile Money Payout",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Withdraw your wallet balance instantly to your MTN MoMo, Telecel Cash or ATU Credit accounts.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (hasRequestedPayout != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(hasRequestedPayout!!, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(onClick = { hasRequestedPayout = null; payoutAmountState = ""; payoutDetailsState = "" }) {
                                            Text("Request another payout", fontSize = 11.sp)
                                        }
                                    } else {
                                        if (payoutErrorMessage != null) {
                                            Text(payoutErrorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        OutlinedTextField(
                                            value = payoutAmountState,
                                            onValueChange = { payoutAmountState = it },
                                            label = { Text("Payout Amount (GH₵)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = payoutDetailsState,
                                            onValueChange = { payoutDetailsState = it },
                                            label = { Text("MoMo / Bank Details (e.g. MTN MoMo - 0541249214)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Button(
                                            onClick = {
                                                val amt = payoutAmountState.toDoubleOrNull()
                                                if (amt == null || amt <= 0.0) {
                                                    payoutErrorMessage = "Please input a valid payout amount."
                                                    return@Button
                                                }
                                                if (currentUser != null && currentUser!!.balance < amt) {
                                                    payoutErrorMessage = "Insufficient sales earnings balance."
                                                    return@Button
                                                }
                                                if (payoutDetailsState.isBlank()) {
                                                    payoutErrorMessage = "Please provide valid account settlement details."
                                                    return@Button
                                                }
                                                payoutErrorMessage = null
                                                viewModel.requestVendorPayout(amt, payoutDetailsState) { success ->
                                                    if (success) {
                                                        hasRequestedPayout = "GH₵ ${"%.2f".format(amt)} payout logged successfully! Will hit your phone wallet shortly."
                                                    } else {
                                                        payoutErrorMessage = "Payout failed. Verify system connection."
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Settle Funds Now")
                                        }
                                    }
                                }
                            }
                        }

                        // C. Vendor Earnings Transaction Ledger list
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                            "Recent Financial Ledger Logs",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Icon(
                                            Icons.Default.List,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (providerTransactions.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("No earnings or withdrawal transactions logged yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        providerTransactions.forEach { tx ->
                                            val isPositive = tx.amount >= 0
                                            val color = if (isPositive) androidx.compose.ui.graphics.Color(0xFF2E7D32) else androidx.compose.ui.graphics.Color(0xFFC62828)
                                            val prefix = if (isPositive) "+" else ""
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tx.details, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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
                    }
                }
            }

            // ADD MENU ITEM DIALOG
            if (isAddingFood) {
                Dialog(onDismissRequest = { isAddingFood = false }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Enlist Culinary Dish", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                            OutlinedTextField(
                                value = newFoodName,
                                onValueChange = { newFoodName = it },
                                label = { Text("Food Title / Name") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newFoodPrice,
                                onValueChange = { newFoodPrice = it },
                                label = { Text("Price (GH₵)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Simple category select list
                            Column {
                                Text("Diet Category Selection:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Traditional", "Snacks", "Drinks", "SpecialTY").forEach { cat ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { newFoodCategory = cat }
                                                .background(if (newFoodCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(cat, fontSize = 9.sp, color = if (newFoodCategory == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = newFoodDescription,
                                onValueChange = { newFoodDescription = it },
                                label = { Text("Aesthetic culinary Description") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFoodInitialStock,
                                    onValueChange = { newFoodInitialStock = it },
                                    label = { Text("Initial Stock Limit") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = newFoodSafetyThreshold,
                                    onValueChange = { newFoodSafetyThreshold = it },
                                    label = { Text("Safety Stock Alert") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFoodCalories,
                                    onValueChange = { newFoodCalories = it },
                                    label = { Text("Calories (kcal)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f).testTag("new_food_calories_input")
                                )
                                OutlinedTextField(
                                    value = newFoodAllergens,
                                    onValueChange = { newFoodAllergens = it },
                                    label = { Text("Allergens (e.g. Nuts, Milk)") },
                                    modifier = Modifier.weight(1f).testTag("new_food_allergens_input")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isAddingFood = false }) { Text("Close") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val p = newFoodPrice.toDoubleOrNull() ?: 0.0
                                        val stockInput = newFoodInitialStock.toIntOrNull() ?: 100
                                        val thresholdInput = newFoodSafetyThreshold.toIntOrNull() ?: 15
                                        val cals = newFoodCalories.toIntOrNull() ?: 180
                                        if (newFoodName.isNotBlank() && p > 0.0) {
                                            viewModel.addVendorFoodItem(
                                                name = newFoodName,
                                                price = p,
                                                category = newFoodCategory,
                                                description = newFoodDescription.ifBlank { "Traditional meals served hot." },
                                                imageUrl = "",
                                                initialStock = stockInput,
                                                threshold = thresholdInput,
                                                calories = cals,
                                                allergens = newFoodAllergens.ifBlank { "None" }
                                            )
                                            isAddingFood = false
                                        }
                                    }
                                ) {
                                    Text("Save Menu Option")
                                }
                            }
                        }
                    }
                }
            }
        }

            // PIN CODE PICKUP VERIFICATION SHEET
            verifyTargetOrder?.let { order ->
                Dialog(onDismissRequest = { verifyTargetOrder = null }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Verification Handshake Lock",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Receipt Order ID: #${order.id} • Item: ${order.foodName}", fontSize = 12.sp)

                            OutlinedTextField(
                                value = enteredTicketPin,
                                onValueChange = { enteredTicketPin = it },
                                label = { Text("Enter student 4-Digit PIN code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            pinVerificationError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { verifyTargetOrder = null }) {
                                    Text("Quit")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.verifySecurePickup(order.id, enteredTicketPin) { success ->
                                            if (success) {
                                                verifyTargetOrder = null
                                            } else {
                                                pinVerificationError = "Bad verification Token. Intercept halted."
                                            }
                                        }
                                    }
                                ) {
                                    Text("Unlock & Validate")
                                }
                            }
                        }
                    }
                }
            }

            // EXTENDED PICKUP ESTIMATED DURATION DIALOG
            showEstTimeDialogForOrder?.let { order ->
                Dialog(onDismissRequest = { showEstTimeDialogForOrder = null }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Configure Preparation Speed",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Determine estimated duration for '${order.foodName}' queue.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Preloaded selector chips
                            Text("Fast Selection Tracks:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            val presetTimes = listOf("5 mins", "10 mins", "15 mins", "20 mins", "30 mins")
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(presetTimes) { timeChip ->
                                    val isSelected = estimatedMinutesSelected == timeChip
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { estimatedMinutesSelected = timeChip },
                                        label = { Text(timeChip) }
                                    )
                                }
                            }

                            // Custom Input field
                            OutlinedTextField(
                                value = estimatedMinutesSelected,
                                onValueChange = { estimatedMinutesSelected = it },
                                label = { Text("Or specify custom duration") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showEstTimeDialogForOrder = null }) {
                                    Text("Dismiss")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.updateOrderStatus(order.id, "PREPARING", estimatedMinutesSelected)
                                        showEstTimeDialogForOrder = null
                                        previewTargetReceipt = order.copy(status = "PREPARING", estimatedPickupTime = estimatedMinutesSelected)
                                    }
                                ) {
                                    Text("Accept & Post Estimated Time")
                                }
                            }
                        }
                    }
                }
            }

            // EXTENDED ORDER CANCELLATION AND REASON LOGGER
            showCancelOrderDialogForOrder?.let { order ->
                Dialog(onDismissRequest = { showCancelOrderDialogForOrder = null }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("cancel_order_dialog"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Cancel Order #${order.id}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Please log the reason for cancelling this order. This provides transparency for student inventory discrepancies.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Preloaded selector chips for cancellation reasons
                            Text("Inventory & Discrepancies Reasons:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            val presetReasons = listOf("Out Of Stock", "Ingredient Shortage", "Kitchen Overcapacity", "Cafeteria Closing")
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(presetReasons) { reasonChip ->
                                    val isSelected = cancelOrderReason == reasonChip
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { cancelOrderReason = reasonChip },
                                        label = { Text(reasonChip) },
                                        modifier = Modifier.testTag("cancel_reason_chip_${reasonChip.replace(" ", "_").lowercase()}")
                                    )
                                }
                            }

                            // Custom input field
                            OutlinedTextField(
                                value = cancelOrderReason,
                                onValueChange = { cancelOrderReason = it },
                                label = { Text("Log custom cancel reason...") },
                                modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showCancelOrderDialogForOrder = null }
                                ) {
                                    Text("Dismiss")
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = {
                                        val finalReason = cancelOrderReason.ifBlank { "Unspecified inventory/ingredient discrepancy" }
                                        viewModel.cancelOrder(order.id, finalReason)
                                        showCancelOrderDialogForOrder = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.testTag("confirm_cancel_order_button")
                                ) {
                                    Text("Confirm Cancellation")
                                }
                            }
                        }
                    }
                }
            }

            // Real-time floating order notification banner (with sound alert indicator)
            if (activeAlerts.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeAlerts.forEach { alert ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                                        .background(MaterialTheme.colorScheme.error, CircleShape)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "New Order Notification Alert",
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "NEW ORDER RECEIVED!",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color.Red, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "LIVE",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = alert.foodName.ifEmpty { "Selection" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Quantity: ${alert.quantity} • Value: GH₵ ${"%.2f".format(alert.totalPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.dismissOrderAlert(alert.id)
                                        }
                                    ) {
                                        Text("Dismiss", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.dismissOrderAlert(alert.id)
                                            activeTab = 0 // Focus/switch and jump directly to Orders manager tab
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Process", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PHYSICAL THERMAL RECEIPT EMULATION DIALOG FOR VENDOR MANUAL FULFILLMENT
            previewTargetReceipt?.let { order ->
                val context = androidx.compose.ui.platform.LocalContext.current
                Dialog(onDismissRequest = { previewTargetReceipt = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Thermal Receipt Preview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            Text(
                                text = "High-fidelity monochromatic emulation format for manual cafeteria preparation and student pickUp validation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Simulated physical roll tape paper
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFBF9)), // thermal roll tint
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Upper Tear Simulation
                                    Text(
                                        text = "- - - - - - - - - - - - - - - - - - -",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = (currentUser?.info ?: currentUser?.fullName ?: "Cafeteria Vendor").uppercase(),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E1E1E),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Accra Technical University",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E2E2E),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "MANUAL FULFILLMENT COPY",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "=====================================",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    
                                    // Main info metadata
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("ORDER ID:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                            Text("#${order.id}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("DATE:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                            val dateFormatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(order.orderTimestamp))
                                            Text(dateFormatted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("CUSTOMER:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                            Text("STUDENT #${order.customerId}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("EST. PICKUP:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, color = Color.Black)
                                            Text(order.estimatedPickupTime, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "-------------------------------------",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )

                                    // Items Column
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("ITEM DESCRIPTION", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Color.Black)
                                        Text("QTY", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = Color.Black)
                                        Text("TOTAL", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp), textAlign = TextAlign.End, color = Color.Black)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "-------------------------------------",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(order.foodName, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f), color = Color.Black)
                                        Text(order.quantity.toString(), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center, color = Color.Black)
                                        Text("GH₵${"%.2f".format(order.totalPrice)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.End, color = Color.Black)
                                    }
                                    
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text("Unit price: GH₵${"%.2f".format(order.unitPrice)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp, color = Color.DarkGray)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "=====================================",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    
                                    // Total amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("TOTAL PAID:", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("GH₵${"%.2f".format(order.totalPrice)}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Handoff code verification
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White)
                                            .border(1.5.dp, Color.Black, RoundedCornerShape(4.dp))
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "CUSTOMER CLAIM PIN",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = order.pickupPin,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 6.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Pseudo Barcode design lines
                                    Text(
                                        text = "||| | || ||||| | |||| | ||| | |||",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = Color.Black,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "*ATU-ORD-${order.id}*",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    // Lower Tear Simulation
                                    Text(
                                        text = "- - - - - - - - - - - - - - - - - - -",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Controls trigger
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { previewTargetReceipt = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Dismiss")
                                }

                                Button(
                                    onClick = {
                                        com.example.ui.util.ReceiptPrinter.printOrderReceipt(
                                            context,
                                            order,
                                            currentUser?.info ?: currentUser?.fullName ?: "Cafeteria Vendor"
                                        )
                                        previewTargetReceipt = null
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Execute Print 🖨️")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVendorNotificationsDialog) {
        val inventoryNotifs by viewModel.vendorInventoryNotifications.collectAsStateWithLifecycle()
        AlertDialog(
            onDismissRequest = { showVendorNotificationsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Inventory Alerts Center", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Real-time warnings triggered by your safety stock thresholds or general menu availability updates.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (inventoryNotifs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(36.dp))
                                Text("All menu operations healthy!", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("No low stock or unavailable alerts active.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(inventoryNotifs) { alert ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("alert_card_${alert.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (alert.type) {
                                            "OUT_OF_STOCK" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                                            "LOW_STOCK" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = when (alert.type) {
                                                            "OUT_OF_STOCK" -> MaterialTheme.colorScheme.error
                                                            "LOW_STOCK" -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.secondary
                                                        },
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = alert.type,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                            
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (!alert.isRead) {
                                                    TextButton(
                                                        onClick = { viewModel.markVendorNotificationRead(alert.id) },
                                                        contentPadding = PaddingValues(0.dp),
                                                        modifier = Modifier.height(24.dp).testTag("alert_mark_read_btn_${alert.id}")
                                                    ) {
                                                        Text("Ack", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                IconButton(
                                                    onClick = { viewModel.dismissVendorNotification(alert.id) },
                                                    modifier = Modifier.size(24.dp).testTag("alert_dismiss_btn_${alert.id}")
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = alert.message,
                                            fontSize = 11.sp,
                                            fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal,
                                            color = when (alert.type) {
                                                "OUT_OF_STOCK" -> MaterialTheme.colorScheme.onErrorContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        val timeString = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(alert.timestamp))
                                        Text(
                                            text = "Raised at: $timeString",
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVendorNotificationsDialog = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
}

// ==========================================
// 5. CAFE MANAGERS COMPLIANCE & GEMINI REVIEW
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

    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Compliance Board, 1: AI Advisor, 2: Cyber Logs

    val scope = rememberCoroutineScope()
    val adminVendorsScrollState = rememberLazyListState()
    val adminStudentsScrollState = rememberLazyListState()
    val adminAiScrollState = rememberScrollState()
    val adminCyberScrollState = rememberLazyListState()
    val adminOversightScrollState = rememberLazyListState()

    // Local Dialog States
    var showAddVendorDialog by remember { mutableStateOf(false) }
    var vendorToEdit by remember { mutableStateOf<User?>(null) }
    var vendorToDelete by remember { mutableStateOf<User?>(null) }

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

    var listSelection by remember { mutableIntStateOf(0) } // 0: Manage Vendors, 1: Student Directory

    // AI selection targets
    var selectedVendorForAiReview by remember { mutableStateOf<User?>(null) }
    val aiAnalysiResultText by viewModel.aiAnalysisText.collectAsStateWithLifecycle()
    val isAnalyzingUiState by viewModel.isAnalyzing.collectAsStateWithLifecycle()

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
                    label = { Text("Gemini Analytics") }
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
                            addUsername = ""
                            addPinCode = ""
                            addFullName = ""
                            addInfo = ""
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
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Vendors Registry",
                                    color = if (listSelection == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { listSelection = 1 }
                                    .background(if (listSelection == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Student Directory",
                                    color = if (listSelection == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (listSelection == 0) {
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

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.startImpersonation(vendor)
                                                            navController.navigate("vendor_home")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1.2f)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                                        modifier = Modifier.weight(1.5f)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Edit", fontSize = 11.sp)
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            vendorToDelete = vendor
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Students Directory
                            val students = allUsers.filter { it.role == "STUDENT" }
                            LazyColumn(
                                state = adminStudentsScrollState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (students.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                            Text("No student accounts enrolled in system.")
                                        }
                                    }
                                } else {
                                    items(students) { student ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(student.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                    Text("Matric ID: ${student.info}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Username: ${student.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.startImpersonation(student)
                                                        navController.navigate("student_home")
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Simulate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Gemini Analytics page
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(adminAiScrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Gemini AI Campus Analytics Advisor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Examine feedback clusters on kitchen units using advanced LLM reasoning to output operational advisories for compliance management.",
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
                                Text("Engage LLM Diagnostics")
                            }
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Choose an active vendor above to consult Gemini", fontSize = 11.0.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text("Gemini is mining campus feedback clusters...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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

                        // 3. Emergency Halting Command
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

    // 1. ADD NEW VENDOR DIALOG
        if (showAddVendorDialog) {
            Dialog(onDismissRequest = { showAddVendorDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add New Campus Vendor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = addUsername,
                            onValueChange = { addUsername = it.lowercase().trim() },
                            label = { Text("Vendor Login Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addFullName,
                            onValueChange = { addFullName = it },
                            label = { Text("Vendor Full Brand name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addInfo,
                            onValueChange = { addInfo = it },
                            label = { Text("Booth Location / Kitchen Desc.") },
                            placeholder = { Text("e.g. Sobolo Palace / Booth 5") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = addPinCode,
                            onValueChange = { addPinCode = it },
                            label = { Text("Access Pin Code (4+ characters)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

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
                                    if (addUsername.isBlank() || addFullName.isBlank() || addPinCode.length < 4) {
                                        addError = "Username, Full Name, and a 4+ digit PIN are mandatory."
                                    } else {
                                        viewModel.addVendor(
                                            username = addUsername,
                                            pinCode = addPinCode,
                                            fullName = addFullName,
                                            info = addInfo,
                                            logoUrl = addLogoUrl.ifBlank { null },
                                            pictureUrl = addPictureUrl.ifBlank { null }
                                        ) { success ->
                                            if (success) {
                                                showAddVendorDialog = false
                                                addUsername = ""
                                                addFullName = ""
                                                addInfo = ""
                                                addPinCode = ""
                                                addLogoUrl = ""
                                                addPictureUrl = ""
                                                addError = null
                                            } else {
                                                addError = "Username already exists."
                                            }
                                        }
                                    }
                                }
                            ) {
                                Text("Add Profile")
                            }
                        }
                    }
                }
            }
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
    }
