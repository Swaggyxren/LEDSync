package com.xiiann.ledsync.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xiiann.ledsync.data.repository.RootState
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

@Composable
fun MainShell(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val rootState by mainViewModel.rootState.collectAsState()
    val isNotifAccessGranted by mainViewModel.isNotifAccessGranted.collectAsState()
    val appearanceMode by mainViewModel.appearanceMode.collectAsState()
    val stockBehavior by mainViewModel.stockBehavior.collectAsState()
    var notifPopupDismissed by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }
    var currentSubDestination by remember { mutableStateOf<SubDestination>(SubDestination.None) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                mainViewModel.checkNotificationAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val openNotifSettings: () -> Unit = {
        try {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
    }

    val tabs = remember {
        listOf(
            NavTab("Home", Icons.Filled.Home, Icons.Outlined.Home),
            NavTab("LEDs", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
            NavTab("Tweaks", Icons.Filled.Tune, Icons.Outlined.Tune)
        )
    }

    val cs = MaterialTheme.colorScheme

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
                .then(if (rootState == RootState.DENIED) Modifier.blur(16.dp) else Modifier)
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
                        var dockVisible by remember { mutableStateOf(true) }
                        LaunchedEffect(selectedTab) { dockVisible = true }
                        val dockNestedScroll = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (available.y < -8f) dockVisible = false
                                    else if (available.y > 8f) dockVisible = true
                                    return Offset.Zero
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(cs.surface)
                                .nestedScroll(dockNestedScroll)
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
                                            onNavigateToPerformance = { currentSubDestination = SubDestination.Performance },
                                            onNavigateToAudioLed = { currentSubDestination = SubDestination.AudioLed },
                                            onNavigateToAppAlerts = { currentSubDestination = SubDestination.AppAlerts },
                                            onNavigateToBatteryConfig = { currentSubDestination = SubDestination.BatteryConfig }
                                        )
                                    }
                                    1 -> {
                                        val viewModel: LedLabViewModel = hiltViewModel()
                                        LedLabScreen(viewModel = viewModel)
                                    }
                                    2 -> {
                                        TweaksScreen(
                                            appearanceMode = appearanceMode,
                                            stockBehavior = stockBehavior,
                                            isNotifAccessGranted = isNotifAccessGranted,
                                            isRootGranted = rootState == RootState.GRANTED,
                                            onAppearanceChange = { mainViewModel.setAppearanceMode(it) },
                                            onUpdateStockBehavior = { mainViewModel.updateStockBehavior(it) },
                                            onOpenNotificationAccess = openNotifSettings,
                                            onNavigateToAppAlerts = { currentSubDestination = SubDestination.AppAlerts },
                                            onNavigateToBatteryConfig = { currentSubDestination = SubDestination.BatteryConfig },
                                            onNavigateToAudioLed = { currentSubDestination = SubDestination.AudioLed }
                                        )
                                    }
                                }
                            }

                            // Slim Floating Pill Dock with bottom gradient scrim + scroll auto-hide
                            AnimatedVisibility(
                                visible = dockVisible,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                enter = fadeIn() + slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ),
                                exit = fadeOut() + slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        cs.surface.copy(alpha = 0.85f),
                                                        cs.surface
                                                    )
                                                )
                                            )
                                    )
                                    Surface(
                                        modifier = Modifier
                                            .navigationBarsPadding()
                                            .padding(horizontal = 48.dp, vertical = 10.dp)
                                            .fillMaxWidth()
                                            .height(60.dp),
                                        shape = RoundedCornerShape(30.dp),
                                        color = cs.surfaceContainerHighest,
                                        shadowElevation = 8.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(5.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            tabs.forEachIndexed { index, tab ->
                                                val selected = selectedTab == index
                                                val itemWeight by animateFloatAsState(
                                                    targetValue = if (selected) 1.45f else 1f,
                                                    animationSpec = spring(
                                                        stiffness = Spring.StiffnessMediumLow,
                                                        dampingRatio = Spring.DampingRatioLowBouncy
                                                    ),
                                                    label = "dockWeight"
                                                )
                                                val bgColor by animateColorAsState(
                                                    targetValue = if (selected) cs.primary else Color.Transparent,
                                                    label = "dockBg"
                                                )
                                                val fgColor by animateColorAsState(
                                                    targetValue = if (selected) cs.onPrimary else cs.onSurfaceVariant,
                                                    label = "dockFg"
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .weight(itemWeight)
                                                        .fillMaxHeight()
                                                        .clip(RoundedCornerShape(25.dp))
                                                        .background(bgColor)
                                                        .bounceClick { selectedTab = index },
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                        contentDescription = tab.title,
                                                        tint = fgColor,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = tab.title,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = fgColor
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

        // Notification Access Required Modal Popup Overlay
        if (rootState == RootState.GRANTED && !isNotifAccessGranted && !notifPopupDismissed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
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
                                .background(cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = cs.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Notification Access Required",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "LEDSync needs Notification Listener permission to trigger LED patterns when app notifications arrive in the background.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = openNotifSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                        ) {
                            Text(
                                text = "Grant Notification Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { notifPopupDismissed = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Later",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = cs.outline
                            )
                        }
                    }
                }
            }
        }

        // Root Permission Gate Dialog Overlay
        if (rootState == RootState.DENIED) {
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
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainer),
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
