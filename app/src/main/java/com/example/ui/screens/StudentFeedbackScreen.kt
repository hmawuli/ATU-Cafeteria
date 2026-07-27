package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Professional Feedback Screen in Compose.
 * Uses star-rating logic across multi-dimensional culinary criteria
 * and provides text input fields for detailed student comments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFeedbackScreen(
    orderId: Int = 0,
    foodName: String = "Campus Meal Special",
    vendorName: String = "Accra Tech Cafeteria Vendor",
    onBackClick: () -> Unit = {},
    onSubmitFeedback: (
        foodQuality: Int,
        cleanliness: Int,
        speed: Int,
        priceValue: Int,
        overallStar: Int,
        detailedComment: String,
        recommendToOthers: Boolean,
        tags: List<String>
    ) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    var foodQualityRating by remember { mutableIntStateOf(5) }
    var cleanlinessRating by remember { mutableIntStateOf(5) }
    var speedRating by remember { mutableIntStateOf(5) }
    var priceValueRating by remember { mutableIntStateOf(5) }
    var overallDishStar by remember { mutableIntStateOf(5) }

    var detailedCommentText by remember { mutableStateOf("") }
    var recommendToOthers by remember { mutableStateOf(true) }
    var isSubmitted by remember { mutableStateOf(false) }

    val availableTags = listOf(
        "Delicious Culinary Taste",
        "Generous Portion Size",
        "Hygienic Eco-Packaging",
        "Lightning Fast Dispatch",
        "Polite Vendor Staff",
        "Great Value for Money"
    )
    val selectedTags = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Rate & Review Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Order #$orderId • $foodName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .testTag("feedback_back_button")
                            .semantics { contentDescription = "Back to dashboard" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isSubmitted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Thank You for Your Feedback!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Your review for $vendorName helps improve food quality and service standards across the Accra Technical University campus.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onBackClick,
                            modifier = Modifier.fillMaxWidth().testTag("feedback_done_button")
                        ) {
                            Text("Return to Orders")
                        }
                    }
                }
            } else {
                // Header Banner Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = "Review Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text("Vendor: $vendorName", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Share your honest rating to inform fellow students and reward quality vendors.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 1. Detailed Criteria Star Ratings
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Overall Quality & Experience Metrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        InteractiveStarRatingBar(
                            rating = foodQualityRating,
                            onRatingChanged = { foodQualityRating = it },
                            label = "🍔 Food Culinary Quality",
                            starSize = 28.dp
                        )

                        InteractiveStarRatingBar(
                            rating = cleanlinessRating,
                            onRatingChanged = { cleanlinessRating = it },
                            label = "🫧 Booth Hygiene & Cleanliness",
                            starSize = 28.dp
                        )

                        InteractiveStarRatingBar(
                            rating = speedRating,
                            onRatingChanged = { speedRating = it },
                            label = "⚡ Processing & Pickup Speed",
                            starSize = 28.dp
                        )

                        InteractiveStarRatingBar(
                            rating = priceValueRating,
                            onRatingChanged = { priceValueRating = it },
                            label = "💰 Price-to-Portion Value Ratio",
                            starSize = 28.dp
                        )

                        InteractiveStarRatingBar(
                            rating = overallDishStar,
                            onRatingChanged = { overallDishStar = it },
                            label = "⭐ Overall Meal Star Rating",
                            starSize = 32.dp
                        )
                    }
                }

                // 2. Feedback Category Chips
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Highlights & Tags",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Select tags that best describe your meal experience:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableTags.chunked(2).forEach { chunk ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    chunk.forEach { tag ->
                                        val isSelected = selectedTags.contains(tag)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                            },
                                            label = { Text(tag, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f).testTag("feedback_tag_${tag.lowercase().replace(" ", "_")}")
                                        )
                                    }
                                    if (chunk.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Text Input for Detailed Comments
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Detailed Student Comments",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = detailedCommentText,
                            onValueChange = { detailedCommentText = it },
                            placeholder = { Text("Write your detailed review, taste notes, or suggestions for the vendor here...", fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("detailed_comment_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Recommend this meal/vendor to students?", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = recommendToOthers,
                                onCheckedChange = { recommendToOthers = it },
                                modifier = Modifier.testTag("recommend_switch")
                            )
                        }
                    }
                }

                // Submit Action Button
                Button(
                    onClick = {
                        onSubmitFeedback(
                            foodQualityRating,
                            cleanlinessRating,
                            speedRating,
                            priceValueRating,
                            overallDishStar,
                            detailedCommentText,
                            recommendToOthers,
                            selectedTags.toList()
                        )
                        isSubmitted = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_student_feedback_button")
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Rating Star Icon", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Review & Earn Loyalty Points", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
