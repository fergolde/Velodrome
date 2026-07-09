package com.fergolde.velodrome.presentation.player

import androidx.lifecycle.ViewModel
import com.fergolde.velodrome.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Shared ViewModel exposing player state for UI global.
 * Injected at app level via hiltViewModel() in MainActivity.
 */
@HiltViewModel
class SharedPlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager
) : ViewModel() {

    val currentTrack: StateFlow<Track?> = playerManager.currentTrack
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition

    fun togglePlayPause() = playerManager.togglePlayPause()
    fun next() = playerManager.next()
    fun previous() = playerManager.previous()
}