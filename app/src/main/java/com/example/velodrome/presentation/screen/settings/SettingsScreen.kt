package com.example.velodrome.presentation.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.velodrome.R
import androidx.compose.material3.SliderDefaults

/**
 * Settings screen with cache configuration, appearance, and stream settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }

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
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
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
                // Image Cache
                CacheSliderItem(
                    title = stringResource(R.string.settings_image_cache),
                    subtitle = stringResource(R.string.settings_image_cache_desc),
                    currentValue = uiState.imageCacheSizeMb,
                    maxValue = 500,
                    currentSizeFormatted = uiState.currentImageCacheSize,
                    unit = "MB",
                    onValueChange = { viewModel.setImageCacheSizeMb(it) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Music Cache
                CacheSliderItem(
                    title = stringResource(R.string.settings_music_cache),
                    subtitle = stringResource(R.string.settings_music_cache_desc),
                    currentValue = uiState.musicCacheSizeGb,
                    maxValue = 20,
                    currentSizeFormatted = uiState.currentMusicCacheSize,
                    unit = "GB",
                    onValueChange = { viewModel.setMusicCacheSizeGb(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Clear Cache Button
                OutlinedButton(
                    onClick = { viewModel.clearAllCaches() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isClearingCache,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF5350)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
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
            color = Color(0xFFB6A0FF),
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
fun CacheSliderItem(
    title: String,
    subtitle: String,
    currentValue: Int,
    maxValue: Int,
    currentSizeFormatted: String,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currentValue $unit",
                    color = Color(0xFFB6A0FF),
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
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = currentValue.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..maxValue.toFloat(),
            steps = maxValue - 1,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFB6A0FF),
                activeTrackColor = Color(0xFFB6A0FF),
                inactiveTrackColor = Color(0xFF2A2D3A)
            )
        )
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
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFB6A0FF),
                uncheckedThumbColor = Color.White,
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