package com.fergolde.velodrome.presentation.audio

import com.fergolde.velodrome.domain.model.Album

/**
 * Cross-device listening-affinity profile derived from Navidrome's own
 * aggregates (most-frequent + recently-played album lists). The server is the
 * single source of truth so every device converges to the same profile.
 */
data class TasteProfile(
    val genreWeights: Map<String, Int>,
    val artistWeights: Map<String, Int>
)

object TasteProfileBuilder {

    private const val FREQUENT_WEIGHT = 3
    private const val RECENT_WEIGHT = 1

    /**
     * Accumulates affinity weights from the two server album lists.
     * Frequent listens weigh more than a recent one-off play.
     */
    fun fromServerLists(frequent: List<Album>, recent: List<Album>): TasteProfile {
        val genres = HashMap<String, Int>()
        val artists = HashMap<String, Int>()

        fun accumulate(album: Album, weight: Int) {
            album.genre?.trim()?.takeIf { it.isNotEmpty() }?.let {
                genres[it] = (genres[it] ?: 0) + weight
            }
            album.artistName.trim().takeIf { it.isNotEmpty() }?.let {
                artists[it] = (artists[it] ?: 0) + weight
            }
        }

        frequent.forEach { accumulate(it, FREQUENT_WEIGHT) }
        recent.forEach { accumulate(it, RECENT_WEIGHT) }

        return TasteProfile(genres, artists)
    }
}
