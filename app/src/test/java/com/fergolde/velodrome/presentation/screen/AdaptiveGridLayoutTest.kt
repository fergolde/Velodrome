package com.fergolde.velodrome.presentation.screen

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fergolde.velodrome.presentation.components.DetailsContentMaxWidth
import com.fergolde.velodrome.presentation.components.detailsContentMaxWidth
import com.fergolde.velodrome.presentation.screen.albums.albumGridColumns
import com.fergolde.velodrome.presentation.screen.artistdetail.artistAlbumGridColumns
import com.fergolde.velodrome.presentation.screen.artists.artistGridColumns
import com.fergolde.velodrome.presentation.screen.explore.genreGridColumns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S1/T3: grillas y detalles adaptativos por ancho de ventana.
 *
 * Antes cada pantalla decidía su layout por su cuenta y con señales distintas
 * (`smallestScreenWidthDp` en Álbumes, `R.bool.allow_rotation` en Artistas y en
 * el detalle de artista, orientación en los detalles). Ahora todas parten de
 * `WindowWidthSizeClass`: el teléfono (`Compact`) conserva exactamente las
 * columnas y el ancho que tenía, y las clases de tablet suman columnas y acotan
 * la columna central del detalle.
 */
class AdaptiveGridLayoutTest {

    @Test
    fun `el telefono conserva las columnas de siempre`() {
        assertEquals(3, albumGridColumns(WindowWidthSizeClass.Compact))
        assertEquals(2, artistGridColumns(WindowWidthSizeClass.Compact))
        assertEquals(2, artistAlbumGridColumns(WindowWidthSizeClass.Compact, isLandscape = false))
        assertEquals(3, genreGridColumns(WindowWidthSizeClass.Compact))
    }

    @Test
    fun `las clases de tablet suman columnas y crecen con el ancho`() {
        assertEquals(4, albumGridColumns(WindowWidthSizeClass.Medium))
        assertEquals(5, albumGridColumns(WindowWidthSizeClass.Expanded))
        assertEquals(4, artistGridColumns(WindowWidthSizeClass.Medium))
        assertEquals(6, artistGridColumns(WindowWidthSizeClass.Expanded))
        assertEquals(4, artistAlbumGridColumns(WindowWidthSizeClass.Medium, isLandscape = false))
        assertEquals(5, artistAlbumGridColumns(WindowWidthSizeClass.Expanded, isLandscape = false))
        assertEquals(4, genreGridColumns(WindowWidthSizeClass.Medium))
        assertEquals(6, genreGridColumns(WindowWidthSizeClass.Expanded))

        // Invariante común a las cuatro grillas: más ancho nunca da menos
        // columnas, así una tablet ancha no queda más apretada que una angosta.
        val grillas: List<Pair<String, (WindowWidthSizeClass) -> Int>> = listOf(
            "albumes" to ::albumGridColumns,
            "artistas" to ::artistGridColumns,
            "albumes de artista" to { clase: WindowWidthSizeClass ->
                artistAlbumGridColumns(clase, isLandscape = false)
            },
            "generos" to ::genreGridColumns,
        )
        grillas.forEach { (nombre, columnas) ->
            val compact = columnas(WindowWidthSizeClass.Compact)
            val medium = columnas(WindowWidthSizeClass.Medium)
            val expanded = columnas(WindowWidthSizeClass.Expanded)
            assertTrue(
                "$nombre: se esperaba compact < medium < expanded, fue $compact / $medium / $expanded",
                compact < medium && medium < expanded,
            )
        }
    }

    /**
     * En vertical la tarjeta de álbum mide lo mismo que siempre. Es el caso de
     * referencia: la orientación apaisada debe igualar ese tamaño, no estirarlo.
     */
    @Test
    fun `en vertical el detalle de artista conserva sus columnas`() {
        assertEquals(2, artistAlbumGridColumns(WindowWidthSizeClass.Compact, isLandscape = false))
        assertEquals(4, artistAlbumGridColumns(WindowWidthSizeClass.Medium, isLandscape = false))
        assertEquals(5, artistAlbumGridColumns(WindowWidthSizeClass.Expanded, isLandscape = false))
    }

    /**
     * En apaisado la grilla no tiene tope de ancho, así que mantener las columnas
     * verticales estiraba cada tarjeta. Se suman columnas para que mida ~200dp,
     * como en vertical, y para que más ancho nunca dé menos columnas.
     */
    @Test
    fun `en horizontal el detalle de artista suma columnas por clase`() {
        val compact = artistAlbumGridColumns(WindowWidthSizeClass.Compact, isLandscape = true)
        val medium = artistAlbumGridColumns(WindowWidthSizeClass.Medium, isLandscape = true)
        val expanded = artistAlbumGridColumns(WindowWidthSizeClass.Expanded, isLandscape = true)

        assertEquals(3, compact)
        assertEquals(4, medium)
        assertEquals(6, expanded)

        assertTrue(
            "se esperaba compact < medium < expanded, fue $compact / $medium / $expanded",
            compact < medium && medium < expanded,
        )
        // La orientación apaisada nunca deja la tarjeta más grande que en vertical.
        assertTrue(compact > artistAlbumGridColumns(WindowWidthSizeClass.Compact, isLandscape = false))
        assertTrue(expanded > artistAlbumGridColumns(WindowWidthSizeClass.Expanded, isLandscape = false))
    }

    @Test
    fun `el detalle de telefono no acota el ancho`() {
        assertEquals(Dp.Infinity, detailsContentMaxWidth(WindowWidthSizeClass.Compact))
    }

    @Test
    fun `en tablet el detalle se centra a un ancho contenido`() {
        // Los tres detalles comparten un único tope: ya no hay una constante por
        // pantalla que pueda desincronizarse.
        assertEquals(DetailsContentMaxWidth, detailsContentMaxWidth(WindowWidthSizeClass.Medium))
        assertEquals(DetailsContentMaxWidth, detailsContentMaxWidth(WindowWidthSizeClass.Expanded))
        assertEquals(880.dp, DetailsContentMaxWidth)
        // 840dp es el ancho máximo de la clase Medium: un tope de 880dp todavía
        // no recorta nada ahí, así que el acotado real solo aparece en Expanded.
        assertTrue(
            "el tope de tablet debe cubrir la clase Medium completa (hasta 839dp)",
            detailsContentMaxWidth(WindowWidthSizeClass.Expanded) >= 840.dp,
        )
    }
}
