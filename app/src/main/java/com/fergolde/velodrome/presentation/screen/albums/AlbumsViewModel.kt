package com.fergolde.velodrome.presentation.screen.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.repository.AlbumRepository
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Album> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    private val playerManager: PlayerManager,
    private val trackUseCases: TrackUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    val pagedAlbums: Flow<PagingData<Album>> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
        pagingSourceFactory = { albumRepository.getAlbumsPaged() }
    ).flow.cachedIn(viewModelScope)

    init {
        observeSearch()
        _uiState.update { it.copy(isLoading = false) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    flowOf(albumRepository.searchLocal(query))
                }
            }.collect { results ->
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = searchQuery.value.isNotBlank()
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query, isSearching = query.isNotBlank()) }
    }

    fun onPlayAlbumNow(album: Album) {
        viewModelScope.launch {
            trackUseCases.syncTracksForAlbum(album.id)
            trackUseCases.observeTracksByAlbum(album.id).first().let { tracks ->
                playerManager.playNow(tracks.sortedBy { it.trackNumber })
            }
        }
    }

    fun onPlayAlbumNext(album: Album) {
        viewModelScope.launch {
            trackUseCases.syncTracksForAlbum(album.id)
            trackUseCases.observeTracksByAlbum(album.id).first().let { tracks ->
                playerManager.playNext(tracks.sortedBy { it.trackNumber })
            }
        }
    }

    fun onAddAlbumToQueue(album: Album) {
        viewModelScope.launch {
            trackUseCases.syncTracksForAlbum(album.id)
            trackUseCases.observeTracksByAlbum(album.id).first().let { tracks ->
                playerManager.addToQueue(tracks.sortedBy { it.trackNumber })
            }
        }
    }
}