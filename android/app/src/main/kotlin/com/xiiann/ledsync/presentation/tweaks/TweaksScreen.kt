package com.xiiann.ledsync.presentation.tweaks

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.domain.model.AppearanceMode
import com.xiiann.ledsync.domain.model.StockBehaviorSettings
import com.xiiann.ledsync.domain.model.StockGuardPolicy
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.common.ExpressivePillSwitch
import com.xiiann.ledsync.presentation.theme.LocalExtraColors
import com.xiiann.ledsync.presentation.theme.bounceClick

@Composable
fun TweaksScreen(
    appearanceMode: AppearanceMode,
    stockBehavior: StockBehaviorSettings,
    isNotifAccessGranted: Boolean,
    isRootGranted: Boolean,
    onAppearanceChange: (AppearanceMode) -> Unit,
    onUpdateStockBehavior: ((StockBehaviorSettings) -> StockBehaviorSettings) -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onNavigateToAppAlerts: () -> Unit,
    onNavigateToBatteryConfig: () -> Unit,
    onNavigateToAudioLed: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 18.dp)) {
            Text(
                text = "Tweaks",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = cs.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Modes, triggers and preferences",
                fontSize = 15.sp,
                color = cs.onSurfaceVariant
            )
        }

        // Primary Feature Navigation Group
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            GroupedNavRow(
                title = "Audio Reactive LED",
                subtitle = "Spectrum and static modes",
                blob = ExpressiveBlob.SUNBURST,
                blobColor = cs.primary,
                icon = Icons.Default.GraphicEq,
                iconTint = cs.onPrimary,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                onClick = onNavigateToAudioLed
            )
            GroupedNavRow(
                title = "App LED Sync",
                subtitle = "Per-app effect mapping",
                blob = ExpressiveBlob.SOFT_DIAMOND,
                blobColor = cs.surfaceContainerHighest,
                icon = Icons.Default.Apps,
                iconTint = cs.onSurface,
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToAppAlerts
            )
            GroupedNavRow(
                title = "Battery LED Studio",
                subtitle = "Triggers and patterns",
                blob = ExpressiveBlob.WAVE_BLOB,
                blobColor = extra.warmContainer,
                icon = Icons.Default.Bolt,
                iconTint = extra.onWarmContainer,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
                onClick = onNavigateToBatteryConfig
            )
        }

        // Appearance Segmented Control
        Text(
            text = "Appearance",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(cs.surfaceContainerHighest)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppearanceMode.entries.forEach { mode ->
                val selected = appearanceMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selected) cs.primary else Color.Transparent)
                        .bounceClick { onAppearanceChange(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) cs.onPrimary else cs.onSurfaceVariant
                    )
                }
            }
        }

        // Access Section
        Text(
            text = "Access",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(cs.surfaceContainer)
                    .bounceClick(onClick = onOpenNotificationAccess)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlobIconBadge(
                    blob = ExpressiveBlob.CLOVER,
                    containerColor = cs.tertiaryContainer,
                    icon = Icons.Default.Notifications,
                    iconTint = cs.onTertiaryContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Notification access",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Required for app sync",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                ExpressivePillSwitch(
                    checked = isNotifAccessGranted,
                    onCheckedChange = { onOpenNotificationAccess() }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlobIconBadge(
                    blob = ExpressiveBlob.WAVE_BLOB,
                    containerColor = extra.accentContainer,
                    icon = Icons.Default.VerifiedUser,
                    iconTint = extra.onAccentContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LED service",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = if (isRootGranted) "Root sysfs controller ready" else "Superuser permission required",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (isRootGranted) cs.secondaryContainer else cs.errorContainer)
                        .padding(horizontal = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRootGranted) "Granted" else "Denied",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRootGranted) cs.onSecondaryContainer else cs.onErrorContainer
                    )
                }
            }
        }

        // Stock Integration & Guards Section
        Text(
            text = "Stock behavior & guards",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            GuardSwitchRow(
                title = "Rear-camera guard",
                subtitle = "Pause LEDs when rear camera opens",
                icon = Icons.Default.CameraAlt,
                iconBg = cs.primaryContainer,
                iconTint = cs.onPrimaryContainer,
                checked = stockBehavior.cameraGuardEnabled,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                onCheckedChange = { enabled ->
                    onUpdateStockBehavior { it.copy(cameraGuardEnabled = enabled) }
                }
            )
            GuardSwitchRow(
                title = "Respect Do Not Disturb",
                subtitle = "Suppress background LEDs in Zen mode",
                icon = Icons.Default.DoNotDisturbOn,
                iconBg = cs.tertiaryContainer,
                iconTint = cs.onTertiaryContainer,
                checked = stockBehavior.respectDnd,
                shape = RoundedCornerShape(12.dp),
                onCheckedChange = { enabled ->
                    onUpdateStockBehavior { it.copy(respectDnd = enabled) }
                }
            )
            GuardSwitchRow(
                title = "Respect Battery Saver",
                subtitle = "Pause background LEDs in low-power mode",
                icon = Icons.Default.BatterySaver,
                iconBg = extra.warmContainer,
                iconTint = extra.onWarmContainer,
                checked = stockBehavior.respectBatterySaver,
                shape = RoundedCornerShape(12.dp),
                onCheckedChange = { enabled ->
                    onUpdateStockBehavior { it.copy(respectBatterySaver = enabled) }
                }
            )

            // Active Hours Schedule Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.TILTED_PEBBLE,
                        containerColor = cs.secondaryContainer,
                        icon = Icons.Default.Schedule,
                        iconTint = cs.onSecondaryContainer,
                        size = 48.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Active schedule",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Text(
                            text = if (stockBehavior.scheduleEnabled) {
                                "Active ${StockGuardPolicy.formatMinutes(stockBehavior.scheduleStartMinutes)} – ${StockGuardPolicy.formatMinutes(stockBehavior.scheduleEndMinutes)}"
                            } else {
                                "Run LEDs all day"
                            },
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                    ExpressivePillSwitch(
                        checked = stockBehavior.scheduleEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateStockBehavior { it.copy(scheduleEnabled = enabled) }
                        }
                    )
                }

                if (stockBehavior.scheduleEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleWindowSelector(
                            label = "Start",
                            minutes = stockBehavior.scheduleStartMinutes,
                            modifier = Modifier.weight(1f),
                            onStepHour = { delta ->
                                val next = ((stockBehavior.scheduleStartMinutes / 60 + delta + 24) % 24) * 60
                                onUpdateStockBehavior { it.copy(scheduleStartMinutes = next) }
                            }
                        )
                        ScheduleWindowSelector(
                            label = "End",
                            minutes = stockBehavior.scheduleEndMinutes,
                            modifier = Modifier.weight(1f),
                            onStepHour = { delta ->
                                val next = ((stockBehavior.scheduleEndMinutes / 60 + delta + 24) % 24) * 60
                                onUpdateStockBehavior { it.copy(scheduleEndMinutes = next) }
                            }
                        )
                    }
                }
            }

            // Phone Call Pattern Selector
            var callMenuExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlobIconBadge(
                    blob = ExpressiveBlob.SUNBURST,
                    containerColor = extra.accentContainer,
                    icon = Icons.Default.Call,
                    iconTint = extra.onAccentContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone call pattern",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Pattern played while ringing",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                Box {
                    Row(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cs.surfaceContainerHighest)
                            .bounceClick { callMenuExpanded = true }
                            .padding(start = 16.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stockBehavior.phoneCallEffectName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = callMenuExpanded,
                        onDismissRequest = { callMenuExpanded = false }
                    ) {
                        StockGuardPolicy.PHONE_CALL_PATTERNS.forEach { pattern ->
                            DropdownMenuItem(
                                text = { Text(pattern) },
                                onClick = {
                                    onUpdateStockBehavior { it.copy(phoneCallEffectName = pattern) }
                                    callMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@Composable
private fun GroupedNavRow(
    title: String,
    subtitle: String,
    blob: ExpressiveBlob,
    blobColor: Color,
    icon: ImageVector,
    iconTint: Color,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainer)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BlobIconBadge(
            blob = blob,
            containerColor = blobColor,
            icon = icon,
            iconTint = iconTint,
            size = 52.dp,
            iconSize = 23.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            Text(text = subtitle, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun GuardSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    checked: Boolean,
    shape: RoundedCornerShape,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BlobIconBadge(
            blob = ExpressiveBlob.SOFT_DIAMOND,
            containerColor = iconBg,
            icon = icon,
            iconTint = iconTint,
            size = 48.dp,
            iconSize = 22.dp
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            Text(text = subtitle, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }
        ExpressivePillSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ScheduleWindowSelector(
    label: String,
    minutes: Int,
    modifier: Modifier = Modifier,
    onStepHour: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$label ${StockGuardPolicy.formatMinutes(minutes)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(cs.surfaceContainer)
                    .bounceClick { onStepHour(-1) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "-", fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(cs.surfaceContainer)
                    .bounceClick { onStepHour(1) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", fontWeight = FontWeight.Bold, color = cs.onSurface)
            }
        }
    }
}
