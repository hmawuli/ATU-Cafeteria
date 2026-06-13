package com.example.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Order
import com.example.data.FoodItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt

// ==========================================
// 1. RADAR / SPIDER CHART FOR FEEDBACK MATRIX
// ==========================================

@Composable
fun RadarFeedbackChart(
    metrics: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    val labels = listOf("Quality", "Cleanliness", "Speed", "Price-Value")
    // Map keys to index matching label order
    val values = listOf(
        metrics["foodQuality"] ?: 0.0,
        metrics["cleanliness"] ?: 0.0,
        metrics["speed"] ?: 0.0,
        metrics["priceValue"] ?: 0.0
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Multimodal Performance Web (Rating Index)",
            style = MaterialTheme.typography.titleSmall,
            color = labelColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(12.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width.coerceAtMost(size.height) / 2.2f

                // Draw Nested concentric web circles/polygons (representing scores 1, 2, 3, 4, 5)
                for (i in 1..5) {
                    val scaleFactor = i / 5f
                    val currentRadius = radius * scaleFactor
                    val polygonPath = Path()

                    for (j in 0..3) {
                        val angle = (j * 90f - 90f) * (Math.PI / 180f)
                        val x = center.x + currentRadius * cos(angle).toFloat()
                        val y = center.y + currentRadius * sin(angle).toFloat()
                        if (j == 0) {
                            polygonPath.moveTo(x, y)
                        } else {
                            polygonPath.lineTo(x, y)
                        }
                    }
                    polygonPath.close()
                    drawPath(
                        path = polygonPath,
                        color = outlineColor.copy(alpha = 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Draw Axis Spoke Lines
                for (j in 0..3) {
                    val angle = (j * 90f - 90f) * (Math.PI / 180f)
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    drawLine(
                        color = outlineColor,
                        start = center,
                        end = Offset(x, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw actual ratings web polygon overlay
                val ratingPath = Path()
                val scorePoints = mutableListOf<Offset>()

                for (j in 0..3) {
                    // Ratings constraint between 0.0 and 5.0
                    val rating = values[j].coerceIn(0.0, 5.0)
                    val scaleFactor = rating.toFloat() / 5f
                    val currentRadius = radius * scaleFactor

                    val angle = (j * 90f - 90f) * (Math.PI / 180f)
                    val x = center.x + currentRadius * cos(angle).toFloat()
                    val y = center.y + currentRadius * sin(angle).toFloat()
                    scorePoints.add(Offset(x, y))

                    if (j == 0) {
                        ratingPath.moveTo(x, y)
                    } else {
                        ratingPath.lineTo(x, y)
                    }
                }
                if (scorePoints.isNotEmpty()) {
                    ratingPath.close()

                    // Fill polygon
                    drawPath(
                        path = ratingPath,
                        color = primaryColor.copy(alpha = 0.25f)
                    )
                    // Stroke polygon
                    drawPath(
                        path = ratingPath,
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw circular anchors/vertices
                    scorePoints.forEach { point ->
                        drawCircle(
                            color = secondaryColor,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }
            }

            // Overlay Text labels manually at quadrants
            Text(labels[0], Modifier.align(Alignment.TopCenter), fontSize = 10.sp, color = labelColor)
            Text(labels[1], Modifier.align(Alignment.CenterEnd), fontSize = 10.sp, color = labelColor)
            Text(labels[2], Modifier.align(Alignment.BottomCenter), fontSize = 10.sp, color = labelColor)
            Text(labels[3], Modifier.align(Alignment.CenterStart), fontSize = 10.sp, color = labelColor)
        }
    }
}

// ==========================================
// 2. DAILY REVENUE BAR CHART FOR STORES
// ==========================================

@Composable
fun DailyRevenueBarChart(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    // Aggregate total revenue for the last 5 days
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("EEE", Locale.getDefault())

    val revenueMap = mutableMapOf<String, Double>()
    val daysList = mutableListOf<String>()

    // Bootstrap last 5 days with zero values
    for (i in 4 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val dayLabel = sdf.format(cal.time)
        daysList.add(dayLabel)
        revenueMap[dayLabel] = 0.0
    }

    // Populate actuals
    for (o in orders) {
        if (o.status == "COMPLETED") {
            calendar.timeInMillis = o.orderTimestamp
            val dayLabel = sdf.format(calendar.time)
            if (revenueMap.containsKey(dayLabel)) {
                revenueMap[dayLabel] = (revenueMap[dayLabel] ?: 0.0) + o.totalPrice
            }
        }
    }

    val maxRevenue = revenueMap.values.maxOrNull()?.toFloat()?.coerceAtLeast(10f) ?: 10f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Daily Revenue History (GH₵)",
            style = MaterialTheme.typography.titleSmall,
            color = labelColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 40.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Draw horizontal volume grids
                val gridLinesCount = 3
                for (i in 0..gridLinesCount) {
                    val y = chartHeight * (i / gridLinesCount.toFloat())
                    drawLine(
                        color = gridColor.copy(alpha = 0.4f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Render coordinate frame lines
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // Plot columns
                val numDays = daysList.size
                val spaceBetween = chartWidth / numDays
                val barWidth = (spaceBetween * 0.5f)

                daysList.forEachIndexed { index, day ->
                    val value = (revenueMap[day] ?: 0.0).toFloat()
                    val barHeight = (value / maxRevenue) * chartHeight
                    val startX = paddingLeft + (index * spaceBetween) + (spaceBetween - barWidth) / 2f
                    val startY = chartHeight - barHeight

                    // Draw solid rect bar
                    drawRect(
                        color = barColor,
                        topLeft = Offset(startX, startY),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                    )
                }
            }

            // Labels overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp)
                    .align(Alignment.BottomStart)
                    .height(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                daysList.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 9.sp,
                        color = labelColor
                    )
                }
            }

            // Max scale amount overlay
            Text(
                text = "GH₵${maxRevenue.toInt()}",
                fontSize = 8.sp,
                color = labelColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp)
            )
        }
    }
}

// ==========================================
// 3. STUDENT TRENDS & FREQUENCY LINE CHART
// ==========================================

data class TrendPoint(
    val dayLabel: String,
    val spend: Double,
    val count: Int
)

@Composable
fun StudentTrendsLineChart(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(0) } // 0: Spend, 1: Frequency
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface

    // Generate date labels in the last 30 days
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L

    val points = remember(orders, selectedMetric) {
        val pts = mutableListOf<TrendPoint>()
        for (i in 29 downTo 0) {
            val dayMillis = now - (i * dayMs)
            pts.add(TrendPoint(sdf.format(Date(dayMillis)), 0.0, 0))
        }

        val orderSdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        orders.forEach { order ->
            val orderDayLabel = orderSdf.format(Date(order.orderTimestamp))
            val index = pts.indexOfFirst { it.dayLabel == orderDayLabel }
            if (index != -1) {
                val currentPt = pts[index]
                pts[index] = currentPt.copy(
                    spend = currentPt.spend + order.totalPrice,
                    count = currentPt.count + 1
                )
            }
        }
        pts
    }

    val maxVal = remember(points, selectedMetric) {
        if (selectedMetric == 0) {
            points.maxOfOrNull { it.spend }?.toFloat()?.coerceAtLeast(10f) ?: 10f
        } else {
            points.maxOfOrNull { it.count }?.toFloat()?.coerceAtLeast(4f) ?: 4f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "30-Day Budget & Frequency Tracker",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )

            // Dynamic Option Badges
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(2.dp)
            ) {
                listOf("Spend (GH₵)", "Volume").forEachIndexed { idx, text ->
                    val isSelected = selectedMetric == idx
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedMetric = idx }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingLeft = 45.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Grids
                val gridLinesCount = 3
                for (i in 0..gridLinesCount) {
                    val y = chartHeight * (i / gridLinesCount.toFloat())
                    drawLine(
                        color = gridColor.copy(alpha = 0.3f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Coordinate Frame Axes
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                if (points.isNotEmpty()) {
                    val numPoints = points.size
                    val segmentWidth = chartWidth / (numPoints - 1)

                    val linePath = Path()
                    val fillPath = Path()

                    points.forEachIndexed { idx, pt ->
                        val value = if (selectedMetric == 0) pt.spend.toFloat() else pt.count.toFloat()
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (value / maxVal) * chartHeight

                        if (idx == 0) {
                            linePath.moveTo(cx, cy)
                            fillPath.moveTo(cx, chartHeight)
                            fillPath.lineTo(cx, cy)
                        } else {
                            linePath.lineTo(cx, cy)
                            fillPath.lineTo(cx, cy)
                        }

                        if (idx == numPoints - 1) {
                            fillPath.lineTo(cx, chartHeight)
                            fillPath.close()
                        }
                    }

                    // Render Transparent Underlay Fill
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Render Sleek Line Stroke
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Overlay circular indicator anchors for non-zero points
                    points.forEachIndexed { idx, pt ->
                        val value = if (selectedMetric == 0) pt.spend.toFloat() else pt.count.toFloat()
                        if (value > 0f) {
                            val cx = paddingLeft + (idx * segmentWidth)
                            val cy = chartHeight - (value / maxVal) * chartHeight
                            drawCircle(
                                color = secondaryColor,
                                radius = 3.5.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = 1.5.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }

            // Labels overlay
            if (points.size >= 30) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 45.dp)
                        .align(Alignment.BottomStart)
                        .height(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "30d ago (" + points[0].dayLabel + ")", fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    Text(text = points[10].dayLabel, fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    Text(text = points[20].dayLabel, fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    Text(text = "Today (" + points[29].dayLabel + ")", fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                }
            }

            // Max value callout
            Text(
                text = if (selectedMetric == 0) "GH₵ ${"%.1f".format(maxVal)}" else "${maxVal.toInt()} orders",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp)
            )

            // Zero baseline indicator
            Text(
                text = "0",
                fontSize = 8.sp,
                color = labelColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 2.dp, bottom = 20.dp)
            )
        }
    }
}

// ==========================================
// 4. VENDOR PERFORMANCE TRENDS LINE CHART
// ==========================================

@Composable
fun VendorPerformanceTrendChart(
    performanceData: List<com.example.data.LaravelDailyPerformance>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Sort ascending chronologically by date
    val chronologicalData = remember(performanceData) {
        performanceData.sortedBy { it.order_date }
    }

    val maxRevenue = remember(chronologicalData) {
        chronologicalData.maxOfOrNull { it.daily_revenue }?.toFloat()?.coerceAtLeast(10f) ?: 10f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chronological Revenue (Laravel Backend)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(primaryColor, androidx.compose.foundation.shape.CircleShape)
                )
                Text("Daily Trend Rate", fontSize = 9.sp, color = labelColor.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (chronologicalData.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No daily performance data loaded from controller.",
                        fontSize = 11.sp,
                        color = labelColor.copy(alpha = 0.5f)
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paddingLeft = 45.dp.toPx()
                    val paddingBottom = 20.dp.toPx()
                    val chartWidth = size.width - paddingLeft
                    val chartHeight = size.height - paddingBottom

                    // Grids
                    val gridLinesCount = 3
                    for (i in 0..gridLinesCount) {
                        val y = chartHeight * (i / gridLinesCount.toFloat())
                        drawLine(
                            color = gridColor.copy(alpha = 0.3f),
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width, y),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    // Coordinate Frame Axes
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, 0f),
                        end = Offset(paddingLeft, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(paddingLeft, chartHeight),
                        end = Offset(size.width, chartHeight),
                        strokeWidth = 1.dp.toPx()
                    )

                    val numPoints = chronologicalData.size
                    val segmentWidth = if (numPoints > 1) chartWidth / (numPoints - 1) else chartWidth

                    val linePath = Path()
                    val fillPath = Path()

                    chronologicalData.forEachIndexed { idx, pt ->
                        val value = pt.daily_revenue.toFloat()
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (value / maxRevenue) * chartHeight

                        if (idx == 0) {
                            linePath.moveTo(cx, cy)
                            fillPath.moveTo(cx, chartHeight)
                            fillPath.lineTo(cx, cy)
                        } else {
                            val prevPt = chronologicalData[idx - 1]
                            val prevX = paddingLeft + ((idx - 1) * segmentWidth)
                            val prevY = chartHeight - (prevPt.daily_revenue.toFloat() / maxRevenue) * chartHeight
                            
                            // Cubic Bezier spline interpolation representing modern Recharts line
                            linePath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                            fillPath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                        }

                        if (idx == numPoints - 1) {
                            fillPath.lineTo(cx, chartHeight)
                            fillPath.lineTo(paddingLeft, chartHeight)
                            fillPath.close()
                        }
                    }

                    // Render Recharts-style Transparent Underlay Gradient Fill
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )

                    // Render Sleek Line Stroke
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Overlay point circular anchors
                    chronologicalData.forEachIndexed { idx, pt ->
                        val value = pt.daily_revenue.toFloat()
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (value / maxRevenue) * chartHeight

                        drawCircle(
                            color = secondaryColor,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = surfaceColor,
                            radius = 2.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }

                // Labels overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 45.dp)
                        .align(Alignment.BottomStart)
                        .height(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formatLabel = { fullDate: String ->
                        try {
                            if (fullDate.contains("-") && fullDate.length >= 10) {
                                fullDate.substring(5) // Extract MM-DD for clean aesthetics
                            } else fullDate
                        } catch (e: Exception) {
                            fullDate
                        }
                    }

                    Text(text = formatLabel(chronologicalData[0].order_date), fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    if (chronologicalData.size > 2) {
                        Text(text = formatLabel(chronologicalData[chronologicalData.size / 2].order_date), fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    }
                    if (chronologicalData.size > 1) {
                        Text(text = formatLabel(chronologicalData[chronologicalData.size - 1].order_date), fontSize = 8.sp, color = labelColor.copy(alpha = 0.6f))
                    }
                }

                // Max value callout GH₵
                Text(
                    text = "GH₵ ${"%.1f".format(maxRevenue)}",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 2.dp)
                )

                // Zero baseline indicator
                Text(
                    text = "0",
                    fontSize = 8.sp,
                    color = labelColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 2.dp, bottom = 20.dp)
                )
            }
        }
    }
}

// ==========================================
// 5. WEEKLY REVENUE TREND LINE CHART (RECHARTS STYLE)
// ==========================================

data class WeeklyRevenueDataPoint(
    val dateLabel: String,
    val dayKey: String,
    val revenue: Double
)

@Composable
fun WeeklyRevenueTrendLineChart(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    // Last 7 days data generation
    val weeklyData = remember(orders) {
        val list = mutableListOf<WeeklyRevenueDataPoint>()
        val sdfLabel = SimpleDateFormat("EEE (MM/dd)", Locale.US)
        val sdfDayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            
            val dayStartCal = Calendar.getInstance().apply {
                time = cal.time
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMillis = dayStartCal.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000L - 1
            
            val dateLabel = sdfLabel.format(cal.time)
            val dayKey = sdfDayKey.format(cal.time)
            
            val completedRev = orders.filter {
                it.status == "COMPLETED" && it.orderTimestamp in startMillis..endMillis
            }.sumOf { it.totalPrice }
            
            list.add(WeeklyRevenueDataPoint(dateLabel, dayKey, completedRev))
        }
        list
    }

    val maxRevenue = remember(weeklyData) {
        weeklyData.maxOfOrNull { it.revenue }?.toFloat()?.coerceAtLeast(10f) ?: 10f
    }
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("weekly_revenue_trend_line_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Weekly Revenue Trend (Last 7 Days)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
                Text(
                    text = "Completed Orders Revenue History",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = labelColor.copy(alpha = 0.6f)
                )
            }
            
            // Icon Badge
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "7D Revenue Trend",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(weeklyData) {
                        detectTapGestures(
                            onPress = { offset ->
                                val paddingLeft = 45.dp.toPx()
                                val paddingBottom = 20.dp.toPx()
                                val chartWidth = size.width - paddingLeft
                                val spaceBetween = chartWidth / (weeklyData.size - 1)
                                
                                val rawIdx = ((offset.x - paddingLeft) / spaceBetween).roundToInt()
                                selectedIndex = rawIdx.coerceIn(0, weeklyData.size - 1)
                            }
                        )
                    }
                    .pointerInput(weeklyData) {
                        detectDragGestures(
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null },
                            onDrag = { change, dragAmount ->
                                val paddingLeft = 45.dp.toPx()
                                val paddingBottom = 20.dp.toPx()
                                val chartWidth = size.width - paddingLeft
                                val spaceBetween = chartWidth / (weeklyData.size - 1)
                                
                                val rawIdx = ((change.position.x - paddingLeft) / spaceBetween).roundToInt()
                                selectedIndex = rawIdx.coerceIn(0, weeklyData.size - 1)
                            }
                        )
                    }
            ) {
                val paddingLeft = 45.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Horizontal Grid lines and axis labels
                val gridLinesCount = 3
                for (i in 0..gridLinesCount) {
                    val y = chartHeight * (i / gridLinesCount.toFloat())
                    drawLine(
                        color = gridColor.copy(alpha = 0.3f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Coordinate Frame Axes
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                // Render Recharts spline path
                if (weeklyData.isNotEmpty()) {
                    val segmentWidth = chartWidth / (weeklyData.size - 1)
                    val linePath = Path()
                    val fillPath = Path()

                    weeklyData.forEachIndexed { idx, pt ->
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (pt.revenue.toFloat() / maxRevenue) * chartHeight

                        if (idx == 0) {
                            linePath.moveTo(cx, cy)
                            fillPath.moveTo(cx, chartHeight)
                            fillPath.lineTo(cx, cy)
                        } else {
                            val prevPt = weeklyData[idx - 1]
                            val prevX = paddingLeft + ((idx - 1) * segmentWidth)
                            val prevY = chartHeight - (prevPt.revenue.toFloat() / maxRevenue) * chartHeight

                            // Cubic bezier spline interpolation representing elegant Recharts trend curve
                            linePath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                            fillPath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                        }

                        if (idx == weeklyData.size - 1) {
                            fillPath.lineTo(cx, chartHeight)
                            fillPath.lineTo(paddingLeft, chartHeight)
                            fillPath.close()
                        }
                    }

                    // Draw translucent underlay gradient representation of Recharts AreaChart
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw spline curve stroke line
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Optional Guideline for selection
                    selectedIndex?.let { hoverIdx ->
                        val hx = paddingLeft + (hoverIdx * segmentWidth)
                        val hy = chartHeight - (weeklyData[hoverIdx].revenue.toFloat() / maxRevenue) * chartHeight
                        
                        // Vertical dotted reference line
                        drawLine(
                            color = secondaryColor.copy(alpha = 0.7f),
                            start = Offset(hx, 0f),
                            end = Offset(hx, chartHeight),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw focus intersection anchor point
                        drawCircle(
                            color = secondaryColor,
                            radius = 6.dp.toPx(),
                            center = Offset(hx, hy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(hx, hy)
                        )
                    }

                    // Normal point anchors
                    weeklyData.forEachIndexed { idx, pt ->
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (pt.revenue.toFloat() / maxRevenue) * chartHeight

                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // High Precision Axis Indicators
            // Max Value Tick Label
            Text(
                text = "GH₵ ${"%.1f".format(maxRevenue)}",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp)
            )

            // Zero Value Tick Label
            Text(
                text = "0",
                fontSize = 8.sp,
                color = labelColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 2.dp, bottom = 22.dp)
            )

            // Horizontal Date labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 45.dp)
                    .align(Alignment.BottomStart)
                    .height(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (weeklyData.isNotEmpty()) {
                    Text(text = weeklyData[0].dateLabel, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    if (weeklyData.size > 3) {
                        Text(text = weeklyData[weeklyData.size / 2].dateLabel, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    }
                    if (weeklyData.size > 1) {
                        Text(text = weeklyData[weeklyData.size - 1].dateLabel, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Selected interactive point hover card feedback ("Tooltip")
        selectedIndex?.let { idx ->
            if (idx in weeklyData.indices) {
                val dataPoint = weeklyData[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("weekly_chart_tooltip"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected Point:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(dataPoint.dateLabel, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Day Revenue:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("GH₵ ${"%.2f".format(dataPoint.revenue)}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        } ?: run {
            // Interactive assistance description instruction
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💡 Tap or drag across the line graph above to inspect exact daily performance stats.",
                    fontSize = 9.sp,
                    color = labelColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun LaravelDailyRevenueTrendChart(
    dailyRevenueResponse: com.example.data.LaravelDailyRevenueResponse?,
    modifier: Modifier = Modifier
) {
    if (dailyRevenueResponse == null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(150.dp)
                .testTag("laravel_daily_revenue_trend_empty_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Loading daily revenue trends from Laravel...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val dailyData = dailyRevenueResponse.data
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurface
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    if (dailyData.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(150.dp)
                .testTag("laravel_daily_revenue_trend_no_data"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded completed sales from Laravel database for this vendor yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = labelColor.copy(alpha = 0.6f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        return
    }

    val maxRevenue = remember(dailyData) {
        dailyData.maxOfOrNull { it.revenue }?.toFloat()?.coerceAtLeast(10f) ?: 10f
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("laravel_daily_revenue_trend_chart")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Laravel Daily Revenue Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
                Text(
                    text = "Direct server-aggregated sales from completed pre-orders",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = labelColor.copy(alpha = 0.6f)
                )
            }

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Laravel Live API",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(dailyData) {
                        detectTapGestures(
                            onPress = { offset ->
                                val paddingLeft = 45.dp.toPx()
                                val paddingBottom = 20.dp.toPx()
                                val chartWidth = size.width - paddingLeft
                                val spaceBetween = if (dailyData.size > 1) chartWidth / (dailyData.size - 1) else chartWidth

                                val rawIdx = if (dailyData.size > 1) {
                                    ((offset.x - paddingLeft) / spaceBetween).roundToInt()
                                } else {
                                    0
                                }
                                selectedIndex = rawIdx.coerceIn(0, dailyData.size - 1)
                            }
                        )
                    }
                    .pointerInput(dailyData) {
                        detectDragGestures(
                            onDragEnd = { selectedIndex = null },
                            onDragCancel = { selectedIndex = null },
                            onDrag = { change, _ ->
                                val paddingLeft = 45.dp.toPx()
                                val paddingBottom = 20.dp.toPx()
                                val chartWidth = size.width - paddingLeft
                                val spaceBetween = if (dailyData.size > 1) chartWidth / (dailyData.size - 1) else chartWidth

                                val rawIdx = if (dailyData.size > 1) {
                                    ((change.position.x - paddingLeft) / spaceBetween).roundToInt()
                                } else {
                                    0
                                }
                                selectedIndex = rawIdx.coerceIn(0, dailyData.size - 1)
                            }
                        )
                    }
            ) {
                val paddingLeft = 45.dp.toPx()
                val paddingBottom = 20.dp.toPx()
                val chartWidth = size.width - paddingLeft
                val chartHeight = size.height - paddingBottom

                // Grids
                val gridLinesCount = 3
                for (i in 0..gridLinesCount) {
                    val y = chartHeight * (i / gridLinesCount.toFloat())
                    drawLine(
                        color = gridColor.copy(alpha = 0.3f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Coordinate Frame Axes
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                if (dailyData.isNotEmpty()) {
                    val segmentWidth = if (dailyData.size > 1) chartWidth / (dailyData.size - 1) else chartWidth
                    val linePath = Path()
                    val fillPath = Path()

                    dailyData.forEachIndexed { idx, pt ->
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (pt.revenue.toFloat() / maxRevenue) * chartHeight

                        if (idx == 0) {
                            linePath.moveTo(cx, cy)
                            fillPath.moveTo(cx, chartHeight)
                            fillPath.lineTo(cx, cy)
                        } else {
                            val prevPt = dailyData[idx - 1]
                            val prevX = paddingLeft + ((idx - 1) * segmentWidth)
                            val prevY = chartHeight - (prevPt.revenue.toFloat() / maxRevenue) * chartHeight

                            // Cubic bezier spline interpolation representing elegant Recharts trend curve
                            linePath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                            fillPath.cubicTo(
                                (prevX + cx) / 2f, prevY,
                                (prevX + cx) / 2f, cy,
                                cx, cy
                            )
                        }

                        if (idx == dailyData.size - 1) {
                            fillPath.lineTo(cx, chartHeight)
                            fillPath.lineTo(paddingLeft, chartHeight)
                            fillPath.close()
                        }
                    }

                    // Draw translucent underlay gradient representation of Recharts AreaChart
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw spline curve stroke line
                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Optional Guideline for selection
                    selectedIndex?.let { hoverIdx ->
                        if (hoverIdx in dailyData.indices) {
                            val hx = paddingLeft + (hoverIdx * segmentWidth)
                            val hy = chartHeight - (dailyData[hoverIdx].revenue.toFloat() / maxRevenue) * chartHeight

                            // Vertical dotted reference line
                            drawLine(
                                color = secondaryColor.copy(alpha = 0.7f),
                                start = Offset(hx, 0f),
                                end = Offset(hx, chartHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Draw focus intersection anchor point
                            drawCircle(
                                color = secondaryColor,
                                radius = 6.dp.toPx(),
                                center = Offset(hx, hy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 3.dp.toPx(),
                                center = Offset(hx, hy)
                            )
                        }
                    }

                    // Normal point anchors
                    dailyData.forEachIndexed { idx, pt ->
                        val cx = paddingLeft + (idx * segmentWidth)
                        val cy = chartHeight - (pt.revenue.toFloat() / maxRevenue) * chartHeight

                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.5.dp.toPx(),
                            center = Offset(cx, cy)
                        )
                    }
                }
            }

            // High Precision Axis Indicators
            Text(
                text = "GH₵ ${"%.1f".format(maxRevenue)}",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp)
            )

            Text(
                text = "0",
                fontSize = 8.sp,
                color = labelColor.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 2.dp, bottom = 22.dp)
            )

            // Horizontal Date labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 45.dp)
                    .align(Alignment.BottomStart)
                    .height(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dailyData.isNotEmpty()) {
                    Text(text = dailyData[0].date, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    if (dailyData.size > 3) {
                        Text(text = dailyData[dailyData.size / 2].date, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    }
                    if (dailyData.size > 1) {
                        Text(text = dailyData[dailyData.size - 1].date, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = labelColor.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // Selected interactive point hover card feedback ("Tooltip")
        selectedIndex?.let { idx ->
            if (idx in dailyData.indices) {
                val dataPoint = dailyData[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("laravel_daily_chart_tooltip"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Reporting Date:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text(dataPoint.date, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Orders:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("${dataPoint.orders_count} ords", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Day Earnings:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("GH₵ ${"%.2f".format(dataPoint.revenue)}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💡 Tap or swipe the line chart to examine precise date-by-date order and revenue analytics.",
                    fontSize = 9.sp,
                    color = labelColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun D3DashboardChart(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val parsedData = remember(orders) {
        orders.groupBy {
            sdf.format(Date(it.orderTimestamp))
        }.mapValues { entry ->
            val volume = entry.value.size
            val revenue = entry.value.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
            Pair(volume, revenue)
        }.toSortedMap()
    }

    val jsonStr = remember(parsedData) {
        val jsonBuilder = StringBuilder("[")
        parsedData.entries.forEachIndexed { index, entry ->
            jsonBuilder.append("{")
            jsonBuilder.append("\"date\":\"${entry.key}\",")
            jsonBuilder.append("\"volume\":${entry.value.first},")
            jsonBuilder.append("\"revenue\":${entry.value.second}")
            jsonBuilder.append("}")
            if (index < parsedData.size - 1) {
                jsonBuilder.append(",")
            }
        }
        jsonBuilder.append("]")
        jsonBuilder.toString()
    }

    val htmlContent = remember(jsonStr) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://d3js.org/d3.v7.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    margin: 0;
                    padding: 8px;
                    background-color: #1a1a1a;
                    color: #e0e0e0;
                }
                .chart-container {
                    background-color: #212121;
                    border-radius: 8px;
                    padding: 12px;
                    margin-bottom: 12px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.15);
                    border: 1px solid #333333;
                }
                h3 {
                    margin-top: 0;
                    margin-bottom: 4px;
                    color: #2196F3;
                    font-size: 11px;
                    font-weight: bold;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .bar {
                    fill: #2196F3;
                    rx: 2;
                }
                .line {
                    fill: none;
                    stroke: #4CAF50;
                    stroke-width: 2.5;
                }
                .dot {
                    fill: #4CAF50;
                    stroke: #212121;
                    stroke-width: 1.5;
                }
                .axis text {
                    fill: #aaaaaa;
                    font-size: 8px;
                }
                .axis path, .axis line {
                    stroke: #444444;
                }
                .grid line {
                    stroke: #333333;
                    stroke-opacity: 0.5;
                    shape-rendering: crispEdges;
                }
                .tooltip {
                    position: absolute;
                    background-color: rgba(30,30,30,0.95);
                    border: 1px solid #555555;
                    color: #ffffff;
                    padding: 6px;
                    border-radius: 4px;
                    pointer-events: none;
                    font-size: 9px;
                    opacity: 0;
                    transition: opacity 0.15s;
                    z-index: 9999;
                }
            </style>
        </head>
        <body>
            <div class="chart-container">
                <h3>📊 D3.js Daily Order Volume</h3>
                <div id="volume-chart"></div>
            </div>
            <div class="chart-container">
                <h3>📈 D3.js Total Revenue Trend (GH₵)</h3>
                <div id="revenue-chart"></div>
            </div>

            <div class="tooltip" id="tooltip"></div>

            <script>
                const data = $jsonStr;
                
                const margin = {top: 15, right: 15, bottom: 30, left: 35};
                const width = window.innerWidth - margin.left - margin.right - 20;
                const height = 110 - margin.top - margin.bottom;

                // Tooltip
                const tooltip = d3.select("#tooltip");

                if (data.length === 0) {
                    const emptyInfo = "<div style='font-size:10px;color:#888;text-align:center;padding:12px;'>No order transactions in filters.</div>";
                    document.getElementById("volume-chart").innerHTML = emptyInfo;
                    document.getElementById("revenue-chart").innerHTML = emptyInfo;
                } else {
                    // --- volume chart ---
                    const svgVol = d3.select("#volume-chart")
                        .append("svg")
                        .attr("width", width + margin.left + margin.right)
                        .attr("height", height + margin.top + margin.bottom)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                    const xVol = d3.scaleBand()
                        .range([0, width])
                        .domain(data.map(d => d.date))
                        .padding(0.3);

                    const yVol = d3.scaleLinear()
                        .range([height, 0])
                        .domain([0, d3.max(data, d => d.volume) || 5]);

                    // Grids
                    svgVol.append("g")			
                        .attr("class", "grid")
                        .call(d3.axisLeft(yVol).tickSize(-width).tickFormat(""));

                    svgVol.append("g")
                        .attr("class", "axis")
                        .attr("transform", "translate(0," + height + ")")
                        .call(d3.axisBottom(xVol))
                        .selectAll("text")
                        .style("text-anchor", "end")
                        .attr("dx", "-.5em")
                        .attr("dy", ".15em")
                        .attr("transform", "rotate(-25)");

                    svgVol.append("g")
                        .attr("class", "axis")
                        .call(d3.axisLeft(yVol).ticks(4));

                    svgVol.selectAll(".bar")
                        .data(data)
                        .enter().append("rect")
                        .attr("class", "bar")
                        .attr("x", d => xVol(d.date))
                        .attr("width", xVol.bandwidth())
                        .attr("y", d => yVol(d.volume))
                        .attr("height", d => height - yVol(d.volume))
                        .on("touchstart", function(event, d) {
                            tooltip.style("opacity", 1)
                                .html("Date: " + d.date + "<br>Orders: " + d.volume);
                        })
                        .on("touchend", function() {
                            tooltip.style("opacity", 0);
                        });

                    // --- revenue chart ---
                    const svgRev = d3.select("#revenue-chart")
                        .append("svg")
                        .attr("width", width + margin.left + margin.right)
                        .attr("height", height + margin.top + margin.bottom)
                        .append("g")
                        .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                    const xRev = d3.scalePoint()
                        .range([0, width])
                        .domain(data.map(d => d.date))
                        .padding(0.3);

                    const yRev = d3.scaleLinear()
                        .range([height, 0])
                        .domain([0, d3.max(data, d => d.revenue) * 1.1 || 10]);

                    svgRev.append("g")			
                        .attr("class", "grid")
                        .call(d3.axisLeft(yRev).tickSize(-width).tickFormat(""));

                    svgRev.append("g")
                        .attr("class", "axis")
                        .attr("transform", "translate(0," + height + ")")
                        .call(d3.axisBottom(xRev))
                        .selectAll("text")
                        .style("text-anchor", "end")
                        .attr("dx", "-.5em")
                        .attr("dy", ".15em")
                        .attr("transform", "rotate(-25)");

                    svgRev.append("g")
                        .attr("class", "axis")
                        .call(d3.axisLeft(yRev).ticks(4));

                    const valueline = d3.line()
                        .x(d => xRev(d.date))
                        .y(d => yRev(d.revenue))
                        .curve(d3.curveMonotoneX);

                    svgRev.append("path")
                        .data([data])
                        .attr("class", "line")
                        .attr("d", valueline);

                    svgRev.selectAll(".dot")
                        .data(data)
                        .enter().append("circle")
                        .attr("class", "dot")
                        .attr("cx", d => xRev(d.date))
                        .attr("cy", d => yRev(d.revenue))
                        .attr("r", 4)
                        .on("touchstart", function(event, d) {
                            tooltip.style("opacity", 1)
                                .html("Date: " + d.date + "<br>Revenue: GH₵ " + d.revenue.toFixed(2));
                        })
                        .on("touchend", function() {
                            tooltip.style("opacity", 0);
                        });
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("d3_js_vendor_dashboard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📈", fontSize = 20.sp)
                Column {
                    Text(
                        text = "D3.js Interactive Visualizer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Sophisticated charting powered by D3 web standards engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = android.webkit.WebViewClient()
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        }
    }
}

@Composable
fun RechartsDashboardChart(
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val dailyJson = remember(orders) {
        val list = mutableListOf<String>()
        val sdfLabel = SimpleDateFormat("MM-dd", Locale.US)
        for (i in 13 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            
            val dayStart = cal.clone() as Calendar
            dayStart.set(Calendar.HOUR_OF_DAY, 0)
            dayStart.set(Calendar.MINUTE, 0)
            dayStart.set(Calendar.SECOND, 0)
            dayStart.set(Calendar.MILLISECOND, 0)
            val startMillis = dayStart.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000L - 1
            
            val dateLabel = sdfLabel.format(cal.time)
            
            val completedRev = orders.filter {
                it.orderTimestamp in startMillis..endMillis && it.status == "COMPLETED"
            }.sumOf { it.totalPrice }
            
            val completedCount = orders.count {
                it.orderTimestamp in startMillis..endMillis && it.status == "COMPLETED"
            }
            
            list.add("""{"date": "$dateLabel", "revenue": $completedRev, "count": $completedCount}""")
        }
        list.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    val weeklyJson = remember(orders) {
        val list = mutableListOf<String>()
        for (i in 7 downTo 0) {
            val weekCal = Calendar.getInstance()
            weekCal.add(Calendar.WEEK_OF_YEAR, -i)
            
            val startOfWeek = weekCal.clone() as Calendar
            startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
            startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
            startOfWeek.set(Calendar.MINUTE, 0)
            startOfWeek.set(Calendar.SECOND, 0)
            startOfWeek.set(Calendar.MILLISECOND, 0)
            
            val endOfWeek = startOfWeek.clone() as Calendar
            endOfWeek.add(Calendar.DAY_OF_WEEK, 7)
            endOfWeek.add(Calendar.MILLISECOND, -1)
            
            val startMillis = startOfWeek.timeInMillis
            val endMillis = endOfWeek.timeInMillis
            
            val formattedWeekLabel = if (i == 0) "Current Wk" else "Wk -$i"
            
            val completedRev = orders.filter {
                it.orderTimestamp in startMillis..endMillis && it.status == "COMPLETED"
            }.sumOf { it.totalPrice }
            
            val completedCount = orders.count {
                it.orderTimestamp in startMillis..endMillis && it.status == "COMPLETED"
            }
            
            list.add("""{"week": "$formattedWeekLabel", "revenue": $completedRev, "count": $completedCount}""")
        }
        list.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    val totalRevenue = remember(orders) {
        orders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
    }
    val totalVolume = remember(orders) {
        orders.count { it.status == "COMPLETED" }
    }

    val htmlContent = remember(dailyJson, weeklyJson, totalRevenue, totalVolume) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Recharts Dashboard</title>
            <!-- Load React -->
            <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
            <!-- Load Prop-Types -->
            <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js" crossorigin></script>
            <!-- Load Recharts -->
            <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.js" crossorigin></script>
            <!-- Load Babel -->
            <script src="https://unpkg.com/@babel/standalone/babel.min.js" crossorigin></script>
            <style>
                body {
                    margin: 0;
                    padding: 8px;
                    background-color: #1a1a1a;
                    color: #e0e0e0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                .card {
                    background-color: #212121;
                    border: 1px solid #333333;
                    border-radius: 8px;
                    padding: 12px;
                    margin-bottom: 12px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.15);
                }
                .header {
                    margin-bottom: 12px;
                }
                .title {
                    font-size: 11px;
                    font-weight: bold;
                    color: #4fc3f7;
                    margin: 0;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .subtitle {
                    font-size: 9px;
                    color: #888;
                    margin: 2px 0 0 0;
                }
                .chart-container {
                    height: 180px;
                    position: relative;
                }
                .stats-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 12px;
                    gap: 8px;
                }
                .stat-card {
                    background-color: #242424;
                    border: 1px solid #3a3a3a;
                    border-radius: 6px;
                    padding: 8px;
                    flex: 1;
                    text-align: center;
                }
                .stat-value {
                    font-size: 13px;
                    font-weight: bold;
                    color: #81c784;
                }
                .stat-label {
                    font-size: 8px;
                    color: #aaa;
                    margin-top: 2px;
                    text-transform: uppercase;
                }
            </style>
        </head>
        <body>
            <div id="root"></div>

            <script type="text/babel">
                const { 
                    AreaChart, Area, BarChart, Bar, LineChart, Line,
                    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
                } = Recharts;

                const dailyData = $dailyJson;
                const weeklyData = $weeklyJson;
                const totalVolume = $totalVolume;
                const totalRevenue = $totalRevenue;

                function App() {
                    return (
                        <div>
                            <div className="stats-row">
                                <div className="stat-card">
                                    <div className="stat-value">GH₵ {totalRevenue.toFixed(2)}</div>
                                    <div className="stat-label">Total Revenue</div>
                                </div>
                                <div className="stat-card">
                                    <div className="stat-value">{totalVolume}</div>
                                    <div className="stat-label">Order Volume</div>
                                </div>
                            </div>

                            <div className="card">
                                <div className="header">
                                    <p className="title">Daily Revenue Trend</p>
                                    <p className="subtitle">Interactive 14-day sales lifecycle</p>
                                </div>
                                <div className="chart-container">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={dailyData} margin={{ top: 5, right: 5, left: -25, bottom: 5 }}>
                                            <defs>
                                                <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#4fc3f7" stopOpacity={0.8}/>
                                                    <stop offset="95%" stopColor="#4fc3f7" stopOpacity={0.1}/>
                                                </linearGradient>
                                            </defs>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#2d2d2d" />
                                            <XAxis dataKey="date" stroke="#888" style={{ fontSize: '8px' }} />
                                            <YAxis stroke="#888" style={{ fontSize: '8px' }} />
                                            <Tooltip contentStyle={{ backgroundColor: '#222', borderColor: '#444', fontSize: '9px' }} />
                                            <Legend wrapperStyle={{ fontSize: '9px', marginTop: '4px' }} />
                                            <Area type="monotone" dataKey="revenue" name="Revenue (GH₵)" stroke="#4fc3f7" fillOpacity={1} fill="url(#colorRevenue)" />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>

                            <div className="card">
                                <div className="header">
                                    <p className="title">Weekly Business Revenue Progression</p>
                                    <p className="subtitle">Aggregate order revenue trends grouped by full calendar weeks</p>
                                </div>
                                <div className="chart-container">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <BarChart data={weeklyData} margin={{ top: 5, right: 5, left: -25, bottom: 5 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#2d2d2d" />
                                            <XAxis dataKey="week" stroke="#888" style={{ fontSize: '8px' }} />
                                            <YAxis stroke="#888" style={{ fontSize: '8px' }} />
                                            <Tooltip contentStyle={{ backgroundColor: '#222', borderColor: '#444', fontSize: '9px' }} />
                                            <Legend wrapperStyle={{ fontSize: '9px', marginTop: '4px' }} />
                                            <Bar dataKey="revenue" name="Weekly Rev (GH₵)" fill="#81c784" radius={[4, 4, 0, 0]} />
                                            <Bar dataKey="count" name="Weekly Orders" fill="#e57373" radius={[4, 4, 0, 0]} />
                                        </BarChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>
                        </div>
                    );
                }

                const container = document.getElementById('root');
                const root = ReactDOM.createRoot(container);
                root.render(<App />);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_vendor_dashboard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📈", fontSize = 20.sp)
                Column {
                    Text(
                        text = "Recharts Analytics Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "High-fidelity charting powered by React & Recharts engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
 
             AndroidView(
                 factory = { context ->
                     android.webkit.WebView(context).apply {
                         settings.javaScriptEnabled = true
                         webViewClient = android.webkit.WebViewClient()
                         settings.domStorageEnabled = true
                         settings.useWideViewPort = true
                         settings.loadWithOverviewMode = true
                         setBackgroundColor(android.graphics.Color.TRANSPARENT)
                     }
                 },
                 update = { webView ->
                     webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
                 },
                 modifier = Modifier
                     .fillMaxWidth()
                     .height(490.dp)
             )
         }
     }
 }
 
@Composable
fun ChartJsVendorPerformanceChart(
    performanceMetrics: List<com.example.data.LaravelVendorMetric>,
    currentUser: com.example.data.User?,
    orders: List<Order>,
    modifier: Modifier = Modifier
) {
    val metricsJson = remember(performanceMetrics, currentUser, orders) {
        val items = if (performanceMetrics.isNotEmpty()) {
            performanceMetrics
        } else {
            // Local fallback calculations for simulation robustness
            val localTotal = orders.size
            val localCompleted = orders.count { it.status == "COMPLETED" }
            val localSales = orders.filter { it.status == "COMPLETED" }.sumOf { it.totalPrice }
            val localFulfillment = if (localTotal > 0) (localCompleted.toDouble() / localTotal * 100.0) else 100.0
            
            listOf(
                com.example.data.LaravelVendorMetric(
                    vendor_id = currentUser?.id ?: 1,
                    vendor_name = currentUser?.fullName ?: "My Food Booth",
                    contact_info = "N/A",
                    operational_status = "active",
                    total_completed_orders = localCompleted,
                    total_orders = localTotal,
                    total_sales = localSales,
                    avg_completion_time_minutes = 12.5,
                    avg_completion_time_display = "12.5 mins",
                    average_delivery_time = 12.5,
                    average_delivery_time_display = "12.5 mins",
                    order_fulfillment_rate = localFulfillment
                ),
                com.example.data.LaravelVendorMetric(
                    vendor_id = 101,
                    vendor_name = "Waakye Express",
                    contact_info = "0245-WA-AKYE",
                    operational_status = "active",
                    total_completed_orders = 18,
                    total_orders = 20,
                    total_sales = 270.00,
                    avg_completion_time_minutes = 9.8,
                    avg_completion_time_display = "9.8 mins",
                    average_delivery_time = 9.8,
                    average_delivery_time_display = "9.8 mins",
                    order_fulfillment_rate = 90.0
                ),
                com.example.data.LaravelVendorMetric(
                    vendor_id = 102,
                    vendor_name = "Auntie Mary's Waakye",
                    contact_info = "0554-MARY-K",
                    operational_status = "active",
                    total_completed_orders = 24,
                    total_orders = 25,
                    total_sales = 360.00,
                    avg_completion_time_minutes = 14.1,
                    avg_completion_time_display = "14.1 mins",
                    average_delivery_time = 14.1,
                    average_delivery_time_display = "14.1 mins",
                    order_fulfillment_rate = 96.0
                )
            )
        }

        val listStr = items.map { item ->
            val isMe = item.vendor_id == currentUser?.id
            """{
                "name": "${item.vendor_name}",
                "fulfillment": ${item.order_fulfillment_rate},
                "deliveryTime": ${item.average_delivery_time},
                "isMe": $isMe
            }"""
        }
        listStr.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    val htmlContent = remember(metricsJson) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Chart.js Vendor Performance</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 8px;
                    background-color: #121212;
                    color: #e0e0e0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                .chart-card {
                    background-color: #1d1d1f;
                    border: 1px solid #333333;
                    border-radius: 12px;
                    padding: 14px;
                    margin-bottom: 16px;
                    box-shadow: 0 4px 6px rgba(0,0,0,0.3);
                }
                .chart-header {
                    margin-bottom: 12px;
                }
                .chart-title {
                    font-size: 13px;
                    font-weight: bold;
                    color: #64b5f6;
                    margin: 0;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                .chart-subtitle {
                    font-size: 10px;
                    color: #9e9e9e;
                    margin: 2px 0 0 0;
                }
                .canvas-container {
                    position: relative;
                    height: 220px;
                    width: 100%;
                }
                .tab-bar {
                    display: flex;
                    background-color: #1c1c1e;
                    border-radius: 8px;
                    padding: 2.5px;
                    gap: 4px;
                    margin-bottom: 14px;
                    border: 1px solid #2d2d2d;
                }
                .tab-btn {
                    flex: 1;
                    background: none;
                    border: none;
                    color: #9e9e9e;
                    font-size: 11px;
                    font-weight: 600;
                    padding: 8px 12px;
                    border-radius: 6px;
                    cursor: pointer;
                    transition: all 0.2s;
                }
                .tab-btn.active {
                    background-color: #2c2c2e;
                    color: #ffffff;
                    box-shadow: 0 1px 3px rgba(0,0,0,0.4);
                }
                .chart-section {
                    display: none;
                }
                .chart-section.active {
                    display: block;
                }
            </style>
        </head>
        <body>
            <div class="tab-bar">
                <button id="btn-fulfillment" class="tab-btn active" onclick="switchTab('fulfillment')">⚡ Fulfillment Rates</button>
                <button id="btn-delivery" class="tab-btn" onclick="switchTab('delivery')">⏱️ Delivery Times</button>
            </div>

            <!-- Fulfillment Section -->
            <div id="section-fulfillment" class="chart-section active">
                <div class="chart-card">
                    <div class="chart-header">
                        <p class="chart-title">🏆 Order Fulfillment Rates (%)</p>
                        <p class="chart-subtitle">Direct live tracking of completed orders vs total incoming orders</p>
                    </div>
                    <div class="canvas-container">
                        <canvas id="fulfillmentChart"></canvas>
                    </div>
                </div>
            </div>

            <!-- Delivery Time Section -->
            <div id="section-delivery" class="chart-section">
                <div class="chart-card">
                    <div class="chart-header">
                        <p class="chart-title">⏱️ Average Turnaround Times (Mins)</p>
                        <p class="chart-subtitle">Order preparation-to-delivery dispatch (minutes)</p>
                    </div>
                    <div class="canvas-container">
                        <canvas id="deliveryChart"></canvas>
                    </div>
                </div>
            </div>

            <script>
                const rawData = $metricsJson;

                function switchTab(tab) {
                    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
                    document.querySelectorAll('.chart-section').forEach(sec => sec.classList.remove('active'));
                    
                    if (tab === 'fulfillment') {
                        document.getElementById('btn-fulfillment').classList.add('active');
                        document.getElementById('section-fulfillment').classList.add('active');
                    } else {
                        document.getElementById('btn-delivery').classList.add('active');
                        document.getElementById('section-delivery').classList.add('active');
                    }
                }

                const labels = rawData.map(d => d.isMe ? "👤 " + d.name + " (Me)" : d.name);
                
                const fulfillmentBgColors = rawData.map(d => d.isMe ? 'rgba(76, 175, 80, 0.45)' : 'rgba(33, 150, 243, 0.35)');
                const fulfillmentBorderColors = rawData.map(d => d.isMe ? '#4CAF50' : '#2196F3');
                
                const deliveryBgColors = rawData.map(d => d.isMe ? 'rgba(255, 152, 0, 0.25)' : 'rgba(156, 39, 176, 0.15)');
                const deliveryBorderColors = rawData.map(d => d.isMe ? '#FF9800' : '#9C27B0');

                // Fulfillment bar chart
                const ctxFulfillment = document.getElementById('fulfillmentChart').getContext('2d');
                new Chart(ctxFulfillment, {
                    type: 'bar',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Fulfillment Rate (%)',
                            data: rawData.map(d => d.fulfillment),
                            backgroundColor: fulfillmentBgColors,
                            borderColor: fulfillmentBorderColors,
                            borderWidth: 1.5,
                            borderRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                backgroundColor: '#222222',
                                titleColor: '#64b5f6',
                                bodyColor: '#ffffff',
                                borderWidth: 1,
                                borderColor: '#444444',
                                bodyFont: {
                                    size: 11
                                }
                            }
                        },
                        scales: {
                            x: {
                                grid: {
                                    color: '#2a2a2a'
                                },
                                ticks: {
                                    color: '#9e9e9e',
                                    font: {
                                        size: 9
                                    }
                                }
                            },
                            y: {
                                min: 0,
                                max: 100,
                                grid: {
                                    color: '#2a2a2a'
                                },
                                ticks: {
                                    color: '#9e9e9e',
                                    font: {
                                        size: 9
                                    },
                                    callback: function(value) {
                                        return value + '%';
                                    }
                                }
                            }
                        }
                    }
                });

                // Delivery time line chart
                const ctxDelivery = document.getElementById('deliveryChart').getContext('2d');
                new Chart(ctxDelivery, {
                    type: 'line',
                    data: {
                        labels: labels,
                        datasets: [{
                            label: 'Average Delivery (Minutes)',
                            data: rawData.map(d => d.deliveryTime),
                            backgroundColor: deliveryBgColors,
                            borderColor: deliveryBorderColors,
                            borderWidth: 2.5,
                            pointBackgroundColor: deliveryBorderColors,
                            pointBorderColor: '#ffffff',
                            pointRadius: 4,
                            pointHoverRadius: 6,
                            tension: 0.35,
                            fill: true
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                display: false
                            },
                            tooltip: {
                                backgroundColor: '#222222',
                                titleColor: '#FF9800',
                                bodyColor: '#ffffff',
                                borderWidth: 1,
                                borderColor: '#444444',
                                bodyFont: {
                                    size: 11
                                }
                            }
                        },
                        scales: {
                            x: {
                                grid: {
                                    color: '#2a2a2a'
                                },
                                ticks: {
                                    color: '#9e9e9e',
                                    font: {
                                        size: 9
                                    }
                                }
                            },
                            y: {
                                min: 0,
                                grid: {
                                    color: '#2a2a2a'
                                },
                                ticks: {
                                    color: '#9e9e9e',
                                    font: {
                                        size: 9
                                    },
                                    callback: function(value) {
                                        return value + 'm';
                                    }
                                }
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chartjs_vendor_dashboard"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 20.sp)
                Column {
                    Text(
                        text = "Chart.js Service Performance Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dynamic line & bar charts tracking fulfillment and speed benchmarks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = android.webkit.WebViewClient()
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(355.dp)
            )
        }
    }
}

@Composable
fun ChartJsInventoryLevelChart(
    vendorFoods: List<FoodItem>,
    modifier: Modifier = Modifier
) {
    val itemsJson = remember(vendorFoods) {
        val list = vendorFoods.map { food ->
            val isLow = food.currentStock <= food.lowStockThreshold
            """{
                "name": "${food.name.replace("\"", "\\\"")}",
                "current": ${food.currentStock},
                "threshold": ${food.lowStockThreshold},
                "isLow": $isLow
            }"""
        }
        list.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    val htmlContent = remember(itemsJson) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Inventory Levels Chart</title>
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 8px;
                    background-color: #121212;
                    color: #e0e0e0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                .chart-container {
                    position: relative;
                    height: 220px;
                    width: 100%;
                }
            </style>
        </head>
        <body>
            <div class="chart-container">
                <canvas id="inventoryChart"></canvas>
            </div>
            <script>
                const dataRaw = $itemsJson;
                
                const labels = dataRaw.map(d => d.name);
                const currentData = dataRaw.map(d => d.current);
                const thresholdData = dataRaw.map(d => d.threshold);
                
                const bgColors = dataRaw.map(d => d.isLow ? 'rgba(239, 83, 80, 0.45)' : 'rgba(38, 166, 154, 0.45)');
                const borderColors = dataRaw.map(d => d.isLow ? '#ef5350' : '#26a69a');
                
                const ctx = document.getElementById('inventoryChart').getContext('2d');
                new Chart(ctx, {
                    type: 'bar',
                    data: {
                        labels: labels,
                        datasets: [
                            {
                                label: 'Remaining Serving Plates',
                                data: currentData,
                                backgroundColor: bgColors,
                                borderColor: borderColors,
                                borderWidth: 1.5,
                                borderRadius: 4,
                                barPercentage: 0.6
                            },
                            {
                                label: 'Safety Threshold Limit',
                                data: thresholdData,
                                type: 'line',
                                borderColor: 'rgba(255, 179, 0, 0.85)',
                                borderDash: [4, 4],
                                borderWidth: 2,
                                pointBackgroundColor: '#ffb300',
                                pointRadius: 4,
                                fill: false,
                                tension: 0.2
                            }
                        ]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: {
                            legend: {
                                labels: {
                                    color: '#b0bec5',
                                    font: { size: 9, weight: 'bold' }
                                }
                            },
                            tooltip: {
                                backgroundColor: '#212121',
                                titleColor: '#ffb300',
                                bodyColor: '#ffffff',
                                borderWidth: 1,
                                borderColor: '#424242'
                            }
                        },
                        scales: {
                            x: {
                                grid: { color: '#2a2a2a' },
                                ticks: { 
                                    color: '#b0bec5',
                                    font: { size: 8.5 },
                                    maxRotation: 45,
                                    minRotation: 0
                                }
                            },
                            y: {
                                grid: { color: '#2a2a2a' },
                                ticks: { 
                                    color: '#b0bec5',
                                    font: { size: 8.5 }
                                },
                                min: 0
                            }
                        }
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    AndroidView(
        factory = { context ->
            android.webkit.WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = android.webkit.WebViewClient()
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .testTag("chartjs_inventory_canvas")
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InventoryTrackingHub(
    vendorFoods: List<FoodItem>,
    onUpdateThreshold: (FoodItem, Int) -> Unit,
    onReplenishStock: (FoodItem, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // "ALL", "LOW", "NORMAL"
    
    val filteredItems = remember(vendorFoods, searchQuery, filterType) {
        vendorFoods.filter { food ->
            val nameMatch = food.name.contains(searchQuery, ignoreCase = true) ||
                    food.category.contains(searchQuery, ignoreCase = true)
            val typeMatch = when (filterType) {
                "LOW" -> food.currentStock <= food.lowStockThreshold
                "NORMAL" -> food.currentStock > food.lowStockThreshold
                else -> true
            }
            nameMatch && typeMatch
        }
    }
    
    val totalItems = vendorFoods.size
    val lowStockCount = vendorFoods.count { it.currentStock <= it.lowStockThreshold }
    val depletedCount = vendorFoods.count { it.currentStock == 0 }
    val normalCount = totalItems - lowStockCount
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inventory_tracking_hub_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📦", fontSize = 22.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Real-Time Inventory Level Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Track remaining plates, safety control thresholds, and forecasts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Stats summary grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Dishes", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("$totalItems", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                // Card 2: Low Stock Alert
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = if (lowStockCount > 0) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Low Stock", fontSize = 10.sp, color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("$lowStockCount", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                // Card 3: Depleted
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = if (depletedCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Depleted", fontSize = 10.sp, color = if (depletedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("$depletedCount", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (depletedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Chart.js levels visualization
            if (vendorFoods.isNotEmpty()) {
                ChartJsInventoryLevelChart(vendorFoods = vendorFoods, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(14.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No inventory items found. Add items to your menu first.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Controls & Filters Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search ingredients...", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
                
                // Segmented quick filters
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ALL" to "All", "LOW" to "🚨 Low").forEach { (typeKey, label) ->
                        val isSelected = filterType == typeKey
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { filterType = typeKey },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // List of items
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No items match query", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        filteredItems.take(5).forEach { food ->
                            val isBelow = food.currentStock <= food.lowStockThreshold
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(
                                        if (isBelow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = food.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isBelow) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (food.currentStock == 0) "OUT" else "LOW",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onError,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Stock: ${food.currentStock}/${food.initialStock}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isBelow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text(
                                                text = "Threshold Limit:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            // Dynamic Threshold adjusting controls
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        val prevLim = (food.lowStockThreshold - 1).coerceAtLeast(1)
                                                        onUpdateThreshold(food, prevLim)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Remove,
                                                        contentDescription = "Reduce threshold limit value",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                
                                                Text(
                                                    text = "${food.lowStockThreshold}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                
                                                IconButton(
                                                    onClick = {
                                                        val nextLim = food.lowStockThreshold + 1
                                                        onUpdateThreshold(food, nextLim)
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Increase threshold limit value",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Quick replenish +25 serves
                                    OutlinedButton(
                                        onClick = { onReplenishStock(food, 25) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("+25 Plates", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val progressVal = if (food.initialStock > 0) {
                                    (food.currentStock.toFloat() / food.initialStock.toFloat()).coerceIn(0f, 1f)
                                } else 0f
                                
                                LinearProgressIndicator(
                                    progress = { progressVal },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = if (isBelow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        
                        if (filteredItems.size > 5) {
                            Text(
                                text = "And ${filteredItems.size - 5} more dishes. View & edit completely in Menu List tab.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RechartsFeedbackDashboardChart(
    orders: List<Order>,
    feedbacks: List<com.example.data.Feedback>,
    modifier: Modifier = Modifier
) {
    val feedbackDataJson = remember(orders, feedbacks) {
        val list = mutableListOf<String>()
        val sdfLabel = java.text.SimpleDateFormat("MM-dd", java.util.Locale.US)
        for (i in 29 downTo 0) {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            
            val dayStart = cal.clone() as java.util.Calendar
            dayStart.set(java.util.Calendar.HOUR_OF_DAY, 0)
            dayStart.set(java.util.Calendar.MINUTE, 0)
            dayStart.set(java.util.Calendar.SECOND, 0)
            dayStart.set(java.util.Calendar.MILLISECOND, 0)
            val startMillis = dayStart.timeInMillis
            val endMillis = startMillis + 24 * 60 * 60 * 1000L - 1
            
            val dateLabel = sdfLabel.format(cal.time)
            
            val dayOrders = orders.filter {
                it.orderTimestamp in startMillis..endMillis && it.status == "COMPLETED"
            }
            val orderVolume = dayOrders.size
            
            val dayFeedbacks = feedbacks.filter {
                it.timestamp in startMillis..endMillis
            }
            
            val avgRating = if (dayFeedbacks.isNotEmpty()) {
                dayFeedbacks.map { f ->
                    (f.ratingFoodQuality + f.ratingCleanliness + f.ratingServiceSpeed + f.ratingPriceValue) / 4.0
                }.average()
            } else {
                4.2
            }
            
            val formattedRating = String.format(java.util.Locale.US, "%.1f", avgRating)
            list.add("""{"date": "$dateLabel", "volume": $orderVolume, "rating": $formattedRating}""")
        }
        list.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    val stats = remember(orders, feedbacks) {
        val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        val recentOrders = orders.filter { it.orderTimestamp >= thirtyDaysAgo && it.status == "COMPLETED" }
        val recentFeedbacks = feedbacks.filter { it.timestamp >= thirtyDaysAgo }
        
        val avgRating = if (recentFeedbacks.isNotEmpty()) {
            recentFeedbacks.map { f ->
                (f.ratingFoodQuality + f.ratingCleanliness + f.ratingServiceSpeed + f.ratingPriceValue) / 4.0
            }.average()
        } else {
            4.5
        }
        
        Triple(
            recentOrders.size,
            recentFeedbacks.size,
            String.format(java.util.Locale.US, "%.1f", avgRating)
        )
    }

    val totalVolume = stats.first
    val totalReviews = stats.second
    val averageRating = stats.third

    val htmlContent = remember(feedbackDataJson, totalVolume, totalReviews, averageRating) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>30-Day Performance & Feedback Tracker</title>
            <!-- Load React -->
            <script src="https://unpkg.com/react@18/umd/react.production.min.js" crossorigin></script>
            <script src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js" crossorigin></script>
            <!-- Load Prop-Types -->
            <script src="https://unpkg.com/prop-types@15.8.1/prop-types.min.js" crossorigin></script>
            <!-- Load Recharts -->
            <script src="https://unpkg.com/recharts@2.12.7/umd/Recharts.js" crossorigin></script>
            <!-- Load Babel -->
            <script src="https://unpkg.com/@babel/standalone/babel.min.js" crossorigin></script>
            <style>
                body {
                    margin: 0;
                    padding: 8px;
                    background-color: #1a1a1a;
                    color: #e0e0e0;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                }
                .card {
                    background-color: #212121;
                    border: 1px solid #333333;
                    border-radius: 8px;
                    padding: 12px;
                    margin-bottom: 12px;
                    box-shadow: 0 2px 4px rgba(0,0,0,0.15);
                }
                .header {
                    margin-bottom: 12px;
                }
                .title {
                    font-size: 11px;
                    font-weight: bold;
                    color: #ffd54f;
                    margin: 0;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                }
                .subtitle {
                    font-size: 9px;
                    color: #aaa;
                    margin: 2px 0 0 0;
                }
                .chart-container {
                    height: 240px;
                    position: relative;
                }
                .stats-row {
                    display: flex;
                    justify-content: space-between;
                    margin-bottom: 12px;
                    gap: 8px;
                }
                .stat-card {
                    background-color: #242424;
                    border: 1px solid #3a3a3a;
                    border-radius: 6px;
                    padding: 8px;
                    flex: 1;
                    text-align: center;
                }
                .stat-value {
                    font-size: 13px;
                    font-weight: bold;
                }
                .rating-color {
                    color: #ffd54f;
                }
                .volume-color {
                    color: #4fc3f7;
                }
                .review-color {
                    color: #a5d6a7;
                }
                .stat-label {
                    font-size: 8px;
                    color: #aaa;
                    margin-top: 2px;
                    text-transform: uppercase;
                }
            </style>
        </head>
        <body>
            <div id="root"></div>

            <script type="text/babel">
                const { 
                    ComposedChart, Line, Bar,
                    XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer
                } = Recharts;

                const chartData = $feedbackDataJson;
                const totalVolume = $totalVolume;
                const totalReviews = $totalReviews;
                const averageRating = "$averageRating";

                function App() {
                    return (
                        <div>
                            <div className="stats-row">
                                <div className="stat-card">
                                    <div className="stat-value rating-color">★ {averageRating} / 5.0</div>
                                    <div className="stat-label">30d Avg Rating</div>
                                </div>
                                <div className="stat-card">
                                    <div className="stat-value volume-color">{totalVolume}</div>
                                    <div className="stat-label">30d Order Volume</div>
                                </div>
                                <div className="stat-card">
                                    <div className="stat-value review-color">{totalReviews}</div>
                                    <div className="stat-label">30d Feedback Count</div>
                                </div>
                            </div>

                            <div className="card">
                                <div className="header">
                                    <p className="title">Rating Velocity & Order Density</p>
                                    <p className="subtitle">Interactive 30-day cross-timeline comparing customer sentiment scores to fulfillment volume</p>
                                </div>
                                <div className="chart-container">
                                    <ResponsiveContainer width="100%" height="100%">
                                        <ComposedChart data={chartData} margin={{ top: 5, right: -25, left: -25, bottom: 5 }}>
                                            <CartesianGrid strokeDasharray="3 3" stroke="#2d2d2d" />
                                            <XAxis dataKey="date" stroke="#888" style={{ fontSize: '8px' }} />
                                            <YAxis yAxisId="left" orientation="left" stroke="#4fc3f7" style={{ fontSize: '8px' }} label={{ value: 'Orders', angle: -90, position: 'insideLeft', style: {fontSize: '8px', fill: '#4fc3f7', textAnchor: 'middle'} }} />
                                            <YAxis yAxisId="right" orientation="right" stroke="#ffd54f" domain={[1.0, 5.0]} style={{ fontSize: '8px' }} label={{ value: 'Review Score', angle: 90, position: 'insideRight', style: {fontSize: '8px', fill: '#ffd54f', textAnchor: 'middle'} }} />
                                            <Tooltip contentStyle={{ backgroundColor: '#222', borderColor: '#444', fontSize: '9px' }} />
                                            <Legend wrapperStyle={{ fontSize: '9px', marginTop: '4px' }} />
                                            <Bar yAxisId="left" dataKey="volume" name="Fulfillment Count" fill="#4fc3f7" radius={[2, 2, 0, 0]} opacity={0.6} />
                                            <Line yAxisId="right" type="monotone" dataKey="rating" name="Customer Rating (1-5★)" stroke="#ffd54f" strokeWidth={2} activeDot={{ r: 4 }} />
                                        </ComposedChart>
                                    </ResponsiveContainer>
                                </div>
                            </div>
                        </div>
                    );
                }

                const container = document.getElementById('root');
                const root = ReactDOM.createRoot(container);
                root.render(<App />);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_feedback_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 20.sp)
                Column {
                    Text(
                        text = "30-Day Vendor Analytics & Feedback Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Real-time dual-axis rating trends & order velocity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
 
            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = android.webkit.WebViewClient()
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            )
        }
    }
}




