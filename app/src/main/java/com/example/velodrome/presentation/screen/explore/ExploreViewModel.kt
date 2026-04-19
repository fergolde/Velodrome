package com.example.velodrome.presentation.screen.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Album
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.GetAlbumsByGenreUseCase
import com.example.velodrome.domain.usecase.GetArtistsUseCase
import com.example.velodrome.domain.usecase.GetGenresUseCase
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
import com.example.velodrome.domain.usecase.GetTracksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExploreViewModel"

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val getArtistsUseCase: GetArtistsUseCase,
    private val getRandomAlbumsUseCase: GetRandomAlbumsUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val getAlbumsByGenreUseCase: GetAlbumsByGenreUseCase,
    private val getTracksUseCase: GetTracksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    
    // Dynamic playlist state
    private val playlist = mutableListOf<Track>()
    private var currentPlaylistPosition = 0
    private var isLoadingMore = false
    private val genresByAlbum = mutableMapOf<String, List<Track>>()

    init {
        loadContent()
    }

    fun loadContent() {
        Log.d(TAG, "=== loadContent() called ===")
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            Log.d(TAG, "Loading artists...")
            // Load artists and shuffle for random order
            getArtistsUseCase(offset = 0, size = 20)
                .onSuccess { artists ->
                    Log.d(TAG, "Loaded ${artists.size} artists, shuffling...")
                    val shuffledArtists = artists.shuffled()
                    _uiState.update { it.copy(randomArtists = shuffledArtists) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading artists: ${e.message}")
                    _uiState.update { it.copy(error = e.message) }
                }
        }
        
        viewModelScope.launch {
            Log.d(TAG, "Loading random albums...")
            // Load random albums
            getRandomAlbumsUseCase(size = 20)
                .onSuccess { albums ->
                    Log.d(TAG, "Loaded ${albums.size} random albums")
                    _uiState.update { it.copy(randomAlbums = albums) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading random albums: ${e.message}")
                    _uiState.update { it.copy(error = e.message) }
                }
        }
        
        viewModelScope.launch {
            Log.d(TAG, "Loading curated albums...")
            // Load curated/albums for "Based on your activity" section
            getRandomAlbumsUseCase(size = 10)
                .onSuccess { albums ->
                    Log.d(TAG, "Loaded ${albums.size} curated albums")
                    _uiState.update { it.copy(curatedAlbums = albums) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading curated albums: ${e.message}")
                    _uiState.update { it.copy(error = e.message) }
                }
        }
        
        viewModelScope.launch {
            Log.d(TAG, "Loading genres...")
            // Load all genres
            getGenresUseCase()
                .onSuccess { genres ->
                    Log.d(TAG, "Loaded ${genres.size} genres")
                    _uiState.update { it.copy(genres = genres, isLoading = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "Error loading genres: ${e.message}")
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onGenreToggle(genre: String) {
        _uiState.update { state ->
            val newSelected = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newSelected)
        }
    }

    fun onPlayGenres() {
        val selectedGenres = _uiState.value.selectedGenres
        
        Log.d(TAG, "Playing: ${if (selectedGenres.isEmpty()) "all genres" else selectedGenres}")
        _uiState.update { it.copy(isLoading = true) }
        
        // Clear and rebuild playlist
        playlist.clear()
        currentPlaylistPosition = 0
        genresByAlbum.clear()
        
        viewModelScope.launch {
            try {
                if (selectedGenres.isEmpty()) {
                    // No genre selected - load random albums (all genres)
                    Log.d(TAG, "Loading random albums (all genres)")
                    getRandomAlbumsUseCase(size = 50)
                        .onSuccess { albums ->
                            for (album in albums) {
                                getTracksUseCase(album.id)
                                    .onSuccess { tracks ->
                                        if (tracks.isNotEmpty()) {
                                            genresByAlbum[album.id] = tracks
                                            playlist.addAll(tracks)
                                        }
                                    }
                            }
                        }
                } else {
                    // Get albums for each selected genre
                    for (genre in selectedGenres) {
                        Log.d(TAG, "Loading albums for genre: $genre")
                        getAlbumsByGenreUseCase(genre, size = 50)
                            .onSuccess { albums ->
                                Log.d(TAG, "Got ${albums.size} albums for $genre")
                                // Get tracks from each album
                                for (album in albums) {
                                    getTracksUseCase(album.id)
                                        .onSuccess { tracks ->
                                            if (tracks.isNotEmpty()) {
                                                genresByAlbum[album.id] = tracks
                                                playlist.addAll(tracks)
                                                Log.d(TAG, "Added ${tracks.size} tracks from album ${album.id}")
                                            }
                                        }
                                }
                            }
                            .onFailure { e ->
                                Log.e(TAG, "Error loading albums for $genre: ${e.message}")
                            }
                    }
                }
                
                // Shuffle the full playlist
                playlist.shuffle()
                Log.d(TAG, "Total playlist size: ${playlist.size}")
                
                // Emit first 10 tracks
                val initialTracks = playlist.take(10)
                currentPlaylistPosition = 10
                
                _uiState.update { it.copy(
                    isLoading = false,
                    dynamicPlaylist = initialTracks
                ) }
                
                Log.d(TAG, "Starting playback with ${initialTracks.size} tracks")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
    
    /**
     * Load more tracks when approaching the end
     * Called when 5 or fewer tracks remain
     */
    private fun loadMoreTracks() {
        if (isLoadingMore || currentPlaylistPosition >= playlist.size) return
        
        isLoadingMore = true
        val remaining = playlist.size - currentPlaylistPosition
        
        viewModelScope.launch {
            val nextBatch = playlist.drop(currentPlaylistPosition).take(5)
            currentPlaylistPosition += 5
            
            _uiState.update { state ->
                val newPlaylist = state.dynamicPlaylist + nextBatch
                state.copy(dynamicPlaylist = newPlaylist)
            }
            
            isLoadingMore = false
            Log.d(TAG, "Loaded ${nextBatch.size} more tracks, total: ${playlist.size}")
        }
    }
    
    /**
     * Call this when track position changes - auto-loads more when needed
     */
    fun onTrackPositionChanged(position: Int) {
        val remaining = playlist.size - position
        if (remaining <= 5) {
            loadMoreTracks()
        }
    }

    fun onArtistClick(artist: com.example.velodrome.domain.model.Artist) {
        // TODO: Navigate to artist detail
    }

    fun onAlbumClick(album: com.example.velodrome.domain.model.Album) {
        // TODO: Navigate to album detail
    }
}