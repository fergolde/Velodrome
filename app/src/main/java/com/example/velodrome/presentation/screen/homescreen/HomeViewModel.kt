package com.example.velodrome.presentation.screen.homescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.data.local.datasource.LocalMusicDataSource
import com.example.velodrome.data.local.entity.AlbumEntity
import com.example.velodrome.data.local.entity.ArtistEntity
import com.example.velodrome.domain.repository.NavidromeRepository
import com.example.velodrome.domain.usecase.GetAlbumsByGenreUseCase
import com.example.velodrome.domain.usecase.GetAlbumsByYearUseCase
import com.example.velodrome.domain.usecase.GetGenresUseCase
import com.example.velodrome.domain.usecase.GetLatestAlbumsUseCase
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
import com.example.velodrome.domain.usecase.GetRecentlyPlayedAlbumsUseCase
import com.example.velodrome.domain.usecase.GetTopAlbumsUseCase
import com.example.velodrome.domain.usecase.GetTracksUseCase
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Navidrome Home Screen.
 * Manages the state of all home screen features including:
 * - Recently added albums
 * - Most played albums
 * - Genre and year filtering
 * - Playback state (synced with PlayerManager)
 * - Initial sync to local DB for search
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLatestAlbumsUseCase: GetLatestAlbumsUseCase,
    private val getTopAlbumsUseCase: GetTopAlbumsUseCase,
    private val getRecentlyPlayedAlbumsUseCase: GetRecentlyPlayedAlbumsUseCase,
    private val getRandomAlbumsUseCase: GetRandomAlbumsUseCase,
    private val getAlbumsByYearUseCase: GetAlbumsByYearUseCase,
    private val getAlbumsByGenreUseCase: GetAlbumsByGenreUseCase,
    private val getGenresUseCase: GetGenresUseCase,
    private val getTracksUseCase: GetTracksUseCase,
    private val navidromeRepository: NavidromeRepository,
    private val localMusicDataSource: LocalMusicDataSource,
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        syncWithPlayerManager()
        syncDataToLocal()
    }

    /**
     * Sync all artists and albums to local DB on first load
     * This enables offline search in Explore screen
     */
    private fun syncDataToLocal() {
        viewModelScope.launch {
            try {
                val artistCount = localMusicDataSource.getArtistCount()
                val albumCount = localMusicDataSource.getAlbumCount()

                Log.d("HomeViewModel", "Local DB: $artistCount artists, $albumCount albums")

                // Load and sync all artists
                if (artistCount == 0) {
                    Log.d("HomeViewModel", "Syncing artists to local DB...")
                    var offset = 0
                    while (offset < 10000) {
                        val result = navidromeRepository.getArtists(offset = offset, size = 500)
                        val artists = result.getOrNull() ?: break
                        if (artists.isEmpty()) break

                        val entities = artists.map { artist ->
                            ArtistEntity(
                                id = artist.id,
                                name = artist.name,
                                albumCount = artist.albumCount,
                                coverUrl = artist.coverUrl
                            )
                        }
                        localMusicDataSource.insertArtists(entities)
                        Log.d("HomeViewModel", "Synced ${entities.size} artists")
                        if (artists.size < 500) break
                        offset += 500
                    }
                    Log.d("HomeViewModel", "Artist sync complete")
                }

                // Load and sync all albums (using alphabetical to get more)
                if (albumCount == 0) {
                    Log.d("HomeViewModel", "Syncing albums to local DB...")
                    var offset = 0
                    while (offset < 10000) {
                        val result = navidromeRepository.getAllAlbumsFromServer(offset = offset, size = 500)
                        val albums = result.getOrNull() ?: break
                        if (albums.isEmpty()) break

                        val entities = albums.map { album ->
                            AlbumEntity(
                                id = album.id,
                                artistId = album.artistId,
                                artistName = album.artistName,
                                title = album.title,
                                year = album.year,
                                genre = album.genre,
                                coverUrl = album.coverUrl
                            )
                        }
                        localMusicDataSource.insertAlbums(entities)
                        Log.d("HomeViewModel", "Synced ${entities.size} albums")
                        if (entities.size < 500) break
                        offset += 500
                    }
                    Log.d("HomeViewModel", "Album sync complete")
                }

                Log.d("HomeViewModel", "Initial sync complete!")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error syncing data", e)
            }
        }
    }

    /**
     * Sync UI state with PlayerManager injected instance
     */
    private fun syncWithPlayerManager() {
        viewModelScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        viewModelScope.launch {
            playerManager.currentTrack.collect { track ->
                _uiState.update { it.copy(currentTrackId = track?.id) }
            }
        }
    }

    /**
     * Loads all initial data for the home screen.
     * Called on ViewModel initialization.
     */
    private fun loadInitialData() {
        loadLatestAlbums()
        loadTopAlbums()
        loadRecentlyPlayedAlbums()
        loadRandomAlbums()
        loadGenres()
        loadAvailableYears()
    }

    /**
     * Loads the latest added albums.
     * @param size Number of albums to fetch
     */
    fun loadLatestAlbums(size: Int = 20) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getLatestAlbumsUseCase(size)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            latestAlbums = albums,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to load latest albums",
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
        }
    }

    /**
     * Loads the most played albums.
     * @param size Number of albums to fetch
     */
    fun loadTopAlbums(size: Int = 20) {
        viewModelScope.launch {
            getTopAlbumsUseCase(size)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            topAlbums = albums,
                            featuredAlbum = albums.firstOrNull()
                        )
                    }
                }
                // Error handling is covered by isLoading state
        }
    }

    /**
     * Loads recently played albums.
     */
    fun loadRecentlyPlayedAlbums(size: Int = 20) {
        viewModelScope.launch {
            getRecentlyPlayedAlbumsUseCase(size)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(recentlyPlayedAlbums = albums)
                    }
                }
        }
    }

    /**
     * Loads random albums.
     */
    fun loadRandomAlbums(size: Int = 20) {
        viewModelScope.launch {
            getRandomAlbumsUseCase(size)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(randomAlbums = albums)
                    }
                }
        }
    }

    /**
     * Loads available genres for filtering.
     */
    private fun loadGenres() {
        viewModelScope.launch {
            getGenresUseCase()
                .onSuccess { genres ->
                    _uiState.update { it.copy(genres = genres) }
                }
        }
    }

    /**
     * Loads available years from albums for filtering.
     */
    private fun loadAvailableYears() {
        viewModelScope.launch {
            getLatestAlbumsUseCase(50)
                .onSuccess { albums ->
                    val years = albums
                        .mapNotNull { it.year }
                        .distinct()
                        .sortedDescending()
                    _uiState.update { it.copy(availableYears = years) }
                }
        }
    }

    /**
     * Loads albums filtered by year.
     * @param year The year to filter by
     */
    private fun loadAlbumsByYear(year: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getAlbumsByYearUseCase(year)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            filteredAlbums = albums,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to filter by year",
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Loads albums filtered by genre.
     * @param genre The genre to filter by
     */
    private fun loadAlbumsByGenre(genre: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            getAlbumsByGenreUseCase(genre)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            filteredAlbums = albums,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to filter by genre",
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Retries loading all data after an error.
     */
    fun retry() {
        loadInitialData()
    }

    /**
     * Toggles play/pause for the current track.
     */
    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    /**
     * Plays a random playlist from all available albums.
     * Uses API to get random songs directly with infinite scroll.
     */
    fun playShuffle() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val songsResult = navidromeRepository.getRandomSongs(size = 10)

                songsResult.onSuccess { songs ->
                    Log.d("HomeViewModel", "Loaded ${songs.size} random songs")

                    if (songs.isNotEmpty()) {
                        val shuffledSongs = songs.shuffled().take(10)

                        playerManager.setLoadMoreCallback {
                            Log.d("HomeViewModel", "Home shuffle: loading more songs")
                            loadMoreRandomSongs()
                        }

                        playerManager.setPlaylist(shuffledSongs, startPlaying = true)
                        Log.d("HomeViewModel", "Started shuffle playback with ${shuffledSongs.size} songs")
                    }

                    _uiState.update { it.copy(isLoading = false, isPlaying = true) }
                }.onFailure { error ->
                    Log.e("HomeViewModel", "Error loading random songs: ${error.message}")
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in playShuffle", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Load more random songs for infinite scroll
     */
    private fun loadMoreRandomSongs() {
        viewModelScope.launch {
            try {
                val songsResult = navidromeRepository.getRandomSongs(size = 10)
                songsResult.onSuccess { songs ->
                    if (songs.isNotEmpty()) {
                        playerManager.appendToPlaylist(songs)
                        Log.d("HomeViewModel", "Appended ${songs.size} more random songs")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading more random songs", e)
            }
        }
    }
}