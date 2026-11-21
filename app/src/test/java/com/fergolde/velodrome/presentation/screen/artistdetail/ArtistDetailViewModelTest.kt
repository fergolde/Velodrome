package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.lifecycle.SavedStateHandle
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.ArtistWithAlbums
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.repository.DEFAULT_ARTIST_ALBUM_SORT_ORDER
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.domain.usecase.GetArtistUseCase
import com.fergolde.velodrome.domain.usecase.GetTracksForAlbumIdsUseCase
import com.fergolde.velodrome.domain.usecase.ObserveTracksByAlbumUseCase
import com.fergolde.velodrome.domain.usecase.SyncTracksForAlbumUseCase
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val artist = Artist(id = "a1", name = "Artist", albumCount = 1, coverUrl = null)
    private val album = Album(id = "al1", artistId = "a1", artistName = "Artist", title = "Album", year = 2020, genre = null, coverUrl = null)

    private val getArtistUseCase: GetArtistUseCase = mockk()
    private val syncTracksForAlbumUseCase: SyncTracksForAlbumUseCase = mockk()
    private val observeTracksByAlbumUseCase: ObserveTracksByAlbumUseCase = mockk()
    private val getTracksForAlbumIdsUseCase: GetTracksForAlbumIdsUseCase = mockk(relaxed = true)
    private val playerManager: PlayerManager = mockk(relaxed = true)
    private val smartRadioEngine: SmartRadioEngine = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val artistUseCases = ArtistUseCases(
        getArtist = getArtistUseCase,
        searchLocal = mockk(),
        observeArtists = mockk()
    )

    private val trackUseCases = TrackUseCases(
        observeTracksByAlbum = observeTracksByAlbumUseCase,
        syncTracksForAlbum = syncTracksForAlbumUseCase,
        getRandomSongs = mockk(),
        searchRemoteTracks = mockk(),
        getOfflineTracks = mockk(),
        getTopGlobalTracks = mockk(),
        getAllLocalTracks = mockk(),
        getTracksForAlbumIds = getTracksForAlbumIdsUseCase
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getArtistUseCase(any()) } returns Result.success(ArtistWithAlbums(artist, listOf(album)))
        every { settingsRepository.artistAlbumSortOrder } returns flowOf(DEFAULT_ARTIST_ALBUM_SORT_ORDER)
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
            smartRadioEngine = smartRadioEngine,
            settingsRepository = settingsRepository
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

    /**
     * El detalle de artista solo cargaba desde `init`: un fallo dejaba el error
     * en pantalla sin forma de reintentar.
     */
    @Test
    fun `retry limpia el error y vuelve a pedir el artista`() = runTest {
        coEvery { getArtistUseCase(any()) } returns Result.failure(RuntimeException("sin red"))
        val viewModel = createViewModel()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { getArtistUseCase(any()) } returns Result.success(ArtistWithAlbums(artist, listOf(album)))
        viewModel.retry()

        assertNull(viewModel.uiState.value.error)
        assertEquals(artist, viewModel.uiState.value.artist)
        assertEquals(listOf(album), viewModel.uiState.value.albums)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 2) { getArtistUseCase("a1") }
    }

    @Test
    fun `startArtistRadio does not set isPreparingPlayback`() = runTest {
        val viewModel = createViewModel()
        viewModel.startArtistRadio()

        assertFalse(viewModel.uiState.value.isPreparingPlayback)
        verify { smartRadioEngine.startRadio(any()) }
    }

    // --- Orden y filtros de álbumes (S1) ---

    private fun album(id: String, title: String, year: Int?) = Album(
        id = id,
        artistId = "a1",
        artistName = "Artist",
        title = title,
        year = year,
        genre = null,
        coverUrl = null
    )

    private fun stubArtistAlbums(vararg albums: Album) {
        coEvery { getArtistUseCase(any()) } returns
            Result.success(ArtistWithAlbums(artist, albums.toList()))
    }

    /**
     * El loader ya ordenaba por año ascendente: sin preferencia guardada ese
     * sigue siendo el orden visible, no el orden en que responde el servidor.
     */
    @Test
    fun `sin preferencia guardada los álbumes siguen en año ascendente`() = runTest {
        every { settingsRepository.artistAlbumSortOrder } returns flowOf()
        stubArtistAlbums(album("b", "Beta", 2020), album("a", "Alpha", 1990))

        val viewModel = createViewModel()

        assertEquals(listOf("a", "b"), viewModel.uiState.value.albums.map { it.id })
        assertEquals(AlbumSortOrder.YEAR_ASC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `el orden guardado se aplica al cargar el detalle`() = runTest {
        every { settingsRepository.artistAlbumSortOrder } returns flowOf("title_asc")
        stubArtistAlbums(album("b", "Beta", 2020), album("a", "Alpha", 1990))

        val viewModel = createViewModel()

        assertEquals(listOf("a", "b"), viewModel.uiState.value.albums.map { it.id })
        assertEquals(AlbumSortOrder.TITLE_ASC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `elegir un orden lo persiste y reordena la lista`() = runTest {
        stubArtistAlbums(album("old", "Old", 1990), album("new", "New", 2020))
        val viewModel = createViewModel()

        viewModel.setSortOrder(AlbumSortOrder.YEAR_DESC)

        coVerify { settingsRepository.setArtistAlbumSortOrder("year_desc") }
        assertEquals(listOf("new", "old"), viewModel.uiState.value.albums.map { it.id })
        assertEquals(AlbumSortOrder.YEAR_DESC, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `el filtro por año se limita a los años presentes y se puede quitar`() = runTest {
        stubArtistAlbums(
            album("old", "Old", 1990),
            album("new", "New", 2020),
            album("unknown", "Unknown", null)
        )
        val viewModel = createViewModel()
        assertEquals(listOf(1990, 2020), viewModel.uiState.value.availableYears)

        viewModel.setYearFilter(2020)
        assertEquals(listOf("new"), viewModel.uiState.value.albums.map { it.id })
        assertEquals(2020, viewModel.uiState.value.selectedYear)

        viewModel.setYearFilter(null)
        // Sin filtro vuelve el orden por defecto del loader: el álbum sin año
        // cuenta como 0 y por eso queda primero.
        assertEquals(listOf("unknown", "old", "new"), viewModel.uiState.value.albums.map { it.id })
        assertNull(viewModel.uiState.value.selectedYear)
    }

    /**
     * "Más tocadas" se deriva del historial local disponible: las
     * reproducciones por track que ya están en Room, sumadas por álbum.
     */
    @Test
    fun `más tocadas usa las reproducciones locales de los tracks`() = runTest {
        stubArtistAlbums(album("few", "Few", 1990), album("many", "Many", 2020))
        coEvery { getTracksForAlbumIdsUseCase(any()) } returns listOf(
            Track(
                id = "t1", albumId = "few", title = "t1", durationSec = 10,
                sizeBytes = 0L, trackNumber = 1, playCount = 1
            ),
            Track(
                id = "t2", albumId = "many", title = "t2", durationSec = 10,
                sizeBytes = 0L, trackNumber = 1, playCount = 9
            )
        )
        val viewModel = createViewModel()

        viewModel.setSortOrder(AlbumSortOrder.MOST_PLAYED)
        advanceUntilIdle()

        assertEquals(listOf("many", "few"), viewModel.uiState.value.albums.map { it.id })
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
        coEvery { getArtistUseCase("a1") } coAnswers {
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
