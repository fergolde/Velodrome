package com.fergolde.velodrome.presentation.screen.albumdetail

import androidx.lifecycle.SavedStateHandle
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.AlbumUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * El detalle de álbum solo cargaba desde `init`: un fallo de red dejaba la
 * pantalla clavada en el error sin forma de reintentar. Estos tests fijan que
 * `retry()` vuelve a lanzar el loader, limpia el error y no deja dos
 * observadores de tracks vivos a la vez.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val album = Album(
        id = "al1",
        artistId = "a1",
        artistName = "Artist",
        title = "Album",
        year = 2020,
        genre = null,
        coverUrl = null
    )
    private val track = Track(
        id = "t1",
        albumId = "al1",
        title = "Song",
        durationSec = 180,
        sizeBytes = 5_000_000,
        trackNumber = 1
    )

    private val albumUseCases: AlbumUseCases = mockk()
    private val trackUseCases: TrackUseCases = mockk()
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playerManager.currentTrackId } returns MutableStateFlow(null)
        every { trackUseCases.observeTracksByAlbum("al1") } returns MutableStateFlow(emptyList())
        coEvery { trackUseCases.syncTracksForAlbum("al1") } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AlbumDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("albumId" to "al1"))
        return AlbumDetailViewModel(
            savedStateHandle = savedStateHandle,
            albumUseCases = albumUseCases,
            trackUseCases = trackUseCases,
            playerManager = playerManager,
            smartRadioEngine = smartRadioEngine
        )
    }

    @Test
    fun `la carga inicial publica album y tracks`() = runTest {
        coEvery { albumUseCases.getAlbum("al1") } returns Result.success(album)
        every { trackUseCases.observeTracksByAlbum("al1") } returns flow { emit(listOf(track)) }

        val viewModel = createViewModel()

        assertEquals(album, viewModel.uiState.value.album)
        assertEquals(listOf(track), viewModel.uiState.value.tracks)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retry limpia el error y vuelve a pedir el album`() = runTest {
        coEvery { albumUseCases.getAlbum("al1") } returns Result.failure(RuntimeException("sin red"))
        val viewModel = createViewModel()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { albumUseCases.getAlbum("al1") } returns Result.success(album)
        viewModel.retry()

        assertNull(viewModel.uiState.value.error)
        assertEquals(album, viewModel.uiState.value.album)
        coVerify(exactly = 2) { albumUseCases.getAlbum("al1") }
    }

    @Test
    fun `retry no deja dos observadores de tracks vivos`() = runTest {
        // Flujo que nunca termina y cuenta cuántas recolecciones conviven.
        var active = 0
        var maxActive = 0
        every { trackUseCases.observeTracksByAlbum("al1") } returns flow {
            active++
            maxActive = maxOf(maxActive, active)
            try {
                emit(emptyList())
                awaitCancellation()
            } finally {
                active--
            }
        }
        coEvery { albumUseCases.getAlbum("al1") } returns Result.success(album)

        val viewModel = createViewModel()
        assertEquals(1, maxActive)

        viewModel.retry()

        assertEquals(1, maxActive)
    }
}
