package com.example.velodrome.presentation.screen.artistdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.GetArtistUseCase
import com.example.velodrome.domain.usecase.TrackUseCases
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ArtistDetailViewModel"

data class ArtistDetailUiState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val isPreparingPlayback: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArtistUseCase: GetArtistUseCase,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>("artistId") ?: ""

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        loadArtistData()
    }

    private fun loadArtistData() {
        if (artistId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid artist ID") }
            return
        }

        Log.d(TAG, "Loading artist data for: $artistId")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getArtistUseCase(artistId)
                .onSuccess { artistWithAlbums ->
                    Log.d(TAG, "Loaded artist: ${artistWithAlbums.artist.name} with ${artistWithAlbums.albums.size} albums")
                    val sortedAlbums = artistWithAlbums.albums.sortedByDescending { it.year ?: 0 }
                    _uiState.update { it.copy(
                        artist = artistWithAlbums.artist,
                        albums = sortedAlbums,
                        isLoading = false
                    ) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading artist: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    private suspend fun fetchAllArtistTracks(): List<Track> = coroutineScope {
        val sortedAlbums = _uiState.value.albums.sortedBy { it.year ?: 0 }

        sortedAlbums.map { album ->
            async {
                trackUseCases.syncTracksForAlbum(album.id)
                trackUseCases.observeTracksByAlbum(album.id).first()
            }
        }.awaitAll().flatten()
    }

    fun playAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = fetchAllArtistTracks()
            playerManager.setPlaylist(tracks, startIndex = 0)
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = fetchAllArtistTracks().shuffled()
            playerManager.setPlaylist(tracks, startIndex = 0)
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }

    fun addToQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = fetchAllArtistTracks().shuffled()
            playerManager.appendToPlaylist(tracks)
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }
}