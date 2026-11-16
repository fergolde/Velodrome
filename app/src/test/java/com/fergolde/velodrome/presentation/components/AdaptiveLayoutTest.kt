package com.fergolde.velodrome.presentation.components

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S1/L7: comportamiento del helper compartido de layout adaptativo.
 *
 * Antes cada pantalla repetía el mismo `when` sobre la clase de ancho y la
 * misma constante de 880dp para acotar los detalles. Estas pruebas fijan el
 * comportamiento único del helper; cada pantalla aporta solo sus números.
 */
class AdaptiveLayoutTest {

    @Test
    fun `cada clase de ancho usa su propia cuenta de columnas`() {
        // Números distintos por rama: si el `when` cayera siempre en el caso por
        // defecto, la prueba lo delata.
        assertEquals(
            2,
            adaptiveGridColumns(WindowWidthSizeClass.Compact, compact = 2, medium = 4, expanded = 6),
        )
        assertEquals(
            4,
            adaptiveGridColumns(WindowWidthSizeClass.Medium, compact = 2, medium = 4, expanded = 6),
        )
        assertEquals(
            6,
            adaptiveGridColumns(WindowWidthSizeClass.Expanded, compact = 2, medium = 4, expanded = 6),
        )
    }

    @Test
    fun `el telefono no acota el ancho del detalle`() {
        assertEquals(Dp.Infinity, detailsContentMaxWidth(WindowWidthSizeClass.Compact))
    }

    @Test
    fun `las clases de tablet acotan el detalle al ancho compartido`() {
        assertEquals(880.dp, detailsContentMaxWidth(WindowWidthSizeClass.Medium))
        assertEquals(880.dp, detailsContentMaxWidth(WindowWidthSizeClass.Expanded))
    }

    @Test
    fun `el ancho compartido de detalle cubre la clase Medium completa`() {
        assertEquals(880.dp, DetailsContentMaxWidth)
        // 840dp es el ancho máximo de la clase Medium: con 880dp el recorte real
        // solo aparece en Expanded, y el ancho de Medium queda intacto.
        assertTrue(
            "el tope de tablet debe cubrir la clase Medium completa (hasta 839dp)",
            DetailsContentMaxWidth >= 840.dp,
        )
    }
}
