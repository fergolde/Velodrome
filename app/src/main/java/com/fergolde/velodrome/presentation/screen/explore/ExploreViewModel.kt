package com.fergolde.velodrome.presentation.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.repository.SearchHistoryRepository
import com.fergolde.velodrome.domain.repository.SearchSuggestionRepository
import com.fergolde.velodrome.domain.usecase.AlbumUseCases
import com.fergolde.velodrome.domain.usecase.ArtistUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.audio.RadioContext
import com.fergolde.velodrome.presentation.audio.SmartRadioEngine
import com.fergolde.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds


@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val albumUseCases: AlbumUseCases,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val searchSuggestionRepository: SearchSuggestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadContent()
        observeSearchHistory()
        observeSuggestions()
        observeSearchQuery()
    }

    /** Historial de la cuenta activa: local, sin red. */
    private fun observeSearchHistory() {
        viewModelScope.launch {
            searchHistoryRepository.observeHistory().collect { history ->
                _uiState.update { it.copy(searchHistory = history) }
            }
        }
    }

    /**
     * Sugerencias por prefijo sobre la biblioteca local. Se piden antes que los
     * resultados remotos (debounce más corto) para que aparezcan primero.
     */
    @OptIn(FlowPreview::class)
    private fun observeSuggestions() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(200L.milliseconds)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(suggestions = SearchResults()) }
                        return@collectLatest
                    }

                    try {
                        val suggestions = SearchResults(
                            artists = searchSuggestionRepository.suggestArtists(query),
                            albums = searchSuggestionRepository.suggestAlbums(query),
                            tracks = searchSuggestionRepository.suggestTracks(query)
                        )
                        _uiState.update { it.copy(suggestions = suggestions) }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.update { it.copy(suggestions = SearchResults()) }
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(500L.milliseconds) // Espera 500 ms tras dejar de escribir
                .distinctUntilChanged() // No busca si la query es idéntica a la anterior
                .collectLatest { query ->
                    // 1. Limpiar resultados si la query está vacía
                    if (query.isBlank()) {
                        _uiState.update {
                            it.copy(searchResults = SearchResults(), isSearching = false)
                        }
                        return@collectLatest
                    }

                    // 2. Iniciar estado de carga
                    _uiState.update { it.copy(isSearching = true) }

                    try {
                        // 3. Ejecución en paralelo
                        // coroutineScope asegura que si esta búsqueda se cancela (porque llega otra query),
                        // todas las tareas internas se cancelen inmediatamente.
                        coroutineScope {
                            val artistsDeferred = async { artistUseCases.searchLocal(query) }
                            val albumsDeferred = async { albumUseCases.searchLocal(query) }
                            val tracksDeferred = async {
                                // Importante: usamos getOrDefault para que si falla la red,
                                // los resultados locales (artistas/albums) sigan apareciendo.
                                trackUseCases.searchRemoteTracks(query)
                            }

                            // Esperamos a que todos terminen (el tiempo total será el de la petición más lenta)
                            val artists = artistsDeferred.await()
                            val albums = albumsDeferred.await()
                            val tracksResult = tracksDeferred.await()
                            val tracks = tracksResult.getOrDefault(emptyList())

                            // 4. Actualizar estado final
                            _uiState.update { it.copy(
                                searchResults = SearchResults(
                                    artists = artists,
                                    albums = albums,
                                    tracks = tracks
                                ),
                                isSearching = false
                            ) }

                            // 5. Historial: solo se guarda la query cuya búsqueda
                            // remota salió bien. La decisión (ver T2) es guardar en
                            // cada búsqueda remota exitosa y no en cada submit,
                            // porque el buscador no tiene acción de teclado y así
                            // el historial también se llena buscando desde el teclado
                            // sin confirmar. El repositorio descarta los prefijos
                            // intermedios para no llenar la lista con "d", "da", "daf".
                            if (tracksResult.isSuccess) {
                                searchHistoryRepository.recordQuery(query)
                            }
                        }
                    } catch (e: Exception) {
                        // 6. Manejo de excepciones
                        // Si el error es por cancelación (el usuario escribió otra letra), lo relanzamos.
                        if (e is kotlinx.coroutines.CancellationException) throw e

                        _uiState.update { it.copy(isSearching = false) }
                    }
                }
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            try {
                val minYear = albumUseCases.getMinYear()
                _uiState.update { it.copy(minYear = if (minYear > 0) minYear else 1950, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(minYear = 1950, isLoading = false) }
            }
        }

        viewModelScope.launch {
            // Artistas: colectamos el Flow local para reaccionar a la sync sin bloquear.
            artistUseCases.observeArtists().collect { artists ->
                _uiState.update {
                    it.copy(
                        randomArtists = artists.shuffled().take(20),
                        isLoading = false
                    )
                }
            }
        }

        // Álbumes siguen desde API (son endpoints específicos: random, latest...)
        viewModelScope.launch {
            albumUseCases.getRandomAlbums(size = 20)
                .onSuccess { albums ->
                    _uiState.update { it.copy(randomAlbums = albums, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }

        viewModelScope.launch {
            albumUseCases.getGenres()
                .onSuccess { genres ->
                    _uiState.update { it.copy(genres = genres, isLoading = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchResults = SearchResults(),
                suggestions = SearchResults()
            )
        }
        // Sin esto la query vieja seguía viva en el debounce y podía registrarse
        // en el historial después de limpiar el buscador.
        searchQueryFlow.value = ""
    }

    /** Repite una búsqueda guardada: basta con devolverla al buscador. */
    fun onHistoryEntryClick(query: String) {
        onSearchQueryChange(query)
    }

    fun onRemoveHistoryEntry(query: String) {
        viewModelScope.launch { searchHistoryRepository.removeQuery(query) }
    }

    fun onClearSearchHistory() {
        viewModelScope.launch { searchHistoryRepository.clearHistory() }
    }

    fun onGenreToggle(genre: String) {
        _uiState.update { state ->
            val newSelected = if (state.selectedGenres.contains(genre)) {
                state.selectedGenres - genre
            } else {
                state.selectedGenres + genre
            }
            state.copy(selectedGenres = newSelected)
        }
    }

    fun onPlayGenres() {
        val selectedGenres = _uiState.value.selectedGenres.toList()
        val yearRange = _uiState.value.selectedYearRange

        val context = if (selectedGenres.isEmpty() && yearRange == null) {
            RadioContext.Random
        } else {
            RadioContext.GenreAndYear(
                genres = selectedGenres,
                fromYear = yearRange?.first,
                toYear = yearRange?.last
            )
        }

        viewModelScope.launch {
            smartRadioEngine.startRadio(context)
        }
    }

    fun playSearchedTrack(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.playTrack(track)
    }

    fun onPlayTrackNow(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.playNow(track)
    }

    fun onPlayTrackNext(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.playNext(track)
    }

    fun onAddTrackToQueue(track: Track) {
        smartRadioEngine.stopRadio()
        playerManager.addToQueue(track)
    }

    fun onYearRangeSelected(range: IntRange?) {
        _uiState.update { it.copy(selectedYearRange = range) }
    }
}