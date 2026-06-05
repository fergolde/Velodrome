package com.example.velodrome.presentation.screen.artists

import android.content.res.Configuration
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.velodrome.R
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.presentation.components.UniversalOptionsSheet
import com.example.velodrome.presentation.components.VeloSearchBar
import com.example.velodrome.presentation.screen.home.ArtistAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel = hiltViewModel(),
    onArtistClick: (Artist) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedArtists = viewModel.pagedArtists.collectAsLazyPagingItems()

    var showOptions by remember { mutableStateOf(false) }
    var selectedArtist by remember { mutableStateOf<Artist?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gridColumns = when {
        isLandscape -> GridCells.Adaptive(180.dp)
        screenWidthDp >= 600 -> GridCells.Fixed(4)
        else -> GridCells.Fixed(2)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 15.dp)) {
                    VeloSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClearClick = { viewModel.onSearchQueryChange("") },
                        hint = stringResource(R.string.artists_search_hint)
                    )
                    Spacer(modifier = Modifier.height(36.dp))

                    if (uiState.isSearching) {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.searchResults, key = { it.id }) { artist ->
                                ArtistGridCard(
                                    artist = artist,
                                    onClick = { onArtistClick(artist) },
                                    onLongClick = {
                                        selectedArtist = artist
                                        showOptions = true
                                    }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(count = pagedArtists.itemCount) { index ->
                                val artist = pagedArtists[index]
                                if (artist != null) {
                                    ArtistGridCard(
                                        artist = artist,
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = {
                                            selectedArtist = artist
                                            showOptions = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showOptions && selectedArtist != null) {
            ModalBottomSheet(
                onDismissRequest = { showOptions = false },
                sheetState = sheetState
            ) {
                UniversalOptionsSheet(
                    title = selectedArtist!!.name,
                    subtitle = "Artist - ${selectedArtist!!.albumCount} albums",
                    coverArtId = selectedArtist!!.coverUrl,
                    onPlayNow = {
                        viewModel.onPlayArtistNow(selectedArtist!!)
                        showOptions = false
                    },
                    onPlayNext = {
                        viewModel.onPlayArtistNext(selectedArtist!!)
                        showOptions = false
                    },
                    onAddToQueue = {
                        viewModel.onAddArtistToQueue(selectedArtist!!)
                        showOptions = false
                    }
                )
            }
        }
    }
}

@Composable
fun ArtistGridCard(artist: Artist, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ArtistAvatar(
                coverArtId = artist.coverUrl,
                contentDescription = artist.name,
                size = 96.dp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = artist.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${artist.albumCount} ALBUMS",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}