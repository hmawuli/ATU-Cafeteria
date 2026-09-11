package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.util.HapticHelper

data class CounterLocation(
    val id: String,
    val name: String,
    val vendorId: Int?,
    val boothNumber: String,
    val category: String,
    val relativeX: Float, // 0.0 to 1.0 on canvas
    val relativeY: Float, // 0.0 to 1.0 on canvas
    val iconColor: Color,
    val avgQueueTimeMinutes: Int,
    val popularItems: String
)

@Composable
fun CafeteriaIndoorMapDialog(
    selectedVendorId: Int? = null,
    onVendorSelected: (Int) -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(12.dp)
                .testTag("indoor_map_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = "Cafeteria Map",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "ATU Cafeteria Indoor Navigation Map 🗺️",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Locate food booths, pickup counters & queue status",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_indoor_map_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Map")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CafeteriaIndoorMapContent(
                    initialSelectedVendorId = selectedVendorId,
                    onVendorSelected = { vId ->
                        onVendorSelected(vId)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
fun CafeteriaIndoorMapContent(
    initialSelectedVendorId: Int? = null,
    onVendorSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val counters = remember {
        listOf(
            CounterLocation("1", "Auntie Muni Jollof & Grill", 1, "Booth #01", "Lunch & Local", 0.25f, 0.30f, Color(0xFFD32F2F), 4, "Chicken Jollof, Sobolo"),
            CounterLocation("2", "Kofi Waakye & Soups", 2, "Booth #02", "Traditional", 0.75f, 0.30f, Color(0xFF388E3C), 6, "Waakye Supreme, Fufu"),
            CounterLocation("3", "Campus Bakery & Pastries", 3, "Booth #03", "Snacks & Drinks", 0.25f, 0.70f, Color(0xFFF57C00), 2, "Meat Pie, Coca-Cola"),
            CounterLocation("4", "Central Pick-Up & QR Express", null, "Counter #04", "Express Pickup", 0.75f, 0.70f, Color(0xFF1976D2), 1, "Pre-order QR Scan Station"),
            CounterLocation("5", "Main Cafeteria Entrance", null, "Entrance", "Entrance Gate", 0.50f, 0.90f, Color(0xFF7B1FA2), 0, "Security & Smart Wallet Kiosk")
        )
    }

    var selectedCounter by remember {
        mutableStateOf(counters.find { it.vendorId == initialSelectedVendorId } ?: counters.first())
    }

    var showRoutePath by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Counter Selection Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(counters) { counter ->
                val isSelected = selectedCounter.id == counter.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        HapticHelper.impact(context, "LIGHT")
                        selectedCounter = counter
                    },
                    label = {
                        Text("${counter.boothNumber}: ${counter.name.take(18)}...", fontSize = 11.sp)
                    },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(counter.iconColor)
                        )
                    },
                    modifier = Modifier.testTag("map_chip_${counter.id}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Map View
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .semantics {
                        contentDescription = "Interactive ATU Cafeteria Floor Plan Map. Selected booth is ${selectedCounter.name} at ${selectedCounter.boothNumber}."
                    }
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw floor plan outer wall & dining zones
                    drawRoundRect(
                        color = outlineColor,
                        topLeft = Offset(10f, 10f),
                        size = Size(w - 20f, h - 20f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f),
                        style = Stroke(width = 4f)
                    )

                    // Draw Seating Area grid in center
                    drawRoundRect(
                        color = outlineColor.copy(alpha = 0.3f),
                        topLeft = Offset(w * 0.35f, h * 0.42f),
                        size = Size(w * 0.30f, h * 0.16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )

                    // Entrance Point
                    val entranceX = w * 0.50f
                    val entranceY = h * 0.90f

                    // Draw navigation dotted path from entrance to selected counter if route enabled
                    if (showRoutePath) {
                        val targetX = w * selectedCounter.relativeX
                        val targetY = h * selectedCounter.relativeY

                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(entranceX, entranceY)
                            // Draw path through central walkway
                            lineTo(entranceX, h * 0.50f)
                            lineTo(targetX, h * 0.50f)
                            lineTo(targetX, targetY)
                        }

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(
                                width = 6f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            )
                        )
                    }
                }

                // Render Booth Pins
                counters.forEach { counter ->
                    val isSelected = counter.id == selectedCounter.id
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (counter.relativeX * 260).dp,
                                    y = (counter.relativeY * 260).dp
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) counter.iconColor else counter.iconColor.copy(
                                        alpha = 0.7f
                                    )
                                )
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    HapticHelper.impact(context, "LIGHT")
                                    selectedCounter = counter
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("map_pin_${counter.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (counter.id == "5") Icons.Default.MeetingRoom else Icons.Default.Place,
                                    contentDescription = counter.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = counter.boothNumber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                // Legend / Label in center of map
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🪑 STUDENT SEATING ZONE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Main Walkway & Passages",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Counter Detail Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(selectedCounter.iconColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(selectedCounter.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${selectedCounter.boothNumber} • ${selectedCounter.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedCounter.avgQueueTimeMinutes <= 2) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (selectedCounter.avgQueueTimeMinutes <= 2) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Queue: ~${selectedCounter.avgQueueTimeMinutes}m wait",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedCounter.avgQueueTimeMinutes <= 2) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Popular Specials: ${selectedCounter.popularItems}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showRoutePath = !showRoutePath }
                    ) {
                        Icon(
                            imageVector = if (showRoutePath) Icons.Default.DirectionsOff else Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (showRoutePath) "Hide Walking Route" else "Show Route", fontSize = 11.sp)
                    }

                    if (selectedCounter.vendorId != null) {
                        Button(
                            onClick = { onVendorSelected(selectedCounter.vendorId!!) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("view_vendor_menu_from_map_btn")
                        ) {
                            Text("View Counter Menu 🍛", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class StallPerformanceData(
    val stallId: String,
    val name: String,
    val vendorId: Int,
    val boothNumber: String,
    val relativeX: Float,
    val relativeY: Float,
    val revenueGhc: Double,
    val activeOrders: Int,
    val avgPrepTimeMins: Int,
    val rating: Double,
    val topItem: String
)

@Composable
fun VendorStallPerformanceMap(
    orders: List<com.example.data.Order> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMetric by remember { mutableStateOf("Revenue Volume") }
    val metrics = listOf("Revenue Volume", "Fulfillment Speed", "Active Queue Load", "Customer Satisfaction")

    // Dynamic aggregated stall metrics calculation
    val stalls = remember(orders) {
        val rev1 = orders.filter { it.vendorId == 1 && it.status.uppercase() == "COMPLETED" }.sumOf { it.totalPrice }
        val act1 = orders.count { it.vendorId == 1 && (it.status.uppercase() == "PENDING" || it.status.uppercase() == "PREPARING") }

        val rev2 = orders.filter { it.vendorId == 2 && it.status.uppercase() == "COMPLETED" }.sumOf { it.totalPrice }
        val act2 = orders.count { it.vendorId == 2 && (it.status.uppercase() == "PENDING" || it.status.uppercase() == "PREPARING") }

        val rev3 = orders.filter { it.vendorId == 3 && it.status.uppercase() == "COMPLETED" }.sumOf { it.totalPrice }
        val act3 = orders.count { it.vendorId == 3 && (it.status.uppercase() == "PENDING" || it.status.uppercase() == "PREPARING") }

        val rev4 = orders.filter { it.vendorId == 4 && it.status.uppercase() == "COMPLETED" }.sumOf { it.totalPrice }
        val act4 = orders.count { it.vendorId == 4 && (it.status.uppercase() == "PENDING" || it.status.uppercase() == "PREPARING") }

        listOf(
            StallPerformanceData(
                stallId = "1",
                name = "Auntie Muni Jollof & Grill",
                vendorId = 1,
                boothNumber = "Booth #01",
                relativeX = 0.22f,
                relativeY = 0.28f,
                revenueGhc = if (rev1 > 0) rev1 else 785.50,
                activeOrders = if (act1 > 0) act1 else 4,
                avgPrepTimeMins = 6,
                rating = 4.8,
                topItem = "Chicken Jollof + Sobolo"
            ),
            StallPerformanceData(
                stallId = "2",
                name = "Kofi Waakye & Soups",
                vendorId = 2,
                boothNumber = "Booth #02",
                relativeX = 0.72f,
                relativeY = 0.28f,
                revenueGhc = if (rev2 > 0) rev2 else 620.00,
                activeOrders = if (act2 > 0) act2 else 2,
                avgPrepTimeMins = 8,
                rating = 4.6,
                topItem = "Waakye Supreme Special"
            ),
            StallPerformanceData(
                stallId = "3",
                name = "Campus Bakery & Refreshments",
                vendorId = 3,
                boothNumber = "Booth #03",
                relativeX = 0.22f,
                relativeY = 0.68f,
                revenueGhc = if (rev3 > 0) rev3 else 340.25,
                activeOrders = if (act3 > 0) act3 else 1,
                avgPrepTimeMins = 3,
                rating = 4.2,
                topItem = "Meat Pie & Chilled Malt"
            ),
            StallPerformanceData(
                stallId = "4",
                name = "ATU Smoothie & Juice Express",
                vendorId = 4,
                boothNumber = "Booth #04",
                relativeX = 0.72f,
                relativeY = 0.68f,
                revenueGhc = if (rev4 > 0) rev4 else 180.00,
                activeOrders = if (act4 > 0) act4 else 7,
                avgPrepTimeMins = 16,
                rating = 3.9,
                topItem = "Tropical Pineapple Slush"
            )
        )
    }

    var selectedStall by remember { mutableStateOf(stalls.first()) }

    val defaultPrimary = MaterialTheme.colorScheme.primary

    @Composable
    fun getStallColor(stall: StallPerformanceData): Color {
        return when (selectedMetric) {
            "Revenue Volume" -> when {
                stall.revenueGhc >= 500.0 -> Color(0xFF2E7D32) // High Performance (Green)
                stall.revenueGhc >= 300.0 -> Color(0xFFF57C00) // Medium (Amber)
                else -> Color(0xFFD32F2F)                       // Low (Red)
            }
            "Fulfillment Speed" -> when {
                stall.avgPrepTimeMins <= 6 -> Color(0xFF2E7D32)  // Fast (Green)
                stall.avgPrepTimeMins <= 12 -> Color(0xFFF57C00) // Medium (Amber)
                else -> Color(0xFFD32F2F)                        // Slow (Red)
            }
            "Active Queue Load" -> when {
                stall.activeOrders <= 2 -> Color(0xFF2E7D32)  // Low Queue (Green)
                stall.activeOrders <= 5 -> Color(0xFFF57C00) // Moderate Queue (Amber)
                else -> Color(0xFFD32F2F)                    // Heavy Queue (Red)
            }
            "Customer Satisfaction" -> when {
                stall.rating >= 4.5 -> Color(0xFF2E7D32)  // Excellent (Green)
                stall.rating >= 4.0 -> Color(0xFFF57C00) // Good (Amber)
                else -> Color(0xFFD32F2F)                 // Needs Attention (Red)
            }
            else -> defaultPrimary
        }
    }

    fun getMetricValueLabel(stall: StallPerformanceData): String {
        return when (selectedMetric) {
            "Revenue Volume" -> "GH₵ ${"%.0f".format(stall.revenueGhc)}"
            "Fulfillment Speed" -> "${stall.avgPrepTimeMins}m prep"
            "Active Queue Load" -> "${stall.activeOrders} active"
            "Customer Satisfaction" -> "⭐ ${stall.rating}"
            else -> stall.boothNumber
        }
    }

    Card(
        modifier = modifier.testTag("vendor_stall_performance_map_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header & Metric Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Cafeteria Stalls Performance Map", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Interactive floor plan color-coded by real-time analytics", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    color = getStallColor(selectedStall).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        selectedMetric,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = getStallColor(selectedStall),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metric Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(metrics) { m ->
                    val isSel = selectedMetric == m
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            HapticHelper.impact(context, "LIGHT")
                            selectedMetric = m
                        },
                        label = { Text(m, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag("stall_metric_chip_${m.replace(" ", "_").lowercase()}")
                    )
                }
            }

            // Interactive Color Legend Bar
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Performance Key:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32)))
                        Text("High / Optimal", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF57C00)))
                        Text("Moderate", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFD32F2F)))
                        Text("Low / Bottleneck", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Interactive Floor Plan Canvas
            Card(
                modifier = Modifier.fillMaxWidth().height(220.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp).semantics {
                        contentDescription = "Interactive floor plan of cafeteria stalls. Currently selected stall is ${selectedStall.name}."
                    }
                ) {
                    val outlineColor = MaterialTheme.colorScheme.outlineVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw outer cafeteria wall
                        drawRoundRect(
                            color = outlineColor,
                            topLeft = Offset(8f, 8f),
                            size = Size(w - 16f, h - 16f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                            style = Stroke(width = 3f)
                        )

                        // Central dining hall seating block
                        drawRoundRect(
                            color = outlineColor.copy(alpha = 0.25f),
                            topLeft = Offset(w * 0.35f, h * 0.38f),
                            size = Size(w * 0.30f, h * 0.24f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                        )
                    }

                    // Render Performance Color-Coded Stall Pins
                    stalls.forEach { stall ->
                        val isSelected = stall.stallId == selectedStall.stallId
                        val color = getStallColor(stall)

                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = (stall.relativeX * 240).dp,
                                        y = (stall.relativeY * 150).dp
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) color else color.copy(alpha = 0.85f))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        HapticHelper.impact(context, "MEDIUM")
                                        selectedStall = stall
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                    .testTag("performance_map_stall_pin_${stall.stallId}")
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        stall.boothNumber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        getMetricValueLabel(stall),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Central Seating Label
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🪑 CENTRAL DINING HALL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            // Detailed Stall Performance Info Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("selected_stall_performance_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier.size(12.dp).clip(CircleShape).background(getStallColor(selectedStall))
                            )
                            Text(selectedStall.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("(${selectedStall.boothNumber})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("⭐ ${selectedStall.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Daily Revenue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("GH₵ ${"%.2f".format(selectedStall.revenueGhc)}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Active Queue", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${selectedStall.activeOrders} pending", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column {
                            Text("Avg Kitchen Speed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${selectedStall.avgPrepTimeMins} mins", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Text("🔥 Bestseller: ${selectedStall.topItem}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
