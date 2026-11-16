package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.compose.runtime.Immutable

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.DEFAULT_ARTIST_ALBUM_SORT_ORDER
import com.fergolde.velodrome.domain.repository.SettingsRepository
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random


/**
 * Orden de los álbumes del detalle de artista (S1). [storageKey] es el valor
 * que se guarda en [SettingsRepository.artistAlbumSortOrder].
 */
enum class AlbumSortOrder(val storageKey: String) {
    YEAR_ASC("year_asc"),
    YEAR_DESC("year_desc"),
    TITLE_ASC("title_asc"),
    MOST_PLAYED("most_played"),
    RANDOM("random");

    companion object {
        /** Cualquier clave desconocida (o ausente) cae en año ascendente. */
        fun fromStorageKey(key: String?): AlbumSortOrder =
            entries.firstOrNull { it.storageKey == key } ?: YEAR_ASC
    }
}

/**
 * Años presentes en [albums], sin repetir y de más viejo a más nuevo. Los
 * álbumes sin año no aportan entrada al filtro.
 */
fun albumYearsOf(albums: List<Album>): List<Int> =
    albums.mapNotNull { it.year }.distinct().sorted()

/**
 * Reproducciones por álbum derivadas del historial local: suma del `playCount`
 * de cada track guardado en Room. Es la única fuente de reproducciones que el
 * detalle tiene en el dispositivo, así que ordenar no toca la red.
 */
fun localPlaysByAlbum(tracks: List<Track>): Map<String, Int> =
    tracks.groupBy { it.albumId }
        .mapValues { (_, albumTracks) -> albumTracks.sumOf { it.playCount } }

/**
 * Ordena [albums] según [order]. Función pura: no toca Room ni Compose, vivir
 * fuera del ViewModel la hace testeable en JVM.
 *
 * - Año: un álbum sin año cuenta como 0, igual que el orden que el loader ya
 *   aplicaba antes de S1; en ascendente queda primero y en descendente último.
 * - Más tocadas: desempata por año y luego por título, para que el resultado
 *   no dependa del orden en que respondió el servidor.
 * - Aleatorio: determinista para una misma [randomSeed].
 */
fun sortArtistAlbums(
    albums: List<Album>,
    order: AlbumSortOrder,
    playsByAlbum: Map<String, Int> = emptyMap(),
    randomSeed: Long = 0L
): List<Album> = when (order) {
    AlbumSortOrder.YEAR_ASC -> albums.sortedBy { it.year ?: 0 }
    AlbumSortOrder.YEAR_DESC -> albums.sortedByDescending { it.year ?: 0 }
    AlbumSortOrder.TITLE_ASC -> albums.sortedBy { it.title.lowercase() }
    AlbumSortOrder.MOST_PLAYED -> albums.sortedWith(
        compareByDescending<Album> { playsByAlbum[it.id] ?: 0 }
            .thenBy { it.year ?: 0 }
            .thenBy { it.title.lowercase() }
    )
    AlbumSortOrder.RANDOM -> albums.shuffled(Random(randomSeed))
}

/** Álbumes visibles: primero el filtro por año, después el orden elegido. */
fun visibleArtistAlbums(
    albums: List<Album>,
    order: AlbumSortOrder,
    selectedYear: Int?,
    playsByAlbum: Map<String, Int> = emptyMap(),
    randomSeed: Long = 0L
): List<Album> {
    val filtered = selectedYear?.let { year -> albums.filter { it.year == year } } ?: albums
    return sortArtistAlbums(filtered, order, playsByAlbum, randomSeed)
}

