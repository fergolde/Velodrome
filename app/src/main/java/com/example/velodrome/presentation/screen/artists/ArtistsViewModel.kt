package com.example.velodrome.presentation.screen.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.usecase.GetArtistsUseCase
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
    val searchQuery: String = "",
    val filteredArtists: List<Artist> = emptyList()
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val getArtistsUseCase: GetArtistsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    // Original artists loaded from server (without filtering)
    private var allArtists: List<Artist> = emptyList()

    init {
        loadArtists()
    }

    fun loadArtists() {
        Log.d(TAG, "Loading artists...")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getArtistsUseCase(offset = 0, size = 100)
                .onSuccess { artists ->
                    Log.d(TAG, "Loaded ${artists.size} artists")
                    allArtists = artists
                    val filtered = filterArtists(artists, _uiState.value.searchQuery)
                    _uiState.update { it.copy(artists = filtered, filteredArtists = filtered, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading artists: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = filterArtists(allArtists, query)
        _uiState.update { it.copy(searchQuery = query, artists = filtered, filteredArtists = filtered) }
    }

    private fun filterArtists(artists: List<Artist>, query: String): List<Artist> {
        if (query.isBlank()) {
            return artists
        }
        val lowerQuery = query.lowercase()
        return artists.filter { artist ->
            artist.name?.lowercase()?.contains(lowerQuery) == true
        }
    }
}