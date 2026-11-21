package com.fergolde.velodrome.presentation.screen.artistdetail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.presentation.components.VeloListBottomClearance
import com.fergolde.velodrome.presentation.components.VeloErrorWithRetry
import com.fergolde.velodrome.presentation.components.VeloSnackbarHost
import com.fergolde.velodrome.presentation.components.adaptiveGridColumns
import com.fergolde.velodrome.presentation.components.detailsContentMaxWidth
import com.fergolde.velodrome.presentation.components.currentWindowWidthSizeClass
import com.fergolde.velodrome.presentation.screen.home.AlbumCover
import com.fergolde.velodrome.presentation.screen.home.rememberArtworkImageRequest

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
            // ancho contenido; en horizontal la grilla ocupa todo el ancho y no
            // queda margen que centrar. El spinner y el error fijan su propia
            // alineación.
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
                        sortOrder = uiState.sortOrder,
                        availableYears = uiState.availableYears,
                        selectedYear = uiState.selectedYear,
                        isPreparingPlayback = uiState.isPreparingPlayback,
                        onAlbumClick = onAlbumClick,
                        onSortOrderChange = viewModel::setSortOrder,
                        onYearSelected = viewModel::setYearFilter,
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
 * Columnas de la grilla de álbumes del detalle de artista (S1).
 *
 * La cuenta por clase la resuelve [adaptiveGridColumns]: el teléfono queda en
 * las dos columnas de siempre. La orientación entra en la cuenta porque en
 * horizontal la grilla ocupa todo el ancho (ver
 * [artistAlbumsContentMaxWidth]) y conservar las columnas verticales estiraba
 * cada tarjeta a lo ancho del dispositivo; con más columnas la tarjeta mide
 * ~180-200dp, lo mismo que en vertical.
 */
internal fun artistAlbumGridColumns(
    widthSizeClass: WindowWidthSizeClass,
    isLandscape: Boolean,
): Int = if (isLandscape) {
    adaptiveGridColumns(widthSizeClass, compact = 3, medium = 4, expanded = 6)
} else {
    adaptiveGridColumns(widthSizeClass, compact = 2, medium = 4, expanded = 5)
}

/**
 * Ancho máximo de la grilla de álbumes del detalle de artista.
 *
 * En vertical se conserva el tope de [detailsContentMaxWidth], que centra el
 * contenido en tablet para que el header no se desparrame. En horizontal ese
 * tope se descarta (`Dp.Infinity`): en tablet apaisada la franja acotada dejaba
 * bandas vacías a los lados y la grilla no aprovechaba el ancho disponible.
 */
internal fun artistAlbumsContentMaxWidth(
    widthSizeClass: WindowWidthSizeClass,
    isLandscape: Boolean,
): Dp = if (isLandscape) Dp.Infinity else detailsContentMaxWidth(widthSizeClass)

/** Padding horizontal que la grilla de álbumes reserva a cada lado. */
internal val artistHeaderHorizontalGutter = 16.dp

/** Radio del difuminado de la portada que hace de fondo del header. */
private val ArtistBackdropBlurRadius = 10.dp

/**
 * Tamaño que se le pide a Coil para la portada de fondo.
 *
 * El difuminado se come el detalle, así que basta con el escalón de 512dp de
 * `canonicalArtworkSize`: pedir más solo gastaría memoria.
 */
private val ArtistBackdropRequestSize = 512.dp

/**
 * Indica si el header del detalle de artista puede pintar la portada
 * difuminada de fondo.
 *
 * Sin portada no hay imagen que pintar: se conserva el fondo plano de la app,
 * igual que cuando la descarga falla y Coil no dibuja nada.
 */
internal fun artistHeaderShowsBackdrop(coverArtId: String?): Boolean = !coverArtId.isNullOrBlank()

/**
 * Sangrado horizontal del fondo difuminado del header.
 *
 * En el teléfono la grilla llega al borde de la pantalla con un padding de
 * contenido de [artistHeaderHorizontalGutter], así que el fondo sangra esos
 * mismos 16dp por lado y toca los bordes. En tablet la grilla ya está acotada a
 * ancho contenido: ahí un sangrado dejaría el fondo asomando fuera de la banda,
 * así que el fondo se queda dentro.
 */
internal fun artistHeaderBackdropBleed(widthSizeClass: WindowWidthSizeClass): Dp =
    if (widthSizeClass == WindowWidthSizeClass.Compact) artistHeaderHorizontalGutter else 0.dp

/**
 * Ensancha el nodo [bleed] dp por cada lado y lo recoloca, para que el fondo
 * del header llegue a los bordes aunque la grilla tenga padding de contenido.
 */
private fun Modifier.artistBackdropBleed(bleed: Dp): Modifier =
    if (bleed <= 0.dp) {
        this
    } else {
        layout { measurable, constraints ->
            val bleedPx = bleed.roundToPx()
            val widenedWidth = constraints.maxWidth + bleedPx * 2
            val placeable = measurable.measure(
                constraints.copy(minWidth = widenedWidth, maxWidth = widenedWidth)
            )
            layout(constraints.maxWidth, placeable.height) {
                placeable.place(-bleedPx, 0)
            }
        }
    }