@Immutable
data class ArtistDetailUiState(
    val artist: Artist? = null,
    /** Álbumes visibles: ya filtrados por año y ordenados. */
    val albums: List<Album> = emptyList(),
    val sortOrder: AlbumSortOrder = AlbumSortOrder.YEAR_ASC,
    /** Año elegido en el filtro; `null` significa sin filtro. */
    val selectedYear: Int? = null,
    /** Años presentes en los álbumes del artista, de más viejo a más nuevo. */
    val availableYears: List<Int> = emptyList(),
    val isLoading: Boolean = true,
    val isPreparingPlayback: Boolean = false, // <-- Agregado
    val error: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val artistId: String = savedStateHandle.get<String>("artistId") ?: ""

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    // Job de la última carga del artista: `retry()` lo cancela antes de relanzar
    // el loader para no dejar dos peticiones vivas a la vez.
    private var loadJob: Job? = null

    // Álbumes completos del artista. `uiState.albums` expone el subconjunto
    // visible, así el filtro y el orden no destruyen los datos originales.
    private var allAlbums: List<Album> = emptyList()

    // Reproducciones locales por álbum, cargadas solo cuando hacen falta.
    private var playsByAlbum: Map<String, Int> = emptyMap()
    private var playsLoaded = false

    // Semilla del orden aleatorio: avanza solo al volver a elegir "aleatorio",
    // así el orden no cambia entre recomposiciones ni al mover el filtro.
    private var randomSeed: Long = 0L

    init {
        // La preferencia se lee antes de pintar para no mostrar un orden y
        // saltar a otro. Si DataStore falla, se usa el orden por defecto.
        viewModelScope.launch {
            val stored = settingsRepository.artistAlbumSortOrder
                .catch { emit(DEFAULT_ARTIST_ALBUM_SORT_ORDER) }
                .firstOrNull()
            _uiState.update { it.copy(sortOrder = AlbumSortOrder.fromStorageKey(stored)) }
            loadArtistData()
        }
    }

    private fun loadArtistData() {
        if (artistId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid artist ID") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }
        loadJob?.cancel()

        loadJob = viewModelScope.launch {
            artistUseCases.getArtist(artistId)
                .onSuccess { artistWithAlbums ->
                    allAlbums = artistWithAlbums.albums
                    _uiState.update { it.copy(
                        artist = artistWithAlbums.artist,
                        isLoading = false
                    ) }
                    refreshVisibleAlbums()
                    loadLocalPlayCountsIfNeeded()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    /**
     * Cambia el orden visible y lo guarda como preferencia.
     *
     * Volver a elegir "aleatorio" genera un orden nuevo; el resto de criterios
     * es determinista.
     */
    fun setSortOrder(order: AlbumSortOrder) {
        if (order == AlbumSortOrder.RANDOM) randomSeed++
        persistSortOrder(order)
        _uiState.update { it.copy(sortOrder = order) }
        refreshVisibleAlbums()
        loadLocalPlayCountsIfNeeded()
    }

    /** Filtra los álbumes por [year]; `null` quita el filtro. */
    fun setYearFilter(year: Int?) {
        _uiState.update { it.copy(selectedYear = year) }
        refreshVisibleAlbums()
    }

    /**
     * Recalcula los álbumes visibles a partir de [allAlbums].
     *
     * Un año que ya no existe en el artista deja el filtro en "todos" para no
     * mostrar una lista vacía sin explicación.
     */
    private fun refreshVisibleAlbums() {
        val years = albumYearsOf(allAlbums)
        _uiState.update { state ->
            val selectedYear = state.selectedYear?.takeIf { it in years }
            state.copy(
                availableYears = years,
                selectedYear = selectedYear,
                albums = visibleArtistAlbums(
                    albums = allAlbums,
                    order = state.sortOrder,
                    selectedYear = selectedYear,
                    playsByAlbum = playsByAlbum,
                    randomSeed = randomSeed
                )
            )
        }
    }

    /**
     * Carga el historial local solo cuando el orden lo necesita.
     *
     * Si la lectura falla, el orden queda en el desempate por año y título en
     * vez de romper la pantalla; se reintenta en la próxima elección.
     */
    private fun loadLocalPlayCountsIfNeeded() {
        if (_uiState.value.sortOrder != AlbumSortOrder.MOST_PLAYED || playsLoaded) return
        viewModelScope.launch {
            try {
                playsByAlbum = localPlaysByAlbum(
                    trackUseCases.getTracksForAlbumIds(allAlbums.map { it.id })
                )
                playsLoaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Sin historial local no hay nada que sumar.
            }
            refreshVisibleAlbums()
        }
    }

    private fun persistSortOrder(order: AlbumSortOrder) {
        viewModelScope.launch {
            try {
                settingsRepository.setArtistAlbumSortOrder(order.storageKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // El orden ya está aplicado en memoria; guardar es best-effort.
            }
        }
    }

    /**
     * Reintenta la carga tras un fallo.
     *
     * Limpia el error y relanza el mismo loader sobre este ViewModel: la pantalla
     * no se recrea y no queda ningún ViewModel anterior retenido.
     */
    fun retry() {
        _uiState.update { it.copy(error = null) }
        loadArtistData()
    }

    // Lógica para obtener todas las canciones de los álbumes visibles:
    // reproducir respeta el filtro por año y el orden que el usuario ve.
    private suspend fun gatherAllArtistTracks(): List<Track> = supervisorScope {
        val albums = _uiState.value.albums
        val albumIds = albums.map { it.id }
        if (albumIds.isEmpty()) return@supervisorScope emptyList()

        // 1. Sincronizar todos los álbumes en paralelo con fan-out acotado
        val semaphore = Semaphore(5)
        albums.map { album ->
            async {
                semaphore.withPermit { trackUseCases.syncTracksForAlbum(album.id) }
            }
        }.awaitAll()

        // 2. Obtener las canciones de la DB local en una sola query IN
        trackUseCases.getTracksForAlbumIds(albumIds)
    }

    /** Smart artist radio: dense on this artist, opening toward taste affinities. */
    fun startArtistRadio() {
        val name = uiState.value.artist?.name ?: return
        smartRadioEngine.stopRadio()
        smartRadioEngine.startRadio(RadioContext.Artist(artistName = name))
        // Smart radio prepares tracks asynchronously; do not lock the UI flag here.
    }

    fun playAll() = prepareTracks { tracks ->
        playerManager.setPlaylist(tracks, startIndex = 0, startPlaying = true)
    }

    fun shuffleAll() = prepareTracks { tracks ->
        playerManager.setPlaylist(tracks.shuffled(), startIndex = 0, startPlaying = true)
    }

    fun addToQueue() = prepareTracks { tracks ->
        tracks.shuffled().forEach { track ->
            playerManager.addToQueue(track)
        }
    }

    private fun prepareTracks(block: suspend (List<Track>) -> Unit) {
        smartRadioEngine.stopRadio()
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingPlayback = true) }
            try {
                val tracks = gatherAllArtistTracks()
                if (tracks.isNotEmpty()) {
                    block(tracks)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isPreparingPlayback = false) }
            }
        }
    }
}