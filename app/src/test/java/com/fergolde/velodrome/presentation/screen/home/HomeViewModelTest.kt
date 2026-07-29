package com.fergolde.velodrome.presentation.screen.home

import android.util.Log
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.AlbumUseCases
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val albumUseCases: AlbumUseCases = mockk(relaxed = true)
    private val artistUseCases: ArtistUseCases = mockk(relaxed = true)
    private val trackUseCases: TrackUseCases = mockk(relaxed = true)
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleAlbum = Album(id = "a1", artistId = "art1", artistName = "Artist", title = "Album", year = 2020, genre = "Rock", coverUrl = "cov-1")
    private val sampleTrack = Track(id = "t1", albumId = "a1", title = "Song", durationSec = 180, sizeBytes = 5000000, bitrate = 320, trackNumber = 1)

    private val isPlayingFlow = MutableStateFlow(false)
    private val currentTrackFlow = MutableStateFlow<Track?>(null)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { playerManager.isPlaying } returns isPlayingFlow
        every { playerManager.currentTrack } returns currentTrackFlow
        coEvery { albumUseCases.observeAlbums() } returns flowOf(emptyList())
        coEvery { artistUseCases.observeArtists() } returns flowOf(emptyList())
        coEvery { albumUseCases.getLatestAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumUseCases.getTopAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumUseCases.getRecentlyPlayedAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumUseCases.getRandomAlbums(any()) } returns Result.success(emptyList())
        coEvery { albumUseCases.getGenres() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(albumUseCases, artistUseCases, trackUseCases, playerManager, smartRadioEngine)
    }

    @Test
    fun initialState_isLoading() = runTest {
        val vm = createViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun loadLatestAlbums_success() = runTest {
        coEvery { albumUseCases.getLatestAlbums(20) } returns Result.success(listOf(sampleAlbum))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(sampleAlbum), vm.uiState.value.latestAlbums)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun loadLatestAlbums_failure() = runTest {
        coEvery { albumUseCases.getLatestAlbums(20) } returns Result.failure(Exception("API error"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("API error", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun loadTopAlbums_success() = runTest {
        coEvery { albumUseCases.getTopAlbums(20) } returns Result.success(listOf(sampleAlbum))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(sampleAlbum), vm.uiState.value.topAlbums)
        assertEquals(sampleAlbum, vm.uiState.value.featuredAlbum)
    }

    @Test
    fun loadRecentlyPlayedAlbums_success() = runTest {
        coEvery { albumUseCases.getRecentlyPlayedAlbums(20) } returns Result.success(listOf(sampleAlbum))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(sampleAlbum), vm.uiState.value.recentlyPlayedAlbums)
    }

    @Test
    fun loadRandomAlbums_success() = runTest {
        coEvery { albumUseCases.getRandomAlbums(20) } returns Result.success(listOf(sampleAlbum))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf(sampleAlbum), vm.uiState.value.randomAlbums)
    }

    @Test
    fun playShuffle_startsRadio() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.playShuffle()
        advanceUntilIdle()

        verify { smartRadioEngine.startRadio(any()) }
    }

    @Test
    fun playTop100_fetchesTracksAndPlays() = runTest {
        coEvery { trackUseCases.getTopGlobalTracks(100) } returns Result.success(listOf(sampleTrack))

        val vm = createViewModel()
        advanceUntilIdle()

        vm.playTop100()
        advanceUntilIdle()

        verify { playerManager.playNow(listOf(sampleTrack)) }
    }

    @Test
    fun playTop100_emptyPlaylist_doesNotPlay() = runTest {
        coEvery { trackUseCases.getTopGlobalTracks(100) } returns Result.success(emptyList())

        val vm = createViewModel()
        advanceUntilIdle()

        vm.playTop100()
        advanceUntilIdle()

        verify(exactly = 0) { playerManager.playNow(any<List<Track>>()) }
    }

    @Test
    fun playOfflineOnly_fetchesOfflineTracks() = runTest {
        coEvery { trackUseCases.getOfflineTracks() } returns listOf(sampleTrack)

        val vm = createViewModel()
        advanceUntilIdle()

        vm.playOfflineOnly()
        advanceUntilIdle()

        verify { playerManager.playNow(any<List<Track>>()) }
    }

    @Test
    fun playDiscovery_startsRadio() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.playDiscovery()

        verify { smartRadioEngine.startRadio(any()) }
    }

    @Test
    fun syncWithPlayerManager_isPlayingUpdates() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        isPlayingFlow.value = true
        advanceUntilIdle()

        assertTrue(vm.uiState.value.isPlaying)
    }

    @Test
    fun syncWithPlayerManager_currentTrackUpdates() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        currentTrackFlow.value = sampleTrack
        advanceUntilIdle()

        assertEquals(sampleTrack.id, vm.uiState.value.currentTrackId)
    }
}
