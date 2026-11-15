package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.activity.compose.LocalActivity
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
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.presentation.components.VeloListBottomClearance
import com.fergolde.velodrome.presentation.components.VeloErrorWithRetry
import com.fergolde.velodrome.presentation.components.VeloSnackbarHost
import com.fergolde.velodrome.presentation.screen.home.AlbumCover
import com.fergolde.velodrome.presentation.screen.home.ArtistAvatar

@Composable
fun ArtistDetailScreen(
    onBackClick: () -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // Los textos se resuelven en composición: los callbacks lanzan corrutinas y
    // no pueden leer recursos por sí mismos.
    val msg_artist_radio = stringResource(R.string.snackbar_artist_radio_started)
    val msg_playback_started = stringResource(R.string.snackbar_playback_started)
    val msg_shuffle_started = stringResource(R.string.snackbar_shuffle_started)
    val msg_albums_added_to_queue = stringResource(R.string.snackbar_albums_added_to_queue)

    Scaffold(
        snackbarHost = { VeloSnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = it.calculateBottomPadding()),
            // El detalle se centra horizontalmente cuando la tablet lo acota a
            // ancho contenido; el spinner y el error fijan su propia alineación.
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                uiState.error != null -> {
                    VeloErrorWithRetry(
                        message = uiState.error ?: stringResource(R.string.error_loading),
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    ArtistAlbumsList(
                        artist = uiState.artist,
                        albums = uiState.albums,
                        isPreparingPlayback = uiState.isPreparingPlayback,
                        onAlbumClick = onAlbumClick,
                        onPlayAllClick = {
                            viewModel.playAll()
                            scope.launch {
                                snackbarHostState.showSnackbar(msg_playback_started)
                            }
                        },
                        onShuffleAllClick = {
                            viewModel.shuffleAll()
                            scope.launch {
                                snackbarHostState.showSnackbar(msg_shuffle_started)
                            }
                        },
                        onRadioClick = {
                            viewModel.startArtistRadio()
                            scope.launch {
                                snackbarHostState.showSnackbar(msg_artist_radio)
                            }
                        },
                        onAddToQueueClick = {
                            viewModel.addToQueue()
                            scope.launch {
                                snackbarHostState.showSnackbar(msg_albums_added_to_queue)
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
                // El círculo visible sigue en 40dp; el IconButton aporta el área
                // táctil de 48dp sin cambiar el dibujo.
                IconButton(onClick = onBackClick) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
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
}

/**
 * Ancho máximo de la columna central de un detalle en tablet (S1).
 *
 * Es el mismo tope en los tres detalles para que álbum, artista y playlist se
 * lean igual; queda por encima de los 840dp de ancho de la clase `Medium`, así
 * que solo recorta de verdad en `Expanded`.
 */
internal val ArtistDetailContentMaxWidth: Dp = 880.dp

/**
 * Columnas de la grilla de álbumes del detalle de artista (S1).
 *
 * Reemplaza `R.bool.allow_rotation`, que no distinguía tablet vertical de
 * ancha, y deja el teléfono en las dos columnas de siempre.
 */
internal fun artistAlbumGridColumns(widthSizeClass: WindowWidthSizeClass): Int =
    when (widthSizeClass) {
        WindowWidthSizeClass.Medium -> 4
        WindowWidthSizeClass.Expanded -> 5
        else -> 2
    }

/**
 * Ancho máximo del contenido del detalle de artista (S1).
 *
 * `Dp.Infinity` significa "sin tope": el teléfono conserva el ancho completo.
 */
internal fun artistDetailContentMaxWidth(widthSizeClass: WindowWidthSizeClass): Dp =
    if (widthSizeClass == WindowWidthSizeClass.Compact) Dp.Infinity else ArtistDetailContentMaxWidth

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ArtistAlbumsList(
    artist: com.fergolde.velodrome.domain.model.Artist?,
    albums: List<Album>,
    isPreparingPlayback: Boolean,
    onAlbumClick: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onRadioClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit
) {
    // S1: el detalle se centra a ancho contenido en tablet para que el header
    // no se desparrame sobre toda la pantalla. La alineación la aporta el `Box`
    // de [ArtistDetailScreen].
    val widthSizeClass = LocalActivity.current
        ?.let { calculateWindowSizeClass(it).widthSizeClass }
        ?: WindowWidthSizeClass.Compact
    LazyVerticalGrid(
        columns = GridCells.Fixed(artistAlbumGridColumns(widthSizeClass)),
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = artistDetailContentMaxWidth(widthSizeClass)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = VeloListBottomClearance),
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
                        Text(stringResource(R.string.action_play_all))
                    }

                    Button(
                        onClick = onShuffleAllClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isPreparingPlayback,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.player_shuffle)
                        )
                    }

                    Button(
                            onClick = onRadioClick,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Radio,
                                contentDescription = stringResource(R.string.action_artist_radio),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Button(
                        onClick = onAddToQueueClick,
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isPreparingPlayback,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = stringResource(R.string.action_add_to_queue)
                        )
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