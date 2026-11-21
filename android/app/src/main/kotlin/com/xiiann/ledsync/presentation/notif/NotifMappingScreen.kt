package com.xiiann.ledsync.presentation.notif

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.data.source.AppInfoModel
import com.xiiann.ledsync.presentation.common.BlobIconBadge
import com.xiiann.ledsync.presentation.common.BlobLetterBadge
import com.xiiann.ledsync.presentation.common.ExpressiveBlob
import com.xiiann.ledsync.presentation.common.ExpressiveLoadingScreen
import com.xiiann.ledsync.presentation.common.ExpressivePillSwitch
import com.xiiann.ledsync.presentation.theme.LocalExtraColors
import com.xiiann.ledsync.presentation.theme.bounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotifMappingScreen(
    viewModel: NotifMappingViewModel,
    onBack: () -> Unit
) {
    val appList by viewModel.appList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showSystem by viewModel.showSystemApps.collectAsState(initial = false)
    val workingMap by viewModel.workingMap.collectAsState()
    val isDirty by viewModel.isDirty.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val deviceConfig = remember { viewModel.getDeviceConfig() }

    val cs = MaterialTheme.colorScheme

    val filteredApps = remember(appList, searchQuery) {
        if (searchQuery.isBlank()) {
            appList
        } else {
            appList.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = cs.surface,
        floatingActionButton = {
            if (isDirty) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.saveMappings() },
                    icon = { Icon(imageVector = Icons.Default.Save, contentDescription = "Save") },
                    text = { Text("Save Changes", fontWeight = FontWeight.Bold) },
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.bounceClick()
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
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
                        text = "App LED Sync",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                        color = cs.onSurface
                    )
                }

                // Pill Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.surfaceContainer)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search installed apps…",
                                color = cs.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = TextStyle(color = cs.onSurface, fontSize = 15.sp),
                            cursorBrush = SolidColor(cs.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = cs.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // System App Sync Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(cs.surfaceContainer)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BlobIconBadge(
                        blob = ExpressiveBlob.SUNBURST,
                        containerColor = cs.secondaryContainer,
                        icon = Icons.Default.SettingsSuggest,
                        iconTint = cs.onSecondaryContainer,
                        size = 52.dp,
                        iconSize = 23.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System App Sync",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                        Text(
                            text = "Include system apps in LED effects",
                            fontSize = 13.sp,
                            color = cs.onSurfaceVariant
                        )
                    }
                    ExpressivePillSwitch(
                        checked = showSystem,
                        onCheckedChange = { viewModel.toggleSystemApps(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Installed apps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurfaceVariant
                    )
                    Text(
                        text = "${filteredApps.size} apps",
                        fontSize = 13.sp,
                        color = cs.onSurfaceVariant
                    )
                }

                PullToRefreshBox(
                    isRefreshing = isLoading && appList.isNotEmpty(),
                    onRefresh = { viewModel.reloadApps() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading && appList.isEmpty()) {
                        ExpressiveLoadingScreen(message = "Scanning installed applications…")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                                val itemShape = when {
                                    filteredApps.size == 1 -> RoundedCornerShape(32.dp)
                                    index == 0 -> RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
                                    index == filteredApps.lastIndex -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 32.dp, bottomEnd = 32.dp)
                                    else -> RoundedCornerShape(12.dp)
                                }
                                M3AppMappingCard(
                                    index = index,
                                    app = app,
                                    shape = itemShape,
                                    selectedEffect = workingMap[app.packageName],
                                    availableEffects = deviceConfig.ledEffects.keys.toList(),
                                    loopingPatterns = deviceConfig.loopingPatterns.toList(),
                                    onSelectEffect = { effect -> viewModel.updateMapping(app.packageName, effect) }
                                )
                            }
                        }
                    }
                }
            }

            // Clear All Floating Pill
            AnimatedVisibility(
                visible = workingMap.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 18.dp, bottom = 24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.secondaryContainer,
                    tonalElevation = 6.dp,
                    modifier = Modifier.bounceClick {
                        workingMap.keys.toList().forEach { pkg -> viewModel.updateMapping(pkg, null) }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Clear All (${workingMap.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun M3AppMappingCard(
    index: Int,
    app: AppInfoModel,
    shape: RoundedCornerShape,
    selectedEffect: String?,
    availableEffects: List<String>,
    loopingPatterns: List<String>,
    onSelectEffect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    var expanded by remember { mutableStateOf(false) }
    val isLooping = selectedEffect != null && loopingPatterns.contains(selectedEffect)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cs.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBitmap = remember(app.iconBitmap) { app.iconBitmap?.asImageBitmap() }
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            val blobs = ExpressiveBlob.entries
            val blob = blobs[index % blobs.size]
            val (bg, fg) = when (index % 5) {
                0 -> cs.primaryContainer to cs.onPrimaryContainer
                1 -> cs.tertiaryContainer to cs.onTertiaryContainer
                2 -> cs.secondaryContainer to cs.onSecondaryContainer
                3 -> extra.accentContainer to extra.onAccentContainer
                else -> cs.surfaceContainerHighest to cs.onSurface
            }
            BlobLetterBadge(
                blob = blob,
                containerColor = bg,
                letter = app.label.firstOrNull()?.uppercase() ?: "A",
                letterColor = fg,
                size = 52.dp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1
            )
            Text(
                text = app.packageName,
                fontSize = 13.sp,
                color = cs.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedEffect != null) cs.primaryContainer else cs.surfaceContainerHighest)
                    .bounceClick { expanded = true }
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedEffect ?: "Default",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedEffect != null) cs.onPrimaryContainer else cs.onSurface
                )
                if (isLooping) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.Loop, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("None (Default)", color = cs.outline) },
                    onClick = {
                        onSelectEffect(null)
                        expanded = false
                    }
                )
                availableEffects.forEach { name ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(name, fontWeight = FontWeight.Medium)
                                if (loopingPatterns.contains(name)) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.Default.Loop, contentDescription = null, tint = cs.outline, modifier = Modifier.size(12.dp))
                                }
                            }
                        },
                        onClick = {
                            onSelectEffect(name)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
