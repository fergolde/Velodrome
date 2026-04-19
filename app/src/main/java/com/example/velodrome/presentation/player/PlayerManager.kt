package com.example.velodrome.presentation.player

import android.util.Log
import com.example.velodrome.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

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

    // Private MutableStateFlow that tracks current track
    private val _currentTrack = MutableStateFlow<Track?>(null)
    /**
     * Observable current track.
     * Automatically updates when playlist or currentIndex changes.
     */
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    /**
     * Sync currentTrack whenever playlist or index changes.
     * Call this after any operation that changes playlist or index.
     */
    private fun syncCurrentTrack() {
        _currentTrack.value = _playlist.value.getOrNull(_currentIndex.value)
    }

    /**
     * Set a new playlist and start playing from index 0
     */
    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        _playlist.value = tracks
        _currentIndex.value = 0
        _currentPosition.value = 0
        _isPlaying.value = startPlaying
        syncCurrentTrack()
    }

    /**
     * Append tracks to the current playlist (for infinite loading)
     */
    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            _playlist.value = _playlist.value + tracks
            Log.d("PlayerManager", "Appended ${tracks.size} tracks, playlist size: ${_playlist.value.size}")
        }
    }

    /**
     * Replace entire playlist with new tracks
     */
    fun replacePlaylist(tracks: List<Track>) {
        _playlist.value = tracks
        _currentIndex.value = 0
        _currentPosition.value = 0
        syncCurrentTrack()
    }

    /**
     * Set current index directly (for queue navigation)
     */
    fun setCurrentIndex(index: Int) {
        if (index >= 0 && index < _playlist.value.size) {
            _currentIndex.value = index
            _currentPosition.value = 0
            Log.d("PlayerManager", "Set current index to $index, track: ${_playlist.value[index].title}")
            syncCurrentTrack()
        }
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
            syncCurrentTrack()
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
            syncCurrentTrack()
            true
        } else {
            false
        }
    }
    
    /**
     * Register a callback to be called when more tracks need to be loaded
     * The callback should load more tracks and append them via appendToPlaylist
     */
    private var loadMoreCallback: (() -> Unit)? = null

    fun setLoadMoreCallback(callback: () -> Unit) {
        loadMoreCallback = callback
    }

    /**
     * Notify that more tracks are needed (called from PlayerViewModel)
     */
    fun requestLoadMore() {
        loadMoreCallback?.invoke()
    }
}