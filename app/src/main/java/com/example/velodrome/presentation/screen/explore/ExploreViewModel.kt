package com.example.velodrome.presentation.screen.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.repository.NavidromeRepository
import com.example.velodrome.domain.usecase.GetArtistsUseCase
import com.example.velodrome.domain.usecase.GetGenresUseCase
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
import com.example.velodrome.presentation.player.PlayerManager
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
    private val navidromeRepository: NavidromeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()
    
    // Dynamic playlist state
    private val playlist = mutableListOf<Track>()
    private var currentPlaylistPosition = 0
    private var isLoadingMore = false
    private var currentGenreFilter: List<String> = emptyList()  // Tracks which genre(s) to use

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
        
        // Determine which genre(s) to use for this session
        currentGenreFilter = if (selectedGenres.isEmpty()) {
            emptyList()
        } else {
            // Keep all selected genres
            selectedGenres.toList()
        }
        Log.d(TAG, "Genre filter for this session: $currentGenreFilter")
        
        viewModelScope.launch {
            try {
                // Use the new API endpoints to get songs directly by genre
                val songsResult: Result<List<Track>>
                
                if (selectedGenres.isEmpty()) {
                    // No genre selected - get random songs (all genres)
                    Log.d(TAG, "Loading random songs (all genres)")
                    songsResult = navidromeRepository.getRandomSongs(size = 100)
                } else if (selectedGenres.size == 1) {
                    // Single genre - use getSongsByGenre for exact genre filtering
                    val genre = selectedGenres.first()
                    Log.d(TAG, "Loading songs for single genre: $genre using getSongsByGenre")
                    songsResult = navidromeRepository.getSongsByGenre(genre, count = 100, offset = 0)
                } else {
                    // Multiple genres - get songs from ALL selected genres and mix them
                    Log.d(TAG, "Multiple genres selected: $selectedGenres, loading from ALL")
                    val allSongs = mutableListOf<Track>()
                    for (genre in selectedGenres) {
                        val result = navidromeRepository.getSongsByGenre(genre, count = 50, offset = 0)
                        result.onSuccess { songs ->
                            allSongs.addAll(songs)
                            Log.d(TAG, "Got ${songs.size} songs for genre: $genre")
                        }
                    }
                    songsResult = Result.success(allSongs)
                }
                
                songsResult.onSuccess { songs ->
                    playlist.addAll(songs)
                    Log.d(TAG, "Total tracks loaded: ${playlist.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading songs: ${error.message}")
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    return@launch
                }
                
                // Shuffle the full playlist
                playlist.shuffle()
                Log.d(TAG, "Total playlist after shuffle: ${playlist.size}")
                
                // Emit first 10 tracks
                val initialTracks = playlist.take(10)
                currentPlaylistPosition = 10
                Log.d(TAG, "First 10 tracks: ${initialTracks.map { it.title }}")
                
                _uiState.update { it.copy(
                    isLoading = false,
                    dynamicPlaylist = initialTracks
                ) }
                
                // Set up callback for PlayerManager to request more tracks
                PlayerManager.setLoadMoreCallback {
                    Log.d(TAG, "=== PlayerManager callback triggered! ===")
                    checkAndLoadMore()
                }

                // Set playlist in PlayerManager to start playback
                if (initialTracks.isNotEmpty()) {
                    PlayerManager.setPlaylist(initialTracks, startPlaying = true)
                    Log.d(TAG, "Started playback with PlayerManager")
                }
                
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
    fun loadMoreTracks() {
        Log.d(TAG, "=== loadMoreTracks ENTRY ===")
        if (isLoadingMore) {
            Log.d(TAG, "loadMoreTracks: already loading, returning")
            return
        }

        isLoadingMore = true
        Log.d(TAG, "loadMoreTracks: Current playlist size: ${playlist.size}, position: $currentPlaylistPosition")
        
        viewModelScope.launch {
            try {
                // First, check if there are remaining tracks in current playlist
                val remaining = playlist.size - currentPlaylistPosition
                Log.d(TAG, "Remaining tracks in playlist: $remaining")
                
                if (remaining > 0) {
                    // Use remaining tracks from current playlist
                    val nextBatch = playlist.drop(currentPlaylistPosition).take(10)
                    currentPlaylistPosition += nextBatch.size
                    PlayerManager.appendToPlaylist(nextBatch)
                    Log.d(TAG, "Added ${nextBatch.size} tracks from existing playlist")
                    
                    // If we still have enough, stop here
                    if (remaining > 10) {
                        isLoadingMore = false
                        return@launch
                    }
                }

                // Need to load more songs from server using the new API
                Log.d(TAG, "Loading more songs with genre filter: $currentGenreFilter")
                
                val songsResult: Result<List<Track>>
                
                if (currentGenreFilter.isEmpty()) {
                    // No genre filter - get more random songs
                    Log.d(TAG, "Loading 50 more random songs...")
                    songsResult = navidromeRepository.getRandomSongs(size = 50)
                } else if (currentGenreFilter.size == 1) {
                    // Single genre - use getSongsByGenre with offset
                    val genre = currentGenreFilter.first()
                    val offset = playlist.size
                    Log.d(TAG, "Loading 50 more songs for genre: $genre with offset: $offset")
                    songsResult = navidromeRepository.getSongsByGenre(genre, count = 50, offset = offset)
                } else {
                    // Multiple genres - load from ALL of them
                    Log.d(TAG, "Loading 50 more songs from ALL genres: $currentGenreFilter")
                    val allSongs = mutableListOf<Track>()
                    for (genre in currentGenreFilter) {
                        val result = navidromeRepository.getSongsByGenre(genre, count = 25, offset = 0)
                        result.onSuccess { songs ->
                            allSongs.addAll(songs)
                            Log.d(TAG, "Got ${songs.size} more songs for genre: $genre")
                        }
                    }
                    songsResult = Result.success(allSongs)
                }
                
                songsResult.onSuccess { newSongs ->
                    playlist.addAll(newSongs)
                    Log.d(TAG, "Added ${newSongs.size} new songs, playlist size now: ${playlist.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading more songs: ${error.message}")
                }

                // Now add new tracks to player
                val newTracks = playlist.drop(currentPlaylistPosition).take(10)
                currentPlaylistPosition += newTracks.size
                
                if (newTracks.isNotEmpty()) {
                    PlayerManager.appendToPlaylist(newTracks)
                    Log.d(TAG, "Added ${newTracks.size} NEW tracks to PlayerManager")
                } else {
                    Log.w(TAG, "No new tracks to add!")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading more tracks", e)
            } finally {
                isLoadingMore = false
                Log.d(TAG, "loadMoreTracks completed, isLoadingMore=false")
            }
        }
    }

    /**
     * Check if we need to load more tracks - called from player
     */
    fun checkAndLoadMore() {
        Log.d(TAG, "checkAndLoadMore ENTRY")
        try {
            if (isLoadingMore) {
                Log.d(TAG, "checkAndLoadMore: already loading, skipping")
                return
            }
            
            val remainingInPlaylist = playlist.size - currentPlaylistPosition
            Log.d(TAG, "checkAndLoadMore: remaining=$remainingInPlaylist, playlist.size=${playlist.size}")
            
            if (remainingInPlaylist <= 10) {
                Log.d(TAG, "checkAndLoadMore: calling loadMoreTracks()")
                loadMoreTracks()
            } else {
                Log.d(TAG, "checkAndLoadMore: not needed, remaining > 10")
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndLoadMore EXCEPTION: ${e.message}", e)
        }
        Log.d(TAG, "checkAndLoadMore EXIT")
    }
}