package com.example.velodrome.presentation.screen.albumdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import com.example.velodrome.presentation.screen.homescreen.AlbumCover
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.velodrome.R
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.UiConstants
import com.example.velodrome.presentation.components.MiniPlayer
import com.example.velodrome.presentation.player.PlayerManager
import com.example.velodrome.util.CredentialsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit = {},
    onExploreClick: () -> Unit = {},
    onPlayerClick: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTrack by PlayerManager.currentTrack.collectAsState()
    val isPlaying by PlayerManager.isPlaying.collectAsState()
    val currentPosition by PlayerManager.currentPosition.collectAsState()

    // Track options bottom sheet
    var showTrackOptions by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.album?.title ?: stringResource(R.string.album_detail_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AlbumDetailBottomNavigationBar(
                onHomeClick = onPlayerClick,
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
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: stringResource(R.string.error_loading),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    AlbumContent(
                        album = uiState.album,
                        tracks = uiState.tracks,
                        onTrackClick = { track -> viewModel.playTrack(track) },
                        onTrackLongClick = { track ->
                            selectedTrack = track
                            showTrackOptions = true
                        },
                        onPlayAllClick = { viewModel.playAll() },
                        onShuffleClick = { viewModel.shuffleAll() },
                        onAddToQueueClick = { viewModel.addAllToQueue() }
                    )
                }
            }
            
            // MiniPlayer overlay - shown when music is playing
            if (currentTrack != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    MiniPlayer(
                        modifier = Modifier.padding(bottom = UiConstants.MiniPlayerBottomMargin),
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

    // Track options bottom sheet
    if (showTrackOptions && selectedTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showTrackOptions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TrackOptionsSheet(
                track = selectedTrack!!,
                onPlayNext = {
                    viewModel.playNext(selectedTrack!!)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showTrackOptions = false }
                },
                onAddToQueue = {
                    viewModel.addToQueue(selectedTrack!!)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showTrackOptions = false }
                }
            )
        }
    }
}

@Composable
private fun AlbumContent(
    album: com.example.velodrome.domain.model.Album?,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddToQueueClick: () -> Unit
) {
    val currentTrackId by PlayerManager.currentTrackId.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Album Header with cover image
        item {
            AlbumHeader(
                album = album,
                trackCount = tracks.size,
                onPlayAllClick = onPlayAllClick,
                onShuffleClick = onShuffleClick,
                onAddToQueueClick = onAddToQueueClick
            )
        }

        // Track list
        item {
            Text(
                text = stringResource(R.string.album_detail_tracks),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        items(tracks) { track ->
            TrackItem(
                track = track,
                isPlaying = track.id == currentTrackId,
                onClick = { onTrackClick(track) },
                onLongClick = { onTrackLongClick(track) }
            )
        }
    }
}

@Composable
private fun AlbumHeader(
    album: com.example.velodrome.domain.model.Album?,
    trackCount: Int,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddToQueueClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AlbumCover(
                coverArtId = album?.coverUrl,
                contentDescription = album?.title,
                size = 200.dp,
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxSize()
            )
            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Album info
        Text(
            text = album?.title ?: "",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album?.artistName ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )

        album?.year?.let { year ->
            Text(
                text = year.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$trackCount ${stringResource(R.string.album_detail_tracks).lowercase()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAllClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.album_detail_play_all))
            }

            IconButton(
                onClick = onShuffleClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.album_detail_shuffle),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = onAddToQueueClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = stringResource(R.string.album_detail_add_to_queue),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun TrackItem(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val trackDuration = formatDuration(track.durationSec)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    text = track.trackNumber.toString(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track title
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Duration
        Text(
            text = trackDuration,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TrackOptionsSheet(
    track: Track,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Track title
        Text(
            text = track.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))

        Spacer(modifier = Modifier.height(8.dp))

        // Play Next option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPlayNext)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.track_options_play_next),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Add to Queue option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAddToQueue)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.track_options_add_to_queue),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

@Composable
fun AlbumDetailBottomNavigationBar(
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