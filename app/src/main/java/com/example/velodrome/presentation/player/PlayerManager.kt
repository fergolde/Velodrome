package com.example.velodrome.presentation.player

import com.example.velodrome.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton player state manager - shared across all screens
 */
object PlayerManager {
    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()
    
    /**
     * Set a new playlist and start playing from index 0
     */
    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        _playlist.value = tracks
        _currentIndex.value = 0
        _currentPosition.value = 0
        _isPlaying.value = startPlaying
    }
    
    /**
     * Update current track position (called by player)
     */
    fun updatePosition(position: Int) {
        _currentPosition.value = position
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }
    
    /**
     * Go to next track
     */
    fun next(): Boolean {
        return if (_currentIndex.value < _playlist.value.size - 1) {
            _currentIndex.value++
            _currentPosition.value = 0
            true
        } else {
            false
        }
    }
    
    /**
     * Go to previous track
     */
    fun previous(): Boolean {
        return if (_currentIndex.value > 0) {
            _currentIndex.value--
            _currentPosition.value = 0
            true
        } else {
            false
        }
    }
    
    fun getCurrentTrack(): Track? {
        return _playlist.value.getOrNull(_currentIndex.value)
    }
}