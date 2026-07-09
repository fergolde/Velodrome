package com.fergolde.velodrome.presentation.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fergolde.velodrome.domain.model.Track
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ExploreViewModel"

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val albumUseCases: AlbumUseCases,
    private val artistUseCases: ArtistUseCases,
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val smartRadioEngine: SmartRadioEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        loadContent()
        observeSearchQuery()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(500L) // Espera 500 ms tras dejar de escribir
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
                                trackUseCases.searchRemoteTracks(query).getOrDefault(emptyList())
                            }

                            // Esperamos a que todos terminen (el tiempo total será el de la petición más lenta)
                            val artists = artistsDeferred.await()
                            val albums = albumsDeferred.await()
                            val tracks = tracksDeferred.await()

                            // 4. Actualizar estado final
                            _uiState.update { it.copy(
                                searchResults = SearchResults(
                                    artists = artists,
                                    albums = albums,
                                    tracks = tracks
                                ),
                                isSearching = false
                            ) }
                        }
                    } catch (e: Exception) {
                        // 5. Manejo de excepciones
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
                _uiState.update { it.copy(minYear = if (minYear > 0) minYear else 1950) }
            } catch (e: Exception) {
                _uiState.update { it.copy(minYear = 1950) }
            }
        }

        viewModelScope.launch {
            // Artistas: ahora desde BD local, aleatorios
            val localArtists = artistUseCases.observeArtists().first()
            _uiState.update {
                it.copy(randomArtists = localArtists.shuffled().take(20))
            }
        }

        // Álbumes siguen desde API (son endpoints específicos: random, latest...)
        viewModelScope.launch {
            albumUseCases.getRandomAlbums(size = 20)
                .onSuccess { albums ->
                    _uiState.update { it.copy(randomAlbums = albums) }
                }
        }

        viewModelScope.launch {
            albumUseCases.getRandomAlbums(size = 10)
                .onSuccess { albums ->
                    _uiState.update { it.copy(curatedAlbums = albums) }
                }
        }

        viewModelScope.launch {
            albumUseCases.getGenres()
                .onSuccess { genres ->
                    _uiState.update { it.copy(genres = genres, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueryFlow.value = query
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", isSearching = false, searchResults = SearchResults()) }
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

        _uiState.update { it.copy(isLoading = true) }

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
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun playSearchedTrack(track: Track) {
        playerManager.playTrack(track)
    }

    fun onPlayTrackNow(track: Track) {
        playerManager.playNow(track)
    }

    fun onPlayTrackNext(track: Track) {
        playerManager.playNext(track)
    }

    fun onAddTrackToQueue(track: Track) {
        playerManager.addToQueue(track)
    }

    fun onYearRangeSelected(range: IntRange?) {
        _uiState.update { it.copy(selectedYearRange = range) }
    }
}