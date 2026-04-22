package com.example.velodrome.presentation.player

import android.util.Log
import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.audio.AudioPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player state manager - delegates to AudioPlayerManager.
 * Uses companion object pattern for backwards compatibility with UI code.
 */
@Singleton
class PlayerManager @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) {
    companion object {
        private val instanceRef = AtomicReference<PlayerManager?>(null)
        
        val playlist: StateFlow<List<Track>>
            get() = instanceRef.get()?.playlist ?: MutableStateFlow(emptyList())
        val currentIndex: StateFlow<Int>
            get() = instanceRef.get()?.currentIndex ?: MutableStateFlow(0)
        val isPlaying: StateFlow<Boolean>
            get() = instanceRef.get()?.isPlaying ?: MutableStateFlow(false)
        val currentPosition: StateFlow<Long>
            get() = instanceRef.get()?.currentPosition ?: MutableStateFlow(0L)
        val currentTrack: StateFlow<Track?>
            get() = instanceRef.get()?.currentTrack ?: MutableStateFlow(null)
        val currentTrackId: StateFlow<String?>
            get() = instanceRef.get()?.currentTrackId ?: MutableStateFlow(null)
        val isBuffering: StateFlow<Boolean>
            get() = instanceRef.get()?.isBuffering ?: MutableStateFlow(false)

        fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) = 
            instanceRef.get()?.setPlaylist(tracks, startPlaying)
        fun setPlaylist(tracks: List<Track>, startIndex: Int, startPlaying: Boolean = true) = 
            instanceRef.get()?.setPlaylist(tracks, startIndex, startPlaying)
        fun playTrack(track: Track, playlist: List<Track> = listOf(track)) = 
            instanceRef.get()?.playTrack(track, playlist)
        fun appendToPlaylist(tracks: List<Track>) = instanceRef.get()?.appendToPlaylist(tracks)
        fun setCurrentIndex(index: Int) = instanceRef.get()?.setCurrentIndex(index)
        fun updatePosition(position: Int) = instanceRef.get()?.updatePosition(position)
        fun togglePlayPause() = instanceRef.get()?.togglePlayPause()
        fun play() = instanceRef.get()?.play()
        fun next(): Boolean = instanceRef.get()?.next() ?: false
        fun previous(): Boolean = instanceRef.get()?.previous() ?: false
        fun seekTo(positionMs: Long) = instanceRef.get()?.seekTo(positionMs)
        fun playNext(track: Track) = instanceRef.get()?.playNext(track)
        fun addToQueue(track: Track) = instanceRef.get()?.addToQueue(track)
        fun setLoadMoreCallback(callback: () -> Unit) = instanceRef.get()?.setLoadMoreCallback(callback)
    }
    
    init {
        instanceRef.set(this)
    }

    val playlist: StateFlow<List<Track>> = audioPlayerManager.playlist
    val currentIndex: StateFlow<Int> = audioPlayerManager.currentIndex
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val currentPosition: StateFlow<Long> = audioPlayerManager.currentPosition
    val currentTrack: StateFlow<Track?> = audioPlayerManager.currentTrack
    val currentTrackId: StateFlow<String?> = audioPlayerManager.currentTrackId
    val isBuffering: StateFlow<Boolean> = audioPlayerManager.isBuffering

    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        if (tracks.isNotEmpty()) {
            val startIndex = if (startPlaying) 0 else -1
            audioPlayerManager.playTrack(tracks[0], tracks, startIndex)
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int, startPlaying: Boolean = true) {
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            audioPlayerManager.playTrack(tracks[startIndex], tracks, startIndex)
        } else if (tracks.isNotEmpty()) {
            audioPlayerManager.playTrack(tracks[0], tracks, 0)
        }
    }

    fun playTrack(track: Track, playlist: List<Track> = listOf(track)) {
        val index = playlist.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        audioPlayerManager.playTrack(track, playlist, index)
    }

    fun appendToPlaylist(tracks: List<Track>) {
        if (tracks.isNotEmpty()) {
            audioPlayerManager.appendToPlaylist(tracks)
        }
    }

    fun setCurrentIndex(index: Int) {
        val p = audioPlayerManager.playlist.value
        if (index in p.indices) {
            audioPlayerManager.playTrack(p[index], p, index)
        }
    }

    fun updatePosition(position: Int) {
        audioPlayerManager.seekTo(position.toLong())
    }

    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
    }

    fun play() {
        audioPlayerManager.play()
    }

    fun next(): Boolean {
        return audioPlayerManager.next()
    }

    fun previous(): Boolean {
        return audioPlayerManager.previous()
    }

    fun seekTo(positionMs: Long) {
        audioPlayerManager.seekTo(positionMs)
    }

    fun playNext(track: Track) {
        val currentList = audioPlayerManager.playlist.value.toMutableList()
        val currentIdx = audioPlayerManager.currentIndex.value
        val insertIndex = (currentIdx + 1).coerceAtMost(currentList.size)
        currentList.add(insertIndex, track)
        audioPlayerManager.setPlaylist(currentList)
    }

    fun addToQueue(track: Track) {
        val currentList = audioPlayerManager.playlist.value.toMutableList()
        currentList.add(track)
        audioPlayerManager.setPlaylist(currentList)
    }

    private var loadMoreCallback: (() -> Unit)? = null

    fun setLoadMoreCallback(callback: () -> Unit) {
        audioPlayerManager.setLoadMoreCallback {
            callback()
        }
    }
}