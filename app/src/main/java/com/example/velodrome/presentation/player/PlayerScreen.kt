package com.example.velodrome.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.screen.homescreen.AlbumCover
import androidx.compose.animation.core.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onMinimizeClick: () -> Unit = {},
    onDrag: (fraction: Float) -> Unit = {},       // fracción 0f–1f del drag
    onDragEnd: (completed: Boolean) -> Unit = {}, // true = supera umbral
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onQueueClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }

    // ── Swipe to dismiss ──────────────────────────────────────────────────────
    val screenHeight = LocalConfiguration.current.screenHeightDp.toFloat()
    val dismissThreshold = 0.28f   // 28% de la pantalla para disparar

    // ─────────────────────────────────────────────────────────────────────────


    var accumulatedFraction by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current.density
    Scaffold(
        bottomBar = {
            PlayerBottomNavigationBar(
                onHomeClick = onHomeClick,
                onExploreClick = onExploreClick,
                onSettingsClick = onSettingsClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // ── Gesto vertical ──────────────────────────────────────────
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            android.util.Log.d("SWIPE", "accumulatedFraction = $accumulatedFraction, threshold = $dismissThreshold")
                            val completed = accumulatedFraction >= dismissThreshold
                            onDragEnd(completed)  // ← primero notifica con el valor real
                            accumulatedFraction = 0f  // ← luego resetea
                        },
                        onDragCancel = {
                            onDragEnd(false)
                            accumulatedFraction = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0 || accumulatedFraction > 0f) {
                                change.consume()
                                val resistance = if (accumulatedFraction < 0.1f) 0.6f else 0.85f
                                accumulatedFraction = (accumulatedFraction + (dragAmount / (screenHeight * density)) * resistance)
                                    .coerceIn(0f, 1f)
                                onDrag(accumulatedFraction)
                            }
                        }
                    )
                }
        ) {
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                PlayerTopBar(onMinimizeClick = onMinimizeClick)
                Spacer(modifier = Modifier.height(20.dp))
                AlbumArt(
                    coverArtId = uiState.currentTrack?.coverArtId,
                )
                Spacer(modifier = Modifier.height(32.dp))
                SongInfoSection(
                    title = uiState.currentTrack?.title ?: stringResource(R.string.player_unknown_track),
                    artist = uiState.currentTrack?.artistName ?: stringResource(R.string.artists_unknown),
                    album = uiState.currentTrack?.albumName ?: stringResource(R.string.albums_unknown_title)
                )
                Spacer(modifier = Modifier.height(28.dp))
                SeekBar(
                    currentPosition = (currentPosition / 1000L).toInt(),
                    duration = uiState.currentTrack?.durationSec ?: 0,
                    onSeek = viewModel::onSeek
                )
                Spacer(modifier = Modifier.height(28.dp))
                PlaybackControls(
                    isPlaying = uiState.isPlaying,
                    onPlayPauseClick = viewModel::onPlayPauseClick,
                    onPreviousClick = viewModel::onPreviousClick,
                    onNextClick = viewModel::onNextClick
                )
                //Spacer(modifier = Modifier.height(28.dp))
                Spacer(modifier = Modifier.weight(1f))
                QueueChip(onClick = { showQueue = true })
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            QueueContent(
                playlist = uiState.playlist,
                currentIndex = uiState.currentIndex,
                onTrackClick = { index ->
                    viewModel.onTrackSelected(index)
                    showQueue = false
                }
            )
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun PlayerTopBar(onMinimizeClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMinimizeClick) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.player_minimize),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.player_now_playing).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        IconButton(onClick = {}) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.player_menu),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Album Art with breathing animation ───────────────────────────────────────

@Composable
fun AlbumArt(
    coverArtId: String? = null,
) {

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)),
        color = MaterialTheme.colorScheme.surface
    ) {
        AlbumCover(
            coverArtId = coverArtId,
            contentDescription = null,
            size = 300.dp,
            cornerRadius = 28.dp,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ─── Song Info ─────────────────────────────────────────────────────────────────

@Composable
fun SongInfoSection(
    title: String,
    artist: String,
    album: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MarqueeText(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = artist,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = " · ",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 15.sp
            )
            MarqueeText(
                text = album,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Seek Bar (standalone, más respirable) ────────────────────────────────────

@Composable
fun SeekBar(
    currentPosition: Int,
    duration: Int,
    onSeek: (Int) -> Unit
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider interactivo
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                onSeek((newProgress * duration).toInt())
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = MaterialTheme.colorScheme.onBackground,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(duration),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Playback Controls (sin panel, diseño abierto tipo Spotify) ───────────────

@Composable
fun PlaybackControls(
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = {}) {
            Icon(
                Icons.Outlined.Shuffle,
                contentDescription = stringResource(R.string.player_shuffle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Previous
        IconButton(
            onClick = onPreviousClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        // Play / Pause — botón principal
        PlayPauseButton(
            isPlaying = isPlaying,
            onClick = onPlayPauseClick
        )

        // Next
        IconButton(
            onClick = onNextClick,
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat
        IconButton(onClick = {}) {
            Icon(
                Icons.Outlined.Repeat,
                contentDescription = stringResource(R.string.player_repeat),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "playPauseScale"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.play),
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(34.dp)
        )
    }
}

// ─── Queue Chip ────────────────────────────────────────────────────────────────

@Composable
fun QueueChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        //shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.player_next_up).uppercase(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ─── Bottom Nav ────────────────────────────────────────────────────────────────

@Composable
fun PlayerBottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text(stringResource(R.string.nav_home), fontSize = 10.sp) },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text(stringResource(R.string.nav_explore), fontSize = 10.sp) },
            selected = false,
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(22.dp)) },
            label = { Text(stringResource(R.string.nav_settings), fontSize = 10.sp) },
            selected = false,
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )
    }
}

// ─── Queue ─────────────────────────────────────────────────────────────────────

@Composable
fun QueueContent(
    playlist: List<Track>,
    currentIndex: Int,
    onTrackClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.player_next_up).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.player_next_up),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "${playlist.size}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(playlist) { index, track ->
                QueueTrackItem(
                    track = track,
                    index = index,
                    isCurrentTrack = index == currentIndex,
                    onClick = { onTrackClick(index) }
                )
            }
        }
    }
}

@Composable
fun QueueTrackItem(
    track: Track,
    index: Int,
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isCurrentTrack)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else Color.Transparent,
        label = "trackBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Portada con overlay de "playing" si es la pista actual
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            AlbumCover(
                coverArtId = track.coverArtId,
                contentDescription = null,
                size = 52.dp,
                cornerRadius = 10.dp,
                modifier = Modifier.fillMaxSize()
            )
            // Overlay oscuro + icono solo en la pista actual
            if (isCurrentTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    // Equalizer animado — 3 barras
                    EqualizerBars()
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = if (isCurrentTrack) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artistName,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatTime(track.durationSec),
                color = if (isCurrentTrack)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Barras de ecualizador animadas para la pista en reproducción
@Composable
fun EqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eq")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(400, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            tween(300, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "b3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(18.dp)
    ) {
        listOf(bar1, bar2, bar3).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
        }
    }
}

// ─── Marquee Text ──────────────────────────────────────────────────────────────

@Composable
fun MarqueeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            repeatDelayMillis = 3000,
            initialDelayMillis = 1500,
            velocity = 40.dp,
            spacing = MarqueeSpacing(80.dp)
        )
    )
}

// ─── Utils ─────────────────────────────────────────────────────────────────────

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}