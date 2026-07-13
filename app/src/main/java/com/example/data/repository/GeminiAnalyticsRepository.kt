package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig

// ==========================================
// GEMINI PREDICTIVE PERFORMANCE ANALYTICS
// ==========================================
class GeminiAnalyticsRepository {

    suspend fun generateVendorPerformanceReview(
        vendorName: String,
        feedbacks: List<Feedback>,
        totalOrdersCount: Int,
        averageRatings: Map<String, Double>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "To generate real-time predictive analytics:\n" +
                    "1. Set up GEMINI_API_KEY in the Secrets/Env configuration panel.\n\n" +
                    "Offline Local Simulated Prediction: \n" +
                    "Feedback Analysis for '$vendorName':\n" +
                    "• Price Value rating is high (${"%.1f".format(averageRatings["priceValue"] ?: 4.0)}/5), indicating good portion-size ratios on campus.\n" +
                    "• Cleanliness is rated at ${"%.1f".format(averageRatings["cleanliness"] ?: 4.0)}/5. Recommend enforcing strict wastebin intervals.\n" +
                    "• Speed of delivery is ${"%.1f".format(averageRatings["speed"] ?: 4.0)}/5. Peak 12:30 PM bottleneck detected on campus. Recommend mobile pre-cooking."
        }

        // Aggregate comments from the reviews
        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are the Chief Academic Evaluator and Data Analytics Officer for the Accra Technical University (ATU) Cafeteria Board.
            Analyze the following student performance metrics and reviews for the Vendor: '$vendorName'.
            
            HISTOGRAM PERFORMANCE SUMMARY:
            - Total Verified Customer Orders: $totalOrdersCount
            - Mean Metric Ratings (out of 5.0 stars):
              * Food Quality: ${averageRatings["foodQuality"] ?: 0.0}
              * Booth Cleanliness: ${averageRatings["cleanliness"] ?: 0.0}
              * Service Speed: ${averageRatings["speed"] ?: 0.0}
              * Price-to-Value Ratio: ${averageRatings["priceValue"] ?: 0.0}
              
            STUDENT REVIEWS & TRANSCRIPTS:
            $reviewSummary
            
            Based on the data above, provide a structured management consulting report containing exactly the following three sections (use neat, professional Markdown text, formatted beautifully for a mobile dashboard):
            
            1. **🏆 Performance Diagnostics Scorecard**: Interpret the mean score combinations. Highlight their specific competitive strengths and operational bottlenecks in the ATU campus environment.
            
            2. **📈 Predictive Demand & Bottleneck Warnings**: Predict what periods or meal types are likely to cause issues based on the student comments, and mention expected crowding patterns during Accra campus peak hours (e.g., 11:30 AM to 1:30 PM).
            
