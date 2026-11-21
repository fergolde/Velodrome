package com.fergolde.velodrome.presentation.screen.explore

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberRangeSliderState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.presentation.components.UniversalOptionsSheet
import com.fergolde.velodrome.presentation.components.VeloListBottomClearance
import com.fergolde.velodrome.presentation.components.VeloSheetShape
import com.fergolde.velodrome.presentation.components.VeloSearchBar
import com.fergolde.velodrome.presentation.components.VeloSearchDropdown
import com.fergolde.velodrome.presentation.components.VeloSectionHeader
import com.fergolde.velodrome.presentation.components.VeloSnackbarHost
import com.fergolde.velodrome.presentation.components.adaptiveGridColumns
import com.fergolde.velodrome.presentation.components.currentWindowWidthSizeClass
import com.fergolde.velodrome.presentation.screen.home.AlbumCover
import com.fergolde.velodrome.presentation.screen.home.ArtistAvatar
import com.fergolde.velodrome.presentation.screen.home.ArtworkPrefetcher
import com.fergolde.velodrome.presentation.screen.home.VeloAlbumCard
import com.fergolde.velodrome.ui.theme.DmSansFontFamily
import com.fergolde.velodrome.ui.theme.SyneFontFamily
import com.fergolde.velodrome.ui.theme.VeloPalette
import com.fergolde.velodrome.util.formatDuration
import kotlin.math.roundToInt

// ─── SCREEN ──────────────────────────────────────────────────────────────────

