package com.xiiann.ledsync.presentation.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.data.repository.LogEntry
import com.xiiann.ledsync.data.repository.LogLevel
import com.xiiann.ledsync.presentation.theme.ConsoleBg
import com.xiiann.ledsync.presentation.theme.ConsoleBlue
import com.xiiann.ledsync.presentation.theme.ConsoleError
import com.xiiann.ledsync.presentation.theme.ConsoleHeaderBg
import com.xiiann.ledsync.presentation.theme.ConsoleMuted
import com.xiiann.ledsync.presentation.theme.ConsoleSuccess
import com.xiiann.ledsync.presentation.theme.ConsoleWarning
import com.xiiann.ledsync.presentation.theme.bounceClick

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedLabScreen(viewModel: LedLabViewModel) {
    val isReady by viewModel.isReady.collectAsState()
    val actionLogs by viewModel.actionLogs.collectAsState()
    var activeEffect by remember { mutableStateOf<String?>(null) }
    var showKernelExtra by remember { mutableStateOf(false) }
    val deviceConfig = remember { viewModel.getDeviceConfig() }

    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(top = 24.dp, bottom = 18.dp)) {
            Text(
                text = "Hardware Lab",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                color = cs.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Effect presets and live console",
                fontSize = 15.sp,
                color = cs.onSurfaceVariant
            )
        }

        // Terminal Log Console
        AospLogConsole(
            logs = actionLogs,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        )

        // Core Effect Presets Header
        Text(
            text = "Effect presets",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 12.dp)
        )

        PresetChipGrid(
            presets = deviceConfig.ledEffects,
            loopingPatterns = deviceConfig.loopingPatterns,
            activeEffect = activeEffect,
            enabled = isReady,
            onSelect = { name, hex ->
                activeEffect = name
                viewModel.sendEffect(name, hex)
            }
        )

        if (deviceConfig.extraKernelEffects.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.surfaceContainer)
                    .bounceClick { showKernelExtra = !showKernelExtra }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kernel Extra (${deviceConfig.extraKernelEffects.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "PUBG, MLBB & raw aw22xxx store_tran_led_cmd modes",
                        fontSize = 12.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (showKernelExtra) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = showKernelExtra) {
                Box(modifier = Modifier.padding(top = 10.dp)) {
                    PresetChipGrid(
                        presets = deviceConfig.extraKernelEffects,
                        loopingPatterns = deviceConfig.loopingPatterns,
                        activeEffect = activeEffect,
                        enabled = isReady,
                        onSelect = { name, hex ->
                            activeEffect = name
                            viewModel.sendEffect(name, hex)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Emergency Hardware Reset Pill Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cs.error)
                .bounceClick {
                    activeEffect = null
                    viewModel.emergencyKillAndRevive()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Emergency,
                contentDescription = "Emergency Hardware Reset",
                tint = cs.onError,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "EMERGENCY HARDWARE RESET",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 0.3.sp,
                color = cs.onError
            )
        }

        Spacer(modifier = Modifier.height(110.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetChipGrid(
    presets: Map<String, String>,
    loopingPatterns: Set<String>,
    activeEffect: String?,
    enabled: Boolean,
    onSelect: (String, String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (name, hex) ->
            val selected = activeEffect == name
            val isLooping = loopingPatterns.contains(name)
            val corner by animateDpAsState(
                targetValue = if (selected) 14.dp else 23.dp,
                animationSpec = spring(),
                label = "presetChipCorner"
            )
            Row(
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(corner))
                    .background(if (selected) cs.primary else cs.surfaceContainer)
                    .bounceClick(enabled = enabled) { onSelect(name, hex) }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) cs.onPrimary else cs.onSurface
                )
                if (isLooping) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Loop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (selected) cs.onPrimary else cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AospLogConsole(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val recentLogs = remember(logs) { logs.takeLast(60) }

    LaunchedEffect(recentLogs.size) {
        if (recentLogs.isNotEmpty()) {
            listState.scrollToItem(recentLogs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 14.dp, bottomStart = 44.dp))
            .background(ConsoleBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleHeaderBg)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ConsoleError))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ConsoleWarning))
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(ConsoleSuccess))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "led-console ~ su",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = ConsoleMuted
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (recentLogs.isEmpty()) {
                item {
                    Text(
                        text = "[00:00:00] Initializing hardware components…",
                        fontSize = 12.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ConsoleBlue
                    )
                }
            } else {
                items(recentLogs, key = { it.id }) { log ->
                    val textColor = when (log.level) {
                        LogLevel.SUCCESS -> ConsoleSuccess
                        LogLevel.WARNING -> ConsoleWarning
                        LogLevel.ERROR -> ConsoleError
                        LogLevel.INFO -> ConsoleBlue
                    }
                    Text(
                        text = log.message,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }
            }
        }
    }
}
