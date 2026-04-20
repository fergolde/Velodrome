package com.example.velodrome.presentation.screen.albums

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.sync.SyncManager
import com.example.velodrome.domain.model.Album
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
    val isSyncing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val localMusicDataSource: LocalMusicDataSource,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumsUiState())
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private var allAlbums: List<Album> = emptyList()

    init {
        observeAlbums()
        triggerSyncIfNeeded()
    }

    private fun observeAlbums() {
        viewModelScope.launch {
            localMusicDataSource.observeAllAlbums().collect { entities ->
                allAlbums = entities.map { it.toDomain() }
                val filtered = filterAlbums(allAlbums, _uiState.value.searchQuery)
                _uiState.update {
                    it.copy(
                        albums = filtered,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private fun triggerSyncIfNeeded() {
        viewModelScope.launch {
            val count = localMusicDataSource.getAlbumCount()
            Log.d(TAG, "Local album count: $count")

            if (count == 0) {
                Log.d(TAG, "No albums in local DB, triggering sync...")
                _uiState.update { it.copy(isSyncing = true) }

                val result = syncManager.syncAlbums()
                when (result) {
                    is com.example.velodrome.data.sync.SyncResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                error = "Failed to sync albums: ${result.message}"
                            )
                        }
                    }
                    is com.example.velodrome.data.sync.SyncResult.Success -> {
                        _uiState.update { it.copy(isSyncing = false) }
                    }
                }
            }
        }
    }

    fun loadAlbums() {
        triggerSyncIfNeeded()
    }

    fun onSearchQueryChange(query: String) {
        val filtered = filterAlbums(allAlbums, query)
        _uiState.update { it.copy(searchQuery = query, albums = filtered) }
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