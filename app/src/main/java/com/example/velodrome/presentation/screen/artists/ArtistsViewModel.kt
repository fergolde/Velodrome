package com.example.velodrome.presentation.screen.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        observeArtists()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeArtists() {
        viewModelScope.launch {
            searchQuery.flatMapLatest { query ->
                if (query.isBlank()) {
                    artistRepository.observeAllArtists()
                } else {
                    flowOf(artistRepository.searchLocal(query))
                }
            }.collect { artists ->
                _uiState.update {
                    it.copy(
                        artists = artists,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }
}