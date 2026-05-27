package com.example.velodrome.presentation.player

import com.example.velodrome.domain.model.Track
import com.example.velodrome.presentation.audio.AudioPlayerManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Player state manager - delegates to AudioPlayerManager.
 * Injected by Hilt - singleton ensures single instance.
 */
@Singleton
class PlayerManager @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) {
    val playlist: StateFlow<List<Track>> = audioPlayerManager.playlist
    val currentIndex: StateFlow<Int> = audioPlayerManager.currentIndex
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val currentPosition: StateFlow<Long> = audioPlayerManager.currentPosition
    val currentTrack: StateFlow<Track?> = audioPlayerManager.currentTrack
    val currentTrackId: StateFlow<String?> = audioPlayerManager.currentTrackId
    val isBuffering: StateFlow<Boolean> = audioPlayerManager.isBuffering
    val isShuffleEnabled: StateFlow<Boolean> = audioPlayerManager.isShuffleEnabled
    val isRepeatEnabled: StateFlow<Boolean> = audioPlayerManager.isRepeatEnabled

    fun setPlaylist(tracks: List<Track>, startPlaying: Boolean = true) {
        if (tracks.isNotEmpty()) {
            audioPlayerManager.playTrack(tracks[0], tracks, if (startPlaying) 0 else -1)
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

    fun playNow(track: Track) {
        val currentList = audioPlayerManager.playlist.value.toMutableList()
        val currentIndex = audioPlayerManager.currentIndex.value
        val insertIndex = (currentIndex + 1).coerceAtMost(currentList.size)
        currentList.add(insertIndex, track)
        audioPlayerManager.setPlaylist(currentList)
        audioPlayerManager.playTrack(track, currentList, insertIndex)
    }

    fun playNext(track: Track) {
        val currentIndex = audioPlayerManager.currentIndex.value
        val insertIndex = (currentIndex + 1).coerceAtMost(audioPlayerManager.playlist.value.size)
        audioPlayerManager.insertIntoPlaylist(insertIndex, listOf(track))
    }

    fun addToQueue(track: Track) {
        audioPlayerManager.addToPlaylist(listOf(track))
    }

    fun playNow(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun playNext(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val insertIndex = (audioPlayerManager.currentIndex.value + 1).coerceAtMost(audioPlayerManager.playlist.value.size)
        audioPlayerManager.insertIntoPlaylist(insertIndex, tracks)
    }

    fun addToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        audioPlayerManager.addToPlaylist(tracks)
    }

    fun setLoadMoreCallback(callback: () -> Unit) {
        audioPlayerManager.setLoadMoreCallback(callback)
    }

    fun appendToPlaylist(tracks: List<Track>) {
        audioPlayerManager.appendToPlaylist(tracks)
    }

    fun setCurrentIndex(index: Int) {
        val p = audioPlayerManager.playlist.value
        if (index in p.indices) {
            audioPlayerManager.playTrack(p[index], p, index)
        }
    }

    fun removeFromPlaylist(index: Int) {
        audioPlayerManager.removeFromPlaylist(index)
    }

    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
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



    fun toggleShuffle() {
        audioPlayerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        audioPlayerManager.toggleRepeat()
    }
}