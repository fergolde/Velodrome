package com.example.velodrome.presentation.screen.explore

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track

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
    val dynamicPlaylist: List<Track> = emptyList()
)

data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList()
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()
    val totalCount: Int get() = artists.size + albums.size + tracks.size
}