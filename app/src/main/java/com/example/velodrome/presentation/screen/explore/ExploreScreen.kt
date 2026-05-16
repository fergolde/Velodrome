package com.example.velodrome.presentation.screen.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.components.UniversalOptionsSheet
import com.example.velodrome.presentation.components.VeloSectionHeader
import com.example.velodrome.presentation.screen.home.AlbumCover
import com.example.velodrome.presentation.screen.home.ArtistAvatar
import com.example.velodrome.presentation.screen.home.VeloAlbumCard
import com.example.velodrome.ui.theme.DmSansFontFamily
import com.example.velodrome.ui.theme.SyneFontFamily
import com.example.velodrome.ui.theme.VeloPalette

// ─── SCREEN ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onArtistsViewAllClick: () -> Unit = {},
    onAlbumsViewAllClick: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── Screen title + search bar ──────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                VeloSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClearClick = viewModel::clearSearch,
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        // ── Search results vs browse ───────────────────────────────────────
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
                )
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
                LazyRow(
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

                val label = buildString {
                    append("Play")
                    when {
                        hasGenres && hasYearRange -> {
                            append(" · ${uiState.selectedGenres.size} géneros")
                            append(", ${uiState.selectedYearRange?.start}–${uiState.selectedYearRange?.endInclusive}")
                        }
                        hasGenres    -> append(" · ${uiState.selectedGenres.size} géneros")
                        hasYearRange -> append(" · ${uiState.selectedYearRange?.start}–${uiState.selectedYearRange?.endInclusive}")
                        else         -> append(" (${stringResource(R.string.player_shuffle)})")
                    }
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
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ─── SEARCH BAR ──────────────────────────────────────────────────────────────

@Composable
fun VeloSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(VeloPalette.Border),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = DmSansFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.explore_search_hint),
                            fontFamily = DmSansFontFamily,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearClick,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Borrar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
    LazyRow(
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
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(VeloPalette.Bg4),
        ) {
            ArtistAvatar(
                coverArtId = artist.coverUrl,
                contentDescription = artist.name,
                size = 72.dp,
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

@Composable
fun VeloGenreGrid(
    genres: List<String>,
    selectedGenres: Set<String> = emptySet(),
    onGenreToggle: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (genres.isEmpty()) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .weight(1f)
                                .height(40.dp)
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
        genres.chunked(3).forEach { row ->
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
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun VeloGenreChip(
    genre: String,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary else VeloPalette.Bg3
    val borderColor = if (isSelected) Color.Transparent else VeloPalette.Border
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .height(40.dp)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
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

@OptIn(ExperimentalMaterial3Api::class)
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
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Quitar filtro",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        RangeSlider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                val from = sliderPosition.start.toInt()
                val to   = sliderPosition.endInclusive.toInt()
                onRangeChange(if (from == minYear && to == currentYear) null else from..to)
            },
            valueRange = minYear.toFloat()..currentYear.toFloat(),
            steps = (currentYear - minYear - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor          = MaterialTheme.colorScheme.primary,
                activeTrackColor    = MaterialTheme.colorScheme.primary,
                inactiveTrackColor  = VeloPalette.Bg4,
            ),
        )

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
) {
    var showOptions by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                        "Sin resultados para \"$searchQuery\"",
                        fontFamily = DmSansFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            else -> {
                if (searchResults.artists.isNotEmpty()) {
                    Column {
                        SearchResultLabel("Artistas")
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(searchResults.artists.take(10)) { artist ->
                                VeloArtistCircle(artist = artist, onClick = { onArtistClick(artist.id) })
                            }
                        }
                    }
                }

                if (searchResults.albums.isNotEmpty()) {
                    Column {
                        SearchResultLabel("Álbumes")
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(searchResults.albums.take(10)) { album ->
                                VeloAlbumCard(album = album, artSize = 110.dp, onClick = { onAlbumClick(album.id) })
                            }
                        }
                    }
                }

                if (searchResults.tracks.isNotEmpty()) {
                    Column {
                        SearchResultLabel("Canciones")
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
            containerColor = VeloPalette.Bg3,
        ) {
            UniversalOptionsSheet(
                title       = selectedTrack!!.title,
                subtitle    = selectedTrack!!.artistName,
                coverArtId  = selectedTrack!!.coverArtId,
                onPlayNow   = { onPlayTrackNow(selectedTrack!!); showOptions = false },
                onPlayNext  = { onPlayTrackNext(selectedTrack!!); showOptions = false },
                onAddToQueue = { onAddTrackToQueue(selectedTrack!!); showOptions = false },
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(14.dp),
        color = VeloPalette.Bg3,
        border = ButtonDefaults.outlinedButtonBorder(true).copy(
            brush = androidx.compose.ui.graphics.SolidColor(VeloPalette.Border),
        ),
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

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}