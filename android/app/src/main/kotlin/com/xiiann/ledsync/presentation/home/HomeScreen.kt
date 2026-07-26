package com.xiiann.ledsync.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.BuildConfig
import com.xiiann.ledsync.R
import com.xiiann.ledsync.presentation.theme.bounceClick

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPerformance: () -> Unit
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()

    val cs = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "LED Sync",
                    style = MaterialTheme.typography.headlineLarge,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Compact Hardware Status Card with Artwork Banner
            M3DeviceCard(deviceInfo = deviceInfo)

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                M3StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Battery",
                    value = if (liveStats.batteryLevel > 0) "${liveStats.batteryLevel}%" else "Reading…",
                    subtitle = if (liveStats.isCharging) "CHARGING" else "BATTERY",
                    icon = Icons.Default.BatteryStd,
                    iconBg = cs.primaryContainer,
                    iconTint = cs.onPrimaryContainer
                )
                M3StatCard(
                    modifier = Modifier.weight(1f),
                    title = "LED Matrix",
                    value = "Active",
                    subtitle = "SYSFS ACTIVE",
                    icon = Icons.Default.Lightbulb,
                    iconBg = cs.secondaryContainer,
                    iconTint = cs.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Performance Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Performance",
                    style = MaterialTheme.typography.titleLarge,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHigh)
                        .bounceClick(onClick = onNavigateToPerformance)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Monitor",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            M3PerformanceCard(
                cpuPct = liveStats.cpuPct,
                ramUsedMb = liveStats.ramUsedMb,
                ramTotalMb = liveStats.ramTotalMb
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Name & App Version Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Xi'annnnnn / @kasajin001",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.outline
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary
                )
            }

            // Expanded bottom spacer to guarantee complete clearance above floating dock
            Spacer(modifier = Modifier.height(130.dp))
        }
    }
}

@Composable
fun M3DeviceCard(deviceInfo: DeviceInfoState) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Column {
            // Compact Header Banner Image Box (130dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.card_banner),
                    contentDescription = "Device Banner",
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    cs.surfaceContainerHigh.copy(alpha = 0.4f),
                                    cs.surfaceContainerHigh
                                )
                            )
                        )
                )
            }

            // Compact Card Details
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Device Model Title
                Text(
                    text = deviceInfo.model,
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Android Version Subtitle
                Text(
                    text = deviceInfo.androidVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )

                if (deviceInfo.kernelVersion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = deviceInfo.kernelVersion,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = cs.outline.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Root Status Line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveMorphingStatusIndicator(isRooted = deviceInfo.isRooted)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Root Status: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        text = if (deviceInfo.isRooted) "ACTIVE" else "INACTIVE",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (deviceInfo.isRooted) cs.primary else cs.error
                    )
                }
            }
        }
    }
}

@Composable
fun ExpressiveMorphingStatusIndicator(isRooted: Boolean) {
    val cs = MaterialTheme.colorScheme
    val color = if (isRooted) cs.primary else cs.error

    val transition = rememberInfiniteTransition(label = "expressiveMorphingPulse")
    val cornerRadiusFraction by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morphCorner"
    )

    val morphRadius by animateDpAsState(
        targetValue = if (isRooted) (20 * cornerRadiusFraction).dp else 4.dp,
        animationSpec = spring(),
        label = "morphRadius"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(morphRadius))
                .background(color.copy(alpha = 0.35f))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun M3StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = modifier.bounceClick(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = cs.outline, letterSpacing = 1.1.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
        }
    }
}

@Composable
fun M3PerformanceCard(
    cpuPct: Float,
    ramUsedMb: Int,
    ramTotalMb: Int
) {
    val cs = MaterialTheme.colorScheme
    val ramFrac = if (ramTotalMb > 0) (ramUsedMb.toFloat() / ramTotalMb.toFloat()).coerceIn(0f, 1f) else 0f

    val animatedCpu by animateFloatAsState(
        targetValue = cpuPct.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "smoothCpuLine"
    )

    val animatedRam by animateFloatAsState(
        targetValue = ramFrac,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "smoothRamLine"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "CPU Load", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(text = "${(cpuPct * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedCpu },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = cs.primary,
                trackColor = cs.surfaceContainerHighest
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "RAM Allocation", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                Text(
                    text = if (ramTotalMb > 0) "${(ramUsedMb / 1024f).let { "%.1f".format(it) }} GB / ${(ramTotalMb / 1024f).let { "%.1f".format(it) }} GB" else "Reading…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedRam },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = cs.secondary,
                trackColor = cs.surfaceContainerHighest
            )
        }
    }
}
