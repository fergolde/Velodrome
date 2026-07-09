package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.GetArtistUseCase
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.player.PlayerManager
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
    val isPreparingPlayback: Boolean = false, // <-- Agregado
    val error: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getArtistUseCase: GetArtistUseCase,
    private val trackUseCases: TrackUseCases, // <-- Inyectado
    private val playerManager: PlayerManager  // <-- Inyectado
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

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            getArtistUseCase(artistId)
                .onSuccess { artistWithAlbums ->
                    // Ordenar álbumes por año ascendente (más viejo a más reciente)
                    val sortedAlbums = artistWithAlbums.albums.sortedBy { it.year ?: 0 }
                    _uiState.update { it.copy(
                        artist = artistWithAlbums.artist,
                        albums = sortedAlbums,
                        isLoading = false
                    ) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    // Lógica para obtener todas las canciones de todos los álbumes
    private suspend fun gatherAllArtistTracks(): List<Track> = coroutineScope {
        val albums = _uiState.value.albums

        // 1. Sincronizar todos los álbumes en paralelo
        albums.map { album ->
            async { trackUseCases.syncTracksForAlbum(album.id) }
        }.awaitAll()

        // 2. Obtener las canciones de la DB local
        val allTracks = albums.map { album ->
            trackUseCases.observeTracksByAlbum(album.id).first()
        }.flatten()

        allTracks
    }

    fun playAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = gatherAllArtistTracks()
            if (tracks.isNotEmpty()) {
                playerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
            }
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }

    fun shuffleAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = gatherAllArtistTracks()
            if (tracks.isNotEmpty()) {
                playerManager.setPlaylist(tracks.shuffled(), startIndex = 0, startPlaying = true)
            }
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }

    fun addToQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            val tracks = gatherAllArtistTracks()
            tracks.shuffled().forEach { track ->
                playerManager.addToQueue(track)
            }
            _uiState.update { it.copy(isPreparingPlayback = false) }
        }
    }
}