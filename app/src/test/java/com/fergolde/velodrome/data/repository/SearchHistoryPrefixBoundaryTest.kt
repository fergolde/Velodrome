package com.fergolde.velodrome.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Límites de la regla de historial no cubiertos por SearchHistoryRulesTest.
 */
class SearchHistoryPrefixBoundaryTest {
    @Test
    fun `filling from empty stops at ten newest`() {
        var history = emptyList<String>()
        (1..12).forEach { history = applySearchHistoryEntry(history, "q$it") }
        assertEquals(10, history.size)
        assertEquals("q12", history.first())
        assertEquals("q3", history.last())
    }

    @Test
    fun `an entry containing the query in the middle is kept`() {
        assertEquals(
            listOf("daft", "punk daft", "air"),
            applySearchHistoryEntry(listOf("punk daft", "air"), "daft")
        )
    }

    @Test
    fun `a longer previous entry is not swallowed by a shorter new one`() {
        assertEquals(
            listOf("daft", "daft punk", "air"),
            applySearchHistoryEntry(listOf("daft punk", "air"), "daft")
        )
    }

    @Test
    fun `new query with surrounding spaces is stored trimmed`() {
        assertEquals(listOf("air", "m83"), applySearchHistoryEntry(listOf("m83"), "  air  "))
    }
}
