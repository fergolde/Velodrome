package com.example.velodrome.presentation.screen.explore

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.presentation.screen.homescreen.AlbumCover
import com.example.velodrome.presentation.screen.homescreen.RecentAlbumsRow
import com.example.velodrome.presentation.screen.homescreen.MiniPlayer
import com.example.velodrome.presentation.screen.homescreen.SectionHeader
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
fun NavidromeExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = { ExploreTopAppBar() },
        bottomBar = { ExploreBottomNavigationBar() },
        containerColor = BackgroundDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange
                )
                Spacer(modifier = Modifier.height(24.dp))
                FilterChips(
                    selected = uiState.selectedFilter,
                    onFilterSelected = viewModel::onFilterSelected
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Random Artists Carousel
            item {
                SectionHeader(title = "Artists", subtitle = "DISCOVER")
                Spacer(modifier = Modifier.height(16.dp))
                RandomArtistsRow(
                    artists = uiState.randomArtists,
                    onArtistClick = viewModel::onArtistClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = "Curated Albums", subtitle = "BASED ON YOUR NOCTURNAL ACTIVITY")
                Spacer(modifier = Modifier.height(16.dp))
                CuratedAlbumsRow(
                    albums = uiState.curatedAlbums,
                    onAlbumClick = viewModel::onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Random Albums Carousel
            item {
                SectionHeader(title = "Random Albums", subtitle = "EXPLORE")
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = uiState.randomAlbums,
                    onAlbumClick = { albumId -> uiState.randomAlbums.find { it.id == albumId }?.let { viewModel.onAlbumClick(it) } }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                SectionHeader(title = "Rising Artists", subtitle = "TRENDING IN YOUR LIBRARY")
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(uiState.randomArtists.take(5)) { artist ->
                ArtistListRow(artist = artist, onArtistClick = viewModel::onArtistClick)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Padding for mini player
            }
        }

        // Mini Player
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            ExploreMiniPlayer(modifier = Modifier.padding(bottom = 88.dp))
        }
    }
}

@Composable
fun ExploreTopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
        Text("Sonic Gallery", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = TextSecondary, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceDark),
        placeholder = { Text("Artists, albums, or tracks", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = TextSecondary) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun FilterChips(
    selected: String,
    onFilterSelected: (String) -> Unit
) {
    val filters = listOf("Explore", "Artists", "Albums", "Genres")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        filters.forEach { text ->
            val isSelected = text == selected
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onFilterSelected(text) },
                color = if (isSelected) AccentPurple else SurfaceDark
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = if (isSelected) BackgroundDark else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun FeaturedArtistBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceDark)
    ) {
        // Image Placeholder
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
        ))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Surface(
                color = SurfaceContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "ARTIST OF THE MONTH",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    color = AccentPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("The Midnight Session", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("4.2M Monthly Listeners", color = TextSecondary, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(AccentPurple)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(32.dp).align(Alignment.Center))
        }
    }
}

@Composable
fun ExploreSectionHeader(title: String, subtitle: String) {
    Column {
        Text(subtitle, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("View all", color = AccentPurple, fontSize = 14.sp)
        }
    }
}

@Composable
fun RandomArtistsRow(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit
) {
    if (artists.isEmpty()) {
        // Show placeholder when loading
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(5) { index ->
                ArtistPlaceholder(modifier = Modifier.width(80.dp))
            }
        }
        return
    }
    
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(artists) { artist ->
            ArtistCircleCard(
                artist = artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

@Composable
private fun ArtistPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceContainer)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(40.dp).height(12.dp).background(SurfaceContainer.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(25.dp).height(8.dp).background(SurfaceContainer.copy(alpha = 0.3f)))
    }
}

@Composable
fun ArtistCircleCard(
    artist: Artist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(SurfaceContainer)
        ) {
            // Artist image placeholder or actual image
            if (!artist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = CredentialsManager.getCoverArtUrl(artist.coverUrl, 160),
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name ?: "Unknown",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun CuratedAlbumsRow(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit
) {
    if (albums.isEmpty()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AlbumExploreCard("Loading...", "...")
            AlbumExploreCard("Loading...", "...")
        }
        return
    }
    
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums.take(5)) { album ->
            AlbumExploreCard(
                title = album.title ?: "Unknown",
                artist = album.artistName ?: "Unknown Artist",
                coverArt = album.coverUrl,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun CuratedAlbumsGrid() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AlbumExploreCard("Neon Cathedral", "Vaporwave Collective")
        AlbumExploreCard("Structure & Echo", "Industrial Theory")
    }
}

@Composable
fun AlbumExploreCard(
    title: String,
    artist: String,
    coverArt: String? = null,
    color: Color = Color(0xFF3D3D3D),
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color)
        ) {
            if (!coverArt.isNullOrBlank()) {
                AsyncImage(
                    model = CredentialsManager.getCoverArtUrl(coverArt, 320),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(artist, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
    }
}

// Legacy data class for static content (can be removed)
data class ArtistItem(val name: String, val info: String)

@Composable
fun ArtistListRow(
    artist: Artist,
    onArtistClick: ((Artist) -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceContainer)
        ) {
            if (!artist.coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = CredentialsManager.getCoverArtUrl(artist.coverUrl, 112),
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(artist.name ?: "Unknown", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("${artist.albumCount} albums", color = TextSecondary, fontSize = 12.sp)
        }
        IconButton(
            onClick = { onArtistClick?.invoke(artist) },
            modifier = Modifier.background(SurfaceDark, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary)
        }
    }
}

@Composable
fun ExploreMiniPlayer(modifier: Modifier = Modifier) {
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
            Text("Neon Cathedral", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("VAPORWAVE COLLEC...", color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = TextPrimary)
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentPurple)) {
            Icon(Icons.Default.Pause, contentDescription = null, tint = BackgroundDark, modifier = Modifier.align(Alignment.Center))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Icon(Icons.Default.SkipNext, contentDescription = null, tint = TextPrimary)
    }
}

@Composable
fun ExploreBottomNavigationBar() {
    NavigationBar(
        containerColor = SurfaceDark.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("HOME") },
            selected = false,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextSecondary)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text("EXPLORE") },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentPurple, selectedTextColor = AccentPurple, unselectedIconColor = TextSecondary)
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
