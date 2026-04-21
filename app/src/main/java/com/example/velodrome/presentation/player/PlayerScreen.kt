package com.example.velodrome.presentation.player

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.velodrome.R
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.screen.homescreen.AlbumCover
import com.example.velodrome.presentation.screen.homescreen.BottomNavigationBar
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.util.CredentialsManager

// --- Theme Tokens (Velvet Echo) ---
val PrimaryColor = Color(0xFF7C4DFF)
val BackgroundDark = Color(0xFF0C0E17)
val SurfaceDark = Color(0xFF171924)
val SurfaceContainer = Color(0xFF222532)
val TextPrimary = Color(0xFFF0F0FD)
val TextSecondary = Color(0xFFAAAAB7)
val AccentPurple = Color(0xFFB6A0FF)

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
    val currentPosition by PlayerManager.currentPosition.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    // Queue bottom sheet state - controlled by button click
    var showQueue by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { PlayerBottomNavigationBar(onHomeClick = onHomeClick, onExploreClick = onExploreClick, onSettingsClick = onSettingsClick) },
        containerColor = BackgroundDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Album Art - use coverArtId to build URL
            AlbumArtCard(
                coverArtId = uiState.currentTrack?.coverArtId
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Song Info
            SongInfoSection(
                title = uiState.currentTrack?.title ?: stringResource(R.string.player_unknown_track),
                artist = uiState.currentTrack?.artistName ?: stringResource(R.string.artists_unknown),
                album = uiState.currentTrack?.albumName ?: stringResource(R.string.albums_unknown_title)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Main Controls Panel
            PlaybackControlsPanel(
                currentPosition = (currentPosition / 1000L).toInt(),
                duration = uiState.currentTrack?.durationSec ?: 0,
                isPlaying = uiState.isPlaying,
                onPlayPauseClick = viewModel::onPlayPauseClick,
                onPreviousClick = viewModel::onPreviousClick,
                onNextClick = viewModel::onNextClick,
                onSeek = viewModel::onSeek
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Queue Button
            NextUpButton(onClick = { showQueue = true })
        }
    }

    // Queue Bottom Sheet
    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = SurfaceDark
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

@Composable
fun PlayerTopAppBar(onMinimizeClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown, 
            contentDescription = stringResource(R.string.player_minimize), 
            tint = TextPrimary,
            modifier = Modifier.clickable(onClick = onMinimizeClick)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.app_name), color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(stringResource(R.string.player_now_playing), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.player_menu), tint = TextPrimary)
    }
}

@Composable
fun AlbumArtCard(coverArtId: String? = null) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = SurfaceDark,
        shadowElevation = 20.dp
    ) {
        AlbumCover(
            coverArtId = coverArtId,
            contentDescription = null,
            size = 300.dp,
            cornerRadius = 32.dp,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun SongInfoSection(title: String, artist: String, album: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title with marquee effect (scrolling text)
        MarqueeText(
            text = title,
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            MarqueeText(
                text = artist,
                color = AccentPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(" • ", color = TextSecondary)
            Text(
                text = album,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Text that scrolls horizontally when it overflows the container.
 * Scrolls from right to left in an infinite loop.
 */
@Composable
fun MarqueeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    var textWidth by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var shouldScroll by remember { mutableStateOf(false) }

    // Infinite scroll animation
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val targetOffset = if (shouldScroll) -(textWidth - containerWidth) else 0f
    val scrollDuration = if (shouldScroll) ((textWidth - containerWidth) / 50 * 1000).toInt().coerceAtLeast(3000) else 3000
    val scrollOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = scrollDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scroll"
    )

    // Delay before starting scroll
    var startScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(text) {
        // Wait for layout to measure
        kotlinx.coroutines.delay(500)
        startScrolling = true
    }

    LaunchedEffect(textWidth, containerWidth) {
        shouldScroll = containerWidth > 0 && textWidth > containerWidth
    }

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                containerWidth = size.width.toFloat()
            }
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            onTextLayout = { textLayoutResult ->
                textWidth = textLayoutResult.size.width.toFloat()
            },
            modifier = Modifier
                .graphicsLayer {
                    translationX = if (shouldScroll && startScrolling) scrollOffset else 0f
                }
        )
    }
}

@Composable
fun PlaybackControlsPanel(
    currentPosition: Int = 0,
    duration: Int = 0,
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSeek: (Int) -> Unit = {}
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        color = SurfaceDark.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar Area
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition), color = TextSecondary, fontSize = 12.sp)
                    Text(formatTime(duration), color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentPurple,
                    trackColor = SurfaceContainer
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons Row: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Shuffle, contentDescription = stringResource(R.string.player_shuffle), tint = TextSecondary, modifier = Modifier.size(24.dp))
                }

                IconButton(onClick = onPreviousClick) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.player_previous), tint = TextPrimary, modifier = Modifier.size(32.dp))
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentPurple)
                        .clickable(onClick = onPlayPauseClick)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.play),
                        tint = BackgroundDark,
                        modifier = Modifier.size(36.dp).align(Alignment.Center)
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.player_next), tint = TextPrimary, modifier = Modifier.size(32.dp))
                }

                IconButton(onClick = { }) {
                    Icon(Icons.Outlined.Repeat, contentDescription = stringResource(R.string.player_repeat), tint = TextSecondary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun NextUpButton(onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.height(48.dp).padding(horizontal = 16.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = stringResource(R.string.player_next_up), tint = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(stringResource(R.string.player_next_up), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun PlayerBottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = SurfaceDark.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_explore)) },
            selected = false,
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
    }
}

@Composable
fun QueueContent(
    playlist: List<Track>,
    currentIndex: Int,
    onTrackClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.player_next_up),
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${playlist.size} tracks",
            color = TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            itemsIndexed(playlist) { index, track ->
                QueueTrackItem(
                    track = track,
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
    isCurrentTrack: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrentTrack) AccentPurple.copy(alpha = 0.2f) else SurfaceContainer)
        ) {
            Icon(
                if (isCurrentTrack) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrentTrack) AccentPurple else TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isCurrentTrack) AccentPurple else TextPrimary,
                fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = track.artistName,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Text(
            text = formatTime(track.durationSec),
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}

/**
 * Format seconds to MM:SS
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
