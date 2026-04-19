package com.example.velodrome.presentation.player

import android.util.Log
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.audio.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton player state manager - delegates to AudioPlayerManager for real playback.
 * Maintains backward compatibility with existing UI code.
 */
object PlayerManager {

    private const val TAG = "PlayerManager"

    // Expose state from AudioPlayerManager
    val playlist: StateFlow<List<Track>> = AudioPlayerManager.playlist
    val currentIndex: StateFlow<Int> = AudioPlayerManager.currentIndex
    val isPlaying: StateFlow<Boolean> = AudioPlayerManager.isPlaying
    val currentPosition: StateFlow<Long> = AudioPlayerManager.currentPosition
    val currentTrack: StateFlow<Track?> = AudioPlayerManager.currentTrack
    val isBuffering: StateFlow<Boolean> = AudioPlayerManager.isBuffering

    // Keep local state for duration (not exposed by AudioPlayerManager in same way)
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    /**
     * Set a new playlist and start playing from index 0
     */
    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        Log.d(TAG, "setPlaylist: ${tracks.size} tracks, startPlaying: $startPlaying")
        if (tracks.isNotEmpty()) {
            AudioPlayerManager.playTrack(tracks[0], tracks, 0)
        }
    }

    /**
     * Play a specific track from a playlist
     */
    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        Log.d(TAG, "playTrack: ${track.title}")
        val index = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        AudioPlayerManager.playTrack(track, playlist, index)
    }

    /**
     * Append tracks to the current playlist (for infinite loading)
     */
    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            AudioPlayerManager.setPlaylist(AudioPlayerManager.playlist.value + tracks)
            Log.d(TAG, "Appended ${tracks.size} tracks, playlist size: ${AudioPlayerManager.playlist.value.size}")
        }
    }

    /**
     * Replace entire playlist with new tracks
     */
    fun replacePlaylist(tracks: List<Track>) {
        AudioPlayerManager.setPlaylist(tracks)
        if (tracks.isNotEmpty()) {
            AudioPlayerManager.playTrack(tracks[0], tracks, 0)
        }
    }

    /**
     * Set current index directly (for queue navigation)
     */
    fun setCurrentIndex(index: Int) {
        val playlist = AudioPlayerManager.playlist.value
        if (index in playlist.indices) {
            Log.d(TAG, "Set current index to $index, track: ${playlist[index].title}")
            AudioPlayerManager.playTrack(playlist[index], playlist, index)
        }
    }

    /**
     * Update current track position
     */
    fun updatePosition(position: Int) {
        AudioPlayerManager.seekTo(position.toLong())
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause")
        AudioPlayerManager.togglePlayPause()
    }

    /**
     * Pause playback
     */
    fun pause() {
        AudioPlayerManager.pause()
    }

    /**
     * Start/resume playback
     */
    fun play() {
        AudioPlayerManager.play()
    }

    /**
     * Go to next track
     */
    fun next(): Boolean {
        return AudioPlayerManager.next()
    }

    /**
     * Go to previous track
     */
    fun previous(): Boolean {
        return AudioPlayerManager.previous()
    }

    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        AudioPlayerManager.seekTo(positionMs)
    }

    /**
     * Get current position in milliseconds directly from MediaController
     */
    fun getCurrentPositionMs(): Long {
        return AudioPlayerManager.getCurrentPositionMs()
    }

    /**
     * Clear the playlist
     */
    fun clearPlaylist() {
        AudioPlayerManager.clearPlaylist()
    }

    /**
     * Register a callback to be called when more tracks need to be loaded
     */
    private var loadMoreCallback: (() -> Unit)? = null

    fun setLoadMoreCallback(callback: () -> Unit) {
        loadMoreCallback = callback
    }

    /**
     * Notify that more tracks are needed
     */
    fun requestLoadMore() {
        loadMoreCallback?.invoke()
    }
}