package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.User
import kotlin.math.sqrt
import kotlin.math.roundToInt

data class CafeteriaBuilding(
    val name: String,
    val description: String,
    val x: Float,
    val y: Float,
    val icon: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen(
    currentUser: User?,
    modifier: Modifier = Modifier
) {
    val buildings = remember {
        listOf(
            CafeteriaBuilding(
                name = "ATU Central Cafeteria (Main Hall)",
                description = "The largest central dining facility on Accra Technical University campus.",
                x = 150f,
                y = 120f,
                icon = "🍽️",
                color = Color(0xFF1976D2)
            ),
            CafeteriaBuilding(
                name = "South Gate Food Arena",
                description = "Lively local dish hotspot next to Accra Technical University library.",
                x = 60f,
                y = 280f,
                icon = "🍲",
                color = Color(0xFFE64A19)
            ),
            CafeteriaBuilding(
                name = "Faculty of Engineering Eatery",
                description = "Modern student hub serving fast food and fresh juices.",
                x = 240f,
                y = 70f,
                icon = "🍟",
                color = Color(0xFF388E3C)
            ),
            CafeteriaBuilding(
                name = "Executive Lounge & Juice Arena",
                description = "Fresh smoothies, Sobolo, pastries, and executive staff buffet.",
                x = 280f,
                y = 220f,
                icon = "🥤",
                color = Color(0xFF8E24AA)
            )
        )
    }

    var selectedBuilding by remember { mutableStateOf(buildings[0]) }
    
    // Student's simulated coordinates on a 0-350 canvas grid
    var studentX by remember { mutableFloatStateOf(120f) }
    var studentY by remember { mutableFloatStateOf(180f) }

    // Map scaling: 1 unit on grid = 1.5 meters
    val scaleFactor = 1.5f
    val distance = remember(studentX, studentY, selectedBuilding) {
        val dx = studentX - selectedBuilding.x
        val dy = studentY - selectedBuilding.y
        sqrt(dx * dx + dy * dy) * scaleFactor
    }
    
    // Standard walking speed: ~1.3 m/s
    val walkMinutes = remember(distance) {
        (distance / (1.3f * 60f)).roundToInt().coerceAtLeast(1)
    }

    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("campus_map_screen_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column {
                Text(
                    text = "ATU Campus Cafeteria Locator",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap on the map grid or use the sliders to simulate your GPS location and calculate precise walking routes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Interactive Map Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                // We will draw map details on a Canvas
                val primaryColor = MaterialTheme.colorScheme.primary
                val outlineColor = MaterialTheme.colorScheme.outlineVariant
                val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val canvasWidth = size.width
                                val canvasHeight = size.height
                                if (canvasWidth > 0 && canvasHeight > 0) {
                                    studentX = (offset.x / canvasWidth) * 320f
                                    studentY = (offset.y / canvasHeight) * 300f
                                }
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Draw grid lines
                    val cols = 8
                    val rows = 8
                    for (i in 1..cols) {
                        val x = (canvasWidth / cols) * i
                        drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), strokeWidth = 1f)
                    }
                    for (i in 1..rows) {
                        val y = (canvasHeight / rows) * i
                        drawLine(gridColor, Offset(0f, y), Offset(canvasWidth, y), strokeWidth = 1f)
                    }

                    // Translate virtual coordinates (320x300 grid) to local canvas space
                    fun toCanvasOffset(gridX: Float, gridY: Float): Offset {
                        return Offset(
                            x = (gridX / 320f) * canvasWidth,
                            y = (gridY / 300f) * canvasHeight
                        )
                    }

                    // Draw paths/trails between landmarks for visual interest
                    buildings.forEachIndexed { index, b ->
                        if (index < buildings.size - 1) {
                            val start = toCanvasOffset(b.x, b.y)
                            val end = toCanvasOffset(buildings[index + 1].x, buildings[index + 1].y)
                            drawLine(
                                color = outlineColor.copy(alpha = 0.5f),
                                start = start,
                                end = end,
                                strokeWidth = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }
                    }

                    // Draw route line to selected building
                    val studentOffset = toCanvasOffset(studentX, studentY)
                    val targetOffset = toCanvasOffset(selectedBuilding.x, selectedBuilding.y)
                    drawLine(
                        color = primaryColor,
                        start = studentOffset,
                        end = targetOffset,
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )

                    // Draw Cafeteria buildings on Canvas
                    buildings.forEach { b ->
                        val bOffset = toCanvasOffset(b.x, b.y)
                        val isSelected = b.name == selectedBuilding.name
                        
                        // Outer glow for selected
                        if (isSelected) {
                            drawCircle(
                                color = b.color.copy(alpha = 0.25f),
                                radius = 24.dp.toPx(),
                                center = bOffset
                            )
                        }

                        // Main landmark circle
                        drawCircle(
                            color = b.color,
                            radius = 12.dp.toPx(),
                            center = bOffset
                        )

                        drawCircle(
                            color = Color.White,
                            radius = 10.dp.toPx(),
                            center = bOffset
                        )
                    }

                    // Draw student position
                    drawCircle(
                        color = Color(0xFF1E88E5).copy(alpha = 0.25f),
                        radius = 20.dp.toPx(),
                        center = studentOffset
                    )
                    drawCircle(
                        color = Color(0xFF1E88E5),
                        radius = 8.dp.toPx(),
                        center = studentOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = studentOffset
                    )
                }

                // Overlay HTML/Unicode Icons for landmarks
                buildings.forEach { b ->
                    val isSelected = b.name == selectedBuilding.name
                    val posX = (b.x / 320f)
                    val posY = (b.y / 300f)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = (posX * 260).dp,
                                top = (posY * 220).dp
                            )
                    ) {
                        Card(
                            onClick = { selectedBuilding = b },
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) b.color else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) Color.White else b.color
                            ),
                            border = BorderStroke(1.dp, b.color),
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("map_building_pin_${b.name.replace(" ", "_").lowercase()}"),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(b.icon, fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                // Overlay Student Marker Icon!
                val studPosX = (studentX / 320f)
                val studPosY = (studentY / 300f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = (studPosX * 260).dp,
                            top = (studPosY * 220).dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color(0xFF1E88E5), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Me", tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Controls & Presets Panel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📍 Drag Sliders to Move Yourself (My GPS):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("X Coordinate:", fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = studentX,
                        onValueChange = { studentX = it },
                        valueRange = 0f..320f,
                        modifier = Modifier.weight(1f).testTag("slider_student_x")
                    )
                    Text("${studentX.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Y Coordinate:", fontSize = 11.sp, modifier = Modifier.width(90.dp))
                    Slider(
                        value = studentY,
                        onValueChange = { studentY = it },
                        valueRange = 0f..300f,
                        modifier = Modifier.weight(1f).testTag("slider_student_y")
                    )
                    Text("${studentY.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                }
                
                // Preset Campus Locations
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("GPS Presets:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    listOf(
                        "Library Block" to Pair(70f, 260f),
                        "Engineering Gates" to Pair(230f, 90f),
                        "Main Admin" to Pair(130f, 160f),
                        "ATU Hostel" to Pair(40f, 50f)
                    ).forEach { (label, coords) ->
                        SuggestionChip(
                            onClick = {
                                studentX = coords.first
                                studentY = coords.second
                            },
                            label = { Text(label, fontSize = 9.sp) }
                        )
                    }
                }
            }

            HorizontalDivider()

            // Selected Landmark Details & Real-Time Navigation
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🎯 Navigation Guidance to Target:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                // Dropdown to change selected target
                var showBuildingsMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showBuildingsMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(selectedBuilding.icon, fontSize = 16.sp)
                                Text(selectedBuilding.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = showBuildingsMenu,
                        onDismissRequest = { showBuildingsMenu = false }
                    ) {
                        buildings.forEach { b ->
                            DropdownMenuItem(
                                leadingIcon = { Text(b.icon) },
                                text = { Text(b.name, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedBuilding = b
                                    showBuildingsMenu = false
                                }
                            )
                        }
                    }
                }

                // Landmark Info Card with calculations
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = selectedBuilding.color.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, selectedBuilding.color.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = selectedBuilding.name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = selectedBuilding.color
                        )
                        Text(
                            text = selectedBuilding.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = selectedBuilding.color.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Navigation, contentDescription = null, tint = selectedBuilding.color, modifier = Modifier.size(14.dp))
                                    Text("Simulated Distance:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${distance.toInt()} meters", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = selectedBuilding.color, modifier = Modifier.size(14.dp))
                                    Text("Walking Time:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("~$walkMinutes min walk", fontSize = 16.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}
