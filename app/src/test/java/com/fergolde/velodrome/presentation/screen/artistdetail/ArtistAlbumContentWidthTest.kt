package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import com.fergolde.velodrome.presentation.components.DetailsContentMaxWidth
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tope de ancho de la grilla de álbumes del detalle de artista.
 *
 * En vertical la grilla se acota a [DetailsContentMaxWidth] para que el header no
 * se desparrame de borde a borde. En horizontal ese tope se descarta: en tablet
 * apaisada dejaba bandas vacías a los lados en vez de aprovechar el ancho.
 */
class ArtistAlbumContentWidthTest {

    @Test
    fun `en vertical la grilla conserva el tope de tablet`() {
        assertEquals(
            DetailsContentMaxWidth,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Medium, isLandscape = false),
        )
        assertEquals(
            DetailsContentMaxWidth,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Expanded, isLandscape = false),
        )
    }

    @Test
    fun `en horizontal la grilla ocupa todo el ancho`() {
        assertEquals(
            Dp.Infinity,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Medium, isLandscape = true),
        )
        assertEquals(
            Dp.Infinity,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Expanded, isLandscape = true),
        )
    }

    @Test
    fun `el telefono nunca acota el ancho`() {
        assertEquals(
            Dp.Infinity,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Compact, isLandscape = false),
        )
        assertEquals(
            Dp.Infinity,
            artistAlbumsContentMaxWidth(WindowWidthSizeClass.Compact, isLandscape = true),
        )
    }
}
