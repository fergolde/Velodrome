package com.example.velodrome.presentation.player

import android.util.Log
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.audio.AudioPlayerManager
import dagger.hilt.android.scopes.ViewModelComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player state manager - delegates to AudioPlayerManager for real playback.
 * Injected as a singleton to maintain backward compatibility with existing UI code.
 */
@Singleton
class PlayerManager @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) {

    private const val TAG = "PlayerManager"

    // Expose state from AudioPlayerManager
    val playlist: StateFlow<List<Track>> = audioPlayerManager.playlist
    val currentIndex: StateFlow<Int> = audioPlayerManager.currentIndex
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val currentPosition: StateFlow<Long> = audioPlayerManager.currentPosition
    val currentTrack: StateFlow<Track?> = audioPlayerManager.currentTrack
    val currentTrackId: StateFlow<String?> = audioPlayerManager.currentTrackId
    val isBuffering: StateFlow<Boolean> = audioPlayerManager.isBuffering

    // Keep local state for duration (not exposed by AudioPlayerManager in same way)
    private val _duration = MutableStateFlow(0L)

    /**
     * Set a new playlist and start playing from index 0
     */
    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        Log.d(TAG, "setPlaylist: ${tracks.size} tracks, startPlaying: $startPlaying")
        if (tracks.isNotEmpty()) {
            val startIndex = if (startPlaying) 0 else -1
            audioPlayerManager.playTrack(tracks[0], tracks, startIndex)
        }
    }

    /**
     * Set a new playlist with start index
     */
    fun setPlaylist(tracks: List<Track>, startIndex: Int, startPlaying: Boolean = true) {
        Log.d(TAG, "setPlaylist: ${tracks.size} tracks, startIndex: $startIndex, startPlaying: $startPlaying")
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            audioPlayerManager.playTrack(tracks[startIndex], tracks, startIndex)
        } else if (tracks.isNotEmpty()) {
            audioPlayerManager.playTrack(tracks[0], tracks, 0)
        }
    }

    /**
     * Play a specific track from a playlist
     */
    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        Log.d(TAG, "playTrack: ${track.title}")
        val index = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        audioPlayerManager.playTrack(track, playlist, index)
    }

    /**
     * Append tracks to the current playlist (for infinite loading)
     */
    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            audioPlayerManager.appendToPlaylist(tracks)
            Log.d(TAG, "Appended ${tracks.size} tracks, playlist size: ${audioPlayerManager.playlist.value.size}")
        }
    }


    /**
     * Set current index directly (for queue navigation)
     */
    fun setCurrentIndex(index: Int) {
        val playlist = audioPlayerManager.playlist.value
        if (index in playlist.indices) {
            Log.d(TAG, "Set current index to $index, track: ${playlist[index].title}")
            audioPlayerManager.playTrack(playlist[index], playlist, index)
        }
    }

    /**
     * Update current track position
     */
    fun updatePosition(position: Int) {
        audioPlayerManager.seekTo(position.toLong())
    }

    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause")
        audioPlayerManager.togglePlayPause()
    }

    /**
     * Start/resume playback
     */
    fun play() {
        audioPlayerManager.play()
    }

    /**
     * Go to next track
     */
    fun next(): Boolean {
        return audioPlayerManager.next()
    }

    /**
     * Go to previous track
     */
    fun previous(): Boolean {
        return audioPlayerManager.previous()
    }

    /**
     * Seek to position in milliseconds
     */
    fun seekTo(positionMs: Long) {
        audioPlayerManager.seekTo(positionMs)
    }

    /**
     * Add track to play next (after current track finishes)
     */
    fun playNext(track: Track) {
        Log.d(TAG, "playNext: ${track.title}")
        val currentList = audioPlayerManager.playlist.value.toMutableList()
        val currentIdx = audioPlayerManager.currentIndex.value
        
        // Insert right after current track
        val insertIndex = (currentIdx + 1).coerceAtMost(currentList.size)
        currentList.add(insertIndex, track)
        audioPlayerManager.setPlaylist(currentList)
    }

    /**
     * Add track to end of queue
     */
    fun addToQueue(track: Track) {
        Log.d(TAG, "addToQueue: ${track.title}")
        val currentList = audioPlayerManager.playlist.value.toMutableList()
        currentList.add(track)
        audioPlayerManager.setPlaylist(currentList)
    }

    /**
     * Register a callback to be called when more tracks need to be loaded
     */
    private var loadMoreCallback: (() -> Unit)? = null

    fun setLoadMoreCallback(callback: () -> Unit) {
        Log.d(TAG, "setLoadMoreCallback called")
        // Also set it in AudioPlayerManager
        audioPlayerManager.setLoadMoreCallback {
            Log.d(TAG, "LoadMoreCallback invoked from AudioPlayerManager, calling inner callback")
            Log.d(TAG, "loadMoreCallback is null: ${loadMoreCallback == null}")
            callback()
            Log.d(TAG, "Inner callback executed")
        }
        Log.d(TAG, "LoadMoreCallback set in both PlayerManager and AudioPlayerManager")
    }
}