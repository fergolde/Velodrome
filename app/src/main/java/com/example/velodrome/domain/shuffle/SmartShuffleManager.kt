package com.example.velodrome.domain.shuffle

import com.example.velodrome.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartShuffleManager @Inject constructor() {

    private val playedIds: MutableSet<String> = mutableSetOf()
    private var sessionExhausted: Boolean = false

    fun startNewSession() {
        playedIds.clear()
        sessionExhausted = false
    }

    fun registerTracks(tracks: List<Track>) {
        tracks.forEach { track ->
            playedIds.add(track.id)
        }
    }

    fun filterNew(candidates: List<Track>): List<Track> {
        if (sessionExhausted) {
            return candidates
        }

        val filtered = candidates.filter { it.id !in playedIds }

        return if (filtered.isEmpty()) {
            sessionExhausted = true
            candidates
        } else {
            filtered
        }
    }

    fun applyArtistSpacing(tracks: List<Track>, lastArtist: String?): List<Track> {
        if (lastArtist == null) {
            return tracks.shuffled()
        }

        val safe = tracks.filter { it.artistName != lastArtist }.toMutableList()
        val conflict = tracks.filter { it.artistName == lastArtist }.toMutableList()

        if (conflict.isEmpty()) {
            return tracks.shuffled()
        }

        val result = mutableListOf<Track>()

        while (safe.isNotEmpty()) {
            val safeIndex = safe.indices.random()
            result.add(safe.removeAt(safeIndex))

            if (conflict.isNotEmpty()) {
                val conflictIndex = conflict.indices.random()
                result.add(conflict.removeAt(conflictIndex))
            }
        }

        result.addAll(conflict)

        return result
    }
}