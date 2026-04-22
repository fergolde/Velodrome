package com.example.velodrome.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlayerViewModel"

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Sync with PlayerManager (position is updated by AudioPlayerManager polling)
        viewModelScope.launch {
            playerManager.playlist.collect { playlist ->
                _uiState.update { it.copy(playlist = playlist) }
            }
        }
        viewModelScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        // Position is polled by AudioPlayerManager, just collect it here
        viewModelScope.launch {
            playerManager.currentPosition.collect { pos ->
                _uiState.update { it.copy(currentPosition = pos.toInt()) }
            }
        }
        viewModelScope.launch {
            playerManager.currentIndex.collect { idx ->
                _uiState.update { state ->
                    val currentTrack = state.playlist.getOrNull(idx)
                    state.copy(currentIndex = idx, currentTrack = currentTrack)
                }
            }
        }
        viewModelScope.launch {
            playerManager.isBuffering.collect { isBuffering ->
                _uiState.update { it.copy(isBuffering = isBuffering) }
            }
        }
    }

    /**
     * Start playing a track
     */
    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        Log.d(TAG, "playTrack: ${track.title}")
        playerManager.playTrack(track, playlist)
    }

    /**
     * Play/Pause toggle
     */
    fun onPlayPauseClick() {
        Log.d(TAG, "onPlayPauseClick")
        playerManager.togglePlayPause()
    }

    /**
     * Go to previous track
     */
    fun onPreviousClick() {
        Log.d(TAG, "onPreviousClick")
        playerManager.previous()
    }

    /**
     * Go to next track
     */
    fun onNextClick() {
        Log.d(TAG, "onNextClick")
        playerManager.next()
    }

    /**
     * Select a track from the queue (jump to index)
     */
    fun onTrackSelected(index: Int) {
        Log.d(TAG, "onTrackSelected: $index")
        playerManager.setCurrentIndex(index)
    }

    /**
     * Seek to position (in seconds)
     */
    fun onSeek(position: Int) {
        Log.d(TAG, "onSeek: $position")
        playerManager.seekTo(position.toLong() * 1000)
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
}