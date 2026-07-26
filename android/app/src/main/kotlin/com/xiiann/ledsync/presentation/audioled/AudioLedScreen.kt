package com.xiiann.ledsync.presentation.audioled

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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xiiann.ledsync.domain.model.AudioGain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLedScreen(
    viewModel: AudioLedViewModel,
    onBack: () -> Unit
) {
    val isEnabled by viewModel.isEnabled.collectAsState()
    val isDynamic by viewModel.isDynamic.collectAsState()
    val gainLevel by viewModel.gainLevel.collectAsState()
    val cs = MaterialTheme.colorScheme

    var sliderPosition by remember { mutableFloatStateOf(gainLevel.toFloat()) }
    LaunchedEffect(gainLevel) { sliderPosition = gainLevel.toFloat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
            .statusBarsPadding()
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(cs.surfaceContainerHigh)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = cs.onSurface
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Audio Reactive LED",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Master Enable Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEnabled) cs.primaryContainer.copy(alpha = 0.5f) else cs.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) cs.primary else cs.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isEnabled) cs.onPrimary else cs.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Audio Reactive LED",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) "Audio reactive mode is active" else "Audio reactive mode is turned off",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { viewModel.toggleEnable(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.onPrimary,
                                checkedTrackColor = cs.primary,
                                uncheckedThumbColor = cs.outline,
                                uncheckedTrackColor = cs.surfaceContainerHighest
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selection Card (Static vs Dynamic)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isEnabled) 1.0f else 0.5f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled && isDynamic) cs.primaryContainer else cs.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDynamic) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isEnabled && isDynamic) cs.onPrimaryContainer else cs.onSecondaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDynamic) "Dynamic RGB Mode" else "Static White Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDynamic) "Reactive spectrum RGB LED light show" else "Steady ambient white LED illumination",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isDynamic,
                            enabled = isEnabled,
                            onCheckedChange = { viewModel.toggleMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cs.onPrimary,
                                checkedTrackColor = cs.primary,
                                uncheckedThumbColor = cs.outline,
                                uncheckedTrackColor = cs.surfaceContainerHighest
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reactivity / Gain Slider Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isEnabled) 1.0f else 0.5f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(cs.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = cs.onTertiaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reactivity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cs.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "How sensitive the strip is to ambient sound",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        Text(
                            text = sliderPosition.toInt().toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it },
                        onValueChangeFinished = { viewModel.setGain(sliderPosition.toInt()) },
                        valueRange = AudioGain.MIN_LEVEL.toFloat()..AudioGain.MAX_LEVEL.toFloat(),
                        steps = AudioGain.MAX_LEVEL - AudioGain.MIN_LEVEL - 1,
                        enabled = isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = cs.primary,
                            activeTrackColor = cs.primary,
                            inactiveTrackColor = cs.surfaceContainerHighest
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Calm", style = MaterialTheme.typography.labelSmall, color = cs.outline)
                        Text(text = "Sensitive", style = MaterialTheme.typography.labelSmall, color = cs.outline)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced Feature Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Hardware Integration & Behavior",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Toggling switch engages Dynamic RGB audio reactivity or Static white ambient lighting. Master toggle completely turns off audio LED hardware mode when not needed.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}
