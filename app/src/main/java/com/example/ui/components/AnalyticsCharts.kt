package com.example.ui.components

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

