package com.example.velodrome.presentation.screen.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.example.velodrome.presentation.screen.homescreen.ArtistAvatar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.UiConstants
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.screen.homescreen.RecentAlbumsRow
import com.example.velodrome.presentation.screen.homescreen.SectionHeader
import com.example.velodrome.util.CredentialsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onHomeClick: () -> Unit = {},
    onArtistsViewAllClick: () -> Unit = {},
    onAlbumsViewAllClick: () -> Unit = {},
    onPlayerClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { ExploreBottomNavigationBar(onHomeClick = onHomeClick, onSettingsClick = onSettingsClick) },
        containerColor = MaterialTheme.colorScheme.background
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
                    onQueryChange = { query -> viewModel.onSearchQueryChange(query) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Random Artists Carousel
            item {
                SectionHeader(
                    title = stringResource(R.string.explore_artists),
                    subtitle = stringResource(R.string.explore_artists_discover),
                    onViewAllClick = onArtistsViewAllClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                RandomArtistsRow(
                    artists = uiState.randomArtists,
                    onArtistClick = { artist -> onArtistClick(artist.id) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Random Albums Carousel
            item {
                SectionHeader(
                    title = stringResource(R.string.explore_random_albums),
                    subtitle = stringResource(R.string.nav_explore),
                    onViewAllClick = onAlbumsViewAllClick
                )
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = uiState.randomAlbums,
                    onAlbumClick = { albumId -> onAlbumClick(albumId) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Genres Section
            item {
                SectionHeader(
                    title = stringResource(R.string.explore_genres),
                    subtitle = stringResource(R.string.explore_all_genres),
                    onViewAllClick = null,
                    showActionText = stringResource(R.string.play),
                    onActionClick = viewModel::onPlayGenres
                )
                Spacer(modifier = Modifier.height(16.dp))
                GenresRow(
                    genres = uiState.genres,
                    selectedGenres = uiState.selectedGenres,
                    onGenreToggle = viewModel::onGenreToggle
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Padding for mini player
            }
        }

        // Mini Player - only show if there's a track to play
        val currentTrack by PlayerManager.currentTrack.collectAsState()
        val currentPosition by PlayerManager.currentPosition.collectAsState()
        val isPlaying by PlayerManager.isPlaying.collectAsState()
        
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
            .background(MaterialTheme.colorScheme.surface),
        placeholder = { Text(stringResource(R.string.explore_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingIcon = { Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(40.dp).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.width(25.dp).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)))
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Artist image with cache
            ArtistAvatar(
                coverArtId = artist.coverUrl,
                contentDescription = artist.name,
                size = 80.dp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = artist.name,
color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun GenresRow(
    genres: List<String>,
    selectedGenres: Set<String> = emptySet(),
    onGenreToggle: (String) -> Unit = {}
) {
    if (genres.isEmpty()) {
        // Show placeholder grid when loading
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) {
                        GenreChipPlaceholder()
                    }
                }
            }
        }
        return
    }
    
    // Show genres in 3-column grid
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        genres.chunked(3).forEach { rowGenres ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowGenres.forEach { genre ->
                    GenreChip(
                        genre = genre,
                        isSelected = selectedGenres.contains(genre),
                        onClick = { onGenreToggle(genre) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining columns if row has less than 3 items
                repeat(3 - rowGenres.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GenreChipPlaceholder() {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
fun GenreChip(
    genre: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = genre,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ExploreBottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
){
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = false,
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_explore)) },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
fun SearchResultsView(
    searchQuery: String,
    searchResults: SearchResults,
    isSearching: Boolean,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClearSearch) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "\"$searchQuery\"",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (searchResults.totalCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${searchResults.totalCount})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isSearching) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (searchResults.isEmpty) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // Artists - horizontal carousel
            if (searchResults.artists.isNotEmpty()) {
                item {
                    Column {
                        Text("Artists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(searchResults.artists) { artist ->
                                ArtistCircleCard(artist = artist, onClick = { onArtistClick(artist.id) })
                            }
                        }
                    }
                }
            }

            // Albums - horizontal carousel
            if (searchResults.albums.isNotEmpty()) {
                item {
                    Column {
                        Text("Albums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(searchResults.albums) { album ->
                                AlbumGridItem(album = album, onClick = { onAlbumClick(album.id) })
                            }
                        }
                    }
                }
            }

            // Tracks - vertical list
            if (searchResults.tracks.isNotEmpty()) {
                item {
                    Column {
                        Text("Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchResults.tracks.forEach { track ->
                                TrackRow(track = track, onClick = { })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(album.title.take(2), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(album.artistName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
fun TrackRow(track: Track, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("♪", color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(track.artistName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}
