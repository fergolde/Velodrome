package com.fergolde.velodrome.presentation.screen.albumdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.domain.usecase.GetAlbumUseCase
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlbumUseCase: GetAlbumUseCase,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val settingsRepository: SettingsRepository,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>("albumId") ?: ""

    val aiRadioEnabled: StateFlow<Boolean> = settingsRepository.aiRadioEnabled
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    val radioError: StateFlow<String?> = smartRadioEngine.error

    fun clearRadioError() {
        smartRadioEngine.clearError()
    }

    fun generateInstantMix(trackId: String) {
        smartRadioEngine.startRadio(RadioContext.AiSimilar(trackId))
    }

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbumData()

        viewModelScope.launch {
            playerManager.currentTrackId.collect { trackId ->
                _uiState.update { it.copy(currentTrackId = trackId) }
            }
        }
        viewModelScope.launch {
            playerManager.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
        viewModelScope.launch {
            playerManager.currentPosition.collect { pos ->
                _uiState.update { it.copy(currentPosition = pos) }
            }
        }
    }

    private fun loadAlbumData() {
        if (albumId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid album ID") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Load album details
            getAlbumUseCase(albumId)
                .onSuccess { album ->
                    _uiState.update { it.copy(album = album) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }

        // Observar tracks desde DB local (instantáneo)
        viewModelScope.launch {
            trackUseCases.observeTracksByAlbum(albumId).collect { tracks ->
                val sortedTracks = tracks.sortedBy { it.trackNumber }
                _uiState.update { it.copy(tracks = sortedTracks, isLoading = false) }
            }
        }

        // Sincronizar tracks desde API en background
        viewModelScope.launch {
            trackUseCases.syncTracksForAlbum(albumId)
        }
    }

    fun playTrack(track: Track) {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        val trackIndex = tracks.indexOf(track)
        if (trackIndex >= 0) {
            playerManager.setPlaylist(tracks, startIndex = trackIndex, startPlaying = true)
        }
    }

    fun playNow(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.playNow(track)
    }

    fun playNext(track: Track) {
        smartRadioEngine.stopRadio()
        if(playerManager.playlist.value.isEmpty())    playNow(track)
        else    playerManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        smartRadioEngine.stopRadio()
        if(playerManager.playlist.value.isEmpty())    playNow(track)
        else    playerManager.addToQueue(track)
    }

    fun playAll() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        playerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun shuffleAll() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        val shuffled = tracks.shuffled()
        playerManager.setPlaylist(shuffled, startIndex = 0, startPlaying = true)
    }

    fun addAllToQueue() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        tracks.forEach { track ->
            playerManager.addToQueue(track)
        }
    }

}