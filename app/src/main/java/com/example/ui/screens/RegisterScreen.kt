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
