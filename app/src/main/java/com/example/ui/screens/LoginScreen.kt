package com.example.ui.screens
import com.example.ui.util.generatePdfReceipt

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.navigation.NavController
import com.example.data.*
import com.example.ui.components.D3DashboardChart
import com.example.ui.components.RechartsDashboardChart
import com.example.ui.components.RechartsFeedbackDashboardChart
import com.example.ui.components.ChartJsVendorPerformanceChart
import com.example.ui.components.InventoryTrackingHub
import com.example.ui.components.DailyRevenueBarChart
import com.example.ui.components.RadarFeedbackChart
import com.example.ui.components.StudentTrendsLineChart
import com.example.ui.components.VendorPerformanceTrendChart
import com.example.ui.components.WeeklyRevenueTrendLineChart
import com.example.ui.components.LaravelDailyRevenueTrendChart
import com.example.ui.viewmodel.CafeteriaViewModel

// ==========================================
// 1. APP AUTHENTICATION SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)


@Composable
fun LoginScreen(
    viewModel: CafeteriaViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var pinCode by remember { mutableStateOf("") }
    var activeSsoProvider by remember { mutableStateOf<String?>(null) }
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cafeteria_cache", android.content.Context.MODE_PRIVATE) }
    var biometricEnabledSetup by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_enabled", false)) }
    val savedBiometricUser = remember { sharedPrefs.getString("biometric_username", "") ?: "" }
    val savedBiometricPin = remember { sharedPrefs.getString("biometric_pin", "") ?: "" }
    val hasStoredBiometrics = remember(savedBiometricUser, savedBiometricPin) {
        savedBiometricUser.isNotBlank() && savedBiometricPin.isNotBlank()
    }
    var biometricErrorText by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ATU CAFETERIA HUB",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Welcome Header
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = com.example.R.drawable.img_app_logo,
                    contentDescription = "ATU Cafeteria Hub Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Accra Technical University",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Secure Mobile Food Portal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Access Lock-In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val isSessionTimedOut by viewModel.isSessionTimedOut.collectAsStateWithLifecycle()

                    OutlinedTextField(
                        value = username,
                        onValueChange = { 
                            username = it 
                            viewModel.resetSessionTimeoutFlag()
                        },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinCode,
                        onValueChange = { 
                            pinCode = it 
                            viewModel.resetSessionTimeoutFlag()
                        },
                        label = { Text("Access PIN (Numeric)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Biometric Fast-Login",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = biometricEnabledSetup,
                            onCheckedChange = { checked ->
                                biometricEnabledSetup = checked
                                if (!checked) {
                                    sharedPrefs.edit()
                                        .remove("biometric_username")
                                        .remove("biometric_pin")
                                        .putBoolean("biometric_enabled", false)
                                        .apply()
                                }
                            },
                            modifier = Modifier.testTag("biometric_login_toggle")
                        )
                    }

                    if (isSessionTimedOut) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Session Expired",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Session Expired",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "For security, your session was automatically cleared after 30 minutes of inactivity.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    loginError?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    biometricErrorText?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (username.isNotBlank() && pinCode.isNotBlank()) {
                                        viewModel.loginUser(username.trim().lowercase(), pinCode) { success ->
                                            if (success) {
                                                if (biometricEnabledSetup) {
                                                    sharedPrefs.edit()
                                                        .putString("biometric_username", username.trim().lowercase())
                                                        .putString("biometric_pin", pinCode)
                                                        .putBoolean("biometric_enabled", true)
                                                        .apply()
                                                }
                                                val u = viewModel.currentUser.value
                                                if (u != null) {
                                                    when (u.role) {
                                                        "STUDENT" -> navController.navigate("student_home") { popUpTo(0) }
                                                        "VENDOR" -> navController.navigate("vendor_home") { popUpTo(0) }
                                                        "ADMIN" -> navController.navigate("admin_home") { popUpTo(0) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Secure Login")
                            }

                            if (hasStoredBiometrics && biometricEnabledSetup) {
                                FilledIconButton(
                                    onClick = {
                                        val activity = com.example.ui.util.BiometricHelper.findActivity(context)
                                        if (activity != null) {
                                            com.example.ui.util.BiometricHelper.showBiometricPrompt(
                                                activity = activity,
                                                title = "ATU Cafeteria Hub Login",
                                                subtitle = "Scan fingerprint/face to access account",
                                                onSuccess = {
                                                    viewModel.loginUser(savedBiometricUser, savedBiometricPin) { success ->
                                                        if (success) {
                                                            val u = viewModel.currentUser.value
                                                            if (u != null) {
                                                                when (u.role) {
                                                                    "STUDENT" -> navController.navigate("student_home") { popUpTo(0) }
                                                                    "VENDOR" -> navController.navigate("vendor_home") { popUpTo(0) }
                                                                    "ADMIN" -> navController.navigate("admin_home") { popUpTo(0) }
                                                                }
                                                            }
                                                        } else {
                                                            biometricErrorText = "Auto-biometric login failed."
                                                        }
                                                    }
                                                },
                                                onError = { err ->
                                                    biometricErrorText = err
                                                }
                                            )
                                        } else {
                                            biometricErrorText = "Device biometric capability not found."
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).testTag("biometric_login_btn"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary
                                    )
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = "Biometric Login", modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                Text(
                    text = "INSTANT CAMPUS SIGN-ON",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            }

            // High Fidelity Social SSO buttons column
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Continue with Google
                OutlinedButton(
                    onClick = { activeSsoProvider = "Google" },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(
                        text = "G",
                        color = Color(0xFFEA4335),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google (Gmail)", fontWeight = FontWeight.SemiBold)
                }

                // Continue with Facebook
                Button(
                    onClick = { activeSsoProvider = "Facebook" },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1877F2),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "f",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Facebook", fontWeight = FontWeight.SemiBold)
                }

                // Continue with Campus Microsoft or Others
                FilledTonalButton(
                    onClick = { activeSsoProvider = "Campus Microsoft" },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Other Institutional SSO Options", fontWeight = FontWeight.SemiBold)
                }
            }

            activeSsoProvider?.let { provider ->
                SocialSsoDialog(
                    provider = provider,
                    onDismiss = { activeSsoProvider = null },
                    onAuthSuccess = { usernameToUse, nameToUse, logoUrlToUse ->
                        activeSsoProvider = null
                        viewModel.loginWithSocial(usernameToUse, nameToUse, provider, logoUrlToUse) { success ->
                            if (success) {
                                navController.navigate("student_home") { popUpTo(0) }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation to Registration page
            TextButton(
                onClick = { navController.navigate("register") }
            ) {
                Text("New Student / Vendor? Enroll Account here", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Demo Credentials helper
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("💡 Demo Portals (Pin is required):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Student Portal: user 'student' / PIN '1234'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Vendor (Mary Joint): user 'maryjoint' / PIN '1111'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("• Admin Compliance Panel: user 'admin' / PIN 'admin123'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Laravel Backend Configuration Tool (Expandable Card)
            var showLaravelConfig by remember { mutableStateOf(false) }
            val context = androidx.compose.ui.platform.LocalContext.current

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (LaravelClientManager.isLaravelEnabled) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLaravelConfig = !showLaravelConfig },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (LaravelClientManager.isLaravelEnabled) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (LaravelClientManager.isLaravelEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (LaravelClientManager.isLaravelEnabled) "Laravel Live Sync: ON" else "Laravel Offline Mode: ON",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = if (showLaravelConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showLaravelConfig) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        var isEnabledState by remember { mutableStateOf(LaravelClientManager.isLaravelEnabled) }
                        var urlInput by remember { mutableStateOf(LaravelClientManager.baseUrl) }
                        var syncStatus by remember { mutableStateOf<String?>(null) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Connect to Laravel REST API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Routes read/saves to remote database", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isEnabledState,
                                onCheckedChange = { isEnabledState = it }
                            )
                        }

                        if (isEnabledState) {
                             Spacer(modifier = Modifier.height(12.dp))
                             OutlinedTextField(
                                 value = urlInput,
                                 onValueChange = { urlInput = it },
                                 label = { Text("Laravel Base API URL") },
                                 leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                 placeholder = { Text("e.g. http://10.0.2.2:8000") },
                                 modifier = Modifier.fillMaxWidth(),
                                 singleLine = true
                             )
                        }

                        syncStatus?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                color = if (it.contains("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                LaravelClientManager.isLaravelEnabled = isEnabledState
                                if (isEnabledState) {
                                    val formattedUrl = if (urlInput.trim().endsWith("/")) urlInput.trim() else "${urlInput.trim()}/"
                                    LaravelClientManager.baseUrl = formattedUrl
                                    syncStatus = "Attempting to handshake & sync database..."
                                    viewModel.syncAllFromLaravel { success ->
                                        if (success) {
                                             syncStatus = "🚀 Handshake Success! All ATU tables fetched."
                                             android.widget.Toast.makeText(context, "Laravel Connection Applied!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                             syncStatus = "❌ Sync Failed! Please verify server is hosted on '$formattedUrl' and connection is reachable."
                                        }
                                    }
                                } else {
                                    syncStatus = "Toggled Offline Local Room mode."
                                    android.widget.Toast.makeText(context, "Switched to standard Offline sandbox database.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnabledState) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Settings")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialSsoDialog(
    provider: String,
    onDismiss: () -> Unit,
    onAuthSuccess: (username: String, fullName: String, logoUrl: String?) -> Unit
) {
    var useCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isVerifyingSso by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isVerifyingSso) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo and Styling
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            when (provider) {
                                "Google" -> Color(0xFFF1F5F9)
                                "Facebook" -> Color(0xFF1877F2)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (provider) {
                        "Google" -> {
                            Text(
                                "G",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA4335)
                            )
                        }
                        "Facebook" -> {
                            Text(
                                "f",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sign in via $provider",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Secure Accra Technical University SSO Connection",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                Spacer(modifier = Modifier.height(16.dp))

                if (isVerifyingSso) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Establishing secure $provider token exchange...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    if (!useCustom) {
                        // Preloaded Accounts List
                        Text(
                            text = "Select an active campus account:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )

                        val accounts = when (provider) {
                            "Google" -> listOf(
                                Triple("Adwoa Boateng", "adwoa.boateng@atu.edu.gh", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=60"),
                                Triple("Kwame Mensah", "kwame.mensah@atu.edu.gh", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=120&auto=format&fit=crop&q=60")
                            )
                            "Facebook" -> listOf(
                                Triple("Abena Poku", "abena.poku@facebook.com", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=120&auto=format&fit=crop&q=60"),
                                Triple("Kofi Taylor", "kofi.taylor@facebook.com", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=120&auto=format&fit=crop&q=60")
                            )
                            else -> listOf(
                                Triple("Efua Osei", "efua.osei@microsoft.atu.edu.gh", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=120&auto=format&fit=crop&q=60"),
                                Triple("Yaw Owusu", "yaw.owusu@microsoft.atu.edu.gh", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=120&auto=format&fit=crop&q=60")
                            )
                        }

                        accounts.forEach { (name, email, imgUrl) ->
                            Card(
                                onClick = {
                                    isVerifyingSso = true
                                    coroutineScope.launch {
                                        delay(1500)
                                        onAuthSuccess(email, name, imgUrl)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    coil.compose.AsyncImage(
                                        model = imgUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { useCustom = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Another $provider Profile")
                        }
                    } else {
                        // Custom Input Flow
                        Text(
                            text = "Link your social credentials:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it },
                            label = { Text("Email or Account Identifier") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        validationError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { 
                                useCustom = false 
                                validationError = null
                            }) {
                                Text("Back")
                            }

                            Button(
                                onClick = {
                                    if (customName.isBlank() || customEmail.isBlank()) {
                                        validationError = "Please fill in all details."
                                        return@Button
                                    }
                                    if (provider == "Google" && !customEmail.contains("@")) {
                                        validationError = "Please enter a valid Google email address."
                                        return@Button
                                    }
                                    validationError = null
                                    isVerifyingSso = true
                                    coroutineScope.launch {
                                        delay(1500)
                                        val generatedKey = customEmail.trim().lowercase()
                                        onAuthSuccess(generatedKey, customName.trim(), null)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Proceed Connect")
                            }
                        }
                    }
                }

                if (!isVerifyingSso) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel Connection", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ==========================================
