package com.example.velodrome.presentation.screen.homescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.velodrome.R
import com.example.velodrome.presentation.UiConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.player.PlayerManager.currentTrack
import com.example.velodrome.ui.theme.VelodromeTheme
import com.example.velodrome.util.CredentialsManager

// --- Theme Tokens (Velvet Echo) ---
val PrimaryColor = Color(0xFF7C4DFF)
val BackgroundDark = Color(0xFF0C0E17)
val SurfaceDark = Color(0xFF171924)
val SurfaceContainer = Color(0xFF222532)
val TextPrimary = Color(0xFFF0F0FD)
val TextSecondary = Color(0xFFAAAAB7)
val AccentPurple = Color(0xFFB6A0FF)
val BackgroundDark2 = Color(0xFF0C0E17)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
    onExploreClick: () -> Unit = {},
    onPlayerClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar() },
        bottomBar = { BottomNavigationBar(onExploreClick = onExploreClick) },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ShuffleButton(onShuffle = { viewModel.playShuffle() })
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = stringResource(R.string.home_recently_added), subtitle = stringResource(R.string.home_new_arrivals))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.latestAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = stringResource(R.string.home_most_played), subtitle = stringResource(R.string.home_your_favorites))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.topAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = stringResource(R.string.home_recently_played), subtitle = stringResource(R.string.home_just_for_you))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.recentlyPlayedAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = stringResource(R.string.home_random), subtitle = stringResource(R.string.home_discover))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.randomAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(100.dp)) // Padding for mini player
            }
        }

        // Mini Player - only show if there's a track to play
        val currentTrack by PlayerManager.currentTrack.collectAsState()
        val currentPosition by PlayerManager.currentPosition.collectAsState()
        val isPlaying by PlayerManager.isPlaying.collectAsState()
        val playlist by PlayerManager.playlist.collectAsState()
        
        if (currentTrack != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                MiniPlayer(
                    modifier = Modifier.padding(bottom = UiConstants.MiniPlayerBottomMarginInScaffold),
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    onPlayPauseClick = { PlayerManager.togglePlayPause() },
                    onClick = onPlayerClick,
                    onNextClick = { PlayerManager.next() },
                    onPreviousClick = { PlayerManager.previous() }
                )
            }
        }
    }

    // Loading state
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentPurple)
        }
    }

    // Error state
    state.error?.let { error ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = error,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.retry() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text(stringResource(R.string.home_retry), color = BackgroundDark)
                }
            }
        }
    }
}

@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sonic Gallery", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextSecondary, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ShuffleButton(
    onShuffle: () -> Unit
) {
    Button(
        onClick = onShuffle,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.home_shuffle), color = BackgroundDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    onViewAllClick: (() -> Unit)? = null,
    showActionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column {
        Text(subtitle, color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            val actionText = showActionText ?: if (onViewAllClick != null) stringResource(R.string.view_all) else null
            if (actionText != null) {
                Text(
                    text = actionText,
                    color = AccentPurple,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = { onActionClick?.invoke() ?: onViewAllClick?.invoke() })
                )
            }
        }
    }
}

@Composable
fun RecentAlbumsRow(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums) { album ->
            AlbumItem(
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}

@Composable
fun FeaturedAlbumCard(
    album: Album?,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
    ) {
        // Placeholder for background image
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))
        ))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                album?.title ?: "No album available",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                album?.artistName ?: "",
                color = AccentPurple,
                fontSize = 14.sp
            )
            Text(
                album?.year?.toString() ?: "",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AccentPurple)
                .clickable { onPlayClick() }
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun AlbumGridSection(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        albums.take(2).forEach { album ->
            AlbumGridItem(
                album = album,
                onClick = { onAlbumClick(album.id) }
            )
        }
    }
}

@Composable
fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(album.genre ?: "", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier,
    currentTrack: Track?,
    isPlaying: Boolean,
    currentPosition: Int = 0,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {}
) {
    // Only recompute coverUrl when track changes
    val coverUrl = remember(currentTrack) {
        currentTrack?.coverArtId?.let { CredentialsManager.getCoverArtUrl(it, 200) }
    }

    val progress = if (currentTrack != null && currentTrack.durationSec > 0) {
        currentPosition.toFloat() / currentTrack.durationSec.toFloat()
    } else {
        0f
    }
    
    Column(modifier = modifier) {
        // Progress bar at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(SurfaceContainer)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(AccentPurple)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark.copy(alpha = 0.95f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceContainer)
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentTrack?.title ?: stringResource(R.string.player_unknown_track),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = currentTrack?.artistName ?: stringResource(R.string.artists_unknown),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.player_previous),
                tint = TextPrimary,
                modifier = Modifier.clickable { onPreviousClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentPurple)
                    .clickable { onPlayPauseClick() }
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = BackgroundDark,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.player_next),
                tint = TextPrimary,
                modifier = Modifier.clickable { onNextClick() }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(onExploreClick: () -> Unit = {}) {
    NavigationBar(
        containerColor = SurfaceDark.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, unselectedIconColor = TextSecondary)
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
            onClick = { },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    VelodromeTheme {
        // Preview without ViewModel - state will be empty/default
        HomeScreen(
            viewModel = hiltViewModel(),
            onAlbumClick = {}
        )
    }
}
