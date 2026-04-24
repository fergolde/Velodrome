package com.example.velodrome.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        // AudioPlayerManager emite posición en milisegundos → convertir a segundos para la UI
        viewModelScope.launch {
            playerManager.currentPosition.collect { posMs ->
                _uiState.update { it.copy(currentPosition = (posMs / 1000).toInt()) }
            }
        }
        // Colectar currentTrack directamente: evita que la info aparezca vacía
        // cuando el sheet se abre con una canción ya en reproducción
        viewModelScope.launch {
            playerManager.currentTrack.collect { track ->
                _uiState.update { it.copy(currentTrack = track) }
            }
        }
        viewModelScope.launch {
            playerManager.currentIndex.collect { idx ->
                _uiState.update { state ->
                    state.copy(currentIndex = idx)
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
     * Play/Pause toggle
     */
    fun onPlayPauseClick() {
        playerManager.togglePlayPause()
    }

    /**
     * Go to previous track
     */
    fun onPreviousClick() {
        playerManager.previous()
    }

    /**
     * Go to next track
     */
    fun onNextClick() {
        playerManager.next()
    }

    /**
     * Select a track from the queue (jump to index)
     */
    fun onTrackSelected(index: Int) {
        playerManager.setCurrentIndex(index)
    }

    /**
     * Seek to position (in seconds)
     */
    fun onSeek(position: Int) {
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