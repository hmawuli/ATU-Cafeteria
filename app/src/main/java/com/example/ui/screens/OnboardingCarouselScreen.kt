package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserSessionRepository
import com.example.ui.util.HapticHelper
import kotlinx.coroutines.launch

data class OnboardingStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

/**
 * Multi-step Onboarding Carousel using Material 3 HorizontalPager.
 * Introduces students to cafeteria features, AI recommendations, and vendor QR verification.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingCarouselScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionRepo = remember { UserSessionRepository(context) }

    val steps = listOf(
        OnboardingStep(
            title = "Welcome to ATU Cafeteria Hub",
            subtitle = "Accra Technical University Campus Dining",
            description = "Explore real-time daily menus from top campus vendors. Order ahead, customize meal options, and bypass cafeteria checkout lines.",
            icon = Icons.Default.Restaurant,
            accentColor = Color(0xFF1B5E20)
        ),
        OnboardingStep(
            title = "AI Nutrition & Budget Coaching",
            subtitle = "Gemini AI Smart Meal Recommendations",
            description = "Receive personalized dietary advice aligned with your daily student budget. Get smart recommendations based on calories, preferences, and allergens.",
            icon = Icons.Default.AutoAwesome,
            accentColor = Color(0xFF0D47A1)
        ),
        OnboardingStep(
            title = "Instant Vendor QR Verification",
            subtitle = "Seamless & Contactless Pickups",
            description = "Every order generates a unique scannable QR code. Show your code to vendors at pickup booths for immediate verification and zero-touch validation.",
            icon = Icons.Default.QrCodeScanner,
            accentColor = Color(0xFFE65100)
        )
    )

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { steps.size })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button or indicator
                    if (pagerState.currentPage < steps.size - 1) {
                        TextButton(
                            onClick = {
                                HapticHelper.impact(context, "LIGHT")
                                sessionRepo.setOnboardingCompleted(true)
                                onFinishOnboarding()
                            },
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    // Pager Indicators (Dots)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(steps.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) steps[pagerState.currentPage].accentColor
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    // Next or Get Started button
                    if (pagerState.currentPage < steps.size - 1) {
                        Button(
                            onClick = {
                                HapticHelper.impact(context, "LIGHT")
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = steps[pagerState.currentPage].accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_next_button")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                HapticHelper.orderSuccess(context)
                                sessionRepo.setOnboardingCompleted(true)
                                onFinishOnboarding()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = steps[pagerState.currentPage].accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("onboarding_finish_button")
                        ) {
                            Text("Get Started")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = step.accentColor.copy(alpha = 0.12f)),
                        shape = CircleShape,
                        modifier = Modifier.size(140.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = step.accentColor,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Text(
                        text = step.subtitle.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = step.accentColor,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = step.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = step.description,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
