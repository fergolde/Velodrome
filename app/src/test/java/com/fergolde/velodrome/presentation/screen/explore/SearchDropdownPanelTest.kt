package com.fergolde.velodrome.presentation.screen.explore

import com.fergolde.velodrome.domain.model.Album
import com.fergolde.velodrome.domain.model.Artist
import com.fergolde.velodrome.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reglas del desplegable de búsqueda: con foco y query vacía se ofrecen las
 * búsquedas recientes; al escribir, las sugerencias locales; sin foco (o tras
 * elegir) el desplegable no se muestra.
 */
class SearchDropdownPanelTest {

    private val history = listOf("nach", "vetusta morla", "triana")

    private val suggestions = SearchResults(
        artists = listOf(Artist(id = "ar1", name = "Nacha Pop", albumCount = 2, coverUrl = null)),
        albums = listOf(
            Album(
                id = "al1",
                artistId = "ar1",
                artistName = "Nacha Pop",
                title = "Nacha",
                year = 1980,
                genre = "Pop",
                coverUrl = null,
            )
        ),
        tracks = listOf(
            Track(
                id = "t1",
                albumId = "al1",
                title = "Chica de ayer",
                durationSec = 200,
                sizeBytes = 1_000,
                trackNumber = 3,
            )
        ),
    )

    @Test
    fun `query vacia con foco muestra las busquedas recientes`() {
        val panel = searchDropdownPanel(
            isFocused = true,
            query = "",
            history = history,
            suggestions = suggestions,
        )

        assertEquals(SearchDropdownPanel.RecentSearches(history), panel)
    }

    @Test
    fun `query en blanco cuenta como vacia y muestra las busquedas recientes`() {
        val panel = searchDropdownPanel(
            isFocused = true,
            query = "   ",
            history = history,
            suggestions = suggestions,
        )

        assertEquals(SearchDropdownPanel.RecentSearches(history), panel)
    }

    @Test
    fun `query vacia sin historial no muestra el desplegable`() {
        val panel = searchDropdownPanel(
            isFocused = true,
            query = "",
            history = emptyList(),
            suggestions = suggestions,
        )

        assertNull(panel)
    }

    @Test
    fun `al escribir muestra las sugerencias locales`() {
        val panel = searchDropdownPanel(
            isFocused = true,
            query = "nach",
            history = history,
            suggestions = suggestions,
        )

        assertEquals(SearchDropdownPanel.Suggestions(suggestions), panel)
    }

    @Test
    fun `al escribir sin sugerencias el desplegable se cierra`() {
        val panel = searchDropdownPanel(
            isFocused = true,
            query = "zzz",
            history = history,
            suggestions = SearchResults(),
        )

        assertNull(panel)
    }

    @Test
    fun `al perder el foco el desplegable se cierra`() {
        val panel = searchDropdownPanel(
            isFocused = false,
            query = "",
            history = history,
            suggestions = suggestions,
        )

        assertNull(panel)
    }

    @Test
    fun `al elegir una entrada el desplegable se cierra aunque haya query y sugerencias`() {
        val panel = searchDropdownPanel(
            isFocused = false,
            query = "nach",
            history = history,
            suggestions = suggestions,
        )

        assertNull(panel)
    }
}
