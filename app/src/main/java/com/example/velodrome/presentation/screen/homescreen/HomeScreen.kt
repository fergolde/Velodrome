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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.velodrome.domain.model.Album
import com.example.velodrome.ui.theme.VelodromeTheme

// --- Theme Tokens (Velvet Echo) ---
val PrimaryColor = Color(0xFF7C4DFF)
val BackgroundDark = Color(0xFF0C0E17)
val SurfaceDark = Color(0xFF171924)
val TextPrimary = Color(0xFFF0F0FD)
val TextSecondary = Color(0xFFAAAAB7)
val AccentPurple = Color(0xFFB6A0FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
    onExploreClick: () -> Unit = {}
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
                SectionHeader(title = "Recently Added", subtitle = "NEW ARRIVALS")
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.latestAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = "Most Played", subtitle = "YOUR FAVORITES")
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.topAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = "Recently Played", subtitle = "JUST FOR YOU")
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.recentlyPlayedAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = "Random", subtitle = "DISCOVER")
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.randomAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(100.dp)) // Padding for mini player
            }
        }

        // Mini Player Positioned at the bottom
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            MiniPlayer(
                modifier = Modifier.padding(bottom = 88.dp),
                currentTrackId = state.currentTrackId,
                isPlaying = state.isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() }
            )
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
                    Text("Retry", color = BackgroundDark)
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
        Text("Aleatorio", color = BackgroundDark, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(subtitle, color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Text("View All", color = AccentPurple, fontSize = 14.sp)
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
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark.copy(alpha = 0.95f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Now Playing", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(currentTrackId ?: "", color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = TextPrimary)
        Spacer(modifier = Modifier.width(16.dp))
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
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.SkipNext, contentDescription = null, tint = TextPrimary)
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
            label = { Text("HOME") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text("EXPLORE") },
            selected = false,
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
            label = { Text("PLAYER") },
            selected = false,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("SETTINGS") },
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
