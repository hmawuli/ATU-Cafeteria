package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
                }
            }
        }
    }
}
