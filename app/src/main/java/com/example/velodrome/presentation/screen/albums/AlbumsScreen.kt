package com.example.velodrome.presentation.screen.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.UiConstants
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.util.CredentialsManager


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onPlayerClick: () -> Unit = {},
    onAlbumClick: (Album) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { AlbumsTopAppBar(onSearchQueryChange = viewModel::onSearchQueryChange) },
        bottomBar = {
            AlbumsBottomNavigationBar(
                onHomeClick = onHomeClick,
                onExploreClick = onExploreClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.error != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        AlbumsSearchBar(
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        AlbumsList(
                            albums = uiState.albums,
                            onAlbumClick = onAlbumClick
                        )
                    }
                }
            }

            // Mini Player - only show if there's a track to play
            val currentTrack by PlayerManager.currentTrack.collectAsState()
            val isPlaying by PlayerManager.isPlaying.collectAsState()
            
            if (currentTrack != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    MiniPlayer(
                        modifier = Modifier.padding(bottom = UiConstants.MiniPlayerBottomMargin),
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayPauseClick = { PlayerManager.togglePlayPause() },
                        onClick = onPlayerClick,
                        onNextClick = { PlayerManager.next() },
                        onPreviousClick = { PlayerManager.previous() }
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumsTopAppBar(onSearchQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
        Text(stringResource(R.string.albums_title), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun AlbumsSearchBar(
    query: String = "",
    onQueryChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text(stringResource(R.string.albums_search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun AlbumsList(
    albums: List<Album>,
    onAlbumClick: (Album) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
            AlbumCard(
                album = album,
                onClick = { onAlbumClick(album) }
            )
        }
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!album.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = CredentialsManager.getCoverArtUrl(album.coverUrl, 128),
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title ?: "Unknown Album",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = album.artistName ?: "Unknown Artist",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AlbumsBottomNavigationBar(
    onHomeClick: () -> Unit = {},
    onExploreClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
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
            selected = false,
            onClick = onExploreClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) },
            selected = false,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}