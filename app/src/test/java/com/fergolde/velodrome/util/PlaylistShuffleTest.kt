package com.fergolde.velodrome.util

import com.fergolde.velodrome.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistShuffleTest {

    private fun track(id: String, artist: String) = Track(
        id = id,
        albumId = "a1",
        title = "Song $id",
        artistName = artist,
        durationSec = 180,
        sizeBytes = 5_000_000,
        bitrate = 320,
        trackNumber = 1
    )

    @Test
    fun `no two consecutive tracks from same artist`() {
        val tracks = buildList {
            repeat(4) { i -> add(track("a$i", "Artist A")) }
            repeat(4) { i -> add(track("b$i", "Artist B")) }
            repeat(4) { i -> add(track("c$i", "Artist C")) }
        }

        val result = tracks.shuffledWithArtistSpacing()

        assertEquals(tracks.size, result.size)
        for (i in 1 until result.size) {
            assertTrue(
                "Adjacent same-artist at index $i",
                result[i].artistName != result[i - 1].artistName
            )
        }
    }

    @Test
    fun `preserves all elements`() {
        val tracks = buildList {
            repeat(3) { i -> add(track("a$i", "Artist A")) }
            repeat(2) { i -> add(track("b$i", "Artist B")) }
        }

        val result = tracks.shuffledWithArtistSpacing()

        assertEquals(tracks.map { it.id }.sorted(), result.map { it.id }.sorted())
    }

    @Test
    fun `single track returned unchanged`() {
        val tracks = listOf(track("t1", "Artist A"))

        val result = tracks.shuffledWithArtistSpacing()

        assertEquals(listOf("t1"), result.map { it.id })
    }

    @Test
    fun `empty list returns empty list`() {
        assertTrue(emptyList<Track>().shuffledWithArtistSpacing().isEmpty())
    }

    @Test
    fun `dominant artist falls back without losing tracks`() {
        // 7 of 10 from one artist: adjacent repeats unavoidable, must still return all
        val tracks = buildList {
            repeat(7) { i -> add(track("a$i", "Artist A")) }
            repeat(3) { i -> add(track("b$i", "Artist B")) }
        }

        val result = tracks.shuffledWithArtistSpacing()

        assertEquals(tracks.size, result.size)
        assertEquals(tracks.map { it.id }.sorted(), result.map { it.id }.sorted())
    }

    @Test
    fun `all same artist returns all tracks`() {
        val tracks = (1..5).map { track("t$it", "Only Artist") }

        val result = tracks.shuffledWithArtistSpacing()

        assertEquals(5, result.size)
    }
}
