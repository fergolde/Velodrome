package com.example.velodrome.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R

/**
 * Settings screen with cache configuration, appearance, and stream settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},

) {
    val uiState by viewModel.uiState.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Show dialog when pending changes detected
    LaunchedEffect(uiState.hasPendingChanges) {
        if (uiState.hasPendingChanges) {
            showConfirmDialog = true
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "Confirm Changes",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    if (uiState.pendingImageCacheMb != uiState.imageCacheSizeMb) {
                        Text("Image cache: ${uiState.imageCacheSizeMb} MB → ${uiState.pendingImageCacheMb} MB")
                    }
                    if (uiState.pendingMusicCacheGb != uiState.musicCacheSizeGb) {
                        Text("Music cache: ${uiState.musicCacheSizeGb} GB → ${uiState.pendingMusicCacheGb} GB")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Apply these changes?",
                        color = Color(0xFFAAAAB7),
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmChanges()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Apply", color = Color(0xFFB6A0FF))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.discardChanges()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Cancel", color = Color(0xFFAAAAB7))
                }
            },
            containerColor = Color(0xFF1A1D26),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = { SettingsBottomNavigationBar(onHomeClick=onHomeClick,onExploreClick = onExploreClick) },
        containerColor = Color(0xFF0C0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Cache Section ---
            SettingsSection(title = stringResource(R.string.settings_cache)) {
                // Music Cache with Chips
                CacheChipsItem(
                    title = stringResource(R.string.settings_music_cache),
                    subtitle = stringResource(R.string.settings_music_cache_desc),
                    currentValue = uiState.musicCacheSizeGb,
                    pendingValue = uiState.pendingMusicCacheGb,
                    options = listOf(2, 4, 6, 8, 10),
                    unit = "GB",
                    currentSizeFormatted = uiState.currentMusicCacheSize,
                    onValueChange = { viewModel.setMusicCacheSizeGb(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image Cache with Chips
                CacheChipsItem(
                    title = stringResource(R.string.settings_image_cache),
                    subtitle = stringResource(R.string.settings_image_cache_desc),
                    currentValue = uiState.imageCacheSizeMb,
                    pendingValue = uiState.pendingImageCacheMb,
                    options = listOf(200, 400, 600, 800, 1000),
                    unit = "MB",
                    currentSizeFormatted = uiState.currentImageCacheSize,
                    onValueChange = { viewModel.setImageCacheSizeMb(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Clear Cache Button
                OutlinedButton(
                    onClick = { viewModel.clearAllCaches() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isClearingCache,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF5350)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = !uiState.isClearingCache).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFEF5350))
                    )
                ) {
                    if (uiState.isClearingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFEF5350),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_clear_cache))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Appearance Section ---
            SettingsSection(title = stringResource(R.string.settings_appearance)) {
                // Accent Color
                SettingsClickableItem(
                    title = stringResource(R.string.settings_accent_color),
                    subtitle = uiState.accentColor,
                    onClick = { showColorPicker = !showColorPicker },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(uiState.accentColor))
                        )
                    }
                )

                if (showColorPicker) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ColorPickerGrid(
                        colors = viewModel.availableAccentColors,
                        selectedColor = uiState.accentColor,
                        onColorSelected = { color ->
                            viewModel.setAccentColor(color)
                            showColorPicker = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Scrobble Section ---
            SettingsSection(title = stringResource(R.string.settings_scrobble)) {
                SettingsSwitchItem(
                    title = stringResource(R.string.settings_scrobble_enabled),
                    subtitle = stringResource(R.string.settings_scrobble_desc),
                    checked = uiState.scrobbleEnabled,
                    onCheckedChange = { viewModel.setScrobbleEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- About Section ---
            SettingsSection(title = stringResource(R.string.settings_about)) {
                SettingsInfoItem(
                    title = stringResource(R.string.settings_version),
                    subtitle = stringResource(R.string.settings_version_desc),
                    value = "1.0.0"
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // Space for mini player
        }
    }
}

// --- Reusable Components ---

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF171924)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun CacheChipsItem(
    title: String,
    subtitle: String,
    currentValue: Int,
    pendingValue: Int,
    options: List<Int>,
    unit: String,
    currentSizeFormatted: String,
    onValueChange: (Int) -> Unit
) {
    var selectedValue by remember { mutableStateOf(pendingValue) }

    LaunchedEffect(pendingValue) {
        selectedValue = pendingValue
    }

    Column {
        // Header row with title and value side by side
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFAAAAB7),
                    fontSize = 12.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$selectedValue $unit",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_current) + ": $currentSizeFormatted",
                    color = Color(0xFFAAAAB7),
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Chips row with equal spacing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { option ->
                val isSelected = option == selectedValue
                val isCurrent = option == currentValue

                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(if (options.size <= 5) 56.dp else 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isCurrent -> Color(0xFF2A2D3A)
                                else -> Color(0xFF1A1D26)
                            }
                        )
                        .border(
                            width = if (isCurrent && !isSelected) 1.dp else 0.dp,
                            color = if (isCurrent && !isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onValueChange(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$option",
                        color = when {
                            isSelected -> Color.Black
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFAAAAB7)
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = Color(0xFFAAAAB7),
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFAAAAB7)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFFAAAAB7),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                uncheckedThumbColor = Color(0xFFAAAAB7),
                uncheckedTrackColor = Color(0xFF2A2D3A)
            )
        )
    }
}

@Composable
fun SettingsInfoItem(
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFFAAAAB7),
                fontSize = 12.sp
            )
        }
        Text(
            text = value,
            color = Color(0xFFAAAAB7),
            fontSize = 14.sp
        )
    }
}

@Composable
fun ColorPickerGrid(
    colors: List<AccentColorOption>,
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { colorOption ->
            val isSelected = colorOption.hexColor.equals(selectedColor, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(colorOption.hexColor))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color.White, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelected(colorOption.hexColor) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parse hex color string to Compose Color.
 */
fun parseHexColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$cleanHex"))
    } catch (e: Exception) {
        Color(0xFFB6A0FF) // Default purple
    }
}

@Composable
fun SettingsBottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_explore)) },
            selected = false,
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}