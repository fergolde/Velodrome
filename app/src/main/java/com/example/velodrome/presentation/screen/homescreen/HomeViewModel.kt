package com.example.velodrome.presentation.screen.homescreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val getTracksUseCase: GetTracksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        syncWithPlayerManager()
    }

    /**
     * Sync UI state with PlayerManager singleton
     */
    private fun syncWithPlayerManager() {
        viewModelScope.launch {
            PlayerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        viewModelScope.launch {
            PlayerManager.currentTrack.collect { track ->
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
     * Refreshes all data.
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadInitialData()
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
     * Filters albums by year.
     * Clears genre filter when year is selected.
     * @param year The year to filter by, or null to clear filter
     */
    fun onYearSelected(year: Int?) {
        _uiState.update {
            it.copy(
                selectedYear = year,
                selectedGenre = null,
                filteredAlbums = emptyList()
            )
        }

        if (year != null) {
            loadAlbumsByYear(year)
        }
    }

    /**
     * Filters albums by genre.
     * Clears year filter when genre is selected.
     * @param genre The genre to filter by, or null to clear filter
     */
    fun onGenreSelected(genre: String?) {
        _uiState.update {
            it.copy(
                selectedGenre = genre,
                selectedYear = null,
                filteredAlbums = emptyList()
            )
        }

        if (genre != null) {
            loadAlbumsByGenre(genre)
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
     * Clears all active filters.
     */
    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedYear = null,
                selectedGenre = null,
                filteredAlbums = emptyList()
            )
        }
    }

    /**
     * Retries loading all data after an error.
     */
    fun retry() {
        loadInitialData()
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Updates the playback state.
     * @param trackId The ID of the track being played
     * @param isPlaying Whether the track is currently playing
     */
    fun updatePlaybackState(trackId: String?, isPlaying: Boolean) {
        _uiState.update {
            it.copy(
                currentTrackId = trackId,
                isPlaying = isPlaying
            )
        }
    }

    /**
     * Toggles play/pause for the current track.
     */
    fun togglePlayPause() {
        PlayerManager.togglePlayPause()
    }

    /**
     * Plays a random playlist from all available albums.
     * Similar to ExploreScreen genre selection but for all albums.
     */
    fun playShuffle() {
        val allAlbums = _uiState.value.latestAlbums + _uiState.value.topAlbums
        if (allAlbums.isEmpty()) {
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val playlist = mutableListOf<com.example.velodrome.domain.model.Track>()

                // Load tracks from all albums
                for (album in allAlbums) {
                    val result = getTracksUseCase(album.id)
                    result.onSuccess { tracks ->
                        if (tracks.isNotEmpty()) {
                            playlist.addAll(tracks)
                        }
                    }
                }

                 Log.d("HomeViewModel", "Total tracks loaded for shuffle: ${playlist.size}")

                // Shuffle the full playlist
                playlist.shuffle()

                // Start playback with first 10 tracks
                val initialTracks = playlist.take(10)
                Log.d("HomeViewModel", "Shuffle playlist (first 10): ${initialTracks.map { it.title }}")

                _uiState.update { it.copy(isLoading = false, isPlaying = true) }

                // Set playlist in PlayerManager to start playback
                if (initialTracks.isNotEmpty()) {
                    PlayerManager.setPlaylist(initialTracks, startPlaying = true)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error in playShuffle", e)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Get current track from PlayerManager for MiniPlayer display
     */
    fun getCurrentTrack() = PlayerManager.currentTrack.value
}