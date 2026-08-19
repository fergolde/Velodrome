package com.fergolde.velodrome.presentation.screen.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.repository.ArtistRepository
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.domain.usecase.GetArtistUseCase
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Artist> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository,
    private val getArtistUseCase: GetArtistUseCase,
    private val playerManager: PlayerManager,
    private val trackUseCases: TrackUseCases,
    private val settingsRepository: SettingsRepository,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    val aiRadioEnabled: StateFlow<Boolean> = settingsRepository.aiRadioEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val radioError: StateFlow<String?> = smartRadioEngine.error

    fun clearRadioError() {
        smartRadioEngine.clearError()
    }

    fun generateArtistRadio(artistId: String) {
        smartRadioEngine.startRadio(RadioContext.AiSimilar(artistId))
    }

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    val pagedArtists: Flow<PagingData<Artist>> = Pager(
        config = PagingConfig(pageSize = 20, prefetchDistance = 5, enablePlaceholders = false),
        pagingSourceFactory = { artistRepository.getArtistsPaged() }
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
                    flowOf(artistRepository.searchLocal(query))
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

    fun onPlayArtistNow(artist: Artist) {
        smartRadioEngine.stopRadio()
        viewModelScope.launch {
            val artistWithAlbums = getArtistUseCase(artist.id).getOrNull() ?: return@launch
            val albums = artistWithAlbums.albums

            // Sync all albums and gather all tracks
            albums.map { album -> async { trackUseCases.syncTracksForAlbum(album.id) } }.awaitAll()

            val allTracks = albums.flatMap { album ->
                trackUseCases.observeTracksByAlbum(album.id).first()
            }

            playerManager.playNow(allTracks)
        }
    }

    fun onPlayArtistNext(artist: Artist) {
        smartRadioEngine.stopRadio()
        viewModelScope.launch {
            val artistWithAlbums = getArtistUseCase(artist.id).getOrNull() ?: return@launch
            val albums = artistWithAlbums.albums

            albums.map { album -> async { trackUseCases.syncTracksForAlbum(album.id) } }.awaitAll()

            val allTracks = albums.flatMap { album ->
                trackUseCases.observeTracksByAlbum(album.id).first()
            }

            playerManager.playNext(allTracks)
        }
    }

    fun onAddArtistToQueue(artist: Artist) {
        smartRadioEngine.stopRadio()
        viewModelScope.launch {
            val artistWithAlbums = getArtistUseCase(artist.id).getOrNull() ?: return@launch
            val albums = artistWithAlbums.albums

            albums.map { album -> async { trackUseCases.syncTracksForAlbum(album.id) } }.awaitAll()

            val allTracks = albums.flatMap { album ->
                trackUseCases.observeTracksByAlbum(album.id).first()
            }

            playerManager.addToQueue(allTracks)
        }
    }
}