package com.example.velodrome.presentation.screen.homescreen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.UiConstants
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.presentation.player.PlayerManagerHolder
import com.example.velodrome.ui.theme.VelodromeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onAlbumClick: (String) -> Unit = {},
    onExploreClick: () -> Unit = {},
    onPlayerClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { BottomNavigationBar(onExploreClick = onExploreClick, onSettingsClick = onSettingsClick) },
        containerColor = MaterialTheme.colorScheme.background
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

            //Recien añadidos
            item {
                SectionHeader(title = stringResource(R.string.home_recently_added), subtitle = stringResource(R.string.home_new_arrivals))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.latestAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            //Aleatorios
            item {
                SectionHeader(title = stringResource(R.string.home_random), subtitle = stringResource(R.string.home_discover))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.randomAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            //Mas reproducidos
            item {
                SectionHeader(title = stringResource(R.string.home_most_played), subtitle = stringResource(R.string.home_your_favorites))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.topAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            //Recientemente reproducidos
            item {
                SectionHeader(title = stringResource(R.string.home_recently_played), subtitle = stringResource(R.string.home_just_for_you))
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlbumsRow(
                    albums = state.recentlyPlayedAlbums,
                    onAlbumClick = onAlbumClick
                )
                Spacer(modifier = Modifier.height(32.dp))
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

    // Loading state
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.retry() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.home_retry), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
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
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(28.dp)
    ) {
        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.home_shuffle), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
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
        Text(subtitle, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            val actionText = showActionText ?: if (onViewAllClick != null) stringResource(R.string.view_all) else null
            if (actionText != null) {
                Text(
                    text = actionText,
                    color = MaterialTheme.colorScheme.primary,
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
fun BottomNavigationBar(
    onExploreClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            selected = true,
            onClick = { },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
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
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}
