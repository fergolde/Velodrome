package com.fergolde.velodrome.presentation.audio

import android.util.Log
import com.fergolde.velodrome.domain.model.Track
import com.fergolde.velodrome.domain.usecase.AlbumUseCases
import com.fergolde.velodrome.domain.usecase.TrackUseCases
import com.fergolde.velodrome.presentation.player.PlayerManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

sealed class RadioContext {
    object Random : RadioContext()
    data class GenreAndYear(val genres: List<String>, val fromYear: Int?, val toYear: Int?) : RadioContext()

    /** Instant mix seeded by a track's features (genre/artist). */
    data class Song(val seedTrackId: String) : RadioContext()

    /** Artist radio: dense start on the artist, opens toward taste affinities. */
    data class Artist(val artistId: String, val artistName: String) : RadioContext()
}

/** Local candidate with its resolved album genre (null when untagged). */
internal data class SmartItem(val track: Track, val genre: String?)

/**
 * Weighted random pick using the taste profile's genre weights as multipliers.
 * Pure so selection math stays unit-testable.
 */
internal fun weightedPickByGenre(
    items: List<SmartItem>,
    genreWeights: Map<String, Int>,
    roll: Double = Random.nextDouble()
): SmartItem? {
    if (items.isEmpty()) return null
    val weights = items.map { item ->
        1.0 + ((genreWeights[item.genre]?.toDouble() ?: 0.0) * GENRE_WEIGHT_MULTIPLIER)
    }
    val total = weights.sum()
    var target = roll * total
    weights.forEachIndexed { i, w ->
        target -= w
        if (target <= 0) return items[i]
    }
    return items.last()
}

private const val GENRE_WEIGHT_MULTIPLIER = 2.0

