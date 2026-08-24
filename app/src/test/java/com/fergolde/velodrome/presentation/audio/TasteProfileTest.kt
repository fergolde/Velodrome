package com.fergolde.velodrome.presentation.audio

import com.fergolde.velodrome.domain.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TasteProfileTest {

    private fun album(id: String, genre: String?, artist: String) =
        Album(id = id, artistId = "a-$id", artistName = artist, title = "T$id", year = 2020, genre = genre, coverUrl = null)

    @Test
    fun `frequent listens weigh more than recent ones`() {
        val profile = TasteProfileBuilder.fromServerLists(
            frequent = listOf(album("1", "Rock", "Artist A")),
            recent = listOf(album("2", "Rock", "Artist B"))
        )

        assertEquals(4, profile.genreWeights["Rock"])   // 3 (frequent) + 1 (recent)
        assertEquals(3, profile.artistWeights["Artist A"])
        assertEquals(1, profile.artistWeights["Artist B"])
    }

    @Test
    fun `blank genres and empty artists are ignored`() {
        val profile = TasteProfileBuilder.fromServerLists(
            frequent = listOf(album("1", "", "")),
            recent = listOf(album("2", null, ""))
        )

        assertNull(profile.genreWeights[""])
        assertNull(profile.artistWeights[""])
    }

    @Test
    fun `weightedPickByGenre prefers high affinity buckets`() {
        val items = listOf(
            SmartItem(TrackFactory.track("t1"), "Unknown Genre"),
            SmartItem(TrackFactory.track("t2"), "Rock"),
            SmartItem(TrackFactory.track("t3"), "Rock")
        )
        val weights = mapOf("Rock" to 10)

        // Roll within the Unknown-Genre slice picks t1; any roll inside the Rock
        // slice picks one of the Rock items — never the inverse.
        repeat(50) { i ->
            val chosen = weightedPickByGenre(items, weights, roll = 0.99)
            assertEquals("t3", chosen?.track?.id)
        }
    }

    @Test
    fun `empty list and zero roll edge cases`() {
        assertNull(weightedPickByGenre(emptyList(), emptyMap(), 0.5))
        val single = listOf(SmartItem(TrackFactory.track("only"), "Jazz"))
        assertEquals("only", weightedPickByGenre(single, emptyMap(), 0.0)?.track?.id)
    }
}

private object TrackFactory {
    fun track(id: String) = com.fergolde.velodrome.domain.model.Track(
        id = id, albumId = "a1", title = id, durationSec = 100,
        sizeBytes = 1L, trackNumber = 1
    )
}
