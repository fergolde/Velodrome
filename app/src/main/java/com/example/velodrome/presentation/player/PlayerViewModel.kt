package com.example.velodrome.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val TAG = "PlayerViewModel"

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    /**
     * Consolidated UI state using combine pattern.
     * 
     * Instead of multiple separate collectors (which cause multiple recompositions),
     * we combine all playerManager flows into a single state emission.
     */
    val uiState: StateFlow<PlayerUiState> = combine(
        playerManager.playlist,
        playerManager.isPlaying,
        playerManager.currentPosition,
        playerManager.currentTrack,
        playerManager.currentIndex,
        playerManager.isBuffering,
        playerManager.isShuffleEnabled,
        playerManager.isRepeatEnabled
    ) { values: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        PlayerUiState(
            playlist = values[0] as List<Track>,
            isPlaying = values[1] as Boolean,
            // Convert milliseconds to seconds for UI
            currentPosition = ((values[2] as Long) / 1000).toInt(),
            currentTrack = values[3] as Track?,
            currentIndex = values[4] as Int,
            isBuffering = values[5] as Boolean,
            isShuffleEnabled = values[6] as Boolean,
            isRepeatEnabled = values[7] as Boolean
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlayerUiState()
    )

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
        playerManager.toggleShuffle()
    }

    /**
     * Toggle repeat mode
     */
    fun toggleRepeat() {
        playerManager.toggleRepeat()
    }
}