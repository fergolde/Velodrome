package com.fergolde.velodrome.presentation.screen.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.usecase.AlbumUseCases
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.PlaylistUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import com.fergolde.velodrome.util.shuffledWithArtistSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG_OFFLINE = "LOCAL_OFFLINE"

/**
 * ViewModel for the Navidrome Home Screen.
 * Manages the state of all home screen features including:
 * - Recently added albums
 * - Most played albums
 * - Initial sync to local DB for search
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val albumUseCases: AlbumUseCases,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playlistUseCases: PlaylistUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        syncIfEmpty()
        loadInitialData()
    }

    private fun syncIfEmpty() {
        viewModelScope.launch {
            // COUNT(*) probes instead of materializing whole tables just to call isEmpty()
            if (albumUseCases.albumCount() == 0) {
                albumUseCases.syncAlbums()
            }
            if (artistUseCases.artistCount() == 0) {
                artistUseCases.syncArtists()
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
        loadPlaylists()
    }

    /**
     * Loads the latest added albums.
     * @param size Number of albums to fetch
     */
    fun loadLatestAlbums(size: Int = 20) {
        viewModelScope.launch {
            albumUseCases.getLatestAlbums(size)
                .onSuccess { albums ->
                    _uiState.update { it.copy(latestAlbums = albums) }
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
                        it.copy(topAlbums = albums)
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

    private fun loadPlaylists() {
        viewModelScope.launch {
            runCatching {
                playlistUseCases.getPlaylists().getOrDefault(emptyList())
            }.onSuccess { list ->
                _uiState.update { it.copy(playlists = list.take(10)) }
            }
        }
    }

    /**
     * Plays a random playlist from all available albums.
     * Uses SmartRadioEngine for shuffle logic.
     */
    fun playShuffle() {
        viewModelScope.launch {
            smartRadioEngine.startRadio(RadioContext.Random)
        }
    }

    /**
     * Plays the Top 100 most played songs.
     */
    fun playTop100() {
        viewModelScope.launch {
            trackUseCases.getTopGlobalTracks(size = 100).onSuccess { tracks ->
                if (tracks.isNotEmpty()) {
                    playerManager.playNow(tracks.shuffledWithArtistSpacing())
                    playerManager.setLoadMoreCallback { /* no auto-load for static list */ }
                }
            }
        }
    }

    /**
     * Plays only locally cached/offline tracks.
     */
    fun playOfflineOnly() {
        Log.d(TAG_OFFLINE, "=== playOfflineOnly() called ===")
        viewModelScope.launch {
            Log.d(TAG_OFFLINE, "Calling getOfflineTracks use case...")
            val offlineTracks = trackUseCases.getOfflineTracks()
            Log.d(TAG_OFFLINE, "getOfflineTracks returned: ${offlineTracks.size} tracks")
            if (offlineTracks.isNotEmpty()) {
                Log.d(TAG_OFFLINE, "Calling playerManager.playNow with ${offlineTracks.size} tracks")
                playerManager.playNow(offlineTracks.shuffledWithArtistSpacing())
                playerManager.setLoadMoreCallback { /* no auto-load for offline list */ }
                Log.d(TAG_OFFLINE, "playerManager.playNow called successfully")
            } else {
                Log.d(TAG_OFFLINE, "No offline tracks found - playlist stays empty")
            }
        }
    }

    /**
     * Starts the discovery radio mode with random songs.
     */
    fun playDiscovery() {
        smartRadioEngine.startRadio(RadioContext.Random)
    }

}