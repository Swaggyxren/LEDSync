package com.xiiann.ledsync.presentation.lab

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.data.repository.LogEntry
import com.xiiann.ledsync.data.repository.LogLevel
import com.xiiann.ledsync.presentation.theme.ConsoleBg
import com.xiiann.ledsync.presentation.theme.ConsoleBlue
import com.xiiann.ledsync.presentation.theme.ConsoleBorder
import com.xiiann.ledsync.presentation.theme.ConsoleError
import com.xiiann.ledsync.presentation.theme.ConsoleSuccess
import com.xiiann.ledsync.presentation.theme.ConsoleWarning
import com.xiiann.ledsync.presentation.theme.bounceClick

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LedLabScreen(viewModel: LedLabViewModel) {
    val isReady by viewModel.isReady.collectAsState()
    val actionLogs by viewModel.actionLogs.collectAsState()
    var activeEffect by remember { mutableStateOf<String?>(null) }
    val deviceConfig = remember { viewModel.getDeviceConfig() }

    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Title Bar
        Text(
            text = "Hardware Lab",
            style = MaterialTheme.typography.displayMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 14.dp)
        )

        // Console Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG CONSOLE",
                style = MaterialTheme.typography.labelSmall,
                color = cs.outline,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "sysfs v2",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = cs.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Log Console Card
        AospLogConsole(
            logs = actionLogs,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Effect Grid Header
        Text(
            text = "EFFECT PRESETS",
            style = MaterialTheme.typography.labelSmall,
            color = cs.outline,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Expressive Segmented Container with Morphing Tiles
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            deviceConfig.ledEffects.forEach { (name, hex) ->
                val selected = activeEffect == name
                val isLooping = deviceConfig.loopingPatterns.contains(name)

                // Shape Morphing (MaterialShapes + Morph corner transition)
                val morphRadius by animateDpAsState(
                    targetValue = if (selected) 24.dp else if (isLooping) 16.dp else 8.dp,
                    animationSpec = spring(),
                    label = "expressiveShapeMorph"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(morphRadius))
                        .background(if (selected) cs.primaryContainer else cs.surfaceContainerHigh)
                        .bounceClick(enabled = isReady) {
                            activeEffect = name
                            viewModel.sendEffect(name, hex)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant
                        )
                        if (isLooping) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Loop,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (selected) cs.onPrimaryContainer else cs.outline
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emergency Hardware Reset Button
        Button(
            onClick = {
                activeEffect = null
                viewModel.emergencyKillAndRevive()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = cs.error,
                contentColor = cs.onError
            )
        ) {
            Icon(imageVector = Icons.Default.Emergency, contentDescription = "Kill", modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "EMERGENCY HARDWARE RESET",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun AospLogConsole(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // Take recent logs to keep UI light and fast
    val recentLogs = remember(logs) { logs.takeLast(60) }

    LaunchedEffect(recentLogs.size) {
        if (recentLogs.isNotEmpty()) {
            listState.scrollToItem(recentLogs.size - 1)
        }
    }

    Card(
        modifier = modifier.border(1.dp, ConsoleBorder.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ConsoleBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cs.surfaceContainerHighest.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF2B8B5)))
                Spacer(modifier = Modifier.width(5.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFBBF24)))
                Spacer(modifier = Modifier.width(5.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6DD585)))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "led-console ~ su",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = cs.outline
                )
            }

            // Log stream
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (recentLogs.isEmpty()) {
                    Text(
                        text = "No LED activity recorded.\nSelect an effect preset below.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ConsoleBlue.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(state = listState) {
                        items(
                            items = recentLogs,
                            key = { entry -> entry.timestamp + "_" + entry.message.hashCode() }
                        ) { entry ->
                            val color = when (entry.level) {
                                LogLevel.SUCCESS -> ConsoleSuccess
                                LogLevel.WARNING -> ConsoleWarning
                                LogLevel.ERROR -> ConsoleError
                                LogLevel.INFO -> ConsoleBlue
                            }
                            Text(
                                text = entry.message,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
