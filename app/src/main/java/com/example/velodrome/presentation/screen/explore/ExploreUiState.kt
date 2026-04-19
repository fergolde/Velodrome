package com.example.velodrome.presentation.screen.explore

import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Artist
import com.example.velodrome.domain.model.Track

/**
 * UI State for Explore Screen
 */
data class ExploreUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val randomArtists: List<Artist> = emptyList(),
    val randomAlbums: List<Album> = emptyList(),
    val curatedAlbums: List<Album> = emptyList(),
    val genres: List<String> = emptyList(),
    val selectedGenres: Set<String> = emptySet(),
    val searchQuery: String = "",
    val dynamicPlaylist: List<Track> = emptyList()
)