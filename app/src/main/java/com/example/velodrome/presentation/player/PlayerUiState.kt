package com.example.velodrome.presentation.player

import com.example.velodrome.domain.model.Track

/**
 * UI State for Player Screen
 */
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val currentTrack: Track? = null,
    val playlist: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null
)

enum class RepeatMode {
    OFF, ALL, ONE
}