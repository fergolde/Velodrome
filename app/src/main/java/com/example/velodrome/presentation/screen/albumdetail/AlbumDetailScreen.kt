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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.screen.home.AlbumCover
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTrackId = uiState.currentTrackId
    val isPlaying = uiState.isPlaying
    val currentPosition = uiState.currentPosition

    var showTrackOptions by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "Error")
                }
            }

            else -> {
                AlbumContent(
                    album = uiState.album,
                    tracks = uiState.tracks,
                    onTrackClick = { viewModel.playTrack(it) },
                    onTrackLongClick = {
                        selectedTrack = it
                        showTrackOptions = true
                    },
                    onPlayAllClick = { viewModel.playAll() },
                    onShuffleClick = { viewModel.shuffleAll() },
                    onAddToQueueClick = { viewModel.addAllToQueue() },
                    onBackClick = onBackClick,
                    currentTrackId = uiState.currentTrackId
                )
            }
        }
    }
    if (showTrackOptions && selectedTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showTrackOptions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            TrackOptionsSheet(
                track = selectedTrack!!,
                onPlayNow = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showTrackOptions = false
                    }
                    viewModel.playNow(selectedTrack!!)
                },
                onPlayNext = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showTrackOptions = false
                    }
                    viewModel.playNext(selectedTrack!!)
                },
                onAddToQueue = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showTrackOptions = false
                    }
                    viewModel.addToQueue(selectedTrack!!)
                }
            )
        }
    }
}

@Composable
private fun AlbumContent(
    album: Album?,
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onTrackLongClick: (Track) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onBackClick: () -> Unit,
    currentTrackId: String? = null
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            AlbumHeader(
                album = album,
                tracks = tracks,
                onPlayAllClick = onPlayAllClick,
                onShuffleClick = onShuffleClick,
                onAddToQueueClick = onAddToQueueClick,
                onBackClick = onBackClick
            )
        }

        item {
            Text(
                text = "Tracks",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
    album: Album?,
    tracks: List<Track>,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerHeight = screenHeight / 3

    Column(modifier = Modifier.fillMaxWidth()) {

        // 🔥 HERO IMAGE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
        ) {

            AlbumCover(
                coverArtId = album?.coverUrl,
                contentDescription = album?.title,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp
            )

            // Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            // 🔙 BACK BUTTON
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .padding(16.dp)
                    .size(42.dp)
                    .clip(CircleShape)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }

        // INFO
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = album?.title ?: "",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = album?.artistName ?: "",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            album?.year?.let {
                Text(it.toString())
            }

            Text(
                "${tracks.size} tracks · ${durationDisk(tracks)} minutos",
                fontSize = 14.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                Button(
                    onClick = onPlayAllClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }

                FilledIconButton(
                    onClick = onShuffleClick
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                }

                FilledIconButton(
                    onClick = onAddToQueueClick
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                }
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
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Cabecera con info de la canción
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumCover(
                coverArtId = track.coverArtId,  // ajusta el nombre del campo si es distinto
                contentDescription = track.title,
                size = 44.dp,
                cornerRadius = 8.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = track.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(Modifier.height(8.dp))

        TrackOptionItem(
            icon = Icons.Default.PlayArrow,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            title = "Reproducir ahora",
            subtitle = "Salta a esta canción inmediatamente",
            onClick = onPlayNow
        )

        TrackOptionItem(
            icon = Icons.Default.SkipNext,
            iconTint = Color(0xFF0F6E56),
            iconBackground = Color(0xFF0F6E56).copy(alpha = 0.12f),
            title = "Reproducir siguiente",
            subtitle = "Se pone justo después de la actual",
            onClick = onPlayNext
        )

        TrackOptionItem(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            iconTint = Color(0xFF854F0B),
            iconBackground = Color(0xFF854F0B).copy(alpha = 0.12f),
            title = "Añadir al final",
            subtitle = "Se añade al final de la cola",
            onClick = onAddToQueue
        )
    }
}

@Composable
private fun TrackOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

private fun durationDisk(tracks: List<Track>) : Int {
    var duration = 0
    for (track in tracks){
        duration+=track.durationSec

    }
    return duration/60
}