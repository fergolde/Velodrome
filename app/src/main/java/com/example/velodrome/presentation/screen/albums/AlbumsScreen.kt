package com.example.velodrome.presentation.screen.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.velodrome.R
import com.example.velodrome.domain.model.Album
import com.example.velodrome.presentation.screen.home.AlbumCover

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onAlbumClick: (Album) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagedAlbums = viewModel.pagedAlbums.collectAsLazyPagingItems()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
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
                Column(modifier = Modifier.fillMaxSize()) {
                    AlbumsSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange,
                        onClearClick = { viewModel.onSearchQueryChange("") }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.isSearching) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.searchResults) { album ->
                                AlbumCard(album = album, onClick = { onAlbumClick(album) })
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(count = pagedAlbums.itemCount) { index ->
                                val album = pagedAlbums[index]
                                if (album != null) {
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumsSearchBar(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onClearClick: () -> Unit = {}
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface),
        placeholder = {
            Text(
                stringResource(R.string.albums_search_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        shape = RoundedCornerShape(28.dp),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AlbumCover(
                    coverArtId = album.coverUrl,
                    contentDescription = album.title,
                    size = 64.dp,
                    cornerRadius = 8.dp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = album.artistName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}