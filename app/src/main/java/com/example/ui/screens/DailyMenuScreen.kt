package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.*
import com.example.data.FoodItem
import com.example.data.recommendation.RecommendationStrategy
import com.example.ui.components.DietaryPreferencesDialog
import com.example.ui.components.FoodRecommendationSection
import com.example.ui.viewmodel.CafeteriaViewModel

/**
 * Daily Cafeteria Menu Screen.
 * Fetches and displays the daily cafeteria menu from the API/local cache
 * with smooth Lottie animations for loading and empty states, category filtering,
 * search, smart food recommendation engine, and quick ordering affordances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMenuScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    onFoodClick: ((FoodItem) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allFoodItems by viewModel.allFoodItems.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val recommendedItems by viewModel.recommendedItems.collectAsStateWithLifecycle()
    val activeRecommendationStrategy by viewModel.activeRecommendationStrategy.collectAsStateWithLifecycle()
    val dietaryPreferences by viewModel.dietaryPreferences.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDietaryPreferencesDialog by remember { mutableStateOf(false) }

    val categories = remember(allFoodItems) {
        listOf("All") + allFoodItems.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredItems = remember(allFoodItems, searchQuery, selectedCategory) {
        allFoodItems.filter { food ->
            val matchesCat = selectedCategory == "All" || food.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    food.name.contains(searchQuery, ignoreCase = true) ||
                    food.description.contains(searchQuery, ignoreCase = true) ||
                    food.category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Daily Cafeteria Menu",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            if (isOnline) "🟢 Live Menu • Updated Today" else "🟡 Offline Mode • Showing Local Room Cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("daily_menu_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            viewModel.clearLocalDatabaseCache()
                            isRefreshing = false
                        },
                        modifier = Modifier.testTag("daily_menu_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Menu"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("daily_menu_search_field"),
                placeholder = { Text("Search waakye, jollof, drinks, snacks...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("category_chip_$category")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Content Area with Lottie Animations
            when {
                isRefreshing || allFoodItems.isEmpty() && isOnline -> {
                    // Lottie Loading State
                    LottieMenuLoadingState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                filteredItems.isEmpty() -> {
                    // Lottie Empty / No Results State
                    LottieMenuEmptyState(
                        searchQuery = searchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                else -> {
                    // Menu Items List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (searchQuery.isBlank() && selectedCategory == "All" && recommendedItems.isNotEmpty()) {
                            item {
                                FoodRecommendationSection(
                                    recommendedItems = recommendedItems,
                                    activeStrategy = activeRecommendationStrategy,
                                    dietaryPreferences = dietaryPreferences,
                                    onStrategySelected = { strategy ->
                                        viewModel.setRecommendationStrategy(strategy)
                                    },
                                    onOpenDietaryPreferences = {
                                        showDietaryPreferencesDialog = true
                                    },
                                    onAddToCart = { foodItem: FoodItem ->
                                        viewModel.addToCart(foodItem, 1)
                                        android.widget.Toast.makeText(context, "Added '${foodItem.name}' to tray!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onItemClick = { foodItem: FoodItem ->
                                        onFoodClick?.invoke(foodItem) ?: run {
                                            navController.navigate("student_home")
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Available Today (${filteredItems.size} items)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredItems, key = { it.id }) { foodItem ->
                            DailyFoodItemCard(
                                foodItem = foodItem,
                                onAddToTray = {
                                    if (foodItem.currentStock > 0 && foodItem.isAvailable) {
                                        viewModel.addToCart(foodItem, 1)
                                        android.widget.Toast.makeText(context, "Added '${foodItem.name}' to tray!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        android.widget.Toast.makeText(context, "'${foodItem.name}' is currently out of stock.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onClick = {
                                    onFoodClick?.invoke(foodItem) ?: run {
                                        navController.navigate("student_home")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDietaryPreferencesDialog) {
            DietaryPreferencesDialog(
                currentPreferences = dietaryPreferences,
                onSavePreferences = { newPrefs ->
                    viewModel.updateDietaryPreferences(newPrefs)
                    android.widget.Toast.makeText(context, "Dietary profile updated! Recommendations refreshed.", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showDietaryPreferencesDialog = false }
            )
        }
    }
}

/**
 * Lottie Loading Animation View for daily menu fetching.
 */
@Composable
fun LottieMenuLoadingState(
    modifier: Modifier = Modifier,
    message: String = "Fetching today's fresh cafeteria menu..."
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_a15m22pt.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier
            .padding(24.dp)
            .testTag("lottie_menu_loading_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(160.dp)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Connecting to ATU kitchen updates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Lottie Empty / No Results Animation View.
 */
@Composable
fun LottieMenuEmptyState(
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Url("https://assets2.lottiefiles.com/packages/lf20_usmfx6bp.json")
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier
            .padding(24.dp)
            .testTag("lottie_menu_empty_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(140.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RestaurantMenu,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No items match '$searchQuery'" else "No menu items found for today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Check back soon or try selecting a different category filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Clean Material 3 Card displaying a food item with stock, price, category, and automatic Out of Stock tray handling.
 */
@Composable
fun DailyFoodItemCard(
    foodItem: FoodItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAddToTray: (() -> Unit)? = null
) {
    val inStock = foodItem.currentStock > 0 && foodItem.isAvailable

    ElevatedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (inStock) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (inStock) 2.dp else 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_food_item_card_${foodItem.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Food Image / Thumbnail
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (foodItem.imageUrl.isNotBlank() && foodItem.imageUrl.startsWith("http")) {
                    AsyncImage(
                        model = foodItem.imageUrl,
                        contentDescription = foodItem.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Fastfood,
                        contentDescription = foodItem.name,
                        tint = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }

                // Overlay tag if out of stock
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
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = foodItem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (inStock) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "GH₵ ${"%.2f".format(foodItem.price)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = foodItem.description.ifBlank { "Freshly prepared meal from ATU Cafeteria vendors." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Category Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = foodItem.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Stock Badge (in stock count vs Out of Stock)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (inStock) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                if (!inStock) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Out of Stock",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                                Text(
                                    text = if (inStock) "${foodItem.currentStock} in stock" else "Out of Stock",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (inStock) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Add to Tray Button (Disabled automatically when stock reaches zero)
                    if (onAddToTray != null) {
                        Button(
                            onClick = onAddToTray,
                            enabled = inStock,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("add_to_tray_btn_${foodItem.id}")
                        ) {
                            Icon(
                                imageVector = if (inStock) Icons.Default.AddShoppingCart else Icons.Default.RemoveShoppingCart,
                                contentDescription = if (inStock) "Add to Tray" else "Out of Stock",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (inStock) "Add to Tray" else "Out of Stock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
