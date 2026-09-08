package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.lifecycle.SavedStateHandle
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.GetArtistUseCase
import com.fergolde.velodrome.domain.usecase.ObserveTracksByAlbumUseCase
import com.fergolde.velodrome.domain.usecase.SyncTracksForAlbumUseCase
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val artist = Artist(id = "a1", name = "Artist", albumCount = 1, coverUrl = null)
    private val album = Album(id = "al1", artistId = "a1", artistName = "Artist", title = "Album", year = 2020, genre = null, coverUrl = null)

    private val getArtistUseCase: GetArtistUseCase = mockk()
    private val syncTracksForAlbumUseCase: SyncTracksForAlbumUseCase = mockk()
    private val observeTracksByAlbumUseCase: ObserveTracksByAlbumUseCase = mockk()
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)

    private val artistUseCases = ArtistUseCases(
        getArtist = getArtistUseCase,
        searchLocal = mockk(),
        syncArtists = mockk(),
        observeArtists = mockk(),
        artistCount = mockk()
    )

    private val trackUseCases = TrackUseCases(
        observeTracksByAlbum = observeTracksByAlbumUseCase,
        syncTracksForAlbum = syncTracksForAlbumUseCase,
        getRandomSongs = mockk(),
        searchRemoteTracks = mockk(),
        getOfflineTracks = mockk(),
        getTopGlobalTracks = mockk(),
        getAllLocalTracks = mockk(),
        getTracksForAlbumIds = mockk()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getArtistUseCase(any()) } returns Result.success(ArtistWithAlbums(artist, listOf(album)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ArtistDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("artistId" to "a1"))
        return ArtistDetailViewModel(
            savedStateHandle = savedStateHandle,
            artistUseCases = artistUseCases,
            trackUseCases = trackUseCases,
            playerManager = playerManager,
            smartRadioEngine = smartRadioEngine
        )
    }

    @Test
    fun `playAll clears isPreparingPlayback on error`() = runTest {
        coEvery { syncTracksForAlbumUseCase(any()) } throws RuntimeException("network down")

        val viewModel = createViewModel()
        viewModel.playAll()

        assertFalse(viewModel.uiState.value.isPreparingPlayback)
    }

    @Test
    fun `shuffleAll clears isPreparingPlayback on error`() = runTest {
        coEvery { syncTracksForAlbumUseCase(any()) } throws RuntimeException("network down")

        val viewModel = createViewModel()
        viewModel.shuffleAll()

        assertFalse(viewModel.uiState.value.isPreparingPlayback)
    }

    @Test
    fun `addToQueue clears isPreparingPlayback on error`() = runTest {
        coEvery { syncTracksForAlbumUseCase(any()) } throws RuntimeException("network down")

        val viewModel = createViewModel()
        viewModel.addToQueue()

        assertFalse(viewModel.uiState.value.isPreparingPlayback)
    }

    @Test
    fun `startArtistRadio does not set isPreparingPlayback`() = runTest {
        val viewModel = createViewModel()
        viewModel.startArtistRadio()

        assertFalse(viewModel.uiState.value.isPreparingPlayback)
        verify { smartRadioEngine.startRadio(any()) }
    }
}
