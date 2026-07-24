package com.xiiann.ledsync.presentation.battery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Battery LED Studio",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Battery Gauge Ring Card
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    BatteryRingCard(level = liveBattery.level, isCharging = liveBattery.isCharging)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Thresholds Section Header
                Text(
                    text = "TRIGGER THRESHOLDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.outline,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Low Battery Slider (20% - 30% in discrete 2% steps)
                M3ThresholdSliderCard(
                    title = "Low Battery",
                    value = config.lowThreshold.coerceIn(20, 30),
                    range = 20f..30f,
                    steps = 4,
                    icon = Icons.Default.Battery2Bar,
                    iconBg = cs.primaryContainer,
                    iconTint = cs.onPrimaryContainer,
                    sliderColor = cs.primary,
                    onValueChange = { raw ->
                        val rounded = ((raw / 2f).roundToInt() * 2).coerceIn(20, 30)
                        viewModel.updateLowThreshold(rounded)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Critical Battery Slider (4% - 20% in discrete 2% steps)
                M3ThresholdSliderCard(
                    title = "Critical Battery",
                    value = config.criticalThreshold.coerceIn(4, 20),
                    range = 4f..20f,
                    steps = 7,
                    icon = Icons.Default.BatteryAlert,
                    iconBg = cs.errorContainer,
                    iconTint = cs.onErrorContainer,
                    sliderColor = cs.error,
                    onValueChange = { raw ->
                        val rounded = ((raw / 2f).roundToInt() * 2).coerceIn(4, 20)
                        viewModel.updateCriticalThreshold(rounded)
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Full Charge Slider (70% - 100% in discrete 5% steps)
                M3ThresholdSliderCard(
                    title = "Full Charge",
                    value = config.fullThreshold.coerceIn(70, 100),
                    range = 70f..100f,
                    steps = 5,
                    icon = Icons.Default.BatteryFull,
                    iconBg = cs.tertiaryContainer,
                    iconTint = cs.onTertiaryContainer,
                    sliderColor = cs.tertiary,
                    onValueChange = { raw ->
                        val rounded = ((raw / 5f).roundToInt() * 5).coerceIn(70, 100)
                        viewModel.updateFullThreshold(rounded)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Visual Effects Section Header
                Text(
                    text = "VISUAL EFFECTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.outline,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Low Effect Row
                M3EffectDropdownRow(
                    label = "Low Battery Pattern",
                    hint = "default: Rise",
                    selectedEffect = config.lowEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    onSelect = { viewModel.updateLowEffect(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Critical Effect Row
                M3EffectDropdownRow(
                    label = "Critical Battery Pattern",
                    hint = "default: Lightning",
                    selectedEffect = config.criticalEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.Default.FlashOn,
                    onSelect = { viewModel.updateCriticalEffect(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Full Effect Row
                M3EffectDropdownRow(
                    label = "Full Charge Pattern",
                    hint = "default: Pureness",
                    selectedEffect = config.fullEffectName,
                    availableEffects = availableEffects,
                    icon = Icons.Default.BatteryFull,
                    onSelect = { viewModel.updateFullEffect(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = cs.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Full-charge notification repeats while charging and automatically dismisses upon unplugging.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun BatteryRingCard(level: Int, isCharging: Boolean) {
    val cs = MaterialTheme.colorScheme
    val ringColor = when {
        isCharging -> cs.tertiary
        level > 40 -> cs.primary
        level > 20 -> Color(0xFFFFA726)
        else -> cs.error
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = cs.outlineVariant.copy(alpha = 0.35f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 8.dp.toPx()
                val radius = (size.minDimension - stroke) / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(color = trackColor, radius = radius, center = center, style = Stroke(width = stroke))
                if (level > 0) {
                    val sweepAngle = (level / 100f) * 360f
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$level%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
                Text(
                    text = if (isCharging) "CHARGING" else "BATTERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCharging) cs.tertiary else cs.outline,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}

@Composable
fun M3ThresholdSliderCard(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    sliderColor: Color,
    onValueChange: (Float) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Text(text = "Trigger threshold (${range.start.toInt()}%–${range.endInclusive.toInt()}%)", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }

                // Numeric Badge Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$value%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = sliderColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Slider
            Slider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = sliderColor,
                    activeTrackColor = sliderColor,
                    inactiveTrackColor = cs.surfaceContainerHighest
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun M3EffectDropdownRow(
    label: String,
    hint: String,
    selectedEffect: String?,
    availableEffects: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSelect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = cs.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                Text(text = hint, style = MaterialTheme.typography.bodySmall, color = cs.outline)
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.surfaceContainerHighest)
                        .bounceClick { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedEffect ?: "None", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = cs.outline, modifier = Modifier.size(18.dp))
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    availableEffects.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
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
