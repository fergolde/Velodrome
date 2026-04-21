package com.example.velodrome.presentation.screen.artists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.mapper.toDomain
import com.example.velodrome.data.sync.SyncManager
import com.example.velodrome.domain.model.Artist
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
    val isSyncing: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    private val localMusicDataSource: LocalMusicDataSource,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistsUiState())
    val uiState: StateFlow<ArtistsUiState> = _uiState.asStateFlow()

    private var allArtists: List<Artist> = emptyList()

    init {
        observeArtists()
        triggerSyncIfNeeded()
    }

    private fun observeArtists() {
        viewModelScope.launch {
            localMusicDataSource.observeAllArtists().collect { entities ->
                allArtists = entities.map { it.toDomain() }
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

    private fun triggerSyncIfNeeded() {
        viewModelScope.launch {
            val count = localMusicDataSource.getArtistCount()
            Log.d(TAG, "Local artist count: $count")

            if (count == 0) {
                Log.d(TAG, "No artists in local DB, triggering sync...")
                _uiState.update { it.copy(isSyncing = true) }

                val result = syncManager.syncArtists()
                when (result) {
                    is com.example.velodrome.data.sync.SyncResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                error = "Failed to sync artists: ${result.message}"
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

    fun loadArtists() {
        triggerSyncIfNeeded()
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