package com.fergolde.velodrome.util

import com.fergolde.velodrome.domain.model.Track

/**
 * Shuffles the list randomly while avoiding two consecutive tracks from the
 * same artist when possible.
 *
 * Greedy strategy: repeatedly take a track from the artist with the most
 * remaining tracks that differs from the last placed artist. If a single
 * artist holds more than half of the playlist, adjacent repeats are
 * mathematically unavoidable and the greedy choice is used as fallback.
 */
fun List<Track>.shuffledWithArtistSpacing(): List<Track> {
    if (size < 2) return toList()

    val buckets = groupBy { it.artistName }
        .values
        .map { ArrayDeque(it.shuffled()) }
        .sortedByDescending { it.size }
        .toMutableList()

    val result = ArrayList<Track>(size)
    var lastArtist: String? = null

    repeat(size) {
        val candidate = buckets
            .filter { it.isNotEmpty() && it.first().artistName != lastArtist }
            .maxByOrNull { it.size }
            ?: buckets.first { it.isNotEmpty() }

        val track = candidate.removeFirst()
        result += track
        lastArtist = track.artistName
    }

    return result
}
