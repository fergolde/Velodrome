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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.screen.home.AlbumCover
import com.example.velodrome.presentation.screen.home.ArtistAvatar
import com.example.velodrome.presentation.screen.home.RecentAlbumsRow
import com.example.velodrome.presentation.screen.home.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onHomeClick: () -> Unit = {},
    onArtistsViewAllClick: () -> Unit = {},
    onAlbumsViewAllClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
        ) {
item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { query -> viewModel.onSearchQueryChange(query) },
                    onClearClick = { viewModel.clearSearch() }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (uiState.searchQuery.isNotBlank()) {
                item {
                    SearchResultsView(
                        searchQuery = uiState.searchQuery,
                        searchResults = uiState.searchResults,
                        isSearching = uiState.isSearching,
                        onArtistClick = onArtistClick,
                        onAlbumClick = onAlbumClick,
                        onPlayTrackClick = viewModel::playSearchedTrack,
                        onClearSearch = viewModel::clearSearch
                    )
                }
            } else {
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
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Padding for mini player
            }
        }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit
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
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Borrar búsqueda",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
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
            repeat(5) { _ ->
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
fun SearchResultsView(
    searchQuery: String,
    searchResults: SearchResults,
    isSearching: Boolean,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlayTrackClick: (Track) -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (searchResults.isEmpty) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // Artists - horizontal carousel
            if (searchResults.artists.isNotEmpty()) {
                Column {
                    Text("Artists", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(searchResults.artists.take(10)) { artist ->
                            ArtistCircleCard(artist = artist, onClick = { onArtistClick(artist.id) })
                        }
                    }
                }
            }

            // Albums - horizontal carousel
            if (searchResults.albums.isNotEmpty()) {
                Column {
                    Text("Albums", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(searchResults.albums.take(10)) { album ->
                            AlbumGridItem(album = album, onClick = { onAlbumClick(album.id) })
                        }
                    }
                }
            }

            // Tracks - vertical list
            if (searchResults.tracks.isNotEmpty()) {
                Column {
                    Text("Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        searchResults.tracks.take(25).forEach { track ->
                            TrackRow(track = track, onClick = { onPlayTrackClick(track) })
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
        AlbumCover(
            coverArtId = album.coverUrl,
            contentDescription = album.title,
            size = 100.dp,
            cornerRadius = 8.dp
        )
        Spacer(Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Text(album.artistName, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
fun TrackRow(track: Track, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CARÁTULA REAL EN LA CANCIÓN
            AlbumCover(
                coverArtId = track.coverArtId,
                contentDescription = track.albumName,
                size = 48.dp,
                cornerRadius = 8.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Indicador de duración sutil
            Text(
                text = formatDuration(track.durationSec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
