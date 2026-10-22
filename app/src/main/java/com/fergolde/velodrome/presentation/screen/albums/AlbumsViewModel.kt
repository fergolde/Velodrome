package com.fergolde.velodrome.presentation.screen.albums

import androidx.compose.runtime.Immutable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.AlbumRepository
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class AlbumsUiState(
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

    private var pagingSource: androidx.paging.PagingSource<Int, Album>? = null

    val pagedAlbums: Flow<PagingData<Album>> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
        pagingSourceFactory = {
            albumRepository.getAlbumsPaged().also { pagingSource = it }
        }
    ).flow.cachedIn(viewModelScope)

    init {
        observeSearch()
        observeLibraryChanges()
    }

    /**
     * The custom PagingSource bypasses Room's automatic invalidation, so a
     * library sync would otherwise stay invisible until process restart.
     */
    @OptIn(FlowPreview::class)
    private fun observeLibraryChanges() {
        viewModelScope.launch {
            albumRepository.observeAlbumsChanged()
                .drop(1) // Skip the initial count snapshot.
                .debounce(500L.milliseconds)
                .collect { pagingSource?.invalidate() }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(500L.milliseconds) // Espera 500 ms tras dejar de escribir
                .distinctUntilChanged() // No busca si la query es idéntica a la anterior
                .flatMapLatest { query ->
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

    private suspend fun loadAlbumTracks(album: Album): List<Track> {
        trackUseCases.syncTracksForAlbum(album.id)
        return trackUseCases.observeTracksByAlbum(album.id).first().sortedBy { it.trackNumber }
    }

    fun onPlayAlbumNow(album: Album) {
        viewModelScope.launch {
            playerManager.playNow(loadAlbumTracks(album))
        }
    }

    fun onPlayAlbumNext(album: Album) {
        viewModelScope.launch {
            playerManager.playNext(loadAlbumTracks(album))
        }
    }

    fun onAddAlbumToQueue(album: Album) {
        viewModelScope.launch {
            playerManager.addToQueue(loadAlbumTracks(album))
        }
    }
}