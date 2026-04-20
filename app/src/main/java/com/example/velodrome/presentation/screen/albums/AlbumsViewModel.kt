package com.example.velodrome.presentation.screen.albums

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.usecase.GetAllAlbumsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val filteredAlbums: List<Album> = emptyList()
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getAllAlbumsUseCase: GetAllAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    // Original albums loaded from server (without filtering)
    private var allAlbums: List<Album> = emptyList()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        Log.d(TAG, "Loading albums...")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getAllAlbumsUseCase(size = 100)
                .onSuccess { albums ->
                    Log.d(TAG, "Loaded ${albums.size} albums")
                    allAlbums = albums
                    val filtered = filterAlbums(albums, _uiState.value.searchQuery)
                    _uiState.update { it.copy(albums = filtered, filteredAlbums = filtered, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading albums: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = filterAlbums(allAlbums, query)
        _uiState.update { it.copy(searchQuery = query, albums = filtered, filteredAlbums = filtered) }
    }

    private fun filterAlbums(albums: List<Album>, query: String): List<Album> {
        if (query.isBlank()) {
            return albums
        }
        val lowerQuery = query.lowercase()
        Log.d(TAG, "Filtering by query: '$lowerQuery'")
        val filtered = albums.filter { album ->
            val titleMatch = album.title?.lowercase()?.contains(lowerQuery) == true
            val artistMatch = album.artistName?.lowercase()?.contains(lowerQuery) == true
            if (titleMatch || artistMatch) {
                Log.d(TAG, "Match: ${album.title} by ${album.artistName}")
            }
            titleMatch || artistMatch
        }
        Log.d(TAG, "Found ${filtered.size} albums matching '$query'")
        return filtered
    }
}