package com.xiiann.ledsync.presentation.battery

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.common.ExpressiveTrackSlider
import com.xiiann.ledsync.presentation.theme.LocalExtraColors
import com.xiiann.ledsync.presentation.theme.bounceClick
import kotlin.math.roundToInt

@Composable
fun BatteryConfigScreen(
    viewModel: BatteryConfigViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.workingConfig.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val liveBattery by viewModel.liveBattery.collectAsState()
    val deviceConfig = remember { viewModel.getDeviceConfig() }
    val availableEffects = remember { deviceConfig.ledEffects.keys.toList().sorted() }

    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current

    Scaffold(
        containerColor = cs.surface,
        floatingActionButton = {
            if (isDirty) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveConfig() },
                    icon = { Icon(imageVector = Icons.Default.Save, contentDescription = "Save") },
                    text = { Text("Save Configuration", fontWeight = FontWeight.Bold) },
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.bounceClick()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest)
                        .bounceClick(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = cs.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Battery LED Studio",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    color = cs.onSurface
                )
            }

            // Circular Battery Gauge Tile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(44.dp))
                    .background(cs.surfaceContainer)
                    .padding(vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val ringColor = when {
                    liveBattery.isCharging -> cs.primary
                    liveBattery.level > 30 -> cs.primary
                    liveBattery.level > 15 -> extra.warmAccent
                    else -> cs.error
                }
                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    val trackColor = cs.surfaceContainerHighest
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 16.dp.toPx()
                        val radius = (size.minDimension - stroke) / 2
                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(color = trackColor, radius = radius, center = center, style = Stroke(width = stroke))
                        if (liveBattery.level > 0) {
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = (liveBattery.level / 100f) * 360f,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text = "${liveBattery.level}%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (liveBattery.isCharging) "CHARGING" else "BATTERY",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = cs.onSurfaceVariant
                )
            }

            Text(
                text = "Trigger thresholds",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
            )

            // Low Battery Threshold Card
            ExpressiveThresholdCard(
                title = "Low battery",
                subtitle = "Trigger threshold (20%–30%)",
                value = config.lowThreshold.coerceIn(20, 30),
                range = 20f..30f,
                containerColor = extra.warmContainer,
                contentColor = extra.onWarmContainer,
                blob = ExpressiveBlob.TILTED_PEBBLE,
                blobColor = extra.warmAccent,
                icon = Icons.Default.Battery2Bar,
                iconTint = cs.surface,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 14.dp),
                onValueChange = { raw ->
                    val rounded = ((raw / 2f).roundToInt() * 2).coerceIn(20, 30)
                    viewModel.updateLowThreshold(rounded)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Critical Battery Threshold Card
            ExpressiveThresholdCard(
                title = "Critical battery",
                subtitle = "Trigger threshold (4%–20%)",
                value = config.criticalThreshold.coerceIn(4, 20),
                range = 4f..20f,
                containerColor = cs.errorContainer,
                contentColor = cs.onErrorContainer,
                blob = ExpressiveBlob.WAVE_BLOB,
                blobColor = cs.error,
                icon = Icons.Default.BatteryAlert,
                iconTint = cs.onError,
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 36.dp, bottomEnd = 14.dp, bottomStart = 36.dp),
                onValueChange = { raw ->
                    val rounded = ((raw / 2f).roundToInt() * 2).coerceIn(4, 20)
                    viewModel.updateCriticalThreshold(rounded)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Full Charge Threshold Card
            ExpressiveThresholdCard(
                title = "Full charge",
                subtitle = "Trigger threshold (70%–100%)",
                value = config.fullThreshold.coerceIn(70, 100),
                range = 70f..100f,
                containerColor = cs.tertiaryContainer,
                contentColor = cs.onTertiaryContainer,
                blob = ExpressiveBlob.CLOVER,
                blobColor = cs.primary,
                icon = Icons.Default.BatteryFull,
                iconTint = cs.onPrimary,
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 14.dp),
                onValueChange = { raw ->
                    val rounded = ((raw / 5f).roundToInt() * 5).coerceIn(70, 100)
                    viewModel.updateFullThreshold(rounded)
                }
            )

            Text(
                text = "Visual effects",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GroupedEffectRow(
                    label = "Low battery pattern",
                    hint = "default: Rise",
                    selectedEffect = config.lowEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
                    onSelect = { viewModel.updateLowEffect(it) }
                )
                GroupedEffectRow(
                    label = "Critical battery pattern",
                    hint = "default: Lightning",
                    selectedEffect = config.criticalEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.Default.PriorityHigh,
                    shape = RoundedCornerShape(12.dp),
                    onSelect = { viewModel.updateCriticalEffect(it) }
                )
                GroupedEffectRow(
                    label = "Charging plug-in pattern",
                    hint = "stock: Charge Tiers (0–20 / 20–60 / 60–100)",
                    selectedEffect = "Charge Tiers",
                    availableEffects = emptyList(),
                    icon = Icons.Default.Bolt,
                    shape = RoundedCornerShape(12.dp),
                    onSelect = {}
                )
                GroupedEffectRow(
                    label = "Full charge pattern",
                    hint = "default: Halo",
                    selectedEffect = config.fullEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.Default.BatteryFull,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp),
                    onSelect = { viewModel.updateFullEffect(it) }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ExpressiveThresholdCard(
    title: String,
    subtitle: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    containerColor: Color,
    contentColor: Color,
    blob: ExpressiveBlob,
    blobColor: Color,
    icon: ImageVector,
    iconTint: Color,
    shape: RoundedCornerShape,
    onValueChange: (Float) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                Text(text = subtitle, fontSize = 13.sp, color = contentColor.copy(alpha = 0.78f))
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(cs.surface)
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "$value%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ExpressiveTrackSlider(
            value = value.toFloat(),
            valueRange = range,
            onValueChange = onValueChange,
            activeColor = contentColor,
            trackColor = cs.surfaceContainer
        )
    }
}

@Composable
private fun GroupedEffectRow(
    label: String,
    hint: String,
    selectedEffect: String,
    availableEffects: List<String>,
    icon: ImageVector,
    shape: RoundedCornerShape,
    onSelect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = cs.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
            Text(text = hint, fontSize = 13.sp, color = cs.onSurfaceVariant)
        }

        Box {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surfaceContainerHighest)
                    .then(if (availableEffects.isNotEmpty()) Modifier.bounceClick { expanded = true } else Modifier)
                    .padding(start = 16.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = selectedEffect, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                if (availableEffects.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }

            if (availableEffects.isNotEmpty()) {
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    availableEffects.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                onSelect(name)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
