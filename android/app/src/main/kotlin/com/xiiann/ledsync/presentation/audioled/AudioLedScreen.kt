package com.xiiann.ledsync.presentation.audioled

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.domain.model.AudioGain
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.common.ExpressivePillSwitch
import com.xiiann.ledsync.presentation.common.ExpressiveTrackSlider
import com.xiiann.ledsync.presentation.theme.bounceClick
import kotlin.math.roundToInt

@Composable
fun AudioLedScreen(
    viewModel: AudioLedViewModel,
    onBack: () -> Unit
) {
    val isEnabled by viewModel.isEnabled.collectAsState()
    val isDynamic by viewModel.isDynamic.collectAsState()
    val gainLevel by viewModel.gainLevel.collectAsState()
    val stockBehavior by viewModel.stockBehavior.collectAsState()
    val cs = MaterialTheme.colorScheme

    var sliderPosition by remember { mutableFloatStateOf(gainLevel.toFloat()) }
    LaunchedEffect(gainLevel) { sliderPosition = gainLevel.toFloat() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.surface)
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
                text = "Audio Reactive LED",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.6).sp,
                color = cs.onSurface
            )
        }

        // Hero Primary Toggle + 24-Bar Visualizer Card
        val heroBg = if (isEnabled) cs.primary else cs.surfaceContainer
        val heroFg = if (isEnabled) cs.onPrimary else cs.onSurface
        val heroSub = if (isEnabled) cs.onPrimary.copy(alpha = 0.78f) else cs.onSurfaceVariant

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 44.dp, topEnd = 14.dp, bottomEnd = 44.dp, bottomStart = 14.dp))
                .background(heroBg)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(if (isEnabled) cs.onPrimary else cs.primary)
                        .bounceClick { viewModel.toggleEnable(!isEnabled) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Audio Reactive LED",
                        tint = if (isEnabled) cs.primary else cs.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Audio Reactive LED",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 24.sp,
                        color = heroFg
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isEnabled) "Audio reactive mode is active" else "Audio reactive mode is turned off",
                        fontSize = 13.sp,
                        color = heroSub
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 24-bar visualizer
            val bars = remember {
                listOf(
                    0.30f, 0.67f, 0.34f, 0.71f, 0.38f, 0.75f, 0.42f, 0.79f,
                    0.46f, 0.83f, 0.50f, 0.87f, 0.54f, 0.91f, 0.58f, 0.95f,
                    0.62f, 0.99f, 0.66f, 0.33f, 0.70f, 0.37f, 0.74f, 0.41f
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(106.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { frac ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(if (isEnabled) frac else 0.16f)
                            .clip(RoundedCornerShape(5.dp))
                            .background(heroFg.copy(alpha = if (isEnabled) 0.82f else 0.25f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grouped Options List
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlobIconBadge(
                    blob = ExpressiveBlob.CLOVER,
                    containerColor = cs.tertiaryContainer,
                    icon = Icons.Default.GraphicEq,
                    iconTint = cs.onTertiaryContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDynamic) "Dynamic RGB mode" else "Static white mode",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = if (isDynamic) "Reactive spectrum RGB light show" else "Steady ambient white illumination",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                ExpressivePillSwitch(
                    checked = isDynamic,
                    enabled = isEnabled || stockBehavior.autoMusicSync,
                    onCheckedChange = { viewModel.toggleMode(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BlobIconBadge(
                    blob = ExpressiveBlob.WAVE_BLOB,
                    containerColor = cs.secondaryContainer,
                    icon = Icons.Default.MusicNote,
                    iconTint = cs.onSecondaryContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-trigger on playback",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Stock behavior: start automatically with media",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                ExpressivePillSwitch(
                    checked = stockBehavior.autoMusicSync,
                    onCheckedChange = { viewModel.toggleAutoMusicSync(it) }
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
                    blob = ExpressiveBlob.TILTED_PEBBLE,
                    containerColor = cs.primaryContainer,
                    icon = Icons.Default.VolumeUp,
                    iconTint = cs.onPrimaryContainer,
                    size = 48.dp,
                    iconSize = 22.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Follow media volume gain",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Use stock inverse volume curve instead of slider",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }
                ExpressivePillSwitch(
                    checked = stockBehavior.autoVolumeGain,
                    onCheckedChange = { viewModel.toggleAutoVolumeGain(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Reactivity Slider Card
        val sliderActive = (isEnabled || stockBehavior.autoMusicSync) && !stockBehavior.autoVolumeGain
        val displayPct = ((sliderPosition.roundToInt() - AudioGain.MIN_LEVEL) * 100f /
            (AudioGain.MAX_LEVEL - AudioGain.MIN_LEVEL)).roundToInt().coerceIn(0, 100)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (sliderActive) 1f else 0.55f)
                .clip(RoundedCornerShape(32.dp))
                .background(cs.surfaceContainer)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (stockBehavior.autoVolumeGain) "Reactivity (auto volume)" else "Reactivity",
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = if (stockBehavior.autoVolumeGain) "Auto" else "$displayPct% (Lv ${sliderPosition.roundToInt()})",
                    fontWeight = FontWeight.Bold,
                    color = cs.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            ExpressiveTrackSlider(
                value = sliderPosition,
                valueRange = AudioGain.MIN_LEVEL.toFloat()..AudioGain.MAX_LEVEL.toFloat(),
                onValueChange = { sliderPosition = it.roundToInt().toFloat() },
                onValueChangeFinished = { viewModel.setGain(sliderPosition.roundToInt()) },
                activeColor = cs.primary,
                trackColor = cs.secondaryContainer,
                enabled = sliderActive
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Calm", fontSize = 12.sp, color = cs.onSurfaceVariant)
                Text(text = "Sensitive", fontSize = 12.sp, color = cs.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Outlined Info Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 36.dp))
                .background(cs.surfaceContainerLow)
                .border(
                    width = 2.dp,
                    color = cs.surfaceContainerHighest,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 36.dp, bottomStart = 36.dp)
                )
                .padding(20.dp)
        ) {
            Text(
                text = "Hardware integration & behavior",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = cs.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Dynamic RGB follows the audio spectrum. Turning it off switches to static white ambient light. Auto-trigger and volume gain mirror stock LightBelt behavior while keeping manual override via the Quick Settings tile or power toggle.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = cs.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(64.dp))
    }
}
