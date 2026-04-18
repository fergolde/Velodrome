package com.example.velodrome.presentation.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.usecase.GetArtistsUseCase
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getArtistsUseCase: GetArtistsUseCase,
    private val getRandomAlbumsUseCase: GetRandomAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    fun loadContent() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            // Load random artists
            getArtistsUseCase(offset = 0, size = 20)
                .onSuccess { artists ->
                    _uiState.update { it.copy(randomArtists = artists) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
        
        viewModelScope.launch {
            // Load random albums
            getRandomAlbumsUseCase(size = 20)
                .onSuccess { albums ->
                    _uiState.update { it.copy(randomAlbums = albums) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
        
        viewModelScope.launch {
            // Load curated/albums for "Based on your activity" section
            getRandomAlbumsUseCase(size = 10)
                .onSuccess { albums ->
                    _uiState.update { it.copy(curatedAlbums = albums, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onArtistClick(artist: com.example.velodrome.domain.model.Artist) {
        // TODO: Navigate to artist detail
    }

    fun onAlbumClick(album: com.example.velodrome.domain.model.Album) {
        // TODO: Navigate to album detail
    }
}