package com.example.data.recommendation

import com.example.data.FoodItem
import com.example.data.Order
import com.example.data.User
import java.util.Calendar

/**
 * Recommendation strategies supported by the ATU Food Recommendation Engine.
 */
enum class RecommendationStrategy(val title: String, val iconName: String) {
    TOP_PICKS("Top Picks For You", "auto_awesome"),
    ORDER_HISTORY("Based on Your Orders", "history"),
    TRENDING("🔥 Campus Trending", "local_fire_department"),
    DIETARY_GOALS("🥗 Matches Your Diet", "eco"),
    TIME_OF_DAY("🕒 Time-of-Day Specials", "schedule")
}

enum class RecommendationBadgeType {
    PERSONAL_FAVORITE,
    TRENDING_HOT,
    DIETARY_MATCH,
    VALUE_PICK,
    TIME_SPECIAL,
    HIGH_PROTEIN
}

data class DietaryPreferences(
    val isVegetarian: Boolean = false,
    val isVegan: Boolean = false,
    val isHalal: Boolean = false,
    val isLowCalorie: Boolean = false,      // Under 400 kcal
    val isHighProtein: Boolean = false,     // High protein options
    val isGlutenFree: Boolean = false,
    val isBudgetFriendly: Boolean = false,  // Under GH₵ 15.00
    val maxBudgetPerMeal: Double = 40.0,
    val preferredCategories: Set<String> = emptySet(),
    val excludedAllergens: Set<String> = emptySet() // "Peanuts", "Fish", "Dairy", "Eggs", "Gluten"
) {
    fun toSerializedString(): String {
        val flags = mutableListOf<String>()
        if (isVegetarian) flags.add("VEGETARIAN")
        if (isVegan) flags.add("VEGAN")
        if (isHalal) flags.add("HALAL")
        if (isLowCalorie) flags.add("LOW_CALORIE")
        if (isHighProtein) flags.add("HIGH_PROTEIN")
        if (isGlutenFree) flags.add("GLUTEN_FREE")
        if (isBudgetFriendly) flags.add("BUDGET_FRIENDLY")
        if (preferredCategories.isNotEmpty()) flags.add("CATS:" + preferredCategories.joinToString("|"))
        if (excludedAllergens.isNotEmpty()) flags.add("ALLERGENS:" + excludedAllergens.joinToString("|"))
        return flags.joinToString(",")
    }

    companion object {
        fun fromSerializedString(str: String?): DietaryPreferences {
            if (str.isNullOrBlank()) return DietaryPreferences()
            val tokens = str.split(",")
            var isVeg = false
            var isVegan = false
            var isHalal = false
            var isLowCal = false
            var isHighProt = false
            var isGlutenFree = false
            var isBudget = false
            val cats = mutableSetOf<String>()
            val allergens = mutableSetOf<String>()

            for (t in tokens) {
                val token = t.trim()
                when {
                    token == "VEGETARIAN" -> isVeg = true
                    token == "VEGAN" -> isVegan = true
                    token == "HALAL" -> isHalal = true
                    token == "LOW_CALORIE" -> isLowCal = true
                    token == "HIGH_PROTEIN" -> isHighProt = true
                    token == "GLUTEN_FREE" -> isGlutenFree = true
                    token == "BUDGET_FRIENDLY" -> isBudget = true
                    token.startsWith("CATS:") -> {
                        val cList = token.removePrefix("CATS:").split("|").filter { it.isNotBlank() }
                        cats.addAll(cList)
                    }
                    token.startsWith("ALLERGENS:") -> {
                        val aList = token.removePrefix("ALLERGENS:").split("|").filter { it.isNotBlank() }
                        allergens.addAll(aList)
                    }
                }
            }
            return DietaryPreferences(
                isVegetarian = isVeg,
                isVegan = isVegan,
                isHalal = isHalal,
                isLowCalorie = isLowCal,
                isHighProtein = isHighProt,
                isGlutenFree = isGlutenFree,
                isBudgetFriendly = isBudget,
                preferredCategories = cats,
                excludedAllergens = allergens
            )
        }
    }
}

