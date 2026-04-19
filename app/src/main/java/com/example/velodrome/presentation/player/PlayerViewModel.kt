package com.example.velodrome.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.GetStreamUrlUseCase
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlayerViewModel"

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getStreamUrlUseCase: GetStreamUrlUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null

    init {
        // Set up load more callback for infinite playlist
        PlayerManager.setLoadMoreCallback {
            Log.d(TAG, "PlayerManager requesting load more")
        }

        // Sync with PlayerManager
        viewModelScope.launch {
            PlayerManager.playlist.collect { playlist ->
                _uiState.update { it.copy(playlist = playlist) }
            }
        }
        viewModelScope.launch {
            PlayerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying && progressJob == null) {
                    startProgressSimulation()
                } else if (!isPlaying) {
                    progressJob?.cancel()
                }
            }
        }
        viewModelScope.launch {
            PlayerManager.currentPosition.collect { pos ->
                _uiState.update { it.copy(currentPosition = pos) }
            }
        }
        viewModelScope.launch {
            PlayerManager.currentIndex.collect { idx ->
                _uiState.update { state ->
                    val currentTrack = state.playlist.getOrNull(idx)
                    state.copy(currentIndex = idx, currentTrack = currentTrack)
                }
            }
        }
    }

    /**
     * Start playing a track
     */
    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        val index = playlist.indexOfFirst { it.id == track.id }
        _uiState.update { it.copy(
            currentTrack = track,
            playlist = playlist,
            currentIndex = if (index >= 0) index else 0,
            isPlaying = true
        ) }
        startProgressSimulation()
    }

    /**
     * Play/Pause toggle
     */
    fun onPlayPauseClick() {
        val isPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = isPlaying) }
        
        if (isPlaying) {
            startProgressSimulation()
        } else {
            progressJob?.cancel()
        }
    }

    /**
     * Go to previous track
     */
    fun onPreviousClick() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            val prevIndex = state.currentIndex - 1
            val prevTrack = state.playlist[prevIndex]
            _uiState.update { it.copy(
                currentTrack = prevTrack,
                currentIndex = prevIndex,
                currentPosition = 0
            ) }
        }
    }

    /**
     * Go to next track
     */
    fun onNextClick() {
        val state = _uiState.value
        if (state.currentIndex < state.playlist.size - 1) {
            val nextIndex = state.currentIndex + 1
            val nextTrack = state.playlist[nextIndex]
            _uiState.update { it.copy(
                currentTrack = nextTrack,
                currentIndex = nextIndex,
                currentPosition = 0
            ) }
            PlayerManager.next()
        }
    }

    /**
     * Select a track from the queue (jump to index)
     */
    fun onTrackSelected(index: Int) {
        val state = _uiState.value
        if (index >= 0 && index < state.playlist.size) {
            val track = state.playlist[index]
            _uiState.update { it.copy(
                currentTrack = track,
                currentIndex = index,
                currentPosition = 0,
                isPlaying = true
            ) }
            // Update PlayerManager
            PlayerManager.setCurrentIndex(index)
            startProgressSimulation()
        }
    }

    /**
     * Seek to position
     */
    fun onSeek(position: Int) {
        _uiState.update { it.copy(currentPosition = position) }
    }

    /**
     * Toggle shuffle mode
     */
    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    /**
     * Cycle repeat mode
     */
    fun cycleRepeat() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    /**
     * Load more tracks when approaching end
     */
    private fun checkAndLoadMore() {
        val state = _uiState.value
        val remaining = state.playlist.size - state.currentIndex
        if (remaining <= 5) {
            Log.d(TAG, "Need to load more tracks, remaining: $remaining, requesting from PlayerManager")
            PlayerManager.requestLoadMore()
        }
    }

    /**
     * Simulate progress and auto-load more tracks when needed
     */
    private fun startProgressSimulation() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                delay(1000)
                val currentPos = _uiState.value.currentPosition
                val duration = _uiState.value.currentTrack?.durationSec ?: 0

                if (currentPos >= duration && duration > 0) {
                    // Song finished, check if we can play next
                    if (_uiState.value.currentIndex < _uiState.value.playlist.size - 1) {
                        onNextClick()
                    } else if (_uiState.value.repeatMode == RepeatMode.ALL && _uiState.value.playlist.isNotEmpty()) {
                        // Loop to first
                        _uiState.update { it.copy(
                            currentIndex = 0,
                            currentPosition = 0,
                            currentTrack = it.playlist.firstOrNull()
                        ) }
                        PlayerManager.updatePosition(0)
                    } else {
                        // Stop
                        _uiState.update { it.copy(isPlaying = false) }
                        PlayerManager.togglePlayPause() // This will stop it
                    }
                } else {
                    _uiState.update { it.copy(currentPosition = currentPos + 1) }
                    // Update PlayerManager with current position
                    PlayerManager.updatePosition(currentPos + 1)

                    // Check if we need to load more tracks (approaching end of loaded playlist)
                    checkAndLoadMore()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}