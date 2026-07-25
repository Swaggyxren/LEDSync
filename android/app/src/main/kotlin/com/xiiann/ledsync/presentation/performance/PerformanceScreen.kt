package com.xiiann.ledsync.presentation.performance

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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PerformanceScreen(
    viewModel: PerformanceViewModel,
    onBack: () -> Unit
) {
    val metrics by viewModel.metrics.collectAsState()
    val history by viewModel.cpuHistory.collectAsState()

    val cs = MaterialTheme.colorScheme

    Scaffold(
        containerColor = cs.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            // Header Bar
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
                    text = "Performance Monitor",
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
                // 1. CPU Sparkline Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cs.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = "CPU Load", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                    Text(text = "Realtime usage graph", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                }
                            }
                            Text(
                                text = "${(metrics.cpuPct * 100).toInt()}%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = cs.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        AospSparklineGraph(
                            history = history,
                            lineColor = cs.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Memory Allocation Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cs.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Memory, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "RAM Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                Text(text = "Physical memory allocation", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val ramUsed = metrics.ramUsedMb
                        val ramTotal = metrics.ramTotalMb
                        val ramAvail = (ramTotal - ramUsed).coerceAtLeast(0)
                        val frac = if (ramTotal > 0) ramUsed.toFloat() / ramTotal.toFloat() else 0f

                        LinearProgressIndicator(
                            progress = { frac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = cs.secondary,
                            trackColor = cs.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AospStatChip(label = "USED", value = "${"%.1f".format(ramUsed / 1024f)} GB", color = cs.secondary, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "FREE", value = "${"%.1f".format(ramAvail / 1024f)} GB", color = cs.outline, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "TOTAL", value = "${"%.1f".format(ramTotal / 1024f)} GB", color = cs.onSurface, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Storage Capacity Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cs.tertiaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.SdStorage, contentDescription = null, tint = cs.onTertiaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Storage Capacity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                Text(text = "Internal flash storage breakdown", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val storageUsed = metrics.storageUsedGb
                        val storageTotal = metrics.storageTotalGb
                        val storageFree = (storageTotal - storageUsed).coerceAtLeast(0f)
                        val storageFrac = if (storageTotal > 0f) (storageUsed / storageTotal).coerceIn(0f, 1f) else 0f

                        LinearProgressIndicator(
                            progress = { storageFrac },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = cs.tertiary,
                            trackColor = cs.surfaceContainerHighest
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AospStatChip(label = "USED", value = "${"%.1f".format(storageUsed)} GB", color = cs.tertiary, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "FREE", value = "${"%.1f".format(storageFree)} GB", color = cs.outline, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "TOTAL", value = "${"%.1f".format(storageTotal)} GB", color = cs.onSurface, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. System Diagnostics & Uptime Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cs.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Dns, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "System Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                                Text(text = "Kernel & sysfs controller uptime", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AospStatChip(label = "UPTIME", value = metrics.uptimeFormatted, color = cs.primary, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "SYSFS LED", value = "Active", color = cs.secondary, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            AospStatChip(label = "KERNEL", value = metrics.kernelVersion, color = cs.onSurface, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Comfortable bottom padding so scroll area sits nicely above floating nav dock
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun AospStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color, letterSpacing = 1.1.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = cs.onSurface)
    }
}

@Composable
fun AospSparklineGraph(
    history: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val trackBg = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (history.size < 2) {
            drawLine(color = trackBg, start = androidx.compose.ui.geometry.Offset(0f, height / 2), end = androidx.compose.ui.geometry.Offset(width, height / 2), strokeWidth = 2.dp.toPx())
            return@Canvas
        }

        val stepX = width / (history.size - 1)
        val points = history.mapIndexed { idx, valPct ->
            val x = idx * stepX
            val y = height - (valPct.coerceIn(0f, 1f) * height)
            androidx.compose.ui.geometry.Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.size - 1) {
                val p1 = points[i]
                val p2 = points[i + 1]
                val cx = (p1.x + p2.x) / 2
                cubicTo(cx, p1.y, cx, p2.y, p2.x, p2.y)
            }
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
