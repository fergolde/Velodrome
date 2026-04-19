package com.example.velodrome.presentation.screen.albumdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.GetAlbumUseCase
import com.example.velodrome.domain.usecase.GetTracksUseCase
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AlbumDetailViewModel"

data class AlbumDetailUiState(
    val album: Album? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAlbumUseCase: GetAlbumUseCase,
    private val getTracksUseCase: GetTracksUseCase
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>("albumId") ?: ""

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        loadAlbumData()
    }

    private fun loadAlbumData() {
        if (albumId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid album ID") }
            return
        }

        Log.d(TAG, "Loading album data for: $albumId")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // Load album details
            getAlbumUseCase(albumId)
                .onSuccess { album ->
                    Log.d(TAG, "Loaded album: ${album.title}")
                    _uiState.update { it.copy(album = album) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading album: ${e.message}")
                    _uiState.update { it.copy(error = e.message) }
                }
        }

        viewModelScope.launch {
            // Load tracks - already sorted by track number from API
            getTracksUseCase(albumId)
                .onSuccess { tracks ->
                    // Sort by track number ascending
                    val sortedTracks = tracks.sortedBy { it.trackNumber }
                    Log.d(TAG, "Loaded ${tracks.size} tracks")
                    _uiState.update { it.copy(tracks = sortedTracks, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading tracks: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun playTrack(track: Track) {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        val trackIndex = tracks.indexOf(track)
        if (trackIndex >= 0) {
            Log.d(TAG, "Playing track: ${track.title} at index $trackIndex")
            PlayerManager.setPlaylist(tracks, startIndex = trackIndex, startPlaying = true)
        }
    }

    fun playAll() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        Log.d(TAG, "Playing all ${tracks.size} tracks")
        PlayerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun shuffleAll() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        Log.d(TAG, "Shuffling and playing ${tracks.size} tracks")
        val shuffled = tracks.shuffled()
        PlayerManager.setPlaylist(shuffled, startIndex = 0, startPlaying = true)
    }

    fun addAllToQueue() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return

        Log.d(TAG, "Adding all ${tracks.size} tracks to queue")
        tracks.forEach { track ->
            PlayerManager.addToQueue(track)
        }
    }

    fun playNext(track: Track) {
        Log.d(TAG, "Play next: ${track.title}")
        PlayerManager.playNext(track)
    }

    fun addToQueue(track: Track) {
        Log.d(TAG, "Add to queue: ${track.title}")
        PlayerManager.addToQueue(track)
    }

    fun retry() {
        loadAlbumData()
    }
}