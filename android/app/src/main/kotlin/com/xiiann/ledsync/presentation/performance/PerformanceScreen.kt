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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.theme.bounceClick

@Composable
fun PerformanceScreen(
    viewModel: PerformanceViewModel,
    onBack: () -> Unit
) {
    val metrics by viewModel.metrics.collectAsState()
    val history by viewModel.cpuHistory.collectAsState()
    val cs = MaterialTheme.colorScheme

    Scaffold(containerColor = cs.surface) { padding ->
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
                    text = "Performance Monitor",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                    color = cs.onSurface
                )
            }

            // 1. CPU Load Sparkline Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 14.dp, bottomEnd = 44.dp, bottomStart = 14.dp))
                    .background(cs.surfaceContainer)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.SOFT_DIAMOND,
                        containerColor = cs.primary,
                        icon = Icons.Default.Speed,
                        iconTint = cs.onPrimary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CPU load", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text(text = "Realtime usage graph", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                    Text(
                        text = "${(metrics.cpuPct * 100).toInt()}%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.primary
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                AospSparklineGraph(
                    history = history,
                    lineColor = cs.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. RAM Breakdown Card
            val ramUsed = metrics.ramUsedMb
            val ramTotal = metrics.ramTotalMb
            val ramAvail = (ramTotal - ramUsed).coerceAtLeast(0)
            val ramFrac = if (ramTotal > 0) (ramUsed.toFloat() / ramTotal.toFloat()).coerceIn(0f, 1f) else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 44.dp, bottomEnd = 14.dp, bottomStart = 44.dp))
                    .background(cs.surfaceContainer)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.CLOVER,
                        containerColor = cs.tertiaryContainer,
                        icon = Icons.Default.Memory,
                        iconTint = cs.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "RAM breakdown", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text(text = "Physical memory allocation", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ramFrac.coerceAtLeast(0.03f))
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.primary)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AospStatChip(label = "Used", value = "${"%.1f".format(ramUsed / 1024f)} GB", modifier = Modifier.weight(1f))
                    AospStatChip(label = "Free", value = "${"%.1f".format(ramAvail / 1024f)} GB", modifier = Modifier.weight(1f))
                    AospStatChip(label = "Total", value = "${"%.1f".format(ramTotal / 1024f)} GB", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Storage Capacity Card
            val storageUsed = metrics.storageUsedGb
            val storageTotal = metrics.storageTotalGb
            val storageFree = (storageTotal - storageUsed).coerceAtLeast(0f)
            val storageFrac = if (storageTotal > 0f) (storageUsed / storageTotal).coerceIn(0f, 1f) else 0f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 14.dp, bottomEnd = 44.dp, bottomStart = 14.dp))
                    .background(cs.surfaceContainer)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.TILTED_PEBBLE,
                        containerColor = cs.secondaryContainer,
                        icon = Icons.Default.SdStorage,
                        iconTint = cs.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "Storage capacity", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text(text = "Internal flash storage breakdown", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(storageFrac.coerceAtLeast(0.03f))
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cs.tertiary)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AospStatChip(label = "Used", value = "${"%.1f".format(storageUsed)} GB", modifier = Modifier.weight(1f))
                    AospStatChip(label = "Free", value = "${"%.1f".format(storageFree)} GB", modifier = Modifier.weight(1f))
                    AospStatChip(label = "Total", value = "${"%.1f".format(storageTotal)} GB", modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. System Diagnostics
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(cs.surfaceContainer)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.SUNBURST,
                        containerColor = cs.primaryContainer,
                        icon = Icons.Default.Dns,
                        iconTint = cs.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = "System diagnostics", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text(text = "Kernel & sysfs controller uptime", fontSize = 13.sp, color = cs.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AospStatChip(label = "Uptime", value = metrics.uptimeFormatted, modifier = Modifier.weight(1f))
                    AospStatChip(label = "Sysfs LED", value = "Active", modifier = Modifier.weight(1f))
                    AospStatChip(label = "Kernel", value = metrics.kernelVersion.ifBlank { "--" }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun AospStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceContainerHighest)
            .padding(14.dp)
    ) {
        Text(text = label, fontSize = 13.sp, color = cs.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface, maxLines = 1)
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
            drawLine(color = trackBg, start = Offset(0f, height / 2), end = Offset(width, height / 2), strokeWidth = 2.dp.toPx())
            return@Canvas
        }
        val stepX = width / (history.size - 1)
        val points = history.mapIndexed { idx, valPct ->
            Offset(idx * stepX, height - (valPct.coerceIn(0f, 1f) * height))
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
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
