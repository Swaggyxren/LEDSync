package com.xiiann.ledsync.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.BuildConfig
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.common.LightBeltHeroArtwork
import com.xiiann.ledsync.presentation.theme.LocalExtraColors
import com.xiiann.ledsync.presentation.theme.bounceClick

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPerformance: () -> Unit,
    onNavigateToAudioLed: () -> Unit,
    onNavigateToAppAlerts: () -> Unit,
    onNavigateToBatteryConfig: () -> Unit
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val liveStats by viewModel.liveStats.collectAsState()
    val masterEnabled by viewModel.masterEnabled.collectAsState()
    val isSuppressed by viewModel.isSuppressed.collectAsState()
    val suppressionReason by viewModel.suppressionReason.collectAsState()

    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val serviceActive = masterEnabled && !isSuppressed

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
                .padding(horizontal = 18.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                Text(
                    text = "LED Sync",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.8).sp,
                    color = cs.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Device overview",
                    fontSize = 14.sp,
                    color = cs.onSurfaceVariant
                )
            }

            // Hero Device Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 14.dp))
                    .background(cs.surfaceContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                        .clipToBounds()
                ) {
                    LightBeltHeroArtwork(
                        isActive = serviceActive,
                        modifier = Modifier.fillMaxSize()
                    )

                    val modeChipText = when {
                        !masterEnabled -> "Manual · Paused"
                        isSuppressed -> "Guard · ${suppressionReason ?: "Paused"}"
                        else -> "Auto · Normal"
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 24.dp, top = 22.dp)
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(cs.primaryContainer)
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modeChipText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 18.dp, end = 18.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (masterEnabled) cs.primary else cs.surfaceContainerHighest)
                            .bounceClick { viewModel.toggleMasterEnabled() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Toggle LED service",
                            tint = if (masterEnabled) cs.onPrimary else cs.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 18.dp)
                ) {
                    Text(
                        text = deviceInfo.model,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        lineHeight = 25.sp,
                        color = cs.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = deviceInfo.androidVersion,
                        fontSize = 13.sp,
                        color = cs.onSurface.copy(alpha = 0.75f)
                    )
                    if (deviceInfo.kernelVersion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = deviceInfo.kernelVersion,
                            fontSize = 12.sp,
                            color = cs.onSurface.copy(alpha = 0.6f),
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (serviceActive) cs.primary else cs.surfaceContainerHighest)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (serviceActive) cs.onPrimary else cs.outline)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                !masterEnabled -> "LED service: OFF"
                                isSuppressed -> "LED service: GUARDED"
                                else -> "LED service: ACTIVE"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (serviceActive) cs.onPrimary else cs.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Asymmetric Battery + Sysfs Tiles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(114.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 36.dp, bottomEnd = 14.dp, bottomStart = 36.dp))
                        .background(extra.accentContainer)
                        .bounceClick(onClick = onNavigateToBatteryConfig)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.TILTED_PEBBLE,
                        containerColor = cs.primary,
                        icon = Icons.Default.Battery5Bar,
                        iconTint = cs.onPrimary,
                        size = 42.dp,
                        iconSize = 20.dp
                    )
                    Column {
                        Text(
                            text = if (liveStats.isCharging) "Battery · Charging" else "Battery",
                            fontSize = 12.sp,
                            color = extra.onAccentContainer.copy(alpha = 0.78f)
                        )
                        Text(
                            text = if (liveStats.batteryLevel > 0) "${liveStats.batteryLevel}%" else "--%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = extra.onAccentContainer
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(114.dp)
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 14.dp))
                        .background(cs.tertiaryContainer)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.CLOVER,
                        containerColor = cs.primary,
                        icon = Icons.Default.Lightbulb,
                        iconTint = cs.onPrimary,
                        size = 42.dp,
                        iconSize = 20.dp
                    )
                    Column {
                        Text(
                            text = "Sysfs",
                            fontSize = 12.sp,
                            color = cs.onTertiaryContainer.copy(alpha = 0.78f)
                        )
                        Text(
                            text = if (deviceInfo.isRooted) "Active" else "Pending",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = cs.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Controls Bento Section
            Text(
                text = "Controls",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(246.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tall Audio Reactive LED Card
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 14.dp, bottomEnd = 44.dp, bottomStart = 14.dp))
                        .background(cs.primary)
                        .bounceClick(onClick = onNavigateToAudioLed)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.SUNBURST,
                        containerColor = cs.onPrimary,
                        icon = Icons.Default.GraphicEq,
                        iconTint = cs.primary,
                        size = 64.dp,
                        iconSize = 29.dp
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val bars = listOf(0.30f, 0.67f, 0.34f, 0.71f, 0.38f, 0.75f, 0.42f, 0.79f)
                        bars.forEach { frac ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(cs.onPrimary.copy(alpha = 0.8f))
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Audio Reactive LED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                            color = cs.onPrimary
                        )
                        Text(
                            text = "Spectrum or static",
                            fontSize = 13.sp,
                            color = cs.onPrimary.copy(alpha = 0.78f)
                        )
                    }
                }

                // Stacked App Sync + Battery Studio Cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 36.dp, bottomEnd = 14.dp, bottomStart = 36.dp))
                            .background(cs.secondaryContainer)
                            .bounceClick(onClick = onNavigateToAppAlerts)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BlobIconBadge(
                            blob = ExpressiveBlob.SOFT_DIAMOND,
                            containerColor = cs.primary,
                            icon = Icons.Default.Apps,
                            iconTint = cs.onPrimary,
                            size = 40.dp,
                            iconSize = 18.dp
                        )
                        Text(
                            text = "App LED Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                            color = cs.onSecondaryContainer
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 14.dp))
                            .background(extra.warmContainer)
                            .bounceClick(onClick = onNavigateToBatteryConfig)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BlobIconBadge(
                            blob = ExpressiveBlob.WAVE_BLOB,
                            containerColor = cs.primary,
                            icon = Icons.Default.Bolt,
                            iconTint = cs.onPrimary,
                            size = 40.dp,
                            iconSize = 18.dp
                        )
                        Text(
                            text = "Battery LED Studio",
                            fontSize = 16.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                            color = extra.onWarmContainer
                        )
                    }
                }
            }

            // System Performance Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System performance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(cs.secondaryContainer)
                        .bounceClick(onClick = onNavigateToPerformance)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Monitor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = cs.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Performance Summary Card
            val cpuPct = liveStats.cpuPct.coerceIn(0f, 1f)
            val ramFrac = if (liveStats.ramTotalMb > 0) {
                (liveStats.ramUsedMb.toFloat() / liveStats.ramTotalMb.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val animatedCpuPct by animateFloatAsState(
                targetValue = cpuPct.coerceAtLeast(0.03f),
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "homeCpuBar"
            )
            val animatedRamFrac by animateFloatAsState(
                targetValue = ramFrac.coerceAtLeast(0.03f),
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                label = "homeRamBar"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 44.dp, bottomEnd = 44.dp, bottomStart = 44.dp))
                    .background(cs.surfaceContainer)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CPU load", fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Text(
                        text = "${(cpuPct * 100).toInt()}%",
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedCpuPct)
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primary)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "RAM allocation", fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Text(
                        text = if (liveStats.ramTotalMb > 0) {
                            "${"%.1f".format(liveStats.ramUsedMb / 1024f)} / ${"%.1f".format(liveStats.ramTotalMb / 1024f)} GB"
                        } else {
                            "0.0 / 0.0 GB"
                        },
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedRamFrac)
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.tertiary)
                    )
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Xi'annnnnn / @kasajin001",
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariant
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.primary
                )
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}
