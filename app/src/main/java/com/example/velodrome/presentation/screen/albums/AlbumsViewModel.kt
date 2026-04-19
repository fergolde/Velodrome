package com.example.velodrome.presentation.screen.albums

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
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
    val searchQuery: String = ""
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val getRandomAlbumsUseCase: GetRandomAlbumsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        Log.d(TAG, "Loading albums...")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getRandomAlbumsUseCase(size = 100)
                .onSuccess { albums ->
                    Log.d(TAG, "Loaded ${albums.size} albums")
                    albums.forEach { album ->
                        Log.d(TAG, "Album: ${album.title}, coverUrl: ${album.coverUrl}")
                    }
                    _uiState.update { it.copy(albums = albums, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading albums: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onAlbumClick(album: Album) {
        Log.d(TAG, "Album clicked: ${album.title}")
        // TODO: navigate to album detail
    }
}