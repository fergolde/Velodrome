package com.example.velodrome.presentation.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.usecase.AlbumUseCases
import com.example.velodrome.domain.usecase.ArtistUseCases
import com.example.velodrome.domain.usecase.TrackUseCases
import com.example.velodrome.presentation.audio.RadioContext
import com.example.velodrome.presentation.audio.SmartRadioEngine
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG_OFFLINE = "LOCAL_OFFLINE"

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
    private val albumUseCases: AlbumUseCases,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        syncIfEmpty()
        loadInitialData()
        syncWithPlayerManager()
    }

    private fun syncIfEmpty() {
        viewModelScope.launch {
            val localAlbums = albumUseCases.observeAlbums().first()
            if (localAlbums.isEmpty()) {
                albumUseCases.syncAlbums()
            }
            val localArtists = artistUseCases.observeArtists().first()
            if (localArtists.isEmpty()) {
                artistUseCases.syncArtists()
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
            albumUseCases.getLatestAlbums(size)
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
            albumUseCases.getTopAlbums(size)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            topAlbums = albums,
                            featuredAlbum = albums.firstOrNull()
                        )
                    }
                }
        }
    }

    /**
     * Loads recently played albums.
     */
    fun loadRecentlyPlayedAlbums(size: Int = 20) {
        viewModelScope.launch {
            albumUseCases.getRecentlyPlayedAlbums(size)
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
            albumUseCases.getRandomAlbums(size)
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
            albumUseCases.getGenres()
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
            albumUseCases.getLatestAlbums(50)
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
     * Plays a random playlist from all available albums.
     * Uses SmartRadioEngine for shuffle logic.
     */
    fun playShuffle() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            smartRadioEngine.startRadio(RadioContext.Random)
            _uiState.update { it.copy(isLoading = false, isPlaying = true) }
        }
    }

    /**
     * Plays the Top 100 most played songs.
     * Note: Navidrome getTopSongs requires artist param (Last.fm lookup).
     * Fallback: getRandomSongs as proxy for library favorites.
     */
    fun playTop100() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            trackUseCases.getRandomSongs(size = 100).onSuccess { tracks ->
                if (tracks.isNotEmpty()) {
                    playerManager.playNow(tracks)
                    playerManager.setLoadMoreCallback { /* no auto-load for static list */ }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Plays only locally cached/offline tracks.
     */
    fun playOfflineOnly() {
        Log.d(TAG_OFFLINE, "=== playOfflineOnly() called ===")
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            Log.d(TAG_OFFLINE, "Calling getOfflineTracks use case...")
            val offlineTracks = trackUseCases.getOfflineTracks()
            Log.d(TAG_OFFLINE, "getOfflineTracks returned: ${offlineTracks.size} tracks")
            if (offlineTracks.isNotEmpty()) {
                Log.d(TAG_OFFLINE, "Calling playerManager.playNow with ${offlineTracks.size} tracks")
                playerManager.playNow(offlineTracks.shuffled())
                playerManager.setLoadMoreCallback { /* no auto-load for offline list */ }
                Log.d(TAG_OFFLINE, "playerManager.playNow called successfully")
            } else {
                Log.d(TAG_OFFLINE, "No offline tracks found - playlist stays empty")
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Starts the discovery radio mode with random songs.
     */
    fun playDiscovery() {
        _uiState.update { it.copy(isLoading = true) }
        smartRadioEngine.startRadio(RadioContext.Random)
        _uiState.update { it.copy(isLoading = false) }
    }
}