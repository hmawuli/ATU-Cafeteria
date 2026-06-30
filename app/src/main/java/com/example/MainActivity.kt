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
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.StudentDashboardScreen
import com.example.ui.screens.VendorDashboardScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CafeteriaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CafeteriaViewModel by viewModels()

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        viewModel.updateActivity()
        return super.dispatchTouchEvent(ev)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val currentUser by viewModel.currentUser.collectAsState()

                    LaunchedEffect(currentUser) {
                        if (currentUser == null) {
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            if (currentRoute != "login" && currentRoute != "register") {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }

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

enum class ScreenRotationMode(val label: String, val info: Int) {
    SENSOR("Auto-Rotate (Sensor)", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    PORTRAIT("Locked Portrait", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT),
    LANDSCAPE("Locked Landscape", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
}
