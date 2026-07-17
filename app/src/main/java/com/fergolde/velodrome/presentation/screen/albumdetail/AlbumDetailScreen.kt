package com.fergolde.velodrome.presentation.screen.albumdetail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.presentation.components.UniversalOptionsSheet
import com.fergolde.velodrome.presentation.screen.home.AlbumCover
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBackClick: () -> Unit = {},
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTrackOptions by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = data.visuals.message,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = paddingValues.calculateBottomPadding())
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
                        onShuffleClick = {
                            viewModel.shuffleAll()
                            scope.launch {
                                snackbarHostState.showSnackbar("Reproducción aleatoria iniciada")
                            }
                        },
                        onAddToQueueClick = {
                            viewModel.addAllToQueue()
                            scope.launch {
                                snackbarHostState.showSnackbar("Álbum añadido a la cola")
                            }
                        },
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
                UniversalOptionsSheet(
                    title = selectedTrack!!.title,
                    subtitle = selectedTrack!!.artistName,
                    coverArtId = selectedTrack!!.coverArtId,
                    onPlayNow = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showTrackOptions = false
                        }
                        viewModel.playNow(selectedTrack!!)
                    },
                    onPlayNext = {
                        val track = selectedTrack!!
                        scope.launch {
                            sheetState.hide()
                            showTrackOptions = false
                            viewModel.playNext(track)
                            snackbarHostState.showSnackbar("Se reproducirá a continuación")
                        }
                    },
                    onAddToQueue = {
                        val track = selectedTrack!!
                        scope.launch {
                            sheetState.hide()
                            showTrackOptions = false
                            viewModel.addToQueue(track)
                            snackbarHostState.showSnackbar("Añadido a la cola")
                        }
                    }
                )
            }
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
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        // ── Landscape: todo izq (cover+info+btns), solo tracks der ───────
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left: cover + album info + action buttons (centrado vertical)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.35f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Back button siempre arriba
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(start = 8.dp, top = 8.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .align(Alignment.Start)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }

                // Bloque centrado verticalmente
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cover
                    AlbumCover(
                        coverArtId = album?.coverUrl,
                        contentDescription = album?.title,
                        size = 0.dp,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f)
                    )

                    Spacer(Modifier.height(20.dp))

                    // Album info
                    Text(
                        text = album?.title ?: "",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = album?.artistName ?: "",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        album?.year?.let { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = " · ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        Text(
                            text = "${tracks.size} tracks · ${durationDisk(tracks)} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onPlayAllClick,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play", fontSize = 13.sp)
                        }
                        Button(
                            onClick = onShuffleClick,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Button(
                            onClick = onAddToQueueClick,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Right: solo tracks
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.65f)
                    .padding(end = 16.dp)
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
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
        }
    } else {
        // ── Portrait: layout actual (sin cambios) ─────────────────────────
        LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
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
    val headerHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() / 3f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                        )
                    )
            )
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

        Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Text(text = album?.title ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                text = album?.artistName ?: "",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            album?.year?.let { Text(it.toString()) }
            Text("${tracks.size} tracks · ${durationDisk(tracks)} minutos", fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Button(
                    onClick = onPlayAllClick,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }
                Button(
                    onClick = onShuffleClick,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                }
                Button(
                    onClick = onAddToQueueClick,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isPlaying) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
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
        Text(text = trackDuration, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "$mins:${secs.toString().padStart(2, '0')}"
}

private fun durationDisk(tracks: List<Track>): Int {
    var duration = 0
    for (track in tracks) {
        duration += track.durationSec
    }
    return duration / 60
}