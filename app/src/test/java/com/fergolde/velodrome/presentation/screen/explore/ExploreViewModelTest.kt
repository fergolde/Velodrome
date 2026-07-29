package com.fergolde.velodrome.presentation.screen.explore

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
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
class ExploreViewModelTest {

    private val albumUseCases: AlbumUseCases = mockk()
    private val artistUseCases: ArtistUseCases = mockk()
    private val trackUseCases: TrackUseCases = mockk()
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val sampleAlbum = Album(id = "a1", artistId = "art1", artistName = "Artist", title = "Album", year = 2020, genre = "Rock", coverUrl = "cov-1")
    private val sampleArtist = Artist(id = "art1", name = "Artist", albumCount = 3, coverUrl = null)
    private val sampleTrack = Track(id = "t1", albumId = "a1", title = "Song", durationSec = 180, sizeBytes = 5000000, bitrate = 320, trackNumber = 1)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { albumUseCases.getMinYear() } returns 1980
        coEvery { artistUseCases.observeArtists() } returns flowOf(emptyList())
        coEvery { albumUseCases.getRandomAlbums(20) } returns Result.success(emptyList())
        coEvery { albumUseCases.getRandomAlbums(10) } returns Result.success(emptyList())
        coEvery { albumUseCases.getGenres() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ExploreViewModel {
        return ExploreViewModel(albumUseCases, artistUseCases, trackUseCases, playerManager, smartRadioEngine)
    }

    @Test
    fun initialState_isDefault() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSearching)
        assertEquals("", state.searchQuery)
        assertTrue(state.genres.isEmpty())
        assertTrue(state.selectedGenres.isEmpty())
        assertNull(state.selectedYearRange)
    }

    @Test
    fun onGenreToggle_addsGenre() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onGenreToggle("Rock")

        assertTrue(vm.uiState.value.selectedGenres.contains("Rock"))
    }

    @Test
    fun onGenreToggle_removesGenre() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onGenreToggle("Rock")
        vm.onGenreToggle("Rock")

        assertFalse(vm.uiState.value.selectedGenres.contains("Rock"))
    }

    @Test
    fun onGenreToggle_multipleGenres() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onGenreToggle("Rock")
        vm.onGenreToggle("Pop")

        assertEquals(setOf("Rock", "Pop"), vm.uiState.value.selectedGenres)
    }

    @Test
    fun onSearchQueryChange_updatesQuery() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onSearchQueryChange("test query")

        assertEquals("test query", vm.uiState.value.searchQuery)
    }

    @Test
    fun clearSearch_resetsState() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.clearSearch()

        assertEquals("", vm.uiState.value.searchQuery)
        assertFalse(vm.uiState.value.isSearching)
    }

    @Test
    fun onPlayGenres_callsSmartRadioEngine() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPlayGenres()
        advanceUntilIdle()

        verify { smartRadioEngine.startRadio(any()) }
    }

    @Test
    fun onYearRangeSelected_updatesState() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val range = 2000..2020
        vm.onYearRangeSelected(range)

        assertEquals(range, vm.uiState.value.selectedYearRange)
    }

    @Test
    fun onYearRangeSelected_null() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onYearRangeSelected(null)

        assertNull(vm.uiState.value.selectedYearRange)
    }

    @Test
    fun playSearchedTrack_delegatesToPlayerManager() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.playSearchedTrack(sampleTrack)

        verify { playerManager.playTrack(sampleTrack) }
    }

    @Test
    fun onPlayTrackNow_delegates() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPlayTrackNow(sampleTrack)

        verify { playerManager.playNow(sampleTrack) }
    }

    @Test
    fun onPlayTrackNext_delegates() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onPlayTrackNext(sampleTrack)

        verify { playerManager.playNext(sampleTrack) }
    }

    @Test
    fun onAddTrackToQueue_delegates() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAddTrackToQueue(sampleTrack)

        verify { playerManager.addToQueue(sampleTrack) }
    }

    @Test
    fun loadContent_fetchesGenres() = runTest {
        coEvery { albumUseCases.getGenres() } returns Result.success(listOf("Rock", "Pop"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(listOf("Rock", "Pop"), vm.uiState.value.genres)
    }

    @Test
    fun loadContent_failure_setsError() = runTest {
        coEvery { albumUseCases.getGenres() } returns Result.failure(Exception("API error"))

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("API error", vm.uiState.value.error)
    }
}
