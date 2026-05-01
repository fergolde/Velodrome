package com.example.velodrome.presentation.audio

import com.example.velodrome.domain.model.Track
import com.example.velodrome.domain.usecase.TrackUseCases
import com.example.velodrome.presentation.player.PlayerManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

sealed class RadioContext {
    object Random : RadioContext()
    data class GenreAndYear(val genres: List<String>, val fromYear: Int?, val toYear: Int?) : RadioContext()
}

@Singleton
class SmartRadioEngine @Inject constructor(
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentContext: RadioContext? = null
    private val pool = mutableListOf<Track>()
    private val sessionPlayedIds = mutableSetOf<String>()
    private var isRefilling = false
    private val recentArtists = ArrayDeque<String>(2)

    // Subfase 2.2 — startRadio(context: RadioContext)
    fun startRadio(context: RadioContext) {
        engineScope.launch {
            currentContext = context
            pool.clear()
            sessionPlayedIds.clear()
            recentArtists.clear()
            isRefilling = false

            // Awaiteado — el pool debe estar listo antes de continuar
            refillPool()

            // Inicializar recentArtists con los 2 últimos artistas del playlist actual
            val currentPlaylist = playerManager.playlist.value
            if (currentPlaylist.isNotEmpty()) {
                val lastTwo = currentPlaylist.takeLast(2)
                lastTwo.forEach { recentArtists.addLast(it.artistName) }
            }

            val initialTracks = pickNext(10)
            if (initialTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerManager.setPlaylist(initialTracks, startPlaying = true)
                    playerManager.setLoadMoreCallback { onLoadMoreRequested() }
                }
            }
        }
    }

    // Subfase 2.3 — onLoadMoreRequested()
    private fun onLoadMoreRequested() {
        engineScope.launch {
            if (pool.size < 15) {
                refillPool()
            }

            val nextTracks = pickNext(10)
            if (nextTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerManager.appendToPlaylist(nextTracks)
                }
            }
        }
    }

    // Subfase 2.4 — refillPool()
    private suspend fun refillPool() {
        if (isRefilling) return
        isRefilling = true

        try {
            val ctx = currentContext ?: return

            val newSongs = when (ctx) {
                is RadioContext.Random -> {
                    trackUseCases.getRandomSongs(size = 50).getOrDefault(emptyList())
                }
                is RadioContext.GenreAndYear -> {
                    val songs = mutableListOf<Track>()
                    if (ctx.genres.isEmpty()) {
                        songs.addAll(trackUseCases.getRandomSongs(size = 50, fromYear = ctx.fromYear, toYear = ctx.toYear).getOrDefault(emptyList()))
                    } else if (ctx.genres.size == 1) {
                        songs.addAll(trackUseCases.getRandomSongs(size = 50, genre = ctx.genres.first(), fromYear = ctx.fromYear, toYear = ctx.toYear).getOrDefault(emptyList()))
                    } else {
                        val limitPerGenre = 50 / ctx.genres.size
                        coroutineScope {
                            val deferreds = ctx.genres.map { genre ->
                                async { trackUseCases.getRandomSongs(size = limitPerGenre, genre = genre, fromYear = ctx.fromYear, toYear = ctx.toYear).getOrDefault(emptyList()) }
                            }
                            deferreds.awaitAll().forEach { songs.addAll(it) }
                        }
                    }
                    songs.shuffled()
                }
            }

            // Filtrar las que ya están en sessionPlayedIds y las que ya están en pool
            val existingPoolIds = pool.map { it.id }.toSet()
            val newFiltered = newSongs.filter { it.id !in sessionPlayedIds && it.id !in existingPoolIds }

            // Caso especial — sesión agotada: si no se añadió ninguna canción nueva,
            // desactivar el filtro y añadir todas ignorando sessionPlayedIds
            if (newFiltered.isNotEmpty()) {
                pool.addAll(newFiltered)
            } else if (newSongs.isNotEmpty()) {
                // Sesión agotada — añadir todas las recibidas ignorando sessionPlayedIds
                pool.addAll(newSongs.filter { it.id !in existingPoolIds })
            }

        } finally {
            isRefilling = false
        }
    }

    // Subfase 2.5 — pickNext(count: Int): List<Track>
    private fun pickNext(count: Int): List<Track> {
        val selected = mutableListOf<Track>()

        // Asegurar que recentArtists tiene los 2 últimos artistas del playlist actual
        val currentPlaylist = playerManager.playlist.value
        if (recentArtists.isEmpty() && currentPlaylist.isNotEmpty()) {
            currentPlaylist.takeLast(2).forEach { recentArtists.addLast(it.artistName) }
        }

        for (i in 0 until count) {
            if (pool.isEmpty()) break

            // Regla: no repetir artista (ventana de 2). Si no hay alternativa, usar todo el pool.
            val candidates = pool.filter { it.artistName !in recentArtists }
            val chosen = if (candidates.isNotEmpty()) {
                candidates.random()
            } else {
                pool.random()
            }

            pool.remove(chosen)
            sessionPlayedIds.add(chosen.id)
            selected.add(chosen)

            // Actualizar recentArtists (ventana de 2)
            recentArtists.addLast(chosen.artistName)
            if (recentArtists.size > 2) {
                recentArtists.removeFirst()
            }
        }

        return selected
    }
}