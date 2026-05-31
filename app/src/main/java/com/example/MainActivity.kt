package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
