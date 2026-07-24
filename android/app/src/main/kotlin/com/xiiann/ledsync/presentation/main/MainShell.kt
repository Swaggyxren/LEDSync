package com.xiiann.ledsync.presentation.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
}

data class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainShell() {
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

    AnimatedContent(
        targetState = currentSubDestination,
        transitionSpec = {
            if (targetState != SubDestination.None) {
                // Navigating into sub-screen: fluid slide in from right
                (slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn()) togetherWith (slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut())
            } else {
                // Popping back to MainShell: fluid slide out to right
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
            is SubDestination.None -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cs.surface)
                ) {
                    // Main Content Tab View with Directional Slide Transitions
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
                                    }
                                )
                            }
                        }
                    }

                    // Stable Fixed-Size Expressive Floating Navigation Dock with Fluid Spring Animations
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
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            tabs.forEachIndexed { index, tab ->
                                val selected = selectedTab == index
                                val containerColor by animateColorAsState(
                                    targetValue = if (selected) cs.primaryContainer else cs.surfaceContainerHighest,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "tabContainerColor"
                                )
                                val iconTextColor by animateColorAsState(
                                    targetValue = if (selected) cs.onPrimaryContainer else cs.outline,
                                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                                    label = "tabIconTextColor"
                                )
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.15f else 1.0f,
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    ),
                                    label = "tabIconScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(88.dp)
                                        .clip(CircleShape)
                                        .background(containerColor)
                                        .bounceClick { selectedTab = index }
                                        .padding(vertical = 8.dp),
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
