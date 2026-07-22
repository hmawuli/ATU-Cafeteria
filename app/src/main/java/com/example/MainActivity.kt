package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.VendorDashboardScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CafeteriaViewModel
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val viewModel: CafeteriaViewModel by viewModels()

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        viewModel.updateActivity()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.ui.util.NotificationHelper.createNotificationChannels(this)
        enableEdgeToEdge()
        setContent {
            val highContrast by viewModel.isHighContrastMode.collectAsState()
            val darkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = darkMode, highContrast = highContrast) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser by viewModel.currentUser.collectAsState()
                    val isOnline by viewModel.isOnline.collectAsState()
                    val currentBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = currentBackStackEntry?.destination?.route

                    LaunchedEffect(currentUser, currentRoute) {
                        val user = currentUser
                        if (user == null) {
                            if (currentRoute != "login" && currentRoute != "register" && currentRoute != null) {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        } else {
                            val role = user.role.uppercase()
                            if (role == "STUDENT" && (currentRoute == "vendor_home" || currentRoute == "admin_home")) {
                                navController.navigate("student_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else if (role == "VENDOR" && (currentRoute == "student_home" || currentRoute == "admin_home")) {
                                navController.navigate("vendor_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else if (role == "ADMIN" && (currentRoute == "student_home" || currentRoute == "vendor_home")) {
                                navController.navigate("admin_home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

                    com.example.ui.components.AppErrorBoundary(viewModel = viewModel) {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            NavHost(
                                navController = navController,
                                startDestination = "login"
                            ) {
                                composable("login") {
                                    LoginScreen(
                                        viewModel = viewModel,
                                        navController = navController
                                    )
                                }
                                composable("register") {
                                    RegisterScreen(
                                        viewModel = viewModel,
                                        navController = navController
                                    )
                                }
                                composable("student_home") {
                                    StudentDashboardScreen(
                                        viewModel = viewModel,
                                        navController = navController
                                    )
                                }
                                composable("vendor_home") {
                                    VendorDashboardScreen(
                                        viewModel = viewModel,
                                        navController = navController
                                    )
                                }
                                composable("admin_home") {
                                    AdminDashboardScreen(
                                        viewModel = viewModel,
                                        navController = navController
                                    )
                                }
                            }

                            // Friendly "Offline" Banner Overlay
                            AnimatedVisibility(
                                visible = !isOnline,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                                modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                            ) {
                                androidx.compose.material3.Card(
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("offline_banner")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                                    ) {
                                        androidx.compose.material3.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.CloudOff,
                                            contentDescription = "Offline Mode Active",
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        androidx.compose.material3.Text(
                                            text = "Offline Mode • Running on local database cache",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Floating Orientation Toggler overlay
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val activity = context as? android.app.Activity
                            
                            val sharedPrefs = androidx.compose.runtime.remember {
                                context.getSharedPreferences("screen_settings", android.content.Context.MODE_PRIVATE)
                            }
                            var rotationMode by androidx.compose.runtime.remember {
                                val saved = sharedPrefs.getString("rotation_mode", "SENSOR") ?: "SENSOR"
                                androidx.compose.runtime.mutableStateOf(
                                    try { ScreenRotationMode.valueOf(saved) } catch (e: Exception) { ScreenRotationMode.SENSOR }
                                )
                            }

                            androidx.compose.runtime.LaunchedEffect(rotationMode) {
                                activity?.requestedOrientation = rotationMode.info
                            }

                            androidx.compose.material3.FloatingActionButton(
                                onClick = {
                                    val nextMode = when (rotationMode) {
                                        ScreenRotationMode.SENSOR -> ScreenRotationMode.PORTRAIT
                                        ScreenRotationMode.PORTRAIT -> ScreenRotationMode.LANDSCAPE
                                        ScreenRotationMode.LANDSCAPE -> ScreenRotationMode.SENSOR
                                    }
                                    rotationMode = nextMode
                                    sharedPrefs.edit().putString("rotation_mode", nextMode.name).apply()
                                    android.widget.Toast.makeText(context, "Orientation: ${nextMode.label}", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier
                                    .align(androidx.compose.ui.Alignment.BottomStart)
                                    .padding(start = 20.dp, bottom = 90.dp)
                                    .size(54.dp)
                                    .testTag("screen_rotation_fab")
                            ) {
                                val icon = when (rotationMode) {
                                    ScreenRotationMode.SENSOR -> androidx.compose.material.icons.Icons.Default.ScreenRotation
                                    ScreenRotationMode.PORTRAIT -> androidx.compose.material.icons.Icons.Default.StayCurrentPortrait
                                    ScreenRotationMode.LANDSCAPE -> androidx.compose.material.icons.Icons.Default.StayCurrentLandscape
                                }
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = "Toggle Screen Orientation: ${rotationMode.label}",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class ScreenRotationMode(val label: String, val info: Int) {
    SENSOR("Auto-Rotate (Sensor)", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    PORTRAIT("Locked Portrait", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    LANDSCAPE("Locked Landscape", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
}
