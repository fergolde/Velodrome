package com.example.velodrome.presentation.screen.artistdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.screen.home.AlbumCover
import com.example.velodrome.presentation.screen.home.ArtistAvatar

@Composable
fun ArtistDetailScreen(
    onBackClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
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
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = it.calculateBottomPadding())
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: stringResource(R.string.error_loading),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    ArtistAlbumsList(
                        artist = uiState.artist,
                        albums = uiState.albums,
                        isPreparingPlayback = uiState.isPreparingPlayback,
                        onBackClick = onBackClick,
                        onAlbumClick = onAlbumClick,
                        onPlayAllClick = {
                            viewModel.playAll()
                            scope.launch {
                                snackbarHostState.showSnackbar("Reproducción iniciada")
                            }
                        },
                        onShuffleAllClick = {
                            viewModel.shuffleAll()
                            scope.launch {
                                snackbarHostState.showSnackbar("Reproducción aleatoria iniciada")
                            }
                        },
                        onAddToQueueClick = {
                            viewModel.addToQueue()
                            scope.launch {
                                snackbarHostState.showSnackbar("Álbumes añadidos a la cola")
                            }
                        }
                    )
                }
            }
        }

        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = Color.White
                    )
                }
            }
    }
}

@Composable
fun ArtistAlbumsList(
    artist: com.example.velodrome.domain.model.Artist?,
    albums: List<Album>,
    isPreparingPlayback: Boolean,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onAddToQueueClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 24.dp)
                        .size(180.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    ArtistAvatar(
                        coverArtId = artist?.coverUrl,
                        contentDescription = artist?.name,
                        size = 180.dp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = artist?.name ?: "",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(R.string.artist_detail_albums_count, artist?.albumCount ?: 0).uppercase(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPlayAllClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isPreparingPlayback,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (isPreparingPlayback) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Play")
                    }

                    Button(
                        onClick = onShuffleAllClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isPreparingPlayback,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                    }

                    Button(
                        onClick = onAddToQueueClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isPreparingPlayback,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = stringResource(R.string.artist_detail_albums),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(albums, key = { it.id }) { album ->
            ArtistAlbumCard(
                album = album,
                onClick = { onAlbumClick(album.id) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ArtistAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AlbumCover(
                coverArtId = album.coverUrl,
                contentDescription = album.title,
                size = 200.dp,
                cornerRadius = 12.dp,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            fontSize = 14.sp
        )
        Text(
            text = album.year?.toString() ?: "",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}