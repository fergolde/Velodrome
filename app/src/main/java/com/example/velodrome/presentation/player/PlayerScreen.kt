package com.example.velodrome.presentation.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.velodrome.R
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.screen.homescreen.BottomNavigationBar
import com.example.velodrome.presentation.screen.homescreen.MiniPlayer
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
    
    // Logging for debugging
    androidx.compose.runtime.LaunchedEffect(uiState.playlist, uiState.currentTrack, uiState.isPlaying) {
        Log.d("PlayerScreen", "=== PlayerScreen State ===")
        Log.d("PlayerScreen", "playlist.size: ${uiState.playlist.size}")
        Log.d("PlayerScreen", "currentTrack: ${uiState.currentTrack?.title}")
        Log.d("PlayerScreen", "isPlaying: ${uiState.isPlaying}")
        Log.d("PlayerScreen", "currentPosition: ${uiState.currentPosition}")
    }
    
    Scaffold(
        topBar = { PlayerTopAppBar(onMinimizeClick = onMinimizeClick) },
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

            // Album Art
            AlbumArtCard(
                coverUrl = uiState.currentTrack?.let { 
                    CredentialsManager.getCoverArtUrl(it.id, 600)
                }
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
                currentPosition = uiState.currentPosition,
                duration = uiState.currentTrack?.durationSec ?: 0,
                isPlaying = uiState.isPlaying,
                onPlayPauseClick = viewModel::onPlayPauseClick,
                onPreviousClick = viewModel::onPreviousClick,
                onNextClick = viewModel::onNextClick,
                onSeek = viewModel::onSeek
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Queue Button - always visible for debugging
            Log.d("PlayerScreen", "Showing NextUpButton? playlist.size=${uiState.playlist.size}")
            NextUpButton(onClick = onQueueClick)
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
fun AlbumArtCard(coverUrl: String? = null) {
    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = SurfaceDark,
        shadowElevation = 20.dp
    ) {
        if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder for Album Art Image
            Box(modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF2A2D3E), Color(0xFF171924)))
            )) {
                // Inner vinyl-style circle placeholder
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD4A373))
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun SongInfoSection(title: String, artist: String, album: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = artist,
                color = AccentPurple,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(" • ", color = TextSecondary)
            Text(
                text = album,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
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

/**
 * Format seconds to MM:SS
 */
private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
