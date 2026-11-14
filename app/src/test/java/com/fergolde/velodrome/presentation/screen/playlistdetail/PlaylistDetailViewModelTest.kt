package com.fergolde.velodrome.presentation.screen.playlistdetail

import androidx.lifecycle.SavedStateHandle
import com.fergolde.velodrome.domain.model.Playlist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.PlaylistUseCases
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
 * El detalle de playlist solo cargaba desde `init`: un fallo dejaba el error en
 * pantalla sin acción posible. Estos tests fijan la carga inicial y que
 * `retry()` vuelve a lanzar el loader limpiando el error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val track = Track(
        id = "t1",
        albumId = "al1",
        title = "Song",
        durationSec = 180,
        sizeBytes = 5_000_000,
        trackNumber = 1
    )
    private val playlist = Playlist(
        id = "p1",
        name = "Playlist",
        songCount = 1,
        tracks = listOf(track)
    )

    private val playlistUseCases: PlaylistUseCases = mockk()
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { playerManager.currentTrackId } returns MutableStateFlow(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): PlaylistDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("playlistId" to "p1"))
        return PlaylistDetailViewModel(
            savedStateHandle = savedStateHandle,
            playlistUseCases = playlistUseCases,
            playerManager = playerManager,
            smartRadioEngine = smartRadioEngine
        )
    }

    @Test
    fun `la carga inicial publica la playlist y sus tracks`() = runTest {
        coEvery { playlistUseCases.getPlaylist("p1") } returns Result.success(playlist)

        val viewModel = createViewModel()

        assertEquals(playlist, viewModel.uiState.value.playlist)
        assertEquals(listOf(track), viewModel.uiState.value.tracks)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `retry limpia el error y vuelve a pedir la playlist`() = runTest {
        coEvery { playlistUseCases.getPlaylist("p1") } returns Result.failure(RuntimeException("sin red"))
        val viewModel = createViewModel()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { playlistUseCases.getPlaylist("p1") } returns Result.success(playlist)
        viewModel.retry()

        assertNull(viewModel.uiState.value.error)
        assertEquals(playlist, viewModel.uiState.value.playlist)
        assertEquals(listOf(track), viewModel.uiState.value.tracks)
        coVerify(exactly = 2) { playlistUseCases.getPlaylist("p1") }
    }

    /**
     * `retry()` cancela la carga anterior antes de relanzarla. Sin esa
     * cancelación, cada reintento dejaría una petición más viva a la vez.
     */
    @Test
    fun `retry x3 no acumula jobs ni deja dos peticiones vivas`() = runTest {
        var started = 0
        var ended = 0
        var maxLive = 0
        coEvery { playlistUseCases.getPlaylist("p1") } coAnswers {
            started++
            maxLive = maxOf(maxLive, started - ended)
            try {
                awaitCancellation()
            } finally {
                ended++
            }
        }

        val viewModel = createViewModel()
        assertEquals(1, started)

        repeat(3) { viewModel.retry() }
        advanceUntilIdle()

        assertEquals(4, started)
        assertEquals(3, ended)
        assertEquals(1, started - ended)
        assertEquals(1, maxLive)
    }
}
