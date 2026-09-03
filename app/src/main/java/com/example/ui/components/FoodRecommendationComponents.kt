package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.FoodItem
import com.example.data.recommendation.*
import com.example.ui.screens.bounceClickable

@Composable
fun FoodRecommendationSection(
    recommendedItems: List<RecommendedItem>,
    activeStrategy: RecommendationStrategy,
    dietaryPreferences: DietaryPreferences,
    onStrategySelected: (RecommendationStrategy) -> Unit,
    onOpenDietaryPreferences: () -> Unit,
    onAddToCart: (FoodItem) -> Unit,
    onItemClick: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExplanationDialog by remember { mutableStateOf<RecommendedItem?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recommendation_engine_section"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title, AI Badge, and Dietary Preferences Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFFFFB300))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Recommendation Engine",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Smart Food Recommendations",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.3).sp
                                )
                            )
                            Surface(
                                color = Color(0xFFFF8F00).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFFF8F00).copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "AI ENGINE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tailored to your orders, campus trends & dietary goals",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dietary Preferences Button
                FilledTonalIconButton(
                    onClick = onOpenDietaryPreferences,
                    modifier = Modifier.testTag("open_dietary_preferences_btn")
                ) {
                    BadgedBox(
                        badge = {
                            val activeDietCount = (if (dietaryPreferences.isVegetarian) 1 else 0) +
                                    (if (dietaryPreferences.isHalal) 1 else 0) +
                                    (if (dietaryPreferences.isLowCalorie) 1 else 0) +
                                    (if (dietaryPreferences.isHighProtein) 1 else 0) +
                                    (if (dietaryPreferences.isBudgetFriendly) 1 else 0) +
                                    (if (dietaryPreferences.isVegan) 1 else 0)

                            if (activeDietCount > 0) {
                                Badge(containerColor = Color(0xFF2E7D32)) {
                                    Text(activeDietCount.toString(), fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Dietary Preferences"
                        )
                    }
                }
            }

            // Strategy Filter Selector Pills
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RecommendationStrategy.values()) { strategy ->
                    val isSelected = activeStrategy == strategy
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStrategySelected(strategy) },
                        label = {
                            Text(
                                text = strategy.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            when (strategy) {
                                RecommendationStrategy.TOP_PICKS -> Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                RecommendationStrategy.ORDER_HISTORY -> Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp))
                                RecommendationStrategy.TRENDING -> Icon(Icons.Default.Whatshot, null, modifier = Modifier.size(16.dp))
                                RecommendationStrategy.DIETARY_GOALS -> Icon(Icons.Default.Eco, null, modifier = Modifier.size(16.dp))
                                RecommendationStrategy.TIME_OF_DAY -> Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("strategy_chip_${strategy.name}")
                    )
                }
            }

            // Recommended Items Carousel
            if (recommendedItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No matching items for current filter",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Try adjusting your dietary filters or checking back during cafeteria hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recommendedItems, key = { it.foodItem.id }) { item ->
                        RecommendationCard(
                            recommendedItem = item,
                            onAddToCart = { onAddToCart(item.foodItem) },
                            onClick = { onItemClick(item.foodItem) },
                            onExplainClick = { showExplanationDialog = item }
                        )
                    }
                }
            }
        }
    }

    // Recommendation Explanation Dialog
    showExplanationDialog?.let { recItem ->
        AlertDialog(
            onDismissRequest = { showExplanationDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Why We Recommended This",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = recItem.foodItem.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${recItem.matchScore}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF2E7D32)
                            )
                            Text(
                                text = "Match Score based on your campus ordering behavior.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    Text(
                        text = "• ${recItem.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (recItem.orderCountByStudent > 0) {
                        Text(
                            text = "• You previously ordered this ${recItem.orderCountByStudent} time(s).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (recItem.matchingDietaryTags.isNotEmpty()) {
                        Text(
                            text = "• Matches your preferences: ${recItem.matchingDietaryTags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "• Served by ${recItem.vendorName} • Price: GH₵ ${"%.2f".format(recItem.foodItem.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                val inStock = recItem.foodItem.currentStock > 0 && recItem.foodItem.isAvailable
                Button(
                    onClick = {
                        onAddToCart(recItem.foodItem)
                        showExplanationDialog = null
                    },
                    enabled = inStock
                ) {
                    Icon(if (inStock) Icons.Default.AddShoppingCart else Icons.Default.RemoveShoppingCart, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (inStock) "Add to Cart" else "Out of Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExplanationDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun RecommendationCard(
    recommendedItem: RecommendedItem,
    onAddToCart: () -> Unit,
    onClick: () -> Unit,
    onExplainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val food = recommendedItem.foodItem
    val inStock = food.currentStock > 0 && food.isAvailable

    Card(
        modifier = modifier
            .width(220.dp)
            .bounceClickable(onClick = onClick)
            .testTag("recommended_card_${food.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (inStock) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (inStock) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image and Match Score Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = food.imageUrl.ifBlank { "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400" },
                    contentDescription = food.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (!inStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OUT OF STOCK",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Match Score Pill (Top Left)
                Surface(
                    color = Color(0xFF1B5E20).copy(alpha = 0.88f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp, topStart = 20.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${recommendedItem.matchScore}% Match",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.5.sp
                        )
                    }
                }

                // Badge Label (Top Right)
                Surface(
                    color = when (recommendedItem.badgeType) {
                        RecommendationBadgeType.PERSONAL_FAVORITE -> Color(0xFFD81B60).copy(alpha = 0.9f)
                        RecommendationBadgeType.TRENDING_HOT -> Color(0xFFE65100).copy(alpha = 0.9f)
                        RecommendationBadgeType.DIETARY_MATCH -> Color(0xFF2E7D32).copy(alpha = 0.9f)
                        RecommendationBadgeType.HIGH_PROTEIN -> Color(0xFF512DA8).copy(alpha = 0.9f)
                        RecommendationBadgeType.VALUE_PICK -> Color(0xFF00897B).copy(alpha = 0.9f)
                        RecommendationBadgeType.TIME_SPECIAL -> Color(0xFF1976D2).copy(alpha = 0.9f)
                    },
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = recommendedItem.badgeLabel,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                // Info / Explain Button (Bottom Right of Image)
                SmallFloatingActionButton(
                    onClick = onExplainClick,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Why Recommended",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Card Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = food.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (inStock) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = recommendedItem.vendorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Recommendation reason
                Text(
                    text = recommendedItem.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp,
                    modifier = Modifier.height(28.dp)
                )

                // Price and Add To Cart Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "GH₵ ${"%.2f".format(food.price)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        if (food.calories > 0) {
                            Text(
                                text = "${food.calories} kcal",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledIconButton(
                        onClick = onAddToCart,
                        enabled = inStock,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("add_recommended_to_cart_${food.id}"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = if (inStock) Icons.Default.AddShoppingCart else Icons.Default.RemoveShoppingCart,
                            contentDescription = if (inStock) "Add to Cart" else "Out of Stock",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DietaryPreferencesDialog(
    currentPreferences: DietaryPreferences,
    onSavePreferences: (DietaryPreferences) -> Unit,
    onDismiss: () -> Unit
) {
    var isVegetarian by remember { mutableStateOf(currentPreferences.isVegetarian) }
    var isVegan by remember { mutableStateOf(currentPreferences.isVegan) }
    var isHalal by remember { mutableStateOf(currentPreferences.isHalal) }
    var isLowCalorie by remember { mutableStateOf(currentPreferences.isLowCalorie) }
    var isHighProtein by remember { mutableStateOf(currentPreferences.isHighProtein) }
    var isGlutenFree by remember { mutableStateOf(currentPreferences.isGlutenFree) }
    var isBudgetFriendly by remember { mutableStateOf(currentPreferences.isBudgetFriendly) }
    var maxBudget by remember { mutableStateOf(currentPreferences.maxBudgetPerMeal.toFloat()) }
    val excludedAllergens = remember { mutableStateListOf<String>().apply { addAll(currentPreferences.excludedAllergens) } }

    val commonAllergens = listOf("Peanuts", "Dairy", "Fish", "Eggs", "Gluten", "Shellfish")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("dietary_preferences_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Dietary Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Customize how the AI suggests food to you",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Diet Options
                Text(
                    text = "Dietary Styles & Goals",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                PreferenceSwitchRow(
                    title = "🌱 Vegetarian",
                    subtitle = "Exclude meat, poultry, fish, and seafood dishes",
                    checked = isVegetarian,
                    onCheckedChange = { isVegetarian = it }
                )

                PreferenceSwitchRow(
                    title = "🌿 Vegan",
                    subtitle = "100% plant-based (no eggs, dairy, or honey)",
                    checked = isVegan,
                    onCheckedChange = { isVegan = it }
                )

                PreferenceSwitchRow(
                    title = "☪️ Halal Options",
                    subtitle = "Prioritize Halal certified campus vendors",
                    checked = isHalal,
                    onCheckedChange = { isHalal = it }
                )

                PreferenceSwitchRow(
                    title = "🥗 Low Calorie (< 400 kcal)",
                    subtitle = "Highlight light, nutrient-dense campus meals",
                    checked = isLowCalorie,
                    onCheckedChange = { isLowCalorie = it }
                )

                PreferenceSwitchRow(
                    title = "💪 High Protein",
                    subtitle = "Highlight chicken, fish, eggs, beans & fitness meals",
                    checked = isHighProtein,
                    onCheckedChange = { isHighProtein = it }
                )

                PreferenceSwitchRow(
                    title = "🌾 Gluten-Free",
                    subtitle = "Flag wheat, flour, and gluten ingredients",
                    checked = isGlutenFree,
                    onCheckedChange = { isGlutenFree = it }
                )

                PreferenceSwitchRow(
                    title = "💰 Student Budget Deal",
                    subtitle = "Prioritize meals under GH₵ 15.00",
                    checked = isBudgetFriendly,
                    onCheckedChange = { isBudgetFriendly = it }
                )

                HorizontalDivider()

                // Allergen Exclusions
                Text(
                    text = "Allergen Filters (Exclude items containing)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonAllergens.take(3).forEach { allergen ->
                        val isExcluded = excludedAllergens.contains(allergen)
                        FilterChip(
                            selected = isExcluded,
                            onClick = {
                                if (isExcluded) excludedAllergens.remove(allergen)
                                else excludedAllergens.add(allergen)
                            },
                            label = { Text(allergen, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    commonAllergens.drop(3).forEach { allergen ->
                        val isExcluded = excludedAllergens.contains(allergen)
                        FilterChip(
                            selected = isExcluded,
                            onClick = {
                                if (isExcluded) excludedAllergens.remove(allergen)
                                else excludedAllergens.add(allergen)
                            },
                            label = { Text(allergen, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }
                }

                HorizontalDivider()

                // Max Budget Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Max Target Meal Price", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "GH₵ ${"%.0f".format(maxBudget)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = maxBudget,
                        onValueChange = { maxBudget = it },
                        valueRange = 10f..80f,
                        steps = 13
                    )
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = DietaryPreferences(
                                isVegetarian = isVegetarian,
                                isVegan = isVegan,
                                isHalal = isHalal,
                                isLowCalorie = isLowCalorie,
                                isHighProtein = isHighProtein,
                                isGlutenFree = isGlutenFree,
                                isBudgetFriendly = isBudgetFriendly,
                                maxBudgetPerMeal = maxBudget.toDouble(),
                                excludedAllergens = excludedAllergens.toSet()
                            )
                            onSavePreferences(updated)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apply Preferences")
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
