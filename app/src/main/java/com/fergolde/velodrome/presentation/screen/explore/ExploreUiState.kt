package com.fergolde.velodrome.presentation.screen.explore

import androidx.compose.runtime.Immutable

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import java.util.Calendar

/**
 * UI State for Explore Screen
 */
@Immutable
data class ExploreUiState(
    val isLoading: Boolean = true,
    val randomArtists: List<Artist> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: SearchResults = SearchResults(),
    /** Sugerencias por prefijo de la biblioteca local, sin red. */
    val suggestions: SearchResults = SearchResults(),
    /** Últimas queries de la cuenta activa, la más reciente primero. */
    val searchHistory: List<String> = emptyList(),
    val minYear: Int = 0, // Will be loaded from DB
    val currentYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedYearRange: IntRange? = null
)

@Immutable
data class SearchResults(
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val tracks: List<Track> = emptyList()
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && tracks.isEmpty()
}