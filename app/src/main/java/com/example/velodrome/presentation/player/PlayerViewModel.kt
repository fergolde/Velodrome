package com.example.velodrome.presentation.player

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.player.PlayerManager.currentTrack
import com.example.velodrome.presentation.player.PlayerManager.isPlaying
import com.example.velodrome.presentation.player.PlayerManager.playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PlayerViewModel"

@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        // Sync with PlayerManager (position is updated by AudioPlayerManager polling)
        viewModelScope.launch {
            PlayerManager.playlist.collect { playlist ->
                _uiState.update { it.copy(playlist = playlist) }
            }
        }
        viewModelScope.launch {
            PlayerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        // Position is polled by AudioPlayerManager, just collect it here
        viewModelScope.launch {
            PlayerManager.currentPosition.collect { pos ->
                _uiState.update { it.copy(currentPosition = pos.toInt()) }
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
        viewModelScope.launch {
            PlayerManager.isBuffering.collect { isBuffering ->
                _uiState.update { it.copy(isBuffering = isBuffering) }
            }
        }
    }

    /**
     * Start playing a track
     */
    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        Log.d(TAG, "playTrack: ${track.title}")
        PlayerManager.playTrack(track, playlist)
    }

    /**
     * Play/Pause toggle
     */
    fun onPlayPauseClick() {
        Log.d(TAG, "onPlayPauseClick")
        PlayerManager.togglePlayPause()
    }

    /**
     * Go to previous track
     */
    fun onPreviousClick() {
        Log.d(TAG, "onPreviousClick")
        PlayerManager.previous()
    }

    /**
     * Go to next track
     */
    fun onNextClick() {
        Log.d(TAG, "onNextClick")
        PlayerManager.next()
    }

    /**
     * Select a track from the queue (jump to index)
     */
    fun onTrackSelected(index: Int) {
        Log.d(TAG, "onTrackSelected: $index")
        PlayerManager.setCurrentIndex(index)
    }

    /**
     * Seek to position (in seconds)
     */
    fun onSeek(position: Int) {
        Log.d(TAG, "onSeek: $position")
        PlayerManager.seekTo(position.toLong() * 1000)
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