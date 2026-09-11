package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.data.FoodItem
import com.example.data.Order
import com.example.data.User
import com.example.data.WalletTransaction
import com.example.ui.util.SnackbarManager
import com.example.ui.viewmodel.CafeteriaViewModel
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. CAMPUS USER MONITORING STATS BANNER
// ==========================================

@Composable
fun CampusUserMonitoringStatsBanner(
    allUsers: List<User>,
    allOrders: List<Order>,
    modifier: Modifier = Modifier
) {
    val totalStudents = remember(allUsers) { allUsers.count { it.role == "STUDENT" } }
    val totalVendors = remember(allUsers) { allUsers.count { it.role == "VENDOR" } }
    val totalWalletsValue = remember(allUsers) { allUsers.filter { it.role == "STUDENT" }.sumOf { it.balance } }
    val totalOrdersCount = remember(allOrders) { allOrders.size }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Live Campus User Telemetry",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                ) {
                    Text(
                        "LIVE MONITORING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Students
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Students", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalStudents", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Vendors
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0B2).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Vendors", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalVendors", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }

                // Wallets Pool
                Card(
                    modifier = Modifier.weight(1.3f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Student Balances", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("GH₵ ${"%.2f".format(totalWalletsValue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                }

                // Total Orders
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Orders", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$totalOrdersCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. USER ACTIVITY & BEHAVIOR MONITORING DIALOG
// ==========================================

@Composable
fun UserActivityMonitoringDialog(
    user: User,
    allOrders: List<Order>,
    allWalletTransactions: List<WalletTransaction>,
    allFoodItems: List<FoodItem>,
    allVendors: List<User>,
    viewModel: CafeteriaViewModel,
    navController: NavController,
    onDismiss: () -> Unit,
    onAdjustWallet: (User) -> Unit,
    onResetPin: (User) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val userOrders = remember(allOrders, user.id) {
        allOrders.filter { it.customerId == user.id }
    }
    val userTransactions = remember(allWalletTransactions, user.id) {
        allWalletTransactions.filter { it.userId == user.id }
    }

    val totalSpentLifetime = remember(userOrders) {
        userOrders.filter { it.status != "CANCELLED" && it.status != "DECLINED" }.sumOf { it.totalPrice }
    }

    // Particular foods this student buys more
    val favoriteFoods = remember(userOrders) {
        userOrders.filter { it.status != "CANCELLED" && it.status != "DECLINED" }
            .groupBy { it.foodName }
            .map { (name, orders) ->
                val qty = orders.sumOf { it.quantity }
                val spend = orders.sumOf { it.totalPrice }
                val vendorId = orders.firstOrNull()?.vendorId ?: 0
                val vendorName = allVendors.find { it.id == vendorId }?.fullName ?: "Campus Vendor"
                Triple(name, qty, spend) to vendorName
            }
            .sortedByDescending { it.first.second } // Sort by quantity ordered
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0: Overview & Foods, 1: Orders Log, 2: Wallet Ledger

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (user.role) {
                                "STUDENT" -> MaterialTheme.colorScheme.primary
                                "VENDOR" -> Color(0xFFE65100)
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.fullName.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
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
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    "@${user.username} • ${user.role}",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Sub-tabs navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Overview & Diet", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Orders (${userOrders.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Wallet Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(14.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            // User Profile Telemetry & Foods Consumed
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Smart Wallet & Activity Metrics
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Wallet Balance", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "GH₵ ${"%.2f".format(user.balance)}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Total Food Spend", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "GH₵ ${"%.2f".format(totalSpentLifetime)}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Orders Placed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    "${userOrders.size}",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // KYC & Identity Info Card
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("Account & Identity Information", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Student / Staff ID:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(user.student_staff_id ?: user.info.ifBlank { "N/A" }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Email Address:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(user.email ?: "Not provided", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Phone Number:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(user.telephone ?: "Not provided", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            }
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Account Status:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    if (user.isOpen) "Active & Verified" else "Suspended / Frozen",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (user.isOpen) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }

                                // MOST PURCHASED / FAVORITE FOODS SECTION
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "Foods Purchased Most by this Student",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    "${favoriteFoods.size} unique dishes",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (favoriteFoods.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "This student has not placed any meal orders yet.",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            } else {
                                                favoriteFoods.forEachIndexed { index, (itemData, vendorName) ->
                                                    val (foodName, qty, spend) = itemData
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                if (index == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                                else Color.Transparent,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = when (index) {
                                                                    0 -> Color(0xFFFFD700)
                                                                    1 -> Color(0xFFC0C0C0)
                                                                    2 -> Color(0xFFCD7F32)
                                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                                },
                                                                modifier = Modifier.size(22.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        "${index + 1}",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (index < 3) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }

                                                            Column {
                                                                Text(
                                                                    foodName,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 12.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    vendorName,
                                                                    fontSize = 10.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                "$qty orders ($qty plates)",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                            Text(
                                                                "GH₵ ${"%.2f".format(spend)} total",
                                                                fontSize = 10.sp,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                    if (index < favoriteFoods.size - 1) {
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Order History Log
                            if (userOrders.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No orders recorded for this user.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(userOrders) { order ->
                                        val vendorName = allVendors.find { it.id == order.vendorId }?.fullName ?: "Campus Vendor"
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Order #${order.id} • $vendorName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = when (order.status) {
                                                            "COMPLETED" -> Color(0xFFE8F5E9)
                                                            "READY" -> Color(0xFFE3F2FD)
                                                            "PREPARING" -> Color(0xFFFFF3E0)
                                                            "CANCELLED", "DECLINED" -> Color(0xFFFFEBEE)
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                    ) {
                                                        Text(
                                                            order.status,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when (order.status) {
                                                                "COMPLETED" -> Color(0xFF2E7D32)
                                                                "READY" -> Color(0xFF1976D2)
                                                                "PREPARING" -> Color(0xFFE65100)
                                                                "CANCELLED", "DECLINED" -> Color(0xFFC62828)
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "${order.quantity}x ${order.foodName}",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        "GH₵ ${"%.2f".format(order.totalPrice)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        dateFormat.format(Date(order.orderTimestamp)),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        "PIN: ${order.pickupPin}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Wallet Transaction Ledger
                            if (userTransactions.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No wallet transactions recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(userTransactions) { tx ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            if (tx.amount >= 0) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                            contentDescription = null,
                                                            tint = if (tx.amount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(tx.type, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    }
                                                    Text(tx.details, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(dateFormat.format(Date(tx.timestamp)), fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                                }

                                                Text(
                                                    "${if (tx.amount >= 0) "+" else ""}GH₵ ${"%.2f".format(tx.amount)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (tx.amount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Footer
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onAdjustWallet(user)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjust Funds", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onResetPin(user)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset PIN", fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            viewModel.startImpersonation(user)
                            if (user.role == "VENDOR") {
                                navController.navigate("vendor_home")
                            } else {
                                navController.navigate("student_home")
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. POPULAR FOODS & STUDENT CONSUMPTION TRENDS
// ==========================================

data class PopularFoodMetric(
    val foodId: Int,
    val foodName: String,
    val vendorId: Int,
    val vendorName: String,
    val category: String,
    val price: Double,
    val totalSoldQuantity: Int,
    val totalRevenue: Double,
    val orderCount: Int,
    val currentStock: Int,
    val isAvailable: Boolean
)

@Composable
fun PopularFoodsAnalyticsSection(
    allOrders: List<Order>,
    allFoodItems: List<FoodItem>,
    allVendors: List<User>,
    modifier: Modifier = Modifier
) {
    var categoryFilter by remember { mutableStateOf("ALL") }
    var timeFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, WEEK
    var sortBy by remember { mutableStateOf("UNITS") } // UNITS, REVENUE, ORDERS

    val currentTime = System.currentTimeMillis()

    // 1. Filter orders based on student purchases & time horizon
    val eligibleOrders = remember(allOrders, timeFilter) {
        allOrders.filter { order ->
            val validStatus = order.status != "CANCELLED" && order.status != "DECLINED"
            val matchesTime = when (timeFilter) {
                "TODAY" -> (currentTime - order.orderTimestamp) <= 24 * 60 * 60 * 1000L
                "WEEK" -> (currentTime - order.orderTimestamp) <= 7 * 24 * 60 * 60 * 1000L
                else -> true
            }
            validStatus && matchesTime
        }
    }

    // 2. Aggregate sales per food item
    val foodStats = remember(eligibleOrders, allFoodItems, allVendors, categoryFilter, sortBy) {
        val foodMap = allFoodItems.associateBy { it.id }
        val vendorMap = allVendors.associateBy { it.id }

        val ordersByFood = eligibleOrders.groupBy { it.foodItemId }

        val list = ordersByFood.map { (foodId, orders) ->
            val food = foodMap[foodId]
            val vendor = vendorMap[orders.firstOrNull()?.vendorId ?: food?.vendorId ?: 0]
            val name = food?.name ?: orders.firstOrNull()?.foodName ?: "Cafeteria Dish"
            val vendorName = vendor?.fullName ?: "Campus Vendor"
            val category = food?.category ?: "Local Dishes"
            val price = food?.price ?: orders.firstOrNull()?.unitPrice ?: 0.0
            val qty = orders.sumOf { it.quantity }
            val revenue = orders.sumOf { it.totalPrice }
            val count = orders.size
            val stock = food?.currentStock ?: 100
            val isAvail = food?.isAvailable ?: true

            PopularFoodMetric(
                foodId = foodId,
                foodName = name,
                vendorId = vendor?.id ?: 0,
                vendorName = vendorName,
                category = category,
                price = price,
                totalSoldQuantity = qty,
                totalRevenue = revenue,
                orderCount = count,
                currentStock = stock,
                isAvailable = isAvail
            )
        }.filter {
            if (categoryFilter == "ALL") true
            else it.category.equals(categoryFilter, ignoreCase = true)
        }

        when (sortBy) {
            "REVENUE" -> list.sortedByDescending { it.totalRevenue }
            "ORDERS" -> list.sortedByDescending { it.orderCount }
            else -> list.sortedByDescending { it.totalSoldQuantity }
        }
    }

    val totalUnitsAll = remember(foodStats) { foodStats.sumOf { it.totalSoldQuantity }.coerceAtLeast(1) }
    val totalRevenueAll = remember(foodStats) { foodStats.sumOf { it.totalRevenue } }
    val maxUnits = remember(foodStats) { foodStats.maxOfOrNull { it.totalSoldQuantity }?.coerceAtLeast(1) ?: 1 }
    val topFood = remember(foodStats) { foodStats.firstOrNull() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Student Food Consumption & Popularity Leaderboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "Real-time breakdown of the exact foods and dishes ATU students are ordering the most across all cafeteria vendors.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Top KPI cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // #1 Most Ordered
                    Card(
                        modifier = Modifier.weight(1.4f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🥇 #1 Top Campus Dish", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                topFood?.foodName ?: "None ordered yet",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (topFood != null) "${topFood.totalSoldQuantity} plates sold (${"%.0f".format((topFood.totalSoldQuantity.toDouble() / totalUnitsAll) * 100)}% of campus orders)" else "Awaiting orders",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Total Meals Volume
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Total Meals Sold", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$totalUnitsAll",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "GH₵ ${"%.2f".format(totalRevenueAll)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }

        // Filters: Time Horizon & Sort By
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TIME HORIZON:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL" to "All-Time", "TODAY" to "Today (24h)", "WEEK" to "7 Days").forEach { (key, label) ->
                        FilterChip(
                            selected = timeFilter == key,
                            onClick = { timeFilter = key },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SORT LEADERBOARD:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("UNITS" to "Most Sold (Plates)", "REVENUE" to "Highest Revenue", "ORDERS" to "Order Count").forEach { (key, label) ->
                        FilterChip(
                            selected = sortBy == key,
                            onClick = { sortBy = key },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All Dishes (${foodStats.size})",
                    "Local Dishes" to "🍛 Local Dishes",
                    "Fast Food" to "🍔 Fast Food",
                    "Breakfast" to "🥞 Breakfast",
                    "Drinks" to "🥤 Drinks",
                    "Snacks" to "🍰 Snacks"
                ).forEach { (cat, label) ->
                    FilterChip(
                        selected = categoryFilter.equals(cat, ignoreCase = true),
                        onClick = { categoryFilter = cat },
                        label = { Text(label, fontSize = 10.sp) }
                    )
                }
            }
        }

        // Ranked Food Items Leaderboard
        if (foodStats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No dish sales matching the selected filters.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                foodStats.forEachIndexed { index, item ->
                    val percentageOfTotal = (item.totalSoldQuantity.toDouble() / totalUnitsAll * 100).coerceIn(0.0, 100.0)
                    val progressRatio = (item.totalSoldQuantity.toFloat() / maxUnits).coerceIn(0f, 1f)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (index == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Top row: Rank badge, Name, Category, Price
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
                                    Surface(
                                        shape = CircleShape,
                                        color = when (index) {
                                            0 -> Color(0xFFFFD700)
                                            1 -> Color(0xFFC0C0C0)
                                            2 -> Color(0xFFCD7F32)
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "#${index + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (index < 3) Color.Black else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            item.foodName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "Vendor: ${item.vendorName} • ${item.category}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        "GH₵ ${"%.2f".format(item.price)}/ea",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Progress bar showing demand relative to top food
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Student Purchases: ${item.totalSoldQuantity} units (${item.orderCount} orders)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Total Revenue: GH₵ ${"%.2f".format(item.totalRevenue)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = when (index) {
                                        0 -> Color(0xFFE65100)
                                        1 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.secondary
                                    },
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            // Footer tags: Stock, Campus Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "📊 Campus Popularity Share: ${"%.1f".format(percentageOfTotal)}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.isAvailable && item.currentStock > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        if (item.isAvailable && item.currentStock > 0) "In Stock (${item.currentStock} left)" else "Sold Out",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isAvailable && item.currentStock > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
