package com.fergolde.velodrome.presentation.screen.home

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Playlist

/**
 * UI State for the Home screen.
 * Represents the complete state of the HomeScreen UI.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Recently added albums (RecentlyAdded section)
    val latestAlbums: List<Album> = emptyList(),

    // Most played albums (MostPlayed section)
    val topAlbums: List<Album> = emptyList(),

    // Recently played albums
    val recentlyPlayedAlbums: List<Album> = emptyList(),

    // Random albums
    val randomAlbums: List<Album> = emptyList(),

    // Available genres for filtering
    val genres: List<String> = emptyList(),

    // Playback state
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,

    // Server playlists (incl. AudioMuse auto-generated)
    val playlists: List<Playlist> = emptyList(),
    val isPlaylistLoading: Boolean = false,
    val playingPlaylistId: String? = null
)