@Composable
fun ArtistAlbumsList(
    artist: Artist?,
    albums: List<Album>,
    sortOrder: AlbumSortOrder,
    availableYears: List<Int>,
    selectedYear: Int?,
    isPreparingPlayback: Boolean,
    onAlbumClick: (String) -> Unit,
    onSortOrderChange: (AlbumSortOrder) -> Unit,
    onYearSelected: (Int?) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onRadioClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit
) {
    // El detalle se acota a ancho contenido en tablet para que el header no se
    // desparrame sobre toda la pantalla, salvo en horizontal, donde la grilla
    // ocupa el ancho completo (ver [artistAlbumsContentMaxWidth]). La alineación
    // la aporta el `Box` de [ArtistDetailScreen].
    val widthSizeClass = currentWindowWidthSizeClass()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LazyVerticalGrid(
        columns = GridCells.Fixed(artistAlbumGridColumns(widthSizeClass, isLandscape)),
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = artistAlbumsContentMaxWidth(widthSizeClass, isLandscape)),
        contentPadding = PaddingValues(
            start = artistHeaderHorizontalGutter,
            end = artistHeaderHorizontalGutter,
            bottom = VeloListBottomClearance
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ArtistHeader(
                artist = artist,
                widthSizeClass = widthSizeClass,
                isPreparingPlayback = isPreparingPlayback,
                onPlayAllClick = onPlayAllClick,
                onShuffleAllClick = onShuffleAllClick,
                onRadioClick = onRadioClick,
                onAddToQueueClick = onAddToQueueClick
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.artist_detail_albums),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    ArtistSortMenu(
                        sortOrder = sortOrder,
                        onSortOrderChange = onSortOrderChange
                    )
                }

                // El filtro solo ofrece los años que el artista realmente
                // tiene: un año sin álbumes dejaría la grilla vacía.
                if (availableYears.isNotEmpty()) {
                    ArtistYearFilterRow(
                        availableYears = availableYears,
                        selectedYear = selectedYear,
                        onYearSelected = onYearSelected
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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

/**
 * Header del detalle de artista.
 *
 * La portada del artista hace de fondo difuminado a sangre completa, con un
 * scrim oscuro encima para que el nombre, el contador de álbumes y los botones
 * de acción se lean. Sin portada el header se queda con el fondo plano de la
 * app, que es lo que se veía antes.
 */
@Composable
private fun ArtistHeader(
    artist: Artist?,
    widthSizeClass: WindowWidthSizeClass,
    isPreparingPlayback: Boolean,
    onPlayAllClick: () -> Unit,
    onShuffleAllClick: () -> Unit,
    onRadioClick: () -> Unit,
    onAddToQueueClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        if (artistHeaderShowsBackdrop(artist?.coverUrl)) {
            ArtistHeaderBackdrop(
                coverArtId = artist?.coverUrl,
                bleed = artistHeaderBackdropBleed(widthSizeClass),
                modifier = Modifier.matchParentSize()
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // El espacio de la barra de estado más un margen para que el nombre no
            // choque con el botón de volver de la esquina superior.
            Spacer(
                modifier = Modifier
                    .statusBarsPadding()
                    .height(64.dp)
            )

            Text(
                text = artist?.name ?: "",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
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
}

/**
 * Fondo del header: la portada del artista, difuminada, con un scrim vertical
 * que oscurece la zona del texto y se funde con el fondo de la app al final.
 */
@Composable
private fun ArtistHeaderBackdrop(
    coverArtId: String?,
    bleed: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.artistBackdropBleed(bleed)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                // El blur se aplica a la imagen sola: el scrim de encima tiene
                // que quedar nítido para que el degradado sea uniforme.
                .blur(radius = ArtistBackdropBlurRadius)
        ) {
            if (!coverArtId.isNullOrBlank()) {
                AsyncImage(
                    model = rememberArtworkImageRequest(coverArtId, ArtistBackdropRequestSize),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.6f to Color.Black.copy(alpha = 0.35f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )
    }
}

/**
 * Menú de orden de los álbumes del detalle de artista (S1): año ascendente y
 * descendente, nombre, más tocadas por historial local y aleatorio.
 */
@Composable
private fun ArtistSortMenu(
    sortOrder: AlbumSortOrder,
    onSortOrderChange: (AlbumSortOrder) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(text = albumSortOrderLabel(sortOrder), fontSize = 14.sp)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AlbumSortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(albumSortOrderLabel(order)) },
                    onClick = {
                        expanded = false
                        onSortOrderChange(order)
                    },
                    trailingIcon = {
                        if (order == sortOrder) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}

/** Texto visible de cada criterio de orden, resuelto en composición. */
@Composable
private fun albumSortOrderLabel(order: AlbumSortOrder): String = stringResource(
    when (order) {
        AlbumSortOrder.YEAR_ASC -> R.string.artist_sort_year_asc
        AlbumSortOrder.YEAR_DESC -> R.string.artist_sort_year_desc
        AlbumSortOrder.TITLE_ASC -> R.string.artist_sort_title
        AlbumSortOrder.MOST_PLAYED -> R.string.artist_sort_most_played
        AlbumSortOrder.RANDOM -> R.string.artist_sort_random
    }
)

/** Chips de año: "Todos" más un chip por año presente en los álbumes. */
@Composable
private fun ArtistYearFilterRow(
    availableYears: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedYear == null,
            onClick = { onYearSelected(null) },
            label = { Text(stringResource(R.string.artist_filter_all_years)) },
            // El chip seleccionado usa el color de acento de la app,
            // no el gris por defecto del sistema.
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        availableYears.forEach { year ->
            FilterChip(
                selected = selectedYear == year,
                // Segundo tap sobre el año ya seleccionado deselecciona
                // y vuelve a mostrar todos, igual que pulsar Todos.
                onClick = { onYearSelected(if (selectedYear == year) null else year) },
                label = { Text(year.toString()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
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