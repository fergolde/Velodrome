package com.example.velodrome.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.GetStreamUrlUseCase
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
        startProgress simulation()
    }

    /**
     * Play/Pause toggle
     */
    fun onPlayPauseClick() {
        val isPlaying = !_uiState.value.isPlaying
        _uiState.update { it.copy(isPlaying = isPlaying) }
        
        if (isPlaying) {
            startProgress simulation()
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
            // TODO: Load more tracks from repository or genre playlist
            Log.d(TAG, "Need to load more tracks, remaining: $remaining")
        }
    }

    /**
     * Simulate progress (in real app would use MediaPlayer)
     */
    private fun startProgress simulation() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive && _uiState.value.isPlaying) {
                delay(1000)
                val currentPos = _uiState.value.currentPosition
                val duration = _uiState.value.currentTrack?.durationSec ?: 0
                
                if (currentPos >= duration) {
                    // Song finished, play next
                    if (_uiState.value.currentIndex < _uiState.value.playlist.size - 1) {
                        onNextClick()
                    } else if (_uiState.value.repeatMode == RepeatMode.ALL) {
                        // Loop to first
                        val firstTrack = _uiState.value.playlist.firstOrNull() ?: return@launch
                        _uiState.update { it.copy(
                            currentTrack = firstTrack,
                            currentIndex = 0,
                            currentPosition = 0
                        ) }
                    } else {
                        // Stop
                        _uiState.update { it.copy(isPlaying = false) }
                    }
                } else {
                    _uiState.update { it.copy(currentPosition = currentPos + 1) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}