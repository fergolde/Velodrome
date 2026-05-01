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

    fun startRadio(context: RadioContext) {
        engineScope.launch {
            currentContext = context
            pool.clear()
            sessionPlayedIds.clear()

            refillPool()

            val initialTracks = pickNext(10)
            if (initialTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerManager.setPlaylist(initialTracks, startPlaying = true)
                    playerManager.setLoadMoreCallback { checkAndLoadMore() }
                }
            }
        }
    }

    private fun checkAndLoadMore() {
        engineScope.launch {
            if (pool.size < 10) {
                refillPool()
            }

            val nextTracks = pickNext(5)
            if (nextTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerManager.appendToPlaylist(nextTracks)
                }
            }
        }
    }

    private fun pickNext(count: Int): List<Track> {
        val selected = mutableListOf<Track>()

        if (pool.size < count) {
            engineScope.launch { refillPool() }
        }

        for (i in 0 until count) {
            var available = pool.filter { it.id !in sessionPlayedIds }
            if (available.isEmpty()) available = pool.toList()
            if (available.isEmpty()) break

            val track = available.random()
            pool.remove(track)
            selected.add(track)

            sessionPlayedIds.add(track.id)
        }
        return selected
    }

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

            val existingIds = pool.map { it.id }.toSet()
            pool.addAll(newSongs.filter { it.id !in existingIds })

        } finally {
            isRefilling = false
        }
    }
}