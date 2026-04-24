package com.example.velodrome.presentation.screen.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.repository.ArtistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ArtistsViewModel"

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

    private var allArtists: List<Artist> = emptyList()

    init {
        observeArtists()
    }

    private fun observeArtists() {
        viewModelScope.launch {
            artistRepository.observeAllArtists().collect { artists ->
                allArtists = artists
                val filtered = filterArtists(allArtists, _uiState.value.searchQuery)
                _uiState.update {
                    it.copy(
                        artists = filtered,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = filterArtists(allArtists, query)
        _uiState.update { it.copy(searchQuery = query, artists = filtered) }
    }

    private fun filterArtists(artists: List<Artist>, query: String): List<Artist> {
        if (query.isBlank()) {
            return artists
        }
        val lowerQuery = query.lowercase()
        return artists.filter { artist ->
            artist.name.lowercase().contains(lowerQuery)
        }
    }
}