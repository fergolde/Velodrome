package com.fergolde.velodrome.presentation.player

import androidx.compose.runtime.Immutable

import com.fergolde.velodrome.domain.model.Track

/**
 * UI State for Player Screen
 */
@Immutable
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentTrack: Track? = null,
    val playlist: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val isShuffleEnabled: Boolean = false,
    val isRepeatEnabled: Boolean = false
)