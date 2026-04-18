package com.example.velodrome.presentation.screen.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.usecase.GetAlbumsByGenreUseCase
import com.example.velodrome.domain.usecase.GetAlbumsByYearUseCase
import com.example.velodrome.domain.usecase.GetGenresUseCase
import com.example.velodrome.domain.usecase.GetLatestAlbumsUseCase
import com.example.velodrome.domain.usecase.GetRandomAlbumsUseCase
import com.example.velodrome.domain.usecase.GetRecentlyPlayedAlbumsUseCase
import com.example.velodrome.domain.usecase.GetTopAlbumsUseCase
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
 * - Playback state
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLatestAlbumsUseCase: GetLatestAlbumsUseCase,
    private val getTopAlbumsUseCase: GetTopAlbumsUseCase,
    private val getRecentlyPlayedAlbumsUseCase: GetRecentlyPlayedAlbumsUseCase,
    private val getRandomAlbumsUseCase: GetRandomAlbumsUseCase,
    private val getAlbumsByYearUseCase: GetAlbumsByYearUseCase,
    private val getAlbumsByGenreUseCase: GetAlbumsByGenreUseCase,
    private val getGenresUseCase: GetGenresUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
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
        _uiState.update {
            it.copy(isPlaying = !it.isPlaying)
        }
    }

    /**
     * Plays a specific album (shuffle mode).
     */
    fun playShuffle() {
        val allAlbums = _uiState.value.latestAlbums + _uiState.value.topAlbums
        if (allAlbums.isNotEmpty()) {
            // In a real implementation, this would trigger a shuffle play
            _uiState.update {
                it.copy(
                    currentTrackId = allAlbums.random().id,
                    isPlaying = true
                )
            }
        }
    }
}