package com.fergolde.velodrome.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Reglas puras del historial de búsqueda. Se prueban aquí porque la parte de
 * Room (SQL) no corre en un test JVM.
 */
class SearchHistoryRulesTest {

    @Test
    fun `blank query is normalized to null`() {
        assertEquals(null, normalizeSearchQuery(""))
        assertEquals(null, normalizeSearchQuery("   "))
        assertEquals(null, normalizeSearchQuery("\n\t "))
    }

    @Test
    fun `query is trimmed`() {
        assertEquals("daft punk", normalizeSearchQuery("  daft punk  "))
    }

    @Test
    fun `new query goes first without touching unrelated entries`() {
        val result = applySearchHistoryEntry(
            previous = listOf("daft punk", "air"),
            rawQuery = "m83",
        )

        assertEquals(listOf("m83", "daft punk", "air"), result)
    }

    @Test
    fun `repeated query is not duplicated and moves to the front`() {
        val result = applySearchHistoryEntry(
            previous = listOf("daft punk", "air", "m83"),
            rawQuery = "m83",
        )

        assertEquals(listOf("m83", "daft punk", "air"), result)
    }

    @Test
    fun `matching ignores case when deduplicating`() {
        val result = applySearchHistoryEntry(
            previous = listOf("Rock"),
            rawQuery = "rock",
        )

        assertEquals(listOf("rock"), result)
    }

    @Test
    fun `earlier prefix of the new query is dropped`() {
        // El usuario escribió "daf" y luego "daft": el historial no debe llenarse
        // con los estados intermedios de una misma búsqueda.
        val result = applySearchHistoryEntry(
            previous = listOf("daf", "da"),
            rawQuery = "daft",
        )

        assertEquals(listOf("daft"), result)
    }

    @Test
    fun `a longer earlier query is kept when the new one is shorter`() {
        val result = applySearchHistoryEntry(
            previous = listOf("daft punk"),
            rawQuery = "daft",
        )

        assertEquals(listOf("daft", "daft punk"), result)
    }

    @Test
    fun `history is capped at the limit keeping the newest`() {
        val previous = (1..10).map { "query $it" }

        val result = applySearchHistoryEntry(previous = previous, rawQuery = "nueva")

        assertEquals(MAX_SEARCH_HISTORY_ENTRIES, result.size)
        assertEquals("nueva", result.first())
        assertEquals("query 9", result.last())
    }

    @Test
    fun `blank query records nothing`() {
        val previous = listOf("daft punk", "air")

        assertEquals(previous, applySearchHistoryEntry(previous, "   "))
        assertEquals(emptyList<String>(), applySearchHistoryEntry(emptyList(), ""))
    }

    @Test
    fun `previous history is kept intact when the new query is blank`() {
        // Guarda contra borrar el historial al pulsar limpiar en el buscador.
        val previous = listOf("air", "m83")

        assertEquals(previous, applySearchHistoryEntry(previous, ""))
    }
}
