package com.xiiann.ledsync.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xiiann.ledsync.presentation.audioled.AudioLedScreen
import com.xiiann.ledsync.presentation.audioled.AudioLedViewModel
import com.xiiann.ledsync.presentation.battery.BatteryConfigScreen
import com.xiiann.ledsync.presentation.battery.BatteryConfigViewModel
import com.xiiann.ledsync.presentation.home.HomeScreen
import com.xiiann.ledsync.presentation.home.HomeViewModel
import com.xiiann.ledsync.presentation.lab.LedLabScreen
import com.xiiann.ledsync.presentation.lab.LedLabViewModel
import com.xiiann.ledsync.presentation.notif.NotifMappingScreen
import com.xiiann.ledsync.presentation.notif.NotifMappingViewModel
import com.xiiann.ledsync.presentation.performance.PerformanceScreen
import com.xiiann.ledsync.presentation.performance.PerformanceViewModel
import com.xiiann.ledsync.presentation.theme.bounceClick
import com.xiiann.ledsync.presentation.tweaks.TweaksScreen

sealed class SubDestination {
    data object None : SubDestination()
    data object AppAlerts : SubDestination()
    data object BatteryConfig : SubDestination()
    data object Performance : SubDestination()
    data object AudioLed : SubDestination()
}

data class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainShell(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val isReady by mainViewModel.isReady.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubDestination by remember { mutableStateOf<SubDestination>(SubDestination.None) }

    val tabs = remember {
        listOf(
            NavTab("Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTab("LEDs", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
            NavTab("Tweaks", Icons.Filled.Tune, Icons.Outlined.Tune)
        )
    }

    val cs = MaterialTheme.colorScheme

    // Handle System Back Gesture / Physical Back Button
    BackHandler(enabled = currentSubDestination != SubDestination.None || selectedTab != 0) {
        if (currentSubDestination != SubDestination.None) {
            currentSubDestination = SubDestination.None
        } else if (selectedTab != 0) {
            selectedTab = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isReady) Modifier.blur(16.dp) else Modifier)
        ) {
            AnimatedContent(
                targetState = currentSubDestination,
                transitionSpec = {
                    if (targetState != SubDestination.None) {
                        (slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeIn()) togetherWith (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
                    } else {
                        (slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn()) togetherWith (slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ) + fadeOut())
                    }
                },
                label = "SubDestinationTransition",
                modifier = Modifier.fillMaxSize()
            ) { destination ->
                when (destination) {
                    is SubDestination.AppAlerts -> {
                        val viewModel: NotifMappingViewModel = hiltViewModel()
                        NotifMappingScreen(
                            viewModel = viewModel,
                            onBack = { currentSubDestination = SubDestination.None }
                        )
                    }
                    is SubDestination.BatteryConfig -> {
                        val viewModel: BatteryConfigViewModel = hiltViewModel()
                        BatteryConfigScreen(
                            viewModel = viewModel,
                            onBack = { currentSubDestination = SubDestination.None }
                        )
                    }
                    is SubDestination.Performance -> {
                        val viewModel: PerformanceViewModel = hiltViewModel()
                        PerformanceScreen(
                            viewModel = viewModel,
                            onBack = { currentSubDestination = SubDestination.None }
                        )
                    }
                    is SubDestination.AudioLed -> {
                        val viewModel: AudioLedViewModel = hiltViewModel()
                        AudioLedScreen(
                            viewModel = viewModel,
                            onBack = { currentSubDestination = SubDestination.None }
                        )
                    }
                    is SubDestination.None -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(cs.surface)
                        ) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally(
                                            initialOffsetX = { it / 2 },
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn()) togetherWith (slideOutHorizontally(targetOffsetX = { -it / 2 }) + fadeOut())
                                    } else {
                                        (slideInHorizontally(
                                            initialOffsetX = { -it / 2 },
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        ) + fadeIn()) togetherWith (slideOutHorizontally(targetOffsetX = { it / 2 }) + fadeOut())
                                    }
                                },
                                label = "TabTransition",
                                modifier = Modifier.fillMaxSize()
                            ) { tab ->
                                when (tab) {
                                    0 -> {
                                        val viewModel: HomeViewModel = hiltViewModel()
                                        HomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToPerformance = {
                                                currentSubDestination = SubDestination.Performance
                                            }
                                        )
                                    }
                                    1 -> {
                                        val viewModel: LedLabViewModel = hiltViewModel()
                                        LedLabScreen(viewModel = viewModel)
                                    }
                                    2 -> {
                                        TweaksScreen(
                                            onNavigateToAppAlerts = {
                                                currentSubDestination = SubDestination.AppAlerts
                                            },
                                            onNavigateToBatteryConfig = {
                                                currentSubDestination = SubDestination.BatteryConfig
                                            },
                                            onNavigateToAudioLed = {
                                                currentSubDestination = SubDestination.AudioLed
                                            }
                                        )
                                    }
                                }
                            }

                            // Expressive M3 Sliding Indicator Floating Navigation Dock
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(bottom = 16.dp),
                                shape = CircleShape,
                                color = cs.surfaceContainerHighest,
                                tonalElevation = 8.dp,
                                shadowElevation = 12.dp
                            ) {
                                val indicatorOffset by animateDpAsState(
                                    targetValue = (92 * selectedTab).dp,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    ),
                                    label = "expressiveDockIndicatorOffset"
                                )

                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    // Sliding Pill Active Indicator (uses offset to gracefully support spring bouncy overshoot)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = indicatorOffset)
                                            .width(88.dp)
                                            .height(48.dp)
                                            .clip(CircleShape)
                                            .background(cs.primaryContainer)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tabs.forEachIndexed { index, tab ->
                                            val selected = selectedTab == index
                                            val iconTextColor by animateColorAsState(
                                                targetValue = if (selected) cs.onPrimaryContainer else cs.outline,
                                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                                label = "tabIconTextColor"
                                            )
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (selected) 1.20f else 1.0f,
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioLowBouncy
                                                ),
                                                label = "tabIconScale"
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .width(88.dp)
                                                    .height(48.dp)
                                                    .clip(CircleShape)
                                                    .bounceClick { selectedTab = index },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.title,
                                                        tint = iconTextColor,
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .graphicsLayer {
                                                                scaleX = iconScale
                                                                scaleY = iconScale
                                                            }
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = tab.title,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                        color = iconTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Root Permission Gate Dialog Overlay when Superuser access is not ready
        if (!isReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(cs.errorContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = cs.onErrorContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Superuser Access Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "LEDSync requires Root privileges to communicate directly with hardware kernel sysfs endpoints (/sys/class/leds/). Please grant superuser permission in Magisk, KernelSU, or APatch to proceed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { mainViewModel.retryRoot() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                        ) {
                            Text(
                                text = "Grant Root Permission / Retry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
