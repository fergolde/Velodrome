package com.fergolde.velodrome.presentation.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class EqualizerPresetMappingTest {

    @Test
    fun `rock bucket maps to Rock`() {
        assertEquals("Rock", presetNameForGenre("Rock"))
        assertEquals("Rock", presetNameForGenre("Heavy Metal"))
        assertEquals("Rock", presetNameForGenre("punk rock"))
    }

    @Test
    fun `dance bucket wins over pop for electronic genres`() {
        assertEquals("Dance", presetNameForGenre("Electronic"))
        assertEquals("Dance", presetNameForGenre("Hip Hop"))
        assertEquals("Dance", presetNameForGenre("Reggaetón"))
    }

    @Test
    fun `jazz classical pop folk buckets`() {
        assertEquals("Jazz", presetNameForGenre("Smooth Jazz"))
        assertEquals("Classical", presetNameForGenre("Música Clásica"))
        assertEquals("Pop", presetNameForGenre("Pop"))
        assertEquals("Folk", presetNameForGenre("Country Folk"))
    }

    @Test
    fun `null empty and unknown genres fall back to Normal`() {
        assertEquals("Normal", presetNameForGenre(null))
        assertEquals("Normal", presetNameForGenre(""))
        assertEquals("Normal", presetNameForGenre("   "))
        assertEquals("Normal", presetNameForGenre("Klezmer Fusion"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals("Rock", presetNameForGenre("ROCK"))
        assertEquals("Jazz", presetNameForGenre("jAzZ"))
    }

    @Test
    fun `user library genres map as designed`() {
        // The exact genre list of the owner's library — regression guard.
        val expected = mapOf(
            "Hip-Hop" to "Dance",
            "pop" to "Pop",
            "heavy" to "Rock",
            "rock" to "Rock",
            "folk rock" to "Rock",
            "latin" to "Dance",
            "alternativo" to "Rock",
            "punk" to "Rock",
            "country" to "Folk",
            "electro" to "Dance",
            "folk metal" to "Rock",
            "dance" to "Dance"
        )
        expected.forEach { (genre, preset) ->
            assertEquals("genre: $genre", preset, presetNameForGenre(genre))
        }
    }
}
