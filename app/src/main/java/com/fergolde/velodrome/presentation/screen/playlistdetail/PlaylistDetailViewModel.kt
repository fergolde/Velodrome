package com.fergolde.velodrome.presentation.screen.playlistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Playlist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.PlaylistUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import com.fergolde.velodrome.util.shuffledWithArtistSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentTrackId: String? = null
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistUseCases: PlaylistUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()

        viewModelScope.launch {
            playerManager.currentTrackId.collect { trackId ->
                _uiState.update { it.copy(currentTrackId = trackId) }
            }
        }
    }

    private fun loadPlaylist() {
        if (playlistId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid playlist ID") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            playlistUseCases.getPlaylist(playlistId)
                .onSuccess { playlist ->
                    _uiState.update {
                        it.copy(
                            playlist = playlist,
                            tracks = playlist.tracks,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun playTrack(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.setLoadMoreCallback { }
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
        if (playerManager.playlist.value.isEmpty()) playNow(track)
        else playerManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        smartRadioEngine.stopRadio()
        if (playerManager.playlist.value.isEmpty()) playNow(track)
        else playerManager.addToQueue(track)
    }

    fun playAll() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        // Playlists are finite server lists: never let a stale radio callback
        // append songs after the last one.
        playerManager.setLoadMoreCallback { }
        playerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun shuffleAll() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        playerManager.setLoadMoreCallback { }
        playerManager.setPlaylist(tracks.shuffledWithArtistSpacing(), startPlaying = true)
    }

    /** Instant mix seeded by this track's features (genre/artist). */
    fun startInstantMix(trackId: String) {
        smartRadioEngine.stopRadio()
        smartRadioEngine.startRadio(RadioContext.Song(trackId))
    }

    fun addAllToQueue() {
        smartRadioEngine.stopRadio()
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        playerManager.addToQueue(tracks)
    }
}
