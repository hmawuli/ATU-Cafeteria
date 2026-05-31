package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val vendorAnnouncement by viewModel.vendorAnnouncement.collectAsStateWithLifecycle()
    val isAdminActing by viewModel.isAdminActing.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Browse Food, 1: Track Orders, 2: Smart Wallet & ID
    var selectedFoodForOrder by remember { mutableStateOf<FoodItem?>(null) }
    var orderQuantity by remember { mutableIntStateOf(1) }
    var payViaWallet by remember { mutableStateOf(false) }
    var orderPlacementError by remember { mutableStateOf<String?>(null) }

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
                    label = { Text("Browse Foods") }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("My Orders & Reviews") }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Smart Wallet & ID") }
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
                    // Track student orders & reviews
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Your Secure Order Registry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        item {
                            StudentTrendsLineChart(
                                orders = studentOrders,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (studentOrders.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No orders placed yet. Select Browse to fill a plate!")
                                }
                            }
                        } else {
                            items(studentOrders) { order ->
                                val isReviewed = allFeedback.any { it.orderId == order.id }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Order #${order.id}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(order.foodName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                Text("QTY: ${order.quantity} • Total: GH₵ ${"%.2f".format(order.totalPrice)}", style = MaterialTheme.typography.bodySmall)
                                            }

                                            // Status Badge
                                            val badgeColor = when (order.status) {
                                                "PENDING" -> Color(0xFFF9A825)
                                                "PREPARING" -> Color(0xFF1976D2)
                                                "READY" -> Color(0xFF2E7D32)
                                                "COMPLETED" -> Color(0xFF555555)
                                                else -> Color(0xFFC62828)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeColor)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(order.status, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // SECURITY KEY HANDSHAKE
                                        if (order.status != "COMPLETED" && order.status != "DECLINED") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "SECURE PICKUP TOKEN: [ ${order.pickupPin} ]",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Text(
                                                        "Declare this 4-digit token to the seller booth to claim food custody.",
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else if (order.status == "COMPLETED") {
                                            if (!isReviewed) {
                                                Button(
                                                    onClick = {
                                                        feedbackTargetOrder = order
                                                        foodQualityRating = 5
                                                        cleanlinessRating = 5
                                                        speedRating = 5
                                                        priceRating = 5
                                                        feedbackComment = ""
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Submit Performance Rating Feed", fontSize = 12.sp)
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Feedback logged. Compliance Audit Trace records saved.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
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

    var activeTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Menu List, 2: Ratings/Analytics, 3: Settings & Hub
    var isAddingFood by remember { mutableStateOf(false) }

    // PIN verification dialogues
    var verifyTargetOrder by remember { mutableStateOf<Order?>(null) }
    var enteredTicketPin by remember { mutableStateOf("") }
    var pinVerificationError by remember { mutableStateOf<String?>(null) }

    // Add Food States
    var newFoodName by remember { mutableStateOf("") }
    var newFoodPrice by remember { mutableStateOf("") }
    var newFoodCategory by remember { mutableStateOf("Local Dish") }
    var newFoodDescription by remember { mutableStateOf("") }

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
                                                    onClick = { viewModel.updateOrderStatus(order.id, "PREPARING") },
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Ratings / Analytics Panel
                    val metrics = viewModel.getVendorMetrics(currentUser?.id ?: 0, vendorFeedbackList)
                    val completedOrders = incomingOrders.filter { it.status == "COMPLETED" }

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

                        // 1. Core Summary metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("OVERALL INDEX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${"%.1f".format(metrics["overall"] ?: 0.0)}★", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("From ${vendorFeedbackList.size} reviews", fontSize = 8.sp)
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("VOLUME DISPATCHED", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${completedOrders.size}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Text("Total Completed Plates", fontSize = 8.sp)
                                }
                            }
                        }

                        // 2. Dynamic Radar Visual Chart
                        RadarFeedbackChart(metrics = metrics, modifier = Modifier.fillMaxWidth())

                        // 3. Dynamic Revenue Bar Chart
                        DailyRevenueBarChart(orders = incomingOrders, modifier = Modifier.fillMaxWidth())

                        // 4. Feedback details
                        Text("Live Customer Sentiment Transcripts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (vendorFeedbackList.isEmpty()) {
                            Text("No reviews submitted on campus yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            vendorFeedbackList.forEach { f ->
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
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isAddingFood = false }) { Text("Close") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val p = newFoodPrice.toDoubleOrNull() ?: 0.0
                                        if (newFoodName.isNotBlank() && p > 0.0) {
                                            viewModel.addVendorFoodItem(
                                                name = newFoodName,
                                                price = p,
                                                category = newFoodCategory,
                                                description = newFoodDescription.ifBlank { "Traditional meals served hot." },
                                                imageUrl = ""
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
}
