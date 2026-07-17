package com.fergolde.velodrome.presentation.screen.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.fergolde.velodrome.R
import com.fergolde.velodrome.ui.theme.DmSansFontFamily
import com.fergolde.velodrome.ui.theme.SyneFontFamily
import com.fergolde.velodrome.ui.theme.VeloPalette

// ─── SCREEN ──────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.hasPendingChanges) {
        if (uiState.hasPendingChanges) showConfirmDialog = true
    }

    // ── Confirm dialog ─────────────────────────────────────────────────────
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor   = VeloPalette.Bg3,
            titleContentColor = VeloPalette.TextPrimary,
            textContentColor  = VeloPalette.TextSecondary,
            title = {
                Text(
                    stringResource(R.string.settings_apply_changes),
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                    if (uiState.pendingImageCacheMb != uiState.imageCacheSizeMb) {
                        Text(
                            stringResource(
                                R.string.settings_image_cache_change,
                                uiState.imageCacheSizeMb,
                                uiState.pendingImageCacheMb
                            )
                        )
                    }

                    if (uiState.pendingMusicCacheGb != uiState.musicCacheSizeGb) {
                        Text(
                            stringResource(
                                R.string.settings_music_cache_change,
                                uiState.musicCacheSizeGb,
                                uiState.pendingMusicCacheGb
                            )
                        )
                    }

                    Text(
                        stringResource(R.string.settings_confirm_changes_question),
                        fontSize = 12.sp,
                        color = VeloPalette.TextTertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmChanges()
                    showConfirmDialog = false
                }) {
                    Text(
                        stringResource(R.string.settings_apply),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = DmSansFontFamily
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardChanges()
                    showConfirmDialog = false
                }) {
                    Text(
                        stringResource(R.string.settings_cancel),
                        color = VeloPalette.TextSecondary,
                        fontFamily = DmSansFontFamily
                    )
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Toolbar ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VeloPalette.Bg3)
                    .border(1.dp, VeloPalette.Border, RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ── Content ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Appearance
            VeloSettingsSection(eyebrow = stringResource(R.string.settings_appearance)) {
                VeloSettingsClickableRow(
                    icon = Icons.Default.Palette,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_accent_color),
                    subtitle = uiState.accentColor,
                    trailing = {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(uiState.accentColor))
                                .border(1.dp, VeloPalette.Border, CircleShape)
                        )
                    },
                    onClick = { showColorPicker = !showColorPicker },
                )

                if (showColorPicker) {
                    ColorPickerPanel(
                        colors = viewModel.availableAccentColors,
                        selectedColor = uiState.accentColor,
                        onColorSelected = { viewModel.setAccentColor(it) },
                        onDismiss = { showColorPicker = false },
                    )
                }
            }

            // Scrobble
            VeloSettingsSection(eyebrow = "Last.fm") {
                VeloSettingsSwitchRow(
                    icon     = Icons.Default.GraphicEq,
                    iconTint = VeloPalette.LastFmRed,
                    title    = stringResource(R.string.settings_scrobble_enabled),
                    subtitle = stringResource(R.string.settings_scrobble_desc),
                    checked  = uiState.scrobbleEnabled,
                    onCheckedChange = viewModel::setScrobbleEnabled,
                )
            }

            // Cache
            VeloSettingsSection(eyebrow = stringResource(R.string.settings_cache)) {
                // Music cache
                CacheConfigRow(
                    icon = Icons.Default.LibraryMusic,
                    iconTint = Color(0xFF60A5FA),
                    title = stringResource(R.string.settings_music_cache),
                    subtitle = stringResource(R.string.settings_music_cache_desc),
                    currentSizeFormatted = uiState.currentMusicCacheSize,
                    currentValue = uiState.musicCacheSizeGb,
                    pendingValue = uiState.pendingMusicCacheGb,
                    options = listOf(2, 4, 6, 8, 10),
                    unit = "GB",
                    onValueChange = viewModel::setMusicCacheSizeGb,
                )

                HorizontalDivider(
                    color = VeloPalette.Border,
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                // Image cache
                CacheConfigRow(
                    icon = Icons.Default.Image,
                    iconTint = Color(0xFFA78BFA),
                    title = stringResource(R.string.settings_image_cache),
                    subtitle = stringResource(R.string.settings_image_cache_desc),
                    currentSizeFormatted = uiState.currentImageCacheSize,
                    currentValue = uiState.imageCacheSizeMb,
                    pendingValue = uiState.pendingImageCacheMb,
                    options = listOf(200, 400, 600, 800, 1000),
                    unit = "MB",
                    onValueChange = viewModel::setImageCacheSizeMb,
                )

                Spacer(Modifier.height(4.dp))

                // Clear button

                if (showClearCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCacheDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = VeloPalette.Destructive,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.settings_clear_cache_dialog_title),
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.settings_clear_cache_dialog_message),
                                fontFamily = DmSansFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showClearCacheDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                    brush = SolidColor(VeloPalette.Border),
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_cancel),
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        confirmButton = {
                            OutlinedButton(
                                onClick = {
                                    showClearCacheDialog = false
                                    viewModel.clearAllCaches()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = VeloPalette.Destructive,
                                ),
                                border = ButtonDefaults.outlinedButtonBorder(true).copy(
                                    brush = SolidColor(VeloPalette.Destructive.copy(alpha = .5f)),
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_clear_cache),
                                    fontFamily = DmSansFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                            }
                        },
                    )
                }
                OutlinedButton(
                    onClick = { showClearCacheDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    enabled = !uiState.isClearingCache,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VeloPalette.Destructive,
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = SolidColor(VeloPalette.Destructive.copy(alpha = .5f)),
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (uiState.isClearingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = VeloPalette.Destructive,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.DeleteSweep, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.settings_clear_cache),
                        fontFamily = DmSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }

            // About
            VeloSettingsSection(eyebrow = stringResource(R.string.settings_about)) {
                VeloSettingsInfoRow(
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.settings_version),
                    subtitle = stringResource(R.string.settings_version_desc),
                    value = uiState.appVersion,
                )
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

// ─── SECTION WRAPPER ─────────────────────────────────────────────────────────

@Composable
fun VeloSettingsSection(
    eyebrow: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        Text(
            text = eyebrow.uppercase(),
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = VeloPalette.Bg3,
            border = ButtonDefaults.outlinedButtonBorder(true).copy(
                brush = SolidColor(VeloPalette.Border),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                content = content,
            )
        }
    }
}

