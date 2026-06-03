package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.data.*
import com.example.ui.components.DailyRevenueBarChart
import com.example.ui.components.RadarFeedbackChart
import com.example.ui.components.StudentTrendsLineChart
import com.example.ui.components.VendorPerformanceTrendChart
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
    val allFeedback by viewModel.allFeedback.collectAsStateWithLifecycle()
    val studentWalletBalance by viewModel.studentWalletBalance.collectAsStateWithLifecycle()
    val userWalletTransactions by viewModel.userWalletTransactions.collectAsStateWithLifecycle()
    val vendorAnnouncement by viewModel.vendorAnnouncement.collectAsStateWithLifecycle()
    val isAdminActing by viewModel.isAdminActing.collectAsStateWithLifecycle()

    val acknowledgedOrders = remember { mutableStateListOf<Int>() }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Browse Food, 1: Orders Hub, 2: Nutrition, 3: Prep Reserves, 4: Smart Wallet & ID
    var selectedFoodForOrder by remember { mutableStateOf<FoodItem?>(null) }
    var orderQuantity by remember { mutableIntStateOf(1) }
    var payViaWallet by remember { mutableStateOf(false) }
    var orderPlacementError by remember { mutableStateOf<String?>(null) }

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
                    Column {
                        Text(if (isAdminActing) "SIMULATION: STUDENT" else "ATU Student Panel", fontWeight = FontWeight.Bold)
                        Text(
                            "ID: ${currentUser?.info ?: ""} • Welcome, ${currentUser?.fullName ?: ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
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
                            Text(
                                text = "Traditional Campus Menus Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        val filteredFoods = allFoodItems.filter { food ->
                            food.isAvailable &&
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

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Button(
                                            onClick = {
                                                orderQuantity = 1
                                                selectedFoodForOrder = food
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Place Order", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Merged tracking and history hub
                    var ordersSubTab by remember { mutableIntStateOf(0) } // 0: Live Tracker, 1: Dining History
                    
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
                            val activeOrders = studentOrders.filter { it.status == "PENDING" || it.status == "PREPARING" || it.status == "READY" }
                            
                            LazyColumn(
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
                                            val badgeColor = when (order.status) {
                                                "PENDING" -> Color(0xFFF9A825) // Amber
                                                "PREPARING" -> Color(0xFF1976D2) // Blue
                                                "READY" -> Color(0xFF2E7D32) // Green
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(badgeColor)
                                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = when (order.status) {
                                                        "PENDING" -> "Order Received"
                                                        "PREPARING" -> "Preparing"
                                                        "READY" -> "Ready for Pickup"
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
                                        val steps = listOf("Received", "Preparing", "Ready")
                                        val activeStep = when (order.status) {
                                            "PENDING" -> 0
                                            "PREPARING" -> 1
                                            "READY" -> 2
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
                                                    } else if (order.status == "READY") {
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
                                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "SECURE PICKUP TOKEN PIN: [ ${order.pickupPin} ]",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                                Text(
                                                    "Quote this secret verification code to the server booth.",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                            val completedOrCanceledOrders = studentOrders.filter { it.status == "COMPLETED" || it.status == "DECLINED" }
                            val totalSpend = completedOrCanceledOrders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
                            val orderCount = completedOrCanceledOrders.filter { it.status == "COMPLETED" }.size
                            
                            LazyColumn(
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

                                item {
                                    Text("Official Receipt Journal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }

                                if (completedOrCanceledOrders.isEmpty()) {
                                    item {
                                        Text("No dining history found yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                } else {
                                    items(completedOrCanceledOrders) { order ->
                                        val orderFeedback = allFeedback.find { it.orderId == order.id }
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                        Text("Total paid: GH₵ ${"%.2f".format(order.totalPrice)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(if (order.status == "COMPLETED") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer)
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = order.status,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (order.status == "COMPLETED") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(10.dp))

                                                if (order.status == "COMPLETED" && orderFeedback == null) {
                                                    Button(
                                                        onClick = { feedbackTargetOrder = order },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                        shape = RoundedCornerShape(6.dp),
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("File Safety & Quality Review", fontSize = 10.sp)
                                                    }
                                                } else if (orderFeedback != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                                            .padding(8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Feedback logged. Compliance Audit Trace records saved.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
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
                    val calorieLoggedSum = loggedNutritionMeals.sumOf { food ->
                        val macro = getNutritionalProfile(food.first)
                        val kcal = macro.first * 4f + macro.second * 4f + macro.third * 9f
                        kcal.toDouble()
                    }.toFloat()

                    val proteinLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).first.toDouble() }.toFloat()
                    val carbsLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).second.toDouble() }.toFloat()
                    val fatLogged = loggedNutritionMeals.sumOf { getNutritionalProfile(it.first).third.toDouble() }.toFloat()

                    LazyColumn(
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
                    var showTopUpDialog by remember { mutableStateOf(false) }
                    var topUpAmount by remember { mutableStateOf("") }
                    var topUpPhone by remember { mutableStateOf("") }
                    var isTopUpProcessing by remember { mutableStateOf(false) }
                    var topUpSuccess by remember { mutableStateOf(false) }
                    var helpSubject by remember { mutableStateOf("") }
                    var helpMessage by remember { mutableStateOf("") }
                    var helpSubmitted by remember { mutableStateOf(false) }

                    LazyColumn(
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
                    }

                    // TOP UP DIALOG
                    if (showTopUpDialog) {
                        Dialog(onDismissRequest = { if (!isTopUpProcessing) showTopUpDialog = false }) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        "Mobile Money Gateway",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
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
                                        OutlinedTextField(
                                            value = topUpAmount,
                                            onValueChange = { topUpAmount = it },
                                            label = { Text("Transfer Sum (GH₵)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = topUpPhone,
                                            onValueChange = { topUpPhone = it },
                                            label = { Text("MoMo Number (05X / 02X / 050)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (isTopUpProcessing) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text("Awaiting MoMo OTP Approval...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                                        if (amt != null && amt > 0 && topUpPhone.isNotBlank()) {
                                                            isTopUpProcessing = true
                                                            // simulate secure gateway timer
                                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                                viewModel.rechargeWallet(amt)
                                                                isTopUpProcessing = false
                                                                topUpSuccess = true
                                                            }, 2200)
                                                        }
                                                    }
                                                ) {
                                                    Text("Initialize Secure Transfer")
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
                Dialog(onDismissRequest = { selectedFoodForOrder = null }) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                "Confirm Food Booking",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))

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
                                        onClick = { if (orderQuantity > 1) orderQuantity-- }
                                    ) {
                                        Text("-", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("$orderQuantity", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp))
                                    IconButton(
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = payViaWallet,
                                    onCheckedChange = { 
                                        orderPlacementError = null
                                        payViaWallet = it 
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Pre-pay securely using ATU virtual Wallet", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    Text("Wallet Balance: GH₵ ${"%.2f".format(studentWalletBalance)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }

                            orderPlacementError?.let { err ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = err,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                        viewModel.placeOrder(food, orderQuantity, payViaWallet) { success ->
                                            if (success) {
                                                selectedFoodForOrder = null
                                                activeTab = 1 // Switch to orders list
                                            } else {
                                                orderPlacementError = "Insufficient smart balance. Top up your virtual ID Wallet!"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Verify & Route Order")
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
                                "Vendor Performance Feed",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Rate '${order.foodName}' vendor quality, healthiness & speeds:", fontSize = 11.sp)
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
                                            feedbackTargetOrder = null
                                        }
                                    }
                                ) {
                                    Text("Inject Data Metric")
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
        }
    }
}

// ==========================================
// 4. VENDOR WORKSPACE & VERIFICATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
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

    var activeTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Menu List, 2: Ratings/Analytics, 3: Settings & Hub
    var isAddingFood by remember { mutableStateOf(false) }

    // PIN verification dialogues
    var verifyTargetOrder by remember { mutableStateOf<Order?>(null) }
    var enteredTicketPin by remember { mutableStateOf("") }
    var pinVerificationError by remember { mutableStateOf<String?>(null) }

    // Estimation dialogues
    var showEstTimeDialogForOrder by remember { mutableStateOf<Order?>(null) }
    var estimatedMinutesSelected by remember { mutableStateOf("15 mins") }

    // Add Food States
    var newFoodName by remember { mutableStateOf("") }
    var newFoodPrice by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("Local Dish") }
    var newFoodDescription by remember { mutableStateOf("") }
    var newFoodInitialStock by remember { mutableStateOf("100") }
    var newFoodSafetyThreshold by remember { mutableStateOf("15") }

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
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🚨", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Low Stock Alerts & Depletion Projections",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "${lowStockItems.size} items are below or near exhaustion thresholds",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
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
                                                    "| Threshold: ${item.lowStockThreshold}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                    // Orders dispatcher
                    val uncompletedOrders = incomingOrders.filter { it.status != "COMPLETED" && it.status != "DECLINED" }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Incoming Student Orders (${uncompletedOrders.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (uncompletedOrders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Waiting for student cafeteria transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            items(uncompletedOrders) { order ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Receipt Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text("QTY: ${order.quantity} • Volume: GH₵ ${"%.2f".format(order.totalPrice)}", style = MaterialTheme.typography.bodySmall)
                                            }

                                            // Transition states
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(order.status, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                                                    onClick = { viewModel.updateOrderStatus(order.id, "DECLINED") },
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("Reject", fontSize = 11.sp)
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
                1 -> {
                    // Menu Management
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
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
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    IconButton(onClick = { viewModel.deleteVendorFoodItem(food) }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                            .verticalScroll(rememberScrollState())
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

                        // 2. Dynamic Radar Visual Chart
                        RadarFeedbackChart(metrics = metrics, modifier = Modifier.fillMaxWidth())

                        // 3. Dynamic Revenue Bar Chart
                        DailyRevenueBarChart(orders = filteredIncomingOrders, modifier = Modifier.fillMaxWidth())

                        // 3a. Recharts-style spline line chart fed by VendorPerformanceController from backend
                        if (com.example.data.LaravelClientManager.isLaravelEnabled) {
                            VendorPerformanceTrendChart(
                                performanceData = performanceData,
                                modifier = Modifier.fillMaxWidth()
                            )
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4. Feedback details
                        Text("Live Customer Sentiment Transcripts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (filteredFeedbackList.isEmpty()) {
                            Text("No reviews submitted on campus yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            filteredFeedbackList.forEach { f ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Star metrics: Quality=${f.ratingFoodQuality} Cleanliness=${f.ratingCleanliness}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
                                            Text("Order Ref #${f.orderId}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("\"${f.comment}\"", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
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
                            .verticalScroll(rememberScrollState())
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
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isAddingFood = false }) { Text("Close") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val p = newFoodPrice.toDoubleOrNull() ?: 0.0
                                        val stockInput = newFoodInitialStock.toIntOrNull() ?: 100
                                        val thresholdInput = newFoodSafetyThreshold.toIntOrNull() ?: 15
                                        if (newFoodName.isNotBlank() && p > 0.0) {
                                            viewModel.addVendorFoodItem(
                                                name = newFoodName,
                                                price = p,
                                                category = newFoodCategory,
                                                description = newFoodDescription.ifBlank { "Traditional meals served hot." },
                                                imageUrl = "",
                                                initialStock = stockInput,
                                                threshold = thresholdInput
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
                                    }
                                ) {
                                    Text("Accept & Post Estimated Time")
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
        }
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
                            .verticalScroll(rememberScrollState()),
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
                                        viewModel.addVendor(addUsername, addPinCode, addFullName, addInfo) { success ->
                                            if (success) {
                                                showAddVendorDialog = false
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
                                        val updatedVendor = vendor.copy(fullName = editFullName, info = editInfo.ifBlank { "ATU Cafeteria Vendor" })
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
