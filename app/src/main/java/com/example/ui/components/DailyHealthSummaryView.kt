package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FoodItem
import com.example.data.Order
import com.example.ui.util.HapticHelper
import com.example.ui.viewmodel.CafeteriaViewModel

@Composable
fun DailyHealthSummaryDialog(
    orders: List<Order>,
    availableMenu: List<FoodItem>,
    viewModel: CafeteriaViewModel,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(12.dp)
                .testTag("health_summary_dialog_surface"),
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
                                .background(Color(0xFF2E7D32).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = "Health", tint = Color(0xFF2E7D32))
                        }
                        Column {
                            Text("Daily Health & Nutrition Summary 🍏", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Aggregated macro data & ATU Smart meal advice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_health_summary_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                DailyHealthSummaryContent(
                    orders = orders,
                    availableMenu = availableMenu,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun DailyHealthSummaryContent(
    orders: List<Order>,
    availableMenu: List<FoodItem>,
    viewModel: CafeteriaViewModel
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingNutrition.collectAsState()
    val nutritionCoachingText by viewModel.nutritionCoachingText.collectAsState()

    var healthGoal by remember { mutableStateOf("Maintain Focus & Energy") }
    val healthGoals = listOf("Maintain Focus & Energy", "Lean Muscle Growth", "Calorie Deficit / Weight Loss", "Exam Prep High-Stamina")

    // Aggregate nutrition estimates from recent orders
    val totalCalories = remember(orders) {
        orders.sumOf { order ->
            when {
                order.foodName.contains("Waakye", true) -> 650 * order.quantity
                order.foodName.contains("Jollof", true) -> 580 * order.quantity
                order.foodName.contains("Fufu", true) -> 750 * order.quantity
                order.foodName.contains("Salad", true) -> 320 * order.quantity
                order.foodName.contains("Pie", true) -> 380 * order.quantity
                order.foodName.contains("Sobolo", true) -> 120 * order.quantity
                else -> 450 * order.quantity
            }
        }.coerceAtLeast(420)
    }

    val totalProtein = remember(totalCalories) { (totalCalories * 0.22f / 4f).toInt() }
    val totalCarbs = remember(totalCalories) { (totalCalories * 0.55f / 4f).toInt() }
    val totalFat = remember(totalCalories) { (totalCalories * 0.23f / 9f).toInt() }

    val dailyTargetKcal = 2200f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Macro Summary Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TODAY'S AGGREGATED CONSUMPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$totalCalories / ${dailyTargetKcal.toInt()} kcal", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("${((totalCalories / dailyTargetKcal) * 100).toInt()}% Daily Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (totalCalories / dailyTargetKcal).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (totalCalories > dailyTargetKcal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroNutrientBadge("Protein 🥩", "${totalProtein}g", Color(0xFFD32F2F))
                        MacroNutrientBadge("Carbs 🌾", "${totalCarbs}g", Color(0xFFF57C00))
                        MacroNutrientBadge("Fat 🥑", "${totalFat}g", Color(0xFF388E3C))
                    }
                }
            }
        }

        // Goal Selection
        item {
            Column {
                Text("Select Primary Campus Wellness Goal:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    healthGoals.take(2).forEach { goal ->
                        FilterChip(
                            selected = healthGoal == goal,
                            onClick = { healthGoal = goal },
                            label = { Text(goal, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f).testTag("goal_chip_$goal")
                        )
                    }
                }
            }
        }

        // ATU Smart Consultation Action Button
        item {
            Button(
                onClick = {
                    HapticHelper.notification(context, "SUCCESS")
                    viewModel.runNutritionCoaching(
                        dailyTargetKcal = dailyTargetKcal,
                        currentKcal = totalCalories.toFloat(),
                        protein = totalProtein.toFloat(),
                        carbs = totalCarbs.toFloat(),
                        fat = totalFat.toFloat(),
                        availableFoodItems = availableMenu
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("ask_atu_health_advice_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulting ATU Smart Nutritionist...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate ATU Meal Adjustments ✨", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ATU Response Card
        if (nutritionCoachingText != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("atu_health_advice_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = "ATU", tint = Color(0xFF2E7D32), modifier = Modifier.size(22.dp))
                            Text("ATU Personalized Health Advice", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = nutritionCoachingText!!,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroNutrientBadge(label: String, value: String, accentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
        }
    }
}