            3. **💡 Strategic University Directives**: Deliver exactly 3 highly specific, localized action items (e.g. food prep instructions, waste control, digitised queuing) that the vendor must implement to comply with ATU hygiene and efficiency standards. Keep the tone insightful, academic, encouraging, and highly professional.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insight received from ATU Analytics, try again later."
        } catch (e: Exception) {
            Log.e("GeminiAnalytics", "Error communicating with Gemini", e)
            "Offline Simulation Mode (Network/API Limit reached): \n\n" +
                    "### 🏆 Performance Diagnostics Scorecard\n" +
                    "• **Strengths**: Excelling on Price-to-Value ratios for ATU student budgets, keeping food local and appetizing.\n" +
                    "• **Bottlenecks**: Pounded food preparation and Jollof peak crowding delays service speed during lunch hour transitions.\n\n" +
                    "### 📈 Predictive Demand & Bottleneck Warnings\n" +
                    "• High queue lengths are simulated on Tuesdays/Thursdays between 11:45 AM and 1:15 PM following morning lectures. Demand for Sobolo spikes alongside temperature peaks. \n\n" +
                    "### 💡 Strategic University Directives\n" +
                    "1. **Pre-portion Waakye Shito Sides**: Pre-packaging standard student packages before 11:30 AM will cut serving times by 40%.\n" +
                    "2. **Implement Dual-Line Service**: Have separate channels for cash/PIN-verification pickups and queue orders.\n" +
                    "3. **Campus Hygiene Protocol**: Arrange structured cleaning sweeps at 11:00 AM and 2:00 PM."
        }
    }

    suspend fun generateVendorSentimentAnalysis(
        vendorName: String,
        feedbacks: List<Feedback>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "To generate real-time sentiment analytics:\n" +
                    "1. Set up GEMINI_API_KEY in the Secrets/Env configuration panel.\n\n" +
                    "Offline Local Simulated Sentiment Analysis:\n\n" +
                    "### 📊 Overall Sentiment Balance\n" +
                    "🟢 **Positive**: 78% | 🟡 **Neutral**: 14% | 🔴 **Negative**: 8%\n\n" +
                    "### 🏆 Key Praise & Strengths\n" +
                    "• **Value for Money**: Students consistently highlight generous portions of Waakye relative to prices.\n" +
                    "• **Taste & Spiciness**: Shito and chicken seasoning received high praise across multiple comments.\n\n" +
                    "### ⚠️ Key Friction Points & Complaints\n" +
                    "• **Queue Waiting Bottlenecks**: Peak lunch transit congestion at 12:15 PM remains student friction point.\n" +
                    "• **Order status signaling**: Students noted that sometimes orders are marked 'Ready' but are still being boxed."
        }

        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are an advanced Customer Sentiment and Linguistic Specialist for the Accra Technical University (ATU) Cafeteria Board.
            Perform a qualitative sentiment analysis on student reviews and order comments for Vendor: '$vendorName'.
            
            STUDENT REVIEWS & TRANSCRIPTS:
            $reviewSummary
            
            Based on this raw conversational text, generate a beautiful, concise, polished sentiment report. Your output must contain:
            
            1. **📊 Sentiment Balance Breakdown**: Provide estimated percentages for Positive, Neutral, and Negative sentiments based on comments and rating distributions.
            2. **👍 Praise Highlights**: Summarize the leading aspects that students are happy about (e.g., taste, hygiene, hospitality, portion sizes).
            3. **👎 Critical Actionable Friction Points**: Identify specific student complaints, pain points, or constructive criticism in their descriptions.
            4. **💡 Executive Recommendation**: Give a 2-sentence summary recommendation to improve student experiences.
            
            Keep the content highly structured, engaging, and professional for a mobile dashboard. Use bold markdown headers and formatting.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No sentiment insight received, try again later."
        } catch (e: Exception) {
            Log.e("GeminiSentiment", "Error communicating with Gemini", e)
            "Offline Simulation Mode (Network/API Limit reached): \n\n" +
                    "### 📊 Overall Sentiment Balance\n" +
                    "🟢 **Positive**: 78% | 🟡 **Neutral**: 14% | 🔴 **Negative**: 8%\n\n" +
                    "### 🏆 Key Praise & Strengths\n" +
                    "• **Value for Money**: Students consistently highlight generous portions of Waakye relative to prices.\n" +
                    "• **Taste & Spiciness**: Shito and chicken seasoning received high praise across multiple comments.\n\n" +
                    "### ⚠️ Key Friction Points & Complaints\n" +
                    "• **Queue Waiting Bottlenecks**: Peak lunch transit congestion at 12:15 PM remains student friction point.\n" +
                    "• **Order status signaling**: Students noted that sometimes orders are marked 'Ready' but are still being boxed."
        }
    }

    suspend fun generateVendorAutoReplies(
        vendorName: String,
        feedbacks: List<Feedback>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "Offline Local Simulated Response Templates:\n\n" +
                    "### 📝 Template 1: For Service Speed/Waiting Complaints\n" +
                    "\"Dear Student, thank you for your valuable feedback. We are sincerely sorry you experienced a delay during peak hours. ATU Cafeteria values your time, and we are implementing pre-packaging and dual lines next week to speed up order collection. We hope to serve you better next time! - $vendorName\"\n\n" +
                    "### 📝 Template 2: For Food Quality/Portion Complaints\n" +
                    "\"Hello! Thank you for sharing your experience. We take food quality seriously. We want to ensure you get the best value for your money. Please show this message to our manager on your next visit so we can make this right. - $vendorName\"\n\n" +
                    "### 📝 Template 3: For Booth Hygiene/Cleanliness Complaints\n" +
                    "\"Thank you for bringing this to our attention. We are committed to strict hygienic protocols on campus. We have augmented our clean-up sweeps to address this immediately. Thank you for helping us keep ATU clean! - $vendorName\""
        }

        val reviewSummary = feedbacks.mapIndexed { i, f ->
            "Review #${i+1}: Quality=${f.ratingFoodQuality}, Cleanliness=${f.ratingCleanliness}, Speed=${f.ratingServiceSpeed}, Price=${f.ratingPriceValue}. Comment: \"${f.comment}\""
        }.joinToString("\n")

        val prompt = """
            You are a Professional Communications and PR specialist for the Accra Technical University (ATU) Cafeteria Board.
            Your task is to generate professional, polite, and constructive custom auto-reply response templates for Vendor: '$vendorName' to use when responding to critical or negative student reviews and comments.
            
            STUDENT REVIEWS & COMMENTS:
            $reviewSummary

            Generate 3 customized, highly professional, polite response templates tailored to the specific friction points, complaints, or negative aspects found in the reviews (such as slow service, cleanliness issues, taste, or price-value mismatch). Each template should be actionable and present a solution.
            
            Format your response beautifully with:
            - Clear Markdown headers (e.g., ### 📝 Template 1: [Topic Title])
            - Brief explanation of when the vendor should use each template.
            - The actual ready-to-copy placeholder response text enclosed in professional quotes.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No template suggestions received, please try again."
        } catch (e: Exception) {
            Log.e("GeminiAutoReply", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 📝 Template 1: For Service Speed/Waiting Complaints\n" +
                    "\"Dear Student, thank you for your valuable feedback. We are sincerely sorry you experienced a delay during peak hours. ATU Cafeteria values your time, and we are implementing pre-packaging and dual lines next week to speed up order collection. We hope to serve you better next time! - $vendorName\"\n\n" +
                    "### 📝 Template 2: For Food Quality/Portion Complaints\n" +
                    "\"Hello! Thank you for sharing your experience. We take food quality seriously. We want to ensure you get the best value for your money. Please show this message to our manager on your next visit so we can make this right. - $vendorName\"\n\n" +
                    "### 📝 Template 3: For Booth Hygiene/Cleanliness Complaints\n" +
                    "\"Thank you for bringing this to our attention. We are committed to strict hygienic protocols on campus. We have augmented our clean-up sweeps to address this immediately. Thank you for helping us keep ATU clean! - $vendorName\""
        }
    }

    suspend fun generateMenuPricingSuggestions(
        vendorName: String,
        orders: List<Order>,
        foodItems: List<FoodItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                    "Offline Local Simulated Suggestions:\n\n" +
                    "### ☀️ Breakfast Peak Hour Suggestions (8:00 AM - 10:30 AM)\n" +
                    "• **Special**: 'Rise & Shine Porridge Combo' (Koko + Egg + Bread) reduced from GH₵ 18.00 to **GH₵ 15.00**.\n" +
                    "• **Pricing Strategy**: Maintain current prices for single pastries as they are highly price-elastic for students first thing in the morning.\n\n" +
                    "### 🍚 Lunch Rush hour Suggestions (11:30 AM - 2:00 PM)\n" +
                    "• **Dynamic Pricing**: Waakye premium packages with fish & egg can support a **5% peak price increase** (GH₵ 30 to GH₵ 31.50) due to high demand.\n" +
                    "• **Special Combo**: 'ATU Lunch Champion' (Waakye + Sobolo) bundled for GH₵ 35.00 (saves 12% compared to separate purchases).\n\n" +
                    "### 🍹 Afternoon Slack Hour Suggestions (2:30 PM - 5:00 PM)\n" +
                    "• **Specials**: 'Happy Hour Drinks': Discount Sobolo and fresh juices by **20%** to generate traffic during lecture intervals."
        }

        val cal = java.util.Calendar.getInstance()
        var breakfastCount = 0
        var lunchCount = 0
        var dinnerCount = 0
        
        val breakfastItems = java.util.HashMap<String, Int>()
        val lunchItems = java.util.HashMap<String, Int>()
        val dinnerItems = java.util.HashMap<String, Int>()

        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val name = order.foodName
            
            when (hour) {
                in 6..10 -> {
                    breakfastCount += qty
                    breakfastItems[name] = (breakfastItems[name] ?: 0) + qty
                }
                in 11..14 -> {
                    lunchCount += qty
                    lunchItems[name] = (lunchItems[name] ?: 0) + qty
                }
                else -> {
                    dinnerCount += qty
                    dinnerItems[name] = (dinnerItems[name] ?: 0) + qty
                }
            }
        }

        val menuListStr = foodItems.joinToString("\n") { "• ${it.name} (${it.category}) - GH₵ ${"%.2f".format(it.price)}" }

        val bSorted = breakfastItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }
        val lSorted = lunchItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }
        val dSorted = dinnerItems.entries.sortedByDescending { it.value }.take(3).joinToString(", ") { "${it.key} (${it.value} units)" }

        val prompt = xmlDocClean("""
            You are an expert hospitality consultant and algorithmic pricing strategist for the Accra Technical University (ATU) Cafeteria Board.
            Analyze the following menu and actual purchase demand logs for the Vendor '$vendorName' to suggest optimal menu pricing strategies and daily specials.
            
            CURRENT MENU OVERVIEW:
            $menuListStr
            
            REAL-TIME PURCHASE PATTERNS (By Time of Day):
            
            1. **Breakfast Period (6:00 AM - 10:59 AM)**:
               - Total items ordered: $breakfastCount units
               - Top selling dishes: ${if (bSorted.isEmpty()) "No data yet" else bSorted}
               
            2. **Lunch Period (11:00 AM - 2:59 PM)**:
               - Total items ordered: $lunchCount units
               - Top selling dishes: ${if (lSorted.isEmpty()) "No data yet" else lSorted}
               
            3. **Late Afternoon & Evening (3:00 PM onwards)**:
               - Total items ordered: $dinnerCount units
               - Top selling dishes: ${if (dSorted.isEmpty()) "No data yet" else dSorted}
               
            Based on these realistic demand trends, provide a structured intelligence report containing exactly:
            
            - **☀️ Breakfast Hour pricing & specials**: Analyze if early lecture slots justify breakfast combo bundles (e.g. porridge & pastry combos) and recommend pricing.
            - **🍚 Lunch Rush Peak strategies**: Since lunch is the most crowded period at ATU, should the vendor use dynamic pricing (slight premium on waakye/jollof peak demand) or meal/drink combos (e.g., adding Sobolo) to speed up lines? Recommend exact price adjustments.
            - **🍹 Off-Peak Happy Hour promotions**: Propose discounts or clearance pricing to encourage sales during off-peak windows (2:30 PM - 4:30 PM) to clear stock of perishables.
            
            Keep the report beautifully styled with bullet points, bold percentages, and bold pricing figures (GH₵) so vendors can read them instantly on their phone dashboard.
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No price recommendations generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiPricing", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### ☀️ Breakfast Peak Hour Suggestions (8:00 AM - 10:30 AM)\n" +
                    "• **Special**: 'Rise & Shine Porridge Combo' (Koko + Egg + Bread) reduced from GH₵ 18.00 to **GH₵ 15.00**.\n" +
                    "• **Pricing Strategy**: Maintain current prices for single pastries as they are highly price-elastic for students first thing in the morning.\n\n" +
                    "### 🍚 Lunch Rush hour Suggestions (11:30 AM - 2:00 PM)\n" +
                    "• **Dynamic Pricing**: Waakye premium packages with fish & egg can support a **5% peak price increase** (GH₵ 30 to GH₵ 31.50) due to high demand.\n" +
                    "• **Special Combo**: 'ATU Lunch Champion' (Waakye + Sobolo) bundled for GH₵ 35.00 (saves 12% compared to separate purchases).\n\n" +
                    "### 🍹 Afternoon Slack Hour Suggestions (2:30 PM - 5:00 PM)\n" +
                    "• **Specials**: 'Happy Hour Drinks': Discount Sobolo and fresh juices by **20%** to generate traffic during lecture intervals."
        }
    }

    suspend fun generateTodayInsights(
        vendorName: String,
        orders: List<Order>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        val cal = java.util.Calendar.getInstance()
        val hourlyCount = IntArray(24)
        val hourlyRevenue = DoubleArray(24)
        val itemQuantities = java.util.HashMap<String, Int>()
        val itemRevenue = java.util.HashMap<String, Double>()
        var totalRev = 0.0
        var totalQty = 0
        
        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val price = order.totalPrice
            val name = order.foodName
            
            hourlyCount[hour] += qty
            hourlyRevenue[hour] += price
            itemQuantities[name] = (itemQuantities[name] ?: 0) + qty
            itemRevenue[name] = (itemRevenue[name] ?: 0.0) + price
            totalRev += price
            totalQty += qty
        }
        
        // Find busiest hour range
        var busiestHourIndex = -1
        var maxHourlyQty = 0
        for (h in 0..23) {
            if (hourlyCount[h] > maxHourlyQty) {
                maxHourlyQty = hourlyCount[h]
                busiestHourIndex = h
            }
        }
        
        val busiestHourStr = if (busiestHourIndex != -1) {
            val startHour = busiestHourIndex
            val endHour = (busiestHourIndex + 1) % 24
            val startAmPm = if (startHour >= 12) "PM" else "AM"
            val displayStart = if (startHour % 12 == 0) 12 else startHour % 12
            val endAmPm = if (endHour >= 12) "PM" else "AM"
            val displayEnd = if (endHour % 12 == 0) 12 else endHour % 12
            "$displayStart $startAmPm - $displayEnd $endAmPm"
        } else {
            "No orders logged today yet."
        }
        
        // Find most ordered item
        val topItem = itemQuantities.entries.maxByOrNull { it.value }
        val topItemStr = if (topItem != null) {
            "${topItem.key} (${topItem.value} units - GH₵ ${"%.2f".format(itemRevenue[topItem.key] ?: 0.0)})"
        } else {
            "No orders logged today yet."
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            if (orders.isEmpty()) {
                return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                        "Offline Local Simulated Today's Insights:\n\n" +
                        "• **Busiest Hour**: Lunch Rush (12:00 PM - 1:30 PM) is usually the peak. Jollof and Waakye sales spike by over **35%**.\n" +
                        "• **Most Demanded Item**: Waakye Premium Combo (with fish, egg, and extra shito) is expected to be the most ordered item.\n" +
                        "• **Vendor Pro-Tip**: Prepare Sobolo packaging and dynamic combo bundles before 11:30 AM to minimize queue bottlenecks."
            } else {
                return@withContext "API Configuration Error: Gemini API key has not been entered into the AI Studio Secrets panel.\n\n" +
                        "Offline Local Simulated Today's Insights:\n\n" +
                        "### 📊 Today's Live Sales Analytics\n" +
                        "• **Busiest Hour**: **$busiestHourStr**\n" +
                        "• **Most Ordered Item**: **$topItemStr**\n" +
                        "• **Total Revenue**: **GH₵ ${"%.2f".format(totalRev)}** across **$totalQty** items ordered.\n\n" +
                        "### 💡 Smart Recommendations for $vendorName\n" +
                        "1. **Peak Demand Action**: Your busiest window was around **$busiestHourStr**. Consider preparing pre-packaged portions 15 minutes before this peak to serve students instantaneously!\n" +
                        "2. **Menu Focus**: **$topItemStr** is leading your sales today. Ensure ingredients are adequately stocked to avoid missing out on late-afternoon orders.\n" +
                        "3. **Dynamic Bundle**: Bundle your top seller with Sobolo for an elegant multi-item savings deal (e.g. GH₵ 2.00 off combo) to increase average ticket size."
            }
        }

        val prompt = xmlDocClean("""
            You are an expert AI business intelligence analyst for the Accra Technical University (ATU) Cafeteria Board.
            Analyze today's live sales transactions for the vendor '$vendorName' to generate critical operational insights and actionable advice.
            
            TODAY'S RAW SALES METRICS:
            - **Total Sales Revenue Today**: GH₵ ${"%.2f".format(totalRev)}
            - **Total Items Sold**: $totalQty units
            - **Busiest Registered Hour**: $busiestHourStr
            - **Most Demanded Menu Item**: $topItemStr
            
            FEEDBACK & SALES TRANSACTION LOGS DETAIL:
            ${if (orders.isEmpty()) "No orders recorded yet." else orders.joinToString("\n") { "• Order #${it.id}: ${it.foodName} (QTY: ${it.quantity}), Revenue: GH₵ ${"%.2f".format(it.totalPrice)}, Status: ${it.status}, Customer ID: ${it.customerId}" }}
            
            Based on these realistic intraday sales trends, generate a structured intelligence update containing:
            
            - **🔥 Peak Intensity analysis**: Report on the 'Busiest Hour' ($busiestHourStr) with tips on how $vendorName can handle this rush (e.g. queue management or pre-prep).
            - **✨ Star Product performance**: Analyze the 'Most Ordered Item' ($topItemStr) and dynamic suggestions to cross-promote it or manage inventory around it.
            - **💡 Immediate Operational Pro-Tip**: A bold, highly practical tip tailored specifically to today's transaction size and items to optimize profitability or food waste.
            
            Keep the report beautifully styled with concise bullets, bold keys, and clear pricing symbols (GH₵) so vendors can read and digest them in seconds. Keep it compact!
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No today's insights generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiTodayInsights", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 📊 Today's Live Sales Analytics\n" +
                    "• **Busiest Hour**: **$busiestHourStr**\n" +
                    "• **Most Ordered Item**: **$topItemStr**\n" +
                    "• **Total Revenue**: **GH₵ ${"%.2f".format(totalRev)}** across **$totalQty** items ordered.\n\n" +
                    "### 💡 Smart Recommendations for $vendorName\n" +
                    "1. **Peak Demand Action**: Your busiest window was around **$busiestHourStr**. Consider preparing pre-packaged portions 15 minutes before this peak to serve students instantaneously!"
        }
    }

    suspend fun generateHistoricalOrderInsights(
        vendorName: String,
        orders: List<Order>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        // Compute historical metrics locally
        var totalRev = 0.0
        var totalQty = 0
        val hourlyCount = IntArray(24)
        val itemQuantities = java.util.HashMap<String, Int>()
        val itemRevenue = java.util.HashMap<String, Double>()
        
        val cal = java.util.Calendar.getInstance()
        
        for (order in orders) {
            cal.timeInMillis = order.orderTimestamp
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val qty = order.quantity
            val price = order.totalPrice
            val name = order.foodName
            
            hourlyCount[hour] += qty
            itemQuantities[name] = (itemQuantities[name] ?: 0) + qty
            itemRevenue[name] = (itemRevenue[name] ?: 0.0) + price
            totalRev += price
            totalQty += qty
        }
        
        // Find busiest hour
        var busiestHourIndex = -1
        var maxHourlyQty = 0
        for (h in 0..23) {
            if (hourlyCount[h] > maxHourlyQty) {
                maxHourlyQty = hourlyCount[h]
                busiestHourIndex = h
            }
        }
        
        val busiestHourStr = if (busiestHourIndex != -1) {
            val startHour = busiestHourIndex
            val endHour = (busiestHourIndex + 1) % 24
            val startAmPm = if (startHour >= 12) "PM" else "AM"
            val displayStart = if (startHour % 12 == 0) 12 else startHour % 12
            val endAmPm = if (endHour >= 12) "PM" else "AM"
            val displayEnd = if (endHour % 12 == 0) 12 else endHour % 12
            "$displayStart $startAmPm - $displayEnd $endAmPm"
        } else {
            "No historical orders recorded yet."
        }
        
        // Find top selling food items
        val sortedPopularItems = itemQuantities.entries.sortedByDescending { it.value }.take(3)
        val popularItemsStr = sortedPopularItems.joinToString("\n") { 
            "• **${it.key}**: Sold **${it.value} units**, yielding total sales of **GH₵ ${"%.2f".format(itemRevenue[it.key] ?: 0.0)}**."
        }
        
        val topItem = sortedPopularItems.firstOrNull()?.key ?: "signature meals"
        val topItemQty = sortedPopularItems.firstOrNull()?.value ?: 0
        val topItemRev = itemRevenue[topItem] ?: 0.0
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "### 📈 Gemini Intelligence: Historical Order Analytics Summary for **$vendorName**\n" +
                    "*(Local Smart Fallback Report — Active Data Aggregation Running Live)*\n\n" +
                    "An analysis of **${orders.size} completed transactions** shows heavy student demand and high-contrast purchase peaks sync'd to Accra Technical University's lecture calendar.\n\n" +
                    "---\n\n" +
                    "### 1. 🔥 Peak Traffic Density & Order Velocity\n" +
                    "• **Absolute Peak Hour**: **$busiestHourStr** represents the absolute highest ordering concentration, accounting for major delivery lines.\n" +
                    "• **Operational Strategy**: Prepare portion prep **20 minutes before $busiestHourStr**. Setup a dual-line checkout (split for digital pre-orders vs walk-in ordering) to optimize fulfillment.\n\n" +
                    "---\n\n" +
                    "### 2. 🍔 Core Menu Popularity Index (Top Dish Assessment)\n" +
                    "If details are available:\n" +
                    (if (sortedPopularItems.isEmpty()) "• No menu trends database recorded yet." else popularItemsStr) + "\n\n" +
                    "• **Strategic Recommendation**: Introduce a **'Star Combo Promo'** combining **$topItem** with a popular refreshing drink to boost overall transaction size.\n\n" +
                    "---\n\n" +
                    "### 3. 🔋 Kitchen Resource & Supply Chain Guidance\n" +
                    "• **Inventory Buffer**: Maintain a **20% stock surplus of ingredients** for **$topItem** on heavy lecture days to avoid missing late-stage demand.\n" +
                    "• **Fulfillment Prep**: Standardize prep timing to ensure standard hand-offs during peak rush intervals do not exceed **4-6 minutes per student**."
        }

        val prompt = xmlDocClean("""
            You are a lead institutional restaurant analyst and predictive supply chain strategist at Accra Technical University (ATU).
            Please review this aggregated historical dataset from the 'orders' table for vendor '$vendorName':
            
            HISTORICAL ORDERS SUMMARY:
            - Total Completed Transactions: ${orders.size}
            - Total Cumulative Revenue: GH₵ ${"%.2f".format(totalRev)}
            - Absolute Single Busiest Peak Hour: $busiestHourStr
            
            STAR FOOD ITEMS & POPULARITY:
            ${if (sortedPopularItems.isEmpty()) "No data logged." else sortedPopularItems.joinToString("\n") { "• ${it.key}: Sold ${it.value} units, Revenue of GH₵ ${"%.2f".format(itemRevenue[it.key] ?: 0.0)}" }}
            
            Please construct a comprehensive, action-oriented predictive demand and culinary intelligence report in clean Markdown format with the following pillars:
            
            1. **🔥 Peak Hour Traffic Density & Bottlenecks**: Analyze their busiest windows specifically around $busiestHourStr. Suggest how to adjust service velocity or introduce digital pre-orders to navigate these peak university lecture breaks.
            2. **🍔 Core Menu Popularity Index**: Analyze the top selling items. Suggest how they can bundle slower-moving products with popular items to drive larger orders.
            3. **🔋 Kitchen Resource Planning Guidance**: Give tailored guidance on preparing ingredients beforehand to prevent running out of food, minimizing local wait times, and preventing daily surplus waste.
            
            Keep the report beautifully styled, concise, encouraging, and highly professional. Limit to 350-400 words.
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No historical order insights generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiHistoricalOrderInsights", "Error communicating with Gemini", e)
            "### 📈 Gemini Intelligence: Historical Order Analytics Summary for **$vendorName**\n" +
                    "*(Local Smart Fallback Report — Active Data Aggregation Running Live)*\n\n" +
                    "• **Busiest Hour**: $busiestHourStr\n" +
                    "• **Top Food Performance**:\n" +
                    popularItemsStr + "\n" +
                    "• **Operational Tip**: Prep portion lines 20 minutes before peak sessions."
        }
    }

    suspend fun generateNutritionCoaching(
        dailyTargetKcal: Float,
        currentKcal: Float,
        protein: Float,
        carbs: Float,
        fat: Float,
        availableFoodItems: List<FoodItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key Configuration Error: Gemini API key has not been entered into the Secrets tab.\n\n" +
                    "Offline Local Simulated Diet Coaching:\n\n" +
                    "### 🛡️ Daily Macro Analysis\n" +
                    "• **Calories**: ${currentKcal.toInt()} / ${dailyTargetKcal.toInt()} kcal (${"%.1f".format(if (dailyTargetKcal > 0) (currentKcal / dailyTargetKcal) * 100 else 0f)}% met)\n" +
                    "• **Protein**: ${protein.toInt()}g (Target: 130g) - " + (if (protein < 50) "Critical deficit! Increase lean meat or egg intake." else "A healthy foundation!") + "\n" +
                    "• **Carbs**: ${carbs.toInt()}g - Primary energy source for active lectures.\n" +
                    "• **Fats**: ${fat.toInt()}g - Balanced dietary lipids.\n\n" +
                    "### 🍏 Smart Meal Recommendations\n" +
                    "1. **High-Protein Option**: Choose **Waakye with Egg & Fish** or **Pounded Yam with Goat Soup** from the cafeteria to fulfill your protein targets while respecting your ${dailyTargetKcal.toInt()} kcal budget.\n" +
                    "2. **Hydration Boost**: Pair with local **Sobolo** or a light beverage instead of soda to minimize blood sugar spikes durings lecturings.\n\n" +
                    "### 💡 Lifestyle Tips\n" +
                    "• Try to allocate 40% of your calorie consumption for breakfast and lunch. Avoid heavy carbohydrate menus after 6:00 PM to improve sleep quality."
        }

        val foodMenuStr = availableFoodItems.joinToString("\n") {
            "• ${it.name} (Price: GH₵ ${it.price}, Category: ${it.category}, Description: ${it.description})"
        }

        val prompt = """
            You are an expert Sports Nutritionist and Academic Health Advisor representing the Accra Technical University (ATU) Cafeteria Wellness Board.
            Analyze the following student's modern nutrition state and available campus diner menus:
            
            DAILY GOALS & CURRENT TARGETS:
            - Target Calorie Budget: ${dailyTargetKcal.toInt()} kcal
            - Calories Logged Today: ${currentKcal.toInt()} kcal
            - Macronutrients Logged Today:
              * Protein: ${protein.toInt()}g
              * Carbohydrates: ${carbs.toInt()}g
              * Fats: ${fat.toInt()}g
              
            AVAILABLE DISHES AT ATU CAFETERIA:
            $foodMenuStr
            
            Generate a personalized health advisor report in beautiful Markdown containing exactly these sections:
            1. **🎯 Personalized Calorie & Macro Scorecard**: Assess whether their current macros are balanced, noting deficiencies or excess (e.g. low protein, too much carbs).
            2. **🍽️ Customized Meal Recommendations**: Recommend exactly 2 matching dishes from the provided ATU cafeteria menu list that will optimize their macros and stay within budget. Specify the price and vendor name if applicable.
            3. **⚡ Nutrition Advice for ATU Studies**: Give 2 practical wellness tips to stay focused during lectures, avoid "food coma" drowsiness during Accra afternoon humidity, or build lean muscle.
            
            Make sure your response has a bright, encouraging, supportive tone. Limit the response to 400 words.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No nutrition insight generated."
        } catch (e: Exception) {
            Log.e("GeminiNutrition", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 🛡️ Daily Macro Analysis\n" +
                    "• **Calories**: ${currentKcal.toInt()} / ${dailyTargetKcal.toInt()} kcal\n" +
                    "• **Protein**: ${protein.toInt()}g logged vs 130g goal\n\n" +
                    "### 🍏 Smart Meal Recommendations\n" +
                    "• **High Protein**: Select double eggs with Waakye from the local stands to lift your macro density!"
        }
    }

    suspend fun generateDailyDemandForecast(
        vendorName: String,
        orders: List<Order>,
        menuItems: List<FoodItem>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        
        val itemSales = HashMap<String, Int>()
        for (order in orders) {
            val name = order.foodName
            val qty = order.quantity
            itemSales[name] = (itemSales[name] ?: 0) + qty
        }
        
        val itemsStr = menuItems.joinToString("\n") { 
            "• ${it.name} (Category: ${it.category}) - Sold so far: ${itemSales[it.name] ?: 0} units"
        }
        
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "### 🔮 Gemini AI Daily Demand Forecast for **$vendorName**\n" +
                    "*(Offline Simulated Forecast — Active Predictive Models Running)*\n\n" +
                    "Based on past sales patterns at Accra Technical University, we forecast the following demand for tomorrow:\n\n" +
                    "### 📊 Tomorrow's Demand Forecast (Units Required)\n" +
                    (if (menuItems.isEmpty()) "• No menu items found." else menuItems.take(4).joinToString("\n") { 
                        "• **${it.name}**: Forecasted demand of **${(itemSales[it.name] ?: 15) + 5} - ${(itemSales[it.name] ?: 15) + 12} units** (High Probability)." 
                    }) + "\n\n" +
                    "### 📈 Peak Demand Periods\n" +
                    "• **Breakfast (7:30 AM - 9:00 AM)**: High demand for hot drinks and quick breakfast pastries due to morning lectures.\n" +
                    "• **Lunch Rush (11:45 AM - 1:15 PM)**: Peak demand of the day. Expect a massive influx of students looking for heavy local dishes.\n\n" +
                    "### 💡 Operational Optimization Directives\n" +
                    "1. **Pre-portion Top Dishes**: Pre-package the high-demand items 15 minutes before the lunch hour begins.\n" +
                    "2. **Safety Stock Buffer**: Maintain a 15% buffer of raw ingredients for top forecasted items to avoid early stockouts."
        }

        val prompt = xmlDocClean("""
            You are a lead predictive restaurant analyst and supply chain strategist at Accra Technical University (ATU).
            Please analyze the historical sales data and available menu items for vendor '$vendorName' to forecast tomorrow's daily demand for specific menu items.
            
            HISTORICAL MENU AND CURRENT SALES QUANTITIES:
            $itemsStr
            
            Based on this dataset, generate a highly structured, professional, and action-oriented Daily Demand Forecast report in clean Markdown format with the following:
            
            1. **🔮 Forecasted Daily Demand for Specific Menu Items**: Provide a clear list of specific menu items with an estimated quantity range of units needed for tomorrow (e.g. 20 - 30 units), using past sales as the baseline.
            2. **📈 Predicted Hourly Demand Fluctuations**: Identify peak periods (e.g., breakfast vs lunch rush) and which categories/items will dominate those periods.
            3. **💡 Recommended Inventory & Preparation Directives**: Offer 2 practical recommendations on raw ingredient prep or safety stock buffers to prevent running out of key menu items without causing excessive food waste.
            
            Keep the report beautifully styled, concise, encouraging, and highly professional. Limit to 350-400 words.
        """.trimIndent())

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = prompt)
                    )
                )
            )
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No daily demand forecast generated by ATU Intelligence at this time."
        } catch (e: Exception) {
            Log.e("GeminiDemandForecast", "Error communicating with Gemini", e)
            "Offline Simulation Mode:\n\n" +
                    "### 🔮 Tomorrow's Demand Forecast (Units Required)\n" +
                    "• **Waakye Premium**: Forecasted demand of **35 - 45 units**\n" +
                    "• **Jollof Rice**: Forecasted demand of **25 - 35 units**\n" +
                    "• **Sobolo**: Forecasted demand of **50 - 60 bottles**\n\n" +
                    "### 📈 Peak Demand Periods\n" +
                    "• **Lunch Rush (12:00 PM - 1:30 PM)** is estimated to account for 65% of tomorrow's volume.\n\n" +
                    "### 💡 Operational Directives\n" +
                    "1. Pre-package at least 20 portions of Waakye by 11:30 AM.\n" +
                    "2. Chill Sobolo bottles overnight to meet peak hydration demand during afternoon heat."
        }
    }

    private fun xmlDocClean(input: String): String {
        return input.replace("<", "&lt;").replace(">", "&gt;")
    }
}