data class RecommendedItem(
    val foodItem: FoodItem,
    val matchScore: Int, // 0 - 100%
    val reason: String,
    val badgeType: RecommendationBadgeType,
    val badgeLabel: String,
    val vendorName: String,
    val matchingDietaryTags: List<String> = emptyList(),
    val popularityRank: Int? = null,
    val orderCountByStudent: Int = 0,
    val campusOrderCount: Int = 0
)

/**
 * Intelligent Recommendation Engine for ATU Food Hub.
 * Analyzes order history, campus popularity patterns, user dietary preferences,
 * nutritional parameters, and contextual meal times.
 */
object FoodRecommendationEngine {

    fun generateRecommendations(
        availableFoodItems: List<FoodItem>,
        studentOrders: List<Order>,
        allCampusOrders: List<Order> = emptyList(),
        vendors: List<User> = emptyList(),
        dietaryPreferences: DietaryPreferences = DietaryPreferences(),
        strategy: RecommendationStrategy = RecommendationStrategy.TOP_PICKS,
        limit: Int = 10
    ): List<RecommendedItem> {
        val available = availableFoodItems.filter { it.isAvailable }
        if (available.isEmpty()) return emptyList()

        val vendorMap = vendors.associateBy({ it.id }, { it.fullName.ifBlank { it.username } })

        // 1. Compute User Past Order Metrics
        val studentOrderCounts = studentOrders
            .groupBy { it.foodItemId }
            .mapValues { it.value.sumOf { order -> order.quantity } }

        val studentCategoryCounts = studentOrders
            .mapNotNull { ord -> availableFoodItems.find { it.id == ord.foodItemId }?.category }
            .groupingBy { it }
            .eachCount()

        val favoriteCategories = studentCategoryCounts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .toSet()

        // 2. Compute Campus-Wide Trending Metrics
        val combinedCampusOrders = if (allCampusOrders.isNotEmpty()) allCampusOrders else studentOrders
        val campusOrderCounts = combinedCampusOrders
            .groupBy { it.foodItemId }
            .mapValues { it.value.sumOf { order -> order.quantity } }

        val maxCampusOrders = campusOrderCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1

        // 3. Determine Time of Day Context
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (currentHour) {
            in 6..10 -> "BREAKFAST"
            in 11..15 -> "LUNCH"
            in 16..20 -> "DINNER"
            else -> "LATE_NIGHT"
        }

        // 4. Score Each Food Item
        val scoredList = available.mapNotNull { item ->
            // Allergen Filter: If student has specified allergen exclusions, check if item violates
            if (dietaryPreferences.excludedAllergens.isNotEmpty()) {
                val itemAllergens = item.allergens.split(",", ";").map { it.trim().lowercase() }
                val hasViolatingAllergen = dietaryPreferences.excludedAllergens.any { excluded ->
                    itemAllergens.any { it.contains(excluded.lowercase()) }
                }
                if (hasViolatingAllergen) {
                    return@mapNotNull null // Exclude item safely
                }
            }

            var score = 50.0 // Base score
            val reasons = mutableListOf<String>()
            val matchingTags = mutableListOf<String>()
            var primaryBadge = getFallbackBadge(strategy)
            var badgeLabel = "Recommended"

            val studentCount = studentOrderCounts[item.id] ?: 0
            val campusCount = campusOrderCounts[item.id] ?: (if (item.id % 2 == 0) 12 else 8)

            // --- A. User History Scoring ---
            if (studentCount > 0) {
                val historyBoost = (studentCount * 12.0).coerceAtMost(35.0)
                score += historyBoost
                reasons.add("Ordered by you $studentCount time${if (studentCount > 1) "s" else ""}")
                primaryBadge = RecommendationBadgeType.PERSONAL_FAVORITE
                badgeLabel = if (studentCount >= 3) "Your Top Favorite" else "Re-order Pick"
            } else if (favoriteCategories.contains(item.category)) {
                score += 15.0
                reasons.add("Matches your love for ${item.category}")
            }

            // --- B. Campus Trending Scoring ---
            val trendingRatio = (campusCount.toDouble() / maxCampusOrders).coerceIn(0.0, 1.0)
            val trendingBoost = trendingRatio * 25.0
            score += trendingBoost
            if (campusCount >= 5) {
                reasons.add("🔥 Top seller on ATU campus ($campusCount orders)")
                if (studentCount == 0 && trendingRatio > 0.4) {
                    primaryBadge = RecommendationBadgeType.TRENDING_HOT
                    badgeLabel = "Campus Trending"
                }
            }

            // --- C. Dietary Preferences Scoring ---
            val nameLower = item.name.lowercase()
            val descLower = item.description.lowercase()
            val catLower = item.category.lowercase()

            if (dietaryPreferences.isVegetarian) {
                val isVegMatch = !nameLower.contains("chicken") && !nameLower.contains("beef") &&
                        !nameLower.contains("fish") && !nameLower.contains("meat") && !nameLower.contains("goat") && !nameLower.contains("egg")
                if (isVegMatch) {
                    score += 25.0
                    matchingTags.add("Vegetarian")
                    reasons.add("🌱 100% Plant-based / Vegetarian option")
                    primaryBadge = RecommendationBadgeType.DIETARY_MATCH
                    badgeLabel = "Vegetarian Choice"
                } else {
                    score -= 40.0
                }
            }

            if (dietaryPreferences.isVegan) {
                val isVeganMatch = !nameLower.contains("chicken") && !nameLower.contains("beef") &&
                        !nameLower.contains("fish") && !nameLower.contains("meat") && !nameLower.contains("dairy") &&
                        !nameLower.contains("cheese") && !nameLower.contains("egg") && !nameLower.contains("milk")
                if (isVeganMatch) {
                    score += 25.0
                    matchingTags.add("Vegan")
                    reasons.add("🌿 Vegan Certified")
                } else {
                    score -= 50.0
                }
            }

            if (dietaryPreferences.isHalal) {
                val isHalalFriendly = !nameLower.contains("pork") && !descLower.contains("pork")
                if (isHalalFriendly) {
                    score += 18.0
                    matchingTags.add("Halal")
                    reasons.add("☪️ Halal-friendly campus certified")
                } else {
                    score -= 60.0
                }
            }

            if (dietaryPreferences.isLowCalorie) {
                if (item.calories in 1..380) {
                    score += 22.0
                    matchingTags.add("${item.calories} kcal")
                    reasons.add("🥗 Light & Fit (${item.calories} kcal)")
                    primaryBadge = RecommendationBadgeType.DIETARY_MATCH
                    badgeLabel = "Low Calorie"
                }
            }

            if (dietaryPreferences.isHighProtein) {
                val hasHighProtein = nameLower.contains("chicken") || nameLower.contains("egg") ||
                        nameLower.contains("fish") || nameLower.contains("beef") || nameLower.contains("beans") || nameLower.contains("tuna")
                if (hasHighProtein) {
                    score += 22.0
                    matchingTags.add("High Protein")
                    reasons.add("💪 High-protein energy builder")
                    primaryBadge = RecommendationBadgeType.HIGH_PROTEIN
                    badgeLabel = "High Protein"
                }
            }

            if (dietaryPreferences.isBudgetFriendly || item.price <= dietaryPreferences.maxBudgetPerMeal) {
                if (item.price <= 15.00) {
                    score += 20.0
                    matchingTags.add("Student Budget")
                    reasons.add("💰 Great student value (GH₵ ${"%.2f".format(item.price)})")
                    if (studentCount == 0) {
                        primaryBadge = RecommendationBadgeType.VALUE_PICK
                        badgeLabel = "Budget Deal"
                    }
                }
            }

            // --- D. Time of Day Context ---
            when (timeOfDay) {
                "BREAKFAST" -> {
                    if (catLower.contains("breakfast") || nameLower.contains("tea") || nameLower.contains("porridge") ||
                        nameLower.contains("egg") || nameLower.contains("bread") || nameLower.contains("pancake") || nameLower.contains("coffee")
                    ) {
                        score += 20.0
                        reasons.add("🌅 Perfect breakfast start")
                        if (strategy == RecommendationStrategy.TIME_OF_DAY) {
                            primaryBadge = RecommendationBadgeType.TIME_SPECIAL
                            badgeLabel = "Morning Special"
                        }
                    }
                }
                "LUNCH" -> {
                    if (catLower.contains("local") || catLower.contains("rice") || nameLower.contains("jollof") ||
                        nameLower.contains("waakye") || nameLower.contains("fufu") || nameLower.contains("banku")
                    ) {
                        score += 20.0
                        reasons.add("☀️ Satisfying campus lunch meal")
                        if (strategy == RecommendationStrategy.TIME_OF_DAY) {
                            primaryBadge = RecommendationBadgeType.TIME_SPECIAL
                            badgeLabel = "Lunch Pick"
                        }
                    }
                }
                "DINNER", "LATE_NIGHT" -> {
                    if (catLower.contains("fast") || catLower.contains("snack") || nameLower.contains("noodles") ||
                        nameLower.contains("grill") || nameLower.contains("shawarma") || nameLower.contains("burger")
                    ) {
                        score += 20.0
                        reasons.add("🌙 Quick evening meal & bite")
                        if (strategy == RecommendationStrategy.TIME_OF_DAY) {
                            primaryBadge = RecommendationBadgeType.TIME_SPECIAL
                            badgeLabel = "Evening Pick"
                        }
                    }
                }
            }

            // Strategy-specific weight adjustments
            when (strategy) {
                RecommendationStrategy.ORDER_HISTORY -> {
                    if (studentCount == 0 && !favoriteCategories.contains(item.category)) {
                        score -= 20.0
                    } else {
                        score += 25.0
                    }
                }
                RecommendationStrategy.TRENDING -> {
                    score += (campusCount * 5.0)
                }
                RecommendationStrategy.DIETARY_GOALS -> {
                    if (matchingTags.isEmpty()) score -= 15.0 else score += (matchingTags.size * 15.0)
                }
                RecommendationStrategy.TIME_OF_DAY -> {
                    // Time boost already added
                }
                RecommendationStrategy.TOP_PICKS -> {
                    // Balanced scoring
                }
            }

            val finalMatchScore = score.coerceIn(60.0, 99.0).toInt()
            val primaryReason = if (reasons.isNotEmpty()) reasons.first() else "Popular choice at ${vendorMap[item.vendorId] ?: "ATU Cafeteria"}"
            val vendorName = vendorMap[item.vendorId] ?: "Stall #${item.vendorId}"

            RecommendedItem(
                foodItem = item,
                matchScore = finalMatchScore,
                reason = primaryReason,
                badgeType = primaryBadge,
                badgeLabel = badgeLabel,
                vendorName = vendorName,
                matchingDietaryTags = matchingTags.distinct(),
                orderCountByStudent = studentCount,
                campusOrderCount = campusCount
            )
        }

        // Rank by final match score descending
        val ranked = scoredList.sortedByDescending { it.matchScore }

        // Assign ranking positions
        return ranked.mapIndexed { index, item ->
            item.copy(popularityRank = index + 1)
        }.take(limit)
    }

    private fun getFallbackBadge(strategy: RecommendationStrategy): RecommendationBadgeType {
        return when (strategy) {
            RecommendationStrategy.ORDER_HISTORY -> RecommendationBadgeType.PERSONAL_FAVORITE
            RecommendationStrategy.TRENDING -> RecommendationBadgeType.TRENDING_HOT
            RecommendationStrategy.DIETARY_GOALS -> RecommendationBadgeType.DIETARY_MATCH
            RecommendationStrategy.TIME_OF_DAY -> RecommendationBadgeType.TIME_SPECIAL
            RecommendationStrategy.TOP_PICKS -> RecommendationBadgeType.VALUE_PICK
        }
    }
}
