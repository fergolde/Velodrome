package com.example.velodrome.presentation.screen.albums

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.components.UniversalOptionsSheet
import com.example.velodrome.presentation.components.VeloSearchBar
import com.example.velodrome.presentation.screen.home.AlbumCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onAlbumClick: (Album) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedAlbums = viewModel.pagedAlbums.collectAsLazyPagingItems()

    var showOptions by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val gridColumns = when {
        isLandscape -> GridCells.Adaptive(180.dp)
        screenWidthDp >= 600 -> GridCells.Fixed(4)
        else -> GridCells.Fixed(3)
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
                        hint = stringResource(R.string.albums_search_hint),
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
                            items(uiState.searchResults, key = { it.id }) { album ->
                                AlbumGridCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    onLongClick = {
                                        selectedAlbum = album
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
                            items(count = pagedAlbums.itemCount) { index ->
                                val album = pagedAlbums[index]
                                if (album != null) {
                                    AlbumGridCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) },
                                        onLongClick = {
                                            selectedAlbum = album
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

        if (showOptions && selectedAlbum != null) {
            ModalBottomSheet(
                onDismissRequest = { showOptions = false },
                sheetState = sheetState
            ) {
                UniversalOptionsSheet(
                    title = selectedAlbum!!.title,
                    subtitle = "Album - ${selectedAlbum!!.artistName}",
                    coverArtId = selectedAlbum!!.coverUrl,
                    onPlayNow = {
                        viewModel.onPlayAlbumNow(selectedAlbum!!)
                        showOptions = false
                    },
                    onPlayNext = {
                        viewModel.onPlayAlbumNext(selectedAlbum!!)
                        showOptions = false
                    },
                    onAddToQueue = {
                        viewModel.onAddAlbumToQueue(selectedAlbum!!)
                        showOptions = false
                    }
                )
            }
        }
    }
}

@Composable
fun AlbumGridCard(album: Album, onClick: () -> Unit = {}, onLongClick: () -> Unit = {}) {
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
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AlbumCover(
                    coverArtId = album.coverUrl,
                    contentDescription = album.title,
                    size = 200.dp,
                    cornerRadius = 8.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = album.title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artistName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}