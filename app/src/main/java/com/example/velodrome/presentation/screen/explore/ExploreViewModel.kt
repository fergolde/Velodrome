package com.example.velodrome.presentation.screen.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.AlbumUseCases
import com.example.velodrome.domain.usecase.ArtistUseCases
import com.example.velodrome.domain.usecase.TrackUseCases
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
    private val albumUseCases: AlbumUseCases,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val localMusicDataSource: LocalMusicDataSource,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    // Dynamic playlist state
    private val playlist = mutableListOf<Track>()
    private var currentPlaylistPosition = 0
    private var isLoadingMore = false
    private var currentGenreFilter: List<String> = emptyList()

    init {
        loadContent()
    }

    fun loadContent() {
        Log.d(TAG, "=== loadContent() called ===")
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            Log.d(TAG, "Loading artists...")
            artistUseCases.getArtists(offset = 0, size = 20)
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
            albumUseCases.getRandomAlbums(size = 20)
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
            albumUseCases.getRandomAlbums(size = 10)
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
            albumUseCases.getGenres()
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

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = SearchResults()) }
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

        _uiState.update { it.copy(isLoading = true) }

        playlist.clear()
        currentPlaylistPosition = 0

        currentGenreFilter = if (selectedGenres.isEmpty()) {
            emptyList()
        } else {
            selectedGenres.toList()
        }
        Log.d(TAG, "Genre filter for this session: $currentGenreFilter")

        viewModelScope.launch {
            try {
                val songsResult: Result<List<Track>>

                if (selectedGenres.isEmpty()) {
                    Log.d(TAG, "Loading 10 random songs (all genres)")
                    songsResult = trackUseCases.getRandomSongs(size = 10)
                } else if (selectedGenres.size == 1) {
                    val genre = selectedGenres.first()
                    Log.d(TAG, "Loading 10 songs for single genre: $genre using getSongsByGenre")
                    songsResult = trackUseCases.getSongsByGenre(genre, count = 10, offset = 0)
                } else {
                    Log.d(TAG, "Multiple genres selected: $selectedGenres, loading 10 songs total with random genre")
                    val allSongs = mutableListOf<Track>()
                    for (i in 1..10) {
                        val randomGenre = selectedGenres.random()
                        val result = trackUseCases.getRandomSongsByGenre(randomGenre, size = 1)
                        result.onSuccess { songs ->
                            allSongs.addAll(songs)
                            Log.d(TAG, "Got ${songs.size} song for genre: $randomGenre (call $i)")
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

                playlist.shuffle()
                Log.d(TAG, "Total playlist after shuffle: ${playlist.size}")

                val initialTracks = playlist.take(10)
                currentPlaylistPosition = 10
                Log.d(TAG, "First 10 tracks: ${initialTracks.map { it.title }}")

                _uiState.update { it.copy(
                    isLoading = false,
                    dynamicPlaylist = initialTracks
                ) }

                playerManager.setLoadMoreCallback {
                    Log.d(TAG, "=== PlayerManager callback triggered! ===")
                    checkAndLoadMore()
                }

                if (initialTracks.isNotEmpty()) {
                    playerManager.setPlaylist(initialTracks, startPlaying = true)
                    Log.d(TAG, "Started playback with PlayerManager")
                }

                Log.d(TAG, "Starting playback with ${initialTracks.size} tracks")

            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreTracks() {
        Log.d(TAG, "=== loadMoreTracks ENTRY ===")
        if (isLoadingMore) {
            Log.d(TAG, "loadMoreTracks: already loading, returning")
            return
        }

        isLoadingMore = true
        Log.d(TAG, "loadMoreTracks: loading more songs from server")

        viewModelScope.launch {
            try {
                val songsResult: Result<List<Track>>

                if (currentGenreFilter.isEmpty()) {
                    Log.d(TAG, "loadMoreTracks: Loading 10 random songs...")
                    songsResult = trackUseCases.getRandomSongs(size = 10)
                } else if (currentGenreFilter.size == 1) {
                    val genre = currentGenreFilter.first()
                    Log.d(TAG, "loadMoreTracks: Loading 10 songs for genre: $genre")
                    songsResult = trackUseCases.getSongsByGenre(genre, count = 10, offset = 0)
                } else {
                    Log.d(TAG, "loadMoreTracks: Loading 10 songs total with random genre from: $currentGenreFilter")
                    val allSongs = mutableListOf<Track>()
                    for (i in 1..10) {
                        val randomGenre = currentGenreFilter.random()
                        val result = trackUseCases.getRandomSongsByGenre(randomGenre, size = 1)
                        result.onSuccess { songs ->
                            allSongs.addAll(songs)
                        }
                    }
                    songsResult = Result.success(allSongs)
                }

                songsResult.onSuccess { newSongs ->
                    if (newSongs.isNotEmpty()) {
                        playerManager.appendToPlaylist(newSongs)
                        Log.d(TAG, "loadMoreTracks: Appended ${newSongs.size} new songs to PlayerManager")
                    } else {
                        Log.w(TAG, "loadMoreTracks: No new songs received")
                    }
                }.onFailure { error ->
                    Log.e(TAG, "loadMoreTracks: Error: ${error.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "loadMoreTracks EXCEPTION: ${e.message}", e)
            } finally {
                isLoadingMore = false
                Log.d(TAG, "loadMoreTracks EXIT")
            }
        }
    }

    fun checkAndLoadMore() {
        Log.d(TAG, "=== checkAndLoadMore called - triggering loadMoreTracks ===")
        loadMoreTracks()
    }
}