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
}
