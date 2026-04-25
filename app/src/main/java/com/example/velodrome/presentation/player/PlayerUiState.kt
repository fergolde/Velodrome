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
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false,
    val isBuffering: Boolean = false,
    val error: String? = null
)