package com.fergolde.velodrome.presentation.screen.artistdetail

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reglas del fondo difuminado del header del detalle de artista.
 *
 * El fondo solo existe cuando hay portada: sin portada se conserva el fondo
 * plano de la app. En el teléfono el fondo sangra el padding de la grilla para
 * llegar a los bordes de la pantalla; en tablet, donde la grilla ya está
 * acotada a ancho contenido, el fondo se queda dentro de esa banda.
 */
class ArtistHeaderBackdropTest {

    @Test
    fun `sin portada no se pinta el fondo difuminado`() {
        assertFalse(artistHeaderShowsBackdrop(null))
        assertFalse(artistHeaderShowsBackdrop(""))
        assertFalse(artistHeaderShowsBackdrop("   "))
    }

    @Test
    fun `con portada se pinta el fondo difuminado`() {
        assertTrue(artistHeaderShowsBackdrop("ar-1234"))
        assertTrue(artistHeaderShowsBackdrop("ar-1234"))
    }

    @Test
    fun `en el telefono el fondo sangra el padding de la grilla`() {
        assertEquals(
            artistHeaderHorizontalGutter,
            artistHeaderBackdropBleed(WindowWidthSizeClass.Compact),
        )
        // Ese sangrado es exactamente el padding de contenido de la grilla:
        // con el fondo 2 x 16dp más ancho, llega de borde a borde.
        assertEquals(16.dp, artistHeaderHorizontalGutter)
    }

    @Test
    fun `en tablet el fondo no se sale de la banda de contenido`() {
        assertEquals(0.dp, artistHeaderBackdropBleed(WindowWidthSizeClass.Medium))
        assertEquals(0.dp, artistHeaderBackdropBleed(WindowWidthSizeClass.Expanded))
    }
}
