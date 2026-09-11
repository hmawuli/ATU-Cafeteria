package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.BiometricHelper
import com.example.ui.util.NotificationHelper

/**
 * Centralized Settings Screen using PreferenceScreen pattern in Compose.
 * Enables users to manage notification sounds, biometric authentication,
 * dark mode themes, privacy options, and local Room cache clearing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralizedSettingsScreen(
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    isHighContrastMode: Boolean = false,
    onToggleHighContrastMode: () -> Unit = {},
    onClearCacheClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionRepo = remember { com.example.data.repository.UserSessionRepository(context) }

    var notifSoundEnabled by remember { mutableStateOf(NotificationHelper.isSoundEnabled(context)) }
    var notifVibrationEnabled by remember { mutableStateOf(NotificationHelper.isVibrationEnabled(context)) }
    var orderStatusAlertsEnabled by remember { mutableStateOf(NotificationHelper.isOrderStatusAlertsEnabled(context)) }
    var promoAlertsEnabled by remember { mutableStateOf(NotificationHelper.isPromotionalAlertsEnabled(context)) }

    val vendorList = remember {
        listOf(
            1 to "Royal Kitchen (Main Hall)",
            2 to "Auntie Mary Local Diner",
            3 to "Express Grill & Shawarma",
            4 to "Gourmet Bites & Drinks"
        )
    }
    val vendorNotifStates = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            vendorList.forEach { (vId, _) ->
                put(vId, NotificationHelper.isVendorInventoryAlertsEnabled(context, vId))
            }
        }
    }
    var biometricsEnabled by remember { mutableStateOf(BiometricHelper.isBiometricAvailable(context)) }
    var biometricForPaymentsProfile by remember { mutableStateOf(sessionRepo.isBiometricPaymentProfileRequired()) }
    var forceOfflineMode by remember { mutableStateOf(sessionRepo.isForceOfflineMode()) }
    var currentLanguage by remember { mutableStateOf(com.example.ui.util.LocaleManager.getSavedLanguage(context)) }
    var updateStatusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    var anonymousFeedbackEnabled by remember { mutableStateOf(true) }
    var telemetryOptIn by remember { mutableStateOf(true) }

    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Settings & Preferences", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .testTag("settings_back_button")
                            .semantics { contentDescription = "Back to main screen" }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance & Theme Preference Group
            PreferenceGroupHeader(title = "🎨 Visual Theme & Accessibility")
            PreferenceCard {
                Column {
                    PreferenceSwitchTile(
                        title = "Dark Theme Mode",
                        subtitle = if (isDarkMode) "Dark palette enabled for late night study sessions" else "Light campus theme enabled",
                        icon = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDarkMode() },
                        testTag = "settings_dark_mode_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "High-Contrast Mode (TalkBack & Screen Reader)",
                        subtitle = if (isHighContrastMode) "Bold high-visibility contrast ratios & screen reader focus rings active" else "Standard Material 3 contrast palette",
                        icon = Icons.Default.Contrast,
                        checked = isHighContrastMode,
                        onCheckedChange = { onToggleHighContrastMode() },
                        testTag = "settings_high_contrast_switch"
                    )
                }
            }

            // 2. Notifications & Alert Preferences
            PreferenceGroupHeader(title = "🔔 Granular Push Notifications & Alert Preferences")
            PreferenceCard {
                Column {
                    PreferenceSwitchTile(
                        title = "Order Status Push Alerts",
                        subtitle = "Receive instant push alerts when order transitions to 'Preparing' or 'Ready for Pickup'",
                        icon = Icons.Default.NotificationsActive,
                        checked = orderStatusAlertsEnabled,
                        onCheckedChange = { enabled ->
                            orderStatusAlertsEnabled = enabled
                            NotificationHelper.setOrderStatusAlertsEnabled(context, enabled)
                        },
                        testTag = "settings_order_status_notif_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "Promotions & Daily Specials",
                        subtitle = "Get notified about meal discounts and flash campus deals",
                        icon = Icons.Default.LocalOffer,
                        checked = promoAlertsEnabled,
                        onCheckedChange = { enabled ->
                            promoAlertsEnabled = enabled
                            NotificationHelper.setPromotionalAlertsEnabled(context, enabled)
                        },
                        testTag = "settings_promo_notif_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "Notification Sound Effects",
                        subtitle = "Play notification tone when status updates occur",
                        icon = Icons.Default.VolumeUp,
                        checked = notifSoundEnabled,
                        onCheckedChange = { enabled ->
                            notifSoundEnabled = enabled
                            NotificationHelper.setSoundEnabled(context, enabled)
                        },
                        testTag = "settings_notif_sound_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "Haptic Vibration Pulse",
                        subtitle = "Vibrate device for ready order pickups and low-stock alerts",
                        icon = Icons.Default.Vibration,
                        checked = notifVibrationEnabled,
                        onCheckedChange = { enabled ->
                            notifVibrationEnabled = enabled
                            NotificationHelper.setVibrationEnabled(context, enabled)
                        },
                        testTag = "settings_notif_vibration_switch"
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // Vendor Specific Low Stock Inventory Alert Toggles
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = "Favorite Vendor Inventory Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enable or disable low-stock alerts specifically per vendor booth",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        vendorList.forEach { (vId, vName) ->
                            val isChecked = vendorNotifStates[vId] ?: true
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = vName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        vendorNotifStates[vId] = checked
                                        NotificationHelper.setVendorInventoryAlertsEnabled(context, vId, checked)
                                    },
                                    modifier = Modifier.testTag("vendor_notif_switch_$vId")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    PreferenceActionTile(
                        title = "Test Push Notification",
                        subtitle = "Send a live order status test notification to device",
                        icon = Icons.Default.Send,
                        onClick = {
                            NotificationHelper.sendOrderStatusNotification(
                                context = context,
                                orderId = (1000..9999).random(),
                                title = "🍗 ATU Cafeteria Order Update",
                                text = "Your order ticket #7821 is now READY FOR PICKUP at Royal Kitchen!"
                            )
                        },
                        testTag = "settings_test_push_tile"
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    PreferenceActionTile(
                        title = "System Notification Channels",
                        subtitle = "Open Android OS notification channel manager",
                        icon = Icons.Default.Settings,
                        onClick = { NotificationHelper.openChannelSettings(context, NotificationHelper.CHANNEL_ORDERS) },
                        testTag = "settings_open_channel_tile"
                    )
                }
            }

            // 3. Security & Biometrics Group
            PreferenceGroupHeader(title = "🔒 Security & Biometric Protection")
            PreferenceCard {
                Column {
                    PreferenceSwitchTile(
                        title = "Fingerprint / Face ID Authentication",
                        subtitle = if (biometricsEnabled) "Biometric prompt enabled for secure quick login" else "Biometrics unavailable or disabled on this hardware",
                        icon = Icons.Default.Fingerprint,
                        checked = biometricsEnabled,
                        onCheckedChange = { enabled -> biometricsEnabled = enabled },
                        testTag = "settings_biometric_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "Require Biometrics for Payments & Profile",
                        subtitle = "Trigger fingerprint / facial verification before accessing checkout or profile data",
                        icon = Icons.Default.VerifiedUser,
                        checked = biometricForPaymentsProfile,
                        onCheckedChange = { enabled ->
                            biometricForPaymentsProfile = enabled
                            sessionRepo.setBiometricPaymentProfileRequired(enabled)
                        },
                        testTag = "settings_biometric_payments_profile_switch"
                    )
                }
            }

            // 4. Data Storage & Local Cache Group
            PreferenceGroupHeader(title = "🗄️ Storage, Sync & Offline Mode")
            PreferenceCard {
                Column {
                    PreferenceSwitchTile(
                        title = "Persistent Offline Mode (Local Room DB)",
                        subtitle = if (forceOfflineMode) "Offline Mode Active: App relies exclusively on Room database" else "Live Network Mode Active: Syncing with server backend",
                        icon = Icons.Default.CloudOff,
                        checked = forceOfflineMode,
                        onCheckedChange = { enabled ->
                            forceOfflineMode = enabled
                            sessionRepo.setForceOfflineMode(enabled)
                        },
                        testTag = "settings_force_offline_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceActionTile(
                        title = "Clear Local Room Database & Image Cache",
                        subtitle = cacheClearedMessage ?: "Purge offline menu data and free up local device storage",
                        icon = Icons.Default.CleaningServices,
                        onClick = { showClearCacheConfirmDialog = true },
                        testTag = "settings_clear_cache_tile"
                    )
                }
            }

            // 5. Privacy & Data Protection Group
            PreferenceGroupHeader(title = "🛡️ Privacy & Data Protection")
            PreferenceCard {
                Column {
                    PreferenceSwitchTile(
                        title = "Submit Anonymous Vendor Reviews",
                        subtitle = "Conceal student identity when submitting ratings and comments",
                        icon = Icons.Default.Security,
                        checked = anonymousFeedbackEnabled,
                        onCheckedChange = { anonymousFeedbackEnabled = it },
                        testTag = "settings_anonymous_switch"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    PreferenceSwitchTile(
                        title = "Anonymous Usage Telemetry",
                        subtitle = "Help ATU Cafeteria team improve menu recommendation speeds",
                        icon = Icons.Default.Analytics,
                        checked = telemetryOptIn,
                        onCheckedChange = { telemetryOptIn = it },
                        testTag = "settings_telemetry_switch"
                    )
                }
            }

            // 6. Language & Localization
            PreferenceGroupHeader(title = "🌐 Language & Regional Settings")
            PreferenceCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Application Language", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = currentLanguage == com.example.ui.util.LocaleManager.LANGUAGE_ENGLISH,
                            onClick = {
                                currentLanguage = com.example.ui.util.LocaleManager.LANGUAGE_ENGLISH
                                com.example.ui.util.LocaleManager.setLanguage(context, com.example.ui.util.LocaleManager.LANGUAGE_ENGLISH)
                            },
                            label = { Text("English 🇬🇧") },
                            leadingIcon = if (currentLanguage == com.example.ui.util.LocaleManager.LANGUAGE_ENGLISH) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("lang_english_chip")
                        )
                        FilterChip(
                            selected = currentLanguage == com.example.ui.util.LocaleManager.LANGUAGE_FRENCH,
                            onClick = {
                                currentLanguage = com.example.ui.util.LocaleManager.LANGUAGE_FRENCH
                                com.example.ui.util.LocaleManager.setLanguage(context, com.example.ui.util.LocaleManager.LANGUAGE_FRENCH)
                            },
                            label = { Text("Français 🇫🇷") },
                            leadingIcon = if (currentLanguage == com.example.ui.util.LocaleManager.LANGUAGE_FRENCH) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f).testTag("lang_french_chip")
                        )
                    }
                }
            }

            // 7. Software Updates & Security Patch Compliance
            PreferenceGroupHeader(title = "🚀 In-App Updates & Version Control")
            PreferenceCard {
                Column {
                    PreferenceActionTile(
                        title = "Check for Software Updates",
                        subtitle = updateStatusMessage ?: "Verify app is running the latest security patch and features",
                        icon = Icons.Default.SystemUpdate,
                        onClick = {
                            isCheckingUpdate = true
                            com.example.ui.util.InAppUpdateHelper.checkForUpdates(
                                context = context,
                                activity = context as? android.app.Activity
                            ) { isAvailable, message ->
                                isCheckingUpdate = false
                                updateStatusMessage = message
                            }
                        },
                        testTag = "settings_check_updates_tile"
                    )
                }
            }

            // 6. Logout / Session Action Tile
            OutlinedButton(
                onClick = onLogoutClick,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out of Session", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    // Confirmation Dialog for Clearing Local Room Cache
    if (showClearCacheConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Local Cache?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("This will purge locally cached menu items and order histories. Fresh data will be automatically resynced on network connection.", fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearCacheClick()
                        cacheClearedMessage = "Local Room cache successfully purged (2.4 MB freed)."
                        showClearCacheConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_cache_button")
                ) {
                    Text("Clear Cache")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PreferenceGroupHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun PreferenceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

@Composable
private fun PreferenceSwitchTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun PreferenceActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}