// ─── ROW TYPES ───────────────────────────────────────────────────────────────

@Composable
private fun RowIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = .12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun VeloSettingsClickableRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(Icons.Default.ChevronRight, null, tint = VeloPalette.TextTertiary, modifier = Modifier.size(18.dp))
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowIcon(icon, iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = VeloPalette.TextPrimary)
            Text(subtitle, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = VeloPalette.TextSecondary)
        }
        trailing()
    }
}

@Composable
fun VeloSettingsSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowIcon(icon, iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = VeloPalette.TextPrimary)
            Text(subtitle, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = VeloPalette.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = VeloPalette.TextSecondary,
                uncheckedTrackColor = VeloPalette.Bg4,
            ),
        )
    }
}

@Composable
fun VeloSettingsInfoRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    value: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowIcon(icon, iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = VeloPalette.TextPrimary)
            Text(subtitle, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = VeloPalette.TextSecondary)
        }
        Text(value, fontFamily = DmSansFontFamily, fontSize = 13.sp, color = VeloPalette.TextTertiary)
    }
}

// ─── CACHE CONFIG ROW ────────────────────────────────────────────────────────

@Composable
fun CacheConfigRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    currentSizeFormatted: String,
    currentValue: Int,
    pendingValue: Int,
    options: List<Int>,
    unit: String,
    onValueChange: (Int) -> Unit,
) {
    var selected by remember { mutableIntStateOf(pendingValue) }
    LaunchedEffect(pendingValue) { selected = pendingValue }

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RowIcon(icon, iconTint)
            Column(Modifier.weight(1f)) {
                Text(title, fontFamily = DmSansFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = VeloPalette.TextPrimary)
                Text(subtitle, fontFamily = DmSansFontFamily, fontSize = 11.sp, color = VeloPalette.TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$selected $unit",
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "usado: $currentSizeFormatted",
                    fontFamily = DmSansFontFamily,
                    fontSize = 10.sp,
                    color = VeloPalette.TextTertiary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { opt ->
                val isSelected = opt == selected
                val isCurrent  = opt == currentValue
                val bgColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        else       -> VeloPalette.Bg4
                    },
                    animationSpec = tween(180),
                    label = "chipBg",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .then(
                            if (isCurrent && !isSelected)
                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .5f), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                        .clickable { selected = opt; onValueChange(opt) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$opt",
                        fontFamily = DmSansFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.onPrimary
                            isCurrent  -> MaterialTheme.colorScheme.primary
                            else       -> VeloPalette.TextSecondary
                        },
                    )
                }
            }
        }
    }
}

// ─── COLOR PICKER PANEL ──────────────────────────────────────────────────────
// Rendered inline inside the settings card (no bottom sheet needed here)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerPanel(
    colors: List<AccentColorOption>,
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customHex by remember { mutableStateOf(selectedColor) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VeloPalette.Bg3,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Color de Acento",
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = VeloPalette.TextPrimary,
                modifier = Modifier.padding(bottom = 20.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items(colors) { option ->
                    val isSelected = option.hexColor.equals(selectedColor, ignoreCase = true)
                    val parsedColor = parseHexColor(option.hexColor)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .then(
                                if (isSelected) Modifier.border(2.5.dp, VeloPalette.TextPrimary, CircleShape)
                                else Modifier
                            )
                            .clickable { onColorSelected(option.hexColor) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (parsedColor.luminance() > 0.4f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = VeloPalette.Border)
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Personalizado (HEX)",
                fontFamily = DmSansFontFamily,
                fontSize = 12.sp,
                color = VeloPalette.TextSecondary,
                modifier = Modifier.padding(bottom = 10.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(parseHexColor(customHex))
                        .border(1.dp, VeloPalette.Border, RoundedCornerShape(14.dp))
                )
                OutlinedTextField(
                    value = customHex,
                    onValueChange = { v ->
                        if (v.length <= 7 && v.matches(Regex("^[#a-fA-F0-9]*$"))) {
                            customHex = v
                            if (v.length == 7) onColorSelected(v.uppercase())
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor   = VeloPalette.Bg4,
                        unfocusedContainerColor = VeloPalette.Bg4,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = VeloPalette.Border,
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = DmSansFontFamily,
                        color = VeloPalette.TextPrimary,
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── UTIL ─────────────────────────────────────────────────────────────────────

fun parseHexColor(hex: String): Color = try {
    val clean = hex.removePrefix("#")
    Color("#$clean".toColorInt())
} catch (_: Exception) {
    VeloPalette.AccentDefault
}