package com.xiiann.ledsync.presentation.notif

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiiann.ledsync.data.source.AppInfoModel
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header (Clean title & back button, no top refresh button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "App LED Sync",
                        style = MaterialTheme.typography.headlineSmall,
                        color = cs.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search installed apps…", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cs.surfaceContainerHigh,
                        unfocusedContainerColor = cs.surfaceContainerHigh,
                        focusedBorderColor = cs.primary,
                        unfocusedBorderColor = cs.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // System App Toggle Card
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cs.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.SettingsSuggest, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "System App Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSurface)
                            Text(text = "Include system apps in LED effects", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Switch(
                            checked = showSystem,
                            onCheckedChange = { viewModel.toggleSystemApps(it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "INSTALLED APPS", style = MaterialTheme.typography.labelSmall, color = cs.outline, letterSpacing = 1.1.sp)
                    Text(text = "${filteredApps.size} apps", style = MaterialTheme.typography.labelSmall, color = cs.outline)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Swipe-down PullToRefresh Area
                PullToRefreshBox(
                    isRefreshing = isLoading && appList.isNotEmpty(),
                    onRefresh = { viewModel.reloadApps() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isLoading && appList.isEmpty()) {
                        com.xiiann.ledsync.presentation.common.ExpressiveLoadingScreen(
                            message = "Scanning installed applications…"
                        )
                    } else {
                        // App List
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 110.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                M3AppMappingCard(
                                    app = app,
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

            // Contextual FloatingToolbar for bulk actions
            AnimatedVisibility(
                visible = workingMap.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 100.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.secondaryContainer,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .bounceClick {
                            workingMap.keys.toList().forEach { pkg -> viewModel.updateMapping(pkg, null) }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
    app: AppInfoModel,
    selectedEffect: String?,
    availableEffects: List<String>,
    loopingPatterns: List<String>,
    onSelectEffect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val isLooping = selectedEffect != null && loopingPatterns.contains(selectedEffect)

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
            val iconBitmap = remember(app.iconBitmap) { app.iconBitmap?.asImageBitmap() }
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(cs.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Android, contentDescription = null, tint = cs.outline, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.outline
                )
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selectedEffect != null) cs.primaryContainer else cs.surfaceContainerHighest)
                        .bounceClick { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedEffect ?: "Default",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedEffect != null) cs.onPrimaryContainer else cs.onSurfaceVariant
                    )
                    if (isLooping) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Loop, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = cs.outline, modifier = Modifier.size(18.dp))
                }

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("None (Default)", style = MaterialTheme.typography.bodyMedium, color = cs.outline) },
                        onClick = {
                            onSelectEffect(null)
                            expanded = false
                        }
                    )
                    availableEffects.forEach { name ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
}
