package com.fergolde.velodrome.presentation.screen.explore

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import java.util.Calendar

/**
 * UI State for Explore Screen
 */
data class ExploreUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val randomArtists: List<Artist> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val curatedAlbums: List<Album> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: SearchResults = SearchResults(),
    val dynamicPlaylist: List<Track> = emptyList(),
    val minYear: Int = 0, // Will be loaded from DB
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedYearRange: IntRange? = null
)

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList()
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()
}