package com.example.velodrome.presentation.screen.homescreen

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track

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

    // Featured album (first from most played)
    val featuredAlbum: Album? = null,

    // Available genres for filtering
    val genres: List<String> = emptyList(),

    // Available years for filtering (extracted from albums)
    val availableYears: List<Int> = emptyList(),

    // Current filter selections
    val selectedGenre: String? = null,
    val selectedYear: Int? = null,

    // Filtered albums (displayed when filter is active)
    val filteredAlbums: List<Album> = emptyList(),

    // Playback state
    val currentTrackId: String? = null,
    val isPlaying: Boolean = false,

    // Refresh state
    val isRefreshing: Boolean = false,

    // Global search state
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: SearchResults = SearchResults()
)

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList()
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()
}