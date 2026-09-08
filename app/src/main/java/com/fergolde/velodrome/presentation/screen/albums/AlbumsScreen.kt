package com.fergolde.velodrome.presentation.screen.albums

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.paging.LoadState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.paging.compose.itemKey
import com.fergolde.velodrome.R
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.presentation.components.UniversalOptionsSheet
import com.fergolde.velodrome.presentation.components.VeloSearchBar
import com.fergolde.velodrome.presentation.screen.home.AlbumCover
import com.fergolde.velodrome.ui.theme.DmSansFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onAlbumClick: (Album) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagedAlbums = viewModel.pagedAlbums.collectAsLazyPagingItems()

    var showOptions by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    val sheetState = rememberModalBottomSheetState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val gridColumns = when {
        isLandscape -> GridCells.Adaptive(180.dp)
        isTablet -> GridCells.Fixed(4)
        else -> GridCells.Fixed(3)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 15.dp)
        ) {
            VeloSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClearClick = { viewModel.onSearchQueryChange("") },
                hint = stringResource(R.string.albums_search_hint),
            )
            Spacer(modifier = Modifier.height(36.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isSearching -> {
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
                    }

                    pagedAlbums.loadState.refresh is LoadState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    pagedAlbums.loadState.refresh is LoadState.Error -> {
                        val error = (pagedAlbums.loadState.refresh as LoadState.Error).error
                        PagingErrorMessage(
                            message = error.localizedMessage ?: stringResource(R.string.error_loading),
                            onRetry = { pagedAlbums.retry() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    pagedAlbums.itemCount == 0 -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.albums_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = DmSansFontFamily,
                                fontSize = 14.sp
                            )
                        }
                    }

                    else -> {
                        LazyVerticalGrid(
                            columns = gridColumns,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = pagedAlbums.itemCount,
                                key = pagedAlbums.itemKey { it.id }
                            ) { index ->
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

                            pagedAlbums.apply {
                                when (loadState.append) {
                                    is LoadState.Loading -> item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    is LoadState.Error -> {
                                        val error = (loadState.append as LoadState.Error).error
                                        item {
                                            PagingErrorMessage(
                                                message = error.localizedMessage ?: stringResource(R.string.error_loading),
                                                onRetry = { pagedAlbums.retry() },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }

                                    else -> {}
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
private fun PagingErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = DmSansFontFamily,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = stringResource(R.string.retry),
                fontFamily = DmSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
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