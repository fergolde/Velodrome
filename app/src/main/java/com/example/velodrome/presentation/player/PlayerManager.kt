package com.example.velodrome.presentation.player

import android.util.Log
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.audio.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
    val currentTrackId: StateFlow<String?> = AudioPlayerManager.currentTrackId
    val isBuffering: StateFlow<Boolean> = AudioPlayerManager.isBuffering

    // Keep local state for duration (not exposed by AudioPlayerManager in same way)
    private val _duration = MutableStateFlow(0L)

    /**
     * Set a new playlist and start playing from index 0
     */
    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        Log.d(TAG, "setPlaylist: ${tracks.size} tracks, startPlaying: $startPlaying")
        if (tracks.isNotEmpty()) {
            val startIndex = if (startPlaying) 0 else -1
            AudioPlayerManager.playTrack(tracks[0], tracks, startIndex)
        }
    }

    /**
     * Set a new playlist with start index
     */
    fun setPlaylist(tracks: List<Track>, startIndex: Int, startPlaying: Boolean = true) {
        Log.d(TAG, "setPlaylist: ${tracks.size} tracks, startIndex: $startIndex, startPlaying: $startPlaying")
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            AudioPlayerManager.playTrack(tracks[startIndex], tracks, startIndex)
        } else if (tracks.isNotEmpty()) {
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
            AudioPlayerManager.appendToPlaylist(tracks)
            Log.d(TAG, "Appended ${tracks.size} tracks, playlist size: ${AudioPlayerManager.playlist.value.size}")
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
     * Add track to play next (after current track finishes)
     */
    fun playNext(track: Track) {
        Log.d(TAG, "playNext: ${track.title}")
        val currentList = AudioPlayerManager.playlist.value.toMutableList()
        val currentIdx = AudioPlayerManager.currentIndex.value
        
        // Insert right after current track
        val insertIndex = (currentIdx + 1).coerceAtMost(currentList.size)
        currentList.add(insertIndex, track)
        AudioPlayerManager.setPlaylist(currentList)
    }

    /**
     * Add track to end of queue
     */
    fun addToQueue(track: Track) {
        Log.d(TAG, "addToQueue: ${track.title}")
        val currentList = AudioPlayerManager.playlist.value.toMutableList()
        currentList.add(track)
        AudioPlayerManager.setPlaylist(currentList)
    }

    /**
     * Register a callback to be called when more tracks need to be loaded
     */
    private var loadMoreCallback: (() -> Unit)? = null

    fun setLoadMoreCallback(callback: () -> Unit) {
        loadMoreCallback = callback
        // Also set it in AudioPlayerManager
        AudioPlayerManager.setLoadMoreCallback {
            Log.d(TAG, "LoadMoreCallback invoked from AudioPlayerManager")
            callback()
        }
        Log.d(TAG, "LoadMoreCallback set in both PlayerManager and AudioPlayerManager")
    }
}