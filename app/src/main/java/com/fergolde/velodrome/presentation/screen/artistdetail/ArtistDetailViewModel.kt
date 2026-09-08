package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


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
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine
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
            artistUseCases.getArtist(artistId)
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
    private suspend fun gatherAllArtistTracks(): List<Track> = supervisorScope {
        val albums = _uiState.value.albums
        val albumIds = albums.map { it.id }
        if (albumIds.isEmpty()) return@supervisorScope emptyList()

        // 1. Sincronizar todos los álbumes en paralelo con fan-out acotado
        val semaphore = Semaphore(5)
        albums.map { album ->
            async {
                semaphore.withPermit { trackUseCases.syncTracksForAlbum(album.id) }
            }
        }.awaitAll()

        // 2. Obtener las canciones de la DB local en una sola query IN
        trackUseCases.getTracksForAlbumIds(albumIds)
    }

    /** Smart artist radio: dense on this artist, opening toward taste affinities. */
    fun startArtistRadio() {
        val name = uiState.value.artist?.name ?: return
        smartRadioEngine.stopRadio()
        smartRadioEngine.startRadio(RadioContext.Artist(artistName = name))
        // Smart radio prepares tracks asynchronously; do not lock the UI flag here.
    }

    fun playAll() = prepareTracks { tracks ->
        playerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun shuffleAll() = prepareTracks { tracks ->
        playerManager.setPlaylist(tracks.shuffled(), startIndex = 0, startPlaying = true)
    }

    fun addToQueue() = prepareTracks { tracks ->
        tracks.shuffled().forEach { track ->
            playerManager.addToQueue(track)
        }
    }

    private fun prepareTracks(block: suspend (List<Track>) -> Unit) {
        smartRadioEngine.stopRadio()
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            try {
                val tracks = gatherAllArtistTracks()
                if (tracks.isNotEmpty()) {
                    block(tracks)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isPreparingPlayback = false) }
            }
        }
    }
}