@Singleton
class SmartRadioEngine @Inject constructor(
    private val trackUseCases: TrackUseCases,
    private val playerManager: PlayerManager,
    private val albumUseCases: AlbumUseCases
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentContext: RadioContext? = null
    private val pool = mutableListOf<Track>()
    private val sessionPlayedIds = mutableSetOf<String>()
    private var isRefilling = false
    private val recentArtists = ArrayDeque<String>(2)

    // ── Smart contexts (Song / Artist) ──────────────────────────────────────
    private var corePool = mutableListOf<SmartItem>()
    private var explorePool = mutableListOf<SmartItem>()
    private var genreByAlbum: Map<String, String?> = emptyMap()
    private var explorationBias = ARTIST_EXPLORE_START

    @Volatile
    private var tasteProfile: TasteProfile? = null
    private var tasteProfileAt = 0L

    /**
     * Detiene cualquier radio activa: limpia estado y desactiva el auto-extendido
     * para que una reproducción normal (álbum, etc.) no se "contamine".
     * El perfil de gusto se conserva entre radios (cache de 24 h).
     */
    fun stopRadio() {
        currentContext = null
        pool.clear()
        corePool.clear()
        explorePool.clear()
        explorationBias = ARTIST_EXPLORE_START
        sessionPlayedIds.clear()
        recentArtists.clear()
        isRefilling = false
        playerManager.setLoadMoreCallback { }
    }

    fun startRadio(context: RadioContext) {
        engineScope.launch {
            currentContext = context
            pool.clear()
            corePool.clear()
            explorePool.clear()
            explorationBias = ARTIST_EXPLORE_START
            sessionPlayedIds.clear()
            recentArtists.clear()
            isRefilling = false

            refillPool()

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

    private fun onLoadMoreRequested() {
        engineScope.launch {
            Log.d(TAG, "onLoadMoreRequested: pool.size=${pool.size} core=${corePool.size} explore=${explorePool.size}")
            refillPool()
            val nextTracks = pickNext(10)
            if (nextTracks.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    playerManager.appendToPlaylist(nextTracks)
                }
            }
        }
    }

    private suspend fun refillPool() {
        if (isRefilling) return
        isRefilling = true

        try {
            val ctx = currentContext ?: return
            Log.d(TAG, "refillPool: context=$ctx")

            when (ctx) {
                is RadioContext.Random -> refillLegacy { trackUseCases.getRandomSongs(size = 50) }
                is RadioContext.GenreAndYear -> refillGenreAndYear(ctx)
                is RadioContext.Song -> buildSongPools(ctx.seedTrackId)
                is RadioContext.Artist -> buildArtistPools(ctx.artistName)
            }
        } finally {
            isRefilling = false
        }
    }

    // ── Legacy contexts (unchanged behavior) ─────────────────────────────────

    private suspend fun refillLegacy(request: suspend () -> Result<List<Track>>) {
        val newSongs = fetchWithRetry(request)
        appendLegacy(newSongs)
    }

    private suspend fun refillGenreAndYear(ctx: RadioContext.GenreAndYear) {
        val songs = mutableListOf<Track>()
        if (ctx.genres.isEmpty()) {
            songs.addAll(fetchWithRetry { trackUseCases.getRandomSongs(size = 50, fromYear = ctx.fromYear, toYear = ctx.toYear) })
        } else if (ctx.genres.size == 1) {
            songs.addAll(fetchWithRetry { trackUseCases.getRandomSongs(size = 50, genre = ctx.genres.first(), fromYear = ctx.fromYear, toYear = ctx.toYear) })
        } else {
            val limitPerGenre = 50 / ctx.genres.size
            coroutineScope {
                val deferreds = ctx.genres.map { genre ->
                    async {
                        fetchWithRetry {
                            trackUseCases.getRandomSongs(size = limitPerGenre, genre = genre, fromYear = ctx.fromYear, toYear = ctx.toYear)
                        }
                    }
                }
                deferreds.awaitAll().forEach { songs.addAll(it) }
            }
        }
        appendLegacy(songs.shuffled())
    }

    private fun appendLegacy(newSongs: List<Track>) {
        val existingPoolIds = pool.map { it.id }.toSet()
        val newFiltered = newSongs.filter { it.id !in existingPoolIds }
        Log.d(TAG, "appendLegacy: newSongs=${newSongs.size} new=${newFiltered.size} poolBefore=${existingPoolIds.size}")

        if (newFiltered.isNotEmpty()) {
            pool.addAll(newFiltered)
        } else if (newSongs.isNotEmpty()) {
            pool.addAll(newSongs)
        }
    }

    // ── Smart contexts ────────────────────────────────────────────────────────

    private suspend fun buildSongPools(seedTrackId: String) {
        ensureLocalLibrary()
        val all = corePool + explorePool
        val seed = all.firstOrNull { it.track.id == seedTrackId }
        if (seed == null) {
            // Seed not resolvable locally: fall back to server random discovery.
            topUpExploreFromServer(force = true)
            return
        }
        val core = all.filter {
            it !== seed && (
                (seed.genre != null && it.genre == seed.genre) ||
                    it.track.artistName == seed.track.artistName
                )
        }
        val chosenIds = core.map { it.track.id }.toSet() + seed.track.id
        val explore = all.filter { it.track.id !in chosenIds }

        replaceSmartPools(core.shuffled(), explore.shuffled())
    }

    private suspend fun buildArtistPools(artistName: String) {
        ensureLocalLibrary()
        val all = corePool + explorePool
        val core = all.filter { it.track.artistName == artistName }
        val explore = all.filter { it.track.artistName != artistName }
        replaceSmartPools(core.shuffled(), explore.shuffled())
    }

    private suspend fun ensureLocalLibrary() {
        val albums = albumUseCases.getLocalAlbums().associateBy { it.id }
        genreByAlbum = albums.mapValues { it.value.genre }
        val tracks = trackUseCases.getAllLocalTracks()
        val items = tracks.map { SmartItem(it, genreByAlbum[it.albumId]) }
        replaceSmartPools(items.filter { it.genre != null }, items.filter { it.genre == null })
        // Temporary split: buildSongPools/buildArtistPools re-partition properly.
    }

    private fun replaceSmartPools(core: List<SmartItem>, explore: List<SmartItem>) {
        corePool = core.toMutableList()
        explorePool = explore.toMutableList()
    }

    /**
     * Exploration fuel: server random songs keep the discovery side alive once
     * the local neighborhood is exhausted.
     */
    private suspend fun topUpExploreFromServer(force: Boolean) {
        if (!force && explorePool.size >= EXPLORE_TOPUP_THRESHOLD) return
        val fresh = fetchWithRetry { trackUseCases.getRandomSongs(size = 30) }
        val existing = (corePool + explorePool).map { it.track.id }.toSet() + sessionPlayedIds
        explorePool += fresh
            .filter { it.id !in existing }
            .map { SmartItem(it, genreByAlbum[it.albumId]) }
    }

    private suspend fun loadTasteProfile(): TasteProfile? {
        tasteProfile?.let {
            if (System.currentTimeMillis() - tasteProfileAt < PROFILE_TTL_MS) return it
        }
        val fresh = runCatching {
            val frequent = albumUseCases.getTopAlbums(PROFILE_SIZE).getOrDefault(emptyList())
            val recent = albumUseCases.getRecentlyPlayedAlbums(PROFILE_SIZE).getOrDefault(emptyList())
            TasteProfileBuilder.fromServerLists(frequent, recent)
        }.getOrNull()
        if (fresh != null) {
            tasteProfile = fresh
            tasteProfileAt = System.currentTimeMillis()
        }
        return tasteProfile // stale profile beats none when the refresh fails
    }

    // ── Selection ────────────────────────────────────────────────────────────

    private suspend fun pickNext(count: Int): List<Track> {
        return when (currentContext) {
            is RadioContext.Song, is RadioContext.Artist -> pickSmart(count)
            else -> pickLegacy(count)
        }
    }

    private fun pickLegacy(count: Int): List<Track> {
        val selected = mutableListOf<Track>()

        val currentPlaylist = playerManager.playlist.value
        if (recentArtists.isEmpty() && currentPlaylist.isNotEmpty()) {
            currentPlaylist.takeLast(2).forEach { recentArtists.addLast(it.artistName) }
        }

        for (i in 0 until count) {
            if (pool.isEmpty()) break

            val candidates = pool.filter { it.artistName !in recentArtists }
            val chosen = if (candidates.isNotEmpty()) candidates.random() else pool.random()

            pool.remove(chosen)
            sessionPlayedIds.add(chosen.id)
            selected.add(chosen)

            recentArtists.addLast(chosen.artistName)
            if (recentArtists.size > 2) recentArtists.removeFirst()
        }
        return selected
    }

    /**
     * Smart picking:
     * - Song seeds draw ~60% core (same genre / same artist) and ~40% exploration.
     * - Artist seeds start dense (~80% artist) and open up batch by batch until
     *   exploration reaches 50%.
     * - Exploration picks are weighted by the cross-device taste profile.
     */
    private suspend fun pickSmart(count: Int): List<Track> {
        val selected = mutableListOf<Track>()
        val profile = loadTasteProfile()

        repeat(count) {
            if (corePool.isEmpty() && explorePool.isEmpty()) return selected

            val wantCore = when (val ctx = currentContext) {
                is RadioContext.Artist -> {
                    val useCore = Random.nextDouble() > explorationBias
                    explorationBias = (explorationBias + ARTIST_EXPLORE_RAMP)
                        .coerceAtMost(ARTIST_EXPLORE_MAX)
                    useCore
                }
                else -> Random.nextDouble() < SONG_CORE_PROBABILITY
            }

            val primary = if (wantCore) corePool else explorePool
            val secondary = if (wantCore) explorePool else corePool
            val source = primary.ifEmpty { secondary }.ifEmpty { return selected }

            val candidates = source.filter { it.track.artistName !in recentArtists }
            val bucket = candidates.ifEmpty { source }
            val chosen = if (source === explorePool) {
                weightedPickByGenre(bucket, profile?.genreWeights ?: emptyMap())
                    ?: bucket.random()
            } else {
                bucket.random()
            }

            source.remove(chosen)
            sessionPlayedIds.add(chosen.track.id)
            selected.add(chosen.track)

            recentArtists.addLast(chosen.track.artistName)
            if (recentArtists.size > 2) recentArtists.removeFirst()
        }

        // Keep exploration fueled for future batches.
        topUpExploreFromServer(force = false)

        return selected
    }

    private suspend fun fetchWithRetry(
        request: suspend () -> Result<List<Track>>
    ): List<Track> {
        repeat(MAX_FETCH_ATTEMPTS) { attempt ->
            val result = try {
                request()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(error)
            }

            if (result.isSuccess) {
                return result.getOrDefault(emptyList())
            }

            if (attempt == MAX_FETCH_ATTEMPTS - 1) {
                Log.w(TAG, "Track fetch failed after retries", result.exceptionOrNull())
            } else {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }
        return emptyList()
    }

    private companion object {
        const val MAX_FETCH_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 500L
        const val TAG = "SmartRadioEngine"

        const val SONG_CORE_PROBABILITY = 0.6
        const val ARTIST_EXPLORE_START = 0.2   // dense artist start
        const val ARTIST_EXPLORE_MAX = 0.5
        const val ARTIST_EXPLORE_RAMP = 0.05   // per picked track
        const val EXPLORE_TOPUP_THRESHOLD = 10
        const val PROFILE_SIZE = 50
        const val PROFILE_TTL_MS = 24L * 60 * 60 * 1000
    }
}
