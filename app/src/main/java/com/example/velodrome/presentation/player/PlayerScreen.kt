package com.example.velodrome.presentation.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import android.content.res.Configuration
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.components.SharedBottomNavigationBar
import com.example.velodrome.presentation.screen.home.AlbumCover
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onMinimizeClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onQueueClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLandscape) {
                // ── Landscape: Row partido ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(start = 16.dp, end = 12.dp),
                ) {
                    // Left: TopBar + Album Art centrado + SongInfo + SeekBar + Controls
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.35f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlayerTopBar(
                            onMinimizeClick = onMinimizeClick,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        // Album art centrado verticalmente
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            AlbumArt(
                                coverArtId = uiState.currentTrack?.coverArtId,
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        // Controls debajo del album art
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SongInfoSection(
                                title = uiState.currentTrack?.title ?: stringResource(R.string.player_unknown_track),
                                artist = uiState.currentTrack?.artistName ?: stringResource(R.string.artists_unknown),
                                album = uiState.currentTrack?.albumName ?: stringResource(R.string.albums_unknown_title)
                            )
                            Spacer(Modifier.height(10.dp))
                            SeekBar(
                                currentPosition = uiState.currentPosition,
                                duration = uiState.currentTrack?.durationSec ?: 0,
                                onSeek = viewModel::onSeek
                            )
                            Spacer(Modifier.height(10.dp))
                            PlaybackControls(
                                isPlaying = uiState.isPlaying,
                                onPlayPauseClick = { viewModel.onPlayPauseClick() },
                                onPreviousClick = { viewModel.onPreviousClick() },
                                onNextClick = { viewModel.onNextClick() },
                                isShuffleEnabled = uiState.isShuffleEnabled,
                                isRepeatEnabled = uiState.isRepeatEnabled,
                                onShuffleClick = { viewModel.toggleShuffle() },
                                onRepeatClick = { viewModel.toggleRepeat() }
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    // Right: solo inline Queue
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.60f)
                            .padding(start = 20.dp, end = 4.dp)
                    ) {
                        QueueContent(
                            playlist = uiState.playlist,
                            currentIndex = uiState.currentIndex,
                            isPlaying = uiState.isPlaying,
                            onTrackClick = { index ->
                                viewModel.onTrackSelected(index)
                            },
                            onRemoveTrack = viewModel::onRemoveTrack
                        )
                    }
                }
            } else {
                // ── Portrait: layout actual (sin cambios) ──────────────────
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
                        .windowInsetsPadding(WindowInsets.statusBars)
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
                    Spacer(modifier = Modifier.height(14.dp))
                    SeekBar(
                        currentPosition = uiState.currentPosition,
                        duration = uiState.currentTrack?.durationSec ?: 0,
                        onSeek = viewModel::onSeek
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PlaybackControls(
                        isPlaying = uiState.isPlaying,
                        onPlayPauseClick = { viewModel.onPlayPauseClick() },
                        onPreviousClick = { viewModel.onPreviousClick() },
                        onNextClick = { viewModel.onNextClick() },
                        isShuffleEnabled = uiState.isShuffleEnabled,
                        isRepeatEnabled = uiState.isRepeatEnabled,
                        onShuffleClick = { viewModel.toggleShuffle() },
                        onRepeatClick = { viewModel.toggleRepeat() }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    QueueChip(onClick = { showQueue = true })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Barra de navegación fija en la parte inferior del player
        SharedBottomNavigationBar(
            currentRoute = "player",
            onHomeClick = onHomeClick,
            onExploreClick = onExploreClick,
            onSettingsClick = onSettingsClick
        )
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
                isPlaying = uiState.isPlaying,
                onTrackClick = { index ->
                    viewModel.onTrackSelected(index)
                    showQueue = false
                },
                onRemoveTrack = viewModel::onRemoveTrack
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
fun PlayerTopBar(
    onMinimizeClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Texto centrado REAL
        Text(
            text = stringResource(R.string.player_now_playing).uppercase(),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        // Botón alineado a la izquierda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMinimizeClick) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.player_minimize),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
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
            size = 0.dp,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MarqueeText(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
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
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
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
    onNextClick: () -> Unit = {},
    isShuffleEnabled: Boolean = false,
    isRepeatEnabled: Boolean = false,
    onShuffleClick: () -> Unit = {},
    onRepeatClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        IconButton(onClick = onShuffleClick) {
            Icon(
                Icons.Outlined.Shuffle,
                contentDescription = stringResource(R.string.player_shuffle),
                tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
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
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                modifier = Modifier.size(36.dp)
            )
        }

        // Repeat
        IconButton(onClick = onRepeatClick) {
            Icon(
                Icons.Outlined.Repeat,
                contentDescription = stringResource(R.string.player_repeat),
                tint = if (isRepeatEnabled) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            .background(MaterialTheme.colorScheme.primary)
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.player_next_up).uppercase(),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// ─── Queue ─────────────────────────────────────────────────────────────────────

@Composable
fun QueueContent(
    playlist: List<Track>,
    currentIndex: Int,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit,
    onRemoveTrack: (Int) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current song when playlist changes or currentIndex changes
    LaunchedEffect(currentIndex) {
        if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            listState.animateScrollToItem(currentIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "${playlist.size}",
                    color = MaterialTheme.colorScheme.onPrimaryFixed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(playlist, key = { _, track -> track.id }) { index, track ->
                QueueTrackItem(
                    track = track,
                    index = index,
                    isCurrentTrack = index == currentIndex,
                    isPlaying = isPlaying,
                    onClick = { onTrackClick(index) },
                    onRemove = { onRemoveTrack(index) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueTrackItem(
    track: Track,
    index: Int,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit = {},
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val threshold = with(density) { 100.dp.toPx() }

    val bgColor by animateColorAsState(
        targetValue = if (isCurrentTrack)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else Color.Transparent,
        label = "trackBg"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-500f, 500f)
                    },
                    onDragEnd = {
                        if (abs(offsetX) > threshold) {
                            onRemove()
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    }
                )
            }
    ) {
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
                        // Equalizer animado
                        EqualizerBars(isPlaying)
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
}

// Barras de ecualizador animadas para la pista en reproducción
@Composable
fun EqualizerBars(isPlaying: Boolean) {
    if(isPlaying) {
        val infiniteTransition = rememberInfiniteTransition(label = "eq")

        val bar1 by infiniteTransition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ), label = "b1"
        )
        val bar2 by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 0.4f,
            animationSpec = infiniteRepeatable(
                tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse
            ), label = "b2"
        )
        val bar3 by infiniteTransition.animateFloat(
            initialValue = 0.5f, targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse
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
    else{
        Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Marquee Text ──────────────────────────────────────────────────────────────

@Composable
fun MarqueeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: androidx.compose.ui.text.style.TextAlign = androidx.compose.ui.text.style.TextAlign.Start,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
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