@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onArtistsViewAllClick: () -> Unit = {},
    onAlbumsViewAllClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // S1: la cantidad de chips de género por fila sigue el ancho de ventana, ya
    // resuelto por el helper compartido.
    val widthSizeClass = currentWindowWidthSizeClass()
    // Host hoisteado: el snackbar se dibuja como capa sobre la lista, así no se
    // desplaza con el contenido ni queda debajo del mini-player.
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Estado del desplegable de búsqueda ─────────────────────────────────
    // El foco vive en el campo de texto, así que se observa desde aquí. La
    // posición de la barra se mide para anclar el desplegable a su borde
    // inferior, y sigue a la barra si el contenido se desplaza.
    val focusManager = LocalFocusManager.current
    var searchFieldFocused by remember { mutableStateOf(false) }
    var searchBarBounds by remember { mutableStateOf<Rect?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val closeSearchOverlay: () -> Unit = {
        searchFieldFocused = false
        focusManager.clearFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.boundsInWindow().topLeft },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = VeloListBottomClearance),
        ) {
            // ── Screen title + search bar ──────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp)) {
                    VeloSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClearClick = viewModel::clearSearch,
                        onFocusChanged = { searchFieldFocused = it },
                        modifier = Modifier.onGloballyPositioned {
                            searchBarBounds = it.boundsInWindow()
                        },
                    )
                    Spacer(Modifier.height(36.dp))
                }
            }

            // ── Search results vs browse ───────────────────────────────────────
            // El historial y las sugerencias ya no son bloques fijos de la lista:
            // se pintan en el desplegable anclado a la barra (más abajo).
            if (uiState.searchQuery.isNotBlank()) {
                item {
                    SearchResultsView(
                        searchQuery     = uiState.searchQuery,
                        searchResults   = uiState.searchResults,
                        isSearching     = uiState.isSearching,
                        onArtistClick   = onArtistClick,
                        onAlbumClick    = onAlbumClick,
                        onPlayTrackClick  = viewModel::playSearchedTrack,
                        onPlayTrackNow    = viewModel::onPlayTrackNow,
                        onPlayTrackNext   = viewModel::onPlayTrackNext,
                        onAddTrackToQueue = viewModel::onAddTrackToQueue,
                        onClearSearch   = viewModel::clearSearch,
                        snackbarHostState = snackbarHostState,
                    )
                }
            } else if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (uiState.randomArtists.isEmpty() && uiState.randomAlbums.isEmpty() && uiState.genres.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.explore_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // ── Artists carousel ───────────────────────────────────────────
                item {
                    VeloSectionHeader(
                        eyebrow  = stringResource(R.string.explore_artists_discover),
                        title    = stringResource(R.string.explore_artists),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onViewAll = onArtistsViewAllClick,
                    )
                    Spacer(Modifier.height(16.dp))
                    ArtistsCarousel(
                        artists = uiState.randomArtists,
                        onArtistClick = { onArtistClick(it.id) },
                    )
                    Spacer(Modifier.height(36.dp))
                }

                // ── Albums carousel ────────────────────────────────────────────
                item {
                    VeloSectionHeader(
                        eyebrow  = stringResource(R.string.nav_explore),
                        title    = stringResource(R.string.explore_random_albums),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onViewAll = onAlbumsViewAllClick,
                    )
                    Spacer(Modifier.height(16.dp))
                    val albumsListState = rememberLazyListState()
                    ArtworkPrefetcher(
                        state = albumsListState,
                        itemCount = uiState.randomAlbums.size,
                        artworkIdAt = { uiState.randomAlbums[it].coverUrl },
                        size = 130.dp,
                    )
                    LazyRow(
                        state = albumsListState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(uiState.randomAlbums, key = { it.id }) { album ->
                            VeloAlbumCard(
                                album = album,
                                onClick = { onAlbumClick(album.id) },
                            )
                        }
                    }
                    Spacer(Modifier.height(36.dp))
                }

                // ── Genres ────────────────────────────────────────────────────
                item {
                    VeloSectionHeader(
                        eyebrow  = stringResource(R.string.explore_all_genres),
                        title    = stringResource(R.string.explore_genres),
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    VeloGenreGrid(
                        genres         = uiState.genres,
                        selectedGenres = uiState.selectedGenres,
                        onGenreToggle  = viewModel::onGenreToggle,
                        columns        = genreGridColumns(widthSizeClass),
                        modifier       = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(36.dp))
                }

                // ── Year slider ───────────────────────────────────────────────
                item {
                    VeloYearFilter(
                        minYear      = uiState.minYear,
                        currentYear  = uiState.currentYear,
                        selectedRange = uiState.selectedYearRange,
                        onRangeChange = viewModel::onYearRangeSelected,
                        modifier     = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(36.dp))
                }

                // ── Play button ───────────────────────────────────────────────
                item {
                    val hasGenres    = uiState.selectedGenres.isNotEmpty()
                    val hasYearRange = uiState.selectedYearRange != null

                    // El texto del botón también es recurso: antes se componía con
                    // "géneros"/"Play" incrustados en el código.
                    val label = when {
                        hasGenres && hasYearRange -> stringResource(
                            R.string.explore_play_genres_and_years,
                            uiState.selectedGenres.size,
                            uiState.selectedYearRange?.start ?: 0,
                            uiState.selectedYearRange?.endInclusive ?: 0
                        )
                        hasGenres    -> stringResource(R.string.explore_play_genres, uiState.selectedGenres.size)
                        hasYearRange -> stringResource(
                            R.string.explore_play_year_range,
                            uiState.selectedYearRange?.start ?: 0,
                            uiState.selectedYearRange?.endInclusive ?: 0
                        )
                        else         -> stringResource(R.string.explore_play_plain)
                    }

                    Button(
                        onClick = { viewModel.onPlayGenres() },
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary,
                        ),
                        shape = RoundedCornerShape(25.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        // ── Desplegable anclado a la barra de búsqueda ─────────────────────────
        // Foco con query vacía -> búsquedas recientes; al escribir -> sugerencias
        // locales; elegir, tocar fuera, atrás o perder el foco lo cierra. La
        // query y el historial siguen en el ViewModel: aquí solo se decide qué
        // panel se pinta.
        val dropdownPanel = searchDropdownPanel(
            isFocused   = searchFieldFocused,
            query       = uiState.searchQuery,
            history     = uiState.searchHistory,
            suggestions = uiState.suggestions,
        )
        if (dropdownPanel != null) {
            // La posición se lee solo con el desplegable abierto: cerrado,
            // nadie observa la barra y desplazar la lista no recompone nada.
            val anchorBounds = searchBarBounds
            if (anchorBounds != null) {
                BackHandler { closeSearchOverlay() }

                VeloSearchDropdown(
                    anchorOffset = IntOffset(
                        x = (anchorBounds.left - rootOrigin.x).roundToInt(),
                        y = (anchorBounds.bottom - rootOrigin.y).roundToInt(),
                    ),
                    width = with(LocalDensity.current) { anchorBounds.width.toDp() },
                    onDismiss = closeSearchOverlay,
                ) {
                    when (dropdownPanel) {
                        is SearchDropdownPanel.RecentSearches -> SearchHistoryDropdownContent(
                            history      = dropdownPanel.queries,
                            onQueryClick = { query ->
                                closeSearchOverlay()
                                viewModel.onHistoryEntryClick(query)
                            },
                            onRemove   = viewModel::onRemoveHistoryEntry,
                            onClearAll = viewModel::onClearSearchHistory,
                        )

                        is SearchDropdownPanel.Suggestions -> SearchSuggestionsDropdownContent(
                            suggestions   = dropdownPanel.results,
                            onArtistClick = { artistId ->
                                closeSearchOverlay()
                                onArtistClick(artistId)
                            },
                            onAlbumClick = { albumId ->
                                closeSearchOverlay()
                                onAlbumClick(albumId)
                            },
                            onTrackClick = { track ->
                                closeSearchOverlay()
                                viewModel.playSearchedTrack(track)
                            },
                        )
                    }
                }
            }
        }

        VeloSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
// ─── DESPLEGABLE DE BÚSQUEDA ────────────────────────────────────────────────

/**
 * Qué pinta el desplegable anclado a la barra de búsqueda.
 *
 * Es un tipo plano a propósito: la decisión vive en [searchDropdownPanel], una
 * función pura que los tests de JVM pueden ejercitar sin Compose.
 */
internal sealed interface SearchDropdownPanel {
    /** Query en blanco y con foco: las últimas búsquedas de la cuenta. */
    data class RecentSearches(val queries: List<String>) : SearchDropdownPanel

    /** Query con texto: sugerencias por prefijo de la biblioteca local. */
    data class Suggestions(val results: SearchResults) : SearchDropdownPanel
}

/**
 * Decide el contenido del desplegable.
 *
 * Reglas: sin foco no se muestra nada (así se cierra al elegir una entrada o
 * al salir del campo); con foco y query en blanco, las búsquedas recientes;
 * con texto, las sugerencias locales. Devuelve `null` cuando no hay nada que
 * ofrecer, para no abrir un desplegable vacío.
 */
internal fun searchDropdownPanel(
    isFocused: Boolean,
    query: String,
    history: List<String>,
    suggestions: SearchResults,
): SearchDropdownPanel? = when {
    !isFocused -> null
    query.isBlank() -> history.takeIf { it.isNotEmpty() }?.let(SearchDropdownPanel::RecentSearches)
    suggestions.isEmpty -> null
    else -> SearchDropdownPanel.Suggestions(suggestions)
}

/**
 * Contenido del desplegable con la query vacía: las últimas búsquedas de la
 * cuenta activa. Tocar una la repite; se puede borrar una a una o vaciar la
 * lista entera. Todo local: no hay ninguna llamada de red.
 */
@Composable
private fun SearchHistoryDropdownContent(
    history: List<String>,
    onQueryClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.explore_search_history_title),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClearAll) {
                Text(
                    text = stringResource(R.string.explore_search_history_clear_all),
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        history.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onQueryClick(entry) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = entry,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onRemove(entry) },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.explore_search_history_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

// ─── SUGERENCIAS LOCALES ──────────────────────────────────────────────────

/**
 * Contenido del desplegable mientras se escribe: sugerencias por prefijo de la
 * biblioteca local. No dependen de la red, así que aparecen antes que los
 * resultados remotos, que siguen pintándose en la lista de debajo.
 */
@Composable
private fun SearchSuggestionsDropdownContent(
    suggestions: SearchResults,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        SearchResultLabel(stringResource(R.string.explore_search_suggestions))
        Spacer(Modifier.height(8.dp))

        suggestions.artists.forEach { artist ->
            SuggestionRow(
                icon = Icons.Default.Person,
                title = artist.name,
                subtitle = null,
                onClick = { onArtistClick(artist.id) },
            )
        }
        suggestions.albums.forEach { album ->
            SuggestionRow(
                icon = Icons.Default.Album,
                title = album.title,
                subtitle = album.artistName.takeIf { it.isNotBlank() },
                onClick = { onAlbumClick(album.id) },
            )
        }
        suggestions.tracks.forEach { track ->
            SuggestionRow(
                icon = Icons.Default.MusicNote,
                title = track.title,
                subtitle = track.artistName.takeIf { it.isNotBlank() },
                onClick = { onTrackClick(track) },
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── ARTISTS CAROUSEL ────────────────────────────────────────────────────────

@Composable
fun ArtistsCarousel(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
) {
    if (artists.isEmpty()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(6) { ArtistPlaceholder() }
        }
        return
    }
    val listState = rememberLazyListState()
    ArtworkPrefetcher(
        state = listState,
        itemCount = artists.size,
        artworkIdAt = { artists[it].coverUrl },
        size = 96.dp,
    )
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(artists, key = { it.id }) { artist ->
            VeloArtistCircle(artist = artist, onClick = { onArtistClick(artist) })
        }
    }
}

@Composable
private fun ArtistPlaceholder() {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VeloPalette.Bg4)
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(40.dp).height(10.dp).background(VeloPalette.Bg4.copy(alpha = .5f)))
    }
}

@Composable
fun VeloArtistCircle(
    artist: Artist,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(VeloPalette.Bg4),
        ) {
            ArtistAvatar(
                coverArtId = artist.coverUrl,
                contentDescription = artist.name,
                size = 96.dp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            fontFamily = DmSansFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── GENRE GRID ──────────────────────────────────────────────────────────────

/**
 * Chips de género por fila según el ancho de ventana (S1).
 *
 * La cuenta por clase la resuelve [adaptiveGridColumns]: `Compact` conserva las
 * tres columnas del teléfono y las clases de tablet suman una o tres más.
 */
internal fun genreGridColumns(widthSizeClass: WindowWidthSizeClass): Int =
    adaptiveGridColumns(widthSizeClass, compact = 3, medium = 4, expanded = 6)

/**
 * Grilla de chips de género.
 *
 * Los chips se dibujan con filas armadas a mano, no con un `LazyVerticalGrid`:
 * este composable vive dentro del `LazyColumn` de Explore y dos listas
 * perezosas anidadas en la misma dirección se miden con altura infinita.
 *
 * @param columns chips por fila; el llamador lo deriva del ancho de ventana con
 *   [genreGridColumns]. El valor por defecto mantiene las tres columnas de
 *   teléfono para cualquier llamador que no piense en el tamaño.
 */
@Composable
fun VeloGenreGrid(
    genres: List<String>,
    modifier: Modifier = Modifier,
    selectedGenres: Set<String> = emptySet(),
    onGenreToggle: (String) -> Unit = {},
    columns: Int = 3,
) {
    val columnCount = columns.coerceAtLeast(1)

    if (genres.isEmpty()) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(columnCount) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(VeloPalette.Bg4)
                        )
                    }
                }
            }
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        genres.chunked(columnCount).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { genre ->
                    VeloGenreChip(
                        genre      = genre,
                        isSelected = selectedGenres.contains(genre),
                        onClick    = { onGenreToggle(genre) },
                        modifier   = Modifier.weight(1f),
                    )
                }
                // fill remaining columns
                repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun VeloGenreChip(
    genre: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else VeloPalette.Bg3
    val borderColor = if (isSelected) Color.Transparent else VeloPalette.Border
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .height(48.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            // `selectable` publica el estado seleccionado y el rol: sin ellos el
            // lector de pantalla no distingue un chip activo de uno apagado.
            .selectable(
                selected = isSelected,
                role = Role.Checkbox,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = genre,
                fontFamily = DmSansFontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

// ─── YEAR FILTER ─────────────────────────────────────────────────────────────

@Composable
fun VeloYearFilter(
    minYear: Int,
    currentYear: Int,
    selectedRange: IntRange?,
    onRangeChange: (IntRange?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sliderPosition by remember {
        mutableStateOf(minYear.toFloat()..currentYear.toFloat())
    }

    LaunchedEffect(minYear) {
        if (minYear > 0) {
            sliderPosition = selectedRange?.let {
                it.first.toFloat()..it.last.toFloat()
            } ?: (minYear.toFloat()..currentYear.toFloat())
        }
    }

    val isFiltered = selectedRange != null &&
            (selectedRange.first > minYear || selectedRange.last < currentYear)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.explore_range_years),
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.explore_years),
                    fontFamily = SyneFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                // Active range tag
                if (isFiltered) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "${sliderPosition.start.toInt()} – ${sliderPosition.endInclusive.toInt()}",
                            fontFamily = DmSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            sliderPosition = minYear.toFloat()..currentYear.toFloat()
                            onRangeChange(null)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.explore_clear_year_filter),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // El overload con `value` está deprecado en Material3 1.5.0: el ancla la
        // define el estado (RangeSliderState). sliderPosition sigue siendo la
        // fuente de verdad de la UI y se sincroniza al estado en cada
        // recomposición. La key recrea el estado al llegar los límites reales
        // (cargan asíncronos), porque trackRange es inmutable.
        key(minYear, currentYear) {
            val sliderState = rememberRangeSliderState(
                steps = (currentYear - minYear - 1).coerceAtLeast(0),
                trackRange = minYear.toFloat()..currentYear.toFloat(),
            )
            sliderState.startValue = sliderPosition.start
            sliderState.endValue = sliderPosition.endInclusive
            RangeSlider(
                state = sliderState,
                onValueChange = { sliderPosition = it },
                onValueChangeFinished = {
                    val from = sliderPosition.start.toInt()
                    val to = sliderPosition.endInclusive.toInt()
                    onRangeChange(if (from == minYear && to == currentYear) null else from..to)
                },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = VeloPalette.Bg4,
                ),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "${sliderPosition.start.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${sliderPosition.endInclusive.toInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── SEARCH RESULTS ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsView(
    searchQuery: String,
    searchResults: SearchResults,
    isSearching: Boolean,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onPlayTrackClick: (Track) -> Unit = {},
    onPlayTrackNow: (Track) -> Unit = {},
    onPlayTrackNext: (Track) -> Unit = {},
    onAddTrackToQueue: (Track) -> Unit = {},
    onClearSearch: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
) {
    var showOptions by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val msg_play_next = stringResource(R.string.snackbar_play_next)
    val msg_added_to_queue = stringResource(R.string.snackbar_added_to_queue)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        when {
            isSearching -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            searchResults.isEmpty -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                    Text(
                        stringResource(R.string.explore_search_empty, searchQuery),
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            else -> {
                if (searchResults.artists.isNotEmpty()) {
                    Column {
                        SearchResultLabel(stringResource(R.string.explore_artists))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(searchResults.artists.take(10), key = { it.id }) { artist ->
                                VeloArtistCircle(artist = artist, onClick = { onArtistClick(artist.id) })
                            }
                        }
                    }
                }

                if (searchResults.albums.isNotEmpty()) {
                    Column {
                        SearchResultLabel(stringResource(R.string.artist_detail_albums))
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(searchResults.albums.take(10), key = { it.id }) { album ->
                                VeloAlbumCard(album = album, artSize = 110.dp, onClick = { onAlbumClick(album.id) })
                            }
                        }
                    }
                }

                if (searchResults.tracks.isNotEmpty()) {
                    Column {
                        SearchResultLabel(stringResource(R.string.explore_search_tracks))
                        Spacer(Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            searchResults.tracks.take(25).forEach { track ->
                                VeloTrackRow(
                                    track = track,
                                    onClick = { onPlayTrackClick(track) },
                                    onLongClick = {
                                        selectedTrack = track
                                        showOptions = true
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptions && selectedTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptions = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = VeloSheetShape,
        ) {
            UniversalOptionsSheet(
                title       = selectedTrack!!.title,
                subtitle    = selectedTrack!!.artistName,
                coverArtId  = selectedTrack!!.coverArtId,
                onPlayNow   = { onPlayTrackNow(selectedTrack!!); showOptions = false },
                onPlayNext  = {
                    onPlayTrackNext(selectedTrack!!)
                    showOptions = false
                    scope.launch { snackbarHostState.showSnackbar(msg_play_next) }
                },
                onAddToQueue = {
                    onAddTrackToQueue(selectedTrack!!)
                    showOptions = false
                    scope.launch { snackbarHostState.showSnackbar(msg_added_to_queue) }
                },
            )
        }
    }
}

@Composable
private fun SearchResultLabel(text: String) {
    Text(
        text = text,
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

// ─── TRACK ROW ───────────────────────────────────────────────────────────────

@Composable
fun VeloTrackRow(
    track: Track,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val rowBorder = remember {
        BorderStroke(1.dp, androidx.compose.ui.graphics.SolidColor(VeloPalette.Border))
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        color = VeloPalette.Bg3,
        border = rowBorder,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumCover(
                coverArtId         = track.coverArtId,
                contentDescription = track.albumName,
                size               = 46.dp,
                cornerRadius       = 10.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = track.artistName,
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = formatDuration(track.durationSec),
                fontFamily = DmSansFontFamily,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 10.dp),
            )
        }
    }
}
