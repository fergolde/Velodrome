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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import androidx.core.graphics.toColorInt

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E17))
            .statusBarsPadding()
    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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
                    ColorPickerBottomSheet(
                        colors = viewModel.availableAccentColors,
                        selectedColor = uiState.accentColor,
                        onColorSelected = { color ->
                            viewModel.setAccentColor(color)
                        },
                        onDismiss = { showColorPicker = false }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerBottomSheet(
    colors: List<AccentColorOption>,
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customHex by remember { mutableStateOf(selectedColor) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp) // Extra padding for navigation bar
        ) {
            Text(
                text = "Color de Acento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Grid de colores predefinidos
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(colors) { colorOption ->
                    val isSelected = colorOption.hexColor.equals(selectedColor, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(parseHexColor(colorOption.hexColor))
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorSelected(colorOption.hexColor) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (parseHexColor(colorOption.hexColor).luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(24.dp))

            // Input para color infinito/personalizado
            Text(
                text = "Personalizado (HEX)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Vista previa del color escrito
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(parseHexColor(customHex))
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                )

                OutlinedTextField(
                    value = customHex,
                    onValueChange = { newValue ->
                        // Permitir solo el hashtag y hasta 6 caracteres hex
                        if (newValue.length <= 7 && newValue.matches(Regex("^[#a-fA-F0-9]*$"))) {
                            customHex = newValue
                            // Si es un hex válido de 7 caracteres (#FFFFFF), aplicarlo en vivo
                            if (newValue.length == 7) {
                                onColorSelected(newValue.uppercase())
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.weight(1f)
                )
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
        Color("#$cleanHex".toColorInt())
    } catch (e: Exception) {
        Color(0xFFB6A0FF) // Default purple
    }
}