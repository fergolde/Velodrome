package com.example.velodrome.presentation.screen.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.AlbumUseCases
import com.example.velodrome.domain.usecase.ArtistUseCases
import com.example.velodrome.domain.usecase.TrackUseCases
import com.example.velodrome.presentation.player.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val playerManager: PlayerManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    // Dynamic playlist state
    private val playlist = mutableListOf<Track>()
    private var currentPlaylistPosition = 0
    private var isLoadingMore = false
    private var currentGenreFilter: List<String> = emptyList()

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
        val selectedGenres = _uiState.value.selectedGenres
        val yearRange = _uiState.value.selectedYearRange

        _uiState.update { it.copy(isLoading = true) }

        playlist.clear()
        currentPlaylistPosition = 0

        currentGenreFilter = if (selectedGenres.isEmpty()) emptyList() else selectedGenres.toList()

        val fromYear = yearRange?.first
        val toYear = yearRange?.last

        viewModelScope.launch {
            try {
                val songsResult: Result<List<Track>>

                if (selectedGenres.isEmpty() && yearRange == null) {
                    songsResult = trackUseCases.getRandomSongs(size = 10)
                } else if (selectedGenres.isEmpty() && yearRange != null) {
                    songsResult = trackUseCases.getRandomSongs(size = 10, fromYear = fromYear, toYear = toYear)
                } else if (selectedGenres.size == 1 && yearRange != null) {
                    val genre = selectedGenres.first()
                    songsResult = trackUseCases.getRandomSongs(size = 10, genre = genre, fromYear = fromYear, toYear = toYear)
                } else if (selectedGenres.size == 1) {
                    val genre = selectedGenres.first()
                    songsResult = trackUseCases.getRandomSongs(size = 10, genre = genre)
                } else {
                    // FIX: múltiples géneros — pedir 10 por género y mezclar
                    val allSongs = mutableListOf<Track>()
                    coroutineScope {
                        val deferreds = selectedGenres.map { genre ->
                            async {
                                trackUseCases.getRandomSongs(
                                    size = 10,
                                    genre = genre,
                                    fromYear = fromYear,
                                    toYear = toYear
                                )
                            }
                        }
                        deferreds.awaitAll().forEach { result ->
                            result.onSuccess { songs -> allSongs.addAll(songs) }
                        }
                    }
                    songsResult = Result.success(allSongs.distinctBy { it.id }.shuffled().take(10))
                }

                songsResult.onSuccess { songs ->
                    playlist.addAll(songs)
                }.onFailure { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    return@launch
                }

                playlist.shuffle()

                val initialTracks = playlist.take(10)
                currentPlaylistPosition = 10

                _uiState.update { it.copy(isLoading = false, dynamicPlaylist = initialTracks) }

                playerManager.setLoadMoreCallback { checkAndLoadMore() }

                if (initialTracks.isNotEmpty()) {
                    playerManager.setPlaylist(initialTracks, startPlaying = true)
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadMoreTracks() {
        if (isLoadingMore) {
            return
        }

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val songsResult: Result<List<Track>>

                if (currentGenreFilter.isEmpty()) {
                    songsResult = trackUseCases.getRandomSongs(size = 10)
                } else if (currentGenreFilter.size == 1) {
                    val genre = currentGenreFilter.first()
                    songsResult = trackUseCases.getRandomSongsByGenre(genre, size = 10)
                } else {
                    // Multiple genres: parallel fetching using structured concurrency
                    val allSongs = mutableListOf<Track>()
                    coroutineScope {
                        val deferreds = (1..10).map { _ ->
                            async {
                                val randomGenre = currentGenreFilter.random()
                                trackUseCases.getRandomSongsByGenre(randomGenre, size = 1)
                            }
                        }
                        deferreds.awaitAll().forEach { result ->
                            result.onSuccess { songs ->
                                allSongs.addAll(songs)
                            }
                        }
                    }
                    songsResult = Result.success(allSongs)
                }

                songsResult.onSuccess { newSongs ->
                    if (newSongs.isNotEmpty()) {
                        playerManager.appendToPlaylist(newSongs)
                    }
                }

            } catch (_: Exception) {
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun checkAndLoadMore() {
        loadMoreTracks()
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